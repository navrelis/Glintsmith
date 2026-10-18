#!/usr/bin/env python3
"""Verify the Glintsmith art.  Standard library only.

    py -B art/verify.py            about 6 minutes (the real studio GIFs on disk are not rebuilt)
    py -B art/verify.py --full     also rebuilds the GIFs from the real studio scenes in art/captures
                                   and checks the GIFs in docs/curseforge/img/ (run it after the real build)

1. PNG reader: every filter type and colour layout the game or the vanilla textures use decodes back exactly.
2. Manifest: known element types (no drawn ones), positive crops, every picture inside its card, area-filter options,
   every capture is in SHOTLIST.md.
2b. Contrast: every text colour the build sets on a background it uses (the art/README.md table, the label styles, the
   named text colours, the colours the manifest asks for) has a WCAG contrast ratio of at least 4.5:1.
3. Determinism: the logo files, the cards and the GIFs are built twice (from the captures in the repository, GIFs only with
   --full, and from synthetic test captures: in-world PNGs plus the synthetic studio scenes of the contract, art/studio/
   synth_studio.py) and must be byte-identical both times; the synthetic build draws no placeholder.
4. Committed files: every image in docs/curseforge/img/ and the logo files equal a fresh build (an image whose
   captures are not present on this machine is skipped, not failed; GIFs only with --full).
5. Sizes and GIF properties: card and banner sizes from the manifest (a card whose studio renders are not here yet may
   still be the old one), the GIFs built from the synthetic scenes have the manifest size, a caption bar where asked, an
   endless loop and are below their budget; with --full the GIFs on disk are checked the same way.
6. Area-filter downscale (exact averages) and the logo / icon from a synthetic studio still, including what logo.py does
   when the render is missing (a message, nothing written, exit 0, or 1 with --strict).
6b. Studio (art/studio/): matte round trip of a black / white pair (alpha and colour within 1, exact empty / opaque pixels,
   premultiplied supersampling), a synthetic turntable end to end (check, GIF with the expected frames, size, delays, delta
   boxes, loop, budget and the step-down, identical bytes for any number of workers), stills, the "studio" card element
   (crop, trim, labels) and its missing-scene behaviour, the synthetic contract (seven scenes, names and sizes), the CLI
   exit codes (also synth_studio's --size).
7. Standard library only for every script under art/, and no __pycache__ anywhere below art/.

Exit code 0 when everything passes, 1 otherwise.
"""
import argparse
import ast
import hashlib
import os
import shutil
import struct
import subprocess
import sys
import tempfile
import zlib

sys.dont_write_bytecode = True
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
sys.path.insert(0, os.path.join(HERE, "gallery"))
sys.path.insert(0, os.path.join(HERE, "studio"))

import build     # noqa: E402
import common    # noqa: E402
import gifs      # noqa: E402
import imgio     # noqa: E402
import lib       # noqa: E402
import logo      # noqa: E402
import synth     # noqa: E402
import composite     # noqa: E402  (art/studio)
import gifenc        # noqa: E402
import matte         # noqa: E402
import pipeline      # noqa: E402
import scene as studio_scene   # noqa: E402
import synth_studio  # noqa: E402

ROOT = lib.ROOT
MB = 1024 * 1024
FULL = False        # --full: also the GIFs from the real studio scenes in art/captures and the GIFs in docs/curseforge/img
# sha256 of the hand-drawn emblem files of commit a816b40 (icon.png, logo_animated.gif); only used to print a warning
HAND_DRAWN = {"3a251baa9cfe9f17ab281e4d08b6595645ead9d2ecfb928bc4990b268c2627f3",
              "43e7d0f655c10a1f643d0a4baf1f07f73395a2577d31908916f35d0692fe1f4c"}
failures = []


def check(ok, msg):
    print(("  ok    " if ok else "  FAIL  ") + msg)
    if not ok:
        failures.append(msg)


def sha(data):
    return hashlib.sha256(data).hexdigest()


def disk(rel):
    return os.path.join(ROOT, *rel.split("/"))


def read(path):
    with open(path, "rb") as f:
        return f.read()


# ---------------------------------------------------------------------------------------- 1. PNG reader

def paeth(a, b, c):
    p = a + b - c
    pa, pb, pc = abs(p - a), abs(p - b), abs(p - c)
    return a if pa <= pb and pa <= pc else (b if pb <= pc else c)


def encode_filtered(w, h, ch, pixels, filters, ctype=None, depth=8, extra=b""):
    """A PNG whose row y uses filter filters[y % len(filters)] (an independent encoder, for the reader test)."""
    stride = w * ch
    raw = bytearray()
    prev = bytes(stride)
    for y in range(h):
        row = pixels[y * stride:(y + 1) * stride]
        ft = filters[y % len(filters)]
        raw.append(ft)
        for i in range(stride):
            a = row[i - ch] if i >= ch else 0
            b = prev[i]
            c = prev[i - ch] if i >= ch else 0
            pred = (0, a, b, (a + b) >> 1, paeth(a, b, c))[ft]
            raw.append((row[i] - pred) & 255)
        prev = row
    ihdr = struct.pack(">IIBBBBB", w, h, depth, ctype if ctype is not None else (2 if ch == 3 else 6), 0, 0, 0)
    return (bytes((0x89,)) + b"PNG\r\n\x1a\n" + imgio._chunk(b"IHDR", ihdr) + extra
            + imgio._chunk(b"IDAT", zlib.compress(bytes(raw))) + imgio._chunk(b"IEND", b""))


def test_reader():
    print("1. PNG reader")
    w, h = 23, 11
    seed = 12345
    px = bytearray()
    for _ in range(w * h * 4):
        seed = (seed * 1103515245 + 12345) % 2147483648
        px.append((seed >> 16) & 255)
    rgba = bytes(px)
    rgb = bytes(b for i in range(w * h) for b in rgba[i * 4:i * 4 + 3])
    for ft in range(5):
        for ch, src in ((4, rgba), (3, rgb)):
            data = encode_filtered(w, h, ch, src, [ft])
            rw, rh, buf = imgio.read_png(data)
            want = src if ch == 4 else bytes(v for i in range(w * h) for v in (src[i * 3], src[i * 3 + 1], src[i * 3 + 2], 255))
            check((rw, rh) == (w, h) and bytes(buf) == want,
                  "filter %d, %s: decoded exactly" % (ft, "RGBA" if ch == 4 else "RGB"))
    data = encode_filtered(w, h, 4, rgba, [0, 1, 2, 3, 4])
    check(bytes(imgio.read_png(data)[2]) == rgba, "mixed filters per row, as the game's PNG writer produces")
    rw, rh, buf = imgio.read_png(data, max_rows=5)
    check((rw, rh) == (w, 5) and bytes(buf) == rgba[:w * 5 * 4], "max_rows stops early and returns the top rows")
    # a 1 bit palette PNG with a transparent entry (the shape of the vanilla font sheet)
    row_bits = [0b10100000, 0b01010000]
    pal = imgio._chunk(b"PLTE", bytes((0, 0, 0, 255, 255, 255))) + imgio._chunk(b"tRNS", bytes((0,)))
    data = encode_filtered(1, 2, 1, bytes(row_bits), [0], ctype=3, depth=1, extra=pal)
    w2, h2, buf = imgio.read_png(data)
    check((w2, h2) == (1, 2) and bytes(buf) == bytes((255, 255, 255, 255, 0, 0, 0, 0)),
          "1 bit palette PNG with a transparent entry decodes exactly")
    try:
        imgio.read_png(b"not a png")
        check(False, "garbage is rejected")
    except ValueError:
        check(True, "garbage is rejected with ValueError")


# ---------------------------------------------------------------------------------------- 2. manifest

DRAWN = {"logo", "sparkle", "arrow"}      # element types that drew art; they no longer exist


def test_manifest(manifest):
    print("2. manifest")
    known = set(build.ELEMENTS)
    shotlist = open(os.path.join(HERE, "gallery", "SHOTLIST.md"), encoding="utf-8").read()
    names = set()
    bad = []
    for card in manifest["cards"]:
        w, h = card["size"]
        names.add(card["out"])
        for el in card.get("elements", []):
            if el["type"] not in known:
                bad.append("%s: unknown element type %s" % (card["out"], el["type"]))
            if el["type"] in DRAWN:
                bad.append("%s: element type %s is drawn art, which the gallery does not have" % (card["out"], el["type"]))
            if el["type"] == "capture":
                cw, ch = common.picture_size(el)
                x, y = el["at"]
                if el.get("resample") == "area":
                    crop_w, crop_h = el["crop"][2], el["crop"][3]
                    if "size" not in el:
                        bad.append("%s: %s has resample area but no size" % (card["out"], el["capture"]))
                    elif cw > crop_w or ch > crop_h or abs(cw * crop_h - ch * crop_w) * 100 > crop_w * crop_h:
                        bad.append("%s: %s area size %dx%d must be a downscale of the %dx%d crop with the same shape"
                                   % (card["out"], el["capture"], cw, ch, crop_w, crop_h))
                elif el.get("resample") is not None or "size" in el:
                    bad.append("%s: %s has an unknown resample or a size without resample area" % (card["out"], el["capture"]))
                elif el["crop"][2] % el.get("down", 1) or el["crop"][3] % el.get("down", 1):
                    bad.append("%s: crop of %s is not divisible by down" % (card["out"], el["capture"]))
                if x < 0 or y < 0 or x + cw > w or y + ch > h:
                    bad.append("%s: picture %s at %d,%d size %dx%d leaves the card" % (card["out"], el["capture"], x, y, cw, ch))
                if el["capture"] not in shotlist:
                    bad.append("%s is not described in SHOTLIST.md" % el["capture"])
            if el["type"] == "studio":
                x, y = el["at"]
                sw, sh = el["size"]
                if x < 0 or y < 0 or x + sw > w or y + sh > h or not el.get("scene") or el.get("frame") in (None, ""):
                    bad.append("%s: studio element %s/%s is not inside the card or lacks scene / frame" % (card["out"], el.get("scene"), el.get("frame")))
                elif el["scene"] not in synth_studio.SHOTS or el["frame"] not in synth_studio.SHOTS[el["scene"]]:
                    bad.append("%s: studio shot %s/%s is not in the studio contract (art/studio/synth_studio.py SHOTS)" % (card["out"], el["scene"], el["frame"]))
                elif el["frame"] not in shotlist:
                    bad.append("%s: studio shot %s is not described in SHOTLIST.md" % (card["out"], el["frame"]))
                if el.get("label") and el.get("label_below"):
                    bad.append("%s: studio element %s has both label and label_below" % (card["out"], el.get("frame")))
    check(not bad, "%d cards: element types, picture boxes inside the cards, every capture in SHOTLIST.md%s" % (
        len(manifest["cards"]), "" if not bad else " -> " + "; ".join(bad)))
    check(len(names) == len(manifest["cards"]), "output names are unique")
    banner = [c for c in manifest["cards"] if c["out"] == "banner.png"]
    used = {(e.get("scene"), e.get("frame")) for c in banner for e in c["elements"] if e["type"] == "studio"}
    check(("stills_hero", "banner_hero") in used, "the banner is made of the studio still stills_hero / banner_hero")
    studio_cards = {"card_universal.png": ["universal_vanilla", "universal_modded"],
                    "card_glint_presets.png": [n for n in synth_studio.SHOTS["stills_tiles"] if n.startswith("glint_")],
                    "card_outline_styles.png": [n for n in synth_studio.SHOTS["stills_tiles"] if n.startswith("outline_")],
                    "card_gap_fix.png": synth_studio.SHOTS["stills_gap"]}
    for name, shots in sorted(studio_cards.items()):
        got = [e["frame"] for c in manifest["cards"] if c["out"] == name for e in c["elements"] if e["type"] == "studio"]
        check(got == shots, "%s shows the studio shots %s (%d)" % (name, shots[0] + " ... " + shots[-1] if len(shots) > 2 else ", ".join(shots), len(got)))
    lg = manifest.get("logo", {})
    check(lg and lg.get("scene") in synth_studio.SHOTS and lg.get("frame") in synth_studio.SHOTS.get(lg.get("scene"), []) and lg["frame"] in shotlist,
          "logo section: the studio still %s / %s is in the contract and described in SHOTLIST.md" % (lg.get("scene"), lg.get("frame")))
    check("gif" not in lg and "capture" not in lg, "logo section holds no capture and no GIF (logo_animated.gif is a studio GIF of the gifs list)")
    description = open(os.path.join(ROOT, "docs", "curseforge", "description.md"), encoding="utf-8").read()
    gifs_out = [g["out"] for g in manifest.get("gifs", [])]
    check(gifs_out == ["showcase.gif", "logo_animated.gif", "lineup.gif", "armor.gif"], "the GIFs are %s" % ", ".join(gifs_out))
    for g in manifest.get("gifs", []):
        kind, w0, h0 = synth_studio.CONTRACT.get(g.get("scene"), (None, 0, 0))
        check(kind == "turntable" and g["scene"] in shotlist and g.get("size") and g["size"][0] <= 1000 and g["size"][0] / float(g["size"][1]) == w0 / float(h0)
              and 0 < g.get("budget_mb", 8) <= 8,
              "GIF %s: turntable scene %s, picture %s (the scene's shape, at most 1000 px wide), budget %s MB" % (
                  g["out"], g.get("scene"), g.get("size"), g.get("budget_mb", 8)))
    for name in sorted({c["out"] for c in manifest["cards"]} | set(gifs_out)):
        check((">>> PASTE IMAGE: %s | " % name) in description, "docs/curseforge/description.md has the paste marker for %s" % name)


# ---------------------------------------------------------------------------------------- 2b. contrast

MIN_CONTRAST = 4.5      # WCAG AA for body text; the gallery does not use the 3:1 allowance for large text


def darkest_speck():
    """The darkest colour lib.card() dithers onto the panel, read from a real card (text can sit on it)."""
    c = lib.card(400, 240, seed=7)
    best = None
    for y in range(40, 200):
        for x in range(40, 360):
            px = c.get(x, y)
            if best is None or lib.luminance(px) < lib.luminance(best):
                best = px
    return best


def contrast_report(pairs):
    """(failing descriptions, lowest ratio, text of every pair) for [(name, text colour, background)]."""
    bad, low = [], 99.0
    for name, fg, bg in pairs:
        r = lib.contrast(fg, bg)
        low = min(low, r)
        if r < MIN_CONTRAST:
            bad.append("%s %.2f" % (name, r))
    return bad, low


def test_contrast(manifest):
    print("2b. contrast (WCAG, at least %.1f:1)" % MIN_CONTRAST)
    speck = darkest_speck()
    check(lib.luminance(speck) < lib.luminance(lib.PANEL), "darkest dither speck on a card is darker than the panel (%s)"
          % "#%02x%02x%02x" % speck[:3])

    # the table of art/README.md: one row per line
    rows = [
        ("TEXT on PANEL, speck, PANEL_DARK, PANEL_MID, PAGE", [
            ("TEXT on PANEL", lib.TEXT, lib.PANEL), ("TEXT on the darkest speck", lib.TEXT, speck),
            ("TEXT on PANEL_DARK", lib.TEXT, lib.PANEL_DARK), ("TEXT on PANEL_MID", lib.TEXT, lib.PANEL_MID),
            ("TEXT on PAGE", lib.TEXT, lib.PAGE)]),
        ("TEXT_DIM on PANEL, speck, PANEL_DARK", [
            ("TEXT_DIM on PANEL", lib.TEXT_DIM, lib.PANEL), ("TEXT_DIM on the darkest speck", lib.TEXT_DIM, speck),
            ("TEXT_DIM on PANEL_DARK", lib.TEXT_DIM, lib.PANEL_DARK)]),
        ("INK on BORDER (brass chip)", [("INK on BORDER", lib.INK, lib.BORDER)]),
        ("GOOD, BAD, GOLD_TEXT, ACCENT on PANEL", [
            ("GOOD on PANEL", lib.GOOD, lib.PANEL), ("BAD on PANEL", lib.BAD, lib.PANEL),
            ("GOLD_TEXT on PANEL", lib.GOLD_TEXT, lib.PANEL), ("ACCENT on PANEL", lib.ACCENT, lib.PANEL)]),
        ("TITLE on PANEL and on the darkest speck (card headings)", [
            ("TITLE on PANEL", lib.TITLE, lib.PANEL), ("TITLE on the darkest speck", lib.TITLE, speck)]),
        ("INK on the grey-beige capture-missing slab", [
            ("INK on MISSING_A", lib.INK, lib.MISSING_A), ("INK on MISSING_B", lib.INK, lib.MISSING_B)]),
    ]
    for title, pairs in rows:
        bad, low = contrast_report(pairs)
        check(not bad, "%s: lowest %.1f:1%s" % (title, low, "" if not bad else " -> below %.1f: %s" % (MIN_CONTRAST, "; ".join(bad))))

    # label styles: the text colour of every style sits on the cream label box (lib.PANEL)
    pairs = [("label style %s" % name, col, lib.PANEL) for name, (_tab, col) in sorted(build.LABEL_STYLES.items())]
    bad, low = contrast_report(pairs)
    check(not bad, "%d label styles (%s) on the label box PANEL: lowest %.1f:1%s" % (
        len(pairs), ", ".join(n for n in sorted(build.LABEL_STYLES)), low, "" if not bad else " -> below %.1f: %s" % (MIN_CONTRAST, "; ".join(bad))))

    # every named text colour on the card panel (and the darkest speck); the neutral ones also on the raised and recessed panels.
    # "white" is the light-on-dark colour of the old theme: it is not used on a card and is not a card text colour.
    neutral = ("text", "dim", "ink")
    pairs = []
    for name, col in sorted(lib.COLOURS.items()):
        if name == "white":
            continue
        pairs += [("%s on PANEL" % name, col, lib.PANEL), ("%s on the darkest speck" % name, col, speck)]
        if name in neutral:
            pairs += [("%s on PANEL_MID" % name, col, lib.PANEL_MID), ("%s on PANEL_DARK" % name, col, lib.PANEL_DARK)]
    bad, low = contrast_report(pairs)
    check(not bad, "%d lib.COLOURS text colours on PANEL / speck / PANEL_MID / PANEL_DARK: lowest %.1f:1%s" % (
        len(pairs), low, "" if not bad else " -> below %.1f: %s" % (MIN_CONTRAST, "; ".join(bad))))

    # what cards.json really asks for: text and bullets colours on the card (and on a panel element they sit in), the GIF caption bar on PAGE
    fills = {"dark": lib.PANEL_DARK, "mid": lib.PANEL_MID, "slate": lib.PANEL}
    pairs = []
    for card in manifest["cards"]:
        panels = [(e["at"], e["size"], fills[e.get("fill", "dark")]) for e in card.get("elements", []) if e["type"] == "panel"]
        for e in card.get("elements", []):
            if e["type"] not in ("text", "bullets"):
                continue
            col = lib.colour(e.get("color", "text"))
            x, y = e["at"]
            beds = [lib.PANEL, speck] + [f for (px, py), (pw, ph), f in panels if px <= x < px + pw and py <= y < py + ph]
            pairs += [("%s: %s text on %s" % (card["out"], e.get("color", "text"), "#%02x%02x%02x" % bg[:3]), col, bg) for bg in beds]
    for g in manifest.get("gifs", []):
        if g.get("caption", True):
            pairs.append(("%s caption bar" % g["out"], lib.colour(composite.style(g.get("style"))["bar"]["colour"]), lib.PAGE))
    bad, low = contrast_report(pairs)
    check(pairs and not bad, "%d text colour / background pairs the manifest uses: lowest %.1f:1%s" % (
        len(pairs), low, "" if not bad else " -> below %.1f: %s" % (MIN_CONTRAST, "; ".join(bad))))


# ---------------------------------------------------------------------------------------- 3 + 4. builds

def build_everything(manifest, captures_dir, with_gifs=True, workers=2):
    """Logo files + cards + GIFs as {relative output: bytes}, plus the per-image list of missing captures."""
    report = common.Report()
    caps = common.Captures(captures_dir)
    out = {}
    for rel in logo.build(out, manifest, caps):
        report.missing.append(("logo", rel))
    for name, data in build.build_all(manifest, caps, report).items():
        out["docs/curseforge/img/" + name] = data
    if with_gifs:
        for name, data in gifs.build_all(manifest, caps, report, workers=workers).items():
            out["docs/curseforge/img/" + name] = data
    return out, report


def quiet(fn, *a, **kw):
    """Run fn with the 'capture missing' warnings silenced (they are expected here)."""
    old = sys.stderr
    sys.stderr = open(os.devnull, "w")
    try:
        return fn(*a, **kw)
    finally:
        sys.stderr.close()
        sys.stderr = old


SYNTH_GIF_FRAMES = 6       # frames per synthetic turntable of the verify builds (hero_sword: one per look)
SYNTH = {}                 # filled by test_builds: the GIFs built from the synthetic scenes, for test_sizes


def test_builds(manifest):
    print("3. determinism")
    repo_caps = common.DEFAULT_CAPTURES
    a, rep = quiet(build_everything, manifest, repo_caps, FULL)
    b, _ = quiet(build_everything, manifest, repo_caps, FULL)
    check(a.keys() == b.keys() and all(a[k] == b[k] for k in a),
          "repository captures: %d files built twice, byte-identical%s" % (len(a), "" if FULL else " (GIFs only with --full)"))
    tmp = tempfile.mkdtemp(prefix="glintsmith-synth-")
    try:
        synth.make(tmp, manifest, frames=SYNTH_GIF_FRAMES, supersample=1)
        s1, srep = build_everything(manifest, tmp)
        s2, _ = build_everything(manifest, tmp)
        check(s1.keys() == s2.keys() and all(s1[k] == s2[k] for k in s1),
              "synthetic captures and studio scenes: %d files (logo, icon, cards, %d GIFs) built twice, byte-identical" % (
                  len(s1), len(manifest.get("gifs", []))))
        check(not srep, "synthetic captures cover every slot (no placeholder drawn)")
        check(all(s1[k] != a[k] for k in s1 if k.startswith("docs/curseforge/img/card_")) if rep else True,
              "cards from captures differ from the placeholder state (the slots really draw the captures)")
        SYNTH.update({k: v for k, v in s1.items() if k.endswith(".gif")})
        # the synthetic build also covers the studio scenes' contract
        scenes = studio_scene.scene_ids(tmp)
        check(scenes == sorted(synth_studio.CONTRACT), "synthetic studio scenes: %s" % ", ".join(scenes))
        bad = []
        for sid, (kind, w, h) in sorted(synth_studio.CONTRACT.items()):
            sc = studio_scene.load(tmp, sid)
            names = [f["name"] for f in sc.frames if f.get("name")]
            if (sc.width, sc.height) != (w, h) or studio_scene.validate(sc):
                bad.append("%s is %dx%d or breaks the frame contract" % (sid, sc.width, sc.height))
            if kind == "still" and names != synth_studio.SHOTS[sid]:
                bad.append("%s names %s" % (sid, names))
            if kind == "turntable" and names:
                bad.append("%s has named frames" % sid)
        check(not bad, "every synthetic scene has the contract's size, valid frame pairs and the shot names%s" % ("" if not bad else " -> " + "; ".join(bad)))
    finally:
        shutil.rmtree(tmp, ignore_errors=True)

    print("4. committed files equal a fresh build")
    for rel in (logo.P_LOGO, logo.P_ICON, logo.P_LOGO_SMALL):
        if rel in a:
            continue
        if os.path.exists(disk(rel)):
            print("  skip  %s: the studio render stills_hero / logo is not on this machine, so it cannot be rebuilt" % rel)
    if os.path.exists(disk(logo.P_ICON)) and logo.P_ICON not in a and sha(read(disk(logo.P_ICON))) in HAND_DRAWN:
        print("  WARN  %s is still the hand-drawn icon from a816b40: run py -B art/logo.py once art/captures/studio/stills_hero exists"
              % logo.P_ICON)
    missing_for = {}
    for image, rel in rep.missing:
        missing_for.setdefault(image, set()).add(rel)
    for rel in sorted(a):
        p = disk(rel)
        name = os.path.basename(rel)
        if not os.path.exists(p):
            check(False, "%s exists" % rel)
            continue
        same = sha(read(p)) == sha(a[rel])
        if same:
            check(True, "%s is byte-identical to a fresh build (%s)" % (rel, sha(a[rel])[:12]))
        elif name in missing_for and not rel.startswith("art/"):
            print("  skip  %s differs, but %d of its captures are not on this machine" % (rel, len(missing_for[name])))
        else:
            check(False, "%s is stale: rebuild with py -B art/logo.py and py -B art/gallery/build.py" % rel)
    if not FULL:
        print("  skip  the GIFs: rebuilding them from the real studio scenes takes minutes (py -B art/verify.py --full)")
    return a, rep


# ---------------------------------------------------------------------------------------- 5. sizes

def gif_expectation(g):
    """(width, height, bytes limit, caption?) of the GIF of a manifest entry at its full picture size."""
    w, h = g["size"]
    cap = g.get("caption", True)
    return w, h + (composite.DEFAULT_STYLE["bar"]["height"] if cap else 0), g.get("budget_mb", 8) * MB, cap


def check_gif(name, data, g, exact):
    """Properties of a built GIF: dimensions (exactly the manifest's, or at most that and at least 0.6 of it when the budget
    ladder may have shrunk it), the budget, an endless loop, one frame delay per frame of at least 2 cs."""
    w, h, limit, cap = gif_expectation(g)
    bh = composite.DEFAULT_STYLE["bar"]["height"] if cap else 0
    info = imgio.parse_gif(data)
    gw, gh = info["width"], info["height"]
    ok = (gw, gh) == (w, h) if exact else (0.6 * w - 1 <= gw <= w and 0.6 * g["size"][1] - 1 <= gh - bh <= g["size"][1])
    check(ok, "%s is %dx%d (manifest picture %dx%d%s)" % (name, gw, gh, g["size"][0], g["size"][1], " + caption bar" if cap else ""))
    check(len(data) <= limit, "%s is %.2f MB (budget %.0f MB)" % (name, len(data) / MB, limit / MB))
    check(info["loop"] == 0, "%s loops forever (NETSCAPE loop count %s)" % (name, info["loop"]))
    delays = [f["delay"] for f in info["frames"]]
    check(len(delays) >= 2 and min(delays) >= 2, "%s: %d frames, delays %s ... cs" % (name, len(delays), sorted(set(delays))[:3]))
    if cap:
        # the caption bar: its top line is the dark brass of lib.BORDER_DARK, across the whole width, under the picture
        f0 = info["frames"][0]
        pal = info["palette"]
        row = [tuple(pal[v][:3]) for v in f0["indices"][(gh - bh) * gw:(gh - bh) * gw + gw]] if (f0["w"], f0["h"]) == (gw, gh) else None
        check(row is None or all(c == tuple(lib.BORDER_DARK[:3]) for c in row), "%s has the caption bar under the picture" % name)
    return info


def test_sizes(manifest):
    print("5. sizes and GIF properties")
    for card in manifest["cards"]:
        p = disk("docs/curseforge/img/" + card["out"])
        if os.path.exists(p):
            size = imgio.read_png_size(p)
            studio = any(e["type"] == "studio" for e in card["elements"])
            if size != tuple(card["size"]) and studio and not common.studio_complete(common.DEFAULT_CAPTURES, card):
                print("  skip  %s is %dx%d, the manifest says %dx%d: its studio renders are not on this machine, the committed card is the old one"
                      % (card["out"], size[0], size[1], card["size"][0], card["size"][1]))
                continue
            check(size == tuple(card["size"]), "%s is %dx%d (manifest %dx%d)" % (card["out"], size[0], size[1], card["size"][0], card["size"][1]))
            check(size[0] <= 1000, "%s is at most 1000 px wide (CurseForge description column)" % card["out"])
    for rel, want in ((logo.P_LOGO, (512, 512)), (logo.P_ICON, (128, 128)), (logo.P_LOGO_SMALL, (32, 32))):
        if os.path.exists(disk(rel)):
            size = imgio.read_png_size(disk(rel))
            check(size == want, "%s is %dx%d" % (rel, want[0], want[1]) + ("" if size == want else " (found %dx%d)" % size))

    # the GIFs built from the synthetic scenes (small budgets are met at scale 1: the picture size is the manifest's)
    for g in manifest.get("gifs", []):
        data = SYNTH.get(("docs/curseforge/img/" + g["out"]))
        if data is None:
            check(False, "synthetic %s was built" % g["out"])
            continue
        check_gif("synthetic " + g["out"], data, g, True)
    # the GIFs on disk (after the real build): with --full only, the committed ones can still be the old showcase.gif
    for g in manifest.get("gifs", []):
        p = disk("docs/curseforge/img/" + g["out"])
        if not FULL:
            continue
        if not os.path.exists(p):
            check(False, "%s exists (py -B art/gallery/build.py)" % g["out"])
            continue
        check_gif(g["out"], read(p), g, False)
    if not FULL:
        print("  skip  the GIFs in docs/curseforge/img/: checked with --full once the real studio build has run")

    # GIF budget logic: a budget nothing fits under must end in an error, never a silent oversize file
    synth_dir = tempfile.mkdtemp(prefix="glintsmith-synth-")
    try:
        synth_studio.make_contract(synth_dir, 6, 1, 1, only=["lineup"])
        spec = dict([g for g in manifest["gifs"] if g["out"] == "lineup.gif"][0], budget_mb=0.001)
        try:
            quiet(gifs.build_gif, spec, common.Captures(synth_dir), common.Report(), 1)
            check(False, "an impossible GIF budget stops the build")
        except SystemExit as e:
            check("does not fit" in str(e), "an impossible GIF budget stops the GIF build with an error (and writes nothing)")
        spec = dict(spec, min_scale=1.0)
        try:
            quiet(gifs.build_gif, spec, common.Captures(synth_dir), common.Report(), 1)
            check(False, "min_scale 1 with an impossible budget stops the build")
        except SystemExit as e:
            check("scale 1.00" in str(e) and "0.60" not in str(e), "min_scale 1 never shrinks the picture: the error names scale 1.00 only")
        empty = tempfile.mkdtemp(prefix="glintsmith-empty-")
        try:
            rep = common.Report()
            data = quiet(gifs.build_gif, dict(spec, scene="nothere"), common.Captures(empty), rep, 1)
            info = imgio.parse_gif(data)
            check(bool(rep) and rep.missing[0][1] == "art/captures/studio/nothere/scene.json" and len(info["frames"]) == 1
                  and (info["width"], info["height"]) == (900, 336), "a GIF whose scene is missing: warning and a one frame grey placeholder, never a fake")
        finally:
            shutil.rmtree(empty, ignore_errors=True)
    finally:
        shutil.rmtree(synth_dir, ignore_errors=True)


# ---------------------------------------------------------------------------------------- 6. hygiene

def synthetic_logo_build(manifest, tmp):
    synth_studio.make_contract(tmp, 4, 1, 1, only=["stills_hero"])
    out = {}
    missing = logo.build(out, manifest, common.Captures(tmp))
    return out, missing


def test_area():
    print("6. area filter, logo and icon")
    c = lib.Canvas(4, 4)
    vals = [10, 20, 30, 40, 50, 60, 70, 80, 90, 100, 110, 120, 130, 140, 150, 160]
    for i, v in enumerate(vals):
        c.set(i % 4, i // 4, (v, 255 - v, v // 2, 255))
    a = c.area(2, 2)
    want = [sum(vals[y * 4 + x] for y in (r * 2, r * 2 + 1) for x in (k * 2, k * 2 + 1)) for r in (0, 1) for k in (0, 1)]
    got_r = [a.buf[i * 4] for i in range(4)]
    check(got_r == [(t + 2) // 4 for t in want] and (a.w, a.h) == (2, 2),
          "4x4 -> 2x2 area filter is the mean of each 2x2 block (red %s)" % got_r)
    check(bytes(a.buf) == bytes(c.down(2).buf), "for a whole factor the area filter equals the box downscale")
    row = lib.Canvas(3, 1)
    for x, v in enumerate((0, 90, 180)):
        row.set(x, 0, (v, v, v, 255))
    r = row.area(2, 1)
    check([r.buf[0], r.buf[4]] == [30, 150], "3 -> 2 weights the middle pixel by the fraction it covers (0,90,180 -> 30,150)")
    flat = lib.Canvas(7, 5, (12, 99, 201, 255))
    f = flat.area(3, 2)
    check(all(tuple(f.buf[i:i + 4]) == (12, 99, 201, 255) for i in range(0, len(f.buf), 4)), "a flat colour stays exactly the same at any ratio")
    chk = lib.Canvas(2, 1)
    chk.set(0, 0, (200, 0, 0, 255))
    chk.set(1, 0, (0, 0, 200, 0))
    t = chk.area(1, 1)
    check(tuple(t.buf[:4]) == (200, 0, 0, 128), "transparent pixels do not darken the colour (weighted by alpha)")
    big = lib.Canvas(1080, 1080)
    big.buf[:] = bytes([(i * 7 + i // 1000) % 251 for i in range(len(big.buf))])
    big.buf[3::4] = bytes([255]) * (1080 * 1080)
    r1, r2 = big.area(512, 512), big.area(512, 512)
    check((r1.w, r1.h) == (512, 512) and bytes(r1.buf) == bytes(r2.buf), "1080 -> 512 gives 512x512, deterministic")

    # the square cut
    class Fake:
        w, h = 1920, 1080
    check(logo.square(Fake, {"centre": ["c", "c"], "size": "full"}) == (420, 0, 1080), "default crop: centred square of the full height (420, 0, 1080)")
    check(logo.square(Fake, {"centre": [100, 900], "size": 800}) == (0, 280, 800), "a crop near an edge is moved inside the picture, not shrunk")
    check(logo.square(Fake, {"centre": [1000, 500], "size": 600}) == (700, 200, 600), "crop centre and size from the config")

    manifest = common.load_manifest()
    tmp = tempfile.mkdtemp(prefix="glintsmith-synth-")
    try:
        out, missing = synthetic_logo_build(manifest, tmp)
        check(not missing, "a synthetic studio still stills_hero / logo covers the logo and the icon")
        sizes = {rel: (lib.Canvas.from_png(out[rel]).w, lib.Canvas.from_png(out[rel]).h) for rel in (logo.P_LOGO, logo.P_ICON, logo.P_LOGO_SMALL)}
        check(sizes == {logo.P_LOGO: (512, 512), logo.P_ICON: (128, 128), logo.P_LOGO_SMALL: (32, 32)}, "logo 512, icon 128, small logo 32 px (square)")
        cfg = manifest["logo"]
        sc = studio_scene.load(tmp, cfg["scene"])
        m = pipeline.matte_frame(sc, studio_scene.find_frame(sc, cfg["frame"]))
        pic = composite.render(m, composite.style(cfg.get("style")), None, cfg.get("shadows", True))
        x, y, n = logo.square(pic, cfg.get("crop"))
        square = pic.crop(x, y, n, n)
        check(pic.opaque() and (pic.w, pic.h) == (1024, 1024), "the logo render is matted and set on the cream studio backdrop: opaque, 1024 x 1024")
        check(bytes(lib.Canvas.from_png(out[logo.P_ICON]).buf) == bytes(square.area(128, 128).buf),
              "icon.png is the area-filtered square crop of the logo picture, byte for byte")
        check(bytes(lib.Canvas.from_png(out[logo.P_LOGO]).buf) == bytes(square.area(512, 512).buf),
              "logo_512.png is the area-filtered square crop of the logo picture, byte for byte")
        corner = tuple(lib.Canvas.from_png(out[logo.P_LOGO]).get(3, 3))
        check(corner[3] == 255 and corner[0] > 225 and corner[0] >= corner[2] > 190, "the logo is on cream: the corner is %s" % ("#%02x%02x%02x" % corner[:3]))
    finally:
        shutil.rmtree(tmp, ignore_errors=True)

    # a missing render: message, nothing written, exit 0 (1 with --strict), the committed icon untouched
    empty, outdir = tempfile.mkdtemp(prefix="glintsmith-empty-"), tempfile.mkdtemp(prefix="glintsmith-out-")
    try:
        script = os.path.join(HERE, "logo.py")
        run = lambda *extra: subprocess.run([sys.executable, "-B", script, "--captures", empty, "--out", outdir] + list(extra),
                                            capture_output=True, text=True)
        r = run()
        check(r.returncode == 0 and "stills_hero" in r.stdout and "missing" in r.stdout,
              "logo.py without the studio still stills_hero prints which file is missing and exits 0")
        check(not [f for _, _, fs in os.walk(outdir) for f in fs], "...and writes nothing")
        r = run("--strict")
        check(r.returncode == 1 and "stills_hero" in r.stdout, "logo.py --strict without the render exits 1")
        icon_before = sha(read(disk(logo.P_ICON))) if os.path.exists(disk(logo.P_ICON)) else None
        r = subprocess.run([sys.executable, "-B", script, "--captures", empty], capture_output=True, text=True)
        icon_after = sha(read(disk(logo.P_ICON))) if os.path.exists(disk(logo.P_ICON)) else None
        check(r.returncode == 0 and icon_before == icon_after, "logo.py with the default output folder leaves the committed icon.png untouched")
    finally:
        shutil.rmtree(empty, ignore_errors=True)
        shutil.rmtree(outdir, ignore_errors=True)


# ---------------------------------------------------------------------------------------- 6b. studio

def kw_pair(pixels):
    """K (over black) and W (over white) RGBA buffers for [(alpha 0..1, (r, g, b))], one row, like the in-game studio makes them."""
    k, w = bytearray(), bytearray()
    for a, c in pixels:
        k += bytes(int(round(v * a)) for v in c) + b"\xff"
        w += bytes(min(255, int(round(v * a + 255 * (1 - a)))) for v in c) + b"\xff"
    return k, w


def test_matte():
    print("6b. studio: matte, composite, GIF encoder")
    cases = [(0.0, (10, 20, 30)), (1.0, (10, 20, 30)), (0.4, (200, 100, 50)), (0.75, (255, 128, 0)), (0.5, (33, 200, 99)),
             (0.9, (250, 250, 250)), (0.25, (120, 60, 240)), (0.05, (255, 0, 255))]
    k, w = kw_pair(cases)
    out = matte.matte_pair(k, w, len(cases), 1, 1)
    px = [tuple(out.buf[i * 4:i * 4 + 4]) for i in range(len(cases))]
    ok_alpha = all(abs(p[3] - round(255 * a)) <= 1 + (a < 0.1) for p, (a, _) in zip(px, cases))
    ok_pre = all(abs(p[i] * p[3] / 255.0 - c[i] * a) <= 1.5 for p, (a, c) in zip(px, cases) for i in range(3))
    ok_col = all(max(abs(p[i] - c[i]) for i in range(3)) <= 1 for p, (a, c) in zip(px, cases) if a >= 0.75)
    check(ok_alpha and ok_pre and ok_col,
          "matte round trip: alpha within 1/255, premultiplied colour within 1.5, straight colour within 1 where alpha >= 0.75")
    check(px[0] == (0, 0, 0, 0) and px[1] == (10, 20, 30, 255) and px[2] == (200, 100, 50, 102),
          "matte: empty is (0,0,0,0), W == K is opaque with colour K, a representable pair comes back exactly %s" % (px[:3],))
    # noise: W a little below K (a > 1) clamps to opaque; a dark opaque pixel (K == W) stays opaque
    noisy = matte.matte_pair(bytes((100, 100, 100, 255, 3, 2, 1, 255)), bytes((90, 100, 100, 255, 3, 2, 1, 255)), 2, 1, 1)
    check(tuple(noisy.buf[0:4]) == (100, 100, 100, 255) and tuple(noisy.buf[4:8]) == (3, 2, 1, 255),
          "matte: W below K (noise) clamps to opaque with colour K; W == K stays opaque")
    # supersampling averages in premultiplied space: 1 opaque pixel of a 2 x 2 block is a quarter alpha at the full colour
    k2, w2 = kw_pair([(1.0, (200, 40, 40)), (0.0, (0, 0, 0))])
    ke, we = kw_pair([(0.0, (0, 0, 0)), (0.0, (0, 0, 0))])
    s2 = matte.matte_pair(bytes(k2) + bytes(ke), bytes(w2) + bytes(we), 2, 2, 2)
    check((s2.w, s2.h) == (1, 1) and tuple(s2.buf[:4]) == (200, 40, 40, 64),
          "matte: S = 2 box filter in premultiplied space (1 opaque pixel of 4 -> alpha 64, colour unchanged) %s" % (tuple(s2.buf[:4]),))
    k3, w3 = kw_pair([(1.0, (200, 40, 40)), (0.5, (0, 0, 255))])
    s3 = matte.matte_pair(bytes(k3) + bytes(k3), bytes(w3) + bytes(w3), 2, 2, 2)
    check(tuple(s3.buf[3:4]) == (191,) and s3.buf[2] > 100 and s3.buf[0] > 100,
          "matte: S = 2 block of an opaque red and a half blue pixel mixes by alpha %s" % (tuple(s3.buf[:4]),))
    check(list(matte.box_sums(bytes(range(16)), 4, 4, 2)) == [0 + 1 + 4 + 5, 2 + 3 + 6 + 7, 8 + 9 + 12 + 13, 10 + 11 + 14 + 15],
          "matte.box_sums: 2 x 2 block sums of a 4 x 4 plane")


def run_cli(*args):
    return subprocess.run([sys.executable, "-B", os.path.join(HERE, "studio", "studio.py")] + list(args), capture_output=True, text=True)


def test_studio():
    tmp = tempfile.mkdtemp(prefix="glintsmith-studio-")
    try:
        # ---- a synthetic turntable, 12 frames of 96 x 96 at supersample 2 (never written below art/captures)
        synth_studio.make_scene(tmp, "turn", frames=12, width=96, height=96, supersample=2, fps=30)
        sc = studio_scene.load(tmp, "turn")
        probs = studio_scene.validate(sc)
        check(not probs and len(sc.frames) == 12, "synthetic scene: %d frames follow the frame contract (%s)" % (
            len(sc.frames), "; ".join(m for _, m in probs[:2]) or "no problems"))
        check(studio_scene.find_frame(sc, "hero")["index"] == 1 and studio_scene.find_frame(sc, 4)["index"] == 4
              and studio_scene.find_frame(sc, "0007")["index"] == 7, "frames are found by still name, index and file stem")

        # ---- stills: backdrop and transparent
        fr = studio_scene.find_frame(sc, "hero")
        a = pipeline.still(sc, fr, backdrop=True, caption=True)
        b = pipeline.still(sc, fr, backdrop=False)
        a2 = pipeline.still(sc, fr, backdrop=True, caption=True)
        check((a.w, a.h) == (96, 96 + 36) and a.opaque() and a.to_png() == a2.to_png(),
              "still: backdrop picture %dx%d (96x96 + caption bar), opaque, byte-identical on a rerun" % (a.w, a.h))
        alphas = b.buf[3::4]
        check((b.w, b.h) == (96, 96) and min(alphas) == 0 and max(alphas) == 255 and len([v for v in alphas if 0 < v < 255]) > 50,
              "still --transparent: RGBA with empty, opaque and semi-transparent (glow) pixels, no backdrop")
        bd = composite.backdrop(96, 96)
        corner, centre = tuple(bd[0:3]), tuple(bd[(40 * 96 + 48) * 3:(40 * 96 + 48) * 3 + 3])
        check(corner != (255, 255, 255) and lib.luminance(centre) > lib.luminance(corner) and lib.contrast(centre, corner) < 1.1,
              "backdrop: cream radial gradient, subtle (centre %s, corner %s, contrast %.3f)" % (
                  "#%02x%02x%02x" % centre, "#%02x%02x%02x" % corner, lib.contrast(centre, corner)))

        # ---- GIF: frames, size, delays, delta boxes, loop, budget, determinism, workers
        data, info = pipeline.build_gif(sc, caption=True, budget_mb=8, workers=1)
        gi = imgio.parse_gif(data)
        delays = [f["delay"] for f in gi["frames"]]
        check((gi["width"], gi["height"]) == (96, 96 + 36) and len(gi["frames"]) == 12 and info["fps"] == 30 and info["scale"] == 1.0,
              "GIF decodes: %dx%d, %d frames at the scene's 30 fps and scale 1.0" % (gi["width"], gi["height"], len(gi["frames"])))
        check(len(data) < 8 * MB and gi["loop"] == 0 and sum(delays) == 40 and sorted(set(delays)) == [3, 4] and delays[:3] == [3, 3, 4],
              "GIF: below 8 MB (%d bytes), loops forever, delays %s add up to the loop (40 cs), 30 fps = 3, 3, 4" % (len(data), delays[:6]))
        f0 = gi["frames"][0]
        same = [f for f, prev, cur in zip(gi["frames"][1:], sc.frames, sc.frames[1:]) if prev["caption"] == cur["caption"]]
        check((f0["x"], f0["y"], f0["w"], f0["h"]) == (0, 0, 96, 132) and all(f["trans"] == 255 and f["disposal"] == 1 for f in gi["frames"])
              and len(same) >= 6 and all(f["x"] > 0 and f["y"] + f["h"] <= 96 and f["w"] * f["h"] < 96 * 96 for f in same),
              "GIF: first frame is full, the others are delta boxes (transparent index 255, disposal 1) that leave the backdrop edge and the caption bar alone while the caption stays")
        check(len(gi["palette"]) == 256, "GIF: one global palette of 256 entries (255 colours + the transparent index)")
        data2, _ = pipeline.build_gif(sc, caption=True, budget_mb=8, workers=1)
        check(data == data2, "GIF: byte-identical on a rerun")
        data3, _ = pipeline.build_gif(sc, caption=True, budget_mb=8, workers=2)
        check(data == data3, "GIF: byte-identical with 2 worker processes")
        small, _ = pipeline.build_gif(sc, size=(64, 64), caption=False, workers=1)
        sgi = imgio.parse_gif(small)
        check((sgi["width"], sgi["height"], len(sgi["frames"])) == (64, 64, 12), "GIF --size 64x64 --no-caption: %dx%d, %d frames" % (
            sgi["width"], sgi["height"], len(sgi["frames"])))
        # delta frames: applying every frame (transparent index = keep) reproduces the index frames exactly
        fw, fh = 20, 10
        seq = []
        for t in range(6):
            fr_ = bytearray([5]) * (fw * fh)
            for y in range(3, 7):
                for x in range(2 + 3 * t, 6 + 3 * t):
                    fr_[y * fw + x] = 9 + t % 2
            seq.append(bytes(fr_))
        seq.append(seq[-1])                                            # an identical frame: a 1 x 1 transparent pixel
        gd = imgio.parse_gif(gifenc.encode_gif(fw, fh, [(i, i, i) for i in range(255)], seq, [3] * len(seq)))
        canvas = bytearray(fw * fh)
        same = True
        for want, f in zip(seq, gd["frames"]):
            for yy in range(f["h"]):
                for xx in range(f["w"]):
                    v = f["indices"][yy * f["w"] + xx]
                    if v != f["trans"]:
                        canvas[(f["y"] + yy) * fw + f["x"] + xx] = v
            same = same and bytes(canvas) == want
        check(same and (gd["frames"][-1]["w"], gd["frames"][-1]["h"]) == (1, 1) and all(f["w"] * f["h"] < fw * fh for f in gd["frames"][1:]),
              "delta frames: the GIF's frames applied in order give back every index frame; an unchanged frame is 1 x 1")
        # the dither is anchored to the picture, so a flat subject colour gives the same pattern every frame
        pal = gifenc.Palette([(i * 4 % 256, i * 7 % 256, i * 11 % 256) for i in range(255)])
        flat = bytes([200, 150, 120]) * (16 * 16)
        base = bytes([250, 245, 235]) * (16 * 16)
        idx = gifenc.Indexer(pal, 16, 16, base, step=12, snap=0)
        i1, i2 = idx.index(flat), idx.index(flat)
        check(i1 == i2 and 1 < len(set(i1)) <= 8, "ordered dither: a flat colour dithers to the same pattern of a few palette colours every frame (%d)" % len(set(i1)))
        check(len(set(idx.index(base))) == 1, "the unchanged backdrop is not dithered (one flat index)")
        d540 = gifenc.delays_cs(540, 30, 540)
        d432 = gifenc.delays_cs(432, 30, 540)
        check(d540[:6] == [3, 3, 4, 3, 3, 4] and sum(d540) == 1800 and sum(d432) == 1800 and min(d432) >= 2,
              "delays: 540 frames at 30 fps = 3,3,4 ... 1800 cs; 432 kept frames still add up to 1800 cs")
        check(len(pipeline.kept_frames(540, 30, 24)) == 432 and pipeline.kept_frames(540, 30, 15)[:3] == [0, 2, 4]
              and pipeline.fps_ladder(30) == [30, 24, 20, 15] and pipeline.fps_ladder(30, 20) == [20, 15],
              "budget ladder: 30 -> 24 -> 20 -> 15 fps, frames dropped evenly (432 of 540 at 24 fps)")

        # ---- the budget: a smaller limit steps down and says so, an impossible one stops with an error
        bud = int(len(data) * 0.8)
        d2, i2 = pipeline.build_gif(sc, caption=True, budget_mb=bud / MB, workers=1)
        check(len(d2) <= bud and len(i2["tried"]) > 1,
              "budget: a limit of 80%% of the size steps down to %g fps / scale %.2f (%d bytes) and reports it" % (i2["fps"], i2["scale"], len(d2)))
        try:
            quiet(pipeline.build_gif, sc, None, True, 0.0005, None, 1)
            check(False, "an impossible GIF budget stops the build")
        except SystemExit as e:
            check("does not fit" in str(e), "an impossible GIF budget stops with a clear message and no file")

        # ---- the card element
        spec = {"out": "t.png", "size": [200, 160], "elements": [
            {"type": "studio", "scene": "turn", "frame": "hero", "at": [10, 10], "size": [90, 90], "shadow": True},
            {"type": "studio", "scene": "turn", "frame": "back", "at": [105, 10], "size": [90, 90], "shadow": False},
            {"type": "studio", "scene": "nothere", "frame": "hero", "at": [10, 110], "size": [60, 40]}]}
        report = common.Report()
        card = quiet(build.build_card, spec, common.Captures(tmp), report)
        empty = quiet(build.build_card, {"out": "t.png", "size": [200, 160], "elements": []}, common.Captures(tmp), common.Report())
        again = quiet(build.build_card, spec, common.Captures(tmp), common.Report())
        check(bytes(card.buf) != bytes(empty.buf) and bytes(card.buf) == bytes(again.buf),
              "card element 'studio': draws the matted render on the card, byte-identical on a rerun")
        check(tuple(card.get(10, 10)) == tuple(empty.get(10, 10)) and tuple(card.get(105, 10)) == tuple(empty.get(105, 10)),
              "card element: no frame or box (the corners of the rect keep the card background)")
        check(bool(report) and [m[1] for m in report.missing] == ["art/captures/studio/nothere/scene.json"],
              "card element with a missing scene: warning for studio/nothere/scene.json and a placeholder")
        check([tuple(card.get(x, 99)) for x in range(20, 90)] != [tuple(empty.get(x, 99)) for x in range(20, 90)],
              "card element: shadow true lays a contact shadow under the render")

        # ---- the card element's crop, trim and label_below
        ctx = build.Ctx("t.png", common.Captures(tmp), common.Report())
        mt = build.studio_matte(ctx, "turn", "hero")[0]
        cr = build.shape_matte(mt, {"crop": [10, 20, 50, 40]})
        check((cr.w, cr.h) == (50, 40) and bytes(cr.buf) == bytes(mt.crop(10, 20, 50, 40).buf), "studio element crop: [x, y, w, h] of the matte")
        box = composite.alpha_bbox(bytes(mt.buf[3::4]), mt.w, mt.h, 8)
        ty = build.shape_matte(mt, {"trim": "y", "trim_margin": 2})
        txy = build.shape_matte(mt, {"trim": "xy", "trim_margin": 0})
        check(ty.w == mt.w and ty.h == min(mt.h, box[3] + 2) - max(0, box[1] - 2) and ty.h < mt.h and (txy.w, txy.h) == (box[2] - box[0], box[3] - box[1]),
              "studio element trim: 'y' keeps the width and cuts the empty rows (margin 2), 'xy' is the subject's box (%dx%d)" % (txy.w, txy.h))
        plain = {"type": "studio", "scene": "turn", "frame": "hero", "at": [10, 10], "size": [80, 60], "shadow": False}
        spec2 = {"out": "t.png", "size": [100, 120], "elements": [plain]}
        spec3 = {"out": "t.png", "size": [100, 120], "elements": [dict(plain, label_below="Name")]}
        c2, c3 = (quiet(build.build_card, sp, common.Captures(tmp), common.Report()) for sp in (spec2, spec3))
        under = [(x, y) for y in range(72, 100) for x in range(10, 90) if tuple(c2.get(x, y)) != tuple(c3.get(x, y))]
        inside = [(x, y) for y in range(10, 70) for x in range(10, 90) if tuple(c2.get(x, y)) != tuple(c3.get(x, y))]
        check(under and not inside and abs((min(x for x, _ in under) + max(x for x, _ in under)) / 2.0 - 50) <= 2 and min(y for _, y in under) >= 74,
              "studio element label_below: a name box centred under the rectangle, nothing drawn inside it")

        # ---- the synthetic scenes' command line (the old `--size 256` without an x crashed with a traceback)
        synth_cli = lambda *a: subprocess.run([sys.executable, "-B", os.path.join(HERE, "studio", "synth_studio.py"), os.path.join(tmp, "cli")] + list(a),
                                              capture_output=True, text=True)
        r = synth_cli("--size", "40", "--frames", "2", "--supersample", "1")
        sc2 = studio_scene.load(os.path.join(tmp, "cli"), "synth_turntable")
        check(r.returncode == 0 and (sc2.width, sc2.height) == (40, 40) and "Traceback" not in r.stderr, "synth_studio.py --size 40: a square scene")
        r = synth_cli("--scene", "wide", "--size", "48x32", "--frames", "1", "--supersample", "1")
        sc2 = studio_scene.load(os.path.join(tmp, "cli"), "wide")
        check(r.returncode == 0 and (sc2.width, sc2.height) == (48, 32), "synth_studio.py --size 48x32: a rectangle")
        for bad_size in ("abc", "4", "10x", "0x20"):
            r = synth_cli("--size", bad_size)
            check(r.returncode == 2 and "Traceback" not in r.stderr and "usage" in r.stderr, "synth_studio.py --size %s: a clean usage error, no traceback" % bad_size)

        # ---- the CLI
        out = os.path.join(tmp, "out")
        r = run_cli("--captures", tmp, "check")
        check(r.returncode == 0 and "turn" in r.stdout and "ok" in r.stdout, "studio.py check lists and validates the synthetic scene (exit 0)")
        r = run_cli("--captures", tmp, "gif", "turn", "--out", os.path.join(out, "t.gif"), "--workers", "1")
        check(r.returncode == 0 and os.path.isfile(os.path.join(out, "t.gif")) and read(os.path.join(out, "t.gif")) == data
              and "wrote" in r.stdout, "studio.py gif writes the same bytes as the library call (exit 0)")
        r = run_cli("--captures", tmp, "still", "turn", "back", "--out", os.path.join(out, "s.png"))
        r2 = run_cli("--captures", tmp, "still", "turn", "2", "--out", os.path.join(out, "s2.png"), "--transparent")
        check(r.returncode == 0 and r2.returncode == 0 and lib.Canvas.open(os.path.join(out, "s.png")).opaque()
              and not lib.Canvas.open(os.path.join(out, "s2.png")).opaque(), "studio.py still writes a backdrop picture and a transparent one")
        nothing = os.path.join(tmp, "empty")
        os.makedirs(nothing)
        for cmd in (["gif", "turn", "--out", os.path.join(out, "n.gif")], ["still", "turn", "hero", "--out", os.path.join(out, "n.png")]):
            r = run_cli("--captures", nothing, *cmd)
            check(r.returncode == 0 and "missing" in r.stdout and not os.path.exists(cmd[-1]),
                  "studio.py %s without the scene: says what is missing, writes nothing, exit 0" % cmd[0])
            r = run_cli("--captures", nothing, *(cmd + ["--strict"]))
            check(r.returncode == 1 and not os.path.exists(cmd[-1]), "studio.py %s ... --strict without the scene: exit 1" % cmd[0])
        check(run_cli("--captures", nothing, "check").returncode == 0 and run_cli("--captures", nothing, "check", "--strict").returncode == 1,
              "studio.py check with no scenes: exit 0, with --strict exit 1")
        # an incomplete scene (a W file gone) is 'missing'; a wrong size or missing field is an error
        part = os.path.join(tmp, "part")
        synth_studio.make_scene(part, "turn", frames=3, width=32, height=32, supersample=2)
        os.remove(os.path.join(part, "studio", "turn", "0002_w.png"))
        r = run_cli("--captures", part, "gif", "turn", "--out", os.path.join(out, "p.gif"))
        check(r.returncode == 0 and "0002_w.png" in r.stdout and not os.path.exists(os.path.join(out, "p.gif")),
              "an incomplete scene: names the missing 0002_w.png, writes nothing, exit 0")
        r = run_cli("--captures", part, "check", "--strict")
        check(r.returncode == 1 and "0002_w.png" in r.stdout, "studio.py check --strict names the missing pair and exits 1")
        bad = os.path.join(tmp, "bad")
        synth_studio.make_scene(bad, "turn", frames=2, width=32, height=32, supersample=2)
        with open(os.path.join(bad, "studio", "turn", "0001_k.png"), "wb") as f:
            f.write(imgio.png_bytes(10, 10, bytes(300), 3))
        r = run_cli("--captures", bad, "check")
        check(r.returncode == 1 and "0001_k.png" in r.stdout and "expected 64x64" in r.stdout,
              "studio.py check finds a render whose size does not match width x supersample (exit 1)")
        with open(os.path.join(bad, "studio", "turn", "scene.json"), "w", encoding="utf-8") as f:
            f.write('{"scene": "turn", "width": 32}')
        r = run_cli("--captures", bad, "check")
        check(r.returncode == 1 and "lacks the fields" in r.stdout, "studio.py check reports scene.json fields that are missing")
    finally:
        shutil.rmtree(tmp, ignore_errors=True)


def test_hygiene():
    print("7. standard library only, no bytecode")
    stdlib = set(sys.stdlib_module_names)
    local = set()
    pyfiles = []
    for folder, dirs, files in os.walk(HERE):
        dirs[:] = [d for d in dirs if d != "captures"]
        for f in files:
            if f.endswith(".py"):
                pyfiles.append(os.path.join(folder, f))
                local.add(os.path.splitext(f)[0])
    for path in sorted(pyfiles):
        tree = ast.parse(open(path, encoding="utf-8").read())
        mods = set()
        for node in ast.walk(tree):
            if isinstance(node, ast.Import):
                mods |= {a.name.split(".")[0] for a in node.names}
            elif isinstance(node, ast.ImportFrom) and node.module and node.level == 0:
                mods.add(node.module.split(".")[0])
        bad = sorted(m for m in mods if m not in stdlib and m not in local)
        check(not bad, "%s imports only the standard library and sibling modules%s" % (
            os.path.relpath(path, ROOT).replace("\\", "/"), "" if not bad else " (found %s)" % bad))
    caches = [os.path.join(f, d) for f, ds, _ in os.walk(HERE) for d in ds if d == "__pycache__"]
    check(not caches, "no __pycache__ folder below art/")
    stray = []
    for folder, _, names in os.walk(os.path.join(HERE, "captures")):
        for n in names:
            if n.lower().endswith(".png") and synth.MARK.encode() in read(os.path.join(folder, n)):
                stray.append(n)
    check(not stray, "art/captures holds no synthetic test capture%s" % ("" if not stray else " (found %s)" % stray[:3]))


def main(argv=None):
    global FULL
    ap = argparse.ArgumentParser(description=__doc__.split("\n\n")[0])
    ap.add_argument("--full", action="store_true", help="also rebuild the GIFs from the real studio scenes and check the GIFs on disk")
    FULL = ap.parse_args(argv).full
    manifest = common.load_manifest()
    test_reader()
    test_manifest(manifest)
    test_contrast(manifest)
    test_builds(manifest)
    test_sizes(manifest)
    test_area()
    test_matte()
    test_studio()
    test_hygiene()
    print()
    if failures:
        print("%d check(s) FAILED" % len(failures))
        return 1
    print("all checks passed")
    return 0


if __name__ == "__main__":
    sys.exit(main())

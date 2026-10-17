#!/usr/bin/env python3
"""Verify the Glintsmith art.  Standard library only.

    py -B art/verify.py

1. PNG reader: every filter type and colour layout the game or the vanilla textures use decodes back exactly.
2. Manifest: known element types (no drawn ones), positive crops, every picture inside its card, area-filter options,
   every capture is in SHOTLIST.md.
2b. Contrast: every text colour the build sets on a background it uses (the art/README.md table, the label styles, the
   named text colours, the colours the manifest asks for) has a WCAG contrast ratio of at least 4.5:1.
3. Determinism: the logo files, the cards and the GIF are built twice (once from the captures in the repository,
   once from synthetic test captures) and must be byte-identical both times.
4. Committed files: every image in docs/curseforge/img/ and the logo files equal a fresh build (an image whose
   captures are not present on this machine is skipped, not failed).
5. Sizes and GIF properties: card and banner sizes from the manifest, GIFs below their size limits, endless loop.
6. Area-filter downscale (exact averages) and the logo / icon / animated logo from a synthetic capture, including what
   logo.py does when the capture is missing (a message, nothing written, exit 0, or 1 with --strict).
6b. Studio (art/studio/): matte round trip of a black / white pair (alpha and colour within 1, exact empty / opaque pixels,
   premultiplied supersampling), a synthetic turntable end to end (check, GIF with the expected frames, size, delays, delta
   boxes, loop, budget and the step-down, identical bytes for any number of workers), stills, the "studio" card element
   and its missing-scene behaviour, the CLI exit codes.
7. Standard library only for every script under art/, and no __pycache__ anywhere below art/.

Exit code 0 when everything passes, 1 otherwise.
"""
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
import gif       # noqa: E402
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
    check(not bad, "%d cards: element types, picture boxes inside the cards, every capture in SHOTLIST.md%s" % (
        len(manifest["cards"]), "" if not bad else " -> " + "; ".join(bad)))
    check(len(names) == len(manifest["cards"]), "output names are unique")
    banner = [c for c in manifest["cards"] if c["out"] == "banner.png"]
    used = {e.get("capture") for c in banner for e in c["elements"]}
    check({"hero_lineup.png"} <= used and used & {"logo_sword.png", "hero_sword_rainbow.png"},
          "the banner is made of the hero shots (a sword render and hero_lineup)")
    lg = manifest.get("logo", {})
    need = [lg.get("capture")] + lg.get("gif", {}).get("captures", [])
    check(lg and all(n and n in shotlist for n in need), "logo section: %s are described in SHOTLIST.md" % ", ".join(map(str, need)))
    check(len(lg.get("gif", {}).get("captures", [])) >= 2, "logo_animated.gif cross-fades at least two hero shots")
    for g in manifest.get("gifs", []):
        check(g["frames_dir"] in shotlist and "0001" in shotlist, "GIF frames %s/NNNN.png described in SHOTLIST.md" % g["frames_dir"])


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
        if g.get("caption_bar"):
            pairs.append(("%s caption bar" % g["out"], lib.colour(g["caption_bar"].get("color", "text")), lib.PAGE))
    bad, low = contrast_report(pairs)
    check(pairs and not bad, "%d text colour / background pairs the manifest uses: lowest %.1f:1%s" % (
        len(pairs), low, "" if not bad else " -> below %.1f: %s" % (MIN_CONTRAST, "; ".join(bad))))


# ---------------------------------------------------------------------------------------- 3 + 4. builds

def build_everything(manifest, captures_dir, gif_frames=None):
    """Logo files + cards + GIFs as {relative output: bytes}, plus the per-image list of missing captures."""
    report = common.Report()
    caps = common.Captures(captures_dir)
    out = {}
    for rel in logo.build(out, manifest, caps):
        report.missing.append(("logo", rel))
    for name, data in build.build_all(manifest, caps, report).items():
        out["docs/curseforge/img/" + name] = data
    for name, data in gif.build_all(manifest, caps, report).items():
        out["docs/curseforge/img/" + name] = data
    return out, report


def quiet(fn, *a):
    """Run fn with the 'capture missing' warnings silenced (they are expected here)."""
    old = sys.stderr
    sys.stderr = open(os.devnull, "w")
    try:
        return fn(*a)
    finally:
        sys.stderr.close()
        sys.stderr = old


def test_builds(manifest):
    print("3. determinism")
    repo_caps = common.DEFAULT_CAPTURES
    a, rep = quiet(build_everything, manifest, repo_caps)
    b, _ = quiet(build_everything, manifest, repo_caps)
    check(a.keys() == b.keys() and all(a[k] == b[k] for k in a),
          "repository captures: %d files built twice, byte-identical" % len(a))
    tmp = tempfile.mkdtemp(prefix="glintsmith-synth-")
    try:
        synth.make(tmp, manifest, frames=40)
        s1, srep = build_everything(manifest, tmp)
        s2, _ = build_everything(manifest, tmp)
        check(s1.keys() == s2.keys() and all(s1[k] == s2[k] for k in s1),
              "synthetic captures: %d files built twice, byte-identical" % len(s1))
        check(not srep, "synthetic captures cover every slot (no placeholder drawn)")
        check(all(s1[k] != a[k] for k in s1 if k.startswith("docs/curseforge/img/card_")) if rep else True,
              "cards from captures differ from the placeholder state (the slots really draw the captures)")
    finally:
        shutil.rmtree(tmp, ignore_errors=True)

    print("4. committed files equal a fresh build")
    for rel in (logo.P_LOGO, logo.P_ICON, logo.P_LOGO_SMALL):
        if rel in a:
            continue
        if os.path.exists(disk(rel)):
            print("  skip  %s: logo_sword.png is not on this machine, so it cannot be rebuilt" % rel)
    if os.path.exists(disk(logo.P_ICON)) and logo.P_ICON not in a and sha(read(disk(logo.P_ICON))) in HAND_DRAWN:
        print("  WARN  %s is still the hand-drawn icon from a816b40: run py -B art/logo.py once art/captures/logo_sword.png exists"
              % logo.P_ICON)
    gifp = disk(logo.P_LOGO_GIF)
    if os.path.exists(gifp) and logo.P_LOGO_GIF not in a and sha(read(gifp)) in HAND_DRAWN:
        print("  WARN  %s on this machine is the old hand-drawn logo (untracked build output): delete it or rebuild it from the hero shots"
              % logo.P_LOGO_GIF)
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
    return a, rep


# ---------------------------------------------------------------------------------------- 5. sizes

def test_sizes(manifest):
    print("5. sizes and GIF properties")
    for card in manifest["cards"]:
        p = disk("docs/curseforge/img/" + card["out"])
        if os.path.exists(p):
            size = imgio.read_png_size(p)
            check(size == tuple(card["size"]), "%s is %dx%d (manifest %dx%d)" % (card["out"], size[0], size[1], card["size"][0], card["size"][1]))
            check(size[0] <= 1000, "%s is at most 1000 px wide (CurseForge description column)" % card["out"])
    for rel, want in ((logo.P_LOGO, (512, 512)), (logo.P_ICON, (128, 128)), (logo.P_LOGO_SMALL, (32, 32))):
        if os.path.exists(disk(rel)):
            size = imgio.read_png_size(disk(rel))
            check(size == want, "%s is %dx%d" % (rel, want[0], want[1]) + ("" if size == want else " (found %dx%d)" % size))

    limits = {}
    for g in manifest.get("gifs", []):
        w, h = common.out_size(g["crop"], g.get("down", 1), g.get("scale", 1))
        h += g["caption_bar"]["height"] if g.get("caption_bar") else 0
        limits["docs/curseforge/img/" + g["out"]] = (w, h, g.get("max_mb", 8) * MB, None)
    for rel, (w, h, limit, delay) in limits.items():
        p = disk(rel)
        if not os.path.exists(p):
            check(False, "%s exists" % rel)
            continue
        data = read(p)
        info = imgio.parse_gif(data)
        check((info["width"], info["height"]) == (w, h), "%s is %dx%d" % (rel, info["width"], info["height"]))
        check(len(data) < limit, "%s is %.2f MB (limit %.0f MB)" % (rel, len(data) / MB, limit / MB))
        check(info["loop"] == 0, "%s loops forever (NETSCAPE loop count %s)" % (rel, info["loop"]))
        delays = {f["delay"] for f in info["frames"]}
        if delay:
            check(delays == {delay} and len(info["frames"]) == 40, "%s: %d frames at %d cs = 20 fps" % (rel, len(info["frames"]), delay))
        else:
            check(len(delays) == 1 and min(delays) >= 2 and len(info["frames"]) >= 1, "%s: %d frame(s), delay %s cs" % (rel, len(info["frames"]), sorted(delays)))
    # GIF size limit logic: a limit nothing fits under must end in an error, never a silent oversize file
    synth_dir = tempfile.mkdtemp(prefix="glintsmith-synth-")
    try:
        if manifest.get("gifs"):
            synth.make(synth_dir, manifest, frames=12)
            spec = dict(manifest["gifs"][0], max_mb=0.001, max_step=2)
            try:
                quiet(gif.build_gif, spec, common.Captures(synth_dir), common.Report())
                check(False, "an impossible size limit stops the GIF build")
            except SystemExit:
                check(True, "an impossible size limit stops the GIF build with an error")
            spec = dict(manifest["gifs"][0], captions=["a", "b"], frames_per_caption=5)
            check([gif.caption_for(spec, n) for n in (1, 5, 6, 10, 11)] == ["a", "a", "b", "b", "b"], "GIF captions follow frames_per_caption")
    finally:
        shutil.rmtree(synth_dir, ignore_errors=True)


# ---------------------------------------------------------------------------------------- 6. hygiene

def synthetic_logo_build(manifest, tmp):
    synth.make(tmp, manifest, frames=2)
    out = {}
    missing = logo.build(out, manifest, common.Captures(tmp))
    return out, missing


def test_area():
    print("6. area filter, logo, icon and animated logo")
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
        check(not missing, "synthetic captures cover the logo and the three hero shots")
        sizes = {rel: (lib.Canvas.from_png(out[rel]).w, lib.Canvas.from_png(out[rel]).h) for rel in (logo.P_LOGO, logo.P_ICON, logo.P_LOGO_SMALL)}
        check(sizes == {logo.P_LOGO: (512, 512), logo.P_ICON: (128, 128), logo.P_LOGO_SMALL: (32, 32)}, "logo 512, icon 128, small logo 32 px (square)")
        src = lib.Canvas.open(os.path.join(tmp, manifest["logo"]["capture"]))
        x, y, n = logo.square(src, manifest["logo"].get("crop"))
        square = src.crop(x, y, n, n)
        check(bytes(lib.Canvas.from_png(out[logo.P_ICON]).buf) == bytes(square.area(128, 128).buf),
              "icon.png is the area-filtered square crop of the capture, byte for byte")
        check(bytes(lib.Canvas.from_png(out[logo.P_LOGO]).buf) == bytes(square.area(512, 512).buf),
              "logo_512.png is the area-filtered square crop of the capture, byte for byte")
        info = imgio.parse_gif(out[logo.P_LOGO_GIF])
        g = manifest["logo"]["gif"]
        want_frames = len(g["captures"]) * (g["hold"] + g["fade"])
        check((info["width"], info["height"]) == (g["size"], g["size"]) and len(info["frames"]) == want_frames,
              "logo_animated.gif is %dx%d with %d frames (%d looks, hold %d, fade %d)" % (
                  info["width"], info["height"], len(info["frames"]), len(g["captures"]), g["hold"], g["fade"]))
        check(info["loop"] == 0 and {f["delay"] for f in info["frames"]} == {g["delay_cs"]} and len(out[logo.P_LOGO_GIF]) < MB,
              "logo_animated.gif loops forever at a constant delay and is below 1 MB (%d bytes)" % len(out[logo.P_LOGO_GIF]))
    finally:
        shutil.rmtree(tmp, ignore_errors=True)

    looks = [bytes([v]) * 3 for v in (0, 100, 200)]
    frames = logo.gif_frames(looks, 4, 4)
    steps = [abs(frames[(i + 1) % len(frames)][0] - frames[i][0]) for i in range(len(frames))]
    check(len(frames) == 24 and frames[:4] == [looks[0]] * 4 and max(steps) <= 40,
          "cross-fade: 3 looks x (4 held + 4 fading) frames, the last look fades back into the first (largest step %d)" % max(steps))

    # a missing capture: message, nothing written, exit 0 (1 with --strict), the committed icon untouched
    empty, outdir = tempfile.mkdtemp(prefix="glintsmith-empty-"), tempfile.mkdtemp(prefix="glintsmith-out-")
    try:
        script = os.path.join(HERE, "logo.py")
        run = lambda *extra: subprocess.run([sys.executable, "-B", script, "--captures", empty, "--out", outdir] + list(extra),
                                            capture_output=True, text=True)
        r = run()
        check(r.returncode == 0 and "logo_sword.png" in r.stdout and "missing" in r.stdout,
              "logo.py without logo_sword.png prints which capture is missing and exits 0")
        check(not [f for _, _, fs in os.walk(outdir) for f in fs], "...and writes nothing")
        r = run("--strict")
        check(r.returncode == 1 and "logo_sword.png" in r.stdout, "logo.py --strict without the capture exits 1")
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


def main():
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

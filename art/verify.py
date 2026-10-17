#!/usr/bin/env python3
"""Verify the Glintsmith art.  Standard library only.

    py -B art/verify.py

1. PNG reader: every filter type and colour layout the game or the vanilla textures use decodes back exactly.
2. Manifest: known element types (no drawn ones), positive crops, every picture inside its card, area-filter options,
   every capture is in SHOTLIST.md.
3. Determinism: the logo files, the cards and the GIF are built twice (once from the captures in the repository,
   once from synthetic test captures) and must be byte-identical both times.
4. Committed files: every image in docs/curseforge/img/ and the logo files equal a fresh build (an image whose
   captures are not present on this machine is skipped, not failed).
5. Sizes and GIF properties: card and banner sizes from the manifest, GIFs below their size limits, endless loop.
6. Area-filter downscale (exact averages) and the logo / icon / animated logo from a synthetic capture, including what
   logo.py does when the capture is missing (a message, nothing written, exit 0, or 1 with --strict).
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

import build     # noqa: E402
import common    # noqa: E402
import gif       # noqa: E402
import imgio     # noqa: E402
import lib       # noqa: E402
import logo      # noqa: E402
import synth     # noqa: E402

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
    test_builds(manifest)
    test_sizes(manifest)
    test_area()
    test_hygiene()
    print()
    if failures:
        print("%d check(s) FAILED" % len(failures))
        return 1
    print("all checks passed")
    return 0


if __name__ == "__main__":
    sys.exit(main())

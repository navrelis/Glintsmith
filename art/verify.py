#!/usr/bin/env python3
"""Verify the Glintsmith art.  Standard library only.

    py -B art/verify.py

1. PNG reader: every filter type and colour layout the game or the vanilla textures use decodes back exactly.
2. Manifest: known element types, positive crops, every picture inside its card, every capture is in SHOTLIST.md.
3. Determinism: the logo files, the cards and the GIF are built twice (once from the captures in the repository,
   once from synthetic test captures) and must be byte-identical both times.
4. Committed files: every image in docs/curseforge/img/ and the logo files equal a fresh build (an image whose
   captures are not present on this machine is skipped, not failed).
5. Sizes and GIF properties: card and banner sizes from the manifest, logo 32 / 512 / icon, GIFs below their size
   limits, 20 fps logo, endless loop, one global palette.
6. Standard library only for every script under art/, and no __pycache__ anywhere below art/.

Exit code 0 when everything passes, 1 otherwise.
"""
import ast
import hashlib
import os
import shutil
import struct
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
            if el["type"] == "capture":
                cw, ch = common.out_size(el["crop"], el.get("down", 1), el.get("scale", 1))
                x, y = el["at"]
                if el["crop"][2] % el.get("down", 1) or el["crop"][3] % el.get("down", 1):
                    bad.append("%s: crop of %s is not divisible by down" % (card["out"], el["capture"]))
                if x < 0 or y < 0 or x + cw > w or y + ch > h:
                    bad.append("%s: picture %s at %d,%d size %dx%d leaves the card" % (card["out"], el["capture"], x, y, cw, ch))
                if el["capture"] not in shotlist:
                    bad.append("%s is not described in SHOTLIST.md" % el["capture"])
    check(not bad, "%d cards: element types, picture boxes inside the cards, every capture in SHOTLIST.md%s" % (
        len(manifest["cards"]), "" if not bad else " -> " + "; ".join(bad)))
    check(len(names) == len(manifest["cards"]), "output names are unique")
    for g in manifest.get("gifs", []):
        check(g["frames_dir"] in shotlist and "0001" in shotlist, "GIF frames %s/NNNN.png described in SHOTLIST.md" % g["frames_dir"])


# ---------------------------------------------------------------------------------------- 3 + 4. builds

def build_everything(manifest, captures_dir, gif_frames=None):
    """Logo files + cards + GIFs as {relative output: bytes}, plus the per-image list of missing captures."""
    report = common.Report()
    caps = common.Captures(captures_dir)
    out = {}
    logo.build(out)
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
    native, big, icon = (imgio.read_png_size(disk(p)) for p in (logo.P_LOGO_NATIVE, logo.P_LOGO_512, logo.P_ICON))
    check(native == (32, 32), "logo_native.png is 32x32")
    check(big == (512, 512), "logo_512.png is 512x512")
    check(icon == (128, 128), "icon.png keeps its 128x128 size")
    n = lib.Canvas.open(disk(logo.P_LOGO_NATIVE))
    check(bytes(lib.Canvas.open(disk(logo.P_LOGO_512)).buf) == bytes(n.scaled(16).buf), "logo_512.png is the native logo at exactly x16")
    check(bytes(lib.Canvas.open(disk(logo.P_ICON)).buf) == bytes(n.scaled(4).buf), "icon.png is the native logo at exactly x4")
    check(n.buf[3] == 0 and n.buf[(31 * 32 + 31) * 4 + 3] == 0 and n.buf[(16 * 32 + 16) * 4 + 3] == 255,
          "logo_native.png has transparent rounded corners and an opaque centre")
    d = max(abs(x - y) for x, y in zip(bytes(logo.emblem(1.0).buf), bytes(logo.emblem(0.0).buf)))
    check(d <= 1, "logo_animated loops seamlessly: phase 1.0 equals phase 0.0 (max channel difference %d)" % d)

    limits = {"docs/curseforge/img/logo_animated.gif": (128, 128, 1 * MB, 5)}
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

def test_hygiene():
    print("6. standard library only, no bytecode")
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
    test_hygiene()
    print()
    if failures:
        print("%d check(s) FAILED" % len(failures))
        return 1
    print("all checks passed")
    return 0


if __name__ == "__main__":
    sys.exit(main())

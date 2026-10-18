"""The Glintsmith logo and mod icon, made from a studio render.  Nothing here is drawn.

    py -B art/logo.py [--captures DIR] [--out DIR] [--strict]

Input: the studio still stills_hero / logo (art/captures/studio/stills_hero/, frame named `logo`: a 1024 x 1024 render of
an enchanted sword with the mod's glint and outline, K / W frame pair, see docs/STUDIO.md).  The pair is matted into a
clean transparent render (art/studio/matte.py) and set on the cream studio backdrop (art/studio/composite.py, with or
without the soft shadows: the `logo` section of gallery/cards.json says which).  A square is cut from that picture
(`crop`: centre and size, by default the centred square of the full height) and resized with the area filter (exact box
average, see lib.Canvas.area) to

    art/logo/logo_512.png                              512 x 512   the logo
    src/main/resources/assets/glintsmith/icon.png      128 x 128   the mod icon
    art/logo/logo_32.png                                32 x 32    small version

The animated logo (docs/curseforge/img/logo_animated.gif, from the hero_sword turntable) is a GIF of the gallery
manifest and built by gallery/gifs.py / build.py.

A render that is not there is never replaced by anything: the message names the file, nothing is written (the committed
icon.png stays as it is) and the exit code is 0, or 1 with --strict.  A scene that is there but broken ends the run with
an error.  `--out DIR` writes the whole tree below DIR instead of the repository (use it with synthetic scenes, so that
the real icon.png is never overwritten).

Python 3 standard library only.  Output is byte-identical from run to run.
"""
import argparse
import os
import sys

sys.dont_write_bytecode = True
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
sys.path.insert(0, os.path.join(HERE, "gallery"))
sys.path.insert(0, os.path.join(HERE, "studio"))

import common   # noqa: E402
import composite   # noqa: E402
import lib      # noqa: E402
import pipeline   # noqa: E402
import scene as studio_scene   # noqa: E402

P_LOGO = "art/logo/logo_512.png"
P_LOGO_SMALL = "art/logo/logo_32.png"
P_ICON = "src/main/resources/assets/glintsmith/icon.png"
SIZES = ((P_LOGO, 512), (P_ICON, 128), (P_LOGO_SMALL, 32))


def square(img, crop):
    """(x, y, size) of the square cut from a capture; crop is the manifest's {"centre": [x, y], "size": n | "full"}.
    The square is moved, never shrunk, to lie inside the image."""
    crop = crop or {}
    size = crop.get("size", "full")
    size = min(img.w, img.h) if size == "full" else int(size)
    if size < 1 or size > min(img.w, img.h):
        sys.exit("error: logo crop size %d does not fit a %dx%d capture" % (size, img.w, img.h))
    cx, cy = crop.get("centre", ["c", "c"])
    cx = img.w // 2 if cx == "c" else int(cx)
    cy = img.h // 2 if cy == "c" else int(cy)
    return (min(max(cx - size // 2, 0), img.w - size), min(max(cy - size // 2, 0), img.h - size), size)


def cut_square(img, crop, size):
    x, y, n = square(img, crop)
    return img.crop(x, y, n, n).area(size, size)


def logo_picture(cfg, captures):
    """(Canvas of the logo render on the studio backdrop, None) or (None, the repository-relative file that is missing)."""
    try:
        sc = studio_scene.load(captures.folder, cfg["scene"])
    except studio_scene.SceneError as e:
        if e.kind == "missing":
            return None, "art/captures/studio/%s/scene.json" % cfg["scene"]
        sys.exit("error: %s" % e)
    try:
        frame = studio_scene.find_frame(sc, cfg["frame"])
    except studio_scene.SceneError as e:
        sys.exit("error: %s" % e)
    for path in sc.pair(frame):
        if not os.path.isfile(path):
            return None, "art/captures/studio/%s/%s" % (cfg["scene"], os.path.basename(path))
    try:
        m = pipeline.matte_frame(sc, frame)
    except (ValueError, OSError) as e:
        sys.exit("error: cannot matte studio frame %s of %s: %s" % (frame["file"], cfg["scene"], e))
    return composite.render(m, composite.style(cfg.get("style")), None, cfg.get("shadows", True)), None


def build(out, manifest, captures, log=None):
    """Fill out {relative path: bytes} with whatever can be built from the studio render there is; returns the list
    of missing files (relative to the repository) so that the caller can report them."""
    cfg = manifest["logo"]
    pic, missing = logo_picture(cfg, captures)
    if pic is None:
        return [missing]
    for path, size in SIZES:
        out[path] = cut_square(pic, cfg.get("crop"), size).to_png()
    return []


def main(argv=None):
    ap = argparse.ArgumentParser(description=__doc__.split("\n\n")[0])
    ap.add_argument("--manifest", default=common.DEFAULT_MANIFEST)
    ap.add_argument("--captures", default=common.DEFAULT_CAPTURES, help="folder that holds studio/<scene>/ (default art/captures)")
    ap.add_argument("--out", default=lib.ROOT, help="write the tree below this folder (default: the repository)")
    ap.add_argument("--strict", action="store_true", help="exit 1 if the render is missing")
    args = ap.parse_args(argv)

    manifest = common.load_manifest(args.manifest)
    out = {}
    missing = build(out, manifest, common.Captures(args.captures))
    for rel, data in sorted(out.items()):
        path = os.path.join(args.out, *rel.split("/"))
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, "wb") as f:
            f.write(data)
        print("wrote %s (%d bytes)" % (path, len(data)))
    for rel in missing:
        print("capture missing: %s (skipped, see art/gallery/SHOTLIST.md, \"Studio scenes\")" % rel)
    if missing and not out:
        print("No logo or icon built; the committed files were not touched.")
    return 1 if missing and args.strict else 0


if __name__ == "__main__":
    sys.exit(main())

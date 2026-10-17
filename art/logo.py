#!/usr/bin/env python3
"""The Glintsmith logo, mod icon and animated logo, made from in-game captures.  Nothing here is drawn.

    py -B art/logo.py [--captures DIR] [--out DIR] [--strict] [--no-gif]

Input: art/captures/logo_sword.png, a 1920 x 1080 screenshot of an enchanted sword with the mod's glint and outline on a
white backdrop, subject centred (SHOTLIST.md, "Hero shots").  The logo, the icon and the animated logo are on that
white backdrop (the light theme of the page; the backdrop is the capture's own, nothing is recoloured, tinted or
composited here).  A square is cut from it (the `logo` section of
gallery/cards.json: crop centre and size, by default the centred square of the full height) and resized with the area
filter (exact box average, see lib.Canvas.area) to

    art/logo/logo_512.png                              512 x 512   the logo
    src/main/resources/assets/glintsmith/icon.png      128 x 128   the mod icon
    art/logo/logo_32.png                                32 x 32    small version
    docs/curseforge/img/logo_animated.gif              128 x 128   the hero shots hero_sword_rainbow / _void / _fire,
                                                                   same square crop, cross-faded in a loop

A capture that is not there is never replaced by anything: the message names the file, nothing is written (the
committed icon.png stays as it is) and the exit code is 0, or 1 with --strict.  A GIF whose hero shots are not all
there is skipped the same way.

Python 3 standard library only.  Output is byte-identical from run to run.
"""
import argparse
import os
import sys

sys.dont_write_bytecode = True
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
sys.path.insert(0, os.path.join(HERE, "gallery"))

import common   # noqa: E402
import imgio    # noqa: E402
import lib      # noqa: E402

P_LOGO = "art/logo/logo_512.png"
P_LOGO_SMALL = "art/logo/logo_32.png"
P_ICON = "src/main/resources/assets/glintsmith/icon.png"
P_LOGO_GIF = "docs/curseforge/img/logo_animated.gif"
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


def blend(a, b, i, n):
    """RGB bytes a -> b at step i of n (1 <= i <= n - 1), integer arithmetic."""
    return bytes([(p * (n - i) + q * i + n // 2) // n for p, q in zip(a, b)])


def gif_frames(looks, hold, fade):
    """Every look held `hold` frames, then `fade` frames of cross-fade to the next look (the last fades back into the
    first), so the loop has no seam."""
    frames = []
    for k, look in enumerate(looks):
        nxt = looks[(k + 1) % len(looks)]
        frames += [look] * hold
        frames += [blend(look, nxt, i, fade + 1) for i in range(1, fade + 1)]
    return frames


def build(out, manifest, captures, log=None):
    """Fill out {relative path: bytes} with whatever can be built from the captures there are; returns the list
    of missing capture paths (relative to the repository) so that the caller can report them."""
    cfg = manifest["logo"]
    missing = []
    img = captures.get(cfg["capture"])
    if img is None:
        missing.append("art/captures/" + cfg["capture"])
    else:
        for path, size in SIZES:
            out[path] = cut_square(img, cfg.get("crop"), size).to_png()
    captures.drop(cfg["capture"])

    g = cfg.get("gif")
    if g:
        gone = ["art/captures/" + rel for rel in g["captures"] if not captures.exists(rel)]
        if gone:
            missing += gone
        else:
            size = g["size"]
            looks = []
            for rel in g["captures"]:
                looks.append(cut_square(captures.get(rel), g.get("crop", cfg.get("crop")), size).rgb_bytes())
                captures.drop(rel)
            frames = gif_frames(looks, g["hold"], g["fade"])
            out[P_LOGO_GIF] = imgio.encode_gif_frames(size, size, frames, [g["delay_cs"]] * len(frames), n_colors=255)
    return missing


def main(argv=None):
    ap = argparse.ArgumentParser(description=__doc__.split("\n\n")[0])
    ap.add_argument("--manifest", default=common.DEFAULT_MANIFEST)
    ap.add_argument("--captures", default=common.DEFAULT_CAPTURES, help="folder with the capture PNGs")
    ap.add_argument("--out", default=lib.ROOT, help="write the tree below this folder (default: the repository)")
    ap.add_argument("--strict", action="store_true", help="exit 1 if a capture is missing")
    ap.add_argument("--no-gif", action="store_true", help="skip logo_animated.gif")
    args = ap.parse_args(argv)

    manifest = common.load_manifest(args.manifest)
    if args.no_gif:
        manifest = dict(manifest, logo=dict(manifest["logo"], gif=None))
    out = {}
    missing = build(out, manifest, common.Captures(args.captures))
    for rel, data in sorted(out.items()):
        path = os.path.join(args.out, *rel.split("/"))
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, "wb") as f:
            f.write(data)
        print("wrote %s (%d bytes)" % (path, len(data)))
    for rel in missing:
        print("capture missing: %s (skipped, see art/gallery/SHOTLIST.md, \"Hero shots\")" % rel)
    if missing and not out:
        print("No logo, icon or animated logo built; the committed files were not touched.")
    return 1 if missing and args.strict else 0


if __name__ == "__main__":
    sys.exit(main())

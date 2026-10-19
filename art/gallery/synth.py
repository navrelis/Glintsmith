#!/usr/bin/env python3
"""Synthetic stand-ins for the captures, for testing the layout of the cards.  NOT game art, never committed.

    py -B art/gallery/synth.py DIR [--frames N] [--supersample S] [--workers N] [--no-studio]

Writes, into DIR, a 1920 x 1080 PNG for every in-world capture the manifest names (a light gradient like the white / cream
in-game backdrop, with a coordinate grid, a few shapes, and the file name in the vanilla font) and, unless --no-studio is
given, the eleven synthetic studio scenes of the gallery contract (art/studio/synth_studio.py: the turntables hero_sword,
lineup, armor_stand and logo_anim_a with N frames each, and the still scenes stills_hero, stills_wide, stills_tiles,
stills_gap, stills_default, stills_default_armor and logo_concepts with their named shots).
Point the builders at it with `--captures DIR` to check crops, scales, labels and spacing before real captures
exist.  Every picture is stamped "SYNTHETIC TEST CAPTURE" so it cannot be mistaken for a real one.

Standard library only.
"""
import argparse
import os
import sys

sys.dont_write_bytecode = True
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)

sys.path.insert(0, os.path.join(os.path.dirname(HERE), "studio"))

import common   # noqa: E402
import lib      # noqa: E402
import synth_studio   # noqa: E402
from lib import Canvas   # noqa: E402

W, H = 1920, 1080
MARK = "SYNTHETIC TEST CAPTURE"      # also stored in a PNG text chunk; verify.py refuses such files in art/captures


def hue_colour(seed, v):
    """A deterministic light colour (like the white / cream in-game backdrop) for a seed and a brightness 0..1."""
    import colorsys
    r, g, b = colorsys.hsv_to_rgb((seed * 0.137) % 1.0, 0.10, 0.90 + 0.10 * v)
    return (int(r * 255), int(g * 255), int(b * 255), 255)


def base(seed, label):
    c = Canvas(W, H)
    for y in range(H):
        c.rect(0, y, W, 1, hue_colour(seed, y / H))
    for x in range(0, W, 100):
        c.rect(x, 0, 1, H, (0, 0, 0, 40))
    for y in range(0, H, 100):
        c.rect(0, y, W, 1, (0, 0, 0, 40))
    for x in range(0, W, 200):
        for y in range(0, H, 200):
            lib.font_text(c, x + 4, y + 4, "%d,%d" % (x, y), (0, 0, 0, 170), 1, shadow=False)
    # centre cross and a diagonal "item"
    c.rect(W // 2 - 60, H // 2, 120, 1, (0, 0, 0, 255))
    c.rect(W // 2, H // 2 - 60, 1, 120, (0, 0, 0, 255))
    for i in range(240):
        c.rect(W // 2 - 120 + i, H // 2 + 120 - i, 18, 18, (90, 105, 160, 255))
        c.rect(W // 2 - 120 + i - 4, H // 2 + 120 - i - 4, 4, 22, (200, 120, 255, 255))
    c.rect(W // 2 - 70, H // 2 + 20, 140, 14, (220, 170, 60, 255))
    lib.font_text(c, W // 2 - lib.text_width(label, 3) // 2, 60, label, (30, 25, 40, 255), 3)
    lib.font_text(c, W // 2 - lib.text_width("SYNTHETIC TEST CAPTURE", 3) // 2, 100, "SYNTHETIC TEST CAPTURE", (170, 90, 0, 255), 3)
    return c


def make(folder, manifest=None, frames=12, log=None, studio=True, supersample=2, workers=1):
    """Write the synthetic in-world captures and, with `studio`, the synthetic studio scenes (`frames` frames per turntable)
    into `folder`.  Returns the sorted list of capture names written."""
    manifest = manifest or common.load_manifest()
    seen = {}
    for _image, rel in common.required_captures(manifest):
        if rel.startswith("studio/"):          # studio scenes: see art/studio/synth_studio.py
            continue
        seen.setdefault(rel, len(seen) + 1)
    for rel, seed in sorted(seen.items()):
        common.write(os.path.dirname(os.path.join(folder, *rel.split("/"))), os.path.basename(rel),
                     base(seed, rel).to_png(MARK))
        if log:
            log("synthetic " + rel)
    if studio:
        for sid, path in synth_studio.make_contract(folder, frames, supersample, workers).items():
            if log:
                log("synthetic studio scene %s" % sid)
    return sorted(seen)


def main():
    ap = argparse.ArgumentParser(description=__doc__.split("\n\n")[0])
    ap.add_argument("dir")
    ap.add_argument("--frames", type=int, default=12, help="frames of each synthetic turntable (default 12)")
    ap.add_argument("--supersample", type=int, default=2)
    ap.add_argument("--workers", type=int, default=1)
    ap.add_argument("--no-studio", action="store_true", help="only the in-world captures")
    args = ap.parse_args()
    if os.path.abspath(args.dir).startswith(os.path.join(common.ART, "captures")):
        sys.exit("error: refusing to write synthetic captures into art/captures")
    names = make(args.dir, frames=args.frames, log=print, studio=not args.no_studio, supersample=args.supersample, workers=args.workers)
    print("wrote %d synthetic captures%s to %s" % (len(names), "" if args.no_studio else " and the studio scenes", args.dir))


if __name__ == "__main__":
    main()

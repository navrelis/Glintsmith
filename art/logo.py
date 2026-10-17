"""The Glintsmith emblem: a 32 x 32 native pixel logo, its 512 and 128 upscales and the animated icon.

Original pixel art, drawn by this script (no Minecraft pixels, no third-party art): a diagonal sword with a
brass crossguard and a violet pommel gem on a dark violet tile, wrapped in the one-pixel rainbow outline that
Glintsmith gives items, with a faint glow ring and a glint streak running along the blade.

    py -B art/logo.py [--out DIR]      write the logo files (default: the repository)

Python 3 standard library only.  Output is byte-identical from run to run.
"""
import argparse
import colorsys
import math
import os
import sys

sys.dont_write_bytecode = True
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import imgio   # noqa: E402
import lib     # noqa: E402
from lib import Canvas   # noqa: E402

P_LOGO_NATIVE = "art/logo/logo_native.png"
P_LOGO_512 = "art/logo/logo_512.png"
P_ICON = "src/main/resources/assets/glintsmith/icon.png"
P_LOGO_GIF = "docs/curseforge/img/logo_animated.gif"

STILL_STREAK = 0.42
FRAMES = 40
S2 = math.sqrt(2.0)

# colour ramps, dark to light
STEEL = [lib.rgb(h) for h in ("2b2f54", "4c5486", "7a84b8", "b3bde6", "e6ebff")]
GOLD = [lib.rgb(h) for h in ("5a3416", "8c5a1c", "c28a26", "eab93f", "fff0a0")]
LEATHER = [lib.rgb(h) for h in ("4a2a22", "7c4c2e", "b07a40")]
GEM = [lib.rgb(h) for h in ("3a1a78", "6a36c8", "a468ff", "d9b8ff", "ffffff")]

# tile
NIGHT, LIT, DIM, RIM = lib.rgb("1b1333"), lib.rgb("2d2352"), lib.rgb("120c24"), lib.rgb("08050f")


def tile():
    c = Canvas(32, 32, NIGHT)
    c.frame(1, 1, 30, 30, DIM)
    c.hline(1, 1, 30, LIT)
    c.vline(1, 1, 30, LIT)
    c.frame(0, 0, 32, 32, RIM)
    for x, y in ((0, 0), (1, 0), (0, 1), (31, 0), (30, 0), (31, 1), (0, 31), (1, 31), (0, 30), (31, 31), (30, 31), (31, 30)):
        c.buf[(y * 32 + x) * 4 + 3] = 0
    for x, y in ((1, 1), (30, 1), (1, 30), (30, 30)):
        c.set(x, y, RIM)
    return c


def axes(x, y):
    """(a, b): pixel centre along the sword axis (a, bottom left to top right) and across it (b), in pixels."""
    X, Y = x + 0.5, y + 0.5
    return (X - Y) / S2, (X + Y - 32.0) / S2


def sword_pixels():
    """dict (x, y) -> colour of the sword without outline, light coming from the top left."""
    px = {}
    for y in range(32):
        for x in range(32):
            a, b = axes(x, y)
            col = None
            # blade: five diagonals wide, tapering to a point over the last 6 pixels
            tip = 14.8
            if -2.8 <= a <= tip:
                half = 1.55 if a < tip - 6.0 else 1.55 * max(0.0, (tip - a) / 5.5) + 0.12
                if abs(b) <= half:
                    d = b / max(half, 0.5)                      # -1 lit edge .. +1 shadow edge
                    if d < -0.62:
                        col = STEEL[4]
                    elif d < -0.2:
                        col = STEEL[3]
                    elif d < 0.28:
                        col = STEEL[2]
                    elif d < 0.7:
                        col = STEEL[1]
                    else:
                        col = STEEL[0]
                    if a > tip - 1.2:
                        col = STEEL[4] if b <= 0.2 else STEEL[3]
            # guard: a brass bar across the blade root, its ends swept towards the tip
            ac = -4.0 + 0.04 * b * b
            if abs(a - ac) <= 1.55 and abs(b) <= 5.2:
                d = (a - ac + 1.55) / 3.1                         # 0 lower edge .. 1 upper edge (towards the tip)
                col = GOLD[4] if d > 0.8 else GOLD[3] if d > 0.45 else GOLD[2] if d > 0.12 else GOLD[1]
                if abs(b) > 4.0:
                    col = GOLD[1] if d < 0.5 else GOLD[2]
            # grip
            if -10.4 <= a < -5.3 and abs(b) <= 1.12:
                k = int((a + 10.4) / 1.3)
                tone = LEATHER[2] if b < -0.3 else LEATHER[1] if b < 0.5 else LEATHER[0]
                if k % 2 == 1:
                    tone = LEATHER[1] if tone is LEATHER[2] else LEATHER[0]
                col = tone
            # pommel gem
            pa, pb = a + 12.6, b
            r2 = pa * pa + pb * pb
            if r2 <= 2.4 ** 2:
                if r2 > 1.8 ** 2:
                    col = GEM[0] if (pa + pb) > 0 else GEM[1]
                elif pa + pb < -1.0:
                    col = GEM[3]
                elif pa + pb < 0.1:
                    col = GEM[2]
                else:
                    col = GEM[1]
                if abs(pa + 0.9) + abs(pb + 0.9) < 0.9:
                    col = GEM[4]
            if col is not None:
                px[(x, y)] = col
    return px


def rainbow(a, phase, span, sat=0.62, val=1.0):
    """Outline colour at axis position a (-17..17): hues sweep along the sword and shift with the phase."""
    h = (0.60 + span * ((a + 17.0) / 34.0) + phase) % 1.0
    r, g, b = colorsys.hsv_to_rgb(h, sat, val)
    return (int(r * 255 + 0.5), int(g * 255 + 0.5), int(b * 255 + 0.5), 255)


def render(hue_phase=0.0, streak_phase=STILL_STREAK, span=0.34):
    """The emblem as a 32 x 32 Canvas.  hue_phase and streak_phase in [0, 1) move the outline colours and the
    glint streak (one full turn each is one seamless animation loop)."""
    c = tile()
    sw = sword_pixels()
    cells = set(sw)
    # 1 px outline (4-neighbourhood) and a faint second ring
    ring1 = set()
    for (x, y) in cells:
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            p = (x + dx, y + dy)
            if p not in cells:
                ring1.add(p)
    ring2 = set()
    for (x, y) in ring1 | cells:
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1), (1, 1), (-1, -1), (1, -1), (-1, 1)):
            p = (x + dx, y + dy)
            if p not in cells and p not in ring1:
                ring2.add(p)
    for (x, y) in sorted(ring2):
        a, _ = axes(x, y)
        col = rainbow(a, hue_phase, span, 0.7, 1.0)
        c.set(x, y, col[:3] + (70,))
    for (x, y) in sorted(ring1):
        a, _ = axes(x, y)
        c.set(x, y, rainbow(a, hue_phase, span, 0.72))
    for (x, y), col in sorted(sw.items()):
        c.set(x, y, col)
    # glint streak: a thin band across the blade that travels from guard to tip over one loop
    streak_a = -4.6 + 21.6 * streak_phase
    for (x, y), col in sorted(sw.items()):
        a, b = axes(x, y)
        if a > -3.0 and abs(a - streak_a) < 0.8:
            c.set(x, y, lib.mix(col, GEM[3], 0.55))
    return c


def sparkle_on(c, x, y):
    """A small four-point glint, one pixel arms."""
    c.set(x, y, GEM[4])
    for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        c.set(x + dx, y + dy, GEM[3])


def emblem(phase=None):
    """The native emblem; phase None is the still, else a loop phase in [0, 1) for the animation."""
    c = render(0.0, STILL_STREAK) if phase is None else render(phase, phase)
    sparkle_on(c, 8, 8)
    c.set(24, 24, GEM[3])
    for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        c.set(24 + dx, 24 + dy, GEM[1])
    return c


def build(out):
    still = emblem()
    out[P_LOGO_NATIVE] = still.to_png()
    out[P_LOGO_512] = still.scaled(16).to_png()
    out[P_ICON] = still.scaled(4).to_png()
    frames = [emblem(k / FRAMES).scaled(4).rgb_bytes() for k in range(FRAMES)]
    mask = bytes(still.scaled(4).buf[3::4])
    mask = bytes(255 if a else 0 for a in mask)
    out[P_LOGO_GIF] = imgio.encode_gif_frames(128, 128, frames, [5] * FRAMES, n_colors=255, keep_mask=mask)


def main():
    ap = argparse.ArgumentParser(description=__doc__.split("\n")[0])
    ap.add_argument("--out", default=lib.ROOT, help="write the tree below this folder (default: the repository)")
    args = ap.parse_args()
    out = {}
    build(out)
    for rel, data in out.items():
        path = os.path.join(args.out, *rel.split("/"))
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, "wb") as f:
            f.write(data)
        print("wrote", rel, len(data), "bytes")


if __name__ == "__main__":
    main()

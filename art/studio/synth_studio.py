#!/usr/bin/env python3
"""Synthetic studio scene: a fake turntable in the FRAME CONTRACT format, to test the studio pipeline.  NOT game art.

    py -B art/studio/synth_studio.py DIR [--scene ID] [--frames N] [--size WxH] [--supersample S] [--fps F] [--workers N]

Writes DIR/studio/<scene id>/scene.json and, per frame, NNNN_k.png (rendered over pure black) and NNNN_w.png (over pure
white): a little extruded sword (blade, guard, grip, pommel as boxes) that turns on a vertical axis, with flat face
shading, a 2 px dark outline, a bright glint stripe that sweeps along the blade, and a semi-transparent soft violet glow
round the whole shape.  The picture is drawn at S times the size (supersampling) as scanline-filled polygons, so it
costs a fraction of a second per frame in pure Python.

Every PNG carries the text "SYNTHETIC TEST CAPTURE" in a PNG text chunk and scene.json says `"synthetic": true`;
verify.py refuses such files in art/captures/ (use a temp folder, or pass --out into one).  The renders are fully
deterministic (no randomness, no timestamps).

For the matte test the black render holds the premultiplied colour (K = c * a) and the white one K + 255 * (1 - a),
exactly what the real studio produces, so the glow is a true semi-transparent edge, not a fake.

    make_scene(root, ...)  -> path of the scene folder
"""
import argparse
import json
import math
import os
import sys

sys.dont_write_bytecode = True
HERE = os.path.dirname(os.path.abspath(__file__))
ART = os.path.dirname(HERE)
for p in (HERE, ART):
    if p not in sys.path:
        sys.path.insert(0, p)

import composite   # noqa: E402
import imgio       # noqa: E402
import matte       # noqa: E402

MARK = "SYNTHETIC TEST CAPTURE"
INK = (42, 36, 51)
GLOWS = {"rainbow": (170, 110, 255), "frost": (110, 200, 255), "fire": (255, 140, 60)}
LOOKS = [("rainbow", "Rainbow glint + rainbow outline"), ("frost", "Frost glint + soft cyan outline"),
         ("fire", "Fire glint + pulsing outline")]

# boxes of the sword: (name, x0, x1, y0, y1, z0, z1, colour, takes the glint stripe); units of u, y up
BOXES = [
    ("pommel", -0.13, 0.13, -0.80, -0.62, -0.10, 0.10, (222, 176, 62), False),
    ("grip", -0.055, 0.055, -0.62, -0.14, -0.055, 0.055, (122, 78, 44), False),
    ("blade", -0.11, 0.11, -0.02, 1.10, -0.035, 0.035, (196, 206, 224), True),
    ("tip", -0.06, 0.06, 1.10, 1.30, -0.025, 0.025, (206, 216, 232), True),
    ("guard", -0.42, 0.42, -0.14, -0.02, -0.07, 0.07, (222, 176, 62), False),
]
FACES = [  # normal, four corner signs (sx, sy, sz) in order
    ((0, 0, 1), [(-1, -1, 1), (1, -1, 1), (1, 1, 1), (-1, 1, 1)]),
    ((0, 0, -1), [(1, -1, -1), (-1, -1, -1), (-1, 1, -1), (1, 1, -1)]),
    ((1, 0, 0), [(1, -1, 1), (1, -1, -1), (1, 1, -1), (1, 1, 1)]),
    ((-1, 0, 0), [(-1, -1, -1), (-1, -1, 1), (-1, 1, 1), (-1, 1, -1)]),
    ((0, 1, 0), [(-1, 1, 1), (1, 1, 1), (1, 1, -1), (-1, 1, -1)]),
    ((0, -1, 0), [(-1, -1, -1), (1, -1, -1), (1, -1, 1), (-1, -1, 1)]),
]
PITCH = math.radians(14)
LIGHT = (-0.35, 0.65, 0.68)


def _rot(p, ct, st, cp, sp):
    """Turn a point about the vertical axis, then tip it by the camera pitch.  Returns (x, y, z), z toward the camera."""
    x, y, z = p
    x, z = x * ct + z * st, -x * st + z * ct
    y, z = y * cp - z * sp, y * sp + z * cp
    return x, y, z


class Frame:
    """One supersampled picture: colour planes and an opaque mask, with a scanline polygon filler."""

    def __init__(self, w, h):
        self.w, self.h = w, h
        self.planes = [bytearray(w * h) for _ in range(3)]
        self.mask = bytearray(w * h)

    def span(self, y, xa, xb, col):
        if y < 0 or y >= self.h:
            return
        xa, xb = max(0, xa), min(self.w, xb)
        if xb <= xa:
            return
        o = y * self.w
        n = xb - xa
        for c in range(3):
            self.planes[c][o + xa:o + xb] = bytes((col[c],)) * n
        self.mask[o + xa:o + xb] = b"\xff" * n

    def poly(self, pts, col, stripe=None, dx=0, dy=0):
        """Fill a convex polygon; stripe = (centre at y=0, slope, half width, colour) overpaints a diagonal band."""
        ys = [p[1] for p in pts]
        y0, y1 = int(math.floor(min(ys) + dy)), int(math.ceil(max(ys) + dy))
        n = len(pts)
        for y in range(max(0, y0), min(self.h, y1 + 1)):
            yc = y + 0.5 - dy
            xs = []
            for i in range(n):
                (xa, ya), (xb, yb) = pts[i], pts[(i + 1) % n]
                if ya != yb and min(ya, yb) <= yc < max(ya, yb):
                    xs.append(xa + (yc - ya) * (xb - xa) / (yb - ya))
            if len(xs) < 2:
                continue
            xl, xr = int(round(min(xs) + dx)), int(round(max(xs) + dx))
            self.span(y, xl, xr, col)
            if stripe:
                c0, slope, hw, scol = stripe
                mid = c0 + slope * y
                self.span(y, max(xl, int(mid - hw)), min(xr, int(mid + hw)), scol)


def render_pair(W, H, S, theta, glow_col, stripe_t, glow_strength=0.55):
    """(K rgb bytes, W rgb bytes) of the sword at turntable angle theta, each (W*S) x (H*S)."""
    pw, ph = W * S, H * S
    fr = Frame(pw, ph)
    u = ph * 0.385
    cx, cy = pw / 2.0, ph * 0.505
    ct, st, cp, sp = math.cos(theta), math.sin(theta), math.cos(PITCH), math.sin(PITCH)
    ln = math.sqrt(sum(v * v for v in LIGHT))
    light = tuple(v / ln for v in LIGHT)
    o = 2 * S
    offsets = [(0, 0)] + [(dx, dy) for dx in (-o, 0, o) for dy in (-o, 0, o) if dx or dy]
    boxes = []
    for name, x0, x1, y0, y1, z0, z1, col, glint in BOXES:
        centre = _rot(((x0 + x1) / 2, (y0 + y1) / 2 - 0.25, (z0 + z1) / 2), ct, st, cp, sp)
        boxes.append((centre[2], name, (x0, x1, y0 - 0.25, y1 - 0.25, z0, z1), col, glint))
    boxes.sort()
    for _depth, name, (x0, x1, y0, y1, z0, z1), col, glint in boxes:
        halves = ((x1 - x0) / 2, (y1 - y0) / 2, (z1 - z0) / 2)
        mid = ((x0 + x1) / 2, (y0 + y1) / 2, (z0 + z1) / 2)
        visible = []
        for normal, corners in FACES:
            nx, ny, nz = _rot(normal, ct, st, cp, sp)
            if nz <= 0.02:
                continue
            pts = []
            for sx, sy, sz in corners:
                x, y, z = _rot((mid[0] + sx * halves[0], mid[1] + sy * halves[1], mid[2] + sz * halves[2]), ct, st, cp, sp)
                pts.append((cx + x * u, cy - y * u))
            lit = max(0.0, nx * light[0] + ny * light[1] + nz * light[2])
            k = 0.52 + 0.55 * lit
            shade = tuple(min(255, int(c * k)) for c in col)
            visible.append((pts, shade, normal))
        for pts, _shade, _n in visible:                            # the 2 px outline: the face grown in all directions
            for dx, dy in offsets:
                fr.poly(pts, INK, dx=dx, dy=dy)
        for pts, shade, normal in visible:
            stripe = None
            if glint and normal[2] != 0:
                k = 1.2                                            # a diagonal band: rows y + k * (x - cx) = T
                big_t = cy + (-1.4 + 2.8 * stripe_t) * u
                stripe = (cx + big_t / k, -1.0 / k, 0.05 * u / k,
                          tuple(min(255, int(c * 0.35 + 255 * 0.65)) for c in (255, 245, 255)))
            fr.poly(pts, shade, stripe)
    # soft glow round the shape: the mask reduced, blurred and grown back (a semi-transparent halo)
    F = 4
    gw, gh = pw // F, ph // F
    sums = matte.box_sums(bytes(fr.mask), pw, ph, F)
    grid = [[v // (F * F) for v in sums[gy * gw:(gy + 1) * gw]] for gy in range(gh)]
    grid = composite.blur_grid(grid, 0.030 * pw / F)
    grid = [[min(255, v * 3) for v in row] for row in grid]            # fatter than the blur alone
    live = [gy for gy, r in enumerate(grid) if any(r)]
    glow = bytearray(pw * ph)
    if live:
        glow = composite.upscale(grid, gw, gh, F, pw, ph, live[0] * F - F, live[-1] * F + 2 * F)
    gtab = bytes(int(v * glow_strength) for v in range(256))
    glow = glow.translate(gtab)
    n = pw * ph
    m = int.from_bytes(fr.mask, "little")
    notm = m ^ int.from_bytes(b"\xff" * n, "little")
    k_planes, w_planes = [], []
    for c in range(3):
        gk = glow.translate(bytes((glow_col[c] * v + 127) // 255 for v in range(256)))                    # c * a
        gw_ = glow.translate(bytes(min(255, (glow_col[c] * v + 127) // 255 + 255 - v) for v in range(256)))   # c * a + 255 * (1 - a)
        solid = int.from_bytes(fr.planes[c], "little")
        k_planes.append(((solid & m) | (int.from_bytes(gk, "little") & notm)).to_bytes(n, "little"))
        w_planes.append(((solid & m) | (int.from_bytes(gw_, "little") & notm)).to_bytes(n, "little"))
    out = []
    for planes in (k_planes, w_planes):
        rgb = bytearray(3 * n)
        for c in range(3):
            rgb[c::3] = planes[c]
        out.append(bytes(rgb))
    return out[0], out[1]


def _task(args):
    folder, i, n, W, H, S, look = args
    theta = math.radians(28) + 2 * math.pi * (i - 1) / n
    stripe_t = ((i - 1) % 90) / 90.0
    k, w = render_pair(W, H, S, theta, GLOWS[look], stripe_t)
    name = "%04d" % i
    for suffix, data in (("k", k), ("w", w)):
        with open(os.path.join(folder, "%s_%s.png" % (name, suffix)), "wb") as f:
            f.write(imgio.png_bytes(W * S, H * S, data, 3, MARK))
    return i


def make_scene(root, scene="synth_turntable", frames=36, width=256, height=256, supersample=2, fps=30, workers=1):
    """Write the scene into root/studio/<scene>/ and return that folder.  root must not be art/captures."""
    folder = os.path.join(root, "studio", scene)
    os.makedirs(folder, exist_ok=True)
    per_look = max(1, frames // len(LOOKS))
    meta = []
    jobs = []
    for i in range(1, frames + 1):
        look, caption = LOOKS[min((i - 1) // per_look, len(LOOKS) - 1)]
        name = {1: "hero", frames // 4 + 1: "quarter", frames // 2 + 1: "back"}.get(i)
        meta.append({"index": i, "file": "%04d" % i, "look": look, "caption": caption, "name": name})
        jobs.append((folder, i, frames, width, height, supersample, look))
    if workers > 1:
        import multiprocessing
        with multiprocessing.Pool(workers) as pool:
            for _ in pool.imap_unordered(_task, jobs, chunksize=2):
                pass
    else:
        for job in jobs:
            _task(job)
    with open(os.path.join(folder, "scene.json"), "w", encoding="utf-8") as f:
        json.dump({"scene": scene, "width": width, "height": height, "supersample": supersample, "fps": fps,
                   "frames": meta, "looks": [{"id": k, "caption": c} for k, c in LOOKS],
                   "commit": "synthetic", "created": "synthetic", "synthetic": True, "mark": MARK}, f, indent=1)
    return folder


def main(argv=None):
    ap = argparse.ArgumentParser(description=__doc__.split("\n\n")[0])
    ap.add_argument("dir", help="folder to write studio/<scene>/ into (a temp folder, never art/captures)")
    ap.add_argument("--scene", default="synth_turntable")
    ap.add_argument("--frames", type=int, default=36)
    ap.add_argument("--size", default="256x256", help="output size WxH (the files are S times larger)")
    ap.add_argument("--supersample", type=int, default=2)
    ap.add_argument("--fps", type=int, default=30)
    ap.add_argument("--workers", type=int, default=1)
    args = ap.parse_args(argv)
    w, h = (int(v) for v in args.size.lower().split("x"))
    if os.path.abspath(args.dir).startswith(os.path.join(ART, "captures")):
        sys.exit("error: refusing to write synthetic frames into art/captures")
    folder = make_scene(args.dir, args.scene, args.frames, w, h, args.supersample, args.fps, args.workers)
    print("wrote %d synthetic frame pairs (%dx%d, supersample %d) to %s" % (args.frames, w, h, args.supersample, folder))
    return 0


if __name__ == "__main__":
    sys.exit(main())

#!/usr/bin/env python3
"""Synthetic studio scenes: fake turntables and stills in the FRAME CONTRACT format, to test the studio pipeline and
the gallery.  NOT game art.

    py -B art/studio/synth_studio.py DIR [--scene ID] [--frames N] [--size N | WxH] [--supersample S] [--fps F] [--workers N]
    py -B art/studio/synth_studio.py DIR --contract [--frames N] [--supersample S] [--workers N]

The first form writes DIR/studio/<scene id>/scene.json and, per frame, NNNN_k.png (rendered over pure black) and
NNNN_w.png (over pure white): a little extruded sword (blade, guard, grip, pommel as boxes) that turns on a vertical axis,
with flat face shading, a dark outline, a bright glint stripe that sweeps along the blade, and a semi-transparent soft
glow round the whole shape.  The picture is drawn at S times the size (supersampling) as scanline-filled polygons, so it
costs a fraction of a second per frame in pure Python.  `--size 256` is a square, `--size 480x600` a rectangle.

`--contract` writes the scenes the gallery builds from (the names and sizes the in-game studio has to produce, see
docs/STUDIO.md and art/README.md): the turntables hero_sword (512 x 512, 6 looks), lineup (1200 x 400, six items, a
+-35 degree yaw swing) and armor_stand (480 x 600, 4 looks) with `--frames` frames in all (default 12; the real ones
have 90 per look), and the still scenes stills_hero (1024 x 1024: banner_hero, logo), stills_wide (1200 x 400:
universal_vanilla, universal_modded), stills_tiles (512 x 512: 9 glint_* and 10 outline_* shots) and stills_gap
(800 x 800: gap_sword_off / _on, gap_modded_off / _on).  A still scene has one frame (one K / W pair) per shot; the
frame's `name` is the shot name.

Every PNG carries the text "SYNTHETIC TEST CAPTURE" in a PNG text chunk and scene.json says `"synthetic": true`;
verify.py refuses such files in art/captures/ (use a temp folder, or pass --out into one).  The renders are fully
deterministic (no randomness, no timestamps).

For the matte test the black render holds the premultiplied colour (K = c * a) and the white one K + 255 * (1 - a),
exactly what the real studio produces, so the glow is a true semi-transparent edge, not a fake.

    make_scene(root, ...)     -> path of the scene folder (one generic turntable)
    make_contract(root, ...)  -> {scene id: folder} for the seven scenes above
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
GLOWS = {"rainbow": (170, 110, 255), "frost": (110, 200, 255), "fire": (255, 140, 60), "aurora": (70, 215, 190),
         "void": (110, 70, 200), "gold": (240, 185, 60), "neutral": (110, 215, 160), "sparkle": (255, 225, 110)}
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

# a subject: (x, y, u) as fractions of the width / height / height, optional yaw offset in degrees and body tint (r, g, b factors)
SINGLE = [(0.5, 0.505, 0.385, 0, (1.0, 1.0, 1.0))]


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


def draw_sword(fr, cx, cy, u, theta, stripe_t, ink_px, S, ink_col, tint):
    """Draw one extruded sword centred on (cx, cy) with unit length u (supersampled pixels)."""
    ct, st, cp, sp = math.cos(theta), math.sin(theta), math.cos(PITCH), math.sin(PITCH)
    ln = math.sqrt(sum(v * v for v in LIGHT))
    light = tuple(v / ln for v in LIGHT)
    o = max(0, int(round(ink_px * S)))
    offsets = [(0, 0)] + [(dx, dy) for dx in (-o, 0, o) for dy in (-o, 0, o) if dx or dy] if o else []
    boxes = []
    for name, x0, x1, y0, y1, z0, z1, col, glint in BOXES:
        centre = _rot(((x0 + x1) / 2, (y0 + y1) / 2 - 0.25, (z0 + z1) / 2), ct, st, cp, sp)
        boxes.append((centre[2], name, (x0, x1, y0 - 0.25, y1 - 0.25, z0, z1), col, glint))
    boxes.sort()
    for _depth, name, (x0, x1, y0, y1, z0, z1), col, glint in boxes:
        col = tuple(min(255, int(c * t)) for c, t in zip(col, tint))
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
        for pts, _shade, _n in visible:                            # the outline: the face grown in all directions
            for dx, dy in offsets:
                fr.poly(pts, ink_col, dx=dx, dy=dy)
        for pts, shade, normal in visible:
            stripe = None
            if glint and normal[2] != 0:
                k = 1.2                                            # a diagonal band: rows y + k * (x - cx) = T
                big_t = cy + (-1.4 + 2.8 * stripe_t) * u
                stripe = (cx + big_t / k, -1.0 / k, 0.05 * u / k,
                          tuple(min(255, int(c * 0.35 + 255 * 0.65)) for c in (255, 245, 255)))
            fr.poly(pts, shade, stripe)


def render_pair(W, H, S, theta, glow_col, stripe_t, glow_strength=0.55, subjects=None, ink=2, ink_col=INK, gaps=False):
    """(K rgb bytes, W rgb bytes) of the swords at turntable angle theta, each (W*S) x (H*S).  `subjects` is a list of
    (x, y, u, yaw offset in degrees, tint) as in SINGLE; ink is the outline thickness in output pixels; gaps carves
    hairline seams into the shapes (the "gap fix off" look)."""
    pw, ph = W * S, H * S
    fr = Frame(pw, ph)
    for sx, sy, su, dyaw, tint in (subjects or SINGLE):
        draw_sword(fr, pw * sx, ph * sy, ph * su, theta + math.radians(dyaw), stripe_t, ink, S, ink_col, tint)
    if gaps:
        for y in range(int(ph * 0.12), int(ph * 0.9), max(4, int(ph * 0.045))):
            fr.mask[y * pw:(y + max(1, S)) * pw] = bytes(pw * max(1, S))
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
    folder, i, W, H, S, params = args
    k, w = render_pair(W, H, S, params["theta"], params["glow"], params["stripe_t"], params.get("strength", 0.55),
                       params.get("subjects"), params.get("ink", 2), params.get("ink_col", INK), params.get("gaps", False))
    name = "%04d" % i
    for suffix, data in (("k", k), ("w", w)):
        with open(os.path.join(folder, "%s_%s.png" % (name, suffix)), "wb") as f:
            f.write(imgio.png_bytes(W * S, H * S, data, 3, MARK))
    return i


def _run(jobs, workers):
    if workers > 1:
        import multiprocessing
        with multiprocessing.Pool(workers) as pool:
            for _ in pool.imap_unordered(_task, jobs, chunksize=2):
                pass
    else:
        for job in jobs:
            _task(job)


def _write_scene(folder, scene, width, height, supersample, fps, meta, looks, jobs, workers):
    os.makedirs(folder, exist_ok=True)
    _run(jobs, workers)
    with open(os.path.join(folder, "scene.json"), "w", encoding="utf-8") as f:
        json.dump({"scene": scene, "width": width, "height": height, "supersample": supersample, "fps": fps,
                   "frames": meta, "looks": [{"id": k, "caption": c} for k, c in looks],
                   "commit": "synthetic", "created": "synthetic", "synthetic": True, "mark": MARK}, f, indent=1)
    return folder


def make_scene(root, scene="synth_turntable", frames=36, width=256, height=256, supersample=2, fps=30, workers=1):
    """Write the scene into root/studio/<scene>/ and return that folder.  root must not be art/captures."""
    folder = os.path.join(root, "studio", scene)
    os.makedirs(folder, exist_ok=True)
    per_look = max(1, frames // len(LOOKS))
    meta, jobs = [], []
    for i in range(1, frames + 1):
        look, caption = LOOKS[min((i - 1) // per_look, len(LOOKS) - 1)]
        name = {1: "hero", frames // 4 + 1: "quarter", frames // 2 + 1: "back"}.get(i)
        meta.append({"index": i, "file": "%04d" % i, "look": look, "caption": caption, "name": name})
        params = {"theta": math.radians(28) + 2 * math.pi * (i - 1) / frames, "glow": GLOWS[look],
                  "stripe_t": ((i - 1) % 90) / 90.0}
        jobs.append((folder, i, width, height, supersample, params))
    return _write_scene(folder, scene, width, height, supersample, fps, meta, LOOKS, jobs, workers)


# ------------------------------------------------------------------------------------------------ the gallery's scenes

HERO_LOOKS = [("vanilla", "Vanilla", "rainbow", 0.20), ("outline", "Outline only", "rainbow", 0.20),
              ("rainbow", "Rainbow glint + rainbow outline", "rainbow", 0.55), ("fire", "Fire glint + pulsing outline", "fire", 0.55),
              ("aurora", "Aurora glint + gradient outline", "aurora", 0.55), ("void", "Void glint + soft glow", "void", 0.60)]
ARMOR_LOOKS = [("vanilla", "Vanilla", "rainbow", 0.20), ("rainbow", "Rainbow glint + rainbow outline", "rainbow", 0.55),
               ("frost", "Frost glint + cyan outline", "frost", 0.55), ("gold", "Gold glint + gold outline", "gold", 0.55)]
LINEUP_TINTS = [(1.0, 1.0, 1.0), (0.8, 0.9, 1.1), (1.1, 0.85, 0.8), (0.9, 1.1, 0.85), (1.1, 1.0, 0.7), (0.85, 0.8, 1.1)]
MODDED_TINTS = [(0.7, 1.0, 0.9), (1.2, 0.8, 0.7), (0.8, 0.8, 1.2), (1.15, 1.1, 0.6), (0.9, 0.7, 1.1), (0.7, 1.1, 1.1)]


def _row(tints, u=0.17, y=0.5):
    return [((i + 0.5) / 6.0, y, u, 12 * (i - 2), t) for i, t in enumerate(tints)]


# the contract: scene id -> (type, width, height); the shots of a still scene are the names of its frames (docs/STUDIO.md)
CONTRACT = {
    "hero_sword": ("turntable", 512, 512), "lineup": ("turntable", 1200, 400), "armor_stand": ("turntable", 480, 600),
    "stills_hero": ("still", 1024, 1024), "stills_wide": ("still", 1200, 400), "stills_tiles": ("still", 512, 512),
    "stills_gap": ("still", 800, 800),
}
SHOTS = {
    "stills_hero": ["banner_hero", "logo"],
    "stills_wide": ["universal_vanilla", "universal_modded"],
    "stills_tiles": ["glint_vanilla", "glint_neutral", "glint_rainbow", "glint_aurora", "glint_fire", "glint_frost", "glint_sparkle",
                     "glint_gold", "glint_void", "outline_vanilla", "outline_static", "outline_rainbow", "outline_pulse",
                     "outline_gradient", "outline_enchant", "outline_thick_1", "outline_thick_3", "outline_thick_6", "outline_soft_80"],
    "stills_gap": ["gap_sword_off", "gap_sword_on", "gap_modded_off", "gap_modded_on"],
}

# glint presets and outline styles of the tiles scene: (name, glow colour, glow strength, ink px, ink colour)
VIOLET, RED, GREEN, AMBER = (160, 80, 240), (214, 60, 70), (31, 160, 110), (255, 138, 31)
TILES = ([("glint_vanilla", "rainbow", 0.18, 2, INK)] +
         [("glint_" + k, k, 0.58, 2, INK) for k in ("neutral", "rainbow", "aurora", "fire", "frost", "sparkle", "gold", "void")] +
         [("outline_vanilla", "rainbow", 0.0, 0, INK), ("outline_static", "rainbow", 0.0, 2, VIOLET),
          ("outline_rainbow", "rainbow", 0.0, 3, (90, 110, 230)), ("outline_pulse", "rainbow", 0.0, 3, AMBER),
          ("outline_gradient", "rainbow", 0.0, 3, GREEN), ("outline_enchant", "rainbow", 0.0, 3, RED),
          ("outline_thick_1", "rainbow", 0.0, 1, VIOLET), ("outline_thick_3", "rainbow", 0.0, 3, VIOLET),
          ("outline_thick_6", "rainbow", 0.0, 6, VIOLET), ("outline_soft_80", "rainbow", 0.7, 4, VIOLET)])
assert [t[0] for t in TILES] == SHOTS["stills_tiles"]


def _stills(root, scene, width, height, S, shots, workers):
    """shots: [(name, params)]; one frame per shot, the shot name is the frame's `name` (and `look`, `caption`)."""
    folder = os.path.join(root, "studio", scene)
    os.makedirs(folder, exist_ok=True)
    meta = [{"index": i, "file": "%04d" % i, "look": n, "caption": n, "name": n} for i, (n, _p) in enumerate(shots, 1)]
    jobs = [(folder, i, width, height, S, p) for i, (_n, p) in enumerate(shots, 1)]
    return _write_scene(folder, scene, width, height, S, 30, meta, [(n, n) for n, _p in shots], jobs, workers)


def make_contract(root, frames=12, supersample=2, workers=1, only=None):
    """Write the seven scenes of the gallery contract into root/studio/ (root must not be art/captures).  The turntables
    get `frames` frames each (the real ones: hero_sword 6 x 90, lineup 120, armor_stand 4 x 120), the stills one frame per shot.
    Returns {scene id: folder}.  `only` is a list of scene ids to write."""
    S = supersample
    out = {}

    def want(sid):
        return not only or sid in only

    def turntable(sid, W, H, looks, subjects_for=None, swing=False, u=None):
        n = max(len(looks), frames)
        per = max(1, n // len(looks))
        folder = os.path.join(root, "studio", sid)
        meta, jobs = [], []
        for i in range(1, n + 1):
            k = min((i - 1) // per, len(looks) - 1)
            lid, caption, glow, strength = looks[k]
            meta.append({"index": i, "file": "%04d" % i, "look": lid, "caption": caption, "name": None})
            f = ((i - 1) % per) / float(per)
            theta = math.radians(35) * math.sin(2 * math.pi * (i - 1) / n) if swing else math.radians(28) + 2 * math.pi * f
            params = {"theta": theta, "glow": GLOWS[glow], "stripe_t": f, "strength": strength,
                      "ink": 2, "subjects": subjects_for}
            jobs.append((folder, i, W, H, S, params))
        out[sid] = _write_scene(folder, sid, W, H, S, 30, meta, [(l[0], l[1]) for l in looks], jobs, workers)

    if want("hero_sword"):
        turntable("hero_sword", 512, 512, HERO_LOOKS, [(0.5, 0.505, 0.385, 0, (1.0, 1.0, 1.0))])
    if want("lineup"):
        turntable("lineup", 1200, 400, [("lineup", "Every item its own look", "rainbow", 0.55)], _row(LINEUP_TINTS), swing=True)
    if want("armor_stand"):
        turntable("armor_stand", 480, 600, ARMOR_LOOKS, [(0.5, 0.5, 0.40, 0, (0.9, 0.95, 1.05))])
    if want("stills_hero"):
        out["stills_hero"] = _stills(root, "stills_hero", 1024, 1024, S, [
            ("banner_hero", {"theta": math.radians(34), "glow": GLOWS["rainbow"], "stripe_t": 0.45, "strength": 0.6, "ink": 5,
                             "ink_col": (120, 70, 200), "subjects": [(0.5, 0.5, 0.38, 0, (1, 1, 1))]}),
            ("logo", {"theta": math.radians(30), "glow": GLOWS["aurora"], "stripe_t": 0.5, "strength": 0.6, "ink": 6,
                      "ink_col": (80, 60, 190), "subjects": [(0.5, 0.5, 0.40, 0, (1, 1, 1))]})], workers)
    if want("stills_wide"):
        row = {"theta": math.radians(20), "glow": GLOWS["rainbow"], "stripe_t": 0.5, "strength": 0.5, "ink": 3, "ink_col": (120, 70, 200)}
        out["stills_wide"] = _stills(root, "stills_wide", 1200, 400, S, [
            ("universal_vanilla", dict(row, subjects=_row(LINEUP_TINTS))),
            ("universal_modded", dict(row, glow=GLOWS["gold"], subjects=_row(MODDED_TINTS)))], workers)
    if want("stills_tiles"):
        shots = []
        for i, (name, glow, strength, ink, ink_col) in enumerate(TILES):
            shots.append((name, {"theta": math.radians(25 + 6 * (i % 5)), "glow": GLOWS[glow], "stripe_t": 0.2 + 0.05 * (i % 12),
                                 "strength": strength, "ink": ink, "ink_col": ink_col, "subjects": [(0.5, 0.5, 0.36, 0, (1, 1, 1))]}))
        out["stills_tiles"] = _stills(root, "stills_tiles", 512, 512, S, shots, workers)
    if want("stills_gap"):
        base = {"theta": math.radians(48), "glow": GLOWS["rainbow"], "stripe_t": 0.5, "strength": 0.4, "ink": 0}
        shots = []
        for item, tint in (("sword", (1.0, 1.0, 1.0)), ("modded", (0.8, 1.05, 0.95))):
            for state in ("off", "on"):
                shots.append(("gap_%s_%s" % (item, state), dict(base, gaps=state == "off", subjects=[(0.5, 0.5, 0.37, 0, tint)])))
        out["stills_gap"] = _stills(root, "stills_gap", 800, 800, S, shots, workers)
    return out


def _size(text):
    """'256' (a square) or '480x600'."""
    try:
        parts = [int(v) for v in text.lower().split("x")]
        if len(parts) == 1:
            parts = parts * 2
        if len(parts) != 2 or min(parts) < 8:
            raise ValueError
        return tuple(parts)
    except ValueError:
        raise argparse.ArgumentTypeError("%r is not a size: use N (a square, e.g. 256) or WxH (e.g. 480x600), at least 8 px" % text)


def main(argv=None):
    ap = argparse.ArgumentParser(description=__doc__.split("\n\n")[0])
    ap.add_argument("dir", help="folder to write studio/<scene>/ into (a temp folder, never art/captures)")
    ap.add_argument("--scene", default="synth_turntable")
    ap.add_argument("--frames", type=int, default=None, help="frames of the turntable (default 36; with --contract 12 per scene)")
    ap.add_argument("--size", type=_size, default=(256, 256), help="output size: N (a square) or WxH (the files are S times larger)")
    ap.add_argument("--supersample", type=int, default=2)
    ap.add_argument("--fps", type=int, default=30)
    ap.add_argument("--workers", type=int, default=1)
    ap.add_argument("--contract", action="store_true", help="write the seven scenes of the gallery contract instead of one turntable")
    ap.add_argument("--only", nargs="*", help="with --contract: only these scene ids")
    args = ap.parse_args(argv)
    if args.supersample < 1 or args.frames is not None and args.frames < 1:
        ap.error("--supersample and --frames must be at least 1")
    if os.path.abspath(args.dir).startswith(os.path.join(ART, "captures")):
        sys.exit("error: refusing to write synthetic frames into art/captures")
    if args.contract:
        made = make_contract(args.dir, args.frames or 12, args.supersample, args.workers, args.only)
        for sid, folder in made.items():
            print("wrote scene %s to %s" % (sid, folder))
        return 0
    w, h = args.size
    frames = args.frames or 36
    folder = make_scene(args.dir, args.scene, frames, w, h, args.supersample, args.fps, args.workers)
    print("wrote %d synthetic frame pairs (%dx%d, supersample %d) to %s" % (frames, w, h, args.supersample, folder))
    return 0


if __name__ == "__main__":
    sys.exit(main())

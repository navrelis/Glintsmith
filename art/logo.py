"""The Glintsmith logo, the mod icon and the animated logo, made from studio renders.  Nothing here is drawn.

    py -B art/logo.py [--captures DIR] [--out DIR] [--strict] [--workers N] [--no-gif]

The picture is "the golden blade": an enchanted golden sword on the diagonal (fire glint, violet outline), rendered by the
in-game studio, on the cream studio backdrop with a soft glow of its own colours.

Inputs (studio frame contract, K / W pairs, see docs/STUDIO.md; scenes in src/studio/resources/scenes/):

    art/captures/studio/logo_concepts/   the still `concept_a` (512 x 512, supersample 2)       -> the stills
    art/captures/studio/logo_anim_a/     165 frames at 20 fps (256 x 256, supersample 4), the same
                                         pose with the glint flowing and the outline colour cycling,
                                         an exact 8.25 s loop of both (see logo_anim_a.json)       -> the GIF

How a picture is made (the same for the stills and every GIF frame):

1. MATTE      the K / W pair becomes a straight-alpha render (art/studio/matte.py).
2. FRAME      a square is cut centred on the subject's bounding box, its side the longest bbox side / `fill` (the
              subject fills that fraction of the square), and area-filtered to the output size (never enlarged: a
              square smaller than the output is taken at the output size).  An animation uses the square of its first
              frame for every frame, so the picture does not move.
3. BACKDROP   the cream radial gradient of art/studio/composite.py.
4. GLOW       the subject's own pixels, blurred: every pixel weighted by its alpha and by how colourful it is (max - min
              channel, full weight from `chroma` up, so the grey and dark parts do not glow), its premultiplied colour
              and that weight blurred (composite.blur_grid, with 8 extra bits so the faint tail has no steps), colour =
              blurred colour / blurred weight, pushed away from its grey (`saturate`) and towards white (`lift`), laid
              over the backdrop with opacity * weight ^ `gamma`.  Two layers: a tight one and a wide one (`glow`).  For
              the GIF the glow is the mean of 12 frames spread over the loop and stays still, so only the subject
              changes from frame to frame.
5. SUBJECT    composited over it (composite.compose, no drop shadow).

Outputs:

    art/logo/logo_512.png                              512 x 512   the logo
    src/main/resources/assets/glintsmith/icon.png      128 x 128   the mod icon (area filter of the 512 picture)
    art/logo/logo_32.png                                32 x 32    small version (area filter of the 512 picture)
    docs/curseforge/img/logo_animated.gif              256 x 256   the animated logo, seamless loop, under 2,000,000 bytes

The GIF uses art/studio/gifenc.py: one palette (`base_n` colours reserved for the static backdrop and glow, so its
gradient has no bands; the rest by median cut of the subject), ordered dither on the subject, delta frames, exact
centisecond delays (20 fps = 5 cs).

Settings: DEFAULTS below.  A `logo` section of gallery/cards.json that has a `glow` key (the format of this script)
overrides them key by key; an older section without `glow` (the stills_hero crop format) is ignored.

A render that is not there is never replaced by anything: the message names the file and that output is not written
(the stills and the GIF are independent; committed files stay as they are); the exit code is 0, or 1 with --strict.  A
scene that is there but broken ends the run with an error.  `--out DIR` writes the whole tree below DIR instead of the
repository (use it with synthetic scenes, so that the real files are never overwritten).

Python 3 standard library only.  Output is byte-identical from run to run, for any number of workers.
"""
import argparse
import copy
import os
import sys
import zlib

sys.dont_write_bytecode = True
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
sys.path.insert(0, os.path.join(HERE, "gallery"))
sys.path.insert(0, os.path.join(HERE, "studio"))

import common   # noqa: E402
import composite   # noqa: E402
import gifenc   # noqa: E402
import lib      # noqa: E402
import matte    # noqa: E402
import pipeline   # noqa: E402
import scene as studio_scene   # noqa: E402

P_LOGO = "art/logo/logo_512.png"
P_LOGO_SMALL = "art/logo/logo_32.png"
P_ICON = "src/main/resources/assets/glintsmith/icon.png"
P_GIF = "docs/curseforge/img/logo_animated.gif"
SIZES = ((P_LOGO, 512), (P_ICON, 128), (P_LOGO_SMALL, 32))
GIF_LIMIT = 2000000

DEFAULTS = {
    "scene": "logo_concepts",
    "frame": "concept_a",
    "fill": 0.86,
    "glow": [
        {"sigma": 0.03, "opacity": 0.75, "saturate": 1.4, "lift": 0.1, "gamma": 0.9, "chroma": 120},
        {"sigma": 0.10, "opacity": 0.55, "saturate": 1.5, "lift": 0.2, "gamma": 0.6, "chroma": 120},
    ],
    "animated": {"scene": "logo_anim_a", "size": 256, "dither": 10, "base_n": 120, "glow_frames": 12},
}


def settings(manifest):
    """DEFAULTS, overridden key by key by a `logo` section of the manifest in this script's format (one with `glow`)."""
    cfg = copy.deepcopy(DEFAULTS)
    sec = (manifest or {}).get("logo") or {}
    if "glow" in sec:
        for k, v in sec.items():
            if k == "animated" and isinstance(v, dict):
                cfg["animated"].update(v)
            elif k in cfg:
                cfg[k] = copy.deepcopy(v)
    return cfg


# ------------------------------------------------------------------------------------------------ framing

def square_of(m, fill, thr=8):
    """(centre x, centre y, side) of the square that holds the subject of a matte so that its longest bbox side
    fills `fill` of the side.  None for an empty matte."""
    box = composite.alpha_bbox(bytes(m.buf[3::4]), m.w, m.h, thr)
    if box is None:
        return None
    x0, y0, x1, y1 = box
    return (x0 + x1) / 2.0, (y0 + y1) / 2.0, int(round(max(x1 - x0, y1 - y0) / fill))


def frame(m, sq, size):
    """The square sq (from square_of) cut from the matte (outside is transparent) and area-filtered to size x size;
    a square smaller than size is taken at size (never enlarged)."""
    cx, cy, side = sq
    side = max(side, size)
    c = m.crop(int(round(cx - side / 2.0)), int(round(cy - side / 2.0)), side, side)
    return c if side == size else c.area(size, size)


# ------------------------------------------------------------------------------------------------ glow

def glow_grids(m, layer, F=4):
    """(colour grids [r, g, b], weight grid) of one glow layer of a matte, on a grid of F x F cells, blurred; all values
    carry 8 extra bits (x 256)."""
    w, h = m.w, m.h
    chroma = layer.get("chroma", 0)
    A = bytes(m.buf[3::4])
    r, g, b = m.buf[0::4], m.buf[1::4], m.buf[2::4]
    if chroma:
        A = bytes([(a * min(chroma, max(x, y, z) - min(x, y, z)) + chroma // 2) // chroma for a, x, y, z in zip(A, r, g, b)])
    P = [bytes([(v * a + 127) // 255 for v, a in zip(c, A)]) for c in (r, g, b)]
    gw, gh = w // F, h // F
    f2 = F * F

    def grid(plane):
        s = matte.box_sums(plane, w, h, F)
        return [[(v * 256 + f2 // 2) // f2 for v in s[gy * gw:(gy + 1) * gw]] for gy in range(gh)]
    sigma = layer["sigma"] * w / F
    return [composite.blur_grid(grid(p), sigma) for p in P], composite.blur_grid(grid(A), sigma)


def glow_layer(mattes, layer, F=4):
    """One glow layer of one or several mattes of the same pose (their grids summed): (RGB planes, weight plane),
    w * h bytes each, at the size of the mattes."""
    w, h = mattes[0].w, mattes[0].h
    gw, gh = w // F, h // F
    gp, ga = None, None
    for m in mattes:
        p, a = glow_grids(m, layer, F)
        if ga is None:
            gp, ga = p, a
        else:
            gp = [[[x + y for x, y in zip(r1, r2)] for r1, r2 in zip(c1, c2)] for c1, c2 in zip(gp, p)]
            ga = [[x + y for x, y in zip(r1, r2)] for r1, r2 in zip(ga, a)]
    peak = max(max(r) for r in ga) or 1
    cols = [[[min(255, (rp[x] * 255 + ra[x] // 2) // ra[x]) if ra[x] else 0 for x in range(gw)]
             for rp, ra in zip(gp[c], ga)] for c in range(3)]
    weight = [[(v * 255 + peak // 2) // peak for v in r] for r in ga]
    up = lambda grd: composite.upscale(grd, gw, gh, F, w, h, 0, h)
    return [up(c) for c in cols], up(weight)


def tint(rgb, layer):
    """Push the glow colour away from its grey by `saturate` and towards white by `lift` (integer arithmetic)."""
    sat = int(round(layer.get("saturate", 1.0) * 1000))
    lift = int(round(layer.get("lift", 0.0) * 1000))
    out = [bytearray(len(rgb[0])) for _ in range(3)]
    for i, (r, g, b) in enumerate(zip(*rgb)):
        y = (r * 3 + g * 6 + b) // 10
        for c, v in enumerate((r, g, b)):
            v = y + ((v - y) * sat) // 1000
            v = v + ((255 - v) * lift) // 1000
            out[c][i] = 0 if v < 0 else 255 if v > 255 else v
    return out


def lay_glow(base, rgb, weight, layer):
    """Blend the glow colour over an RGB base (bytearray, changed in place) through opacity * weight ^ gamma."""
    op, gamma = layer["opacity"], layer.get("gamma", 1.0)
    tab = bytes(int(round(((v / 255.0) ** gamma) * op * 255)) for v in range(256))
    a = weight.translate(tab)
    for ch in range(3):
        base[ch::3] = bytes([(x * (255 - al) + c * al + 127) // 255 for x, c, al in zip(base[ch::3], rgb[ch], a)])
    return base


def make_base(mattes, cfg, st):
    """The static part of a picture: backdrop + glow layers of the given mattes (one for a still), RGB bytearray."""
    w, h = mattes[0].w, mattes[0].h
    base = composite.backdrop(w, h, st)
    for layer in cfg["glow"]:
        rgb, weight = glow_layer(mattes, layer)
        lay_glow(base, tint(rgb, layer), weight, layer)
    return base


def to_canvas(rgb, w, h):
    out = lib.Canvas(w, h)
    for ch in range(3):
        out.buf[ch::4] = rgb[ch::3]
    out.buf[3::4] = bytes([255]) * (w * h)
    return out


# ------------------------------------------------------------------------------------------------ scenes

def _scene(captures, scene_id):
    """The scene, or (None, missing file relative to the repository)."""
    try:
        return studio_scene.load(captures.folder, scene_id), None
    except studio_scene.SceneError as e:
        if e.kind == "missing":
            return None, "art/captures/studio/%s/scene.json" % scene_id
        sys.exit("error: %s" % e)


def _missing_pair(sc, frames):
    for f in frames:
        for path in sc.pair(f):
            if not os.path.isfile(path):
                return "art/captures/studio/%s/%s" % (sc.id, os.path.basename(path))
    return None


def logo_picture(cfg, captures):
    """(512 x 512 Canvas of the logo, None) or (None, the repository-relative file that is missing)."""
    sc, missing = _scene(captures, cfg["scene"])
    if sc is None:
        return None, missing
    try:
        fr = studio_scene.find_frame(sc, cfg["frame"])
    except studio_scene.SceneError as e:
        sys.exit("error: %s" % e)
    missing = _missing_pair(sc, [fr])
    if missing:
        return None, missing
    try:
        m = pipeline.matte_frame(sc, fr)
    except (ValueError, OSError) as e:
        sys.exit("error: cannot matte studio frame %s of %s: %s" % (fr["file"], cfg["scene"], e))
    sq = square_of(m, cfg["fill"])
    if sq is None:
        sys.exit("error: studio frame %s of %s is empty" % (fr["file"], cfg["scene"]))
    m = frame(m, sq, 512)
    st = composite.style()
    rgb, _ = composite.compose(m, make_base([m], cfg, st), st, drop=False)
    return to_canvas(rgb, 512, 512), None


# ------------------------------------------------------------------------------------------------ the GIF

_W = {}


def _init_frames(sq, size):
    _W.update(sq=sq, size=size)


def _frame_task(args):
    """Pool task: (folder, file stem, supersample, w, h) -> the framed matte (size x size RGBA), zlib compressed."""
    folder, name, S, w, h = args
    blob, _ = pipeline.matte_task((folder, name, S, w, h))
    m = lib.Canvas(w, h, buf=bytearray(zlib.decompress(blob)))
    return zlib.compress(bytes(frame(m, _W["sq"], _W["size"]).buf), 1)


def _init_compose(base, size, st):
    _W.update(base=base, size=size, st=st)


def _compose_task(blob):
    size = _W["size"]
    m = lib.Canvas(size, size, buf=bytearray(zlib.decompress(blob)))
    rgb, _ = composite.compose(m, _W["base"], _W["st"], drop=False)
    rgb = bytes(rgb)
    return zlib.compress(rgb, 1), gifenc.frame_hist(rgb, _W["base"])


def logo_gif(cfg, captures, workers=1, log=None):
    """(GIF bytes, info) of the animated logo, or (None, the repository-relative file that is missing)."""
    an = cfg["animated"]
    sc, missing = _scene(captures, an["scene"])
    if sc is None:
        return None, missing
    missing = _missing_pair(sc, sc.frames)
    if missing:
        return None, missing
    size, n = int(an["size"]), len(sc.frames)
    first = pipeline.matte_frame(sc, sc.frames[0])
    sq = square_of(first, cfg["fill"])
    if sq is None:
        sys.exit("error: the first frame of %s is empty" % an["scene"])
    _log(log, "animated logo: matting %d frames of %s ..." % (n, an["scene"]))
    jobs = [(sc.dir, f["file"], sc.supersample, sc.width, sc.height) for f in sc.frames]
    with gifenc.Stage(workers, _init_frames, (sq, size)) as stage:
        mattes = stage.map(_frame_task, jobs, 2)
    k = max(1, int(an.get("glow_frames", 12)))
    sample = [lib.Canvas(size, size, buf=bytearray(zlib.decompress(mattes[(i * n) // k]))) for i in range(min(k, n))]
    st = composite.style()
    base = bytes(make_base(sample, cfg, st))
    _log(log, "  compositing ...")
    with gifenc.Stage(workers, _init_compose, (base, size, st)) as stage:
        res = stage.map(_compose_task, mattes, 2)
    hist = {}
    for _, hh in res:
        for key, v in hh.items():
            hist[key] = hist.get(key, 0) + v
    hist = {tuple(key): v for key, v in hist.items()}
    pal = gifenc.build_palette(hist, gifenc.colour_hist(base), (), int(an["base_n"]))
    _log(log, "  indexing (palette of %d colours, %d for the backdrop and glow) ..." % (len(pal), int(an["base_n"])))
    with gifenc.Stage(workers, gifenc.init_indexer, (pal, size, size, base, int(an["dither"]))) as stage:
        idx = stage.map(gifenc.index_task, [r[0] for r in res], 4)
    delays = gifenc.delays_cs(n, sc.fps, n)
    with gifenc.Stage(workers) as stage:
        data = gifenc.encode_gif(size, size, pal, [zlib.decompress(i) for i in idx], delays, stage.map_fn())
    if len(data) > GIF_LIMIT:
        sys.exit("error: %s would be %d bytes, over the %d byte limit; lower animated.dither or the scene's frame count"
                 % (P_GIF, len(data), GIF_LIMIT))
    return data, {"frames": n, "fps": sc.fps, "loop_cs": sum(delays), "bytes": len(data), "palette": len(pal)}


def _log(log, msg):
    if log:
        log(msg)


# ------------------------------------------------------------------------------------------------ build

def build(out, manifest, captures, log=None, workers=1, gif=True):
    """Fill out {relative path: bytes} with whatever can be built from the studio renders there are; returns the list
    of missing files (relative to the repository) so that the caller can report them."""
    cfg = settings(manifest)
    missing = []
    pic, miss = logo_picture(cfg, captures)
    if pic is None:
        missing.append(miss)
    else:
        for path, size in SIZES:
            out[path] = (pic if size == 512 else pic.area(size, size)).to_png()
    if gif:
        data, info = logo_gif(cfg, captures, workers, log)
        if data is None:
            missing.append(info)
        else:
            out[P_GIF] = data
            _log(log, "  %s: %d frames at %g fps (loop %d cs), %d bytes" % (P_GIF, info["frames"], info["fps"],
                                                                            info["loop_cs"], info["bytes"]))
    return missing


def main(argv=None):
    ap = argparse.ArgumentParser(description=__doc__.split("\n\n")[0])
    ap.add_argument("--manifest", default=common.DEFAULT_MANIFEST)
    ap.add_argument("--captures", default=common.DEFAULT_CAPTURES, help="folder that holds studio/<scene>/ (default art/captures)")
    ap.add_argument("--out", default=lib.ROOT, help="write the tree below this folder (default: the repository)")
    ap.add_argument("--strict", action="store_true", help="exit 1 if a render is missing")
    ap.add_argument("--workers", type=int, default=min(8, os.cpu_count() or 1), help="worker processes for the GIF (default up to 8)")
    ap.add_argument("--no-gif", action="store_true", help="only the stills (logo, icon, small logo)")
    args = ap.parse_args(argv)

    manifest = common.load_manifest(args.manifest)
    out = {}
    missing = build(out, manifest, common.Captures(args.captures), lambda m: print(m, flush=True), args.workers, not args.no_gif)
    for rel, data in sorted(out.items()):
        path = os.path.join(args.out, *rel.split("/"))
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, "wb") as f:
            f.write(data)
        print("wrote %s (%d bytes)" % (path, len(data)))
    for rel in missing:
        print("capture missing: %s (skipped, see art/gallery/SHOTLIST.md, \"Studio scenes\")" % rel)
    if missing and not out:
        print("Nothing built; the committed files were not touched.")
    return 1 if missing and args.strict else 0


if __name__ == "__main__":
    sys.exit(main())

"""Studio pipeline: scene frames -> mattes -> studio pictures -> a looping GIF under a size budget.  Standard library only.

    still(scene, frame, size, backdrop, caption)   -> lib.Canvas  one finished picture (or the transparent matte)
    matte_frame(scene, frame)                      -> lib.Canvas  the straight-alpha matte of one frame
    build_gif(scene, ...)                          -> (GIF bytes, info dict)

A GIF is built in four stages, each of which can use a pool of worker processes (`workers`):

    A  matte      K / W pair -> straight RGBA at the scene size (matte.py), kept zlib compressed
    B  composite  matte -> backdrop + contact shadow + drop shadow picture (+ caption bar), and the colour histogram
    C  index      one global palette (gifenc.py), Bayer dither on the subject, palette indices
    D  encode     delta frames and LZW for the frames that are kept, then the file

The static backdrop and the contact shadow come from ONE footprint for the whole turntable (the median of every
frame's), so the shadow does not wobble and the backdrop is identical in every frame.

The size budget (default 8 MB) is met by stepping down, in this order, until the file fits: the fps (the scene's, then
24, 20, 15: frames are dropped evenly, the loop keeps its length) and then the scale 1.0, 0.85, 0.75, 0.6 (at 15 fps).  If
even the last step is over, the build stops with a clear message.  The chosen step is reported in `info`.
"""
import os
import sys
import time
import zlib

sys.dont_write_bytecode = True
HERE = os.path.dirname(os.path.abspath(__file__))
ART = os.path.dirname(HERE)
for p in (HERE, ART):
    if p not in sys.path:
        sys.path.insert(0, p)

import composite   # noqa: E402
import gifenc      # noqa: E402
import lib         # noqa: E402
import matte       # noqa: E402

MB = 1024 * 1024
FPS_STEPS = (30, 24, 20, 15)
SCALE_STEPS = (1.0, 0.85, 0.75, 0.6)
MAX_GIF_FPS = 50                                  # a GIF delay is at least 2 cs; faster is clamped by viewers


# ------------------------------------------------------------------------------------------------ one frame

def matte_frame(sc, frame):
    """The straight-alpha matte of one frame of a scene (Canvas, width x height of the scene)."""
    kp, wp = sc.pair(frame)
    kc, wc = lib.Canvas.open(kp), lib.Canvas.open(wp)
    want = (sc.width * sc.supersample, sc.height * sc.supersample)
    if (kc.w, kc.h) != want or (wc.w, wc.h) != want:
        raise ValueError("frame %s: expected %dx%d, found K %dx%d and W %dx%d" % (
            frame["file"], want[0], want[1], kc.w, kc.h, wc.w, wc.h))
    return matte.matte_pair(kc.buf, wc.buf, kc.w, kc.h, sc.supersample)


def fit(canvas, size):
    """The matte fitted inside size (w, h) and centred on a transparent canvas of that size (area filter).
    Returns (canvas, scale, ox, oy)."""
    tw, th = size
    if (canvas.w, canvas.h) == (tw, th):
        return canvas, 1.0, 0, 0
    s = min(tw / float(canvas.w), th / float(canvas.h))
    rw, rh = max(1, int(round(canvas.w * s))), max(1, int(round(canvas.h * s)))
    small = canvas.area(rw, rh)
    out = lib.Canvas(tw, th)
    ox, oy = (tw - rw) // 2, (th - rh) // 2
    for y in range(rh):
        out.buf[((oy + y) * tw + ox) * 4:((oy + y) * tw + ox + rw) * 4] = small.buf[y * rw * 4:(y + 1) * rw * 4]
    return out, s, ox, oy


def still(sc, frame, size=None, backdrop=True, caption=False, shadows=True, st=None):
    """One picture of a frame: with the studio backdrop (opaque RGBA Canvas, caption bar under it when `caption`) or,
    with backdrop=False, the transparent RGBA matte.  size (w, h) fits the render inside that size."""
    st = st or composite.style()
    m = matte_frame(sc, frame)
    if size:
        m, _, _, _ = fit(m, size)
    if not backdrop:
        return m
    return composite.render(m, st, frame.get("caption", "") if caption else None, shadows)


# ------------------------------------------------------------------------------------------------ stage A

def matte_task(args):
    folder, name, S, w, h = args
    kc = lib.Canvas.open(os.path.join(folder, "%s_k.png" % name))
    wc = lib.Canvas.open(os.path.join(folder, "%s_w.png" % name))
    if (kc.w, kc.h) != (w * S, h * S) or (wc.w, wc.h) != (w * S, h * S):
        raise ValueError("frame %s: expected %dx%d, found K %dx%d and W %dx%d" % (name, w * S, h * S, kc.w, kc.h, wc.w, wc.h))
    m = matte.matte_pair(kc.buf, wc.buf, kc.w, kc.h, S)
    fp = composite.footprint(bytes(m.buf[3::4]), m.w, m.h)
    return zlib.compress(bytes(m.buf), 1), fp


# ------------------------------------------------------------------------------------------------ stage B

_B = {}


def init_compose(base, size, native, st, caption_bar):
    _B.update(base=base, size=size, native=native, st=st, bar=caption_bar, bars={})


def compose_task(args):
    blob, caption = args
    size, native = _B["size"], _B["native"]
    m = lib.Canvas(native[0], native[1], buf=bytearray(zlib.decompress(blob)))
    m, _, _, _ = fit(m, size)
    pic, _ = composite.compose(m, _B["base"], _B["st"], drop=True)
    pic = bytes(pic)
    if _B["bar"]:
        bars = _B["bars"]
        if caption not in bars:
            bh = _B["st"]["bar"]
            bars[caption] = composite.bar(size[0], bh["height"], caption, bh["scale"], bh["colour"]).rgb_bytes()
        frame = pic + bars[caption]
    else:
        frame = pic
    return zlib.compress(frame, 1), gifenc.frame_hist(pic, _B["base"])


# ------------------------------------------------------------------------------------------------ the GIF

def _log(log, msg):
    if log:
        log(msg)


def prepare(sc, mattes, native_fp, size, scale, caption, st, workers, dither, log):
    """Stages B and C for one scale: returns (palette, index blobs, (width, height of the GIF))."""
    pw, ph = max(2, int(round(size[0] * scale))), max(2, int(round(size[1] * scale)))
    native = (sc.width, sc.height)
    s = min(pw / float(native[0]), ph / float(native[1]))
    ox, oy = (pw - int(round(native[0] * s))) // 2, (ph - int(round(native[1] * s))) // 2
    fp = composite.scale_footprint(native_fp, s, ox, oy) if (pw, ph) != native else native_fp
    base = bytes(composite.make_base(pw, ph, fp, st))
    bh = st["bar"]["height"] if caption else 0
    t = time.time()
    _log(log, "  composite %dx%d (scale %.2f), %d frames ..." % (pw, ph, scale, len(sc.frames)))
    with gifenc.Stage(workers, init_compose, (base, (pw, ph), native, st, bool(caption))) as stage:
        res = stage.map(compose_task, [(m, f.get("caption", "") or "") for m, f in zip(mattes, sc.frames)], 2,
                        lambda i, n: _log(log, "    composite %d / %d" % (i, n)))
    blobs = [r[0] for r in res]
    hist = {}
    for _, h in res:
        for k, v in h.items():
            hist[k] = hist.get(k, 0) + v
    hist = {tuple(k): v for k, v in hist.items()}
    reserve = [lib.PAGE[:3], lib.BORDER_DARK[:3], lib.BORDER[:3], lib.colour(st["bar"]["colour"])[:3], (255, 255, 255)] if caption else []
    pal = gifenc.build_palette(hist, gifenc.colour_hist(base), reserve)
    _log(log, "  composite done in %.1f s; palette of %d colours from %d histogram colours" % (time.time() - t, len(pal), len(hist)))
    t = time.time()
    with gifenc.Stage(workers, gifenc.init_indexer, (pal, pw, ph, base, dither)) as stage:
        idx = stage.map(gifenc.index_task, blobs, 4, lambda i, n: _log(log, "    index %d / %d" % (i, n)))
    _log(log, "  indexed in %.1f s" % (time.time() - t))
    return pal, idx, (pw, ph + bh)


def kept_frames(n, scene_fps, fps):
    """Indexes of the frames kept to play at `fps` instead of the scene's fps: spread evenly over the loop."""
    m = max(1, min(n, int(round(n * fps / scene_fps))))
    return [(i * n) // m for i in range(m)]


def fps_ladder(scene_fps, wanted=None):
    start = min(float(wanted or scene_fps), scene_fps, MAX_GIF_FPS)
    start = int(start) if start == int(start) else start
    return [start] + [f for f in FPS_STEPS if f < start]


def build_gif(sc, size=None, caption=True, budget_mb=8.0, fps=None, workers=1, st=None, dither=10, log=None):
    """The turntable of a scene as a looping GIF.  Returns (bytes, info).

    size: (w, h) of the picture (the caption bar is added below); default the scene's.  Steps down to fit the budget
    (see the module docstring); exits with an error message if it cannot."""
    st = st or composite.style()
    size = tuple(size) if size else (sc.width, sc.height)
    n = len(sc.frames)
    t0 = time.time()
    _log(log, "stage A: matting %d frame pairs (%dx%d, supersample %d) ..." % (n, sc.width, sc.height, sc.supersample))
    with gifenc.Stage(workers) as stage:
        res = stage.map(matte_task, [(sc.dir, f["file"], sc.supersample, sc.width, sc.height) for f in sc.frames], 2,
                        lambda i, k: _log(log, "    matte %d / %d" % (i, k)))
    mattes = [r[0] for r in res]
    native_fp = composite.merge_footprints([r[1] for r in res])
    _log(log, "  matted in %.1f s" % (time.time() - t0))
    ladder = [(f, 1.0) for f in fps_ladder(sc.fps, fps)]
    ladder += [(ladder[-1][0], s) for s in SCALE_STEPS[1:]]
    limit = budget_mb * MB
    done, tried = None, []
    for f, scale in ladder:
        if done is None or done[0] != scale:
            _log(log, "scale %.2f:" % scale)
            done = (scale,) + prepare(sc, mattes, native_fp, size, scale, caption, st, workers, dither, log)
        _, pal, idx, (gw, gh) = done
        keep = kept_frames(n, sc.fps, f)
        delays = gifenc.delays_cs(len(keep), sc.fps, n)
        t = time.time()
        with gifenc.Stage(workers) as stage:
            data = gifenc.encode_gif(gw, gh, pal, [zlib.decompress(idx[i]) for i in keep], delays, stage.map_fn())
        tried.append("%g fps / scale %.2f: %.2f MB" % (f, scale, len(data) / MB))
        _log(log, "  encode %g fps, scale %.2f: %d frames, %.2f MB in %.1f s (budget %.1f MB)" % (
            f, scale, len(keep), len(data) / MB, time.time() - t, budget_mb))
        if len(data) <= limit:
            return data, {"fps": f, "scale": scale, "frames": len(keep), "source_frames": n, "width": gw, "height": gh,
                          "bytes": len(data), "tried": tried, "palette": len(pal), "loop_cs": sum(delays),
                          "seconds": time.time() - t0}
    sys.exit("error: the GIF does not fit in %.1f MB even at %g fps and scale %.2f (%s).  Use a smaller --size, a "
             "shorter turntable (fewer frames) or raise --budget-mb." % (budget_mb, ladder[-1][0], ladder[-1][1], "; ".join(tried)))

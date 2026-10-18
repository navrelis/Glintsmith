"""Studio GIF encoder: one global palette, ordered dither on the subject only, delta frames.  Standard library only.

Made for a turntable on a static backdrop (see composite.py): a clip whose pictures are the same backdrop with a subject
that moves.  The pieces, in the order a clip goes through them:

1. HISTOGRAM   the colours of all frames, reduced to 6 bits per channel, counting only the pixels that differ from the
               static base picture (the subject, its glow and drop shadow); the base is counted separately.
2. PALETTE     255 colours by median cut (imgio.median_cut) + the transparent index 255.  A slice of the palette
               (`base_n`, default 48) is cut from the exact backdrop colours so that the cream gradient stays smooth,
               plus a few reserved exact colours (the caption bar: bar cream, brass lines, ink).
3. INDEXING    every pixel becomes a palette index.  The BACKDROP (pixels equal to the base picture) gets the nearest
               palette colour with no dither, so it is identical in every frame and stays still.  The SUBJECT gets an
               8 x 8 Bayer ordered dither, the same matrix anchored to the picture in every frame (no error diffusion), so
               the pattern does not crawl.  The nearest-colour lookup is cached in a dict keyed on the dithered 6 bit
               colour, so the cost per pixel is one dict lookup; the cache is filled by a nearest-colour search that uses
               a coarse colour-cell grid of candidates (exact result, a fraction of 255 comparisons).
4. DELTAS      a frame is stored as the bounding box of the pixels that differ from the previous OUTPUT frame, the
               pixels inside it that did not change are the transparent index, disposal "do not dispose".  With `lossy`
               (a tolerance, weighted squared colour distance) a pixel whose new palette colour is that close to the
               colour the viewer already shows there is left as it is, so it joins the transparent runs (lossy_frames).
5. DELAYS      GIF delays are centiseconds: the loop length (frames / fps) is spread exactly over the frames, so 30 fps
               gives 3, 3, 4, 3, 3, 4 ...  The loop runs forever.

The heavy per-frame stages (indexing, LZW) can run in a multiprocessing pool (`Stage`); results do not depend on it, the
output is byte-identical for any number of workers.

    Palette, build_palette(...), Indexer, frame_hist(...), changed_flags(...), delta_region(...), lossy_frames(...), encode_gif(...)
    delays_cs(n_frames_kept, fps, n_frames_total)     centisecond delays that add up to the loop length
"""
import struct
import sys
import os
import zlib
from collections import Counter
from itertools import compress

sys.dont_write_bytecode = True
HERE = os.path.dirname(os.path.abspath(__file__))
ART = os.path.dirname(HERE)
if ART not in sys.path:
    sys.path.insert(0, ART)

import imgio   # noqa: E402

TRANSPARENT = 255
_NZ = bytes([0]) + bytes([1]) * 255
_MASK = bytes([0]) + bytes([255]) * 255                       # 0 -> 0, else 255
_VALID = bytes([255]) * 255 + bytes([0])                      # 255 (no snap) -> 0, else 255
_EXP6 = bytes((((v >> 2) << 2) | (v >> 6)) for v in range(256))   # a 6 bit quantisation, back at 8 bits
WEIGHTS = (3, 4, 2)                                           # colour distance weights (r, g, b)


def bayer8():
    """The 8 x 8 Bayer threshold matrix, values 0..63."""
    m = [[0, 2], [3, 1]]
    while len(m) < 8:
        n = len(m)
        m = [[4 * m[y % n][x % n] + [[0, 2], [3, 1]][y // n][x // n] for x in range(2 * n)] for y in range(2 * n)]
    return m


# ------------------------------------------------------------------------------------------------ palette

class Palette:
    """255 colours and a fast, exact nearest-colour lookup: `index[key]` with key = r | g << 8 | b << 16."""

    def __init__(self, colours):
        self.colours = [tuple(c) for c in colours][:255]
        self._cells = {}
        self.index = _Lookup(self)

    def candidates(self, cell):
        """Palette entries that can be the nearest to some colour of a 32 x 32 x 32 cell (a superset of the winners)."""
        got = self._cells.get(cell)
        if got is None:
            lo = [c * 32 for c in cell]
            hi = [c * 32 + 31 for c in cell]
            mins, maxs = [], []
            for col in self.colours:
                dmin = dmax = 0
                for ch in range(3):
                    v = col[ch]
                    a, b = abs(v - lo[ch]), abs(v - hi[ch])
                    far = a if a > b else b
                    near = 0 if lo[ch] <= v <= hi[ch] else (a if a < b else b)
                    dmin += WEIGHTS[ch] * near * near
                    dmax += WEIGHTS[ch] * far * far
                mins.append(dmin)
                maxs.append(dmax)
            limit = min(maxs)
            got = self._cells[cell] = [i for i, d in enumerate(mins) if d <= limit]
        return got

    def nearest(self, r, g, b):
        cols = self.colours
        best, bd = 0, None
        wr, wg, wb = WEIGHTS
        for i in self.candidates((r >> 5, g >> 5, b >> 5)):
            c = cols[i]
            d = wr * (c[0] - r) ** 2 + wg * (c[1] - g) ** 2 + wb * (c[2] - b) ** 2
            if bd is None or d < bd:
                best, bd = i, d
        return best


class _Snap(dict):
    """key -> palette index when the nearest colour is close enough to be used as it is (no dither), else 255."""

    def __init__(self, pal, tol):
        super().__init__()
        self.pal, self.tol = pal, tol

    def __missing__(self, key):
        r, g, b = key & 255, (key >> 8) & 255, key >> 16
        i = self.pal.nearest(r, g, b)
        c = self.pal.colours[i]
        d = WEIGHTS[0] * (c[0] - r) ** 2 + WEIGHTS[1] * (c[1] - g) ** 2 + WEIGHTS[2] * (c[2] - b) ** 2
        v = self[key] = i if d <= self.tol else TRANSPARENT
        return v


class _Lookup(dict):
    def __init__(self, pal):
        super().__init__()
        self.pal = pal

    def __missing__(self, key):
        v = self.pal.nearest(key & 255, (key >> 8) & 255, key >> 16)
        self[key] = v
        return v


def colour_hist(rgb):
    """{(r, g, b): count} of an RGB bytes object."""
    return {tuple(k[0]): v for k, v in Counter(struct.iter_unpack("3s", rgb)).items()}


def build_palette(hist, base_hist, reserve=(), base_n=48, n_colours=255):
    """The palette (a list of (r, g, b), 255 colours by default, `n_colours` at most): `reserve` colours exactly, up to
    `base_n` colours cut from the exact backdrop colours (`base_hist`), the rest by median cut of `hist` (both
    {(r, g, b): count})."""
    n_colours = max(1, min(255, n_colours))
    pal = []
    for c in reserve:
        c = tuple(c[:3])
        if c not in pal:
            pal.append(c)
    if base_hist and base_n:
        colours, _ = imgio.median_cut(base_hist, min(base_n, n_colours - len(pal)))
        for c in colours:
            if c not in pal:
                pal.append(c)
    if hist and len(pal) < n_colours:
        colours, _ = imgio.median_cut(hist, n_colours - len(pal))
        for c in colours:
            if c not in pal:
                pal.append(c)
    return pal[:n_colours]


# ------------------------------------------------------------------------------------------------ per frame helpers

def changed_flags(pic, base):
    """bytes, one per pixel: 1 where the RGB picture differs from the RGB base (both bytes, same length)."""
    n = len(pic) // 3
    x = (int.from_bytes(pic, "little") ^ int.from_bytes(base, "little")).to_bytes(len(pic), "little")
    return (int.from_bytes(x[0::3], "little") | int.from_bytes(x[1::3], "little")
            | int.from_bytes(x[2::3], "little")).to_bytes(n, "little").translate(_NZ)


def frame_hist(pic, base):
    """{6 bit colour as 3 bytes: count} of the pixels of `pic` that differ from `base` (colours reduced to 6 bits)."""
    flags = changed_flags(pic, base)
    if flags.find(1) < 0:
        return {}
    return {k[0]: v for k, v in Counter(compress(struct.iter_unpack("3s", pic.translate(_EXP6)), flags)).items()}


def flag_box(flags, w):
    """(x0, y0, x1, y1) of the 1 flags of a w wide bytes plane, or None."""
    first = flags.find(1)
    if first < 0:
        return None
    y0, y1 = first // w, flags.rfind(1) // w + 1
    acc = 0
    for y in range(y0, y1):
        acc |= int.from_bytes(flags[y * w:(y + 1) * w], "little")
    cb = acc.to_bytes(w, "little")
    return cb.find(1), y0, cb.rfind(1) + 1, y1


class Indexer:
    """Turns RGB frames into palette indices (see the module docstring, step 3).

    base:  RGB bytes of the static picture (width x pic_h); frames are width x (pic_h + bar rows) RGB, the rows below
           pic_h (a caption bar) are mapped flat, with no dither.
    step:  dither amplitude in 8 bit levels (offsets run about -step/2 .. +step/2); 0 turns the dither off.
    snap:  a subject pixel whose exact colour is this close (weighted squared distance) to a palette colour takes that
           colour with no dither, so flat colours stay flat and only real gradients are dithered; 0 = always dither."""

    def __init__(self, palette, width, pic_h, base, step=10, snap=110):
        self.pal = palette
        self.lookup = palette.index
        self.snap = _Snap(palette, snap)
        self.w, self.ph = width, pic_h
        self.base = bytes(base)
        self.step = step
        m = bayer8()
        self.offsets = [[((2 * m[y][x] - 63) * step) // 128 for x in range(8)] for y in range(8)]
        self.tables = {}
        for o in {o for row in self.offsets for o in row}:
            self.tables[o] = bytes(_EXP6[min(255, max(0, v + o))] for v in range(256))
        self.base_idx = self.flat(self.base)
        self.bar_cache = {}

    def keys(self, rgb):
        n = len(rgb) // 3
        buf = bytearray(4 * n)
        buf[0::4] = rgb[0::3]
        buf[1::4] = rgb[1::3]
        buf[2::4] = rgb[2::3]
        return struct.unpack("<%dI" % n, buf)

    def flat(self, rgb):
        """Nearest palette index of every pixel, no dither."""
        return bytes(map(self.lookup.__getitem__, self.keys(rgb)))

    def index(self, frame):
        """Index bytes (width x total height) of one RGB frame."""
        w, ph = self.w, self.ph
        pic, bar = frame[:3 * w * ph], frame[3 * w * ph:]
        flags = changed_flags(pic, self.base)
        out = bytearray(self.base_idx)
        box = flag_box(flags, w)
        if box:
            x0, y0, x1, y1 = box
            span = x1 - x0
            get = self.lookup.__getitem__
            for y in range(y0, y1):
                seg = pic[(y * w + x0) * 3:(y * w + x1) * 3]
                planes = [seg[0::3], seg[1::3], seg[2::3]]
                dith = [bytearray(span) for _ in range(3)]
                row_off = self.offsets[y & 7]
                for p in range(min(8, span)):
                    t = self.tables[row_off[(x0 + p) & 7]]
                    for c in range(3):
                        dith[c][p::8] = planes[c][p::8].translate(t)
                buf = bytearray(4 * span)
                buf[0::4], buf[1::4], buf[2::4] = dith
                d = bytes(map(get, struct.unpack("<%dI" % span, buf)))
                if self.snap.tol:
                    sn = bytes(map(self.snap.__getitem__, self.keys(seg)))
                    di = int.from_bytes(d, "little")
                    d = (di ^ ((di ^ int.from_bytes(sn, "little")) & int.from_bytes(sn.translate(_VALID), "little"))
                         ).to_bytes(span, "little")
                b = self.base_idx[y * w + x0:y * w + x1]
                m = int.from_bytes(flags[y * w + x0:y * w + x1].translate(_MASK), "little")
                bi = int.from_bytes(b, "little")
                out[y * w + x0:y * w + x1] = (bi ^ ((int.from_bytes(d, "little") ^ bi) & m)).to_bytes(span, "little")
        if bar:
            idx = self.bar_cache.get(bar)
            if idx is None:
                idx = self.bar_cache[bar] = self.flat(bar)
            out += idx
        return bytes(out)


# ------------------------------------------------------------------------------------------------ deltas, delays, GIF

def delta_region(cur, prev, w):
    """(x, y, cw, ch, data): the box of `cur` (index bytes, w wide) that differs from `prev`, with the unchanged
    pixels inside it set to the transparent index.  prev None gives the whole frame.  No difference: a 1 x 1 transparent pixel."""
    h = len(cur) // w
    if prev is None:
        return 0, 0, w, h, cur
    diff = (int.from_bytes(cur, "little") ^ int.from_bytes(prev, "little")).to_bytes(len(cur), "little")
    box = flag_box(diff.translate(_NZ), w)
    if box is None:
        return 0, 0, 1, 1, bytes([TRANSPARENT])
    x0, y0, x1, y1 = box
    cw, ch = x1 - x0, y1 - y0
    rows_c = b"".join([cur[y * w + x0:y * w + x1] for y in range(y0, y1)])
    rows_d = b"".join([diff[y * w + x0:y * w + x1] for y in range(y0, y1)])
    m = int.from_bytes(rows_d.translate(_MASK), "little")
    t = int.from_bytes(bytes([TRANSPARENT]) * len(rows_c), "little")
    return x0, y0, cw, ch, (t ^ ((int.from_bytes(rows_c, "little") ^ t) & m)).to_bytes(len(rows_c), "little")


def delays_cs(kept, fps, total):
    """Centisecond delays for `kept` frames standing in for `total` source frames played at `fps`: they add up to
    round(100 * total / fps), spread as evenly as possible (30 fps: 3, 3, 4, ...), never below 2 (viewers clamp less)."""
    loop_cs = max(2 * kept, int(round(100.0 * total / fps)))
    return [(loop_cs * (i + 1)) // kept - (loop_cs * i) // kept for i in range(kept)]


def lossy_frames(palette, idx_frames, tol):
    """The index frames as the viewer will see them when a pixel whose new colour is within `tol` (weighted squared
    distance, WEIGHTS) of the colour already shown there keeps that colour: such pixels become unchanged, so delta_region
    makes them transparent and the LZW runs get longer.  Frame 0 is exact; each frame is compared with the previous
    OUTPUT frame, so a slow drift is refreshed as soon as it exceeds `tol` (no accumulation).  tol 0 returns the input."""
    if tol <= 0 or len(idx_frames) < 2:
        return list(idx_frames)
    cols = [tuple(c) for c in palette][:255]
    close = bytearray(65536)                                  # [new | shown << 8] = 255 when the two colours are within tol
    for a, ca in enumerate(cols):
        for b, cb in enumerate(cols):
            if WEIGHTS[0] * (ca[0] - cb[0]) ** 2 + WEIGHTS[1] * (ca[1] - cb[1]) ** 2 + WEIGHTS[2] * (ca[2] - cb[2]) ** 2 <= tol:
                close[a | b << 8] = 255
    close = bytes(close)
    out = [idx_frames[0]]
    prev = idx_frames[0]
    for cur in idx_frames[1:]:
        pair = bytearray(2 * len(cur))
        pair[0::2] = cur
        pair[1::2] = prev
        keep = int.from_bytes(bytes(map(close.__getitem__, memoryview(pair).cast("H"))), "little")
        c, p = int.from_bytes(cur, "little"), int.from_bytes(prev, "little")
        prev = (c ^ ((c ^ p) & keep)).to_bytes(len(cur), "little")
        out.append(prev)
    return out


def encode_gif(width, height, palette, idx_frames, delays, map_fn=map, lossy=0):
    """The GIF bytes from palette (list of (r, g, b), up to 255), full-frame index bytes and delays (cs).

    Frames are delta coded against the previous one (see delta_region), after the `lossy` tolerance (see lossy_frames,
    0 = exact); `map_fn` runs the LZW stage (a pool's map to use several processes).  Returns bytes."""
    idx_frames = lossy_frames(palette, idx_frames, lossy)
    regions, prev = [], None
    for cur in idx_frames:
        regions.append(delta_region(cur, prev, width))
        prev = cur
    lz = list(map_fn(imgio.lzw_encode, [r[4] for r in regions]))
    frames = [dict(x=r[0], y=r[1], w=r[2], h=r[3], lzw=l, delay=d, trans=TRANSPARENT, disposal=1)
              for r, l, d in zip(regions, lz, delays)]
    pal = list(palette) + [(0, 0, 0)] * (255 - len(palette)) + [(0, 0, 0)]
    return imgio.gif_bytes(width, height, pal, frames, 0)


# ------------------------------------------------------------------------------------------------ worker stages

_STATE = {}


def init_indexer(palette_colours, width, pic_h, base, step, snap=110):
    """Pool initializer: builds this process's Indexer."""
    _STATE["indexer"] = Indexer(Palette(palette_colours), width, pic_h, base, step, snap)


def index_task(blob):
    """Pool task: a zlib compressed RGB frame in, a zlib compressed index frame out."""
    return zlib.compress(_STATE["indexer"].index(zlib.decompress(blob)), 1)


class Stage:
    """A group of worker processes with an initializer, or the same thing in this process when workers <= 1.

        with Stage(workers, init_fn, init_args) as st:
            results = st.map(fn, items, chunksize=2)      # ordered; progress(done, total) is called now and then
    """

    def __init__(self, workers=1, initializer=None, initargs=()):
        self.workers, self.initializer, self.initargs = workers, initializer, initargs
        self.pool = None

    def __enter__(self):
        if self.workers > 1:
            import multiprocessing
            self.pool = multiprocessing.Pool(self.workers, self.initializer, self.initargs)
        elif self.initializer:
            self.initializer(*self.initargs)
        return self

    def __exit__(self, *exc):
        if self.pool:
            self.pool.close() if exc[0] is None else self.pool.terminate()
            self.pool.join()
        return False

    def map(self, fn, items, chunksize=1, progress=None):
        items = list(items)
        out = []
        it = self.pool.imap(fn, items, chunksize) if self.pool else map(fn, items)
        for i, r in enumerate(it):
            out.append(r)
            if progress and (i + 1) % max(1, len(items) // 10) == 0:
                progress(i + 1, len(items))
        return out

    def map_fn(self):
        """A map() for encode_gif: the pool's, or the builtin."""
        return (lambda fn, items: self.pool.map(fn, items, 4)) if self.pool else map

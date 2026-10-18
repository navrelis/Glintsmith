"""Studio composite: puts a matted RGBA render on the cream studio backdrop, with soft shadows.  Standard library only.

A "studio render" look, like a Blockbench / Blender product shot:

* the BACKDROP is a cream radial gradient built from the palette in lib.py (the light centre `lib.PANEL`, the edges a
  little darker, `lib.PAGE` towards `lib.PANEL_MID`); it is subtle on purpose;
* the CONTACT shadow is a flat ellipse under the subject, sized from the footprint of its alpha (the lowest rows),
  blurred, about 25 % opaque, in a warm dark brown;
* the optional DROP shadow is the subject's alpha blurred and shifted down-right, about 15 % opaque;
* the optional CAPTION BAR under the picture is the one of the showcase GIF (cream, two brass lines on top, text in the
  vanilla font, the bar of the old legacy/gif.py).

Everything is in the style dict (`DEFAULT_STYLE`, override with `style({...})`).  Sizes are fractions of the picture
width, so the same style looks the same at any size.  Blurs are three box blurs in a row (a Gaussian approximation) done
on a grid reduced by 4 and scaled back up bilinearly, which keeps them fast in pure Python.  All arithmetic is integer
(or a handful of IEEE float operations that behave identically everywhere), so the output is byte-identical from run to run.

    style(overrides)                         -> merged style dict
    backdrop(w, h, st)                       -> RGB bytearray, the gradient
    footprint(alpha, w, h, st)               -> {"bbox", "cx", "bottom", "width"} of a subject, or None
    merge_footprints(list)                   -> one steady footprint for a whole turntable
    make_base(w, h, fp, st)                  -> RGB bytearray: backdrop + contact shadow (the part that never moves)
    compose(matte, base, st, drop=True)      -> (RGB bytearray, changed bbox): subject + drop shadow over the base
    render(matte, st, ...)                   -> lib.Canvas: a finished picture (backdrop, shadows, optional caption bar)
    bar(width, height, text, scale, colour)  -> lib.Canvas: the caption bar
    place_on_canvas(canvas, matte, rect, shadow, st) -> composite onto a card (no backdrop, no frame)
"""
import copy
import os
import sys
import unicodedata
from itertools import accumulate

sys.dont_write_bytecode = True
HERE = os.path.dirname(os.path.abspath(__file__))
ART = os.path.dirname(HERE)
if ART not in sys.path:
    sys.path.insert(0, ART)
if HERE not in sys.path:
    sys.path.insert(0, HERE)

import lib      # noqa: E402
import matte    # noqa: E402

WARM_DARK = lib.BRASS[0][:3]            # 46291a: the shadow colour, a warm dark brown from the palette

DEFAULT_STYLE = {
    "backdrop": {"centre": lib.PANEL[:3], "edge": lib.mix(lib.PAGE, lib.PANEL_MID, 0.6)[:3],
                 "cx": 0.5, "cy": 0.44, "reach": 0.80},
    "contact": {"opacity": 0.25, "colour": WARM_DARK, "blur": 0.026, "ry": 0.17, "min_width": 0.42,
                "gap": 0.010, "band": 0.07, "max_width": 0.46, "grid": 4},
    "drop": {"opacity": 0.15, "colour": WARM_DARK, "dx": 0.014, "dy": 0.022, "blur": 0.020, "grid": 4},
    "bar": {"height": 36, "scale": 2, "colour": "text"},
}


def style(overrides=None):
    """DEFAULT_STYLE with `overrides` (a nested dict, same keys) merged in."""
    out = copy.deepcopy(DEFAULT_STYLE)
    for group, values in (overrides or {}).items():
        out.setdefault(group, {}).update(values)
    return out


# ------------------------------------------------------------------------------------------------ backdrop

def backdrop(w, h, st=None):
    """The cream radial gradient as an RGB bytearray (w * h * 3)."""
    bd = (st or DEFAULT_STYLE)["backdrop"]
    c0, c1 = bd["centre"], bd["edge"]
    table = [bytes((c0[0] + (c1[0] - c0[0]) * t // 256, c0[1] + (c1[1] - c0[1]) * t // 256,
                    c0[2] + (c1[2] - c0[2]) * t // 256)) for t in range(257)]
    cx, cy = bd["cx"] * w, bd["cy"] * h
    rx, ry = bd["reach"] * w, bd["reach"] * h * 1.1
    dx2 = [((x + 0.5 - cx) / rx) ** 2 for x in range(w)]
    out = bytearray()
    for y in range(h):
        dy2 = ((y + 0.5 - cy) / ry) ** 2
        out += b"".join([table[min(256, int((d + dy2) * 256))] for d in dx2])
    return out


# ------------------------------------------------------------------------------------------------ blur

def _blur_rows(rows, r):
    k = 2 * r + 1
    pad = [0] * r
    half = k >> 1
    out = []
    for row in rows:
        pref = list(accumulate(pad + row + pad, initial=0))
        out.append([(b - a + half) // k for a, b in zip(pref, pref[k:])])
    return out


def blur_grid(rows, sigma, passes=3):
    """Gaussian-like blur of a grid (list of lists of ints): `passes` box blurs per axis, sigma in grid cells."""
    if sigma <= 0:
        return rows
    r = max(1, int(round((((4.0 * sigma * sigma + 1.0) ** 0.5) - 1) / 2)))
    for _ in range(passes):
        rows = _blur_rows(rows, r)
    cols = [list(c) for c in zip(*rows)]
    for _ in range(passes):
        cols = _blur_rows(cols, r)
    return [list(c) for c in zip(*cols)]


def upscale(grid, gw, gh, F, w, h, y0, y1, x0=0, x1=None):
    """Bilinear enlargement of a grid (cells of F x F pixels, sample at the cell centre) into a w * h bytes plane;
    only rows y0..y1 and columns x0..x1 are filled, the rest is 0.  Values 0..255."""
    x1 = w if x1 is None else x1
    plane = bytearray(w * h)
    f2 = 2 * F
    cols = []
    for x in range(x0, x1):
        pos = 2 * x + 1 - F
        g0 = pos // f2
        fx = pos - g0 * f2
        cols.append((min(max(g0, 0), gw - 1), min(max(g0 + 1, 0), gw - 1), f2 - fx, fx))
    half = 2 * F * F
    div = 4 * F * F
    for y in range(max(0, y0), min(h, y1)):
        pos = 2 * y + 1 - F
        g0 = pos // f2
        fy = pos - g0 * f2
        ra = grid[min(max(g0, 0), gh - 1)]
        rb = grid[min(max(g0 + 1, 0), gh - 1)]
        v = [a * (f2 - fy) + b * fy for a, b in zip(ra, rb)]
        plane[y * w + x0:y * w + x1] = bytes([(v[a] * wa + v[b] * wb + half) // div for a, b, wa, wb in cols])
    return plane


# ------------------------------------------------------------------------------------------------ footprint

def _threshold_table(thr):
    return bytes(0 if v < thr else 1 for v in range(256))


def alpha_bbox(alpha, w, h, thr=16):
    """(x0, y0, x1, y1) of the pixels with alpha >= thr (x1, y1 exclusive), or None when there are none."""
    flags = alpha.translate(_threshold_table(thr))
    first = flags.find(b"\x01")
    if first < 0:
        return None
    y0, y1 = first // w, flags.rfind(b"\x01") // w + 1
    acc = 0
    for y in range(y0, y1):
        acc |= int.from_bytes(flags[y * w:(y + 1) * w], "little")
    cb = acc.to_bytes(w, "little")
    return cb.find(b"\x01"), y0, cb.rfind(b"\x01") + 1, y1


def footprint(alpha, w, h, st=None, thr=16):
    """Where a subject stands: bbox, the middle of its lowest rows (cx), its lowest row (bottom) and the width of the
    lowest rows (the lowest `contact.band` of its height, at least 2 rows).  alpha: bytes w * h.  None for an empty picture."""
    box = alpha_bbox(alpha, w, h, thr)
    if box is None:
        return None
    band = (st or DEFAULT_STYLE)["contact"]["band"]
    x0, y0, x1, y1 = box
    rows = max(2, int(round((y1 - y0) * band)))
    flags = alpha.translate(_threshold_table(thr))
    acc = 0
    for y in range(max(y0, y1 - rows), y1):
        acc |= int.from_bytes(flags[y * w:(y + 1) * w], "little")
    cb = acc.to_bytes(w, "little")
    bx0, bx1 = cb.find(b"\x01"), cb.rfind(b"\x01") + 1
    return {"bbox": box, "cx": (bx0 + bx1) / 2.0, "bottom": y1, "width": bx1 - bx0}


def merge_footprints(fps):
    """One steady footprint for a sequence of frames (union bbox, median of the rest), so a turntable's shadow stays put."""
    fps = [f for f in fps if f]
    if not fps:
        return None

    def med(vals):
        vals = sorted(vals)
        return vals[len(vals) // 2]
    return {"bbox": (min(f["bbox"][0] for f in fps), min(f["bbox"][1] for f in fps),
                     max(f["bbox"][2] for f in fps), max(f["bbox"][3] for f in fps)),
            "cx": med([f["cx"] for f in fps]), "bottom": med([f["bottom"] for f in fps]),
            "width": med([f["width"] for f in fps])}


def scale_footprint(fp, s, ox=0, oy=0):
    """The footprint of the same picture resized by s and moved by (ox, oy)."""
    if not fp:
        return fp
    x0, y0, x1, y1 = fp["bbox"]
    return {"bbox": (int(x0 * s) + ox, int(y0 * s) + oy, int(x1 * s + 0.999) + ox, int(y1 * s + 0.999) + oy),
            "cx": fp["cx"] * s + ox, "bottom": int(round(fp["bottom"] * s)) + oy, "width": int(round(fp["width"] * s))}


# ------------------------------------------------------------------------------------------------ shadows

def contact_plane(w, h, fp, st=None):
    """The contact shadow as a w * h bytes plane, 255 at its darkest (apply `contact.opacity` when blending);
    also returns the (y0, y1) rows that are not empty."""
    cs = (st or DEFAULT_STYLE)["contact"]
    F = cs["grid"]
    gw, gh = max(1, w // F), max(1, h // F)
    bx0, by0, bx1, by1 = fp["bbox"]
    rx = min(cs["max_width"] * w, max(0.5 * fp["width"], 0.5 * cs["min_width"] * (bx1 - bx0)))
    rx = max(rx, 4.0)
    ry = max(2.0, rx * cs["ry"])
    cx, cy = fp["cx"], fp["bottom"] + cs["gap"] * w + ry * 0.35
    rows = []
    for gy in range(gh):
        py = (gy + 0.5) * F
        t = ((py - cy) / ry) ** 2
        if t >= 1:
            rows.append([0] * gw)
            continue
        half = rx * (1 - t) ** 0.5
        rows.append([255 if abs((gx + 0.5) * F - cx) <= half else 0 for gx in range(gw)])
    sigma = cs["blur"] * w / F
    rows = blur_grid(rows, sigma)
    peak = max(max(r) for r in rows)
    if peak == 0:
        return bytearray(w * h), (0, 0)
    rows = [[(v * 255 + peak // 2) // peak for v in r] for r in rows]
    live = [gy for gy, r in enumerate(rows) if any(r)]
    y0, y1 = live[0] * F - F, live[-1] * F + 2 * F
    return upscale(rows, gw, gh, F, w, h, y0, y1), (max(0, y0), min(h, y1))


def make_base(w, h, fp, st=None):
    """Backdrop + contact shadow as an RGB bytearray (w * h * 3): the part of a studio picture that never moves."""
    st = st or DEFAULT_STYLE
    rgb = backdrop(w, h, st)
    if fp:
        plane, (y0, y1) = contact_plane(w, h, fp, st)
        cs = st["contact"]
        _blend_plane(rgb, w, plane, y0, y1, cs["colour"], int(round(cs["opacity"] * 255)))
    return rgb


def _blend_plane(rgb, w, plane, y0, y1, colour, opacity255):
    """Blend `colour` over an RGB buffer through a 0..255 plane scaled by opacity255 (rows y0..y1)."""
    tab = bytes((v * opacity255 + 127) // 255 for v in range(256))
    zero = bytes(w)
    for y in range(y0, y1):
        row = plane[y * w:(y + 1) * w]
        if row == zero:
            continue
        s = row.translate(tab)
        lo = w - len(s.lstrip(b"\0"))
        hi = len(s.rstrip(b"\0"))
        if hi <= lo:
            continue
        s = s[lo:hi]
        o = (y * w + lo) * 3
        for ch in range(3):
            seg = rgb[o + ch:o + (hi - lo) * 3:3]
            c = colour[ch]
            rgb[o + ch:o + (hi - lo) * 3:3] = bytes([(b * (255 - a) + c * a + 127) // 255 for b, a in zip(seg, s)])


def drop_plane(alpha, w, h, st=None):
    """The drop shadow plane (0..255, before opacity) of an alpha plane, and its (x0, y0, x1, y1) window, or (None, None)."""
    ds = (st or DEFAULT_STYLE)["drop"]
    F = ds["grid"]
    gw, gh = w // F, h // F
    if gw < 2 or gh < 2:
        return None, None
    sums = matte.box_sums(alpha, w, h, F)
    f2 = F * F
    grid = [[(v + f2 // 2) // f2 for v in sums[gy * gw:(gy + 1) * gw]] for gy in range(gh)]
    if not any(any(r) for r in grid):
        return None, None
    sx, sy = int(round(ds["dx"] * w / F)), int(round(ds["dy"] * w / F))
    if sy > 0:
        grid = [[0] * gw for _ in range(sy)] + grid[:gh - sy]
    if sx > 0:
        grid = [[0] * sx + r[:gw - sx] for r in grid]
    grid = blur_grid(grid, ds["blur"] * w / F)
    live = [gy for gy, r in enumerate(grid) if any(r)]
    if not live:
        return None, None
    cols = [any(r[gx] for r in grid[live[0]:live[-1] + 1]) for gx in range(gw)]
    gx0, gx1 = cols.index(True), gw - cols[::-1].index(True)
    x0, x1 = max(0, gx0 * F - F), min(w, gx1 * F + F)
    y0, y1 = max(0, live[0] * F - F), min(h, live[-1] * F + 2 * F)
    return upscale(grid, gw, gh, F, w, h, y0, y1, x0, x1), (x0, y0, x1, y1)


# ------------------------------------------------------------------------------------------------ composite

def compose(canvas, base, st=None, drop=True, alpha=None):
    """The subject (a straight RGBA Canvas) over `base` (RGB bytearray of the same size), with its drop shadow.

    Returns (RGB bytearray, (x0, y0, x1, y1)): the finished picture and the box that can differ from `base`
    (None when nothing was drawn)."""
    st = st or DEFAULT_STYLE
    w, h = canvas.w, canvas.h
    A = alpha if alpha is not None else bytes(canvas.buf[3::4])
    out = bytearray(base)
    box = alpha_bbox(A, w, h, 1)
    dplane, dwin = (drop_plane(A, w, h, st) if drop and box else (None, None))
    if box is None and dwin is None:
        return out, None
    x0, y0, x1, y1 = box if box else dwin
    if dwin:
        x0, y0, x1, y1 = min(x0, dwin[0]), min(y0, dwin[1]), max(x1, dwin[2]), max(y1, dwin[3])
    ds = st["drop"]
    dtab = bytes((v * int(round(ds["opacity"] * 255)) + 127) // 255 for v in range(256))
    sc = ds["colour"]
    for y in range(y0, y1):
        a = A[y * w + x0:y * w + x1]
        d = dplane[y * w + x0:y * w + x1].translate(dtab) if dplane else None
        un = (int.from_bytes(a, "little") | (int.from_bytes(d, "little") if d else 0)).to_bytes(x1 - x0, "little")
        lo = len(un) - len(un.lstrip(b"\0"))
        hi = len(un.rstrip(b"\0"))
        if hi <= lo:
            continue
        a = a[lo:hi]
        d = d[lo:hi] if d else None
        o = (y * w + x0 + lo) * 3
        src = canvas.buf[(y * w + x0 + lo) * 4:(y * w + x0 + hi) * 4]
        n = hi - lo
        for ch in range(3):
            bg = out[o + ch:o + n * 3:3]
            c = src[ch::4]
            if d and d.count(0) != n:
                col = sc[ch]
                out[o + ch:o + n * 3:3] = bytes(
                    [(((b * (255 - s) + col * s + 127) // 255) * (255 - al) + cc * al + 127) // 255
                     for b, cc, al, s in zip(bg, c, a, d)])
            else:
                out[o + ch:o + n * 3:3] = bytes([(b * (255 - al) + cc * al + 127) // 255 for b, cc, al in zip(bg, c, a)])
    return out, (x0, y0, x1, y1)


# ------------------------------------------------------------------------------------------------ caption bar

def ascii_text(text):
    """Text the vanilla font can draw: accents and typographic marks reduced to ASCII, the rest as '?'."""
    text = text.replace("–", "-").replace("—", "-").replace("‘", "'").replace("’", "'")
    text = text.replace("“", '"').replace("”", '"').replace("…", "...")
    out = unicodedata.normalize("NFKD", text).encode("ascii", "replace").decode("ascii")
    return "".join(ch if 32 <= ord(ch) <= 126 else "?" for ch in out)


def bar(width, height, text, scale, colour):
    """The caption bar (as in the old legacy/gif.py): cream, a dark brass and a soft gold line on top, centred text."""
    b = lib.Canvas(width, height, lib.PAGE)
    b.hline(0, 0, width, lib.BORDER_DARK)
    b.hline(0, 1, width, lib.BORDER)
    text = ascii_text(text or "")
    if text:
        while scale > 1 and lib.text_width(text, scale) > width - 16:
            scale -= 1
        lib.text_centred(b, width // 2, (height - 7 * scale) // 2 + 1, text, lib.colour(colour), scale)
    return b


# ------------------------------------------------------------------------------------------------ pictures

def render(canvas, st=None, caption=None, shadows=True, base=None, fp=None):
    """A finished studio picture (opaque RGBA Canvas): backdrop, contact and drop shadow, the subject, and, when
    `caption` is not None, the caption bar under it.  `base` / `fp` reuse a base picture and footprint (a turntable)."""
    st = st or DEFAULT_STYLE
    w, h = canvas.w, canvas.h
    if base is None:
        if fp is None and shadows:
            fp = footprint(bytes(canvas.buf[3::4]), w, h, st)
        base = make_base(w, h, fp if shadows else None, st)
    rgb, _ = compose(canvas, base, st, drop=shadows)
    bh = st["bar"]["height"] if caption is not None else 0
    out = lib.Canvas(w, h + bh)
    out.buf[0:w * h * 4:4] = rgb[0::3]
    out.buf[1:w * h * 4:4] = rgb[1::3]
    out.buf[2:w * h * 4:4] = rgb[2::3]
    out.buf[3:w * h * 4:4] = bytes([255]) * (w * h)
    if caption is not None:
        out.blit(bar(w, bh, caption, st["bar"]["scale"], st["bar"]["colour"]), 0, h)
    return out


def place_on_canvas(canvas, subject, rect, shadow=True, st=None):
    """Composite a matted RGBA picture straight onto `canvas` inside rect (x, y, w, h): fitted inside (only ever
    scaled down, with the area filter) and centred, no frame, no backdrop; with shadow=True the contact shadow is
    laid on the card under it.  Returns the box (x, y, w, h) the subject now occupies."""
    st = st or DEFAULT_STYLE
    x, y, w, h = rect
    s = min(1.0, w / float(subject.w), h / float(subject.h))
    pic = subject if s >= 1.0 else subject.area(max(1, int(subject.w * s)), max(1, int(subject.h * s)))
    ox, oy = (w - pic.w) // 2, (h - pic.h) // 2
    if shadow:
        fp = footprint(bytes(pic.buf[3::4]), pic.w, pic.h, st)
        if fp:
            fp = scale_footprint(fp, 1.0, ox, oy)
            sh = h + max(0, min(int(0.15 * h), canvas.h - (y + h)))      # the shadow may reach a little below the rect
            plane, (y0, y1) = contact_plane(w, sh, fp, st)
            cs = st["contact"]
            rgb = bytearray(w * sh * 3)
            for yy in range(sh):
                row = canvas.buf[((y + yy) * canvas.w + x) * 4:((y + yy) * canvas.w + x + w) * 4]
                rgb[yy * w * 3:(yy + 1) * w * 3:3] = row[0::4]
                rgb[yy * w * 3 + 1:(yy + 1) * w * 3:3] = row[1::4]
                rgb[yy * w * 3 + 2:(yy + 1) * w * 3:3] = row[2::4]
            _blend_plane(rgb, w, plane, y0, y1, cs["colour"], int(round(cs["opacity"] * 255)))
            for yy in range(y0, y1):
                o = ((y + yy) * canvas.w + x) * 4
                seg = rgb[yy * w * 3:(yy + 1) * w * 3]
                canvas.buf[o:o + w * 4:4] = seg[0::3]
                canvas.buf[o + 1:o + w * 4:4] = seg[1::3]
                canvas.buf[o + 2:o + w * 4:4] = seg[2::3]
    canvas.blit(pic, x + ox, y + oy)
    return (x + ox, y + oy, pic.w, pic.h)

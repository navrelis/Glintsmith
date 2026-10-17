"""Studio matte: turns a black / white render pair into a straight-alpha RGBA picture.  Python 3 standard library only.

The in-game studio renders every frame twice, over pure black (K) and over pure white (W), at S times the output size
(supersampling).  For a pixel with straight colour c and alpha a:

    K = c * a                      (c over black)
    W = c * a + 255 * (1 - a)      (c over white)

so   a = 1 - mean_channel(W - K) / 255   (clamped to 0..1)   and   c = K / a   (clamped to 0..255; 0 where a <= 1/255).

Exact cases: W == K in every channel is an opaque pixel (a = 1, c = K); K = 0 and W = 255 in every channel is empty
(0, 0, 0, 0); a tiny negative or over-range difference (noise, W slightly below K) clamps to opaque.  Alpha at or below
1/255 snaps to 0 (colour 0).

The S x S supersampled pixels are then averaged with an area (box) filter in PREMULTIPLIED space (sum of K and of a,
colour = sum(K) / sum(a)), so the anti-aliased edges of the render keep the right colour instead of fringing dark or
light.  All arithmetic is integer, so the result is the same on every machine.

The work is done on whole planes with big-integer / bytes.translate tricks (C speed) for the bulk of the picture
(opaque and empty pixels); only the semi-transparent pixels of a frame go through a Python loop.

    matte_pair(k_rgba, w_rgba, width, height, S)   -> lib.Canvas  (width // S x height // S, straight RGBA)
    matte_files(k_path, w_path, S)                 -> lib.Canvas
    box_sums(plane, w, h, S)                       -> array('H') of the S x S block sums (also used by composite.py)
"""
import os
import sys
from array import array
from itertools import compress

sys.dont_write_bytecode = True
HERE = os.path.dirname(os.path.abspath(__file__))
ART = os.path.dirname(HERE)
if ART not in sys.path:
    sys.path.insert(0, ART)

import lib   # noqa: E402

_NZ = bytes([0]) + bytes([1]) * 255           # 0 -> 0, anything else -> 1
_Z255 = bytes([255]) + bytes(255)             # 0 -> 255, anything else -> 0
_INV = bytes(255 - i for i in range(256))
_LITTLE = sys.byteorder == "little"


def _int(b):
    return int.from_bytes(b, "little")


def _u16(lanes):
    """array('H') from little-endian 16 bit lanes."""
    a = array("H")
    a.frombytes(bytes(lanes))
    if not _LITTLE:
        a.byteswap()
    return a


def box_sums(plane, w, h, S):
    """Sums of the S x S blocks of an 8 bit plane (bytes, w * h): array('H') of (w // S) * (h // S) values.

    A remainder of w or h that does not fill a whole block is ignored.  Needs S * S * 255 <= 65535 (S <= 16).
    Every row is added as 16 bit lanes of one big integer, so there is no Python loop over pixels."""
    ow, oh = w // S, h // S
    if S == 1:
        out = bytearray(2 * ow * oh)
        for y in range(oh):
            out[2 * y * ow:2 * (y + 1) * ow:2] = plane[y * w:y * w + ow]
        return _u16(out)
    zero = bytes(w)
    parts = []
    nbytes = 2 * ow
    for y in range(oh):
        acc = 0
        for j in range(S):
            row = plane[(y * S + j) * w:(y * S + j + 1) * w]
            if row == zero:
                continue
            for i in range(S):
                lanes = bytearray(nbytes)
                lanes[0::2] = row[i::S][:ow]
                acc += int.from_bytes(lanes, "little")
        parts.append(acc.to_bytes(nbytes, "little"))
    return _u16(b"".join(parts))


def premultiplied_planes(k_rgba, w_rgba):
    """(P_r, P_g, P_b, A) bytearrays, one byte per pixel: premultiplied colour (= K, clamped to A) and alpha 0..255,
    at the size of the input.  k_rgba / w_rgba: RGBA buffers of equal length (the alpha channel is ignored)."""
    n = len(k_rgba) // 4
    K = [bytes(k_rgba[c::4]) for c in range(3)]
    Wc = [bytes(w_rgba[c::4]) for c in range(3)]
    diff = 0
    kor = 0
    wnot = 0
    for c in range(3):
        diff |= _int(K[c]) ^ _int(Wc[c])
        kor |= _int(K[c])
        wnot |= _int(Wc[c].translate(_INV))
    differs = diff.to_bytes(n, "little").translate(_NZ)            # 1 where K != W in some channel
    notbg = (kor | wnot).to_bytes(n, "little").translate(_NZ)      # 1 where not (K == 0 and W == 255)
    mixed = (_int(differs) & _int(notbg)).to_bytes(n, "little")    # semi-transparent (or noisy) pixels
    A = bytearray(differs.translate(_Z255))                        # opaque where K == W
    P = [bytearray(K[c]) for c in range(3)]                        # premultiplied colour = K (0 on the empty pixels)
    Kr, Kg, Kb = K
    Wr, Wg, Wb = Wc
    for i in compress(range(n), mixed):
        a765 = 765 - (Wr[i] + Wg[i] + Wb[i] - Kr[i] - Kg[i] - Kb[i])
        if a765 >= 765:
            a8 = 255
        elif a765 <= 3:                                             # a <= 1/255: empty
            A[i] = 0
            P[0][i] = P[1][i] = P[2][i] = 0
            continue
        else:
            a8 = (a765 + 1) // 3
        A[i] = a8
        P[0][i] = Kr[i] if Kr[i] < a8 else a8
        P[1][i] = Kg[i] if Kg[i] < a8 else a8
        P[2][i] = Kb[i] if Kb[i] < a8 else a8
    return P[0], P[1], P[2], A


def matte_pair(k_rgba, w_rgba, width, height, S=1):
    """Straight-alpha RGBA Canvas (width // S x height // S) from the K and W renders (RGBA buffers, width x height)."""
    if len(k_rgba) != width * height * 4 or len(w_rgba) != width * height * 4:
        raise ValueError("K and W renders must both be %dx%d" % (width, height))
    if S < 1 or S > 16:
        raise ValueError("supersample must be 1..16")
    ow, oh = width // S, height // S
    Pr, Pg, Pb, A = premultiplied_planes(k_rgba, w_rgba)
    N = S * S
    sums_a = box_sums(A, width, height, S)
    sums_p = [box_sums(p, width, height, S) for p in (Pr, Pg, Pb)]
    out = lib.Canvas(ow, oh)
    buf = out.buf
    half = N // 2
    for y in range(oh):
        a = sums_a[y * ow:(y + 1) * ow]
        if a.count(0) == ow:
            continue
        rows = [s[y * ow:(y + 1) * ow] for s in sums_p]
        base = y * ow * 4
        alpha = bytes([(v + half) // N for v in a])
        buf[base + 3:base + ow * 4:4] = alpha
        lo = ow - len(alpha.lstrip(b"\0"))
        hi = len(alpha.rstrip(b"\0"))
        if hi <= lo:
            continue
        for ch in range(3):
            p = rows[ch]
            buf[base + lo * 4 + ch:base + hi * 4:4] = bytes(
                [(p[x] * 255 + (a[x] >> 1)) // a[x] if a[x] else 0 for x in range(lo, hi)])
    return out


def matte_files(k_path, w_path, S=1):
    """Read a K / W PNG pair and matte it (the sizes must be equal and a multiple of S)."""
    kc, wc = lib.Canvas.open(k_path), lib.Canvas.open(w_path)
    if (kc.w, kc.h) != (wc.w, wc.h):
        raise ValueError("%s is %dx%d but %s is %dx%d" % (k_path, kc.w, kc.h, w_path, wc.w, wc.h))
    return matte_pair(kc.buf, wc.buf, kc.w, kc.h, S)

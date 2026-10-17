"""Dependency-free image encoders (PNG, animated GIF89a) and a small GIF decoder for checking.

Python 3 standard library only.  Everything is deterministic: no timestamps, no randomness.
"""
import struct
import zlib
from collections import Counter
from itertools import accumulate


# --------------------------------------------------------------------------------------- PNG

def _chunk(tag, data):
    c = struct.pack(">I", len(data)) + tag + data
    return c + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)


def png_bytes(w, h, pixels, channels=4, text=None):
    """pixels: bytes-like, w*h*channels, row-major.  channels 3 = RGB, 4 = RGBA.  text: optional tEXt comment."""
    assert len(pixels) == w * h * channels
    stride = w * channels
    raw = bytearray()
    for y in range(h):
        raw.append(0)
        raw += pixels[y * stride:(y + 1) * stride]
    ctype = 2 if channels == 3 else 6
    return (b"\x89PNG\r\n\x1a\n"
            + _chunk(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, ctype, 0, 0, 0))
            + (_chunk(b"tEXt", b"Comment" + bytes((0,)) + text.encode("latin-1")) if text else b"")
            + _chunk(b"IDAT", zlib.compress(bytes(raw), 9))
            + _chunk(b"IEND", b""))


PNG_SIGNATURE = bytes((0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A))


def _swar_add(a, b):
    """Byte-wise (a + b) mod 256 of two equally long bytes objects, done on whole integers."""
    n = len(a)
    lo = int.from_bytes(bytes((0x7F,)) * n, "little")
    hi = int.from_bytes(bytes((0x80,)) * n, "little")
    x = int.from_bytes(a, "little")
    y = int.from_bytes(b, "little")
    return (((x & lo) + (y & lo)) ^ ((x ^ y) & hi)).to_bytes(n, "little")


def read_png(data, max_rows=None):
    """Decode a non-interlaced PNG: 8-bit RGB / RGBA (what Minecraft writes for screenshots), and the grey and
    palette flavours that the vanilla textures use.

    Returns (width, height, rgba) with rgba a bytearray of width * rows * 4.  max_rows stops decoding after
    that many rows (the returned height is then max_rows), which is enough when only the top of a big
    screenshot is needed.  Anything else (16 bit, interlaced) raises ValueError."""
    if data[:8] != PNG_SIGNATURE:
        raise ValueError("not a PNG file")
    pos, idat, hdr, plte, trns = 8, [], None, None, b""
    while pos < len(data):
        n, tag = struct.unpack(">I4s", data[pos:pos + 8])
        body = data[pos + 8:pos + 8 + n]
        if zlib.crc32(tag + body) & 0xFFFFFFFF != struct.unpack(">I", data[pos + 8 + n:pos + 12 + n])[0]:
            raise ValueError("PNG chunk %r has a bad CRC" % tag)
        pos += 12 + n
        if tag == b"IHDR":
            hdr = struct.unpack(">IIBBBBB", body)
        elif tag == b"PLTE":
            plte = body
        elif tag == b"tRNS":
            trns = body
        elif tag == b"IDAT":
            idat.append(body)
        elif tag == b"IEND":
            break
    if hdr is None:
        raise ValueError("PNG has no IHDR")
    w, h, depth, ctype, _comp, _filt, interlace = hdr
    ok = (ctype in (2, 6, 4, 0) and depth == 8) or (ctype == 3 and depth in (1, 2, 4, 8))
    if not ok or interlace != 0:
        raise ValueError("only non-interlaced 8-bit RGB, RGBA, grey, grey+alpha and palette PNG is supported "
                         "(depth %d, colour type %d, interlace %d)" % (depth, ctype, interlace))
    ch = {2: 3, 6: 4, 4: 2, 0: 1, 3: 1}[ctype]
    stride = (w * ch * depth + 7) // 8
    rows = h if max_rows is None else min(h, max_rows)
    raw = zlib.decompress(b"".join(idat))
    if len(raw) < h * (stride + 1):
        raise ValueError("PNG pixel data is truncated")
    out = bytearray(w * rows * 4)
    prev = bytes(stride)
    mask = (255).__and__
    for y in range(rows):
        p = y * (stride + 1)
        ft = raw[p]
        line = bytearray(raw[p + 1:p + 1 + stride])
        if ft == 1:
            for c in range(ch):
                line[c::ch] = bytes(map(mask, accumulate(line[c::ch])))
        elif ft == 2:
            line = bytearray(_swar_add(line, prev))
        elif ft == 3:
            for i in range(ch):
                line[i] = (line[i] + (prev[i] >> 1)) & 255
            for i in range(ch, stride):
                line[i] = (line[i] + ((line[i - ch] + prev[i]) >> 1)) & 255
        elif ft == 4:
            for i in range(ch):
                line[i] = (line[i] + prev[i]) & 255
            for i in range(ch, stride):
                a = line[i - ch]
                b = prev[i]
                c = prev[i - ch]
                pa = b - c
                pb = a - c
                pc = abs(pa + pb)
                pa = abs(pa)
                pb = abs(pb)
                line[i] = (line[i] + (a if pa <= pb and pa <= pc else (b if pb <= pc else c))) & 255
        elif ft != 0:
            raise ValueError("unknown PNG filter type %d" % ft)
        o = y * w * 4
        if ctype == 6:
            out[o:o + w * 4] = line
        elif ctype == 2:
            out[o:o + w * 4:4] = line[0::3]
            out[o + 1:o + w * 4:4] = line[1::3]
            out[o + 2:o + w * 4:4] = line[2::3]
            out[o + 3:o + w * 4:4] = bytes((255,)) * w
        elif ctype == 0:
            for c in range(3):
                out[o + c:o + w * 4:4] = line
            out[o + 3:o + w * 4:4] = bytes((255,)) * w
        elif ctype == 4:
            for c in range(3):
                out[o + c:o + w * 4:4] = line[0::2]
            out[o + 3:o + w * 4:4] = line[1::2]
        else:
            if depth == 8:
                idx = line
            else:
                per = 8 // depth
                m = (1 << depth) - 1
                idx = bytearray(len(line) * per)
                for k in range(per):
                    idx[k::per] = bytes((b >> (8 - depth * (k + 1))) & m for b in line)
            idx = idx[:w]
            for c in range(3):
                out[o + c:o + w * 4:4] = bytes(plte[i * 3 + c] for i in idx)
            out[o + 3:o + w * 4:4] = bytes(trns[i] if i < len(trns) else 255 for i in idx)
        prev = line
    return w, rows, out


def read_png_size(path):
    with open(path, "rb") as f:
        head = f.read(24)
    assert head[:8] == b"\x89PNG\r\n\x1a\n" and head[12:16] == b"IHDR"
    return struct.unpack(">II", head[16:24])


# --------------------------------------------------------------------------------------- LZW

def lzw_encode(indices, min_bits=8):
    """GIF flavoured variable-width LZW. indices: bytes of palette indices (each < 2**min_bits)."""
    clear = 1 << min_bits
    eoi = clear + 1
    out = bytearray()
    size = min_bits + 1
    table = {}
    next_code = eoi + 1

    acc = clear
    nb = size
    if not indices:
        acc |= eoi << nb
        nb += size
        while nb > 0:
            out.append(acc & 255)
            acc >>= 8
            nb -= 8
        return bytes(out)

    prefix = indices[0]
    get = table.get
    for k in indices[1:]:
        key = (prefix << 8) | k
        c = get(key)
        if c is not None:
            prefix = c
            continue
        acc |= prefix << nb
        nb += size
        while nb >= 8:
            out.append(acc & 255)
            acc >>= 8
            nb -= 8
        if next_code < 4096:
            table[key] = next_code
            if next_code == (1 << size):
                size += 1
            next_code += 1
        else:
            acc |= clear << nb
            nb += size
            while nb >= 8:
                out.append(acc & 255)
                acc >>= 8
                nb -= 8
            table.clear()
            next_code = eoi + 1
            size = min_bits + 1
        prefix = k
    acc |= prefix << nb
    nb += size
    acc |= eoi << nb
    nb += size
    while nb > 0:
        out.append(acc & 255)
        acc >>= 8
        nb -= 8
    return bytes(out)


def lzw_decode(data, min_bits, expected):
    clear = 1 << min_bits
    eoi = clear + 1
    size = min_bits + 1
    table = [bytes([i]) for i in range(clear)] + [b"", b""]
    out = bytearray()
    acc = 0
    nb = 0
    prev = None
    pos = 0
    while len(out) < expected:
        while nb < size:
            if pos >= len(data):
                return bytes(out)
            acc |= data[pos] << nb
            pos += 1
            nb += 8
        code = acc & ((1 << size) - 1)
        acc >>= size
        nb -= size
        if code == clear:
            size = min_bits + 1
            table = table[:clear + 2]
            prev = None
            continue
        if code == eoi:
            break
        if prev is None:
            entry = table[code]
        else:
            if code < len(table):
                entry = table[code]
                table.append(prev + entry[:1])
            else:
                entry = prev + prev[:1]
                table.append(entry)
            if len(table) == (1 << size) and size < 12:
                size += 1
        out += entry
        prev = entry
    return bytes(out)


# --------------------------------------------------------------------------------------- GIF

def _sub_blocks(data):
    out = bytearray()
    for i in range(0, len(data), 255):
        chunk = data[i:i + 255]
        out.append(len(chunk))
        out += chunk
    out.append(0)
    return bytes(out)


def gif_bytes(width, height, palette, frames, loop=0):
    """palette: list of (r,g,b), up to 256.  frames: list of dicts with keys
    x, y, w, h, lzw (bytes from lzw_encode), delay (centiseconds), trans (index or None), disposal."""
    pal = list(palette) + [(0, 0, 0)] * (256 - len(palette))
    out = bytearray(b"GIF89a")
    out += struct.pack("<HHBBB", width, height, 0x80 | (7 << 4) | 7, 0, 0)
    for r, g, b in pal:
        out += bytes((r, g, b))
    out += b"\x21\xff\x0bNETSCAPE2.0\x03\x01" + struct.pack("<H", loop) + b"\x00"
    for fr in frames:
        trans = fr.get("trans")
        packed = (fr.get("disposal", 1) << 2) | (1 if trans is not None else 0)
        out += b"\x21\xf9\x04" + bytes((packed,)) + struct.pack("<H", fr["delay"]) \
            + bytes((trans or 0,)) + b"\x00"
        out += b"\x2c" + struct.pack("<HHHHB", fr["x"], fr["y"], fr["w"], fr["h"], 0)
        out += b"\x08" + _sub_blocks(fr["lzw"])
    out += b"\x3b"
    return bytes(out)


def parse_gif(data):
    """Parse back a GIF written by gif_bytes (or any simple GIF).  Returns dict with
    width, height, palette, loop, frames (each: x,y,w,h,delay,trans,disposal,indices)."""
    assert data[:6] in (b"GIF89a", b"GIF87a"), "bad header"
    assert data[-1] == 0x3B, "missing trailer"
    w, h, packed, _bg, _asp = struct.unpack("<HHBBB", data[6:13])
    pos = 13
    palette = []
    if packed & 0x80:
        n = 2 << (packed & 7)
        for i in range(n):
            palette.append(tuple(data[pos + 3 * i:pos + 3 * i + 3]))
        pos += 3 * n
    frames = []
    loop = None
    gce = {"delay": 0, "trans": None, "disposal": 0}
    while True:
        b = data[pos]
        pos += 1
        if b == 0x3B:
            break
        if b == 0x21:
            label = data[pos]
            pos += 1
            if label == 0xF9:
                size = data[pos]
                p = data[pos + 1]
                delay = struct.unpack("<H", data[pos + 2:pos + 4])[0]
                gce = {"delay": delay, "trans": data[pos + 4] if p & 1 else None, "disposal": (p >> 2) & 7}
                pos += 1 + size
                assert data[pos] == 0
                pos += 1
            else:
                ident = None
                while True:
                    n = data[pos]
                    pos += 1
                    if n == 0:
                        break
                    blk = data[pos:pos + n]
                    if label == 0xFF and ident is None:
                        ident = blk
                    elif label == 0xFF and ident == b"NETSCAPE2.0" and blk[0] == 1:
                        loop = struct.unpack("<H", blk[1:3])[0]
                    pos += n
        elif b == 0x2C:
            x, y, fw, fh, fp = struct.unpack("<HHHHB", data[pos:pos + 9])
            pos += 9
            assert not (fp & 0x80), "local palettes unsupported in checker"
            mb = data[pos]
            pos += 1
            blob = bytearray()
            while True:
                n = data[pos]
                pos += 1
                if n == 0:
                    break
                blob += data[pos:pos + n]
                pos += n
            idx = lzw_decode(bytes(blob), mb, fw * fh)
            assert len(idx) == fw * fh, "frame data length %d != %d" % (len(idx), fw * fh)
            frames.append(dict(x=x, y=y, w=fw, h=fh, indices=idx, **gce))
            gce = {"delay": 0, "trans": None, "disposal": 0}
        else:
            raise ValueError("unknown block 0x%02x at %d" % (b, pos - 1))
    return dict(width=w, height=h, palette=palette, loop=loop, frames=frames)


def gif_compose(info, upto, background=(0, 0, 0)):
    """Compose frames 0..upto (inclusive) the way a viewer does (disposal 1 = keep).
    Returns (rgb bytes, painted mask bytes)."""
    w, h = info["width"], info["height"]
    pal = info["palette"]
    canvas = bytearray(bytes(background) * (w * h))
    painted = bytearray(w * h)
    for fr in info["frames"][:upto + 1]:
        t = fr["trans"]
        i = 0
        idx = fr["indices"]
        for yy in range(fr["y"], fr["y"] + fr["h"]):
            base = yy * w + fr["x"]
            for xx in range(fr["w"]):
                v = idx[i]
                i += 1
                if v == t:
                    continue
                r, g, b = pal[v]
                p = (base + xx) * 3
                canvas[p] = r
                canvas[p + 1] = g
                canvas[p + 2] = b
                painted[base + xx] = 1
    return bytes(canvas), bytes(painted)


# --------------------------------------------------------------------------------------- quantiser

def median_cut(hist, n_colors):
    """hist: dict {(r,g,b): count}.  Returns (palette list, mapping dict colour->index)."""
    items = [(c[0], c[1], c[2], n) for c, n in sorted(hist.items())]
    if len(items) <= n_colors:
        pal = [(r, g, b) for r, g, b, _ in items]
        return pal, {(r, g, b): i for i, (r, g, b, _) in enumerate(items)}

    def box_info(box):
        lo = [255, 255, 255]
        hi = [0, 0, 0]
        tot = 0
        for r, g, b, n in box:
            tot += n
            if r < lo[0]: lo[0] = r
            if g < lo[1]: lo[1] = g
            if b < lo[2]: lo[2] = b
            if r > hi[0]: hi[0] = r
            if g > hi[1]: hi[1] = g
            if b > hi[2]: hi[2] = b
        rng = ((hi[0] - lo[0]) * 0.9, (hi[1] - lo[1]) * 1.15, (hi[2] - lo[2]) * 0.8)
        axis = max(range(3), key=lambda a: rng[a])
        prio = rng[axis] * (tot ** 0.5) if len(box) > 1 else -1
        return axis, prio

    boxes = []
    ax, pr = box_info(items)
    boxes.append([items, ax, pr])
    while len(boxes) < n_colors:
        best = max(range(len(boxes)), key=lambda i: boxes[i][2])
        if boxes[best][2] <= 0:
            break
        box, ax, _ = boxes.pop(best)
        box.sort(key=lambda it: (it[ax], it[0], it[1], it[2]))
        total = sum(it[3] for it in box)
        half = total / 2.0
        acc = 0
        cut = 1
        for i, it in enumerate(box):
            acc += it[3]
            if acc >= half:
                cut = min(max(i + 1, 1), len(box) - 1)
                break
        for part in (box[:cut], box[cut:]):
            a2, p2 = box_info(part)
            boxes.append([part, a2, p2])
    palette = []
    mapping = {}
    for box, _, _ in boxes:
        tot = sum(it[3] for it in box)
        r = sum(it[0] * it[3] for it in box) / tot
        g = sum(it[1] * it[3] for it in box) / tot
        b = sum(it[2] * it[3] for it in box) / tot
        idx = len(palette)
        palette.append((int(r + 0.5), int(g + 0.5), int(b + 0.5)))
        for it in box:
            mapping[(it[0], it[1], it[2])] = idx
    return palette, mapping


def colour_histogram(rgb):
    """dict {(r,g,b): count} for an RGB bytes object (C-speed iteration)."""
    c = Counter(struct.iter_unpack("3s", rgb))
    return {tuple(k[0]): v for k, v in c.items()}


def map_to_indices(rgb, mapping):
    return bytes([mapping[tuple(p[0])] for p in struct.iter_unpack("3s", rgb)])


def encode_gif_frames(width, height, rgb_frames, delays, n_colors=255, keep_mask=None,
                      loop=0, pool=None):
    """Quantise a list of RGB frames (bytes) with one shared median-cut palette, delta-encode them
    with a transparent index (255), and return the GIF bytes.

    keep_mask: optional bytes (w*h); pixels with 0 stay transparent in every frame (rounded corners).
    Rows that are identical to the same row of the previous frame are neither counted nor mapped
    again, which makes long clips with a static background fast."""
    stride = width * 3
    hist = Counter()
    prev = None
    for fr in rgb_frames:
        for y in range(height):
            row = fr[y * stride:(y + 1) * stride]
            if prev is not None and row == prev[y * stride:(y + 1) * stride]:
                continue
            hist.update(struct.iter_unpack("3s", row))
        prev = fr
    hist = {tuple(k[0]): v for k, v in hist.items()}
    palette, mapping = median_cut(hist, n_colors)
    trans_idx = 255
    palette = (palette + [(0, 0, 0)] * 255)[:255] + [(0, 0, 0)]
    mapping = {bytes(k): v for k, v in mapping.items()}
    idx_frames = []
    prev_rgb = None
    prev_idx = None
    for fr in rgb_frames:
        rows = []
        for y in range(height):
            row = fr[y * stride:(y + 1) * stride]
            if prev_rgb is not None and row == prev_rgb[y * stride:(y + 1) * stride]:
                rows.append(prev_idx[y])
            else:
                rows.append(bytes([mapping[p[0]] for p in struct.iter_unpack("3s", row)]))
        prev_rgb, prev_idx = fr, rows
        idx = bytearray(b"".join(rows))
        if keep_mask is not None:
            for i, m in enumerate(keep_mask):
                if not m:
                    idx[i] = trans_idx
        idx_frames.append(bytes(idx))

    jobs = []
    meta = []
    prev = None
    for cur in idx_frames:
        if prev is None:
            x0, y0, x1, y1 = 0, 0, width, height
            data = cur
        else:
            rows = [y for y in range(height)
                    if cur[y * width:(y + 1) * width] != prev[y * width:(y + 1) * width]]
            if not rows:
                x0, y0, x1, y1 = 0, 0, 1, 1
                data = bytes([trans_idx])
            else:
                y0, y1 = rows[0], rows[-1] + 1
                xmin, xmax = width, 0
                for y in rows:
                    a = int.from_bytes(prev[y * width:(y + 1) * width], "big") ^                         int.from_bytes(cur[y * width:(y + 1) * width], "big")
                    first = width - (a.bit_length() + 7) // 8
                    last = width - 1 - ((a & -a).bit_length() - 1) // 8
                    if first < xmin:
                        xmin = first
                    if last + 1 > xmax:
                        xmax = last + 1
                x0, x1 = xmin, xmax
                buf = bytearray()
                for y in range(y0, y1):
                    cr = cur[y * width + x0:y * width + x1]
                    pr = prev[y * width + x0:y * width + x1]
                    if cr == pr:
                        buf += bytes([trans_idx]) * (x1 - x0)
                    else:
                        buf += bytes([trans_idx if a == b else a for a, b in zip(cr, pr)])
                data = bytes(buf)
        meta.append((x0, y0, x1 - x0, y1 - y0))
        jobs.append(data)
        prev = cur
    if pool is not None:
        lz = list(pool.map(lzw_encode, jobs, chunksize=4))
    else:
        lz = [lzw_encode(d) for d in jobs]
    frames = []
    for n, (m, l) in enumerate(zip(meta, lz)):
        frames.append(dict(x=m[0], y=m[1], w=m[2], h=m[3], lzw=l, delay=delays[n],
                           trans=trans_idx, disposal=1))
    return gif_bytes(width, height, palette, frames, loop)

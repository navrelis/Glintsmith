"""Glintsmith art library: canvas, vanilla font, card furniture.  Python 3 standard library only.

    1. Sources   the Minecraft 1.21.1 client jar (read at run time; nothing of it is stored in the repository)
    2. Palette   violet slate panel, brass frame, enchant-purple accent
    3. Canvas    RGBA canvas: fill, frame, crop, integer scale, box downscale, alpha blit, PNG in / out
    4. Text      the vanilla bitmap font (font/ascii.png from the jar), width, word wrap, paragraphs
    5. Furniture card frame, panel, chip, heading, label, bay, placeholder, sparkle, arrow

The card frame, the text routines and the jar lookup are adapted from the MIT licensed showcase library of
Lootr Remastered (art/showcase_lib.py, same author), re-implemented on a byte buffer so that 1920 x 1080
screenshots can be cropped and scaled quickly.

Everything is deterministic (no timestamps, no unseeded randomness).
"""
import glob
import os
import sys
import zipfile

sys.dont_write_bytecode = True
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import imgio   # noqa: E402

ART = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(ART)

# ---------------------------------------------------------------------------
# 1. Sources
# ---------------------------------------------------------------------------

JAR_ENV = "GLINTSMITH_MC_JAR"
_cache = {}


def jar_candidates():
    """Every place a Minecraft 1.21.1 client jar may live, most specific first."""
    out = []
    if os.environ.get(JAR_ENV):
        out.append(os.environ[JAR_ENV])
    home = os.path.expanduser("~")
    pats = [
        os.path.join(ROOT, ".gradle", "loom-cache", "minecraftMaven", "**", "minecraft-merged-*-1.21.1-*.jar"),
        os.path.join(home, ".gradle", "caches", "fabric-loom", "1.21.1", "minecraft-client.jar"),
        os.path.join(home, ".gradle", "caches", "fabric-loom", "1.21.1", "minecraft-client-only.jar"),
        os.path.join(home, ".gradle", "caches", "fabric-loom", "1.21.1", "minecraft-merged.jar"),
        os.path.join(home, ".gradle", "caches", "fabric-loom", "minecraftMaven", "**", "minecraft-merged-*-1.21.1-*.jar"),
    ]
    for pat in pats:
        for p in sorted(glob.glob(pat, recursive=True)):
            if p.endswith(".jar") and "sources" not in os.path.basename(p):
                out.append(p)
    return out


def mc_jar():
    """Path of a Minecraft 1.21.1 client jar that holds the font; exits with a clear message if there is none."""
    if "jar" not in _cache:
        tried = jar_candidates()
        for p in tried:
            if os.path.isfile(p):
                try:
                    if "assets/minecraft/textures/font/ascii.png" in zipfile.ZipFile(p).namelist():
                        _cache["jar"] = p
                        break
                except zipfile.BadZipFile:
                    continue
        else:
            sys.exit("No Minecraft 1.21.1 client jar with assets found (looked in: %s).\n"
                     "Set the environment variable %s to the path of a 1.21.1 client jar (for example the "
                     "Loom cache: .gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-merged-*/...jar)."
                     % (", ".join(tried) or "nothing", JAR_ENV))
    return _cache["jar"]


# ---------------------------------------------------------------------------
# 2. Palette
# ---------------------------------------------------------------------------

def rgb(h):
    h = h.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), 255)


def mix(a, b, t):
    """A colour between a and b (t = 0 gives a, 1 gives b)."""
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3)) + (255,)


BRASS = [rgb(h) for h in ("46291a", "754b1d", "ab7d25", "d8ad38", "f3d975", "fff5c2")]
VIOLET = [rgb(h) for h in ("2a1a55", "4b2d93", "7a4bd6", "b27dff", "dcc0ff", "f5eaff")]   # enchant purple ramp
TEAL = [rgb(h) for h in ("0a3340", "0e6f76", "1cbfab", "6df1d4", "e3fff4")]
INK = rgb("0c0914")

PANEL = rgb("1a1331")            # card background: deep violet slate
PANEL_MID = rgb("2c214f")        # lit bevel: chips, raised strips
PANEL_DARK = rgb("100b20")       # shaded bevel: recessed strips, bays
RIM = rgb("07050d")              # near black: drop shadows, label boxes
BORDER = BRASS[3]                # card frame
BORDER_DARK = BRASS[1]
BORDER_LIGHT = BRASS[4]
TEXT = rgb("efe9ff")             # body text: pale lavender
TEXT_DIM = rgb("aaa0c8")         # captions, secondary text
ACCENT = VIOLET[3]               # chips, bullets, rules
ACCENT_LIGHT = VIOLET[4]
ACCENT_DARK = VIOLET[1]
GOOD = TEAL[3]                   # "after" / on
BAD = rgb("e0707a")              # "before" / vanilla problem
GREY = rgb("8a8794")
MISSING_A, MISSING_B = rgb("55535c"), rgb("46444d")

COLOURS = {"text": TEXT, "dim": TEXT_DIM, "accent": ACCENT_LIGHT, "gold": BORDER_LIGHT, "good": GOOD,
           "bad": BAD, "ink": INK, "white": (255, 255, 255, 255)}


def colour(c, default=None):
    """A colour from a name (see COLOURS), a '#rrggbb' string or a tuple."""
    if c is None:
        return default
    if isinstance(c, str):
        return COLOURS[c] if c in COLOURS else rgb(c)
    c = tuple(c)
    return c if len(c) == 4 else c + (255,)


# ---------------------------------------------------------------------------
# 3. Canvas
# ---------------------------------------------------------------------------

class Canvas:
    """An RGBA canvas on a bytearray.  Coordinates outside the canvas are clipped, never an error."""

    def __init__(self, w, h, fill=None, buf=None):
        self.w, self.h = w, h
        if buf is None:
            buf = bytearray(bytes(colour(fill) if fill else (0, 0, 0, 0)) * (w * h))
        assert len(buf) == w * h * 4
        self.buf = buf

    # -- pixels
    def get(self, x, y):
        if not (0 <= x < self.w and 0 <= y < self.h):
            return None
        o = (y * self.w + x) * 4
        return tuple(self.buf[o:o + 4])

    def set(self, x, y, c):
        if 0 <= x < self.w and 0 <= y < self.h:
            c = colour(c)
            o = (y * self.w + x) * 4
            if c[3] == 255:
                self.buf[o:o + 4] = bytes(c)
            elif c[3]:
                self._blend(o, c)

    def _blend(self, o, c, a=None):
        a = c[3] if a is None else a
        b = self.buf
        if b[o + 3] == 0:
            b[o:o + 4] = bytes((c[0], c[1], c[2], a))
            return
        for i in range(3):
            b[o + i] = (b[o + i] * (255 - a) + c[i] * a + 127) // 255
        b[o + 3] = min(255, b[o + 3] + (a * (255 - b[o + 3]) + 127) // 255)

    def rect(self, x, y, w, h, c):
        c = colour(c)
        x0, y0, x1, y1 = max(0, x), max(0, y), min(self.w, x + w), min(self.h, y + h)
        if x1 <= x0 or y1 <= y0:
            return
        if c[3] == 255:
            px = bytes(c) * (x1 - x0)
            for yy in range(y0, y1):
                o = (yy * self.w + x0) * 4
                self.buf[o:o + len(px)] = px
        elif c[3]:
            for yy in range(y0, y1):
                for xx in range(x0, x1):
                    self._blend((yy * self.w + xx) * 4, c)

    def hline(self, x, y, n, c):
        self.rect(x, y, n, 1, c)

    def vline(self, x, y, n, c):
        self.rect(x, y, 1, n, c)

    def frame(self, x, y, w, h, c, t=1):
        for i in range(t):
            self.rect(x + i, y + i, w - 2 * i, 1, c)
            self.rect(x + i, y + h - 1 - i, w - 2 * i, 1, c)
            self.rect(x + i, y + i + 1, 1, h - 2 * i - 2, c)
            self.rect(x + w - 1 - i, y + i + 1, 1, h - 2 * i - 2, c)

    # -- whole-canvas operations
    def opaque(self):
        return min(self.buf[3::4], default=255) == 255

    def crop(self, x, y, w, h):
        """A new canvas of the region; parts outside this canvas are transparent."""
        out = Canvas(w, h)
        x0, x1 = max(0, x), min(self.w, x + w)
        if x1 <= x0:
            return out
        for yy in range(max(0, y), min(self.h, y + h)):
            o = (yy * self.w + x0) * 4
            d = ((yy - y) * w + (x0 - x)) * 4
            out.buf[d:d + (x1 - x0) * 4] = self.buf[o:o + (x1 - x0) * 4]
        return out

    def scaled(self, k):
        """Nearest-neighbour upscale by the whole factor k."""
        assert k == int(k) and k >= 1, k
        k = int(k)
        if k == 1:
            return Canvas(self.w, self.h, buf=bytearray(self.buf))
        out = Canvas(self.w * k, self.h * k)
        stride = self.w * 4
        ostride = stride * k
        for y in range(self.h):
            row = self.buf[y * stride:(y + 1) * stride]
            big = bytearray(ostride)
            for j in range(k):
                for ch in range(4):
                    big[j * 4 + ch::k * 4] = row[ch::4]
            o = y * k * ostride
            for j in range(k):
                out.buf[o + j * ostride:o + (j + 1) * ostride] = big
        return out

    def down(self, n):
        """Box-filter downscale by the whole factor n (every output pixel is the mean of an n x n block)."""
        assert n == int(n) and n >= 1, n
        n = int(n)
        if n == 1:
            return Canvas(self.w, self.h, buf=bytearray(self.buf))
        ow, oh = self.w // n, self.h // n
        out = Canvas(ow, oh)
        stride, nn = self.w * 4, n * n
        half = nn // 2
        for y in range(oh):
            rows = [self.buf[(y * n + j) * stride:(y * n + j + 1) * stride] for j in range(n)]
            orow = bytearray(ow * 4)
            for ch in range(4):
                parts = [r[ch::4][i::n][:ow] for r in rows for i in range(n)]
                orow[ch::4] = bytes((sum(t) + half) // nn for t in zip(*parts))
            out.buf[y * ow * 4:(y + 1) * ow * 4] = orow
        return out

    def blit(self, src, x, y):
        """Draw src at (x, y): transparent pixels are skipped, half-transparent ones blended."""
        for sy in range(max(0, -y), min(src.h, self.h - y)):
            sx0, sx1 = max(0, -x), min(src.w, self.w - x)
            if sx1 <= sx0:
                return
            so = (sy * src.w + sx0) * 4
            row = src.buf[so:so + (sx1 - sx0) * 4]
            do = ((y + sy) * self.w + x + sx0) * 4
            alphas = row[3::4]
            lo, hi = min(alphas), max(alphas)
            if lo == 255:
                self.buf[do:do + len(row)] = row
            elif hi == 0:
                continue
            else:
                for i in range(sx1 - sx0):
                    a = row[i * 4 + 3]
                    if a == 255:
                        self.buf[do + i * 4:do + i * 4 + 4] = row[i * 4:i * 4 + 4]
                    elif a:
                        self._blend(do + i * 4, (row[i * 4], row[i * 4 + 1], row[i * 4 + 2], a))

    # -- files
    def to_png(self, text=None):
        """PNG bytes: RGB when fully opaque, RGBA otherwise (text: optional tEXt comment)."""
        if self.opaque():
            rgb_ = bytearray(self.w * self.h * 3)
            for ch in range(3):
                rgb_[ch::3] = self.buf[ch::4]
            return imgio.png_bytes(self.w, self.h, bytes(rgb_), 3, text)
        return imgio.png_bytes(self.w, self.h, bytes(self.buf), 4, text)

    def rgb_bytes(self):
        out = bytearray(self.w * self.h * 3)
        for ch in range(3):
            out[ch::3] = self.buf[ch::4]
        return bytes(out)

    @staticmethod
    def from_png(data, max_rows=None):
        w, h, buf = imgio.read_png(data, max_rows)
        return Canvas(w, h, buf=buf)

    @staticmethod
    def open(path, max_rows=None):
        with open(path, "rb") as f:
            return Canvas.from_png(f.read(), max_rows)


def noise(seed):
    """A tiny fixed generator, so the dither is the same on every machine and Python version."""
    state = [seed * 2654435761 % 4294967296 or 1]

    def nxt(n):
        state[0] = (state[0] * 1103515245 + 12345) % 2147483648
        return (state[0] >> 8) % n
    return nxt


# ---------------------------------------------------------------------------
# 4. Text: the vanilla font, read from the jar
# ---------------------------------------------------------------------------
# font/ascii.png is a 16 x 16 grid of 8 x 8 cells indexed by character code.  A glyph is as wide as its last
# used column + 1, a space advances 4 pixels and one empty column separates glyphs, as in the game.

def _glyphs():
    if "font" not in _cache:
        data = zipfile.ZipFile(mc_jar()).read("assets/minecraft/textures/font/ascii.png")
        sheet = Canvas.from_png(data)
        cell = sheet.w // 16
        table = {}
        for code in range(256):
            gx, gy = code % 16 * cell, code // 16 * cell
            dots = [(x, y) for y in range(cell) for x in range(cell) if sheet.buf[((gy + y) * sheet.w + gx + x) * 4 + 3]]
            table[code] = (dots, max(x for x, _ in dots) + 1 if dots else 0)
        _cache["font"] = table
    return _cache["font"]


def _glyph(ch):
    code = ord(ch)
    if not 32 <= code <= 126:
        raise ValueError("character %r is not ASCII; the vanilla font sheet is only indexed by ASCII here" % ch)
    return _glyphs()[code]


def _advance(ch):
    return 4 if ch == " " else _glyph(ch)[1] + 1


def text_width(string, scale=1):
    """Width in pixels of string in the vanilla font at a whole scale."""
    return max(0, sum(_advance(ch) for ch in string) - 1) * scale if string else 0


def font_text(canvas, x, y, string, col=None, scale=1, shadow=True):
    """Draw string in the vanilla font; shadow=True is the game's drop shadow, a colour gives that shadow colour.

    Returns the x just after the text.  A line is 8 * scale high (9 with descenders)."""
    col = colour(col, TEXT)
    if shadow:
        dark = colour(shadow) if shadow is not True else (col[0] // 4, col[1] // 4, col[2] // 4, 255)
        font_text(canvas, x + scale, y + scale, string, dark, scale, False)
    for ch in string:
        if ch != " ":
            for dx, dy in _glyph(ch)[0]:
                canvas.rect(x + dx * scale, y + dy * scale, scale, scale, col)
        x += _advance(ch) * scale
    return x - scale


def text_centred(canvas, cx, y, string, col=None, scale=1, shadow=True):
    return font_text(canvas, int(cx - text_width(string, scale) // 2), y, string, col, scale, shadow)


def wrap(string, max_px, scale=1):
    """Split string into lines no wider than max_px (breaking after spaces only, so ids like MC-73186 stay whole)."""
    words, cur = [], ""
    for ch in string:
        cur += ch
        if ch == " ":
            words.append(cur)
            cur = ""
    if cur:
        words.append(cur)
    lines, line = [], ""
    for word in words:
        if line and text_width((line + word).rstrip(" "), scale) > max_px:
            lines.append(line.rstrip(" "))
            line = word
        else:
            line += word
    if line.strip(" "):
        lines.append(line.rstrip(" "))
    return lines


def balanced_wrap(string, max_px, scale=1):
    """wrap(), but a last line shorter than a fifth of the width is avoided by narrowing the measure to the
    smallest one that keeps the same number of lines."""
    lines = wrap(string, max_px, scale)
    if len(lines) > 1 and text_width(lines[-1], scale) < max_px // 5:
        lo, hi = 1, max_px
        while lo < hi:
            mid = (lo + hi) // 2
            if len(wrap(string, mid, scale)) == len(lines):
                hi = mid
            else:
                lo = mid + 1
        lines = wrap(string, lo, scale)
    return lines


def line_height(scale=2, leading=4):
    return (8 + leading) * scale


def paragraph(canvas, x, y, string, max_px, col=None, scale=2, shadow=True, leading=4, align="left"):
    """Draw string word-wrapped to max_px; align is left, centre or right.  Returns the y below the last line."""
    for line in balanced_wrap(string, max_px, scale):
        if align == "centre":
            text_centred(canvas, x + max_px // 2, y, line, col, scale, shadow)
        elif align == "right":
            font_text(canvas, x + max_px - text_width(line, scale), y, line, col, scale, shadow)
        else:
            font_text(canvas, x, y, line, col, scale, shadow)
        y += line_height(scale, leading)
    return y


# ---------------------------------------------------------------------------
# 5. Furniture
# ---------------------------------------------------------------------------

def panel(w, h, fill=None, border=None, edge=2, ornaments=False):
    """A flat framed box: fill inside an edge-pixel border, optionally with small light corner marks."""
    fill, border = fill or PANEL_DARK, border or BORDER
    c = Canvas(w, h, fill)
    for i in range(edge):
        c.frame(i, i, w - 2 * i, h - 2 * i, border)
    if ornaments:
        n = edge * 2
        for cx, cy, dx, dy in ((edge + 2, edge + 2, 1, 1), (w - edge - 3, edge + 2, -1, 1),
                               (edge + 2, h - edge - 3, 1, -1), (w - edge - 3, h - edge - 3, -1, -1)):
            for i in range(n):
                for j in range(edge):
                    c.set(cx + dx * i, cy + dy * j, BORDER_LIGHT)
                    c.set(cx + dx * j, cy + dy * i, BORDER_LIGHT)
    return c


def card(w, h, dither=True, seed=7, fill=None):
    """The framed card: gold border with corner ornaments round a violet slate panel."""
    fill = fill or PANEL
    c = Canvas(w, h, fill)
    if dither:
        rnd = noise(seed)
        tones = (mix(fill, PANEL_DARK, 0.45), mix(fill, PANEL_DARK, 0.2), mix(fill, PANEL_MID, 0.18))
        for y in range(0, h, 4):
            for x in range(0, w, 4):
                r = rnd(16)
                if r < 6:
                    c.rect(x, y, 4, 4, tones[r % 3])
    # frame: dark lip, brass band with a lit top-left edge, dark lip, then a shadow line on the panel
    for i, col in enumerate((BORDER_DARK, BORDER_DARK, BORDER, BORDER, BORDER, BORDER, BORDER_DARK, BORDER_DARK, RIM, RIM)):
        c.frame(i, i, w - 2 * i, h - 2 * i, col)
    c.hline(2, 2, w - 4, BORDER_LIGHT)
    c.vline(2, 2, h - 4, BORDER_LIGHT)
    c.hline(2, 3, w - 5, BORDER_LIGHT)
    c.vline(3, 2, h - 5, BORDER_LIGHT)
    # corner ornaments: a brass angle with a pale stud, just inside the frame
    for cx, cy, dx, dy in ((14, 14, 1, 1), (w - 15, 14, -1, 1), (14, h - 15, 1, -1), (w - 15, h - 15, -1, -1)):
        for i in range(20):
            for j in range(4):
                c.set(cx + dx * i, cy + dy * j, BORDER)
                c.set(cx + dx * j, cy + dy * i, BORDER)
        for i in range(8):
            for j in range(8):
                c.set(cx + dx * i, cy + dy * j, BORDER_LIGHT)
        for i in range(4):
            for j in range(4):
                c.set(cx + dx * (2 + i), cy + dy * (2 + j), BRASS[5])
    return c


def heading(text, scale=4, col=None, shade=None):
    """Big lettering with a darker copy offset one font pixel down-right, on a transparent canvas."""
    col, shade = colour(col, BORDER_LIGHT), colour(shade, BORDER_DARK)
    c = Canvas(text_width(text, scale) + scale, 9 * scale)
    font_text(c, 0, 0, text, col, scale, shadow=shade)
    return c


def chip(text, style="accent", scale=2, pad=(14, 10)):
    """A tag chip.  style: accent (violet border) or brass (solid gold)."""
    if style == "brass":
        fill, border, col = BORDER, BRASS[5], INK
    else:
        fill, border, col = PANEL_MID, ACCENT, TEXT
    c = panel(text_width(text, scale) + 2 * pad[0], 7 * scale + 2 * pad[1], fill, border, 2)
    font_text(c, pad[0], pad[1], text, col, scale, shadow=False)
    return c


def chips_row(chips, gap=16):
    c = Canvas(sum(ch.w for ch in chips) + gap * (len(chips) - 1), max(ch.h for ch in chips))
    x = 0
    for ch in chips:
        c.blit(ch, x, 0)
        x += ch.w + gap
    return c


def label_box(text, scale=2, col=None, tab=None):
    """A dark name box (brass tab on the left) that sits on a picture corner."""
    c = panel(text_width(text, scale) + 22, 8 * scale + 14, RIM, BORDER_DARK, 2)
    c.rect(2, 2, 4, c.h - 4, colour(tab, BORDER))
    font_text(c, 13, 7, text, colour(col, TEXT), scale, shadow=True)
    return c


def bay(w, h, edge=2):
    """The recessed picture bay: brass rim round a dark interior with a shadow along the top and left."""
    b = panel(w, h, PANEL_DARK, BORDER_DARK, edge)
    b.hline(edge, edge, w - 2 * edge, RIM)
    b.vline(edge, edge, h - 2 * edge, RIM)
    return b


def placeholder(w, h, name):
    """The marker drawn when a capture file is missing: grey hatched slab with the file name.  Never a fake."""
    c = Canvas(w, h, MISSING_A)
    for y in range(h):
        for x in range((y % 24), w, 24):
            c.rect(x, y, 2, 1, MISSING_B)
    c.frame(0, 0, w, h, rgb("2a2931"), 2)
    lines = ["capture missing:", name]
    scale = 2 if max(text_width(t, 2) for t in lines) <= w - 24 else 1
    total = len(lines) * 8 * scale + (len(lines) - 1) * 6
    y = (h - total) // 2
    for line in lines:
        text_centred(c, w // 2, y, line, rgb("f0eef4"), scale, shadow=rgb("1c1b21"))
        y += 8 * scale + 6
    return c


def sparkle(canvas, x, y, k=4, big=True, ramp=None):
    """A glint like the ones on the logo: a plus of k-pixel blocks (big) or a single block."""
    ramp = ramp or VIOLET
    if big:
        for dx, dy in ((0, -1), (-1, 0), (1, 0), (0, 1)):
            canvas.rect(x + dx * k, y + dy * k, k, k, ramp[3])
        canvas.rect(x, y, k, k, ramp[5])
    else:
        canvas.rect(x, y, k, k, ramp[2])


def arrow(length=96, k=4, col=None, shade=None):
    """A blocky arrow pointing right in k-pixel steps with a dark outline, on a transparent canvas."""
    col, shade = colour(col, BORDER_LIGHT), colour(shade, BORDER_DARK)
    head = 5
    c = Canvas(length + 2 * k, (2 * head + 1) * k + 2 * k)
    body = set()
    cells = length // k
    for i in range(cells - head):
        for j in (-1, 0, 1):
            body.add((i, j))
    for i in range(head + 1):
        for j in range(-(head - i), head - i + 1):
            body.add((cells - head - 1 + i, j))
    for (i, j) in body:
        lit = (i, j - 1) not in body
        c.rect(k + i * k, k + (j + head) * k, k, k, BRASS[5] if lit else col)
    for (i, j) in body:
        for di, dj in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            if (i + di, j + dj) not in body:
                c.rect(k + (i + di) * k, k + (j + dj + head) * k, k, k, shade)
    return c

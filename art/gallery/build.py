#!/usr/bin/env python3
"""Glintsmith gallery: composes the CurseForge cards and the banner from in-game captures.

    py -B art/gallery/build.py                      build every image into docs/curseforge/img/
    py -B art/gallery/build.py --strict             exit 1 if any capture is missing
    py -B art/gallery/build.py --only card_gap_fix  build the images whose name contains the text
    py -B art/gallery/build.py --captures DIR --out DIR
    py -B art/gallery/build.py --list               print every capture the manifest needs and whether it exists

Reads art/gallery/cards.json and the screenshots in art/captures/ (what to capture: SHOTLIST.md).  A capture that
is not there is never faked: its picture slot shows a grey "capture missing" placeholder, a warning is printed
and --strict turns it into a failure.  The showcase GIF is built by gif.py (this script calls it unless
--no-gif is given).

The vanilla font is read from the Minecraft 1.21.1 client jar at run time (GLINTSMITH_MC_JAR, else the Loom cache);
neither the jar nor the font is stored in the repository.  The card look (framed violet slate card, heading with
a rule, recessed picture bay, text in the game font at a whole scale) is adapted from the MIT licensed showcase
scripts of Lootr Remastered (same author).  Output is byte-identical from run to run.  Standard library only.
"""
import argparse
import hashlib
import os
import sys

sys.dont_write_bytecode = True
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)

import common   # noqa: E402
import lib      # noqa: E402
from lib import Canvas   # noqa: E402

MARGIN, RULE_Y = 40, 88

LABEL_STYLES = {
    "accent": (lib.ACCENT, lib.TEXT),
    "good": (lib.GOOD, lib.TEAL[4]),
    "bad": (lib.BAD, lib.rgb("ffd0d4")),
    "dim": (lib.GREY, lib.TEXT_DIM),
    "gold": (lib.BORDER, lib.BRASS[5]),
}


# ---------------------------------------------------------------------------------------------- elements

def anchor_x(x, width, align):
    return x if align == "left" else x - width // 2 if align == "centre" else x - width


def el_capture(c, el, ctx):
    crop, down, scale = el["crop"], el.get("down", 1), el.get("scale", 1)
    w, h = common.out_size(crop, down, scale)
    x, y = el["at"]
    img = ctx.captures.get(el["capture"])
    if img is None:
        ctx.report.missing_capture(ctx.name, "art/captures/" + el["capture"])
        pic = lib.placeholder(w, h, el["capture"])
    else:
        pic = Canvas(w, h, lib.PANEL_DARK)
        pic.blit(common.cut(img, crop, down, scale), 0, 0)
    c.blit(pic, x, y)
    edge = el.get("edge", 3)
    if el.get("frame", True) and edge:
        for i, col in enumerate((lib.BORDER_DARK, lib.BORDER, lib.BORDER_DARK)[:edge]):
            c.frame(x + i, y + i, w - 2 * i, h - 2 * i, col)
    if el.get("label"):
        label(c, {"at": None, "text": el["label"], "style": el.get("label_style", "accent")},
              ctx, box=(x, y, w, h), where=el.get("label_at", "tl"))


def label(c, el, ctx, box=None, where="tl"):
    tab, col = LABEL_STYLES[el.get("style", "accent")]
    size = el.get("size", 2)
    lb = lib.label_box(el["text"], size, col, tab)
    if box is not None and lb.w > box[2] - 20 and size > 1:      # too wide for its picture: the small size
        lb = lib.label_box(el["text"], 1, col, tab)
    if box is not None:
        bx, by, bw, bh = box
        inset = 10
        x = bx + inset if where[1] == "l" else bx + bw - lb.w - inset
        y = by + inset if where[0] == "t" else by + bh - lb.h - inset
    else:
        x, y = el["at"]
    c.blit(lb, x, y)


def el_text(c, el, ctx):
    x, y = el["at"]
    width = el["width"]
    align = el.get("align", "left")          # left: at.x is the left edge; centre: the middle; right: the right edge
    lib.paragraph(c, anchor_x(x, width, align), y, el["text"], width, lib.colour(el.get("color", "text")),
                  el.get("size", 2), el.get("shadow", True), el.get("leading", 4), align)


def el_bullets(c, el, ctx):
    x, y = el["at"]
    width, size, gap = el["width"], el.get("size", 2), el.get("gap", 12)
    col = lib.colour(el.get("color", "text"))
    lh = lib.line_height(size)
    for text in el["items"]:
        c.rect(x, y + 3, 8, 8, lib.ACCENT)
        c.rect(x, y + 3, 2, 2, lib.ACCENT_LIGHT)
        for line in lib.balanced_wrap(text, width - 24, size):
            lib.font_text(c, x + 24, y, line, col, size)
            y += lh
        y += gap


def chip_list(items):
    out = []
    for it in items:
        if isinstance(it, str):
            it = {"text": it}
        out.append(lib.chip(it["text"], it.get("style", "accent")))
    return out


def el_chips(c, el, ctx):
    row = lib.chips_row(chip_list(el["items"]), el.get("gap", 16))
    x, y = el["at"]
    c.blit(row, anchor_x(x, row.w, el.get("align", "left")), y)


def el_rule(c, el, ctx):
    x, y = el["at"]
    c.rect(x, y, el.get("width", 36), el.get("height", 2), lib.colour(el.get("color", "accent")))


def el_panel(c, el, ctx):
    x, y = el["at"]
    w, h = el["size"]
    fill = {"dark": lib.PANEL_DARK, "mid": lib.PANEL_MID, "slate": lib.PANEL}[el.get("fill", "dark")]
    c.blit(lib.panel(w, h, fill, lib.colour(el.get("border", "#754b1d")), el.get("edge", 2), el.get("ornaments", False)), x, y)


def el_heading(c, el, ctx):
    x, y = el["at"]
    size = el["size"]
    col, shade = lib.colour(el.get("color", lib.BRASS[4])), lib.colour(el.get("shade", lib.BRASS[1]))
    if el.get("shadow", 0):
        d = el["shadow"] * size
        lib.font_text(c, x + d, y + d, el["text"], lib.RIM, size, shadow=False)
    c.blit(lib.heading(el["text"], size, col, shade), x, y)


def el_strip(c, el, ctx):
    x, y = el["at"]
    width, size = el["width"], el.get("size", 2)
    lines = lib.balanced_wrap(el["text"], width - 40, size)
    lh = (8 + 2) * size + 2
    strip = lib.panel(width, 2 * 13 + lh * len(lines) - 2, lib.PANEL_DARK, lib.BORDER, 2, ornaments=True)
    for i, line in enumerate(lines):
        lib.text_centred(strip, strip.w // 2, 13 + lh * i, line, lib.TEXT, size)
    c.rect(x + 6, y + 6, strip.w, strip.h, lib.RIM)
    c.blit(strip, x, y)


def el_logo(c, el, ctx):
    x, y = el["at"]
    logo = Canvas.open(os.path.join(lib.ART, "logo", "logo_native.png")).scaled(el.get("scale", 5))
    edge = el.get("edge", 4)
    n = logo.w + 2 * edge + 4
    framed = Canvas(n + 6, n + 6)
    framed.rect(6, 6, n, n, lib.RIM)
    framed.blit(lib.panel(n, n, lib.RIM, lib.BORDER, edge), 0, 0)
    framed.hline(0, 0, n, lib.BORDER_LIGHT)
    framed.vline(0, 0, n, lib.BORDER_LIGHT)
    framed.blit(logo, edge + 2, edge + 2)
    c.blit(framed, x, y)


def el_arrow(c, el, ctx):
    x, y = el["at"]
    c.blit(lib.arrow(el.get("length", 96), el.get("k", 4)), x, y)


def el_sparkle(c, el, ctx):
    x, y = el["at"]
    lib.sparkle(c, x, y, el.get("k", 4), el.get("big", True))


ELEMENTS = {
    "capture": el_capture, "label": label, "text": el_text, "bullets": el_bullets, "chips": el_chips, "rule": el_rule,
    "panel": el_panel, "heading": el_heading, "strip": el_strip, "logo": el_logo, "arrow": el_arrow,
    "sparkle": el_sparkle,
}


class Ctx:
    def __init__(self, name, captures, report):
        self.name, self.captures, self.report = name, captures, report


# ---------------------------------------------------------------------------------------------- cards

def build_card(spec, captures, report):
    w, h = spec["size"]
    c = lib.card(w, h, seed=spec.get("seed", 7))
    title = spec.get("title")
    if title:
        size = spec.get("title_size") or (5 if lib.text_width(title, 5) + 5 <= w - 2 * MARGIN else 4)
        c.blit(lib.heading(title, size), MARGIN, 34 + (45 - 9 * size) // 2)
        c.rect(MARGIN, RULE_Y, w - 2 * MARGIN, 2, lib.PANEL_MID)
        c.rect(MARGIN, RULE_Y, 200, 2, lib.BORDER)
    ctx = Ctx(spec["out"], captures, report)
    for el in spec.get("elements", []):
        ELEMENTS[el["type"]](c, el, ctx)
    return c


def build_all(manifest, captures, report, only=None, log=None):
    """Build every card of the manifest.  Returns {file name: PNG bytes}."""
    out = {}
    for spec in manifest["cards"]:
        if only and only not in spec["out"]:
            continue
        if log:
            log("card  " + spec["out"])
        out[spec["out"]] = build_card(spec, captures, report).to_png()
    return out


def main(argv=None):
    ap = argparse.ArgumentParser(description=__doc__.split("\n\n")[0], formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--manifest", default=common.DEFAULT_MANIFEST)
    ap.add_argument("--captures", default=common.DEFAULT_CAPTURES, help="folder with the capture PNGs")
    ap.add_argument("--out", default=common.DEFAULT_OUT, help="folder to write the images to")
    ap.add_argument("--only", help="build only the images whose file name contains this text")
    ap.add_argument("--strict", action="store_true", help="exit 1 if any capture is missing")
    ap.add_argument("--list", action="store_true", help="list the captures the manifest needs and exit")
    ap.add_argument("--no-gif", action="store_true", help="skip the showcase GIF")
    ap.add_argument("--check", action="store_true", help="print a sha256 per written file")
    args = ap.parse_args(argv)

    manifest = common.load_manifest(args.manifest)
    captures = common.Captures(args.captures)
    if args.list:
        seen = set()
        for image, rel in common.required_captures(manifest):
            if rel in seen:
                continue
            seen.add(rel)
            state = "ok     " if (rel.endswith("NNNN.png") and os.path.isdir(captures.path(rel.rsplit("/", 1)[0]))) or captures.exists(rel) else "MISSING"
            print("%s %s   (%s)" % (state, rel, image))
        return 0

    report = common.Report()
    written = {}
    for name, data in build_all(manifest, captures, report, args.only, lambda m: print(m, flush=True)).items():
        written[name] = (common.write(args.out, name, data), data)
    if not args.no_gif:
        import gif
        for name, data in gif.build_all(manifest, captures, report, args.only, lambda m: print(m, flush=True)).items():
            written[name] = (common.write(args.out, name, data), data)
    for name, (path, data) in sorted(written.items()):
        print("wrote %s (%d bytes)%s" % (path, len(data), "  sha256 " + hashlib.sha256(data).hexdigest()[:16] if args.check else ""))
    if report:
        print("%d capture(s) missing: those picture slots show the grey placeholder." % len({m[1] for m in report.missing}),
              file=sys.stderr)
        if args.strict:
            return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())

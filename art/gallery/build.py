#!/usr/bin/env python3
"""Glintsmith gallery: composes the CurseForge cards and the banner from in-game captures.

    py -B art/gallery/build.py                      build every image into docs/curseforge/img/
    py -B art/gallery/build.py --strict             exit 1 if any capture is missing
    py -B art/gallery/build.py --only card_gap_fix  build the images whose name contains the text
    py -B art/gallery/build.py --captures DIR --out DIR
    py -B art/gallery/build.py --list               print every capture the manifest needs and whether it exists

Reads art/gallery/cards.json and the screenshots in art/captures/ (what to capture: SHOTLIST.md).  An element of type
"studio" ({"scene", "frame", "at", "size", "shadow", ...}) sets a matted render from art/captures/studio/<scene>/ (see
art/studio/) straight on the card, no frame, with an optional contact shadow (see el_studio for crop, trim and labels).
A capture or studio frame that is not there is never faked: its picture slot shows a grey "capture missing"
placeholder, a warning is printed and --strict turns it into a failure.  The GIFs of the manifest are studio
turntables built by gifs.py (this script calls it unless --no-gif is given).

The vanilla font is read from the Minecraft 1.21.1 client jar at run time (GLINTSMITH_MC_JAR, else the Loom cache);
neither the jar nor the font is stored in the repository.  The card look (framed cream-white card, heading with
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

# label style -> (tab colour, text colour); the text must keep 4.5:1 against the cream label box (lib.PANEL; ratios are in art/README.md)
LABEL_STYLES = {
    "accent": (lib.ACCENT, lib.TEXT),
    "good": (lib.GOOD, lib.GOOD),
    "bad": (lib.BAD, lib.BAD),
    "dim": (lib.GREY, lib.TEXT_DIM),
    "gold": (lib.BORDER, lib.GOLD_TEXT),
}


# ---------------------------------------------------------------------------------------------- elements

def anchor_x(x, width, align):
    return x if align == "left" else x - width // 2 if align == "centre" else x - width


def el_capture(c, el, ctx):
    w, h = common.picture_size(el)
    x, y = el["at"]
    img = ctx.captures.get(el["capture"])
    if img is None:
        ctx.report.missing_capture(ctx.name, "art/captures/" + el["capture"])
        pic = lib.placeholder(w, h, el["capture"])
    else:
        pic = Canvas(w, h, lib.PANEL_DARK)
        pic.blit(common.cut_el(img, el), 0, 0)
    c.blit(pic, x, y)
    # a thin darker edge (2 px: dark brass, then soft tan) so that a white in-game backdrop does not melt into the cream card
    edge = el.get("edge", 2)
    if el.get("frame", True) and edge:
        for i, col in enumerate((lib.BORDER_DARK, lib.RIM, lib.BORDER_DARK)[:edge]):
            c.frame(x + i, y + i, w - 2 * i, h - 2 * i, col)
    if el.get("label"):
        label(c, {"at": None, "text": el["label"], "style": el.get("label_style", "accent")},
              ctx, box=(x, y, w, h), where=el.get("label_at", "tl"))


def label(c, el, ctx, box=None, where="tl", inset=10):
    tab, col = LABEL_STYLES[el.get("style", "accent")]
    size = el.get("size", 2)
    lb = lib.label_box(el["text"], size, col, tab)
    if box is not None and lb.w > box[2] - 20 and size > 1:      # too wide for its picture: the small size
        lb = lib.label_box(el["text"], 1, col, tab)
    if box is not None:                                   # where: tl, tr, bl, br, tc or bc (top / bottom centre)
        bx, by, bw, bh = box
        x = bx + inset if where[1] == "l" else bx + (bw - lb.w) // 2 if where[1] == "c" else bx + bw - lb.w - inset
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
    c.blit(lib.panel(w, h, fill, lib.colour(el.get("border", lib.BORDER_DARK)), el.get("edge", 2), el.get("ornaments", False)), x, y)


def el_heading(c, el, ctx):
    x, y = el["at"]
    size = el["size"]
    col, shade = lib.colour(el.get("color", lib.TITLE)), lib.colour(el.get("shade", lib.BORDER))
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


def studio_matte(ctx, scene_id, ref):
    """The matted render of a studio scene frame (Canvas), or the repository-relative file that is missing.
    Returns (canvas, None) or (None, missing file)."""
    sys.path.insert(0, os.path.join(common.ART, "studio"))
    try:
        import scene as studio_scene
        import pipeline
    finally:
        sys.path.pop(0)
    cache = ctx.captures.__dict__.setdefault("_studio", {})
    try:
        sc = studio_scene.load(ctx.captures.folder, scene_id)
    except studio_scene.SceneError as e:
        if e.kind == "missing":
            return None, "studio/%s/scene.json" % scene_id
        sys.exit("error: %s" % e)
    try:
        frame = studio_scene.find_frame(sc, ref)
    except studio_scene.SceneError as e:
        sys.exit("error: %s" % e)
    for path in sc.pair(frame):
        if not os.path.isfile(path):
            return None, "studio/%s/%s" % (scene_id, os.path.basename(path))
    key = (scene_id, frame["index"])
    if key not in cache:
        try:
            cache[key] = pipeline.matte_frame(sc, frame)
        except (ValueError, OSError) as e:
            sys.exit("error: cannot matte studio frame %s of %s: %s" % (frame["file"], scene_id, e))
    return cache[key], None


def shape_matte(pic, el):
    """The matte after the element's `crop` ([x, y, w, h] in matte pixels, x / y may be "c"), `up` (whole number: the crop
    enlarged by nearest neighbour, for a detail of a render such as a one pixel crack) and `trim` ("x", "y" or "xy":
    cut the empty rows / columns round the subject, keeping `trim_margin` matte pixels, default 8).  Trimming only the
    height ("y") keeps the scale of two rows of the same width equal."""
    sys.path.insert(0, os.path.join(common.ART, "studio"))
    try:
        import composite
    finally:
        sys.path.pop(0)
    if el.get("crop"):
        x, y, w, h = common.resolve_crop(el["crop"], pic.w, pic.h)
        pic = pic.crop(x, y, w, h)
    if el.get("up", 1) > 1:
        pic = pic.scaled(el["up"])
    trim = el.get("trim")
    if trim:
        box = composite.alpha_bbox(bytes(pic.buf[3::4]), pic.w, pic.h, 8)
        if box:
            m = el.get("trim_margin", 8)
            x0, y0, x1, y1 = box
            if "x" not in trim:
                x0, x1 = 0, pic.w
            if "y" not in trim:
                y0, y1 = 0, pic.h
            x0, y0, x1, y1 = max(0, x0 - m), max(0, y0 - m), min(pic.w, x1 + m), min(pic.h, y1 + m)
            pic = pic.crop(x0, y0, x1 - x0, y1 - y0)
    return pic


def el_studio(c, el, ctx):
    """A studio render (transparent matte of a K / W frame pair, no frame or box) set straight on the card background,
    fitted inside `size` at `at` (only ever scaled down, area filter) and centred; "shadow": true lays the soft contact
    shadow under it.  Optional: `crop` / `trim` / `trim_margin` (see shape_matte), `style` (composite style overrides),
    `border` (true: a thin brass frame round the picture, for a detail crop) and `label`, `label_style`, `label_at` (tl, tr, bl, br, tc, bc: a name box inside the `size` rectangle) or `label_below`
    (a name box centred just under the rectangle, which then holds only the picture)."""
    x, y = el["at"]
    w, h = el["size"]
    pic, missing = studio_matte(ctx, el["scene"], el["frame"])
    if pic is None:
        ctx.report.missing_capture(ctx.name, "art/captures/" + missing)
        c.blit(lib.placeholder(w, h, "studio/%s/%s" % (el["scene"], el["frame"])), x, y)
    else:
        sys.path.insert(0, os.path.join(common.ART, "studio"))
        try:
            import composite
        finally:
            sys.path.pop(0)
        bx, by, bw, bh = composite.place_on_canvas(c, shape_matte(pic, el), (x, y, w, h), el.get("shadow", True),
                                                   composite.style(el.get("style")))
        if el.get("border"):
            c.frame(bx - 2, by - 2, bw + 4, bh + 4, lib.BORDER, 2)
    if el.get("label_below"):                              # a name box centred just under the rectangle
        label(c, {"at": None, "text": el["label_below"], "style": el.get("label_style", "accent")}, ctx,
              box=(x, y + h, w, 0), where="tc", inset=4)
    elif el.get("label"):
        label(c, {"at": None, "text": el["label"], "style": el.get("label_style", "accent")}, ctx,
              box=(x, y, w, h), where=el.get("label_at", "tl"))


ELEMENTS = {
    "capture": el_capture, "label": label, "text": el_text, "bullets": el_bullets, "chips": el_chips, "rule": el_rule,
    "panel": el_panel, "heading": el_heading, "strip": el_strip, "studio": el_studio,
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
        c.rect(MARGIN, RULE_Y, w - 2 * MARGIN, 2, lib.RIM)
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
    ap.add_argument("--no-gif", action="store_true", help="skip the GIFs (they take minutes)")
    ap.add_argument("--workers", type=int, default=None, help="worker processes of the GIF builds (default up to 8)")
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
            state = common.studio_state(args.captures, rel) if rel.startswith("studio/") else "ok" if captures.exists(rel) else "MISSING"
            print("%-7s %s   (%s)" % (state, rel, image))
        return 0

    report = common.Report()
    written = {}
    for name, data in build_all(manifest, captures, report, args.only, lambda m: print(m, flush=True)).items():
        written[name] = (common.write(args.out, name, data), data)
    if not args.no_gif:
        import gifs
        for name, data in gifs.build_all(manifest, captures, report, args.only, lambda m: print(m, flush=True), args.workers).items():
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

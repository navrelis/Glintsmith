"""Shared helpers of the gallery builders: manifest loading, capture loading, the missing-input report.

Python 3 standard library only.
"""
import json
import os
import sys

sys.dont_write_bytecode = True
HERE = os.path.dirname(os.path.abspath(__file__))
ART = os.path.dirname(HERE)
sys.path.insert(0, ART)

import lib   # noqa: E402

ROOT = lib.ROOT
DEFAULT_MANIFEST = os.path.join(HERE, "cards.json")
DEFAULT_CAPTURES = os.path.join(ART, "captures")
DEFAULT_OUT = os.path.join(ROOT, "docs", "curseforge", "img")


class Report:
    """Collects what could not be built from real captures (it is never faked)."""

    def __init__(self):
        self.missing = []     # (image, file)
        self.notes = []

    def missing_capture(self, image, rel):
        self.missing.append((image, rel))
        if rel not in {r for _, r in self.missing[:-1]}:         # one warning per file
            print("warning: capture missing: %s (needed by %s)" % (rel, image), file=sys.stderr)

    def note(self, msg):
        self.notes.append(msg)
        print(msg, file=sys.stderr)

    def __bool__(self):
        return bool(self.missing)


def load_manifest(path=DEFAULT_MANIFEST):
    with open(path, encoding="utf-8") as f:
        return json.load(f)


class Captures:
    """Reads capture PNGs from one folder and keeps them decoded for the length of a build."""

    def __init__(self, folder):
        self.folder = folder
        self._cache = {}

    def path(self, rel):
        return os.path.join(self.folder, *rel.split("/"))

    def exists(self, rel):
        return os.path.isfile(self.path(rel))

    def drop(self, rel):
        """Forget a decoded capture (frames of a GIF are read once; this keeps memory flat)."""
        self._cache.pop(rel, None)

    def get(self, rel):
        """The capture as a Canvas, or None when the file is not there.  A file that is there but cannot be
        decoded is an error, not a placeholder."""
        if rel not in self._cache:
            p = self.path(rel)
            if not os.path.isfile(p):
                self._cache[rel] = None
            else:
                try:
                    self._cache[rel] = lib.Canvas.open(p)
                except (ValueError, OSError) as e:
                    sys.exit("error: cannot read capture %s: %s" % (p, e))
        return self._cache[rel]


def resolve_crop(crop, img_w, img_h):
    """crop is [x, y, w, h]; x and y may be "c" to centre the rectangle on the image."""
    x, y, w, h = crop
    if x == "c":
        x = (img_w - w) // 2
    if y == "c":
        y = (img_h - h) // 2
    return int(x), int(y), int(w), int(h)


def cut(img, crop, down=1, scale=1, size=None):
    """The crop of a capture, then either box-downscaled by `down` and scaled up by `scale` (whole factors only), or,
    when `size` [w, h] is given, resized to exactly that size with the area filter (`resample: "area"`)."""
    x, y, w, h = resolve_crop(crop, img.w, img.h)
    out = img.crop(x, y, w, h)
    if size:
        return out.area(int(size[0]), int(size[1]))
    if down > 1:
        out = out.down(down)
    if scale > 1:
        out = out.scaled(scale)
    return out


def out_size(crop, down=1, scale=1, size=None):
    if size:
        return (int(size[0]), int(size[1]))
    return (crop[2] // down * scale, crop[3] // down * scale)


def picture_size(el):
    """Size on the card of a capture element or GIF entry: `size` with `resample: "area"`, else crop / down * scale."""
    return out_size(el["crop"], el.get("down", 1), el.get("scale", 1), el.get("size") if el.get("resample") == "area" else None)


def cut_el(img, el):
    """The picture of a capture element: crop, then the resampling the element asks for (see cut)."""
    return cut(img, el["crop"], el.get("down", 1), el.get("scale", 1), el.get("size") if el.get("resample") == "area" else None)


def write(folder, name, data):
    os.makedirs(folder, exist_ok=True)
    path = os.path.join(folder, name)
    with open(path, "wb") as f:
        f.write(data)
    return path


def required_captures(manifest):
    """Every capture path the manifest needs, in manifest order: [(image name, relative path)]."""
    out = []
    for card in manifest["cards"]:
        for el in card.get("elements", []):
            if el.get("type") == "capture":
                out.append((card["out"], el["capture"]))
            elif el.get("type") == "studio":
                out.append((card["out"], "studio/%s/scene.json" % el["scene"]))
    for gif in manifest.get("gifs", []):
        out.append((gif["out"], gif["frames_dir"] + "/NNNN.png"))
    logo = manifest.get("logo")
    if logo:
        out.append(("logo.png, icon.png", logo["capture"]))
        for rel in logo.get("gif", {}).get("captures", []):
            out.append((logo["gif"]["out"], rel))
    return out

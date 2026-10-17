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


def cut(img, crop, down=1, scale=1):
    """The crop of a capture, box-downscaled by `down`, then scaled up by `scale` (whole factors only)."""
    x, y, w, h = resolve_crop(crop, img.w, img.h)
    out = img.crop(x, y, w, h)
    if down > 1:
        out = out.down(down)
    if scale > 1:
        out = out.scaled(scale)
    return out


def out_size(crop, down=1, scale=1):
    return (crop[2] // down * scale, crop[3] // down * scale)


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
    for gif in manifest.get("gifs", []):
        out.append((gif["out"], gif["frames_dir"] + "/NNNN.png"))
    return out

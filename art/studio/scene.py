"""Studio scenes: finding, loading and validating the frame contract.  Python 3 standard library only.

The in-game studio writes, per scene, a folder art/captures/studio/<scene_id>/ (git-ignored) with

    scene.json     {"scene": id, "width": W, "height": H, "supersample": S, "fps": F,
                    "frames": [{"index": 1, "file": "0001", "look": "...", "caption": "...", "name": "<still name or null>"}, ...],
                    "looks": [{"id": ..., "caption": ...}], "commit": "...", "created": "..."}
    NNNN_k.png     frame NNNN (1 based, 4 digits) rendered over pure black
    NNNN_w.png     the same frame over pure white; 8 bit RGB or RGBA (alpha ignored), (W * S) x (H * S) pixels

`validate()` lists what is wrong as (kind, message) pairs: kind "missing" for files that are not there yet (the
builders then write nothing and say so), kind "error" for a scene that is there but broken (wrong size, bad JSON, fields).

    load(captures_dir, scene_id)  -> Scene   (SceneError if the folder or scene.json is not there)
    scene_ids(captures_dir)       -> folders under studio/ that look like scenes
    validate(scene)               -> [(kind, message)]
    find_frame(scene, ref)        -> the frame dict for a still name, a frame index or a file stem
"""
import json
import os
import sys

sys.dont_write_bytecode = True
HERE = os.path.dirname(os.path.abspath(__file__))
ART = os.path.dirname(HERE)
if ART not in sys.path:
    sys.path.insert(0, ART)

import imgio   # noqa: E402

REQUIRED = ("scene", "width", "height", "supersample", "fps", "frames")
RECOMMENDED = ("looks", "commit", "created")


class SceneError(Exception):
    """A scene that is not there (kind 'missing') or not readable (kind 'error')."""

    def __init__(self, message, kind="missing"):
        super().__init__(message)
        self.kind = kind


def studio_root(captures):
    return os.path.normpath(os.path.join(captures, "studio"))


def scene_dir(captures, scene_id):
    return os.path.normpath(os.path.join(studio_root(captures), scene_id))


def scene_ids(captures):
    root = studio_root(captures)
    if not os.path.isdir(root):
        return []
    return sorted(d for d in os.listdir(root) if os.path.isdir(os.path.join(root, d)))


class Scene:
    def __init__(self, folder, data):
        self.dir = folder
        self.data = data
        self.id = data.get("scene", os.path.basename(folder))
        self.width = int(data["width"])
        self.height = int(data["height"])
        self.supersample = int(data["supersample"])
        self.fps = float(data["fps"])
        self.frames = sorted(data["frames"], key=lambda f: f["index"])

    def pair(self, frame):
        """(K path, W path) of a frame dict."""
        return (os.path.join(self.dir, "%s_k.png" % frame["file"]), os.path.join(self.dir, "%s_w.png" % frame["file"]))


def load(captures, scene_id):
    """The scene from art/captures/studio/<scene_id>/scene.json.  Raises SceneError."""
    folder = scene_dir(captures, scene_id)
    path = os.path.join(folder, "scene.json")
    if not os.path.isdir(folder):
        raise SceneError("studio scene %r is not there: %s does not exist" % (scene_id, folder))
    if not os.path.isfile(path):
        raise SceneError("studio scene %r has no scene.json (%s)" % (scene_id, path))
    try:
        with open(path, encoding="utf-8") as f:
            data = json.load(f)
        missing = [k for k in REQUIRED if k not in data]
        if missing:
            raise SceneError("%s lacks the fields: %s" % (path, ", ".join(missing)), "error")
        scene = Scene(folder, data)
        if not scene.frames or scene.width < 1 or scene.height < 1 or scene.supersample < 1 or scene.fps <= 0:
            raise SceneError("%s: width, height, supersample, fps and frames must be positive / not empty" % path, "error")
        return scene
    except (ValueError, TypeError, KeyError) as e:
        raise SceneError("%s is not a valid scene.json: %s" % (path, e), "error")


def validate(scene, check_sizes=True):
    """[(kind, message)] for a loaded scene: missing frame files, wrong sizes, field problems.  Empty when fine."""
    problems = []
    d = scene.data
    for key in RECOMMENDED:
        if key not in d:
            problems.append(("error", "scene.json has no %r" % key))
    if d.get("scene") != os.path.basename(scene.dir):
        problems.append(("error", "scene.json says scene %r but the folder is %r" % (d.get("scene"), os.path.basename(scene.dir))))
    want = list(range(1, len(scene.frames) + 1))
    got = [f.get("index") for f in scene.frames]
    if got != want:
        problems.append(("error", "frame indexes must be 1..%d without gaps (found %s)" % (len(want), got[:6])))
    expect = (scene.width * scene.supersample, scene.height * scene.supersample)
    sizes = {}
    for f in scene.frames:
        for key in ("index", "file"):
            if key not in f:
                problems.append(("error", "frame entry %r has no %r" % (f, key)))
        if "file" not in f or "index" not in f:
            continue
        for key in ("look", "caption", "name"):
            if key not in f:
                problems.append(("error", "frame %s has no %r (null is fine for name)" % (f["file"], key)))
        if f["file"] != "%04d" % f["index"]:
            problems.append(("error", "frame %s: file should be %04d" % (f["file"], f["index"])))
        for path in scene.pair(f):
            if not os.path.isfile(path):
                problems.append(("missing", "missing %s" % os.path.relpath(path, scene.dir).replace("\\", "/")))
            elif check_sizes:
                try:
                    size = imgio.read_png_size(path)
                except (AssertionError, OSError, ValueError):
                    problems.append(("error", "%s is not a PNG" % os.path.basename(path)))
                    continue
                sizes[path] = size
                if size != expect:
                    problems.append(("error", "%s is %dx%d, expected %dx%d (%dx%d x supersample %d)" % (
                        os.path.basename(path), size[0], size[1], expect[0], expect[1], scene.width, scene.height, scene.supersample)))
    return problems


def find_frame(scene, ref):
    """The frame dict for `ref`: a still name, a frame index (1 based) or a file stem such as 0007."""
    ref = str(ref)
    for f in scene.frames:
        if f.get("name") and f["name"] == ref:
            return f
    if ref.isdigit():
        n = int(ref)
        for f in scene.frames:
            if f["index"] == n:
                return f
    for f in scene.frames:
        if f.get("file") == ref:
            return f
    names = [f["name"] for f in scene.frames if f.get("name")]
    raise SceneError("scene %s has no frame %r (stills: %s; indexes 1..%d)" % (
        scene.id, ref, ", ".join(names) or "none", len(scene.frames)), "error")

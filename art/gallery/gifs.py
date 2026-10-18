#!/usr/bin/env python3
"""Glintsmith gallery: builds the GIFs of docs/curseforge/img/ from the in-game studio's turntables.

    py -B art/gallery/gifs.py                    build every GIF of the manifest (minutes; --workers N processes)
    py -B art/gallery/gifs.py --only lineup      only the GIFs whose name contains the text
    py -B art/gallery/gifs.py --strict           exit 1 if a scene is missing
    py -B art/gallery/gifs.py --captures DIR --out DIR

Each entry of "gifs" in art/gallery/cards.json names a studio scene (art/captures/studio/<scene>/, the frame contract of
art/studio/scene.py), the picture `size` [w, h] the turntable is fitted into, `caption` (the caption bar of the frame's
`caption` under the picture, true by default), `budget_mb`, an optional start frame rate `fps` and an optional `min_scale`
(the budget ladder of art/studio/pipeline.py steps the frame rate down and then the scale down until the file fits; a
`min_scale` of 1 never shrinks the picture and ends the build with an error instead).  The work is done by
art/studio/pipeline.build_gif (matte, studio backdrop and shadows, one global palette, ordered dither on the subject, delta
frames, endless loop).  When a scene is not there nothing is faked: a one frame grey "capture missing" GIF is written, a
warning is printed and --strict fails.  A scene that is there but broken ends the run with an error.

Standard library only.  Byte-identical output from run to run, for any number of workers.
"""
import argparse
import os
import sys

sys.dont_write_bytecode = True
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
sys.path.insert(0, os.path.join(os.path.dirname(HERE), "studio"))

import common   # noqa: E402
import imgio    # noqa: E402
import lib      # noqa: E402
import pipeline   # noqa: E402
import scene as studio_scene   # noqa: E402

MB = 1024 * 1024


def bar_height(spec):
    return pipeline.composite.DEFAULT_STYLE["bar"]["height"] if spec.get("caption", True) else 0


def build_gif(spec, captures, report, workers=None, log=None):
    """The GIF of one manifest entry as bytes (a grey one frame placeholder when the scene is not there)."""
    w, h = spec["size"]
    bh = bar_height(spec)

    def placeholder(what):
        report.missing_capture(spec["out"], "art/captures/" + what)
        ph = lib.placeholder(w, h + bh, "studio/" + spec["scene"])
        return imgio.encode_gif_frames(w, h + bh, [ph.rgb_bytes()], [100], n_colors=255)

    try:
        sc = studio_scene.load(captures.folder, spec["scene"])
    except studio_scene.SceneError as e:
        if e.kind == "missing":
            return placeholder("studio/%s/scene.json" % spec["scene"])
        sys.exit("error: %s" % e)
    problems = studio_scene.validate(sc)
    errors = [m for k, m in problems if k == "error"]
    if errors:
        sys.exit("error: scene %s does not follow the frame contract:\n  %s" % (spec["scene"], "\n  ".join(errors[:12])))
    if problems:
        return placeholder("studio/%s/%s" % (spec["scene"], problems[0][1].split()[-1]))
    data, info = pipeline.build_gif(sc, (w, h), spec.get("caption", True), spec.get("budget_mb", 8.0), spec.get("fps"),
                                    workers or min(8, os.cpu_count() or 1), pipeline.composite.style(spec.get("style")),
                                    spec.get("dither", 10), log, spec.get("min_scale", 0.0))
    if len(info["tried"]) > 1:
        report.note("note: %s needed %s to fit %.1f MB: %dx%d, %g fps, %.2f MB" % (
            spec["out"], "the step-down ladder (" + " | ".join(info["tried"]) + ")", spec.get("budget_mb", 8.0),
            info["width"], info["height"], info["fps"], info["bytes"] / MB))
    return data


def build_all(manifest, captures, report, only=None, log=None, workers=None):
    """Build every GIF of the manifest.  Returns {file name: bytes}."""
    out = {}
    for spec in manifest.get("gifs", []):
        if only and only not in spec["out"]:
            continue
        if log:
            log("gif   %s  (studio scene %s)" % (spec["out"], spec["scene"]))
        out[spec["out"]] = build_gif(spec, captures, report, workers, log)
    return out


def main(argv=None):
    ap = argparse.ArgumentParser(description=__doc__.split("\n\n")[0])
    ap.add_argument("--manifest", default=common.DEFAULT_MANIFEST)
    ap.add_argument("--captures", default=common.DEFAULT_CAPTURES)
    ap.add_argument("--out", default=common.DEFAULT_OUT)
    ap.add_argument("--only")
    ap.add_argument("--workers", type=int, default=None, help="worker processes (default up to 8)")
    ap.add_argument("--strict", action="store_true", help="exit 1 if a scene is missing")
    args = ap.parse_args(argv)
    report = common.Report()
    manifest = common.load_manifest(args.manifest)
    for name, data in build_all(manifest, common.Captures(args.captures), report, args.only,
                                lambda m: print(m, flush=True), args.workers).items():
        print("wrote %s (%d bytes)" % (common.write(args.out, name, data), len(data)))
    return 1 if report and args.strict else 0


if __name__ == "__main__":
    sys.exit(main())

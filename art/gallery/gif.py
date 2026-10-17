#!/usr/bin/env python3
"""Glintsmith gallery: builds docs/curseforge/img/showcase.gif from numbered in-game frames.

    py -B art/gallery/gif.py                 build the GIFs of the manifest
    py -B art/gallery/gif.py --strict        exit 1 if the frames are missing
    py -B art/gallery/gif.py --captures DIR --out DIR

Input: art/captures/showcase/0001.png, 0002.png, ... (any names that sort in play order), all the same size and
camera (see SHOTLIST.md, "Showcase GIF").  Each frame is cropped (and scaled by a whole factor if the manifest
says so), every `step`-th frame is kept, a caption bar in the vanilla font is added under the picture, and one
global 255 colour palette is used with delta frames (pixels that do not change between frames cost nothing).
The frames are meant to be 20 fps (`source_fps`); a kept frame lasts step / fps seconds, so the clip keeps its
length.  The GIF loops forever and must stay below `max_mb` (8 MB by default): if it does not, the step is raised
(up to `max_step`) and, failing that, the build stops and says which knobs to turn (crop, down).  If the frames are not there nothing is faked: a one frame grey
"capture missing" GIF is written instead, a warning is printed and --strict fails.

Standard library only.  Byte-identical output from run to run.
"""
import argparse
import os
import sys

sys.dont_write_bytecode = True
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)

import common   # noqa: E402
import imgio    # noqa: E402
import lib      # noqa: E402
from lib import Canvas   # noqa: E402

MB = 1024 * 1024


def frame_files(captures, frames_dir):
    folder = captures.path(frames_dir)
    if not os.path.isdir(folder):
        return []
    return sorted(f for f in os.listdir(folder) if f.lower().endswith(".png"))


def caption_for(spec, n):
    """The caption of the n-th (1 based) source frame: `captions` are the looks in order, each lasting
    `frames_per_caption` source frames."""
    caps = spec.get("captions", [])
    if not caps:
        return ""
    return caps[min((n - 1) // spec.get("frames_per_caption", 40), len(caps) - 1)]


def bar(width, height, text, scale, colour):
    b = Canvas(width, height, lib.PAGE)
    b.hline(0, 0, width, lib.BORDER_DARK)
    b.hline(0, 1, width, lib.BORDER)
    if text:
        lib.text_centred(b, width // 2, (height - 7 * scale) // 2 + 1, text, lib.colour(colour), scale)
    return b


def build_gif(spec, captures, report, log=None):
    """Returns the GIF as bytes."""
    crop, down, scale = spec["crop"], spec.get("down", 1), spec.get("scale", 1)
    w, h = common.picture_size(spec)
    cap = spec.get("caption_bar")
    bar_h = cap["height"] if cap else 0
    files = frame_files(captures, spec["frames_dir"])
    if not files:
        report.missing_capture(spec["out"], "art/captures/%s/NNNN.png" % spec["frames_dir"])
        ph = lib.placeholder(w, h + bar_h, "%s/NNNN.png" % spec["frames_dir"])
        return imgio.encode_gif_frames(w, h + bar_h, [ph.rgb_bytes()], [100], n_colors=255)

    step = max(1, spec.get("step", 1))
    limit = spec.get("max_mb", 8) * MB
    fps = spec.get("source_fps", 20)
    while True:
        data = encode(spec, captures, files, step, fps, (w, h, bar_h), log)
        if len(data) < limit:
            return data
        if step >= spec.get("max_step", 4):
            sys.exit("error: %s is %.1f MB, the limit is %.0f MB even at step %d.  Crop a smaller region or set "
                     "`down` (now %d) in cards.json." % (spec["out"], len(data) / MB, limit / MB, step, down))
        step += 1
        if log:
            log("      %.1f MB is over the %.0f MB limit: retrying with every %d. frame (same length, fewer frames)"
                % (len(data) / MB, limit / MB, step))


def encode(spec, captures, files, step, fps, dims, log):
    """The GIF made of every `step`-th frame; one kept frame lasts step / fps seconds, so the length does not change."""
    w, h, bar_h = dims
    crop, down, scale = spec["crop"], spec.get("down", 1), spec.get("scale", 1)
    cap = spec.get("caption_bar")
    chosen = [(i + 1, f) for i, f in enumerate(files) if i % step == 0]
    frames, size = [], None
    for n, name in chosen:
        path = captures.path(spec["frames_dir"] + "/" + name)
        try:
            fw, fh = imgio.read_png_size(path)
            x, y, cw, ch = common.resolve_crop(crop, fw, fh)
            img = lib.Canvas.open(path, max_rows=min(fh, y + ch))      # rows below the crop are never decoded
        except (ValueError, OSError, AssertionError) as e:
            sys.exit("error: cannot read frame %s: %s" % (path, e))
        if size is None:
            size = (fw, fh)
        elif (fw, fh) != size:
            sys.exit("error: frame %s is %dx%d but the first one is %dx%d" % (name, fw, fh, size[0], size[1]))
        pic = common.cut_el(img, dict(spec, crop=[x, y, cw, ch]))
        if cap:
            full = Canvas(w, h + bar_h)
            full.blit(pic, 0, 0)
            full.blit(bar(w, bar_h, caption_for(spec, n), cap.get("scale", 2), cap.get("color", "text")), 0, h)
            pic = full
        frames.append(pic.rgb_bytes())
        if log and len(frames) % 20 == 0:
            log("      %d / %d frames" % (len(frames), len(chosen)))
    delay = max(2, int(round(100.0 * step / fps)))
    return imgio.encode_gif_frames(w, h + bar_h, frames, [delay] * len(frames), n_colors=255, loop=0)


def build_all(manifest, captures, report, only=None, log=None):
    out = {}
    for spec in manifest.get("gifs", []):
        if only and only not in spec["out"]:
            continue
        if log:
            log("gif   " + spec["out"])
        out[spec["out"]] = build_gif(spec, captures, report, log)
    return out


def main(argv=None):
    ap = argparse.ArgumentParser(description=__doc__.split("\n\n")[0])
    ap.add_argument("--manifest", default=common.DEFAULT_MANIFEST)
    ap.add_argument("--captures", default=common.DEFAULT_CAPTURES)
    ap.add_argument("--out", default=common.DEFAULT_OUT)
    ap.add_argument("--only")
    ap.add_argument("--strict", action="store_true", help="exit 1 if the frames are missing")
    args = ap.parse_args(argv)
    report = common.Report()
    manifest = common.load_manifest(args.manifest)
    for name, data in build_all(manifest, common.Captures(args.captures), report, args.only,
                                lambda m: print(m, flush=True)).items():
        print("wrote %s (%d bytes)" % (common.write(args.out, name, data), len(data)))
    return 1 if report and args.strict else 0


if __name__ == "__main__":
    sys.exit(main())

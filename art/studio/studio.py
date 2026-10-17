#!/usr/bin/env python3
"""Glintsmith studio: clean transparent renders, studio pictures and looping GIFs from the in-game studio's frame pairs.

    py -B art/studio/studio.py check [SCENE ...]                 list the scenes under art/captures/studio and validate them
    py -B art/studio/studio.py still SCENE FRAME --out FILE.png  one picture; FRAME is a still name, a frame index or a file stem
                              [--backdrop | --transparent] [--size WxH] [--caption] [--no-shadow]
    py -B art/studio/studio.py gif SCENE --out FILE.gif          the turntable as a looping GIF
                              [--size WxH] [--caption | --no-caption] [--budget-mb 8] [--fps N] [--workers N]

Input is the frame contract (see scene.py): art/captures/studio/<scene>/scene.json and NNNN_k.png / NNNN_w.png pairs, the
same frame rendered over black and over white at S times the output size.  The pairs are turned into clean straight-alpha
renders (matte.py), set on a cream radial backdrop with a soft contact shadow and an optional drop shadow (composite.py)
and, for the GIF, encoded with one global palette, ordered dither on the subject only and delta frames (gifenc.py).  The
GIF steps down (fps 30 -> 24 -> 20 -> 15, then scale 1 -> 0.85 -> 0.75 -> 0.6) until it fits the budget and says which step it
chose (pipeline.py).  Options shared by all commands: --captures DIR (default art/captures), --style FILE.json (overrides
of composite.DEFAULT_STYLE), --strict.

If the scene or some of its frames are not there, the command says what is missing, writes nothing and exits 0 (1 with
--strict): nothing is ever faked.  A scene that is there but broken (wrong size, bad scene.json) exits 1.

Standard library only; byte-identical output from run to run (for any number of workers).
"""
import argparse
import json
import os
import sys
import time

sys.dont_write_bytecode = True
HERE = os.path.dirname(os.path.abspath(__file__))
ART = os.path.dirname(HERE)
for p in (HERE, ART):
    if p not in sys.path:
        sys.path.insert(0, p)

import composite   # noqa: E402
import pipeline    # noqa: E402
import scene as scn   # noqa: E402

DEFAULT_CAPTURES = os.path.join(ART, "captures")


def _size(text):
    try:
        w, h = (int(v) for v in text.lower().split("x"))
        if w < 2 or h < 2:
            raise ValueError
        return w, h
    except ValueError:
        raise argparse.ArgumentTypeError("size must look like 512x512")


def _style(path):
    if not path:
        return composite.style()
    with open(path, encoding="utf-8") as f:
        return composite.style(json.load(f))


def _log(msg):
    print(msg, flush=True)


def load_checked(args, scene_id):
    """The scene, or None after printing what is missing (the caller then exits 0, or 1 with --strict).
    A broken scene ends the run with exit 1."""
    try:
        sc = scn.load(args.captures, scene_id)
    except scn.SceneError as e:
        if e.kind == "missing":
            print("missing: %s" % e)
            print("nothing written.  Run the in-game studio first (art/captures/studio/%s/)." % scene_id)
            return None
        sys.exit("error: %s" % e)
    problems = scn.validate(sc)
    missing = [m for k, m in problems if k == "missing"]
    errors = [m for k, m in problems if k == "error"]
    if errors:
        sys.exit("error: scene %s does not follow the frame contract:\n  " % scene_id + "\n  ".join(errors[:12]))
    if missing:
        print("missing: scene %s is incomplete, %d file(s) not there yet:" % (scene_id, len(missing)))
        for m in missing[:12]:
            print("  " + m)
        print("nothing written.")
        return None
    return sc


def cmd_check(args):
    """List and validate scenes.  Exit 1 for a broken scene; missing input only with --strict."""
    ids = args.scenes or scn.scene_ids(args.captures)
    if not ids:
        print("no studio scenes found under %s" % scn.studio_root(args.captures))
        return 1 if args.strict else 0
    errors = missing = 0
    for sid in ids:
        try:
            sc = scn.load(args.captures, sid)
        except scn.SceneError as e:
            print("%s: %s %s" % (sid, "MISSING" if e.kind == "missing" else "ERROR", e))
            errors += e.kind != "missing"
            missing += e.kind == "missing"
            continue
        problems = scn.validate(sc)
        stills = [f["name"] for f in sc.frames if f.get("name")]
        head = "%s: %d frames, %dx%d at supersample %d, %g fps, %d looks, %d named stills (%s)" % (
            sid, len(sc.frames), sc.width, sc.height, sc.supersample, sc.fps, len(sc.data.get("looks", [])), len(stills),
            ", ".join(stills[:6]))
        if problems:
            print("%s  -> %d PROBLEM(S)" % (head, len(problems)))
            for kind, msg in problems[:20]:
                print("    %s: %s" % (kind, msg))
            if len(problems) > 20:
                print("    ... and %d more" % (len(problems) - 20))
            errors += any(k == "error" for k, _ in problems)
            missing += all(k == "missing" for k, _ in problems)
        else:
            print("%s  -> ok" % head)
    return 1 if errors or (missing and args.strict) else 0


def cmd_still(args):
    sc = load_checked(args, args.scene)
    if sc is None:
        return 1 if args.strict else 0
    try:
        frame = scn.find_frame(sc, args.frame)
    except scn.SceneError as e:
        sys.exit("error: %s" % e)
    st = _style(args.style)
    canvas = pipeline.still(sc, frame, args.size, backdrop=not args.transparent, caption=args.caption,
                            shadows=not args.no_shadow, st=st)
    os.makedirs(os.path.dirname(os.path.abspath(args.out)), exist_ok=True)
    data = canvas.to_png()
    with open(args.out, "wb") as f:
        f.write(data)
    print("wrote %s (%dx%d, %d bytes)" % (args.out, canvas.w, canvas.h, len(data)))
    return 0


def cmd_gif(args):
    sc = load_checked(args, args.scene)
    if sc is None:
        return 1 if args.strict else 0
    t = time.time()
    data, info = pipeline.build_gif(sc, args.size, args.caption, args.budget_mb, args.fps, args.workers,
                                    _style(args.style), args.dither, _log)
    os.makedirs(os.path.dirname(os.path.abspath(args.out)), exist_ok=True)
    with open(args.out, "wb") as f:
        f.write(data)
    print("wrote %s: %dx%d, %d frames (of %d), %g fps, scale %.2f, loop %.2f s, %.2f MB (budget %.1f MB), %d colours, %.0f s" % (
        args.out, info["width"], info["height"], info["frames"], info["source_frames"], info["fps"], info["scale"],
        info["loop_cs"] / 100.0, info["bytes"] / pipeline.MB, args.budget_mb, info["palette"], time.time() - t))
    if len(info["tried"]) > 1:
        print("chosen setting after stepping down: " + " | ".join(info["tried"]))
    return 0


def main(argv=None):
    ap = argparse.ArgumentParser(description=__doc__.split("\n\n")[0], formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--captures", default=DEFAULT_CAPTURES, help="folder that holds studio/<scene>/ (default art/captures)")
    ap.add_argument("--style", help="JSON file with overrides of composite.DEFAULT_STYLE")
    ap.add_argument("--strict", action="store_true", help="exit 1 when the input is missing")
    # the same three options may also follow the command (a parent parser whose defaults do not override the first ones)
    common = argparse.ArgumentParser(add_help=False)
    common.add_argument("--captures", default=argparse.SUPPRESS, help=argparse.SUPPRESS)
    common.add_argument("--style", default=argparse.SUPPRESS, help=argparse.SUPPRESS)
    common.add_argument("--strict", action="store_true", default=argparse.SUPPRESS, help=argparse.SUPPRESS)
    sub = ap.add_subparsers(dest="cmd", required=True)

    c = sub.add_parser("check", help="list and validate the studio scenes", parents=[common])
    c.add_argument("scenes", nargs="*", help="scene ids (default: every folder under studio/)")
    c.set_defaults(fn=cmd_check)

    s = sub.add_parser("still", help="write one composited (or transparent) PNG", parents=[common])
    s.add_argument("scene")
    s.add_argument("frame", help="still name, frame index (1 based) or file stem")
    s.add_argument("--out", required=True)
    g = s.add_mutually_exclusive_group()
    g.add_argument("--backdrop", action="store_true", help="on the studio backdrop with shadows (default)")
    g.add_argument("--transparent", action="store_true", help="the transparent RGBA render, no backdrop")
    s.add_argument("--size", type=_size, help="WxH: fit the render inside this size")
    s.add_argument("--caption", action="store_true", help="add the caption bar of the frame (backdrop only)")
    s.add_argument("--no-shadow", action="store_true", help="no contact and drop shadow")
    s.set_defaults(fn=cmd_still)

    q = sub.add_parser("gif", help="the turntable as a looping GIF", parents=[common])
    q.add_argument("scene")
    q.add_argument("--out", required=True)
    q.add_argument("--size", type=_size, help="WxH of the picture (default: the scene's); the caption bar is extra")
    q.add_argument("--caption", dest="caption", action="store_true", default=True)
    q.add_argument("--no-caption", dest="caption", action="store_false")
    q.add_argument("--budget-mb", type=float, default=8.0)
    q.add_argument("--fps", type=float, help="start at this frame rate (default the scene's, at most 50)")
    q.add_argument("--workers", type=int, default=min(8, os.cpu_count() or 1), help="worker processes (default up to 8)")
    q.add_argument("--dither", type=int, default=10, help="Bayer dither amplitude in levels (0 = off)")
    q.set_defaults(fn=cmd_gif)

    args = ap.parse_args(argv)
    return args.fn(args)


if __name__ == "__main__":
    sys.exit(main())

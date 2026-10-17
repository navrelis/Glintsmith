"""Builds docs/curseforge/preview.html from description.md and checks the marker lines.

    py -B docs/curseforge/preview.py             write preview.html and description_paste.md
    py -B docs/curseforge/preview.py --check     only check; exit 1 on a problem (also if preview.html is out of date)

preview.html shows the description with the images inline (img/<file>), for a browser, offline.
description_paste.md is description.md without the "VERIFY IN GAME" comments; it still has the image markers.
Python 3 standard library only.
"""
import html
import re
import sys

sys.dont_write_bytecode = True
from pathlib import Path

HERE = Path(__file__).resolve().parent
SRC = HERE / "description.md"
OUT = HERE / "preview.html"
PASTE = HERE / "description_paste.md"
IMG = HERE / "img"

MARKER = re.compile(r"^>>> PASTE IMAGE: (?P<file>[A-Za-z0-9_.-]+) \| (?P<alt>.+?) <<<$")
COMMENT = re.compile(r"[ \t]*<!--.*?-->", re.S)
SUMMARY_MAX = 250

CSS = """
:root { color-scheme: dark; }
body { margin: 0; padding: 0 16px 48px; background: #14101c; color: #e2dcec;
  font: 16px/1.6 system-ui, "Segoe UI", Roboto, Helvetica, Arial, sans-serif; }
.note { max-width: 1000px; margin: 0 auto; padding: 10px 0; color: #9a90ad; font-size: 13px;
  border-bottom: 1px solid #2f2740; }
main { max-width: 1000px; margin: 0 auto; }
h1, h2 { color: #f5f1fb; line-height: 1.25; }
h1 { margin: 28px 0 16px; font-size: 32px; }
h2 { margin: 36px 0 12px; padding-bottom: 6px; font-size: 24px; border-bottom: 1px solid #2f2740; }
p, ul, ol { margin: 0 0 14px; }
li { margin: 3px 0; }
strong { color: #ffffff; }
a { color: #c9a0ff; }
code { padding: 1px 5px; border-radius: 3px; background: #231c33; color: #f0d68a;
  font: 14px/1.4 Consolas, "Courier New", monospace; }
img { display: block; max-width: 100%; height: auto; margin: 18px 0; }
.scroll { overflow-x: auto; margin: 0 0 14px; }
table { border-collapse: collapse; font-size: 14px; }
th, td { border: 1px solid #2f2740; padding: 6px 9px; text-align: left; vertical-align: top; }
th { background: #1d1729; color: #f5f1fb; }
"""


def inline(text):
    parts = re.split(r"(`[^`]+`)", text)
    out = []
    for part in parts:
        if part.startswith("`") and part.endswith("`") and len(part) > 1:
            out.append("<code>" + html.escape(part[1:-1]) + "</code>")
            continue
        part = html.escape(part, quote=False)
        part = re.sub(r"\*\*(.+?)\*\*", r"<strong>\1</strong>", part)
        part = re.sub(r"\[([^\]]+)\]\(([^)]+)\)", r'<a href="\2">\1</a>', part)
        part = re.sub(r"(?<![\">=])(https?://[^\s<]+)", r'<a href="\1">\1</a>', part)
        out.append(part)
    return "".join(out)


def split_row(line):
    cells = line.strip().strip("|").split("|")
    return [c.strip() for c in cells]


def render(lines):
    out = []
    i = 0
    n = len(lines)
    while i < n:
        line = lines[i]
        if not line.strip():
            i += 1
            continue
        m = MARKER.match(line)
        if m:
            out.append('<img src="img/%s" alt="%s">' % (m["file"], html.escape(m["alt"])))
            i += 1
            continue
        if line.startswith("# "):
            out.append("<h1>%s</h1>" % inline(line[2:]))
            i += 1
            continue
        if line.startswith("## "):
            out.append("<h2>%s</h2>" % inline(line[3:]))
            i += 1
            continue
        if line.startswith("|"):
            rows = []
            while i < n and lines[i].startswith("|"):
                rows.append(lines[i])
                i += 1
            head = split_row(rows[0])
            body = [split_row(r) for r in rows[2:]]
            t = ['<div class="scroll"><table><thead><tr>']
            t += ["<th>%s</th>" % inline(c) for c in head]
            t.append("</tr></thead><tbody>")
            for r in body:
                t.append("<tr>" + "".join("<td>%s</td>" % inline(c) for c in r) + "</tr>")
            t.append("</tbody></table></div>")
            out.append("".join(t))
            continue
        if re.match(r"^- ", line):
            items = []
            while i < n and re.match(r"^- ", lines[i]):
                items.append("<li>%s</li>" % inline(lines[i][2:]))
                i += 1
            out.append("<ul>" + "".join(items) + "</ul>")
            continue
        if re.match(r"^\d+\. ", line):
            items = []
            while i < n and re.match(r"^\d+\. ", lines[i]):
                items.append("<li>%s</li>" % inline(re.sub(r"^\d+\. ", "", lines[i])))
                i += 1
            out.append("<ol>" + "".join(items) + "</ol>")
            continue
        para = []
        while i < n and lines[i].strip() and not MARKER.match(lines[i]) and not lines[i].startswith(("#", "|", "- ")):
            para.append(lines[i])
            i += 1
        out.append("<p>%s</p>" % inline(" ".join(para)))
    return "\n".join(out)


def build():
    raw = SRC.read_text(encoding="utf-8")
    stripped = COMMENT.sub("", raw)
    stripped = re.sub(r"\n{3,}", "\n\n", stripped)
    body = render(stripped.splitlines())
    page = (
        '<!doctype html>\n<html lang="en">\n<head>\n<meta charset="utf-8">\n'
        '<meta name="viewport" content="width=device-width, initial-scale=1">\n'
        "<title>Glintsmith - description preview</title>\n<style>%s</style>\n</head>\n<body>\n"
        '<div class="note">Preview of docs/curseforge/description.md - not the live page</div>\n<main>\n%s\n</main>\n</body>\n</html>\n'
        % (CSS, body)
    )
    return page, stripped


def check(raw):
    problems = []
    seen = []
    for no, line in enumerate(raw.splitlines(), 1):
        if "PASTE IMAGE" in line:
            m = MARKER.match(line)
            if not m:
                problems.append("line %d: marker is not in the exact form: %s" % (no, line))
                continue
            seen.append(m["file"])
    for f in seen:
        if not (IMG / f).is_file():
            problems.append("img/%s does not exist" % f)
    dup = sorted({f for f in seen if seen.count(f) > 1})
    for f in dup:
        problems.append("img/%s is used by more than one marker" % f)
    summary = HERE / "summary.txt"
    if summary.is_file():
        text = summary.read_text(encoding="utf-8")
        if "\n" in text.strip() or len(text.strip()) > SUMMARY_MAX:
            problems.append("summary.txt must be one line of at most %d characters (is %d)" % (SUMMARY_MAX, len(text.strip())))
    return problems


def main(argv):
    raw = SRC.read_text(encoding="utf-8")
    page, paste = build()
    problems = check(raw)
    if "--check" in argv:
        if not OUT.is_file() or OUT.read_text(encoding="utf-8") != page:
            problems.append("preview.html is out of date; run this script without --check")
        if not PASTE.is_file() or PASTE.read_text(encoding="utf-8") != paste:
            problems.append("description_paste.md is out of date; run this script without --check")
    else:
        OUT.write_text(page, encoding="utf-8", newline="\n")
        PASTE.write_text(paste, encoding="utf-8", newline="\n")
        print("wrote", OUT.name, "and", PASTE.name)
    for p in problems:
        print("PROBLEM:", p)
    print("markers ok" if not problems else "%d problem(s)" % len(problems))
    return 1 if problems else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))

# Glintsmith art

Everything that makes the CurseForge page images and the mod icon.

**No pixel art, no drawn art, anywhere.** Every picture is a real in-game render: a screenshot of Minecraft with Glintsmith
running. The only things set by code are layout: the card frame and panels, and text in the game's own font.

**Light theme.** The page is cream and white: cards are cream-white panels with dark ink text, a violet accent and a thin darker
edge round every capture, and the in-game captures are shot on a **white backdrop** (see `gallery/SHOTLIST.md`). The edge (2 px, dark
brass then soft tan, `lib.BORDER_DARK` / `lib.RIM`) keeps a white picture from melting into the cream card.

**The in-game backdrop.** The rooms are built of white blocks (a `snow_block` studio for the hero shots, smooth quartz
with a birch floor for the item and stand rooms), every air cell holds a level 15 light block, and the game runs at
gamma 1.0 with Night Vision, so nothing is dim and nothing is tinted. A block wall still tops out at about 200 of 255
under Minecraft's face shade, so every close-up (FOV 30) stands in front of a full-bright white / cream panel: a
`text_display` with a white background and light 15 / 15, which is drawn without any shade. The outline and the glint
are rendered by the real mod on top of it. The wide FOV 70 shots (Sodium and Iris, targets, screens) have no panel; the universal wall is an
FOV 30 close-up and has one. Details, coordinates and expected subject sizes are in `gallery/SHOTLIST.md`. Every colour comes from the
palette in `lib.py`; nothing in the builders is hard-coded (see "Theme" below).

* **The gallery** (`docs/curseforge/img/banner.png`, `card_*.png`, `showcase.gif`) is composed from in-game captures: cropped,
  scaled (by whole factors, or with the area filter) and set into framed cards. A picture slot whose capture is missing shows a
  grey "capture missing" placeholder.
* **The logo, the mod icon and `logo_animated.gif`** are cut from captures too: the logo and the icon from `logo_sword.png`
  (an enchanted sword with a custom glint and outline on a white backdrop), the animated logo from the hero shots of three
  glint presets. The banner is the `hero_sword_rainbow` render plus a strip from `hero_lineup`.

Python 3 standard library only (`py -B ...` on this machine; `python` is not on the PATH). PNG is written with `zlib` + `struct`,
GIF with our own LZW encoder and median-cut palette (`imgio.py`). Output is byte-identical from run to run (no timestamps, fixed seeds).

## Workflow

```
1. capture      follow art/gallery/SHOTLIST.md (the hero shots first); save the PNGs as art/captures/<name>.png
                (and art/captures/showcase/0001.png ...). art/captures/ is git-ignored: real captures are never committed
2. look         py -B art/gallery/build.py --list      which captures exist, which are missing
3. build        py -B art/gallery/build.py             writes docs/curseforge/img/*.png and showcase.gif
                py -B art/gallery/build.py --strict    the same, but exit 1 if any capture is missing (use before a release)
                py -B art/logo.py                      the logo, the mod icon and logo_animated.gif from the hero shots
4. adjust       open the cards; move a crop in art/gallery/cards.json where the subject is off (the logo crop is the
                "logo" section of the same file); build again
5. verify       py -B art/verify.py
```

Other commands:

```
py -B art/gallery/build.py --only card_gap_fix            only the images whose name contains the text
py -B art/gallery/build.py --captures DIR --out DIR       read the captures from / write the images to other folders
py -B art/gallery/gif.py                                  only the showcase GIF (build.py calls it too; --no-gif skips it)
py -B art/gallery/synth.py DIR                            synthetic stand-ins for every capture, to test the layout (see below)
py -B art/logo.py [--captures DIR] [--out DIR] [--strict] [--no-gif]
                                                          the logo, the mod icon and logo_animated.gif
```

Always run Python with `-B` so no `__pycache__` is left in `art/` (every script also sets `sys.dont_write_bytecode`).

### The logo, the icon and the animated logo

`py -B art/logo.py` reads `art/captures/logo_sword.png` (1920 x 1080, the sword centred, see the shot list) and cuts a square from
it: by default the centred square of the full height (1080 x 1080). The square can be moved or tightened in the `logo` section of
`gallery/cards.json` (`crop.centre` [x, y] in capture pixels, `"c"` for the middle; `crop.size` in pixels or `"full"`); a square that
would leave the picture is moved inside it. The square is resized with the **area filter** (`Canvas.area`: every output pixel is the
exact mean of the source area it covers, partly covered source pixels by their fraction; for whole factors it equals `down`) to

* `art/logo/logo_512.png` (512 x 512), the logo,
* `src/main/resources/assets/glintsmith/icon.png` (128 x 128), the mod icon,
* `art/logo/logo_32.png` (32 x 32).

`docs/curseforge/img/logo_animated.gif` (128 x 128) is built from `hero_sword_rainbow.png`, `hero_sword_void.png` and
`hero_sword_fire.png`: the same square crop of each, every look held 14 frames, then 6 frames of cross-fade to the next, the last
back into the first, 5 cs per frame, 60 frames. The numbers are in the `logo.gif` entry.

If a capture is missing the script says which one, writes nothing for it and exits 0 (1 with `--strict`); the committed `icon.png`
is never touched by that. A GIF with any of its three hero shots missing is skipped the same way.

### Missing captures are never faked

`build.py` draws a clearly marked grey slab, "capture missing: <file name>", where a capture is not there, prints one warning per
file and carries on; with `--strict` it exits 1 afterwards. `gif.py` does the same with a one-frame grey GIF when
`art/captures/showcase/` has no frames. `logo.py` writes nothing (see above). The repository ships in that state until the
captures exist; `build.py --list` shows every capture, including the hero shots and the logo capture, and which ones are missing.

### Testing the layout without captures

`py -B art/gallery/synth.py DIR` writes a 1920 x 1080 stand-in for every capture the manifest names (a gradient with a coordinate
grid, a diagonal bar, the file name and the words "SYNTHETIC TEST CAPTURE"; the manifest names the hero shots and the logo capture
too) and 40 numbered GIF frames (a short stand-in clip, not the 112 real frames). Build from it with
`build.py --captures DIR --out DIR2` and `logo.py --captures DIR --out DIR3` and
look at `DIR2` / `DIR3` to check crops, scales, labels and spacing. The grid labels show which source
pixels a crop picks. These files are only ever written to a folder you name (use a temp folder); they carry the text in a PNG
chunk and `verify.py` fails if one ends up in `art/captures/`.

## Theme

The palette lives in `lib.py` (section 2); `cards.json` only names colours (`text`, `dim`, `accent`, `gold`, `good`, `bad`) or stays
with the defaults. Contrast is the WCAG ratio (`lib.contrast`); every text pair is at least 4.5:1, none relies on the 3:1 allowance
for large text. `py -B art/verify.py` checks this on every build (see "Verify").

| Role | Name | Hex |
| --- | --- | --- |
| Outer page, GIF caption bar | `PAGE` | `#F6F1E6` |
| Card panel | `PANEL` | `#FBF8F1` |
| Chips, raised strips | `PANEL_MID` | `#EDE5D2` |
| Strips, bays, picture backing | `PANEL_DARK` | `#E1D9C6` |
| Soft shadow, picture edge (inner) | `RIM` | `#CBBFA5` |
| Frame, picture edge (outer), borders | `BORDER` soft gold / `BORDER_DARK` dark brass | `#D8AD38` / `#754B1D` |
| Body text | `TEXT` (= `INK`) | `#2A2433` |
| Secondary text | `TEXT_DIM` | `#5E5448` |
| Card titles / gold labels | `TITLE` / `GOLD_TEXT` | `#5A3A12` / `#7A5210` |
| Accent (chips, bullets, rules) | `ACCENT` | `#7A3FD6` |
| After / before labels | `GOOD` / `BAD` | `#14744A` / `#B0303C` |

| Text on background | Ratio |
| --- | --- |
| `TEXT` on `PANEL` / on the darkest dither speck / on `PANEL_DARK` / on `PANEL_MID` / on `PAGE` | 14.2 / 12.5 / 10.7 / 12.0 / 13.3 |
| `TEXT_DIM` on `PANEL` / on the darkest dither speck / on `PANEL_DARK` | 7.0 / 6.2 / 5.3 |
| `INK` on `BORDER` (brass chip) | 7.1 |
| `GOOD` / `BAD` / `GOLD_TEXT` / `ACCENT` on `PANEL` | 5.5 / 5.9 / 6.5 / 5.6 |
| `TITLE` on `PANEL` (large heading) | 9.7 |

The in-game text inside a capture is the game's own and is not part of this table.

## The gallery pipeline

| File | What it does |
| --- | --- |
| `gallery/cards.json` | Manifest of every gallery image: output name, size, title, and a list of elements (captures with crop / resampling / position / label, text, bullets, chips, panels, ...), the `logo` section (logo capture, square crop, animated logo) and the GIF entry. |
| `gallery/build.py` | Reads the manifest and the captures, writes the PNGs. |
| `gallery/gif.py` | Builds `showcase.gif` from `art/captures/showcase/NNNN.png`: crop, optional whole-number scale, every n-th frame, caption bar in the game font, one global palette with delta frames, loops forever, below 8 MB (raises the step by itself if needed). |
| `gallery/synth.py` | Synthetic test captures (see above): light, white / cream-ish backdrops like the real ones. |
| `gallery/common.py` | Manifest and capture loading, crop / scale / area-resample helpers, the missing-capture report. |
| `gallery/SHOTLIST.md` | Every capture: rig, world, scene, camera, config values, file name, including the seven hero shots. |
| `lib.py` | RGBA canvas (fill, frame, crop, nearest-neighbour scale, box and area downscale, alpha blit, PNG in and out), the vanilla font, word wrap, the card frame, chips, labels, bays, placeholder. |
| `imgio.py` | PNG writer and reader (8-bit RGB / RGBA of every filter type, which is what the game writes; palette and grey for the vanilla font), GIF89a writer with LZW, median-cut quantiser with delta frames, and a small GIF decoder used by `verify.py`. |
| `logo.py` | Logo, icon and animated logo from the captures (square crop, area filter, cross-fade GIF). |
| `verify.py` | The checks below. |

### The manifest

`cards.json` has `cards` (one entry per image: `out`, `size` [w, h], `seed`, `title`, `elements`) and `gifs`. Positions are card pixels.
Element types:

| `type` | Keys |
| --- | --- |
| `capture` | `capture` (file in `art/captures`), `crop` [x, y, w, h] in source pixels (x or y may be `"c"` to centre), optional `down` (box downscale by a whole factor, applied first), `scale` (whole-number upscale), or instead `resample: "area"` with `size` [w, h] (the crop is resized to exactly that size with the area filter; use it for hero shots that are scaled down by a non-whole factor), `at` [x, y] (top left of the picture), `frame` (default true: a 3 px brass rim over the picture edge), `label`, `label_style` (accent, good, bad, dim, gold), `label_at` (tl, tr, bl, br) |
| `label` | `at`, `text`, `style` |
| `text` | `at`, `width`, `text`, `size` (whole scale, default 2), `color` (text, dim, accent, gold, good, bad or `#rrggbb`), `align` (left: `at.x` is the left edge; centre: the middle; right: the right edge), `leading` |
| `bullets` | `at`, `width`, `items`, `size`, `gap` |
| `chips` | `at`, `align`, `items` (strings or `{text, style}`; style accent or brass), `gap` |
| `rule`, `panel`, `heading`, `strip` | see `build.py` (layout only; there are no drawn decorations) |

Picture size on the card is `crop size / down * scale`, or `size` with `resample: "area"` (the crop must have the same shape as `size`), so pictures stay on whole pixels. For captures taken at **GUI scale 2**,
`down: 2` is lossless: one GUI pixel becomes one card pixel and the game's text stays crisp. The card text uses only ASCII, set in the game's own
font read from the Minecraft 1.21.1 client jar at build time.

The `gifs` entry takes `frames_dir`, `crop`, `down`, `scale`, `step` (keep every n-th frame), `source_fps`, `max_step`, `max_mb`, the
`caption_bar` (`height`, `scale`, `color`) and the `captions` (one per look, each lasting `frames_per_caption` source frames).
The showcase clip is 7 looks x 16 frames = 112 frames, 0.25 s apart: `frames_per_caption` 16, `source_fps` 4, `step` 1 (every
frame, 25 cs each), `crop` `["c","c",1440,810]` with `down` 2, so the 1920 x 1080 capture becomes a 720 x 405 picture under a
36 px caption bar. The in-game script shoots a burst of 50 screenshots per look and keeps the 16 frames that lie 0.25 s apart.

## Verify

`py -B art/verify.py` checks that

* the PNG reader decodes every filter type, RGB, RGBA and a 1-bit palette image exactly, and rejects garbage;
* the manifest only uses known elements, every picture box lies inside its card, and every capture is described in `SHOTLIST.md`;
* every text colour the build puts on a cream surface has a WCAG contrast of at least **4.5:1** (`MIN_CONTRAST`; the 3:1
  allowance for large text is not used). It checks the pairs of the table above (`TEXT`, `TEXT_DIM`, `GOOD`, `BAD`,
  `GOLD_TEXT`, `ACCENT`, `TITLE` on `PANEL` and on the darkest dither speck of a real card, `INK` on `BORDER`, the
  neutral colours also on `PANEL_MID` and `PANEL_DARK`), `INK` on the grey-beige "capture missing" slab, the text colour
  of every label style on the label box, every named text colour of `lib.COLOURS` on the card, the colour of every
  `text` and `bullets` element of `cards.json` on the card, the dither speck and the panel it sits in, and the caption
  bar text of the GIF on `PAGE`. Text that is part of an in-game capture is the game's own and is not checked;
* the area filter averages exactly (a 4 x 4 image becomes the 2 x 2 block means, 3 -> 2 weights by covered fraction, a flat colour
  stays flat, transparent pixels do not darken, whole factors equal the box downscale), and the logo crop picks the centred square
  of the full height by default;
* the logo files, the cards and the GIFs are built twice, from the captures in the repository and from synthetic ones, and come out
  byte-identical both times, and the synthetic build draws no placeholder;
* from a synthetic capture: `logo_512.png` and `icon.png` are the area-filtered square crop byte for byte, 512 / 128 / 32 px; the
  animated logo is 128 x 128, 60 frames, constant delay, loops forever, below 1 MB, and cross-fades the last hero shot back into the first;
* `logo.py` without `logo_sword.png` prints the missing capture, writes nothing and exits 0 (1 with `--strict`), and leaves the committed
  `icon.png` untouched;
* every committed image is byte-identical to a fresh build (an image whose captures are not on this machine is reported as skipped, not
  failed; a warning is printed while `icon.png` is still the hand-drawn one of the first commit);
* sizes: cards and banner as in the manifest (at most 1000 px wide), the showcase GIF has the manifest size, loops forever and is below 8 MB;
* the manifest has no drawn elements (`logo`, `sparkle`, `arrow`), area-filter pictures are downscales of the same shape, and the banner
  is made of the hero shots;
* an impossible GIF size limit ends the build with an error instead of an oversized file;
* every script imports only the standard library and no `__pycache__` exists below `art/`; no synthetic capture is in `art/captures/`.

## Output files

| Path | Size | What |
| --- | --- | --- |
| `art/logo/logo_512.png` | 512 x 512 | the logo, from `logo_sword.png` (not in the repository until the capture exists) |
| `art/logo/logo_32.png` | 32 x 32 | the small logo |
| `src/main/resources/assets/glintsmith/icon.png` | 128 x 128 | the mod icon, from `logo_sword.png` |
| `docs/curseforge/img/logo_animated.gif` | 128 x 128, 60 frames | cross-fade of the rainbow, void and fire hero shots |
| `docs/curseforge/img/banner.png` | 1000 x 420 | the `hero_sword_rainbow` render, title, tagline, chips and the `hero_lineup` strip |
| `docs/curseforge/img/card_universal.png` | 1000 x 590 | vanilla and modded items, 3D shields and tridents, all outlined |
| `docs/curseforge/img/card_glint_presets.png` | 1000 x 590 | the glint presets, captured, with the vanilla glint as the before |
| `docs/curseforge/img/card_outline_styles.png` | 1000 x 590 | vanilla, static, rainbow, pulse, gradient, per enchantment, thickness, soft glow |
| `docs/curseforge/img/card_targets.png` | 1000 x 590 | hand and armor (hero shots), third person, dropped items, item frames, inventory |
| `docs/curseforge/img/card_gap_fix.png` | 1000 x 590 | before / after of the gap fix, vanilla and modded item |
| `docs/curseforge/img/card_rules.png` | 1000 x 590 | the rule list and the rule editor |
| `docs/curseforge/img/card_config.png` | 1000 x 590 | the settings screen, Outline and Glint tabs |
| `docs/curseforge/img/card_shaders.png` | 1000 x 590 | Sodium, and an Iris shader pack in the world, the hand and the inventory |
| `docs/curseforge/img/showcase.gif` | 720 x 441 | seven looks cycling on one armor stand, captioned (16 frames per look, 4 fps source) |

## Licence and sources

* **The logo and the icon** are screenshots of the running mod (an enchanted sword), cut and resized by `logo.py`; they are not drawn.
* **The gallery images and the animated logo** show Minecraft and the textures and models of the mods in the test pack as gameplay screenshots. They are
  screenshots of the running game, not redistributed assets, and nothing from the game is committed as a file other than those screenshots.
* **The vanilla font** is read from your own Minecraft 1.21.1 client jar every time the gallery is built (`GLINTSMITH_MC_JAR`, else
  the Loom cache under `.gradle/loom-cache/minecraftMaven/` or `~/.gradle/caches/fabric-loom/1.21.1/`). It is never extracted or committed.
* The card frame, the heading style and the text routines are adapted from the MIT licensed showcase scripts of Lootr Remastered
  (`art/showcase_lib.py`, same author).

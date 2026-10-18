# Glintsmith art

Everything that makes the CurseForge page images and the mod icon.

**No pixel art, no drawn art, anywhere.** Every picture is a real render of Minecraft with Glintsmith running: a screenshot of the
game, or a frame of the dev-only **studio** (`docs/STUDIO.md`), which renders items and armor through the real mod pipeline. The only
things set by code are layout (the card frame and panels, text in the game's own font), the studio matte (the exact transparency
recovered from a black / white pair of renders), the cream backdrop with its soft shadows, and the GIF encoding.

**Light theme.** The page is cream and white: cards are cream-white panels with dark ink text, a violet accent and a thin darker
edge round every in-game capture (2 px, dark brass then soft tan, `lib.BORDER_DARK` / `lib.RIM`; see `gallery/SHOTLIST.md`). The
studio renders have no frame: the item stands straight on the card, or on the studio backdrop of a GIF, with a soft contact shadow.

* **The studio** (`art/studio/`, scenes in `art/captures/studio/<scene>/`, git-ignored) makes: the banner picture, the logo and the
  icon, the four GIFs, and the universal, glint preset, outline style and gap fix cards.
* **In-world captures** (`art/captures/<name>.png`, git-ignored, 14 of them, `gallery/SHOTLIST.md`) make the targets, rules, config
  and shaders cards. They are shot on a white backdrop: the rooms are built of white blocks, every air cell holds a level 15 light
  block, the game runs at gamma 1.0 with Night Vision, and every close-up stands in front of a full-bright white panel (a
  `text_display`, drawn without face shade). Details, coordinates and expected subject sizes are in `gallery/SHOTLIST.md`.

Every colour comes from the palette in `lib.py`; nothing in the builders is hard-coded (see "Theme" below).

Python 3 standard library only (`py -B ...` on this machine; `python` is not on the PATH). PNG is written with `zlib` + `struct`,
GIF with our own LZW encoder and median-cut palette (`imgio.py`, `studio/gifenc.py`). Output is byte-identical from run to run (no
timestamps, fixed seeds, any number of workers).

## Workflow

```
1. render       ./gradlew runStudio -Pscene=<scene>        for the 7 scenes of the contract (below): art/captures/studio/<scene>/
   capture      the 14 in-world shots of art/gallery/SHOTLIST.md, saved as art/captures/<name>.png
2. look         py -B art/studio/studio.py check           every studio scene: frames, sizes, named stills
                py -B art/gallery/build.py --list          which captures and studio frames exist, which are missing
3. build        py -B art/logo.py                          logo_512, logo_32, the mod icon 128, logo_animated.gif (logo_concepts, logo_anim_a)
                py -B art/gallery/build.py --strict        cards, banner and the four GIFs into docs/curseforge/img/
                                                           (the GIFs take minutes; --no-gif skips them; gifs.py builds only them)
4. adjust       open the images; move a crop / box in art/gallery/cards.json where the subject is off; build again
5. verify       py -B art/verify.py --full                 (without --full the real GIFs are not rebuilt; about 30 minutes with --full)
                py -B docs/curseforge/preview.py           the page preview, then --check must report 0 problems
```

Other commands:

```
py -B art/gallery/build.py --only card_gap_fix            only the images whose name contains the text
py -B art/gallery/build.py --captures DIR --out DIR       read captures and studio scenes from / write the images to other folders
py -B art/gallery/build.py --no-gif                       cards only
py -B art/gallery/gifs.py [--only lineup] [--workers N]   only the GIFs (same --captures / --out / --strict)
py -B art/logo.py [--captures DIR] [--out DIR] [--strict] [--workers N] [--no-gif]  the logo, the icon and the animated logo (--out DIR: see below)
py -B art/studio/studio.py still SCENE SHOT --out FILE.png     one studio picture (see docs/STUDIO.md and studio.py)
py -B art/studio/studio.py gif SCENE --out FILE.gif            one turntable as a GIF
py -B art/gallery/synth.py DIR                            synthetic stand-ins for every capture and studio scene (see below)
```

Always run Python with `-B` so no `__pycache__` is left in `art/` (every script also sets `sys.dont_write_bytecode`).

### The studio scenes (the contract)

The scene names, sizes and shots below are what the in-game studio has to produce and what `cards.json` refers to (`gallery/SHOTLIST.md`,
section 4, has the content of each). A **turntable** is the frames of every look in order (`name` null in `scene.json`); a **still scene**
has **one frame (one K / W pair) per shot** and the frame's `name` is the shot name, which `scene.find_frame` and `studio.py still`
look up (`stills_tiles` has 24 frames, `outline_soft_80` among them).

| Scene | Size | What |
| --- | --- | --- |
| `hero_sword` | 512 x 512 | turntable, 5 to 6 looks of one sword: `showcase.gif` (384 px + caption bar), at most 2,000,000 bytes |
| `lineup` | 1200 x 400 | turntable, six items in a row with a +-35 degree yaw swing: `lineup.gif` (810 x 270 + caption bar, at most 2,000,000 bytes) |
| `armor_stand` | 480 x 600 | turntable, 3 to 4 looks of a stand in netherite armor: `armor.gif` (320 x 400 + caption bar, at most 2,000,000 bytes) |
| `stills_hero` | 1024 x 1024 | `banner_hero` (the banner picture); `logo` (the old logo still, no longer used) |
| `logo_concepts` | 512 x 512 | `concept_a` (logo, icon, small logo: the golden blade); `concept_b`, `concept_c` (the two logo concepts not chosen, kept for reference) |
| `logo_anim_a` | 256 x 256, supersample 4 | fixed pose of `concept_a`, 165 frames at 20 fps, glint and outline colour in an exact 8.25 s loop: `logo_animated.gif` |
| `stills_wide` | 1200 x 400 | `universal_vanilla`, `universal_modded` (the two rows of `card_universal.png`) |
| `stills_tiles` | 512 x 512 | `glint_vanilla`, `glint_neutral`, `glint_rainbow`, `glint_aurora`, `glint_fire`, `glint_frost`, `glint_sparkle`, `glint_gold`, `glint_void`, `glint_inferno`, `glint_nebula`, `glint_starlight`, `glint_opal`, `glint_storm` (`card_glint_presets.png`) and `outline_vanilla`, `outline_static`, `outline_rainbow`, `outline_pulse`, `outline_gradient`, `outline_enchant`, `outline_thick_1`, `outline_thick_3`, `outline_thick_6`, `outline_soft_80` (`card_outline_styles.png`) |
| `stills_gap` | 800 x 800 | `gap_sword_off`, `gap_sword_on`, `gap_modded_off`, `gap_modded_on` (`card_gap_fix.png`) |

### The logo and the icon

The logo is "the golden blade": an enchanted golden sword on the diagonal (fire glint, violet outline), rendered by the studio.
`py -B art/logo.py` reads the still `logo_concepts` / `concept_a` (512 x 512), mattes it (`studio/matte.py`), cuts a square centred on
the subject (the sword fills `fill` = 86 % of it) and resizes it with the **area filter** (`Canvas.area`: every output pixel is the
exact mean of the source area it covers, partly covered source pixels by their fraction; never enlarged), then sets it on the cream
studio backdrop (`studio/composite.py`) with a soft **glow** made of the sword's own pixels: each pixel weighted by its alpha and by
how colourful it is (so the dark parts do not glow), blurred in two layers (a tight and a wide one), the colour a little more
saturated and lighter. Nothing is drawn. From that 512 picture (area filter):

* `art/logo/logo_512.png` (512 x 512), the logo,
* `src/main/resources/assets/glintsmith/icon.png` (128 x 128), the mod icon,
* `art/logo/logo_32.png` (32 x 32).

`docs/curseforge/img/logo_animated.gif` (256 x 256, no caption, at most 2,000,000 bytes, about 1.2 MB) is made by the same script from
the scene `logo_anim_a`: the same pose, the glint flowing (speed 10) and the outline colour going violet, magenta, blue and back. The
glint scroll and the outline colour both repeat after exactly 8.25 s (165 frames at 20 fps, 5 cs each), so the loop has no seam. Every
frame is framed with the square of the first frame; the glow is the mean of 12 frames and stays still, and its colours get their own
120 palette entries (gifenc `base_n`), so the glow has no bands.

The settings (scene, frame, fill, glow layers, GIF scene, size, dither, base_n) are `DEFAULTS` in `logo.py`; a `logo` section of
`gallery/cards.json` in that format (with a `glow` key) overrides them. If a render is missing the script says which file and writes
nothing for that output (the stills and the GIF are independent), exit 0 (1 with `--strict`); the committed files are never touched
by that. With `--out DIR` the script writes `DIR/art/logo/...`, `DIR/src/main/resources/...` and `DIR/docs/...` instead of the
repository, which is how a synthetic run is kept away from the real files (see below).

### The GIFs

Each entry of `gifs` in `cards.json` is `{out, scene, size [w, h], caption, budget_mb, fps, min_scale, max_scale, colours, dither, lossy, style}`: a studio turntable
fitted into `size`, with the caption bar of the frame's `caption` under the picture (36 px, `caption` true by default), set on the
cream backdrop with a contact and a drop shadow (`style` overrides `studio/composite.py` `DEFAULT_STYLE`; the lineup GIF has no
contact shadow, a row of floating items has no floor) and encoded by `studio/pipeline.py` with one global palette, ordered dither on
the subject only and delta frames. Every GIF of the page must stay at most **2,000,000 bytes**, so `budget_mb` is 1.9 (1.9 MiB =
1,992,294 bytes, under the limit however a "MB" is counted). The budget ladder steps the frame rate down (30, 24, 20, 15 fps) and
then the picture size down (`max_scale`, then 0.85, 0.75, 0.6, down to `min_scale`) until the file fits `budget_mb`, and the build
prints which step it chose; `min_scale` 1 forbids the size step (the build then stops with an error instead), `max_scale` (default
1) is the first scale tried, so `max_scale` = `min_scale` pins the picture size. The other levers trade quality for bytes:
`colours` (palette size, default 255), `dither` (ordered dither strength on the subject, default 10, 0 = off) and `lossy` (default
0 = exact; a tolerance, the weighted squared colour distance within which a pixel keeps the colour the previous frame showed, so
more of each delta frame is transparent; about 300 is not visible, 800 leaves a faint trail on fast glints). The checked-in
manifest pins them per GIF (see the table). The loop plays the whole turntable once, forever.

| Output | Scene | Picture | Frame rate | Palette, dither, lossy | Budget |
| --- | --- | --- | --- | --- | --- |
| `showcase.gif` | `hero_sword` | 384 x 384 + 36 px caption bar (scale 0.75 of 512) | 15 fps | 128, 0, 300 | 1.9 MB |
| `lineup.gif` | `lineup` | 810 x 270 + 36 px caption bar (scale 0.9 of 900) | 30 fps | 128, 0, 300 | 1.9 MB |
| `armor.gif` | `armor_stand` | 320 x 400 + 36 px caption bar (scale 0.8 of 400) | 12 fps | 64, 0, 800 | 1.9 MB |

`gallery/gifs.py` builds them; `build.py` calls it (`--no-gif` skips). `logo_animated.gif` is not in this list: `logo.py` makes it (see "The logo and the icon"). `legacy/gif.py` is the old screenshot-burst GIF builder, no longer
used.

### Missing input is never faked

`build.py` draws a clearly marked grey slab, "capture missing: <file name>" (or "studio/<scene>/<shot>"), where a capture or a studio
frame is not there, prints one warning per file and carries on; with `--strict` it exits 1 afterwards. `gifs.py` writes a one-frame
grey GIF in the same way. `logo.py` writes nothing (see above). The repository ships in that state until the renders exist;
`build.py --list` shows every capture and studio frame and which ones are missing.

### Testing the layout without renders

```
py -B art/gallery/synth.py DIR [--frames N] [--supersample S] [--workers N] [--no-studio]
py -B art/gallery/build.py --captures DIR --out DIR2            cards, banner and GIFs from the synthetic input
py -B art/logo.py --captures DIR --out DIR3                     logo, icon, animated logo (DIR3/art/logo, DIR3/src/main/resources/..., DIR3/docs/...)
py -B art/studio/synth_studio.py DIR --contract                 only the nine synthetic studio scenes
py -B art/studio/synth_studio.py DIR --size 256 | 480x600       one synthetic turntable of that size
```

`synth.py` writes a 1920 x 1080 stand-in for every in-world capture the manifest names (a gradient with a coordinate grid, a diagonal
bar, the file name and the words "SYNTHETIC TEST CAPTURE") and, unless `--no-studio`, the nine studio scenes of the contract in
`DIR/studio/` (a little extruded sword turning on a vertical axis, a row of six for the lineup, with a glow, as black / white frame pairs
in the contract's names and sizes: 12 frames per turntable by default, one frame per still shot). Look at `DIR2` / `DIR3` to check
crops, scales, labels and spacing. These files are only ever written to a folder you name (use a temp folder); they carry the text in a
PNG chunk and `verify.py` fails if one ends up in `art/captures/`. Both `synth.py` and `synth_studio.py` refuse `art/captures`.

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
| `gallery/cards.json` | Manifest of every gallery image: output name, size, title, and a list of elements (captures with crop / resampling / position / label, studio renders, text, bullets, chips, panels, ...), the `logo` section (studio still, framing, glow and the animated logo of `logo.py`) and the `gifs` list. |
| `gallery/build.py` | Reads the manifest, the captures and the studio scenes, writes the PNGs (and calls `gifs.py`). |
| `gallery/gifs.py` | Builds the GIFs of the manifest from studio turntables (see "The GIFs"). |
| `gallery/synth.py` | Synthetic test captures and studio scenes (see above): light, white / cream-ish backdrops like the real ones. |
| `gallery/common.py` | Manifest and capture loading, crop / scale / area-resample helpers, the missing-capture report, `studio_state` for `--list`. |
| `gallery/SHOTLIST.md` | Every in-world capture: rig, world, scene, camera, config values, file name; and the studio contract (scenes, sizes, shots). |
| `studio/` | The studio pipeline: `scene.py` (frame contract), `matte.py`, `composite.py` (backdrop, shadows, caption bar), `gifenc.py`, `pipeline.py` (matte, composite, palette, budget ladder), `studio.py` (CLI), `synth_studio.py` (synthetic scenes). |
| `lib.py` | RGBA canvas (fill, frame, crop, nearest-neighbour scale, box and area downscale, alpha blit, PNG in and out), the vanilla font, word wrap, the card frame, chips, labels, bays, placeholder. |
| `imgio.py` | PNG writer and reader (8-bit RGB / RGBA of every filter type, which is what the game writes; palette and grey for the vanilla font), GIF89a writer with LZW, median-cut quantiser with delta frames, and a small GIF decoder used by `verify.py`. |
| `logo.py` | Logo, icon and animated logo from the studio scenes `logo_concepts` / `concept_a` and `logo_anim_a` (framing, area filter, glow of the render's own pixels). |
| `legacy/gif.py` | The old screenshot-burst GIF builder (the `showcase` frames of the in-world workflow). Not used. |
| `verify.py` | The checks below. |

### The manifest

`cards.json` has `cards` (one entry per image: `out`, `size` [w, h], `seed`, `title`, `elements`), `logo` and `gifs`. Positions are card
pixels. Element types:

| `type` | Keys |
| --- | --- |
| `capture` | `capture` (file in `art/captures`), `crop` [x, y, w, h] in source pixels (x or y may be `"c"` to centre), optional `down` (box downscale by a whole factor, applied first), `scale` (whole-number upscale), or instead `resample: "area"` with `size` [w, h] (the crop is resized to exactly that size with the area filter; use it for shots that are scaled down by a non-whole factor), `at` [x, y] (top left of the picture), `frame` (default true: a 3 px brass rim over the picture edge), `label`, `label_style` (accent, good, bad, dim, gold), `label_at` (tl, tr, bl, br) |
| `studio` | `scene`, `frame` (a still name, a frame index or a file stem), `at` [x, y] and `size` [w, h] (the rectangle the render is fitted into: centred, only ever scaled down, area filter, no frame or box), `shadow` (default true: the soft contact shadow on the card under it), optional `crop` [x, y, w, h] in matte pixels (a zoom, applied first), `up` (whole number: the crop is enlarged by nearest neighbour, for a detail of a render; the rectangle must be big enough for the enlarged crop, it is only ever scaled down), `border` (true: a 2 px brass frame round the picture, for a detail crop), `trim` (`"y"`, `"x"` or `"xy"`: cut the empty rows / columns round the subject, keeping `trim_margin` matte pixels, default 8; `"y"` keeps the width, so two rows of the same width get the same scale), `style` (composite style overrides), `label` + `label_style` + `label_at` (tl, tr, bl, br, tc, bc: a name box inside the rectangle) or `label_below` (a name box centred just under the rectangle) |
| `label` | `at`, `text`, `style` |
| `text` | `at`, `width`, `text`, `size` (whole scale, default 2), `color` (text, dim, accent, gold, good, bad or `#rrggbb`), `align` (left: `at.x` is the left edge; centre: the middle; right: the right edge), `leading` |
| `bullets` | `at`, `width`, `items`, `size`, `gap` |
| `chips` | `at`, `align`, `items` (strings or `{text, style}`; style accent or brass), `gap` |
| `rule`, `panel`, `heading`, `strip` | see `build.py` (layout only; there are no drawn decorations) |

Picture size on the card of a `capture` is `crop size / down * scale`, or `size` with `resample: "area"` (the crop must have the same shape as `size`),
so pictures stay on whole pixels. For captures taken at **GUI scale 2**, `down: 2` is lossless: one GUI pixel becomes one card pixel and the game's
text stays crisp. The card text uses only ASCII, set in the game's own font read from the Minecraft 1.21.1 client jar at build time.

The cards that use studio renders: `banner.png` (`stills_hero` / `banner_hero`, left of the title), `card_universal.png` (`universal_vanilla`,
`universal_modded`: two 920 x 164 rows, `trim: "y"`, no shadow), `card_glint_presets.png` and `card_outline_styles.png` (the 14 + 10 tile shots at
150 px, a name box under each tile; the outline tiles are cropped to 235 x 213 matte pixels and set at 176 x 160, see "Outline tiles" in `docs/STUDIO.md`), `card_gap_fix.png` (the four gap shots as 3x nearest-neighbour detail crops, `crop` + `up` + `border`, 450 x 198 px each). When the real renders exist, check each tile for a
subject that is cut off or too small and move its `size`, `crop` or `trim` here.

## Verify

`py -B art/verify.py` (about 2 minutes; `--full` takes about 30 minutes) checks that

* the PNG reader decodes every filter type, RGB, RGBA and a 1-bit palette image exactly, and rejects garbage;
* the manifest only uses known elements, every picture box lies inside its card, every capture and every studio shot is described in `SHOTLIST.md`
  and exists in the studio contract, the cards show the shots they should (banner, universal, glint presets, outline styles, gap fix), the `logo`
  section names a contract still, the four GIFs are turntables of the contract with the scene's shape, at most 1000 px wide and at most 2,000,000 bytes (budget at most 1.9), and
  every image of the manifest has its paste marker in `docs/curseforge/description.md`;
* every text colour the build puts on a cream surface has a WCAG contrast of at least **4.5:1** (`MIN_CONTRAST`; the 3:1
  allowance for large text is not used). It checks the pairs of the table above (`TEXT`, `TEXT_DIM`, `GOOD`, `BAD`,
  `GOLD_TEXT`, `ACCENT`, `TITLE` on `PANEL` and on the darkest dither speck of a real card, `INK` on `BORDER`, the
  neutral colours also on `PANEL_MID` and `PANEL_DARK`), `INK` on the grey-beige "capture missing" slab, the text colour
  of every label style on the label box, every named text colour of `lib.COLOURS` on the card, the colour of every
  `text` and `bullets` element of `cards.json` on the card, the dither speck and the panel it sits in, and the caption
  bar text of the GIFs on `PAGE`. Text that is part of an in-game capture is the game's own and is not checked;
* the cards, the logo files and the GIFs are built twice, from the captures in the repository (the GIFs only with `--full`) and from synthetic
  input (in-world PNGs plus the nine synthetic studio scenes), and come out byte-identical both times, and the synthetic build draws no placeholder;
  the synthetic scenes have the contract's names, sizes and still shots;
* the GIFs built from the synthetic scenes have the manifest's size (plus the caption bar), loop forever, have frame delays of at least 2 cs and
  are below their budget; with `--full` the GIFs in `docs/curseforge/img/` are checked the same way (a shrunk picture of at least 0.6 of the size
  is accepted: the budget ladder) and equal a fresh build; an impossible budget ends the build with an error, `min_scale` 1 never shrinks the
  picture, a missing scene gives a grey one-frame GIF and a warning;
* the area filter averages exactly (a 4 x 4 image becomes the 2 x 2 block means, 3 -> 2 weights by covered fraction, a flat colour
  stays flat, transparent pixels do not darken, whole factors equal the box downscale);
* from the synthetic logo scenes (`logo_concepts` / `concept_a`, `logo_anim_a`): logo 512, icon 128 and logo 32 px, the icon and the small logo
  are area filters of the 512 picture byte for byte, the subject fills `fill` of the square, the glow tints the cream backdrop round it, and
  `logo_animated.gif` is 256 x 256 with every frame, loops forever, its delays add up to the loop and it is at most 2,000,000 bytes (also the
  committed one with `--full`), the same bytes for 1 and 2 workers; `logo.py` without the scenes prints the missing files, writes nothing and
  exits 0 (1 with `--strict`), and leaves the committed `icon.png` and `logo_animated.gif` untouched;
* every committed card and logo image is byte-identical to a fresh build (an image whose captures are not on this machine is reported as skipped,
  not failed; a warning is printed while `icon.png` is still the hand-drawn one of the first commit);
* sizes: cards and banner as in the manifest (at most 1000 px wide; a card whose studio renders are not on this machine may still be the old one);
* the studio (`art/studio/`): the matte round trip, a synthetic turntable end to end (GIF frames, size, delays, delta boxes, loop, budget and step-down,
  identical bytes for any number of workers), stills, the `studio` card element (crop, trim, `label_below`, missing scenes) and the command lines,
  among them `synth_studio.py --size 256` (a square) and a clean usage error for a bad size;
* the manifest has no drawn elements (`logo`, `sparkle`, `arrow`) and area-filter pictures are downscales of the same shape;
* every script imports only the standard library and no `__pycache__` exists below `art/`; no synthetic capture is in `art/captures/`.

`--full` additionally rebuilds the four GIFs from the real studio scenes in `art/captures/studio/` twice and checks
the GIFs on disk; run it once after the real build.

## Output files

| Path | Size | What |
| --- | --- | --- |
| `art/logo/logo_512.png` | 512 x 512 | the logo, from `logo_concepts` / `concept_a` |
| `art/logo/logo_32.png` | 32 x 32 | the small logo |
| `src/main/resources/assets/glintsmith/icon.png` | 128 x 128 | the mod icon, from `logo_concepts` / `concept_a` |
| `docs/curseforge/img/logo_animated.gif` | 256 x 256 | the golden blade, `logo_anim_a` (glint and outline colour, fixed pose), made by `logo.py`, at most 2,000,000 bytes |
| `docs/curseforge/img/banner.png` | 1000 x 420 | the `banner_hero` render, title, tagline and chips |
| `docs/curseforge/img/card_universal.png` | 1000 x 590 | two rows of studio renders: vanilla and modded items, all outlined |
| `docs/curseforge/img/lineup.gif` | 810 x 306 | six items in a row, each with its own look, swinging, captioned, at most 2,000,000 bytes |
| `docs/curseforge/img/card_glint_presets.png` | 1000 x 822 | the neutral glint and all twelve generated presets (seven clean, five rich) as studio stills, with the vanilla glint as the before, in a grid of 5 x 3 (the last slot holds a note) |
| `docs/curseforge/img/card_outline_styles.png` | 1000 x 648 | ten outline styles as studio stills: vanilla, static, rainbow, pulse, gradient, per enchantment, thickness, soft glow |
| `docs/curseforge/img/card_targets.png` | 1000 x 590 | hand and armor (hero shots), third person, dropped items, item frames, inventory |
| `docs/curseforge/img/armor.gif` | 320 x 436 | the armor stand turning through its looks, captioned, at most 2,000,000 bytes |
| `docs/curseforge/img/card_gap_fix.png` | 1000 x 744 | before / after of the gap fix on an enchanted netherite sword and a modded chestplate: 3x detail crops of the studio stills, the cracks show in the vanilla tiles |
| `docs/curseforge/img/card_rules.png` | 1000 x 590 | the rule list and the rule editor |
| `docs/curseforge/img/card_config.png` | 1000 x 846 | the 2.0.0 settings screen with the live preview panel, Outline and Glint tabs |
| `docs/curseforge/img/card_shaders.png` | 1000 x 590 | Sodium, and an Iris shader pack in the world, the hand and the inventory |
| `docs/curseforge/img/showcase.gif` | 384 x 420 | the `hero_sword` turntable through its looks, captioned, at most 2,000,000 bytes |

## Licence and sources

* **The logo and the icon** are renders of the running mod (an enchanted golden sword, from the studio), matted, framed, set on the cream backdrop with a
  glow made of the render's own pixels and resized by `logo.py`; they are not drawn.
* **The gallery images and the GIFs** show Minecraft and the textures and models of the mods in the test pack as renders and gameplay screenshots. They are
  renders of the running game, not redistributed assets, and nothing from the game is committed as a file other than those images.
* **The vanilla font** is read from your own Minecraft 1.21.1 client jar every time the gallery is built (`GLINTSMITH_MC_JAR`, else
  the Loom cache under `.gradle/loom-cache/minecraftMaven/` or `~/.gradle/caches/fabric-loom/1.21.1/`). It is never extracted or committed.
* The card frame, the heading style and the text routines are adapted from the MIT licensed showcase scripts of Lootr Remastered
  (`art/showcase_lib.py`, same author).

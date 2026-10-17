# Glintsmith art

Everything that makes the CurseForge page images and the mod icon.

* **The gallery** (`docs/curseforge/img/banner.png`, `card_*.png`, `showcase.gif`) is composed from **in-game captures of the
  real mod running**: screenshots of Minecraft with Glintsmith, cropped, scaled by whole factors and set into framed cards.
  Nothing in it is drawn or simulated; a picture slot whose capture is missing shows a grey "capture missing" placeholder.
* **The logo** (`art/logo/`, the mod icon, `logo_animated.gif`) is **original pixel art** drawn by `art/logo.py`. It contains
  no Minecraft pixels; it is the mod's identity.

Python 3 standard library only (`py -B ...` on this machine; `python` is not on the PATH). PNG is written with `zlib` + `struct`,
GIF with our own LZW encoder and median-cut palette (`imgio.py`). Output is byte-identical from run to run (no timestamps, fixed seeds).

## Workflow

```
1. capture      follow art/gallery/SHOTLIST.md; save the PNGs as art/captures/<name>.png (and art/captures/showcase/0001.png ...)
2. look         py -B art/gallery/build.py --list      which captures exist, which are missing
3. build        py -B art/gallery/build.py             writes docs/curseforge/img/*.png and showcase.gif
                py -B art/gallery/build.py --strict    the same, but exit 1 if any capture is missing (use before a release)
4. adjust       open the cards; move a crop in art/gallery/cards.json where the subject is off; build again
5. verify       py -B art/verify.py
```

Other commands:

```
py -B art/gallery/build.py --only card_gap_fix            only the images whose name contains the text
py -B art/gallery/build.py --captures DIR --out DIR       read the captures from / write the images to other folders
py -B art/gallery/gif.py                                  only the showcase GIF (build.py calls it too; --no-gif skips it)
py -B art/gallery/synth.py DIR                            synthetic stand-ins for every capture, to test the layout (see below)
py -B art/logo.py [--out DIR]                             the logo, the mod icon and logo_animated.gif
```

Always run Python with `-B` so no `__pycache__` is left in `art/` (every script also sets `sys.dont_write_bytecode`).

### Missing captures are never faked

`build.py` draws a clearly marked grey slab, "capture missing: <file name>", where a capture is not there, prints one warning per
file and carries on; with `--strict` it exits 1 afterwards. `gif.py` does the same with a one-frame grey GIF when
`art/captures/showcase/` has no frames. The repository ships in that state until the captures exist.

### Testing the layout without captures

`py -B art/gallery/synth.py DIR` writes a 1920 x 1080 stand-in for every capture the manifest names (a gradient with a coordinate
grid, a diagonal bar, the file name and the words "SYNTHETIC TEST CAPTURE") and 40 numbered GIF frames. Build from it with
`build.py --captures DIR --out DIR2` and look at `DIR2` to check crops, scales, labels and spacing. The grid labels show which source
pixels a crop picks. These files are only ever written to a folder you name (use a temp folder); they carry the text in a PNG
chunk and `verify.py` fails if one ends up in `art/captures/`.

## The gallery pipeline

| File | What it does |
| --- | --- |
| `gallery/cards.json` | Manifest of every gallery image: output name, size, title, and a list of elements (captures with crop / scale / position / label, text, bullets, chips, panels, logo, ...), plus the GIF entry. |
| `gallery/build.py` | Reads the manifest and the captures, writes the PNGs. |
| `gallery/gif.py` | Builds `showcase.gif` from `art/captures/showcase/NNNN.png`: crop, optional whole-number scale, every n-th frame, caption bar in the game font, one global palette with delta frames, loops forever, below 8 MB (raises the step by itself if needed). |
| `gallery/synth.py` | Synthetic test captures (see above). |
| `gallery/common.py` | Manifest and capture loading, crop / scale helpers, the missing-capture report. |
| `gallery/SHOTLIST.md` | Every capture: rig, world, scene, camera, config values, file name. |
| `lib.py` | RGBA canvas (fill, frame, crop, nearest-neighbour scale, box downscale, alpha blit, PNG in and out), the vanilla font, word wrap, the card frame, chips, labels, bays, placeholder. |
| `imgio.py` | PNG writer and reader (8-bit RGB / RGBA of every filter type, which is what the game writes; palette and grey for the vanilla font), GIF89a writer with LZW, median-cut quantiser with delta frames, and a small GIF decoder used by `verify.py`. |
| `logo.py` | The 32 x 32 logo, its 512 and 128 versions and the animated icon. |
| `verify.py` | The checks below. |

### The manifest

`cards.json` has `cards` (one entry per image: `out`, `size` [w, h], `seed`, `title`, `elements`) and `gifs`. Positions are card pixels.
Element types:

| `type` | Keys |
| --- | --- |
| `capture` | `capture` (file in `art/captures`), `crop` [x, y, w, h] in source pixels (x or y may be `"c"` to centre), optional `down` (box downscale by a whole factor, applied first), `scale` (whole-number upscale), `at` [x, y] (top left of the picture), `frame` (default true: a 3 px brass rim over the picture edge), `label`, `label_style` (accent, good, bad, dim, gold), `label_at` (tl, tr, bl, br) |
| `label` | `at`, `text`, `style` |
| `text` | `at`, `width`, `text`, `size` (whole scale, default 2), `color` (text, dim, accent, gold, good, bad or `#rrggbb`), `align` (left: `at.x` is the left edge; centre: the middle; right: the right edge), `leading` |
| `bullets` | `at`, `width`, `items`, `size`, `gap` |
| `chips` | `at`, `align`, `items` (strings or `{text, style}`; style accent or brass), `gap` |
| `rule`, `panel`, `heading`, `strip`, `logo`, `arrow`, `sparkle` | see `build.py` |

Picture size on the card is `crop size / down * scale`, so pictures stay on whole pixels. For captures taken at **GUI scale 2**,
`down: 2` is lossless: one GUI pixel becomes one card pixel and the game's text stays crisp. The card text uses only ASCII, set in the game's own
font read from the Minecraft 1.21.1 client jar at build time.

The `gifs` entry takes `frames_dir`, `crop`, `down`, `scale`, `step` (keep every n-th frame), `source_fps` (20), `max_step`, `max_mb`, the
`caption_bar` (`height`, `scale`, `color`) and the `captions` (one per look, each lasting `frames_per_caption` source frames).

## Verify

`py -B art/verify.py` checks that

* the PNG reader decodes every filter type, RGB, RGBA and a 1-bit palette image exactly, and rejects garbage;
* the manifest only uses known elements, every picture box lies inside its card, and every capture is described in `SHOTLIST.md`;
* the logo files, the cards and the GIF are built twice, from the captures in the repository and from synthetic ones, and come out
  byte-identical both times, and the synthetic build draws no placeholder;
* every committed image is byte-identical to a fresh build (an image whose captures are not on this machine is reported as skipped, not failed);
* sizes: cards and banner as in the manifest (at most 1000 px wide), logo 32 / 512, icon 128, `logo_512.png` and the icon are exact
  whole-number scalings of the native logo, the animated logo is 128 x 128, 40 frames at 20 fps, below 1 MB, loops forever and
  its last frame leads seamlessly into the first, the showcase GIF has the manifest size, loops forever and is below 8 MB;
* an impossible GIF size limit ends the build with an error instead of an oversized file;
* every script imports only the standard library and no `__pycache__` exists below `art/`; no synthetic capture is in `art/captures/`.

## Output files

| Path | Size | What |
| --- | --- | --- |
| `art/logo/logo_native.png` | 32 x 32 | the logo: a sword with a one pixel rainbow outline on a dark violet tile |
| `art/logo/logo_512.png` | 512 x 512 | the logo at x16 |
| `src/main/resources/assets/glintsmith/icon.png` | 128 x 128 | the mod icon (the logo at x4) |
| `docs/curseforge/img/logo_animated.gif` | 128 x 128, 40 frames, 2 s | the icon with the outline hue cycling and a glint running along the blade |
| `docs/curseforge/img/banner.png` | 1000 x 420 | logo, title, tagline, chips and a real capture strip |
| `docs/curseforge/img/card_universal.png` | 1000 x 590 | vanilla and modded items, 3D shields and tridents, all outlined |
| `docs/curseforge/img/card_glint_presets.png` | 1000 x 590 | the glint presets, captured, with the vanilla glint as the before |
| `docs/curseforge/img/card_outline_styles.png` | 1000 x 590 | vanilla, static, rainbow, pulse, gradient, per enchantment, thickness, soft glow |
| `docs/curseforge/img/card_targets.png` | 1000 x 590 | hand, third person, armor, dropped items, item frames, inventory |
| `docs/curseforge/img/card_gap_fix.png` | 1000 x 590 | before / after of the gap fix, vanilla and modded item |
| `docs/curseforge/img/card_rules.png` | 1000 x 590 | the rule list and the rule editor |
| `docs/curseforge/img/card_config.png` | 1000 x 590 | the settings screen, Outline and Glint tabs |
| `docs/curseforge/img/card_shaders.png` | 1000 x 590 | Sodium, and an Iris shader pack in the world, the hand and the inventory |
| `docs/curseforge/img/showcase.gif` | 720 x 441 | seven looks cycling on one armor stand, captioned |

## Licence and sources

* **The logo** (`logo.py` and its outputs, the mod icon) is original pixel art, released under the MIT licence of the repository (see `LICENSE`).
* **The gallery images** show Minecraft and the textures and models of the mods in the test pack as gameplay screenshots. They are
  screenshots of the running game, not redistributed assets, and nothing from the game is committed as a file other than those screenshots.
* **The vanilla font** is read from your own Minecraft 1.21.1 client jar every time the gallery is built (`GLINTSMITH_MC_JAR`, else
  the Loom cache under `.gradle/loom-cache/minecraftMaven/` or `~/.gradle/caches/fabric-loom/1.21.1/`). It is never extracted or committed.
* The card frame, the heading style and the text routines are adapted from the MIT licensed showcase scripts of Lootr Remastered
  (`art/showcase_lib.py`, same author).

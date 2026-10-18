# Final report: Glintsmith 2.0.0

Fabric 1.21.1 client mod: universal item outlines, a customisable enchantment glint, a model gap fix and (2.0.0) a live
preview. 1.0.0 was built in sessions 1 to 12 and uploaded in session 14; 2.0.0 in sessions 13 to 15 (2026-10-18).
Release jar `build/libs/glintsmith-2.0.0.jar`, SHA-256 `b91aea4f6ac4be923a9192f734b5b3438df2a29f3b286ee800a1e54f2dd283c6`,
built at commit 3219f73, 486 tests green. CurseForge project 1751215: 1.0.0 = file 9234824, 2.0.0 = file 9235330 (uploaded 2026-10-18 04:45, release). The owner instance now holds the release jar (old dev jar in `buildquarantineowner-instance-2.0.0-dev-jar`).

## What was implemented, per requirement

| Requirement | Result |
|---|---|
| 1.0.0 on CurseForge | Uploaded 2026-10-18 as file 9234824 (owner-tested fixed jar c1297c15, commit 45414a8, tag `v1.0.0`). |
| Live preview (2.0.0) | Settings screen panel with the held item (fallback enchanted netherite sword) in first-person look under the unsaved settings, turntable + drag, source strip, notes. Checked in game: panel, animation, following unsaved settings. |
| Atlas-independent glint scale | Reads the block atlas size after each reload (factor 0.125 x 0.125 in the 8192 x 4096 test pack), keeps the vanilla pattern density; option on by default. Checked in game (on: pattern, off: 1.0.0 wash). |
| Custom GUI glints with ImmediatelyFast | GUI and batched HUD buffer sources hooked. Checked in game (inventory + hotbar, IF 1.6.15). |
| Thickness per place + owner config as defaults (O7) | `outline.targetThickness` per place; fresh/reset configs use the owner's look (slow rainbow outline 8.5 px 25 %, frost + light purple tint, fire in GUI/entities, armor glint off). Checked in game. |
| Equal widths (O8) and proportional outlines (O9, owner) | One resolution factor for every phase; proportional mode (default) measures the thickness on the first-person item and scales every other item by its on-screen size, with `outline.minThickness` (1 px at 1080p). Checked in game: 8.5 px in hand, ~1 px icons, thinner far drops; off = equal widths. |
| Glint presets redesigned + rich presets (G4, owner) | Seven presets redrawn as vanilla-like streaks over an even glow; five new: inferno, nebula, starlight, opal, storm. All twelve checked in game in first person. |
| Real art, no pixel art, real renders on cream | Dev-only studio renders (7 scenes) composed into the cards and GIFs; settings/targets/rules/shaders cards from in-game screenshots. Lead looked at every card and GIF. |
| Docs | README, CHANGELOG (2.0.0 dated 2026-10-18), DESIGN (sections 3.4.1, 5.6, 10, 12), mod page `docs/curseforge/` (description, paste version, preview, checklist with SHA), STUDIO.md, art README/SHOTLIST. No pending markers left. |
| Git / GitHub | `dev` and `main` pushed to `navrelis/Glintsmith`, tag `v2.0.0`. |

## Changed files (2.0.0, sessions 13 to 15)

- `src/main/java/navrelis/glintsmith/render/preview/`, `gui/preview/`: live preview (render core and panel).
- `render/glint/`: atlas factor, ImmediatelyFast GUI/HUD hooks, `GlintPresets`, `GlintNoise`, `GlintTextures` (G4).
- `render/outline/`: `OutlineScale` (proportional width), one resolution factor, StyleTable nearest-entry fallback.
- `config/`: `targetThickness`, `proportional`, `minThickness`, new built-in defaults, migration; `gui/`: Outline tab rows,
  Glint tab toggle; lang en/de.
- `src/test/`: 486 tests (was 366 at 1.0.0), `TestConfigs.vanilla()` baseline.
- `src/studio/` (dev-only source set, not in the jar): studio runtime, `Looks.java`, 7 scene JSONs.
- `art/studio/` (matte, composite, GIF encoder), `art/gallery/` (cards.json, build.py `up`/`border`), `docs/curseforge/img/`
  (banner, 9 cards, showcase/lineup/armor/logo GIFs), `art/logo/`, jar icon.
- Docs as listed above; `graphify-out/` updated.

## Key decisions (full list in `decisions.md`)

- Proportional outline width is the default; O8 equal pixels stay as the "off" mode; studio looks pin `proportional=false`.
- Presets judged by a mean-squared-RGB budget near the vanilla glint instead of a dark-texel rule; names kept.
- Atlas factor = reference vanilla atlas / actual atlas (times the diamond sword sprite size), applied to item UV sources only.
- In-game testing only on an isolated copy of the owner's instance (owner instance fingerprinted unchanged).
- The 2.0.0 jar is rebuilt at 3219f73; its code equals the in-game-tested jar (28e0ba9), only the icon and one tooltip changed.
- lineup.gif keeps the bow's stray outline dot (renderer quirk, see limitations) instead of hiding it.

## Checks and tests run (session 15)

- `./gradlew build --no-daemon`: green, 486 tests, 0 failures, 0 errors, 0 skipped. Jar: 225 entries, 0 studio entries,
  `LICENSE_glintsmith` inside, `fabric.mod.json` 2.0.0 / client / minecraft 1.21.1, icon identical to the source.
- Studio: 7 scenes rendered, `py -B art/studio/studio.py check` 7 ok (180 stale hero frames quarantined).
- `py -B art/logo.py`, `py -B art/gallery/build.py --strict`, `py -B art/verify.py` (199 ok),
  `py -B docs/curseforge/preview.py --check --strict`: all exit 0. GIFs: showcase 7.18 MB, armor 6.73 MB, lineup 4.04 MB
  (budget 8 MB), logo 1.99 MiB (budget 2 MiB).
- Grep: no TODO/FIXME/System.out/printStackTrace in `src/`; no PENDING / "not yet checked" in README, CHANGELOG, description.
- In game (session 14, V1 run on the instance copy, jar 28e0ba9): 319 steps ok, 66/66 shots, viewed by the lead.
- CurseForge: remote dry run resolved (1.21.1=11779, Fabric=7499, Java 21=11135, Client=9638; fabric-api required,
  modmenu optional), display name = file name, as in the 1.0.0 plan.

## Manual test checklist (owner; not yet done by a human)

- Proportional outlines in the owner's pack at GUI scale 3 fullscreen: hand ~8.5 px, inventory thin line, dropped /
  third person / frames shrink with distance; Minimum width slider; Proportional off gives equal widths.
- Windows of 1440 and 2160 px height; 300 distinct outline colours on screen (DESIGN 10 item 32 c, d).
- Live preview by hand: drag, right-click reset, source strip, rule editor and colour picker live edits, narrow window /
  GUI scale 4, with a shader pack (DESIGN 10 item 31).
- New presets on armor, frames and in the GUI; frost under the default tint; per-place thickness rows at GUI scales 1-4.
- Iris shader pack paths; lodestone compass; atlas option off gives the flat wash; chest and creative tabs with IF.

## Model usage

Work units (short = 1, long = 2), whole project: Sonnet ~81.5, Opus ~31.5, so Opus ≈ 28 %. Session 15: Sonnet only
(P2b-3 S, A2 L, checklist S). Precision overrides: G1 (world glint under Iris, earlier session). G4 (preset taste) and
O9 (core render math) went to Opus within the budget.

## Known limitations and recommendations

- Stray outline dot: with the bow (soft void outline) a single mask pixel with nothing drawn in the colour pass appears
  about 100 px beside the bow at some yaws (studio lineup, 21 of 120 frames). Reproduced with the bow alone. Not
  investigated; recommended follow-up (Opus, mask pass vs colour pass of the bow model).
- In the owner's pack a 3D mace model from a resource pack keeps a purple glint on its handle (not investigated).
- On armor every preset reads as a slow colour wash (vanilla armor glint scale), by design.
- With proportional on, GUI outlines are ~0.5 px and clamp to Minimum width (1 px); at opacity 0.25 they are faint.
- Proportional widths can exceed the set thickness for items drawn larger than the first-person reference (capped at 32 px).
- Under an Iris shader pack: no world outlines, world and hand glint vanilla.
- `loom_version=1.17-SNAPSHOT`: pin a release when one is out.
- Put the CurseForge page address into `fabric.mod.json` `contact.homepage` in the next build.
- Housekeeping for the owner: merged worktrees `F:\Coding\Glintsmith-studio`, `-o9`, `-g4`, `.claude\worktrees\agent-*`,
  and `build\quarantine\` can be removed.

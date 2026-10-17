# Final report: Glintsmith 1.0.0

Fabric 1.21.1 client mod: universal item outlines, a customisable enchantment glint and a model gap fix. Built from
scratch in sessions 1 to 12 (2026-10-17 / 18). Branch `dev`; the jar is `build/libs/glintsmith-1.0.0.jar`
(SHA-256 `168d49fbfccb75151a15617b7b56f13778fbeb15db8bcbff784c10e3a9de74b5`).

## What was implemented, per requirement

| Requirement | Result |
|---|---|
| Universal: every item of every mod, any model type | Outlines come from a screen-space mask of what the game really draws plus an exact distance transform, so flat sprites, 3D models, shields, tridents and modded renderers all work without per-item setup. |
| Outlines on held items (1st/3rd person), armor, dropped items, item frames, optional GUI | All places implemented with a switch each. Armor default off (owner decision). GUI, HUD hotbar and screens work. Punchy-style first-person mods handled (T14/T18). |
| Rules by id, tag, enchantment, mod, allow/deny | Rule engine with id, tag, mod, wildcard, negation, enchantment, enchanted state and place, plus an exception list; later rules override earlier ones. |
| Per rule colour, thickness, animation, durability alert, only-me | Static, rainbow, pulse, gradient, per-enchantment; thickness 0.5 to 16 px, softness, opacity, fade to 256 blocks, durability alert, "only my items". |
| Glint: presets, own textures, tint, speed, scale, strength, per enchantment/rule, separate layers | Seven generated presets + neutral, PNGs from `config/glintsmith/glints/`, resource-pack textures, tint/speed/scale/strength/rotation, four layers (items, armor, entities, GUI), per rule and per enchantment colour. No third-party art shipped. |
| Gap fix: on/off, safe auto default, tuning, no FPS cost, macOS | Off / automatic / custom; UV-shrink scaling, side expansion 0.005 (in-game sweep), indent, precise spans; stays off next to `modelfix` in auto mode. |
| Never crash with Sodium or Iris | Verified in game. Under an Iris shader pack world outlines turn off cleanly (one INFO line), hand outlines are hidden by the arm, GUI outlines and glints work. With Iris installed and shaders off, custom glints work in the world (G1). |
| Own config screen, Mod Menu, keybind, live reload, en + de | Vanilla-widget screen with five tabs and a colour picker, optional Mod Menu entry, `/glintsmith reload|toggle`, two unbound keys, live reload, English and German (287/287 keys). |
| Hard deps Fabric Loader + Fabric API only, nothing bundled | Yes. Mod Menu, Sodium API and Iris are compile-only. |
| Mod page package, logo, GIF, Lootr-style showcase, "how much better" | `docs/curseforge/` (description, paste version, summary, preview, comparison table, upload checklist) and `docs/curseforge/img/` (banner, 8 cards, showcase GIF, animated logo), logo in `art/logo/`, icon in the jar. Every image is a real in-game render on white/cream (no pixel art). |
| Public GitHub repo `navrelis/Glintsmith` | Waiting for the owner's one-time confirmation (see below). |

## Changed files (whole project)

- `src/main/java/navrelis/glintsmith/`: `config/` (JSON config, validation, live reload), `style/` (rule engine, colour
  evaluator, settings records), `render/outline/` (mask, distance transform, composite, Iris occluder pass),
  `render/glint/` (presets, textures, slot pool, Iris buffer hook), `modelfix/` (gap fix maths), `mixin/` (outline, glint,
  modelfix mixins and a plugin for optional mods), `gui/` (screens, tabs, widgets, keys, commands), `integration/` (Mod Menu).
- `src/main/resources/`: `fabric.mod.json`, mixin configs, lang en_us/de_de, icon, glint preset textures generated at build time.
- `src/test/`: 37 test files, 366 tests.
- `docs/DESIGN.md` (architecture), `docs/curseforge/*` (mod page), `README.md`, `CHANGELOG.md`, `LICENSE` (MIT).
- `art/` (reproducible standard-library Python pipeline: gallery cards, GIFs, logo, verification), `art/logo/`.
- `graphify-out/` (knowledge graph of the final code, cache ignored).
- Session 12: final gallery/logo/icon commit, jar SHA-256 in the upload checklist, Graphify update, this report.

## Key decisions (full list in `decisions.md`)

- Screen-space mask + exact distance transform instead of per-item models (universal, any geometry).
- Under an Iris shader pack world outlines step aside; the hand pass records depth-only occluders so the arm hides the outline.
- AUTO gap fix side expansion 0.005 from an in-game sweep; armor outline target off by default (owner).
- Big-atlas preset glints are a documented limitation; the Scale option fixes them per pack.
- Captures use full-bright text_display panels as cream backdrops; close-ups through the vanilla FOV option.

## Checks and tests run (session 12)

- `./gradlew build --no-daemon`: green, 366 tests, 0 failures, 0 skipped. Jar icon identical to the source icon.
- `py -B art/gallery/build.py --strict`, `py -B art/logo.py --strict`, `py -B art/verify.py`, `py -B docs/curseforge/preview.py --check`: all exit 0.
- Lead looked at `card_outline_styles.png` (pulse centred) and `card_shaders.png` (corrected text) and the round-1 outputs.
- Grep: no TODO/FIXME/System.out; the two `LOGGER.debug` lines (first-person phase summary, glint slot overflow) are intentional.
- Graphify update: 2351 nodes, 7012 edges, built from commit 2b902ff; no local paths in the graph or in `.knowledge/`.

## Manual test checklist (✔ = verified in game)

- Gap fix: ✔ no fins · ✔ corner cracks closed · slivers up close · HD items · `modelfix` present
- Outlines, shaders off: ✔ first person · ✔ no outline over the arm · ✔ dropped, frames, occlusion · ✔ GUI toggle · ✔ HUD hotbar · ✔ Fabulous · ✔ stray dots gone · third person with armor target on · creative tabs / chest GUI · colour modes, softness/opacity/thickness/fade, durability alert · window resize
- Iris: ✔ no crash · ✔ world outlines off + 1 log line · ✔ GUI outlines · ✔ glint vanilla in the world under a pack · ✔ hand outline not over the arm · "Hand with shader pack" switch off · vanilla first person with a map · stained glass in one hand · toggling the pack in game
- Glint: ✔ custom presets in the world with Iris installed, shaders off · item frames / dropped / zombie-held under Iris · `file:` PNG, resource-pack texture, tint/speed/scale/strength/rotation · master off = vanilla · per-rule/enchantment tint, 32-slot overflow, F3+T, shield/trident
- Config UI: all tabs, rule editor, colour picker, Mod Menu, German, GUI scales
- Art: ✔ every image a real render on cream/white

## Model usage

Work units (short = 1, long = 2): Sonnet ~58.5, Opus ~18, so Opus ≈ 24 %. Precision override: G1 (world glint under Iris,
Opus): third-party buffer hook with slot lifetime questions, a subtle error would have broken a headline feature. The
render core (T2 design, T5 outline renderer) and the in-game bug hunts T13, T14, T19, T20, T21, T24 also went to Opus as
hard tasks within the budget.

## Known limitations and recommendations

- Big-atlas resource packs make preset glints look small; raise the glint Scale (documented in README, description, tooltip).
- Under an Iris shader pack: no world outlines, world and hand glint vanilla; inventory keeps outlines and custom glints.
- Not checked in game under Iris: item frames, dropped items and mob-held items with custom glints.
- `loom_version=1.17-SNAPSHOT` is a snapshot plugin; pin a release when one is out.
- The original Nytheria test harness is broken; a shadow copy was used for in-game runs.
- After the CurseForge page exists, put its address into `fabric.mod.json` `contact.homepage` in the next build.
- Owner housekeeping: remove the merged worktrees `Glintsmith-hand` and `Glintsmith-hud` and the scratchpad quarantine folders.

# Final report: Glintsmith 2.0.0

Fabric 1.21.1 client mod: universal item outlines, a customisable enchantment glint, a model gap fix and (2.0.0) a live
preview. 1.0.0 was built in sessions 1 to 12 and uploaded in session 14; 2.0.0 in sessions 13 to 15 (2026-10-18).
Release jar `build/libs/glintsmith-2.0.0.jar`, SHA-256 `b91aea4f6ac4be923a9192f734b5b3438df2a29f3b286ee800a1e54f2dd283c6`,
built at commit 3219f73, 486 tests green. CurseForge project 1751215: 1.0.0 = file 9234824, 2.0.0 = file 9235330 (uploaded 2026-10-18 04:45, release). The owner instance now holds the release jar (old dev jar in `build/quarantine/owner-instance-2.0.0-dev-jar/`).

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

- Stray outline dot beside the bow: fixed for 2.0.1 (session 17, see below).
- In the owner's pack a 3D mace model from a resource pack keeps a purple glint on its handle (not investigated).
- On armor every preset reads as a slow colour wash (vanilla armor glint scale), by design.
- With proportional on, GUI outlines are ~0.5 px and clamp to Minimum width (1 px); at opacity 0.25 they are faint.
- Proportional widths can exceed the set thickness for items drawn larger than the first-person reference (capped at 32 px).
- Under an Iris shader pack: no world outlines, world and hand glint vanilla.
- `loom_version=1.17-SNAPSHOT`: pin a release when one is out.
- Put the CurseForge page address into `fabric.mod.json` `contact.homepage` in the next build.
- Housekeeping for the owner: merged worktrees `F:\Coding\Glintsmith-studio`, `-o9`, `-g4`, `.claude\worktrees\agent-*`,
  and `build\quarantine\` can be removed.

## Session 16 follow-up (owner, after the 2.0.0 upload)

- **GIFs under 2,000,000 bytes** (3d7eb15, Sonnet): budget 1.9 MiB; new encoder levers (palette size `colours`, temporal `lossy` tolerance, `max_scale` pin). showcase 1,867,707 (384 px, 15 fps), lineup 1,748,251 (810 px, 30 fps), armor 1,916,336 (320 px, 12 fps, 64 colours; faint lossy trails accepted by the owner).
- **New logo** (b846ff6, Opus): 3 studio-rendered concepts, owner picked A: golden sword, fire glint, violet outline, colour glow on cream. `art/logo.py` builds logo 512/32, icon 128 and `logo_animated.gif` (1,235,898 bytes, 165 frames, 20 fps, seamless 8.25 s loop) from scenes `logo_concepts` / `logo_anim_a`. The jar icon ships with the next mod version.
- **CurseForge page**: description replaced with the 2.0.0 text, all 13 images linked to the owner's uploads (attachments 2071/589-601), saved and verified on reload. The project logo (avatar) itself is set by the owner.
- Checks: gradle build 0, `art/logo.py --strict`, `art/verify.py`, `docs/curseforge/preview.py --check`, `art/studio/studio.py check` all 0. Not run: `verify.py --full`.
- Usage this session: Sonnet S 1, Opus L 2 + S 1; total about 30 % Opus. No precision overrides.
- Open: stray outline dot fix lives on branch claude/eloquent-wing-a06858 (other session, not merged); lineup needs a re-render after it lands. Clean-up candidates (git-ignored): build/logo-concepts, build/quarantine/l1, art/captures/studio/logo_explore1-5 and logo_anim_b.

## Session 17: 2.0.1 fixes (unreleased, on dev)

What was done, per owner decision (2026-10-18):
- **Stray outline dot, outline side** (B1, Opus, 129f299): the flat GUI/PREVIEW outline mask was double-sided while the
  colour pass culls back faces; on a turntable the back quad's atlas-bleeding edge pixels became mask-only seeds with a
  full ring. The flat mask now repeats the source's cull state. `MaskTypes.java`, `MaskTypesTest` (red first, then green).
- **Stray pixel, gap fix side** (B2, Opus, 7dc772e): with gap fix AUTO (UV shrink 0) a pixel at a flat item's outer
  edge sampled the neighbouring atlas sprite. Edge guard, always on with the gap fix: front/back faces stop 1/64 texel
  short of the sprite border (positions and UVs together), border side faces move in by the same amount, side-face UVs
  move 0.25 texel inwards across the thickness. `GapFix.java`, `ItemModelGeneratorMixin.java`, 9 new tests (5 fail
  without the guard). Studio: stray frames 0 at atlas 2048/8192/16384 (before 20/47/94), crack pixels 1994 -> 1991.
- **Lineup GIF** (B3, Sonnet, 743467f): re-rendered on the fixed code with the release studio-mods jars; strays.py 0 of
  120 frames (2.0.0: 61); `docs/curseforge/img/lineup.gif` 1,731,398 bytes with the G2MB settings.
- Docs: CHANGELOG 2.0.1 (Unreleased) with both Fixed entries, DESIGN 3.2 and 11 (new "Edge guard (2.0.1)" with the
  measurement table), README gap-fix section. Lead proofread the DESIGN wording.
- Merged into dev and pushed (main untouched, no release, as decided by the owner). Graphify updated (3284 nodes,
  9961 edges; backup quarantined in the main checkout build/quarantine/graphify-backup-s17).

Checks: `./gradlew build --no-daemon` 495 tests, 0 failures (lead, after each merge); strays.py 0 (lead re-ran);
`art/verify.py` all passed, `preview.py --check` 0 (B3); lead viewed GIF frames 1 and 61.

Manual test checklist (in game, not yet done):
- Settings screen live preview, outlined bow or trident turning: no stray dot rings.
- Inventory GUI outlines unchanged (head-on).
- Held, dropped and item-frame flat items with gap fix AUTO: no wrongly coloured edge pixel, no new hairline cracks.
- Same with a big HD resource pack (atlas 8192+).

Model usage: Opus B1 S 1 + B2 S 1, Sonnet B1 docs S 1 + B3 S 1; project total about 30 % Opus. No precision overrides.

Open for the owner:
- The live CurseForge page still shows the 2.0.0 lineup.gif (attachment 2071/596); upload the new file and update the
  description link if wanted.
- Release 2.0.1 when wanted (version bump, in-game check, merge to main, tag, upload).
- Housekeeping the permission check would not let the lead do: remove the merged worktrees `F:\Coding\Glintsmith-g4`,
  `-o9`, `-studio`, `.claude\worktreesgent-ab21401f389c63083` (locked), `busy-wilson-9ce123`, `eloquent-wing-a06858`
  and their merged local branches (commands in the session's final message).

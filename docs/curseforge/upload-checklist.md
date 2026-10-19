# CurseForge upload checklist: Glintsmith 2.1.0

Glintsmith 1.0.0 was uploaded on 2026-10-18 to CurseForge project 1751215 (file 9234824) with the owner's uploader
`F:\Coding\CurseForgeUpload\curseforge-upload.ps1`; 2.0.0 was uploaded on 2026-10-18 as file 9235330; 2.0.1 was uploaded
on 2026-10-18 as file 9238522; 2.0.2 was uploaded on 2026-10-18 as file 9238814; 2.1.0 is uploaded the same way. Field
names follow the CurseForge file form as I know it; check each one against the form you see, they change from time to
time. Items marked (unverified) are suggestions I could not check.

Paths are relative to the repository root.

This is the checklist for the 2.1.0 update (a minor release: new built-in defaults) of the existing project. The values
of the earlier releases are kept at the end of this file: 2.0.2 first, then 2.0.1, then 2.0.0, then 1.0.0.

## Before 2.1.0 goes public

| Done | Item | Where |
|---|---|---|
| [x] | 2.1.0 release gate: the build is made from the release commit, the tests are green, and `py -B art/verify.py` and `py -B docs/curseforge/preview.py --check` pass. Commit, test count and SHA-256 are entered in the file table below. Done: built from commit 04b6f29; 502 tests green; `art/verify.py` and `preview.py --check` pass. | |
| [ ] | The in-game check of the 2.1.0 defaults is done: a fresh install with no `config\glintsmith.json` shows the new default look, a static light purple outline #E6B2FF at 7.5 px on the held, dropped and framed items, worn armor and GUI items, plus the neutral glint with a light purple rainbow tint on all four layers. The config the mod writes equals the owner's config. Run on the isolated instance copy. Not run: the owner chose to release 2.1.0 without the in-game check (2026-10-18). The defaults are pinned by DefaultConfigTest (a fresh config equals the owner's config file). The check script is ready for a later run on the isolated instance copy. | `CHANGELOG.md` section 2.1.0 |
| [x] | `gradle.properties` says `mod_version=2.1.0` and the `CHANGELOG.md` 2.1.0 heading carries the release date. Done: `gradle.properties` 2.1.0 and the CHANGELOG 2.1.0 heading is dated 2026-10-18. | |
| [x] | Known and not changed by 2.1.0: the page images are rendered with the studio. The banner, `card_default_look.png`, the default look in `showcase.gif` and `armor.gif` and the neutral glint tile show the 2.1.0 defaults (look base `defaults`); the other studio images keep their explicit looks (outline styles, presets, gap fix, lineup, universal), which show what the mod can do. | |

## The file

| Field | Value |
|---|---|
| File to upload | `build/libs/glintsmith-2.1.0.jar` (not the `-sources` jar and not a `-dev` jar if one is in the folder) |
| SHA-256 of the checked build | `15e615d38d686268a28301a2218f272b1113e09f284afc6c22ca41c1937d0a74` (`build/libs/glintsmith-2.1.0.jar`, commit 04b6f29, 502 tests green; not checked in game, see above) |
| Display name | `glintsmith-2.1.0.jar` (the earlier uploads used the file name) |
| Version | 2.1.0 |
| Release type | Release |
| Game version | 1.21.1 |
| Mod loader | Fabric |
| Java | 21 |
| Environment | Client only (`"environment": "client"`) |
| Changelog | paste the section `2.1.0` of `CHANGELOG.md` |

### Relations

| Type | Project |
|---|---|
| Required dependency | Fabric API |
| Optional dependency | Mod Menu |
| Compatible (not a declared relation) | Sodium, Iris, Model Gap Fix |

`fabric.mod.json` lists Mod Menu, Sodium and Iris under `suggests`. Model Gap Fix is not a dependency or a conflict; in automatic mode the gap fix of Glintsmith stays off while it is installed, and in custom mode the settings stack on top of it (precise spans are inactive).

## The project page

| Field | Value |
|---|---|
| Name | Glintsmith |
| Slug | `glintsmith` (if free) |
| Summary | `docs/curseforge/summary.txt` (230 characters; the limit is 250) |
| Description | `docs/curseforge/description_paste.md`, then the images at the marker lines (see `docs/curseforge/README.md`) |
| Logo | the mod icon `src/main/resources/assets/glintsmith/icon.png`, or the logo from `art/` if the owner prefers (check the size limits of the form) |
| Licence | MIT License |
| Categories | suggestion (unverified, pick from the current list): Cosmetic, Utility and QoL |
| Source link | `https://github.com/navrelis/Glintsmith` |
| Issues link | `https://github.com/navrelis/Glintsmith/issues` |
| Wiki, donation links | none |
| Showcase images | the PNGs and GIFs in `docs/curseforge/img/` |
| Screenshots | optional extra captures from `art/captures/` (git-ignored, local only) |
| Third-party distribution (for example in modpacks) | owner decides; the MIT licence allows it |

## What the owner has to decide or do by hand

The project exists (1751215) and the release type is Release. Still open:

1. Whether the comparison table stays on the page. It names other projects; every statement in it is taken from their public pages, and where a page is silent the table says "not stated". Read it again against the live pages.
2. Whether to tell the authors of Custom Glow Enchantment Outline and Model Gap Fix about the comparison (courtesy; nothing requires it).
3. Add the project page's address to `fabric.mod.json` (`contact.homepage` currently points to GitHub) in the next build, and to the README if wanted.

## Earlier release: Glintsmith 2.0.2 (uploaded 2026-10-18, file 9238814)

The values below are the final ones of the 2.0.2 release.

### Before 2.0.2 went public

| Done | Item | Where |
|---|---|---|
| [x] | 2.0.2 release gate: the build is made from the release commit, the tests are green, and `py -B art/verify.py` and `py -B docs/curseforge/preview.py --check` pass. Commit, test count and SHA-256 are entered in the file table below. Done: built from commit 07fae66; 499 tests green; `art/verify.py` and `preview.py --check` pass. | |
| [x] | The in-game check of the 2.0.2 fix is done: an enchanted trident in the main hand is drawn whole and centred in the live preview of the settings screen over a full turn, and the enchanted bow and sword are still inside the panel. Run on the isolated instance copy. Done: tridentcheck run of 2026-10-18 on the isolated instance copy with exactly the release jar (840c1d78). 18 of 18 shots, 80 steps, 0 failed, owner instance unchanged. The enchanted trident (8 shots over a full turn) is whole and centred in the panel, the plain trident (4), enchanted bow (4) and netherite sword (2) stay inside the panel. One harmless Sodium WARN that the preview's measuring buffer does not support optimized vertex writing. | `CHANGELOG.md` section 2.0.2 |
| [x] | `gradle.properties` says `mod_version=2.0.2` and the `CHANGELOG.md` 2.0.2 heading carries the release date. Done: `gradle.properties` 2.0.2 and the CHANGELOG 2.0.2 heading is dated 2026-10-18. | |

### The file (2.0.2)

| Field | Value |
|---|---|
| File to upload | `build/libs/glintsmith-2.0.2.jar` (not the `-sources` jar and not a `-dev` jar if one is in the folder) |
| SHA-256 of the checked build | `840c1d78208719fa74f5a618526fe54e71eb94222ef9b8862ded963b6b97f544` (`build/libs/glintsmith-2.0.2.jar`, commit 07fae66, 499 tests green; checked in game on 2026-10-18 with exactly this jar) |
| Display name | `glintsmith-2.0.2.jar` (the earlier uploads used the file name) |
| Version | 2.0.2 |
| Release type | Release |
| Game version | 1.21.1 |
| Mod loader | Fabric |
| Java | 21 |
| Environment | Client only (`"environment": "client"`) |
| Changelog | paste the section `2.0.2` of `CHANGELOG.md` |

## Earlier release: Glintsmith 2.0.1 (uploaded 2026-10-18, file 9238522)

The values below are the final ones of the 2.0.1 release.

### Before 2.0.1 went public

| Done | Item | Where |
|---|---|---|
| [x] | 2.0.1 release gate: the build is made from the release commit, the tests are green, and `py -B art/verify.py` and `py -B docs/curseforge/preview.py --check` pass. Commit, test count and SHA-256 are entered in the file table below. Done: built from commit 8529521; 495 tests green; `art/verify.py` and `preview.py --check` pass. | |
| [x] | The in-game check of the 2.0.1 fixes is done: no stray outline dot rings beside a turning bow or trident in the live preview of the settings screen, no wrongly coloured edge pixel on flat items (in the hand, dropped, in an item frame) with the gap fix on AUTO, and no new hairline cracks. The 2.0.0 regression run is done as well. Done: v201check run of 2026-10-18 on the isolated instance copy (8192 x 4096 atlas, ImmediatelyFast). 94 of 94 shots, 0 failed steps, owner instance unchanged. Bow (10 shots) and trident (6 shots) turned a full revolution in the live preview with no stray dot rings. No wrong edge pixels on the held, dropped and framed bow and the dropped sword. 2.0.0 regression set (66 shots) as before. | `CHANGELOG.md` section 2.0.1 |
| [x] | `gradle.properties` says `mod_version=2.0.1` and the `CHANGELOG.md` 2.0.1 heading carries the release date. Done: `gradle.properties` 2.0.1 and the CHANGELOG 2.0.1 heading is dated 2026-10-18. | |
| [x] | `lineup.gif` on the page: the repository's `docs/curseforge/img/lineup.gif` was re-rendered without the stray dots. Done: uploaded on 2026-10-18 as attachment 2071/810 (1,731,398 bytes, same size as the repository file), and the description link now points at 2071/810 instead of 2071/596 (the 2.0.0 GIF, still in the media gallery but no longer linked). | `docs/curseforge/img/` |
| [x] | Known and not changed by 2.0.1: in the live preview a held trident shows its 3D held model and sits partly below the panel (the preview draws the first-person transform). This is the same as in 2.0.0. | |

### The file (2.0.1)

| Field | Value |
|---|---|
| File to upload | `build/libs/glintsmith-2.0.1.jar` (not the `-sources` jar and not a `-dev` jar if one is in the folder) |
| SHA-256 of the checked build | `b86b290ef8fdb620ea280b2e2a0256919a4d8147f311a3823316d0dd3fd6ab38` (`build/libs/glintsmith-2.0.1.jar`, commit 8529521, 495 tests green; checked in game on 2026-10-18 with exactly this jar) |
| Display name | `glintsmith-2.0.1.jar` (the earlier uploads used the file name) |
| Version | 2.0.1 |
| Release type | Release |
| Game version | 1.21.1 |
| Mod loader | Fabric |
| Java | 21 |
| Environment | Client only (`"environment": "client"`) |
| Changelog | paste the section `2.0.1` of `CHANGELOG.md` |

## Earlier release: Glintsmith 2.0.0 (uploaded 2026-10-18, file 9235330)

The values below are the final ones of the 2.0.0 release.

### Before 2.0.0 went public

| Done | Item | Where |
|---|---|---|
| [x] | 2.0.0 release gate: the build is made from the release commit, the tests are green, and `py -B art/verify.py` and `py -B docs/curseforge/preview.py --check` pass. Commit, test count and SHA-256 are entered in the file table below. | |
| [x] | The in-game checks of the 2.0.0 features are done (2026-10-18, V1 run: live preview on the Outline and Glint tabs, atlas-independent scale, custom GUI glints with ImmediatelyFast, thickness per place, proportional outlines and minimum width, the redrawn and the five new glint presets). Every pending marker in `docs/curseforge/description.md` and every not-checked note in `README.md` is resolved. The grep `grep -rn "VERIFY IN GAME\|PENDING" README.md CHANGELOG.md docs/curseforge/description.md` finds nothing. What was not tried by hand is listed in `docs/DESIGN.md` section 10, items 31 and 32. | `docs/DESIGN.md` section 10 |
| [x] | The gallery images show the 2.0.0 looks and the settings screen with the preview (`card_config.png` was rebuilt from the 2.0.0 in-game screenshots); the gallery's preset card shows all twelve generated presets (rainbow, aurora, fire, frost, sparkle, gold, void, inferno, nebula, starlight, opal, storm), and the page text about the presets and about proportional outlines is final. | `art/README.md` |
| [x] | The page text and `CHANGELOG.md` agree on the 2.0.0 section (the heading carries the release date), and `fabric.mod.json` and `gradle.properties` say 2.0.0 (`gradle.properties` has `mod_version=2.0.0`, `build.gradle` expands `${version}` in `fabric.mod.json` from it). | |
| [x] | The gap fix, glint and outline wiring is in the build, so every setting does something. Done. | `.knowledge/plan.md` |
| [x] | (1.0.0) The in-game test was done and no open-claim comment was left. See the 2.0.0 row above for the new features. | |
| [x] | `fabric.mod.json` is checked: it says `"minecraft": "1.21.1"` exactly (so it does not match 1.21.2 or later) and `"fabricloader": ">=0.16.0"`; the mod was built and run with 0.19.5. | `src/main/resources/fabric.mod.json` |
| [x] | The gallery images are built from real captures: `py -B art/gallery/build.py --strict` passes and `py -B art/verify.py` is clean. Done. | `art/README.md` |
| [x] | `py -B docs/curseforge/preview.py --check` passes. Done. | |
| [x] | The repository is public (pushed), because the description and `fabric.mod.json` link to `https://github.com/navrelis/Glintsmith`. | |
| [ ] | The comparison table has been read again against the live pages of the projects it names (versions and features change). Sources and date are under the table. | `description.md` |
| [ ] | Licence check of the jar: `LICENSE` (as `LICENSE_glintsmith`) is inside, no file of the studied projects is in the source tree. | `.knowledge/decisions.md` |

### The file (2.0.0)

| Field | Value |
|---|---|
| File to upload | `build/libs/glintsmith-2.0.0.jar` (not the `-sources` jar and not a `-dev` jar if one is in the folder) |
| SHA-256 of the checked build | `b91aea4f6ac4be923a9192f734b5b3438df2a29f3b286ee800a1e54f2dd283c6` (`build/libs/glintsmith-2.0.0.jar`, commit 3219f73, 486 tests green). The code is the same as in the jar checked in game on 2026-10-18 (built at 28e0ba9); since then only the icon and one glint scale tooltip (en/de) changed. |
| Display name | `glintsmith-2.0.0.jar` (the 1.0.0 upload used the file name) |
| Version | 2.0.0 |
| Release type | Release |
| Game version | 1.21.1 |
| Mod loader | Fabric |
| Java | 21 |
| Environment | Client only (`"environment": "client"`) |
| Changelog | paste the section `2.0.0` of `CHANGELOG.md` |

## Earlier release: Glintsmith 1.0.0

| Field | Value |
|---|---|
| File | `build/libs/glintsmith-1.0.0.jar` |
| Uploaded | 2026-10-18, CurseForge project 1751215, file id 9234824 |
| SHA-256 of the uploaded jar | `c1297c15e3b49dd316b148affaea7e017071dc980bb9bdb631a8b07d00d77b69` (the fixed build from commit 45414a8) |
| SHA-256 of the first checked build (not uploaded) | `168d49fbfccb75151a15617b7b56f13778fbeb15db8bcbff784c10e3a9de74b5` (commit 2b902ff, 366 tests green; before the fix) |

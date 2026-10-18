# CurseForge upload checklist: Glintsmith 2.0.0

Nothing here has been uploaded. Field names follow the CurseForge project and file forms as I know them; check each one
against the form you see, they change from time to time. Items marked (unverified) are suggestions I could not check.

Paths are relative to the repository root.

This is the checklist for the 2.0.0 update of the existing project. Items marked `TBD` get their final value at release.
The values of the first release (1.0.0) are kept at the end of this file.

## Before 2.0.0 goes public

| Done | Item | Where |
|---|---|---|
| [ ] | 2.0.0 release gate: the build is made from the release commit, the tests are green, and `py -B art/verify.py` and `py -B docs/curseforge/preview.py --check` pass. Commit, test count and SHA-256 are entered in the file table below. | |
| [ ] | The in-game checks of the 2.0.0 features are done (live preview, atlas-independent scale, custom GUI glints with ImmediatelyFast, thickness per place, proportional outlines and minimum width, the redrawn and the five new glint presets) and every `<!-- PENDING: ... -->` comment in `docs/curseforge/description.md` and every "Not yet checked in game" in `README.md` is resolved. The grep `grep -rn "VERIFY IN GAME\|PENDING" README.md CHANGELOG.md docs/curseforge/description.md` finds nothing. | `docs/DESIGN.md` section 10 |
| [ ] | The gallery images show the 2.0.0 looks and the settings screen with the preview (`card_config.png` is from 1.0.0 until it is rebuilt); the gallery's preset card shows all twelve generated presets (rainbow, aurora, fire, frost, sparkle, gold, void, inferno, nebula, starlight, opal, storm), and the page text about the presets and about proportional outlines is final. | `art/README.md` |
| [ ] | The page text and `CHANGELOG.md` agree on the 2.0.0 section (no `unreleased`), and `fabric.mod.json` and `gradle.properties` say 2.0.0. | |
| [x] | The gap fix, glint and outline wiring is in the build, so every setting does something. Done. | `.knowledge/plan.md` |
| [x] | (1.0.0) The in-game test was done and no open-claim comment was left. See the 2.0.0 row above for the new features. | |
| [x] | `fabric.mod.json` is checked: it says `"minecraft": "1.21.1"` exactly (so it does not match 1.21.2 or later) and `"fabricloader": ">=0.16.0"`; the mod was built and run with 0.19.5. | `src/main/resources/fabric.mod.json` |
| [x] | The gallery images are built from real captures: `py -B art/gallery/build.py --strict` passes and `py -B art/verify.py` is clean. Done. | `art/README.md` |
| [x] | `py -B docs/curseforge/preview.py --check` passes. Done. | |
| [ ] | The repository is public (the owner's step), because the description and `fabric.mod.json` link to `https://github.com/navrelis/Glintsmith`. | |
| [ ] | The comparison table has been read again against the live pages of the projects it names (versions and features change). Sources and date are under the table. | `description.md` |
| [ ] | Licence check of the jar: `LICENSE` (as `LICENSE_glintsmith`) is inside, no file of the studied projects is in the source tree. | `.knowledge/decisions.md` |

## The file

| Field | Value |
|---|---|
| File to upload | `build/libs/glintsmith-2.0.0.jar` (not the `-sources` jar and not a `-dev` jar if one is in the folder) |
| SHA-256 of the checked build | `TBD` (`build/libs/glintsmith-2.0.0.jar`, commit `TBD`, `TBD` tests green) |
| Display name | `Glintsmith 2.0.0` |
| Version | 2.0.0 |
| Release type | owner decides: Release or Beta |
| Game version | 1.21.1 |
| Mod loader | Fabric |
| Java | 21 |
| Environment | Client only (`"environment": "client"`) |
| Changelog | paste the section `2.0.0` of `CHANGELOG.md` |

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

1. Release type and the final wording of the page.
2. Whether the comparison table stays on the page. It names other projects; every statement in it is taken from their public pages, and where a page is silent the table says "not stated".
3. Whether to tell the authors of Custom Glow Enchantment Outline and Model Gap Fix about the comparison (courtesy; nothing requires it).
4. After the project page exists: add its address to `fabric.mod.json` (`contact.homepage` currently points to GitHub) in the next build, and to the README if wanted.
5. Create the CurseForge project, fill in the form from this list, upload the file, and wait for the project and file approval.

## Earlier release: Glintsmith 1.0.0

| Field | Value |
|---|---|
| File | `build/libs/glintsmith-1.0.0.jar` |
| SHA-256 of the checked build | `168d49fbfccb75151a15617b7b56f13778fbeb15db8bcbff784c10e3a9de74b5` (commit 2b902ff, 366 tests green) |

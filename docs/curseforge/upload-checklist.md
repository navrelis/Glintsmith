# CurseForge upload checklist: Glintsmith 1.0.0

Nothing here has been uploaded. Field names follow the CurseForge project and file forms as I know them; check each one
against the form you see, they change from time to time. Items marked (unverified) are suggestions I could not check.

Paths are relative to the repository root.

## Before anything goes public

| Done | Item | Where |
|---|---|---|
| [x] | The gap fix, glint and outline wiring (task T8) is in the build, so every setting does something. Done. | `.knowledge/plan.md` |
| [ ] | The in-game test (T10) is done and every `VERIFY IN GAME` comment in `README.md`, `CHANGELOG.md` and `docs/curseforge/description.md` has been checked and the text corrected. List them with `grep -rn "VERIFY IN GAME" README.md CHANGELOG.md docs/curseforge/description.md` | |
| [ ] | `fabric.mod.json` is checked: it says `"minecraft": "~1.21.1"`, which in Fabric's version syntax also matches 1.21.2 and later 1.21.x; if the mod is meant for 1.21.1 only, use `"1.21.1"`. It says `"fabricloader": ">=0.16.0"`; the mod was built and run with 0.19.5. | `src/main/resources/fabric.mod.json` |
| [ ] | The gallery images are built from real captures: `py -B art/gallery/build.py --strict` passes and `py -B art/verify.py` is clean. | `art/README.md` |
| [ ] | `py -B docs/curseforge/preview.py --check` passes. | |
| [ ] | The repository is public (the owner's step), because the description and `fabric.mod.json` link to `https://github.com/navrelis/Glintsmith`. | |
| [ ] | The comparison table has been read again against the live pages of the projects it names (versions and features change). Sources and date are under the table. | `description.md` |
| [ ] | Licence check of the jar: `LICENSE` (as `LICENSE_glintsmith`) is inside, no file of the studied projects is in the source tree. | `.knowledge/decisions.md` |

## The file

| Field | Value |
|---|---|
| File to upload | `build/libs/glintsmith-1.0.0.jar` (not the `-sources` jar and not a `-dev` jar if one is in the folder) |
| SHA-256 of the checked build | fill in after the final build |
| Display name | `Glintsmith 1.0.0` |
| Version | 1.0.0 |
| Release type | owner decides: Release, or Beta for the first public build |
| Game version | 1.21.1 |
| Mod loader | Fabric |
| Java | 21 |
| Environment | Client only (`"environment": "client"`) |
| Changelog | paste the section `1.0.0` of `CHANGELOG.md`, without the HTML comment |

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
| Summary | `docs/curseforge/summary.txt` (210 characters; the limit is 250) |
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

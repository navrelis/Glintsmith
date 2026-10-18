# Versioning

Glintsmith uses semantic versioning: `MAJOR.MINOR.PATCH`, for example 2.0.2.

## What each number means

* MAJOR: could this break something for me?
* MINOR: is there something new I can use?
* PATCH: same mod, fewer bugs.

## How to choose

Go through the CHANGELOG section of the release and ask three questions in this order. The first "yes" decides.

1. **Does anything break, or does something need the player (or another mod) to act?** Then it is MAJOR
   (2.0.2 -> 3.0.0). Examples:
   * Old config files can no longer be read as they are, so settings are lost or reset.
   * A feature is removed, or works so differently that players must set it up again.
   * A supported Minecraft version is dropped (for example a port to 1.21.4 that drops 1.21.1).
   * Another mod that uses Glintsmith would stop working.
2. **Is there anything new?** Then it is MINOR (2.0.2 -> 2.1.0). A new feature, preset, outline style, option, command
   or mod compatibility. Everything from before still works and old configs load unchanged.
3. **Only fixes?** Then it is PATCH (2.0.2 -> 2.0.3). Something broken now works as intended. Nothing new to learn and
   nothing to change in the config.

## Rules

1. The biggest change wins. Ten fixes plus one new option is a MINOR release.
2. Numbers to the right go back to 0: 2.0.2 -> 2.1.0, and 2.4.3 -> 3.0.0.
3. Numbers only go up and are never reused. An uploaded version is fixed forever. A mistake in it becomes the next patch.
4. A config migration that keeps the player's settings automatically does not by itself force MAJOR. Losing or
   resetting settings does.

## Glintsmith so far

| Release | What changed | Kind |
|---|---|---|
| 1.0.0 -> 2.0.0 | Big rework released as a new major line: config file reshaped, live preview, atlas scale, many behaviour changes | MAJOR |
| 2.0.0 -> 2.0.1 | Stray outline dots and edge bleed fixed | PATCH |
| 2.0.1 -> 2.0.2 | Held trident drawn below the live preview panel fixed | PATCH |

## Release steps

Where the version lives. Change all of these for a new release:

1. `gradle.properties`: `mod_version`.
2. `CHANGELOG.md`: rename `## Unreleased` to `## X.Y.Z (Fabric 1.21.1) — YYYY-MM-DD`.
3. The jar name `build/libs/glintsmith-X.Y.Z.jar` in `README.md` and `docs/STUDIO.md`.
4. The comparison table header in `docs/curseforge/description.md` and `docs/curseforge/description_paste.md`, then run
   `py -B docs/curseforge/preview.py` to regenerate `docs/curseforge/preview.html`.
5. `docs/curseforge/upload-checklist.md`.
6. The git tag `vX.Y.Z` on main.
7. The CurseForge display name, which is the jar file name.

If two Minecraft versions are ever supported in parallel, add a suffix such as `2.1.0+mc1.21.1`.

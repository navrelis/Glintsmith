# Glintsmith

Client-only Minecraft mod (`glintsmith`, Navrelis, MIT): custom enchantment glint, item outlines, model gap fix. Repo `navrelis/Glintsmith`. Version 2.2.0 (unreleased), metadata in `gradle/shared.properties`.

## Branches
- `mc/<version>`: source of one Minecraft version. `mc/1.21.1` is the three-loader reference, ports merge it.
- `dev`: hub with `.knowledge/` and older single-loader code. `main`: releases, tags `vX.Y.Z`.
- Session 22 works on `claude/s22-shaders` (worktree `.claude/worktrees/project-lead-setup-855e3d`), `.knowledge/` is untracked there and committed back on the dev line.

## Structure
- `common/`: shared source tree, not a Gradle module, must stay loader-neutral (`checkCommonIsLoaderNeutral`).
- `fabric/`, `neoforge/`, `forge/`: standalone Gradle projects with their own wrapper. Jar `glintsmith-<ver>+<mc>-<loader>.jar` in `<loader>/build/libs`.
- `common/src/smoke/` + `<loader>/src/smoke/`: dev-only smoke mod, not in the jar. `fabric/src/studio/`: art studio, 1.21.1 only.
- Docs: `docs/DESIGN.md`, `ARCHITECTURE.md`, `TESTING.md`, `VERSIONING.md`, `STUDIO.md`, `docs/curseforge/`.

## Entry points
- Fabric `fabric/src/main/java/navrelis/glintsmith/fabric/GlintsmithFabric.java`, NeoForge `.../neoforge/GlintsmithNeoForge.java`, Forge `.../forge/GlintsmithForge.java` + `GlintsmithForgeClient.java`. All call `GlintsmithClient.init()`.
- Platform service: `common/.../platform/{Platform,Services}.java` via ServiceLoader.

## Key modules (`common/src/main/java/navrelis/glintsmith/`)
`config/`, `style/` (RuleEngine, settings), `render/outline/` (OutlineSystem, OutlineCompositor, `compat/IrisCompat`), `render/glint/` (GlintSystem, GlintSlotPool, IrisWrappedTypes), `render/preview/`, `gui/`, `mixin/` (`outline/`, `glint/`, `glint/compat`, `modelfix/`), `modelfix/`.

## Commands
- All loaders: `./build-all.ps1 [tasks]`. One loader: `cd fabric; ./gradlew --no-daemon build`.
- Smoke: `pwsh scripts/smoke.ps1 -Loader fabric,neoforge,forge [-Exported]`; shader mode `-Loader fabric -Shaders`. Results in `build/smoke/<mc>-<loader>[-exported|-shaders]/`.
- Resource rules in `requirements.md`: count Java processes first, one Gradle or client at a time, always `--no-daemon`, never `gradlew --stop`.

## Versions and conventions
- Minecraft 1.21.1, Java 21, Mojang mappings, tabs. Semver per `docs/VERSIONING.md`.
- Fabric Loader 0.19.5 (shader smoke 0.16.14), Fabric API 0.116.17+1.21.1, Loom 1.17-SNAPSHOT. NeoForge 21.1.264 (ModDevGradle). Forge 52.1.2 (ForgeGradle 7, MixinExtras 0.5.5 jar-in-jar).
- Sodium 0.8.13, Iris 1.8.14-beta.1, Mod Menu 11.0.5: compile-only.

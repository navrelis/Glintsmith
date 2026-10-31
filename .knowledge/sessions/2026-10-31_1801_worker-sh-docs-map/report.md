# Report: sh-docs-map

## Task 1: project map (read only; no code or docs changed, no Gradle, no game)
**What:** client-only MC mod `glintsmith` (Navrelis, MIT): custom enchantment glint, item outlines, model gap fix. 2.2.0, MC 1.21.1, Java 21, Fabric+NeoForge+Forge. Metadata in `gradle/shared.properties`.
**Branches** (inferred from `git branch -vv`; no doc states the roles): `mc/<version>` = source of one MC version, `mc/1.21.1` is the reference, `mc/1.20.6` its port (docs/ARCHITECTURE.md:3-5). `dev` = hub with `.knowledge/` and the lead board (commit 5afff93), still at 2.0.2 era. `main` = releases, tags `vX.Y.Z` (docs/VERSIONING.md:50). Work on `claude/*`; s22 = `claude/s22-shaders`, HEAD 677212e, 3 commits ahead of `mc/1.21.1` (dd20c30).
**Structure** (docs/ARCHITECTURE.md): `common/` is a shared source tree (no Gradle module, must stay loader-neutral: task `checkCommonIsLoaderNeutral`). `fabric/ neoforge/ forge/` are standalone Gradle projects, each with its own wrapper, adding `common/src/main/{java,resources}` + `common/src/test/java`; jar `glintsmith-<ver>+<mc>-<loader>.jar` in `<loader>/build/libs`. Platform service: `common/.../platform/{Platform,Services}.java`, ServiceLoader, registered per loader in `META-INF/services/navrelis.glintsmith.platform.Platform`. Smoke source set: `common/src/smoke/{java,resources}` (SmokeRunner/Scene/Result, glintsmith-smoke.json) + `<loader>/src/smoke/` (dev mod `glintsmith_smoke`); dev-only, built by `check`, not in the jar. Studio: `fabric/src/studio/` (docs/STUDIO.md).
**Entry points:** Fabric `fabric/src/main/java/navrelis/glintsmith/fabric/GlintsmithFabric.java` (+FabricPlatform, ModMenuIntegration, `render/outline/SodiumTeeVertexConsumer`); NeoForge `neoforge/.../neoforge/GlintsmithNeoForge.java` (`@Mod(dist=CLIENT)`); Forge `forge/.../forge/GlintsmithForge.java` + `GlintsmithForgeClient.java`. All call `GlintsmithClient.init()` and forward the hooks of the ARCHITECTURE.md table; NeoForge/Forge use mixin hooks for lifecycle/world-render.
**Key modules** (`common/src/main/java/navrelis/glintsmith/`): `GlintsmithClient` (hooks); `config/` (ConfigManager, GlintsmithConfig); `style/` (RuleEngine, OutlineSettings, GlintSettings); `render/outline/` (OutlineSystem, Mask*, OutlineCompositor, Tee*, OccluderBufferSource, `compat/IrisCompat` = Iris reflection); `render/glint/` (GlintSystem, GlintRenderTypes, GlintSlotPool, IrisWrappedTypes); `render/preview/`; `gui/` (GlintsmithScreen, `tab/`, widget/, preview/); `mixin/` (GlintsmithMixinPlugin; `outline/`, `glint/` with `glint/compat` = Iris G7 + ImmediatelyFast G9/G11/G12; `modelfix/`); `modelfix/`; `platform/`. Mixin configs `common/src/main/resources/glintsmith{,.glint,.outline}.mixins.json`. Design doc: `docs/DESIGN.md` (~2300 lines).
**Build/test/smoke** (docs/TESTING.md): `./build-all.ps1 [tasks]` (all loaders, `--no-daemon`, stops at first failure); one loader `cd fabric && ./gradlew build`. Smoke `pwsh scripts/smoke.ps1 -Loader fabric,neoforge,forge [-Exported] [-OutDir D]`; shader pack mode `-Loader fabric -Shaders` (Iris+Sodium+pack, `-ShaderModsDir`, `-ShaderPack`); game only `cd fabric; .\gradlew.bat runSmoke`. Results in `build/smoke/<mc>-<loader>[-exported|-shaders]/`. Always `--no-daemon`, never `gradlew --stop`.
**Versions/conventions:** semver, docs/VERSIONING.md (MAJOR breaks, MINOR new, PATCH fixes; release steps). Mojang names in `common/`, no loader imports. Fabric (`fabric/gradle.properties`): Loader 0.19.5 (shader smoke 0.16.14), API 0.116.17+1.21.1, Loom 1.17-SNAPSHOT, Sodium 0.8.13, Iris 1.8.14-beta.1, Mod Menu 11.0.5. NeoForge 21.1.264 (ModDevGradle, Parchment 2024.11.17). Forge 52.1.2 (ForgeGradle 7, MixinExtras 0.5.5 jar-in-jar). Optional mods are compile-only. Java uses tabs.

## Task 2 locations (grep-checked on HEAD 677212e)
**`[SH3b pending]`: 5 markers, all `docs/DESIGN.md`** (`grep -c` and `grep -o | wc -l` both 5):
- 1162 (G7 table row, "worn armor only on the chestplate **[SH3b pending]**")
- 1660 (5.2 point 5; sentence starts 1659, "only the chestplate ... so far")
- 1680 (section 6 matrix row "Iris, shader pack on", last sentence of the glint column)
- 2066 (section 9 point 3; sentence starts 2065; line 2067 "*Recommendation:* fix worn armor (SH3b), then re-verify ..." depends on the result)
- 2199 (section 9 point 23; sentence starts 2198)
**"Only the chestplate" sentences, other files:**
- `README.md:213-215` ("Worn armor is only partly covered: the chestplate shows ... (being investigated).")
- `CHANGELOG.md:33-36` (whole "### Known issues" section of 2.2.0; heading at 33, bullet 35-36)
- `docs/curseforge/description.md:59` and `docs/curseforge/description_paste.md:59` (identical: "On worn armor only the chestplate shows it so far."; the two files are twins, keep them identical)
- Not hits: `docs/TESTING.md:104` (old outline port bug).
**Sodium "not yet confirmed" (no TeeVertexConsumer WARN with Sodium 0.8.13):**
- `README.md:178-182` ("(not yet confirmed with a log in game)" at 179-180)
- `docs/DESIGN.md:1678` (section 6 Sodium row: "**[UNVERIFIED in game: no log with Sodium 0.8.13 checked since the change]**")
- `docs/DESIGN.md:2095-2100` (section 9.12: "**[UNVERIFIED in game]**" 2097-2098, plus the Recommendation 2099-2100 "check the log of a Sodium run once")
- May contradict a confirmed result: `docs/curseforge/description.md:100` and `description_paste.md:100` ("The log has one expected Sodium warning ... harmless"). `CHANGELOG.md:26-27` already says done; `CHANGELOG.md:152` is 2.0.0 history. Leave both.
- NOT Sodium: `docs/DESIGN.md:1158, 1160, 1161, 1687` ("not verified yet" = ImmediatelyFast).

## Open points
- `docs/VERSIONING.md` release steps name `gradle.properties` `mod_version`; it is `gradle/shared.properties` now. `README.md:253` still says `glintsmith-2.1.0.jar`. Neither changed (out of scope).

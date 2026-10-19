# Architecture

How the source of one Minecraft version branch (`mc/<version>`) is laid out: loader-neutral code in `common/`, one
standalone Gradle build per mod loader, and a small platform service that is the only way `common/` talks to a loader.
`mc/1.21.1` is the reference; every other version branch has the same shape.

## Layout

```
gradle/shared.properties   mod id, name, version, license, authors, description, Minecraft version, Fabric version
                           predicate; read by every loader's settings.gradle, so each value is defined once
common/                    loader-neutral code and assets, no build of its own
  src/main/java              render, gui, config, style, modelfix, mixins, navrelis.glintsmith.platform
  src/main/resources         assets/glintsmith/** (shaders, lang, icon) and the three glintsmith*.mixins.json
  src/test/java              the pure unit tests (no Fabric or Minecraft bootstrap), run by every loader build
fabric/                    Fabric build (Loom), a complete Gradle project with its own wrapper
  src/main/java/navrelis/glintsmith/fabric/   GlintsmithFabric, FabricPlatform, ModMenuIntegration
  src/main/resources/                         fabric.mod.json, META-INF/services/navrelis.glintsmith.platform.Platform
  src/test/java                               the tests that need Minecraft or Fabric Loader (bootstrap, mixin application)
  src/test/resources/glintsmith/owner-defaults.json
  src/studio/                                 the dev-only studio (docs/STUDIO.md), never packaged
neoforge/, forge/          later: the same shape as fabric/
build-all.ps1              builds every loader folder that exists, stops at the first failure, prints the jars
docs/ARCHITECTURE.md       this file
```

Each loader folder compiles `common/` straight into its own source sets
(`java.srcDir "${commonDir}/src/main/java"`, `resources.srcDir "${commonDir}/src/main/resources"`, and
`test.java.srcDir "${commonDir}/src/test/java"`) next to its own sources, and produces one jar. There is no `common`
Gradle module and no published artifact: `common/` is a shared source tree. Jar name:
`glintsmith-<mod_version>+<minecraft_version>-<loader>.jar` in `<loader>/build/libs`. Build everything with
`./build-all.ps1`, or one loader with `cd fabric && ./gradlew build`.

Rules of `common/`:

* It may use Minecraft (Mojang names), Brigadier, Gson, SLF4J and the Mixin API, but never `net.fabricmc`,
  `com.terraformersmc`, `net.neoforged` or `net.minecraftforge`. The `checkCommonIsLoaderNeutral` task of every loader
  build (part of `check`) fails on such an import.
* Mixin classes, mixin configs and shaders are shared. Each loader build lists the three mixin configs in its own
  mod metadata (`fabric.mod.json` here).
* Tests in `common/src/test/java` must not touch Minecraft registries, a game bootstrap or `Services.PLATFORM`.

## Platform service

`navrelis.glintsmith.platform.Platform` is the one thing a loader has to implement for `common/`:

```java
public interface Platform {
	String loaderName();                  // "fabric", "neoforge", "forge"
	Path configDir();                     // the folder of glintsmith.json
	boolean isModLoaded(String modId);    // "modelfix", "iris"
}
```

`Services.PLATFORM` loads it once with `ServiceLoader.load(Platform.class, Services.class.getClassLoader())` and fails
with a clear message if the loader build did not register an implementation. A loader build registers it in
`<loader>/src/main/resources/META-INF/services/navrelis.glintsmith.platform.Platform` (one line: the class name). Users:
`ConfigManager` (config folder), `GlintsmithClient` (gap fix and the `modelfix` mod), `IrisCompat`
(`isModLoaded("iris")`), `GlintsmithMixinPlugin` (`isModLoaded("modelfix")`; its decision `shouldApply(String, boolean)`
stays a pure function).

## Hook methods (`navrelis.glintsmith.GlintsmithClient`)

The loader layer calls these static methods; none of them has a loader type in its signature. `Glintsmith` holds only
`MOD_ID` and `LOGGER`.

| Method | When the loader calls it |
| --- | --- |
| `init()` | once, from the client entry point, before the registrations below. Loads the config, creates the key mappings, starts the outline and glint systems, applies the gap fix. Every part is guarded. |
| `onClientTickEnd(Minecraft)` | end of every client tick (config live reload, pending gap fix reload, key bindings) |
| `onClientStarted(Minecraft)` | the client has started (first resource load done): outline system, then glint system, then the gap fix flag |
| `onClientStopping(Minecraft)` | the client is stopping |
| `onTagsLoaded()` | tags (re)loaded: `RuleEngine.invalidate()` |
| `onWorldRenderStart(MultiBufferSource consumers)` | start of the world render pass; `consumers` is the buffer source the world is drawn into (may be null) |
| `onWorldRenderLast()` | end of the world render pass, after the world is drawn and before the hand and GUI |
| `reloadListeners()` | returns `List<GlintsmithClient.NamedReloadListener>` (`record NamedReloadListener(ResourceLocation id, ResourceManagerReloadListener listener)`): `glintsmith:outline_shaders` first, then `glintsmith:glint_textures`; register both as client resource reload listeners, in that order, without dependencies |

Other pieces a loader wires up:

* **Key bindings:** `GlintsmithKeys.mappings()` returns the two `KeyMapping`s (open config, toggle outlines; unbound
  by default, category `key.categories.glintsmith`). Register each with the loader's key binding registry. The tick
  handler is `GlintsmithClient.onClientTickEnd`.
* **Client command:** `GlintsmithCommands.<S>build()` returns a Brigadier `LiteralArgumentBuilder<S>` for
  `/glintsmith` (`reload`, `toggle`). It never reads the source; feedback goes to the player's chat through
  `Minecraft.getInstance().player.displayClientMessage`. Register it with the loader's client command dispatcher.
* **Config screen:** the constructor `new GlintsmithScreen(Screen parent)` is the factory (Mod Menu on Fabric, the
  config screen extension point on NeoForge and Forge).
* **Mixins:** `glintsmith.mixins.json`, `glintsmith.glint.mixins.json` and `glintsmith.outline.mixins.json` are in
  `common/src/main/resources`. The first one names the plugin `navrelis.glintsmith.mixin.GlintsmithMixinPlugin`, which
  runs very early and uses `Services.PLATFORM`, so the service file must be loadable then.

## Fabric layer

`GlintsmithFabric` (`ClientModInitializer`) calls `GlintsmithClient.init()` and then registers: a key binding per
mapping (`KeyBindingHelper`), the command (`ClientCommandRegistrationCallback`, `build()` as
`FabricClientCommandSource`), `END_CLIENT_TICK`, `CLIENT_STARTED`, `CLIENT_STOPPING`, `CommonLifecycleEvents.TAGS_LOADED`,
`WorldRenderEvents.START` (passes `context.consumers()`) and `LAST`, and each reload listener wrapped in a
`SimpleSynchronousResourceReloadListener` whose Fabric id is the listener id. `FabricPlatform` is the `Platform`
service. `ModMenuIntegration` returns `GlintsmithScreen::new`. `fabric.mod.json` takes `version` (the mod version) and
`minecraft_range` (`fabric_minecraft_range` of `shared.properties`) from the build.

## Adding a loader

1. Create `<loader>/` as a standalone Gradle project with its own wrapper, `settings.gradle` that loads
   `../gradle/shared.properties` (copy `fabric/settings.gradle`), and a `build.gradle` that adds the `common/` source
   directories as shown above plus the `checkCommonIsLoaderNeutral` task.
2. Implement `Platform` and register it under `META-INF/services`.
3. Write the entry point: call `GlintsmithClient.init()`, register the key mappings, the command, the reload listeners
   and forward the events of the table above.
4. Write the loader's mod metadata (mod id `glintsmith`, the three mixin configs, the config screen factory) and name
   the jar `glintsmith-<mod_version>+<minecraft_version>-<loader>`.
5. `build-all.ps1` finds the folder by itself (it looks for `settings.gradle` and `gradlew` in a top-level folder).

## Tests

Pure tests (`common/src/test/java`) run in every loader build. Tests that need a Minecraft bootstrap, Fabric Loader or
the mixin environment (`GameBootstrap`, `*MixinApplicationTest`, `SideElementBakeTest`, ...) are loader specific and stay
in `<loader>/src/test/java`, as does `DefaultConfigTest` with its `owner-defaults.json`. `LangFilesTest` reads the lang
files from the classpath and scans `common/src/main/java/navrelis/glintsmith/gui` (found by walking up from the test
working directory).

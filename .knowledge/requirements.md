# Requirements (Glintsmith, session 2026-10-17)

## Owner task (2026-10-17)
Build one new Fabric 1.21.1 mod in a new folder in `F:\Coding` with a name that can become popular. It must customize item outlines, the enchantment glint (texture and colours) and fix gaps in item models. Study three sources, take what is allowed, otherwise write our own code. Better than the originals in every aspect: more versatile, optimised, without their bugs.

Sources studied (reference only, clones live in the session scratchpad, never in the repo):
- MoubaEZPZ/custom-glow-enchantment-outline, branch `mc1.21.1/dev`: All Rights Reserved. No code reuse.
- MehVahdJukaar/modelfix-multi, branch `1.21`: GPL-3.0. Technique reimplemented, no code copied.
- "Rainbow Enchantment Glint v1.1" resource pack by TheEscaper: no licence, one 128x128 texture. Not shipped; we generate our own textures.
- tixul "Enchantment Outlines" resource pack (CurseForge): CIT model overrides for vanilla tools only. Shows the limit we must beat.

## Owner answers (2026-10-17)
- **Universal:** must work with all mods and all items, not fixed to specific items or tools. Default target: any enchanted item, from any mod, any model type (flat, 3D, custom renderer).
- The owner replied to the question list without changes, so the lead's defaults stand:
  - Name **Glintsmith**, folder `F:\Coding\Glintsmith`, mod id `glintsmith`, package `navrelis.glintsmith`.
  - Licence MIT, all code our own.
  - Scope v1.0.0:
    - outlines on held items (first and third person), worn armor, dropped items, item frames, optional GUI items
    - rules by item id, tag, enchantment or mod, with allow and deny lists
    - per rule: colour, thickness and animation (static, rainbow, pulse, gradient, per enchantment), low-durability alert, only-me or everyone
    - glint: texture presets, own textures, tint, speed, scale, strength, per enchantment and per rule colour, separate settings for items, armor, entities and GUI, each can be turned off
    - gap fix: on/off, safe automatic default, advanced tuning, no FPS cost on HD packs, macOS bleeding handled
  - Must never crash with Sodium or Iris. Outlines work with shaders off; with Iris shaders on they work where possible or turn off cleanly.
  - Hard dependencies: Fabric Loader and Fabric API only. Own config screen on vanilla widgets, optional Mod Menu entry, keybind, live config reload. Nothing bundled.
  - Stack: Fabric Loom, official Mojang mappings, Java 21, Minecraft 1.21.1 only, client-side only, Fabric Loader 0.19.5, Fabric API 0.116.17+1.21.1 (same as the Nytheria pack).
  - Languages: English and German.
  - GitHub repo `navrelis/Glintsmith`, default branch `main`, work on `dev`. Commits only as Navrelis (noreply). Lead change: created **private**; owner is asked before it goes public.
  - In-game test: lead loads the jar into the Nytheria client with `--add-mod` under the game lock, in a throwaway `PerfTest-*` world, with Sodium and Iris. Owner saves and pack `mods` untouched.
  - No upload; deliver jar plus mod page summary and description.
  - Graphify: build once at the end.

## Environment facts (verified 2026-10-17)
- Java 21.0.10 (Microsoft), gh logged in as `navrelis`, Gradle only via wrapper.
- Template project with the same stack: `F:\Coding\DynamicLighting` (Loom plugin `net.fabricmc.fabric-loom-remap`, Gradle 9.5.0, JUnit with fabric-loader-junit).
- Nytheria pack: Sodium 0.8.13, Iris 1.8.14-beta.1, Mod Menu 11.0.5.

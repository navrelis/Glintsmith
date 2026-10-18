# Requirements (Glintsmith, session 2026-10-17)

## Owner task (2026-10-17)
Build one new Fabric 1.21.1 mod in a new folder in the owner's projects folder with a name that can become popular. It must customize item outlines, the enchantment glint (texture and colours) and fix gaps in item models. Study three sources, take what is allowed, otherwise write our own code. Better than the originals in every aspect: more versatile, optimised, without their bugs.

Sources studied (reference only, clones live in the session scratchpad, never in the repo):
- MoubaEZPZ/custom-glow-enchantment-outline, branch `mc1.21.1/dev`: All Rights Reserved. No code reuse.
- MehVahdJukaar/modelfix-multi, branch `1.21`: GPL-3.0. Technique reimplemented, no code copied.
- "Rainbow Enchantment Glint v1.1" resource pack by TheEscaper: no licence, one 128x128 texture. Not shipped; we generate our own textures.
- tixul "Enchantment Outlines" resource pack (CurseForge): CIT model overrides for vanilla tools only. Shows the limit we must beat.

## Owner answers (2026-10-17)
- **Universal:** must work with all mods and all items, not fixed to specific items or tools. Default target: any enchanted item, from any mod, any model type (flat, 3D, custom renderer).
- The owner replied to the question list without changes, so the lead's defaults stand:
  - Name **Glintsmith**, folder `Glintsmith` next to the other projects, mod id `glintsmith`, package `navrelis.glintsmith`.
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
  - GitHub repo `navrelis/Glintsmith`, default branch `main`, work on `dev`. Commits only as Navrelis (noreply). Public repo confirmed by the owner in chat on 2026-10-17.
  - In-game test: lead loads the jar into the Nytheria client with `--add-mod` under the game lock, in a throwaway `PerfTest-*` world, with Sodium and Iris. Owner saves and pack `mods` untouched.
  - No upload; deliver jar plus mod page summary and description.
  - Graphify: build once at the end.

## Later owner additions (chat, 2026-10-17)
- Wants description, summary, pixel-art logo or rendered sword with glint and outline as a GIF cycling looks, showcase images like Lootr Remastered / Obscura's Simple Storage, and a statement how much better the mod is.
- More parallel subagents allowed (up to 4 Sonnet + 2 Opus, disjoint files).
- Original glow-outline jar may only be downloaded for a benchmark after the owner's OK (not asked yet).

## Environment facts (verified 2026-10-17)
- Java 21.0.10 (Microsoft), gh logged in as `navrelis`, Gradle only via wrapper.
- Template project with the same stack: the sibling project DynamicLighting (Loom plugin `net.fabricmc.fabric-loom-remap`, Gradle 9.5.0, JUnit with fabric-loader-junit).
- Nytheria pack: Sodium 0.8.13, Iris 1.8.14-beta.1, Mod Menu 11.0.5.

- 2026-10-17 (session 8) Owner: logo and image showcase on a white / cream-white background (in-game backdrop for the hero/logo/showcase captures, light card panels), not black.

## Session 12 owner additions (2026-10-18)
- The GIFs (showcase, animated logo) are "very bad": flat front view, 4 fps, dull light, floor seam. Wanted: Blockbench/Blender-like turntables with controlled light and camera.
- Owner choices: studio mode inside the mod (dev-only source set, never in the released jar, real Glintsmith render pipeline); content = hero sword turntable, item lineup turntable, armor stand turntable, and redo the still cards from studio renders; captures run in the Loom dev client (own run folder, may run while the owner's game is open); GIF encoder stays our own standard-library code (better palettes/dither, about 8 MB per GIF).
- CurseForge: owner creates the project from build/curseforge-package and sends the id; release type Release. Upload after the new art is in.
- 2026-10-18 Owner asked for a 1.1.0 with outline "see-through" control, then dropped it after hearing that the Opacity slider (Outline tab and per rule) already exists. No 1.1.0 planned.
- 2026-10-18 Owner: new version 2.0.0 with a live preview window in the settings screen: the item in the hand rendered with the current settings, updating live while settings change. Release order: 1.0.0 upload once the CurseForge project exists, then 2.0.0 when finished.
- 2026-10-18 Owner: CurseForge project id 1751215.

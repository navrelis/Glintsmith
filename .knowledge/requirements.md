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
- 2026-10-18 Owner: outline thickness controllable per place; his current config (rainbow outline 8.5 px opacity 0.25, frost item glint with rainbow tint, armor glint layer off, fire entity/GUI) becomes the built-in default for now.
- 2026-10-18 s14 (owner, before sleeping): lead works to the end without questions: test glint and everything in game himself (isolated instance copy, never the owner's instance), then upload v2.0.0 to CurseForge (project 1751215) when done. Do not contact the owner.

## Owner answers (2026-10-18, 2.0.1 follow-up)
- Merge the lineup stray-dot fix (B1) into dev and push dev.
- Implement the gap-fix edge guard (B2) for 2.0.1.
- After both: re-render the lineup, rebuild the lineup GIF and cards, commit to dev if 0 stray frames.
- No 2.0.1 CurseForge release yet; main untouched.
- Coordinate with all other Glintsmith sessions so nothing collides.
- 2026-10-18 (later): owner: release 2.0.1 now: bump the version, in-game check, merge to main, tag, upload to CurseForge. Also: delete unneeded branches/worktrees (owner ran the cleanup commands himself).

## Session 21 owner task (2026-10-19): page images + multi-version port
Owner: "you can't really see the difference in the pictures in curseforge" and "port this to all versions from 26.3 down to 1.18.x for Fabric, NeoForge and Forge; use worktrees and many subagents to finish it fast". Owner accepted every lead default ("Use all your defaults. Go ahead and finish it, including the push and the CurseForge upload."):
1. Images: showcase/armor GIFs cycle several vivid looks again (full-opacity outlines in several colours, fire/frost/rainbow etc. glints), banner vivid; card_default_look gets a ~3x zoomed close-up so the faint default outline shows. Mod defaults unchanged. Upload to CurseForge 1751215 + relink description.
2. Versions: one jar per family, every release 1.18 to 26.3 covered: 1.18-1.18.1, 1.18.2, 1.19-1.19.2, 1.19.3, 1.19.4, 1.20-1.20.1, 1.20.2, 1.20.3-4, 1.20.5-6, 1.21-1.21.1, 1.21.2-3, 1.21.4, 1.21.5, 1.21.6-8, 1.21.9-10, 1.21.11, 26.1.x, 26.2, 26.3 (19 families).
3. Loaders: Fabric everywhere; Forge wherever Forge exists; NeoForge from 1.20.2 (Forge 1.20.1 jar also marked NeoForge 1.20.1).
4. Layout like the owner's multi-version projects (Sophisticated Building / Unified Xaero Worldmap): branch mc/<version> per family, worktrees under versions/<mc>, loader-neutral shared code; main becomes the hub at release; main/dev stay 2.1.0 until then.
5. Build-only tools allowed (Stonecutter/Architectury Loom/FG/MDG as in those projects); no new runtime deps; Fabric API required on Fabric only.
6. Full feature parity on every jar; Forge/NeoForge config screen from the Mods list; Sodium/Iris compat where they exist; Embeddium/Oculus no crash + world outlines off under shader packs; anything impossible hidden + documented.
7. Version 2.2.0 everywhere, jars glintsmith-2.2.0+<mc>-<loader>.jar; 1.21.1 Fabric behaves like 2.1.0.
8. Testing: build + unit tests per branch; every jar launched automatically in its own dev client (never the owner's instances), test world, screenshots of enchanted sword in hand / inventory / dropped / item frame, lead views each set; Sodium/Iris/Embeddium launch checks on one jar per era; studio stays on 1.21.1 only.
9. Release pre-approved: push all branches to navrelis/Glintsmith, upload all jars to CurseForge 1751215 (versions/loaders tags, Fabric API relation), update description version table/summary/comparison row. No Modrinth.
10. Parallelism: up to 4 Sonnet + 2 Opus, each in its own worktree.

## Resource rules (owner, relayed after the 11:55Z reboot on 2026-10-19; apply to the lead, every session and every subagent)
1. Before any Gradle build, test run or Minecraft client: count Java processes (`(Get-Process java,javaw -ErrorAction SilentlyContinue).Count`); if 8 or more, wait 60 s and re-check; start only below 8.
2. At most ONE Gradle invocation and ONE game client at a time per session (subagents included); loaders built and smoked in sequence.
3. At most 2 subagents alive at once per session; a subagent running Gradle follows rule 1.
4. After a build batch in a worktree: `./gradlew --stop` there; kill smoke/game clients as soon as their screenshots are taken.
5. After a restart nothing is assumed running; re-verify on disk.

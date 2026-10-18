# Glintsmith Studio (dev-only render mode)

The studio renders the mod page GIFs and stills as Blockbench/Blender-style turntables: a fixed camera and light, the
subject rotating on a turntable, every frame over pure black and over pure white so a consumer can pull an exact matte.
The glint, the outline and the gap fix in those frames are the real Glintsmith pipeline, driven through the same hooks
the game uses. Nothing is imitated.

It is a separate Gradle source set and a separate mod (`glintsmith_studio`). The released jar
`build/libs/glintsmith-2.1.0.jar` is built from `sourceSets.main` only and contains no studio class or resource.

## Running a scene

```
./gradlew runStudio -Pscene=hero_sword
./gradlew runStudio -Pscene=src/studio/resources/scenes/lineup.json
./gradlew runStudio -Pscene=C:/path/to/my_scene.json
./gradlew runStudio -Pscene=hero_sword -PstudioOut=C:/somewhere/else   # output base folder override
```

* `-Pscene` is a JSON file (relative to the repository root or absolute) or the name of a scene in
  `src/studio/resources/scenes/` (`hero_sword`, `lineup`, `armor_stand`, `stills_hero`, `stills_wide`, `stills_tiles`, `stills_gap`, `logo_concepts`, `logo_anim_a`). Gradle passes it to the game as
  `-Dglintsmith.studio.scene=...`. Without it the studio mod stays idle and the dev client is a normal client.
* The game runs in `run-studio/` (git-ignored) with its own options, config and saves. It never touches a player's game
  directory, and the in-memory looks never write `run-studio/config/glintsmith.json`.
* The process exits by itself: exit code 0 after a successful run, 1 after a failure (one log line starting with
  `[Glintsmith Studio] FAILED:` says why), 3 if the whole run exceeds `-Dglintsmith.studio.timeout` seconds
  (default 1800; a watchdog halts the JVM).
* Modded items: every `*.jar` in `studio-mods/` (git-ignored, may be empty or missing) is added to the dev client with
  `-Dfabric.addMods=...`, so a scene can use items of other mods without a build dependency. Fabric Loader remaps them
  to the dev mappings on the fly.

Output goes to `art/captures/studio/<scene>/` (git-ignored through `art/captures/*`), see the frame contract below.

## What happens during a run

1. **Bootstrap.** On the title screen (or the first-launch accessibility screen) the studio creates the world
   `run-studio/saves/glintsmith_studio` through the normal "create world" path: void superflat, creative, peaceful,
   commands on, no daylight or weather cycle, no mob spawning. Every run creates it again over the same folder (that
   only rewrites its level data; opening an existing world can stop at a confirmation screen when the set of mod data
   packs changed). A loaded world is needed because enchantments are data driven in 1.21.1. Options are set in memory:
   HUD hidden, Fancy graphics (Fabulous would send items into a separate target), render distance 2, no frame limit,
   no vsync, sound off, no pause on lost focus.
2. **StudioScreen.** After the world has settled for one second, a full-screen pause screen opens and renders one
   studio frame per game frame. When it is done it writes `scene.json`, restores the render target and calls
   `Minecraft.stop()`; the integrated server saves and the process exits.

### Render target and supersampling

The game's main render target is resized to `width × supersample` by `height × supersample` (the window keeps its
size and shows a scaled preview). The frames are written at that size; the consumer downsamples. Rendering into the
main target, not a private one, is deliberate: the outline compositor always composites onto the main target, so the
real compositor runs unchanged.

### How the real glint and outline reach the subjects

Everything is drawn with the main buffer source (`Minecraft.renderBuffers().bufferSource()`), through
`ItemRenderer.renderStatic` (items) and `EntityRenderDispatcher.render` (the armor stand). That passes every Glintsmith
hook the game passes:

* **Glint.** `ItemRenderer.render` and `HumanoidArmorLayer.renderArmorPiece` push the glint context, and the main
  source swaps the vanilla glint render types for Glintsmith's glint slots (custom texture, tint, speed, scale,
  strength) exactly as in game.
* **Outline, GUI display context (default for items).** Each item is wrapped in
  `OutlineSystem.beginGuiItem()` / `endGuiItem()` — the same calls Glintsmith wraps around vanilla
  `GuiGraphics.renderItem`. The item's geometry is teed into the GUI mask, flushed, and composited right after: the GUI
  outline target. In 1.21.1 a flat (`item/generated`) item drawn in the GUI context is the same extruded mesh as in
  the world (front, back and 1/16 thick sides), only viewed head-on in the inventory; on the turntable its thickness
  shows. The studio does not use the inventory's flat lighting: the studio lights apply.
* **Outline, every other case.** Armor stands, and items with another display context (`fixed`, `ground`,
  `third_person_right_hand`, `head`, `none`, ...), are drawn inside the outline system's WORLD phase, the phase the
  level render opens at `WorldRenderEvents.START`. Armor goes through the armor hook (target `armor`), held items
  through the held-item hook (target `third_person`). The phase is opened and composited (mask, depth copy, EDT,
  composite) through `PipelineBridge`, which reaches the private `OutlineSystem.open` / `compositeWorld` by reflection
  (dev-only code; the shipped classes are unchanged).
* **Looks.** A look is applied by making its config the current config of `ConfigManager.instance()` in memory and
  running the config listeners (again by reflection: the private `current` field and `notifyListeners`). The outline
  and glint runtime settings, the rule cache, the glint textures and the gap fix react exactly as to a config load.
  The file is never written.
* **Gap fix.** A look that changes the gap fix makes Glintsmith reload the resources (the item models are baked with
  it), as in game. The studio waits for that reload (counted by a reload listener that runs after model baking) and a
  few more frames before it renders the look.

### Studio clock

While the studio runs, the animation time of the pipeline comes from a deterministic clock instead of
`Util.getMillis()`: `time = time_offset + (frame index - 1) / fps` seconds. Studio mixins redirect exactly the reads
that drive animation, on the render thread only:

| Read | Drives |
|---|---|
| `GlintSystem.onFrameBegin` | animated glint tints (frame time) |
| `GlintShards.setupTexturing` / `vanillaTexturing` | scroll of Glintsmith's glint slots |
| `RenderStateShard.setupGlintTexturing` | scroll of vanilla glints |
| `OutlineSystem.onFrameBegin` / `time()` | animated outline colours (rainbow, pulse, gradient, enchantment) |

Ticks, the loading overlay and the integrated server keep the real clock, so the game behaves normally while the
studio waits for it. The time of the next frame is set at the end of the previous game frame, so the frame-start hooks
read it. Frame `n` and `n+1` are therefore exactly `1/fps` apart in every animation. One turntable revolution per look
(`turns_per_look`, default 1) over `frames_per_look` frames, with the angle `360 × turns × f / frames_per_look` for
`f = 0 .. frames_per_look-1`, so every look loops cleanly in rotation. With `swing` set, the turn is replaced by a
yaw swing: angle `swing × sin(360° × turns × f / frames_per_look)`, i.e. `±swing` degrees around the front view,
which also loops cleanly. Use it for flat items (a flat item is edge-on, a sliver, at 90°). The glint scroll and colour animations do not
loop with the turntable (their periods are unrelated); a GIF that must loop seamlessly in those too needs a matching
duration.

`time_offset` exists because at time 0 the vanilla armor glint (scale 0.16 of the glint texture) happens to sample a
dark part of the texture and is nearly invisible for the first second; the example scenes start at 5 s.

### Camera and light

* The turntable rotates the subject around the vertical axis; the camera stays where it is (pitch, yaw, roll, distance
  and target from the scene). Positions are in blocks.
* **Orthographic** (default): `view_height / zoom` blocks of the scene are visible vertically. Its depth range is
  chosen so that window depth and depth slope at the camera target match the vanilla perspective projection (near
  0.05, far `GameRenderer.getDepthFar()`) that the outline compositor uses to turn depth into blocks; the WORLD-phase
  depth tolerances (0.15 blocks etc.) then mean what they mean in game, around the subject. Keep `distance` larger than
  the subject's radius: geometry farther than about `2 × distance` from the camera is clipped.
* **Perspective**: vertical `fov` (degrees, divided by `zoom`), near 0.05, far `getDepthFar()` (the exact vanilla
  terms, so the outline depth rules are exact).
* **Lights**: `key` and `fill` are directions *towards* the light in view space (x right, y up, z towards the camera).
  They go to `RenderSystem.setShaderLights`, so the vanilla entity/item shaders shade with them; ambient light stays
  the vanilla shader's (0.4 ambient, 0.6 per light; the shaders normalise the directions, so there is no intensity).
  Fog is off, packed light is full bright.

### Black/white pairs

Each studio frame is drawn twice in the same game frame with identical state and clock: once after clearing colour to
(0,0,0) and once to (255,255,255), depth cleared both times. No sky, fog, vignette, HUD, crosshair or hand: the studio
clears the target itself, after the level render. The matte is `a = 1 - (W - K) / 255` per channel.

Additive effects and the matte: the glint is additive (`GLINT_TRANSPARENCY`, src colour + dst) but drawn with an
`EQUAL` depth test, so it only lands on the item's own opaque pixels, never on the background; over black and over
white it adds to the same item pixel. The outline is composited with ordinary alpha blending
(`SRC_ALPHA, ONE_MINUS_SRC_ALPHA`), soft glows included, which is exactly what the black/white matte recovers. Item
textures with partial alpha (translucent items) are blended normally too. Nothing in the current pipeline breaks the
matte.

## Frame contract (output)

Folder `art/captures/studio/<scene>/` (or `<-PstudioOut>/<scene>/`, or the scene's `output`). Existing files with the
same names are overwritten; nothing is ever deleted.

* Per frame two PNGs: `NNNN_k.png` (over black) and `NNNN_w.png` (over white). `NNNN` is the 1-based frame index,
  zero-padded to 4 digits. 8-bit RGBA, alpha always 255 (ignore it). Size `width × supersample` by
  `height × supersample`.
* `scene.json`:

```json
{
  "scene": "hero_sword",
  "width": 512,
  "height": 512,
  "supersample": 2,
  "fps": 30,
  "frames": [
    { "index": 1, "file": "0001", "look": "vanilla", "caption": "Vanilla", "name": null }
  ],
  "looks": [ { "id": "vanilla", "caption": "Vanilla" } ],
  "commit": "69f7c98",
  "created": "2026-10-18T01:40:48+02:00"
}
```

`width` / `height` are the output size after downsampling by `supersample`. `name` is the shot name for stills and
`null` for turntables. `looks` lists the looks used by the frames, in order of first use. `commit` is
`git rev-parse --short HEAD` of the repository, or `unknown`.

## Scene format (input)

Scenes live in `src/studio/resources/scenes/` (also loadable by name) or anywhere on disk. Full example with every
key (values shown are the defaults where a key is optional):

```json
{
  "scene": "my_scene",                 // required; [a-z0-9_.-]+, names the output folder
  "type": "turntable",                 // or "still"
  "width": 512, "height": 512,         // output size (after downsampling)
  "supersample": 2,                    // 1..8; render size = width*supersample x height*supersample
  "fps": 30,
  "seconds_per_look": 3.0,             // or "frames_per_look": 90 (wins if both are given)
  "turns_per_look": 1,                 // turntable revolutions per look (may be fractional or negative)
  "swing": 0,                          // degrees; if not 0 the turntable swings +-swing degrees (sine) instead of turning
  "time_offset": 0.0,                  // seconds added to the studio clock
  "settle_frames": 3,                  // game frames between a look change and its first studio frame
  "output": null,                      // optional output folder (absolute, or relative to the repo); default art/captures/studio/<scene>
  "camera": {
    "projection": "orthographic",      // or "perspective"
    "pitch": 25,                       // degrees, positive looks down on the subject
    "yaw": 0, "roll": 0,               // degrees
    "distance": 4,                     // blocks from the camera to the target
    "fov": 30,                         // perspective only, vertical degrees
    "view_height": 1.4,                // orthographic only, blocks visible vertically
    "zoom": 1.0,                       // divides view_height (ortho) or narrows the fov (perspective)
    "target": [0, 0, 0]
  },
  "lights": {
    "key":  [-0.45, 0.75, 0.6],        // view-space direction towards the light
    "fill": [0.65, 0.2, 0.45]
  },
  "layout": { "type": "row", "spacing": 1.25, "origin": [0, 0, 0] },   // "row": subjects without "pos" are spread along x
  "subjects": [
    {
      "type": "item",                  // or "armor_stand"
      "item": {                        // or a string in /give syntax, e.g. "minecraft:diamond_sword[minecraft:enchantment_glint_override=true]"
        "item": "minecraft:netherite_sword",
        "enchantments": { "minecraft:sharpness": 5 },
        "glint": true,                 // optional enchantment_glint_override
        "count": 1
      },
      "context": "gui",                // item display context; gui = GUI outline phase, anything else = WORLD phase
      "pos": [0, 0, 0],                // centre of the item (an armor stand is centred at its mid height)
      "rotation": [0, 0, 45],          // fixed orientation in degrees, applied before the turntable (Y, then X, then Z)
      "scale": 1.0,                    // a GUI-context item is 1 block (16 px) wide at scale 1
      "pivot": [0, 0, 0],              // point of the subject on the turntable axis (own rotated, unscaled coordinates); recentres off-centre models such as the shield, whose centre is at [-0.08, 0, -0.48]
      "phase": 0,                      // turntable phase offset in degrees
      "spin": true,                    // false keeps the subject still
      "look": null                     // a look id to use for this subject instead of the frame's look
    },
    {
      "type": "armor_stand",
      "nbt": "{ShowArms:1b,NoBasePlate:1b,Pose:{RightArm:[-15f,0f,10f]}}",   // merged over {ShowArms:1b}
      "equipment": {                   // head, chest, legs, feet, mainhand, offhand; item specs as above
        "head": { "item": "minecraft:netherite_helmet", "enchantments": { "minecraft:protection": 4 } },
        "mainhand": "minecraft:netherite_sword"
      }
    }
  ],
  "sequence": ["vanilla", "rainbow"],  // look ids rendered in order (turntable); default: all looks in order
  "looks": [
    {
      "id": "vanilla", "caption": "Vanilla",
      "vanilla": true                  // outline off, glint vanilla, gap fix off
    },
    {
      "id": "rainbow", "caption": "Rainbow glint + rainbow outline",
      "outline": {                     // merged into config.outline (glintsmith.json format)
        "enabled": true,
        "color": { "mode": "rainbow", "speed": 1.0 },   // or "#B05CFF"; modes: static, rainbow, pulse, gradient, enchantment
        "thickness": 3,                // output pixels; multiplied by supersample (then clamped to 16)
        "softness": 0.0, "opacity": 1.0,
        "targets": { "gui": true }     // all targets are on by default in the studio
      },
      "glint": {                       // merged into all four glint layers (item, armor, entity, gui)
        "texture": "preset:rainbow",   // vanilla, preset:<neutral|rainbow|aurora|fire|frost|sparkle|gold|void|inferno|nebula|starlight|opal|storm>, file:x.png, ns:path.png
        "tint": null, "speed": 1.0, "scale": 1.0, "strength": 1.0, "rotation": 10.0   // scale 0.1..10, see "Glint scale" below
      },
      "glint_layers": { "armor": { "texture": "preset:void" } },   // per layer, after "glint"
      "gap_fix": "auto",               // "auto", "off", or a config.gapFix object
      "rules": [],                     // replaces config.rules (studio base has no rules)
      "config": {}                     // any partial glintsmith.json, deep-merged last
    }
  ],
  "shots": [                           // "type": "still" only: one frame per shot
    { "name": "hero_front", "look": "rainbow", "yaw": 30, "pitch": 20, "time": 1.0,
      "camera": { "projection": "perspective" }, "subjects": [ ... ] }
  ]
}
```

(The JSON parser does not accept the comments shown above; they are documentation only.)

Look base config: an explicit vanilla-like baseline that does not follow the built-in defaults of the mod (those change
between versions, and the renders must not): outline on for enchanted items, static `#D088FC`, thickness 2, opacity 1,
softness 0, `proportional` off (`minThickness` 1), so the thickness is in output pixels and only the studio's supersample
factor scales it, never the window height (the mod's default `outline.proportional` would measure it on the first-person item and scale the
others with their on-screen size), all four glint layers at the vanilla values (texture none, no tint, speed / scale / strength 1, rotation 10),
gap fix auto, no durability alert, no enchantment colours. On top of it every outline target is on (first person, third person, armor, dropped,
item frame, GUI, other), "only local player" off, resolution scaling off (the studio scales thickness by the
supersample itself), distance fade off, and no rules. Merge order: `vanilla`, `outline`, `glint`, `glint_layers`,
`gap_fix`, `rules`, `config`. A colour may be a plain hex string; enum values (`mode`, `condition`) may be lower case.
An invalid look (unknown texture, bad rule pattern) fails the run before the first frame.

Per-subject looks (`"look"` on a subject) are applied right before that subject is drawn and the frame's look right
after it, within the same frame; their gap fix is always the frame look's (a gap fix change cannot happen in the middle
of a frame). `lineup.json` uses this: one frame look, six subject looks.

Stills: `"type": "still"` renders one frame per entry of `shots`. A shot sets the look, the turntable angle (`yaw`),
optionally the camera `pitch`, a partial `camera` override, its own `subjects`, and the clock `time` (seconds, plus
`time_offset`; default `(index-1)/fps + time_offset`). The shot `name` goes into `scene.json` (default `shot_NNNN`).

### Glint scale

The glint texture coordinates follow the model's UVs, not its size in the world, and an item sprite covers only a small
part of the block atlas, so at the default `scale` 1 one 16 px item shows a small window of the glint texture, which
reads as one flat colour (measured: the pattern changes with the scale, not with the item's size on screen). A smaller `scale` (the matrix is `base / scale`) puts more of the texture on the item: the scenes use 0.1 to 0.15
for items and 0.1 for armor (the armor glint is sampled over the whole armor model texture and has a much smaller base
scale; 0.25 and above gave blotches the size of a whole armor piece). 0.1 is the minimum the config allows.

### Gap-fix stills

The hairline cracks the gap fix closes are one render pixel wide whatever the zoom, so `stills_gap` renders at
supersample 1 (a downsample would halve their contrast) and uses oblique perspective views (yaw 30, pitch 45) of enchanted items: the cracks are a few dozen single pixels along the edges between the faces, where the background shows through. They only read on a card as a nearest-neighbour detail crop (`crop` + `up`), and the clock `time` is the same in every shot so the glint is identical in the off and on pair. A gap-fix
change reloads the resources, so the shots are ordered by look: both `off` shots, then both `on` shots. The two shots of
a pair use the same view, so the pair can be shown as before / after (or flipped on one spot).

## Example scenes

| Scene | Content |
|---|---|
| `hero_sword` | Netherite sword upright, 3/4 pitch, yaw swing of +-55 degrees (the sword never goes edge-on), six looks (vanilla, static purple outline, rainbow glint + rainbow outline, fire glint + pulse outline, nebula glint + gradient outline, inferno glint + soft glow), glint scale 0.12. 512x512, supersample 2, 30 fps, 2.5 s per look: 450 frames. |
| `lineup` | Netherite sword, diamond pickaxe, trident, shield (pivoted), netherite chestplate (item) and bow in a row (spacing 0.95, view height 2.0, items about 70 % of the frame height), each with its own look (rainbow, inferno, storm, opal, starlight, void), +-35 degree yaw swing. 1200x400, one swing over 4 s: 120 frames. |
| `armor_stand` | Armor stand in full enchanted netherite armor holding the sword, armor outline target on, armor glint scale 0.1, view height 3.4 (the stand is about 60 % of the height, the sword stays inside the frame all round). 480x600, four looks of 4 s (vanilla, armor outline, rainbow, inferno glint + soft glow): 480 frames. |
| `stills_hero` | 1024x1024 stills: `banner_hero` (rainbow glint + rainbow outline, 3/4 view) and `logo` (aurora glint + purple outline, front view; the old logo, no longer used). |
| `logo_concepts` | 512x512 stills, outline thickness 8 (16 render pixels, the maximum) and the custom gap fix (UV shrink 1.0) in every look: `concept_a`, the logo (golden sword on the diagonal, yaw 20, pitch 8, fire glint scale 0.12, violet outline `#9B4DFF`); `concept_b` (crossed golden and diamond swords, fire glint + orange outline and frost glint + blue outline, the front sword turned 90 degrees in Z instead of mirrored so no back face shows) and `concept_c` (diamond sword with rainbow glint lying on an anvil with a 60 % vanilla glint, both in the `none` display context, pitch 30): the concepts not chosen. |
| `logo_anim_a` | 256x256, supersample 4 (outline thickness 4 = 16 render pixels), the pose of `concept_a` with `spin` false: 165 frames at 20 fps, fire glint at speed 10 and a gradient outline (violet, magenta, blue at speed 0.5454545), `time_offset` 0. At the default glint speed option (0.5) the glint scroll repeats after 82.5 s / speed and the gradient after 4.5 s / speed, so both repeat after exactly 8.25 s: a seamless loop for `logo_animated.gif` (art/logo.py). |
| `stills_wide` | 1200x400 stills with one look on every item (aurora glint + gradient outline): `universal_vanilla` (diamond sword, bow, trident, shield, elytra) and `universal_modded` (`advancednetherite:netherite_diamond_ingot`, `eternal_starlight:glistering_morning_star`, `immersive_armors:warrior_helmet`, `immersive_armors:warrior_chestplate`; needs `studio-mods/`; the four modded items sit at explicit `pos` x = -1.65, -0.55, 0.55, 1.65 so they fill the width). |
| `stills_tiles` | 512x512 tiles of one enchanted netherite sword: `glint_vanilla`, `glint_neutral`, `glint_rainbow`, `glint_aurora`, `glint_fire`, `glint_frost`, `glint_sparkle`, `glint_gold`, `glint_void`, `glint_inferno`, `glint_nebula`, `glint_starlight`, `glint_opal`, `glint_storm`, `outline_vanilla`, `outline_static`, `outline_rainbow`, `outline_pulse`, `outline_gradient`, `outline_enchant`, `outline_thick_1`, `outline_thick_3`, `outline_thick_6`, `outline_soft_80`. Glint tiles have no outline, outline tiles keep the vanilla glint. The outline tiles are shot from a farther camera (`view_height` 3.6 per shot) so that the largest outline the config allows (16 render pixels, 8 matte pixels) still measures 6 px on a card tile. |
| `stills_gap` | 800x800, supersample 1, perspective close-ups at a grazing angle: `gap_sword_off`, `gap_sword_on` (netherite sword), `gap_modded_off`, `gap_modded_on` (`immersive_armors:warrior_chestplate`), both items enchanted (vanilla glint), yaw 30, pitch 45. The cracks are one pixel hairlines: show the stills at 1:1 or crop and enlarge a detail. |

## Files

* `build.gradle`: the `studio` source set, the `glintsmith_studio` Loom mod, the `runStudio` run config, and
  `check` depends on `studioClasses` (the studio is compiled by every build, never packaged).
* `src/studio/resources/fabric.mod.json`, `glintsmith_studio.mixins.json`, `scenes/*.json`.
* `src/studio/java/navrelis/glintsmith/studio/`: `StudioMod` (entry point, options, watchdog, exit code),
  `StudioBootstrap` (world), `StudioScreen` (frames), `Scene` (parser), `Looks` (look to config), `PipelineBridge`
  (reflection into the config manager and the outline WORLD phase), `FrameWriter` (PNGs, `scene.json`),
  `StudioClock`, and `mixin/` (clock redirects, exit code).

## Limits

* The studio mixins redirect `Util.getMillis()` inside `GlintSystem.onFrameBegin`, `GlintShards`,
  `OutlineSystem.onFrameBegin` / `time()` and `RenderStateShard.setupGlintTexturing` with `require = 1`: if one of
  those methods is renamed or stops reading the clock, the studio fails at startup with a mixin error (intended: a
  silent fallback to the real clock would make the frames non-deterministic).
* WORLD-phase outlines in an orthographic view use the depth-matched range described above; the depth rules are exact
  only at the camera target distance and approximate a block or two in front of or behind it.
* The GUI-context outline has no depth test (as in the inventory): with several overlapping GUI-context subjects each
  outline is composited after its own item, so a later item covers an earlier item's outline.
* A per-subject look cannot change the gap fix.

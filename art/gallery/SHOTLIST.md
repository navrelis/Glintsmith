# Glintsmith capture shot list

Every image of the CurseForge gallery is built from in-game captures of the real mod running (see `art/README.md`).
This file lists every capture that `art/gallery/cards.json` asks for: 42 stills (7 of them hero shots, which also make the logo, the icon and the banner) and 112 GIF frames (7 looks x 16). Capture them,
save them as PNG under `art/captures/` with exactly these names, then run `py -B art/gallery/build.py --strict`.
`py -B art/gallery/build.py --list` prints the same names and tells you which ones are still missing.

Nothing here may be faked, drawn or composited: a capture is one unedited 1920 x 1080 screenshot of the game. The logo, the mod icon and the banner are
cut from captures too (section 3, "Hero shots"); there is no drawn art anywhere in the gallery.

## 0. Rig

| Setting | Value |
| --- | --- |
| Window / framebuffer | windowed, **1920 x 1080** framebuffer (exactly), no window scaling |
| Version | Minecraft 1.21.1, Fabric Loader 0.19.5, Fabric API 0.116.17, Glintsmith from the build under test |
| Mods | the test pack with `advancednetherite`, `eternal_starlight`, `cataclysm`, `immersive_armors`; Sodium if the pack has it (leave it on); Iris installed, **no shader pack selected** except in the three Iris shots (`shaders_iris_*`) |
| Resource packs | none besides the mods' own |
| Options | FOV 70 (vertical) for the wide shots, **FOV 30 for every close-up** (the vanilla FOV option, slider minimum: `options fov=30`, put back to 70 afterwards; the Director `fov` override does **not** change the world view and is never used; see "Backdrop and close-ups" below), render distance 12, view bobbing off, entity shadows off, particles minimal, brightness **Bright (gamma 1.0)**, mipmap levels 4, graphics Fancy, clouds off, language English (US) |
| GUI scale | **2** for every shot (world shots hide the GUI with F1 anyway). GUI shots rely on 2 so that the builder can halve them losslessly (`down: 2`) |
| Account | offline default skin (Steve), name `GS` |
| HUD | F1 on (hidden) for every world shot; GUI shots show the screen only; the mouse pointer is parked at the very top left of the window |
| Light | every capture is shot **bright**: gamma 1.0 (Bright) **and** Night Vision (`/effect give @s minecraft:night_vision infinite 0 true`; give it again after every game mode change and after a respawn), plus a level 15 light block in **every air cell** of every room (the fills below), so nothing is dim or tinted orange. Only the two inventory shots switch Night Vision off (the inventory lists the effect). The close-ups also stand in front of a full-bright backdrop panel (next section) so the backdrop really is white |
| Warm-up | after every `tp`, config change or screen change wait 2 s (40 ticks) before the screenshot |
| Animated colours | rainbow, pulse, gradient and enchantment colours move with time; for those take the shot when the colour looks best (three screenshots 0.4 s apart is fine, keep one; a pulse has a period of 2 s, so take five 0.4 s apart) |
| Output | `art/captures/<name>.png`, 8-bit RGB or RGBA PNG (the game's own screenshot format) |

### Backdrop and close-ups

**The backdrop is white / cream, never black.** The gallery is a light theme (`art/README.md`, "Theme"): the captures sit on cream cards inside a thin
dark edge, so the studio and the rooms are built of white blocks and lit to full brightness. Hero studio (room H): a box of white blocks (`snow_block`).
Rooms A and B: smooth quartz with a birch floor and stripped birch corner columns. No dark block may show behind a subject or a screen.

**Why panels.** A block wall cannot be pure white: Minecraft shades faces by direction (north / south 0.8, east / west 0.6, top 1.0), so even a white wall that
faces the camera reaches only about 200 of 255. Behind every close-up therefore stands a **full-bright backdrop panel**: a `text_display` that holds a single space,
with `background:-1` (opaque white), `brightness:{sky:15,block:15}`, `billboard:"fixed"` and the tag `gsp`. Its transformation scale stretches the background rectangle
(0.125 x 0.275 block per unit scale) to the size of the wall or floor it covers. The panel is drawn without face shade, so it is pure white, and it lies 0.005 block
in front of the surface, behind the item frames and items. The mod's outline is drawn over it. The panel template (`X Y Z`, the yaw / pitch and the two scales
come from the table below):

```
/summon minecraft:text_display X Y Z {Rotation:[YAWf,PITCHf],text:'{"text":" "}',background:-1,brightness:{sky:15,block:15},billboard:"fixed",Tags:["gsp"],transformation:[SXf,0f,0f,0f,0f,SYf,0f,0f,0f,0f,1f,0f,0f,0f,0f,1f]}
/kill @e[type=minecraft:text_display,tag=gsp]          # removes every panel when a section is done
```

| Panel | Covers | Entity X Y Z | Rotation (yaw, pitch) | Scale SX, SY |
| --- | --- | --- | --- | --- |
| room A, north wall | x -0.5 .. 25.5, y 97.5 .. 107.5, plane z 1.005 | 9.9 97.5 1.005 | 0, 0 | 208, 36.3636 |
| room A, floor | x -0.5 .. 25.5, z 0.5 .. 12.5, y 100.005 | 9.9 100.005 12.5 | 0, -90 | 208, 43.6364 |
| room B (glint presets), north wall | x 3.5 .. 33.5, y 97 .. 107, plane z 34.05 (in front of the corner columns) | 15.5 97 34.05 | 0, 0 | 240, 36.3636 |
| room B (glint presets), floor | x 10.5 .. 26.5, z 34.05 .. 46, y 100.005 | 16.9 100.005 46 | 0, -90 | 128, 43.4545 |
| room B (showcase), north wall | x -2.5 .. 27.5, y 97 .. 107, plane z 34.05 | 9.5 97 34.05 | 0, 0 | 240, 36.3636 |
| room B (showcase), floor | x 4.5 .. 20.5, z 34.05 .. 46, y 100.005 | 10.9 100.005 46 | 0, -90 | 128, 43.4545 |
| room H, north wall | x 0 .. 24, y 98 .. 110, plane z 65.005 | 9.6 98 65.005 | 0, 0 | 192, 43.6364 |
| room H, south wall | x 0 .. 24, y 98 .. 110, plane z 79.995 | 14.4 98 79.995 | 180, 0 | 192, 43.6364 |

Each close-up camera is placed so that its whole view lies inside the panels (the generator checks this). Panels are summoned before
the section that needs them and killed when it is done. The room A panels are summoned a second time right before `outline_vanilla` and
the four `gap_*` shots: the clean-up of the room B glint section (`/kill` by the tag `gsp`) removes every panel, room A's included, and
those five shots come after it, so they still have the white panel behind them (killed again after the last gap shot). The wide FOV 70
shots (`shaders_sodium`, `shaders_iris_*`), the target shots and the screen shots have **no panel**: the quartz wall stays as it is.

**Close-ups** are the shots that fill the frame with one subject: the seven hero shots, the universal wall, the glint presets, the
outline styles, the gap-fix pairs and the showcase GIF. They use the vanilla FOV option **30** (Options > Video Settings > FOV, or the
Director `options fov=30`); the camera distances below were computed for it. Vertical FOV f at d blocks gives 1080 / (2 d tan(f / 2))
pixels per block on the 1080 px high capture. Wide shots (shaders, targets, the screens, `hero_firstperson`) stay at FOV 70 (the first
person hand is drawn with a fixed FOV of 70 anyway).

Outline thickness is in framebuffer pixels and does not grow with the subject, so the big subjects use thicker lines: hero sword 8 px (fire look 10), lineup 5, armor 6,
showcase 6 (frost look 8); the small outline styles keep their own 1 .. 6 px.

Expected subject sizes on the 1920 x 1080 capture (from the generator, not measured). The first full run measured the real capture at
about 0.885 of the formula (frame pitch / frame size at FOV 30 and FOV 70); only `universal_wall` is sized with that factor
(`CAPTURE_SCALE`), the other rows are the formula values:

| Shot | Camera `x y(feet) z (yaw, pitch)` | d | FOV | Subject | Expected px high |
| --- | --- | --- | --- | --- | --- |
| `universal_wall` | 6.5 101.38 8.11 (180, 0) | 7.06 | 30 | the grid of 10 frames in 2 rows (4.75 x 1.75 blocks, x 4.125 .. 8.875) | about 1200 px wide (62 %), 253 px per block |
| `outline_*`, `outline_vanilla` | 13.5 100.88 10.59 (180, 0) | 9.54 | 30 | netherite sword in the frame at (13, 102); the 0.75 block frame is as high as the 160 px cell | 95 |
| `glint_*` | 18.5 99.58 44.25 (180, 0) | 5.75 | 30 | stand chestplate (0.7 block) | 245 |
| `gap_sword_*` | 17.478 100.88 1.906 (131.2, 0) | 1.30 | 30 | diamond sword in the frame at (16, 102, 1) | 701 |
| `gap_modded_*` | 21.478 100.88 1.906 (131.2, 0) | 1.30 | 30 | ingot (0.45 block) in the frame at (20, 102, 1) | 698 |
| `hero_sword_*`, `logo_sword` | 18.5 100.88 66.29 (180, 0) | 1.24 | 30 | netherite sword, frame at (18, 102, 65) | 735 (68 %) |
| `hero_lineup` | 5.0 100.88 71.45 (180, 0) | 6.40 | 30 | 6 items (0.47 block), frames x 2..7, 315 px apart, row 1722 px wide | 148 (14 %) |
| `hero_armor` | 12.5 99.705 75.7 (0, 0) | 3.50 | 30 | stand at (12.5, 79.2), helmet top to chestplate bottom | 720 (67 %) |
| `hero_firstperson` | 12.5 100 74.7 (0, 10) | 5.30 | 70 | held sword (hand FOV is fixed 70) | - |
| `shaders_sodium`, `shaders_iris_world` | 18.5 99.73 42.1 (180, 0) | 3.60 | 70 | whole stand | 423 (39 %) |
| showcase | 12.5 99.367 44.19 (180, 0) | 5.69 | 30 | whole stand at (12.5, 38.5) | 700 (65 %) |

Coordinates: `/tp @s X Y Z YAW PITCH` with **Y = feet**, eye = Y + 1.62, yaw 0 = south (+Z), 90 = west (-X), 180 = north (-Z),
-90 = east (+X), pitch positive = looking down. "d" is the distance from the camera to the subject (the item plane of a frame lies 0.05 block in front of its wall).

### Hero shots rig (differs from the table above)

The seven hero shots (section 3, "Hero shots") are product photos, not showroom shots, and use their own rig:

| Setting | Value |
| --- | --- |
| FOV | **30**, the vanilla FOV option (the slider minimum; no override needed), except `hero_firstperson` (70, see its row) |
| Backdrop | **white**: the hero room H is a closed box of `snow_block`, and the subject stands in front of a full-bright white panel on the north wall (sword, lineup) or the south wall (stand), see "Backdrop and close-ups". Nothing else in frame: no corner, no ceiling, no pillar |
| Light | bright and even: gamma 1.0 + Night Vision, every air cell of room H is a level 15 light block (invisible, so none shows in a picture), `/time set noon`. The glint and the outline must still read clearly against the white |
| HUD | F1 (hidden), GUI hidden, no crosshair, mouse pointer parked at the very top left |
| Resolution | 1920 x 1080 framebuffer, unscaled |
| Framing | the subject is **centred** and leaves a margin of at least 10 percent of the height on every side, so a centred square crop of the full height (1080 x 1080) and a 16 : 9 crop both work |
| Everything else | as the rig above: no resource packs, offline Steve, Sodium on, shaders off, view bobbing off |

## 1. World and rooms

New world `GS-Showroom`: Creative, cheats on, Superflat, preset "The Void" (`{"layers":[],"biome":"minecraft:the_void"}`), no structures.

```
/gamerule doDaylightCycle false
/gamerule doWeatherCycle false
/gamerule doMobSpawning false
/gamerule randomTickSpeed 0
/gamerule keepInventory true
/gamerule sendCommandFeedback false
/time set noon
/weather clear
/difficulty peaceful
/tp @s 12 101 8
```

Room A (item frame shots, z 0..16) and room B (stand, dropped items, hand shots, z 32..48). A bright shell of smooth quartz with a
birch floor and stripped birch corner columns. **Every air cell** holds a level 15 light block (invisible unless you hold one) for even, full, neutral light;
the corner columns replace the light blocks in their four cells, so fill the light blocks first:

```
# room A
/fill 0 98 0 24 109 16 minecraft:smooth_quartz hollow
/fill 1 99 1 23 99 15 minecraft:birch_planks
/fill 1 100 1 23 108 15 minecraft:light[level=15]
/fill 1 100 1 1 108 1 minecraft:stripped_birch_log
/fill 23 100 1 23 108 1 minecraft:stripped_birch_log
/fill 1 100 15 1 108 15 minecraft:stripped_birch_log
/fill 23 100 15 23 108 15 minecraft:stripped_birch_log
# room B (same, 32 blocks further south)
/fill 0 98 32 24 109 48 minecraft:smooth_quartz hollow
/fill 1 99 33 23 99 47 minecraft:birch_planks
/fill 1 100 33 23 108 47 minecraft:light[level=15]
/fill 1 100 33 1 108 33 minecraft:stripped_birch_log
/fill 23 100 33 23 108 33 minecraft:stripped_birch_log
/fill 1 100 47 1 108 47 minecraft:stripped_birch_log
/fill 23 100 47 23 108 47 minecraft:stripped_birch_log
```

Room H (the hero studio, z 64..80) is a plain white box of snow blocks, with the floor raised by one layer so that the walking floor is y = 100 like in the
other rooms, and a level 15 light block in every air cell:

```
/fill 0 98 64 24 109 80 minecraft:snow_block hollow
/fill 1 99 65 23 99 79 minecraft:snow_block
/fill 1 100 65 23 108 79 minecraft:light[level=15]
/time set noon
```

The backdrop panels of the previous section are summoned in front of the walls. Frames for hero shots are invisible: add `Invisible:1b` to the frame summon (and
the tag `gsh`, so they can be killed again).

The walking floor is y = 100. Use `/gamemode spectator` for the camera shots that
say "spectator" (no hand, free flying) and `/gamemode creative` or `survival` where a shot says so; give Night Vision again after every game mode change.

### Enchantment shorthand

`ENCH(sword)` = `minecraft:enchantments={levels:{"minecraft:sharpness":5,"minecraft:unbreaking":3,"minecraft:mending":1}}`.
Items from other mods have no enchantments: give them `minecraft:enchantment_glint_override=true`, which gives them the vanilla
glint and makes them count as "enchanted" for the outline. Wherever a table says `[ench]` use the enchantments of that item
below; `[glint]` means `minecraft:enchantment_glint_override=true`.

| Item | Components |
| --- | --- |
| `minecraft:diamond_sword` | sharpness 5, unbreaking 3 |
| `minecraft:netherite_sword` | sharpness 5, unbreaking 3, mending 1 |
| `minecraft:netherite_pickaxe` | efficiency 5, unbreaking 3, fortune 3 |
| `minecraft:bow` | power 5, unbreaking 3, infinity 1 |
| `minecraft:trident` | loyalty 3, unbreaking 3 |
| `minecraft:shield` | unbreaking 3 |
| `minecraft:elytra` | unbreaking 3, mending 1 |
| `minecraft:netherite_helmet / _chestplate / _leggings / _boots` | protection 4, unbreaking 3 (helmet also respiration 3, boots feather falling 4) |
| `cataclysm:black_steel_targe`, `eternal_starlight:glistering_morning_star`, `advancednetherite:netherite_diamond_ingot`, `immersive_armors:warrior_helmet`, `immersive_armors:warrior_chestplate` | `[glint]` |

### Frames and stand templates

```
# item frame on the NORTH wall of room A (wall block at z = 0, frame block z = 1), X Y = frame block
/summon minecraft:item_frame X.5 Y.5 1.5 {Facing:3b,Invulnerable:1b,Fixed:1b,Item:{id:"<item>",count:1,components:{<components>}}}
# the armor stand of room B for the glint presets, standing at (18.5, 100, 38.5), facing south (towards the camera at z 44.25)
/summon minecraft:armor_stand 18.5 100 38.5 {Rotation:[0f,0f],ShowArms:1b,NoBasePlate:1b,Invulnerable:1b,NoGravity:1b,Pose:{LeftArm:[0f,0f,-12f],RightArm:[-18f,0f,12f]},ArmorItems:[{boots},{leggings},{chestplate},{helmet}],HandItems:[{netherite_sword},{}]}
```

Armor and sword are the enchanted netherite pieces of the table above (ArmorItems order is boots, leggings, chestplate, helmet).
The showcase stand stands at (12.5, 100, 38.5), also facing south. The hero stand stands at (12.5, 100, 79.2) in room H, in front of the south wall panel, facing **north**
(`Rotation:[180f,0f]`); its camera looks south at it.

## 2. Glintsmith config

The config file is `config/glintsmith.json`; it reloads while the game runs (or run `/glintsmith reload`). `/glintsmith` opens the
settings screen. Every shot lists a profile; write the whole profile (deep merge over the defaults) before the shot. **`rules` is
`[]` in every profile except the two Rules shots**, so the default example rules do not interfere.

**BASE** (the plain starting point, outline on):

```json
{"rules": [], "outline": {"enabled": true, "condition": "ENCHANTED", "color": {"mode": "STATIC", "colors": ["#A050F0"]},
  "thickness": 2.0, "softness": 0.0, "opacity": 1.0, "targets": {"gui": false}, "disableWithShaders": false,
  "handWithShaderPack": true},
 "glint": {"enabled": false}, "gapFix": {"enabled": true, "mode": "AUTO"}, "durabilityAlert": {"enabled": false}}
```

`glint.enabled = false` means the vanilla glint, untouched. Macros used below (merge them over BASE):

| Macro | Patch |
| --- | --- |
| `NOOUT` | `{"outline": {"enabled": false}}` |
| `VANILLA` | `{"rules": [], "outline": {"enabled": false}, "glint": {"enabled": false}, "gapFix": {"enabled": false}}` (nothing of the mod active; replaces BASE) |
| `T(n)` | `{"outline": {"thickness": n}}` |
| `OUT(mode, colors, speed)` | `{"outline": {"color": {"mode": mode, "colors": colors, "speed": speed}}}` |
| `RAINBOW(speed)` | `{"outline": {"color": {"mode": "RAINBOW", "colors": [], "speed": speed, "brightness": 0.8}}}` (brightness 0.8 so the rainbow does not wash out on white; speed 1 unless a shot says otherwise) |
| `SOFT(v)` | `{"outline": {"softness": v}}` |
| `TINT(hex)` | `{"glint": {<each of the four layers>: {"tint": {"mode": "STATIC", "colors": [hex]}}}}` |
| `GL(p)` | `{"glint": {"enabled": true, "item": {"texture": "preset:p", "scale": 0.13, "strength": 1.0}, "armor": {"texture": "preset:p", "scale": 0.22, "strength": 1.0}, "entity": {"texture": "preset:p", "scale": 0.22, "strength": 1.0}, "gui": {"texture": "preset:p"}}}` (all four layers; `sparkle` uses scale 0.1 on every tuned layer; the gui layer gets scale 0.13 and strength 1.0 only in `shaders_iris_gui`, where the GUI glint is on the picture) |
| `GUION` | `{"outline": {"targets": {"gui": true}}}` |
| `ARMOR_ON` | `{"outline": {"targets": {"armor": true}}}` (worn armor and the elytra are outlined only with this; the default is `false`. Item frames are not the armor target, they follow `itemFrame`) |

**Glint scale.** The capture pack's block atlas is 8192 x 4096, so at the default scale 1.0 an item sprite shows only a few texels of
the 128 px preset pattern and the glint reads as a flat colour wash. Every shot with a preset glint therefore uses `GL(p)` as written
above (scale 0.13 item, 0.22 armor and entity, 0.1 sparkle, strength 1.0, the maximum): all `glint_*` except `glint_vanilla`, the four
sword shots (`hero_sword_*`, `logo_sword`), `hero_lineup`, `hero_armor`, `hero_firstperson`, the four Sodium and Iris shots and looks 3
to 7 of the showcase GIF. The vanilla baselines (`glint_vanilla`, the vanilla glint of the universal wall, outline, gap-fix and target
shots, looks 1 and 2) keep vanilla settings, and so does `config_glint`, which shows the settings screen with the default values.

Colours (deeper than in the first version, because pale colours vanish on white): violet `#A050F0` (default, last stop of the logo gradient), amber `#FF8A1F`,
ice `#1AA6DC`, gold `#F0A500`, red `#FF4D4D`; logo gradient `GRAD_LOGO` = mint `#1FC77A`, blue `#3D6BFF`, violet `#A050F0`. The default enchantment colours of the stand's
helmet and boots are pale, so the Void look adds `ENCH_DEEP` = `{"enchantmentColors": {"minecraft:feather_falling": "#2F8FE8", "minecraft:respiration": "#12A8C0"}}`.

Changing `gapFix` needs a resource reload (F3 + T, wait until it is done).

## 3. Stills (42)

All camera lines assume the rooms above. "Crop" is where the card looks (source pixels, centred unless given); it only
has to be roughly right, the lead moves the crop in `cards.json` after looking at the capture.

### Universal (1)

| File | Scene, camera, mode | Config | Notes |
| --- | --- | --- | --- |
| `universal_wall.png` | Room A, two rows of five frames. Y 102 (vanilla) X 4..8: `diamond_sword`, `bow`, `trident`, `shield`, `elytra`. Y 103 (modded) X 4..8: `cataclysm:black_steel_targe`, `eternal_starlight:glistering_morning_star`, `advancednetherite:netherite_diamond_ingot`, `immersive_armors:warrior_helmet`, `immersive_armors:warrior_chestplate`. Spectator, **FOV 30** (a close-up, so it stands in front of the room A panels, which cover the north wall and the floor in front of it), `tp 6.5 101.38 8.11 180 0`, d 7.06. The grid is centred and about 1200 px wide in the capture (62 % of 1920). The generator keeps every other item frame out of the view, also with the measured scale: the outline sword frame at x 13 and the corner column at x 1 .. 2 must not show | BASE + `T(3)` | the grid is about 1200 x 442 px (253 px per block); the measured grid is 1230 x 453 px (x 331 .. 1560, y 304 .. 756); the card crop in `cards.json` is `[198,270,1495,520]` (the grid plus a margin, an area-filtered 1.625 : 1 downscale to 920 x 320); the top row is the modded items, the bottom row the vanilla ones |

### Hero shots (7)

The big images: they make the logo, the icon, `logo_animated.gif`, the banner and two cells of `card_targets`. Rig: "Hero shots rig" in
section 0 (FOV 30, white snow-block room H with full-bright white wall panels, bright light, HUD hidden, 1920 x 1080). Every shot is a single unedited
screenshot, subject centred. Outline and glint are the real mod; take animated colours at their best moment (three screenshots 0.4 s apart,
keep one). The three sword shots and the logo shot use one invisible item frame at block `(18, 102, 65)` on the north wall of room H holding
`minecraft:netherite_sword` with sharpness 5, unbreaking 3, mending 1; the camera is spectator at x 18.5 looking north (yaw 180, pitch 0), eye
height 102.5 (feet 100.88), d 1.24 blocks: `tp 18.5 100.88 66.29 180 0`. At FOV 30 the sword is about 735 px high (68 percent of the picture height), its
diagonal fills the picture and stays well away from every edge. If the sword is not exactly centred or the square crop cuts it, move the camera in or out
along z (the generator also takes `x_logo_sword_near` at z 66.1 and `x_logo_sword_far` at z 66.54 as spares). The outline is thick (8 px) because it is
measured in screen pixels.

| File | In frame | Scene, camera | Config |
| --- | --- | --- | --- |
| `hero_sword_rainbow.png` | the netherite sword, centred, diagonal across the picture | sword frame as above | BASE + `GL("rainbow")` + `T(8)` + `RAINBOW()`. Used for the banner picture (crop `[520,98,880,880]`, shown at 180 x 180) and as the first look of `logo_animated.gif` |
| `hero_sword_void.png` | the same sword, same camera, same framing as `hero_sword_rainbow` (no camera move between the shots) | same | BASE + `GL("void")` + `T(8)` + `OUT("ENCHANTMENT", [], 1)`. Second look of `logo_animated.gif` |
| `hero_sword_fire.png` | the same sword, same camera, same framing | same | BASE + `GL("fire")` + `T(10)` + `OUT("PULSE", ["#FF8A1F"], 1)`, shot at the bright phase of the pulse (take five shots 0.4 s apart). Third look of `logo_animated.gif` |
| `logo_sword.png` | **the logo**: the sword exactly centred on the picture centre, blade diagonal about 68 percent of the height. The centred 1080 x 1080 square must hold the whole sword and read on its own at 128 px and 32 px, so keep the silhouette simple and nothing else in frame (square framing) | same frame and camera as the three above | BASE + `GL("aurora")` + `T(8)` + `OUT("GRADIENT", ["#1FC77A", "#3D6BFF", "#A050F0"], 1)`. The icon is the area-filtered square crop of this picture. If one of the three shots above is already exactly centred and strong, it may be saved a second time as `logo_sword.png` |
| `hero_armor.png` | the armor stand's netherite helmet and chestplate, head and chest centred, white behind, about 720 px from helmet top to chestplate bottom (67 percent). The card crops `[585,100,750,800]` (head to the top of the leggings; the stand continues below the picture) | room H: the stand at (12.5, 100, 79.2) facing north (`Rotation:[180f,0f]`, otherwise the summon of section 1) in front of the south wall panel. Spectator, `tp 12.5 99.705 75.7 0 0` (the camera looks south), d 3.50 | BASE + `GL("rainbow")` + `T(6)` + `RAINBOW()` + `ARMOR_ON` |
| `hero_lineup.png` | six enchanted items in a row, evenly spaced (315 px apart), vertically centred, each about 148 px high (14 percent); the row is about 1722 px wide and fits the middle 200 px of the height (the banner strip) | six invisible frames at Y 102, X 2..7 on the north wall of room H (frame blocks at `z = 65`): `diamond_sword[ench]`, `bow[ench]`, `shield[ench]`, `cataclysm:black_steel_targe[glint]`, `eternal_starlight:glistering_morning_star[glint]`, `immersive_armors:warrior_chestplate[glint]`. Spectator, `tp 5.0 100.88 71.45 180 0`, d 6.40 | BASE + `GL("aurora")` + `T(5)` + `RAINBOW()` |
| `hero_firstperson.png` | first person, the netherite sword in the right hand, blade pointing up and left across the picture, whole blade in frame, hand at the lower right, the white wall behind. The card crops `[98,0,1822,1080]` (the sword is cut by the right edge of the capture itself) | room H, **creative, hand visible**, F1, **FOV 70** (the hand is drawn with a fixed FOV of 70, so the option is put back to 70 first). `/item replace entity @s weapon.mainhand with minecraft:netherite_sword[ench]`, `tp 12.5 100 74.7 0 10` (about 5.3 blocks from the south wall, the camera looks south at the south wall panel). The generator also takes spares at FOV 30 and 40 (`x_hero_firstperson_fov30`, `_fov40`) | BASE + `GL("rainbow")` + `T(3)` + `RAINBOW()` |

Frames and stand summons are the ones of section 1, moved into room H as the table says.

### Glint presets (9)

Scene: room B stand (section 1) with the room B glint panels behind it, spectator, FOV 30, `tp 18.5 99.58 44.25 180 0`, d 5.75, the camera looks at the netherite chestplate (about 245 px high).
Config for all nine: BASE + `NOOUT`, plus the glint patch below (`GL(p)` carries the scale and strength of section 2, so the pattern is
visible on the chestplate; `glint_vanilla` keeps the vanilla glint). The whole stand (about 660 px high, 410 px wide) is cropped per capture to 1012 x 920 and area-filtered to 176 x 160, stand centred, its top 42 px below the cell top (room for the label), the floor seam left as rendered.

| File | Glint patch |
| --- | --- |
| `glint_vanilla.png` | none: `glint.enabled = false` (the true vanilla glint, the "before") |
| `glint_neutral.png` | `GL("neutral")` + tint on all four layers: `"tint": {"mode": "STATIC", "colors": ["#52E0A8"]}` |
| `glint_rainbow.png` | `GL("rainbow")` |
| `glint_aurora.png` | `GL("aurora")` |
| `glint_fire.png` | `GL("fire")` |
| `glint_frost.png` | `GL("frost")` |
| `glint_sparkle.png` | `GL("sparkle")` |
| `glint_gold.png` | `GL("gold")` |
| `glint_void.png` | `GL("void")` |

### Outline styles (10)

Scene: room A with the room A panels behind the wall, one frame at X 13, Y 102 holding `minecraft:netherite_sword` with sharpness 5,
unbreaking 3, mending 1. Spectator, FOV 30, `tp 13.5 100.88 10.59 180 0`, d 9.54, the frame centred on the screen (the sword is about
95 px high, the 0.75 block frame 160 px). Crop 176 x 160 native, centred on the item frame (the frame is about 144 px square and sits at a slightly different place in every shot; the other frames at the left edge x < 100 are out of the crop). Glint is vanilla (`glint.enabled = false`) everywhere here.
`outline_vanilla` is shot later than the others, together with the gap-fix pair (see there), with the room A panels summoned again.
Outline-only changes:

| File | Config |
| --- | --- |
| `outline_vanilla.png` | `VANILLA` (no outline at all, the "before") |
| `outline_static.png` | BASE (violet `#A050F0`, 2 px). **Also used by the Item frames cell of `card_targets`** |
| `outline_rainbow.png` | BASE + `T(3)` + `RAINBOW()` |
| `outline_pulse.png` | BASE + `T(3)` + `OUT("PULSE", ["#FF8A1F"], 1)` (shoot at the bright phase) |
| `outline_gradient.png` | BASE + `T(3)` + `OUT("GRADIENT", ["#FF4D4D", "#F0A500", "#1AA6DC"], 0.5)` |
| `outline_enchant.png` | BASE + `T(3)` + `OUT("ENCHANTMENT", [], 0.5)` (the sword has three enchantments, so three colours blend) |
| `outline_thick_1.png` | BASE + `T(1)` |
| `outline_thick_3.png` | BASE + `T(3)` |
| `outline_thick_6.png` | BASE + `T(6)` |
| `outline_soft_80.png` | BASE + `T(4)` + `SOFT(0.8)` (soft glow) |

### Targets (3, plus `outline_static.png` above; first person and armor are hero shots)

BASE + `T(3)` for all of them (the GUI one is BASE + `GUION` with the default thickness 2). All three are wide shots at FOV 70 in the quartz room B with no panel. The card is a layout of five cells plus the inventory at native size (see `cards.json`). The first person and armor cells of this card are the hero shots `hero_firstperson.png` and `hero_armor.png`.

| File | Scene, camera, mode | Notes |
| --- | --- | --- |
| `target_third_person.png` | Room B, creative, sword in hand, F1. `tp 12.0 100 40.5 0 0`, then F5 twice (front view, the player faces the camera) | crop around the sword hand; card crop `[665,466,675,400]` (player and sword, the white block at the left and the second mob at the right are out) |
| `target_dropped.png` | Room B slab floor: `/fill 4 100 40 9 100 45 minecraft:smooth_quartz_slab[type=bottom]`, then two floating items: `/summon minecraft:item 6.15 100.62 42.5 {Item:{id:"minecraft:diamond_sword",count:1,components:{...}},NoGravity:1b,Age:-32768,PickupDelay:32767,Motion:[0.0,0.0,0.0]}` and `/summon minecraft:item 6.85 100.62 42.5 {Item:{id:"minecraft:netherite_chestplate",...},...}`. Spectator, `tp 6.5 100.48 39.0 0 20` | both items fit in the crop; items bob, take the shot with both clearly outlined |
| `target_gui.png` | Survival, standard inventory open (E), GUI scale 2, recipe book closed, Night Vision off (`/effect clear @s minecraft:night_vision`, the inventory would list it). Fill the hotbar and the first inventory row with enchanted items (below). Config adds `GUION` | the card uses `[784,374,352,332]`: exactly the inventory panel, native size (no down or up scaling, the GUI pixels stay crisp; the recipe book button above and the overlay text are out) |

Inventory content (`/item replace entity @s hotbar.0 with ...`, `inventory.0` is the first row of the main inventory):
hotbar 0..8 = `diamond_sword[ench]`, `netherite_pickaxe[ench]`, `bow[ench]`, `trident[ench]`, `shield[ench]`, `elytra[ench]`,
`enchanted_golden_apple`, `cataclysm:black_steel_targe[glint]`, `eternal_starlight:glistering_morning_star[glint]`;
inventory 0..8 = `immersive_armors:warrior_helmet[glint]`, `immersive_armors:warrior_chestplate[glint]`,
`advancednetherite:netherite_diamond_ingot[glint]`, `netherite_helmet[ench]`, `netherite_chestplate[ench]`,
`enchanted_book` (stored mending), `totem_of_undying`, a plain `diamond_sword` (no enchantments, so no outline), `iron_pickaxe[ench]`.

### Gap fix, before and after (4)

The seam is MC-73186: hairline gaps between the faces of flat generated item models, most visible at an oblique angle under the
vanilla glint. **Outline off, vanilla glint on** (`glint.enabled = false` so the vanilla glint draws), so nothing hides the seam.
The "off" shots use `VANILLA` (`gapFix.enabled = false`, nothing of the mod active), the "on" shots `NOOUT` (`gapFix.enabled = true`, AUTO); reload resources (F3 + T) after each switch,
and take all "off" shots first, then all "on" shots, from the **identical** camera.

Scene: room A, **north wall**, with the room A panels behind it (summoned again before `outline_vanilla` and these four shots, because
the room B clean-up killed them; the north wall faces south, so it gets the bright face shade). The two frames are summoned shortly
before the shots, far from the outline sword frame (x 13) and the universal wall (x 4..8): sword frame at block (16, 102, 1), ingot
frame at block (20, 102, 1), both `Invulnerable:1b,Fixed:1b`. Spectator, FOV 30, d 1.30, the camera 40 degrees off the wall normal
looking straight at the item (yaw 131.2).

| File | Scene, camera | Notes |
| --- | --- | --- |
| `gap_sword_off.png` / `gap_sword_on.png` | `minecraft:diamond_sword[ench]` in the frame at (16, 102, 1); `tp 17.478 100.88 1.906 131.2 0` | macro: the sword is about 700 px high. Card crop 864 x 1080 (full height, centred on the frame), box-downscaled by 4 to 216 x 270; the frame is bigger than the capture, so it is cut by the picture edge top and bottom, and the stick at x < 25 is out |
| `gap_modded_off.png` / `gap_modded_on.png` | `advancednetherite:netherite_diamond_ingot[glint]` in the frame at (20, 102, 1); `tp 21.478 100.88 1.906 131.2 0` | same framing, the ingot is about 700 px high |

If the seam is not visible, try other angles (yaw 110..150, 5 degree steps, move the camera along the same line), FOV 20, or `mipmapLevels 0`, and items with many
transparent holes (`minecraft:bow`, `minecraft:fishing_rod`). Whatever item shows the clearest difference may replace the one
above; tell the lead so the card text stays true.

### Rules and config screens (4)

All four: GUI scale 2, FOV 70, open with `/glintsmith`, F1 not needed. Spectator in room A at `tp 21 100 8 -90 0`, looking at the plain east wall so the world
behind the screen is the white quartz wall (no panel; the screen darkens the world behind it). The builder halves these (crops 904 wide, 540 to 630 high, see `cards.json`) so the whole screen should sit inside the
middle 904 x 600 of the capture; if the screen is larger, tell the lead.

| File | Screen | Config |
| --- | --- | --- |
| `rules_tab.png` | Rules tab, the four rules below visible, none selected | BASE + the four rules |
| `rule_edit.png` | the editor of rule 1 (press Edit): name, items `#minecraft:swords`, the outline override with the red colour and thickness 3 visible | BASE + the four rules |
| `config_outline.png` | Outline tab, top of the page, nothing hovered | BASE |
| `config_glint.png` | Glint tab, layer Item, texture `preset:aurora` selected (cycle with the arrow button) | BASE + `GL("aurora")` for the item layer only, texture only (default scale and strength, the screen shows the values) |

The four rules (`"rules"` value):

```json
[{"name": "Swords: red outline", "items": ["#minecraft:swords"],
  "outline": {"enabled": true, "color": {"mode": "STATIC", "colors": ["#FF4D4D"]}, "thickness": 3.0}},
 {"name": "Mending gear: gold glint", "enchantments": ["minecraft:mending"], "glint": {"texture": "preset:gold"}},
 {"name": "Cataclysm: no outline", "items": ["@cataclysm"], "outline": {"enabled": false}},
 {"name": "Totem of Undying", "items": ["minecraft:totem_of_undying"],
  "outline": {"enabled": true, "color": {"mode": "STATIC", "colors": ["#FFF3A5"]}}}]
```

### Sodium and Iris (4)

Same scene and config for all four so they compare: BASE + `T(3)` + `RAINBOW()` + `GL("aurora")` + `GUION` + `ARMOR_ON`
(`handWithShaderPack` true, `disableWithShaders` false). Capture what the game really does and tell the lead if the card text
("glint keeps working, outlines in the inventory work, hand outlines best effort, world outlines step aside") is not true.
The shader pack is any one popular pack that is installed (the generator uses Complementary Unbound with Euphoria Patches); note its name.
All four are wide shots at FOV 70 in the quartz room B with **no backdrop panel**.

| File | Setup | Camera | Crop |
| --- | --- | --- | --- |
| `shaders_sodium.png` | Sodium on, Iris with **no** pack | room B stand, spectator, `tp 18.5 99.73 42.1 180 0` (d 3.60, the stand is about 423 px high) | `[530,381,756,448]` area-filtered to 270 x 160 (the stand, the pillar at x 1290 is out) |
| `shaders_iris_world.png` | the same with the shader pack active | same camera | `[530,381,756,448]` area-filtered to 270 x 160 (the stand, the pillar at x 1290 is out) |
| `shaders_iris_hand.png` | shader pack active, creative, first person, F1, `/item replace entity @s weapon.mainhand with minecraft:netherite_sword[ench]` (FOV 70 as in the rig) | `tp 12.0 100 41.0 180 12` | `[1160,145,760,935]` area-filtered to 270 x 332 (the whole sword; the capture cuts it at the right edge) |
| `shaders_iris_gui.png` | shader pack active, survival, inventory open with the same items as `target_gui.png`, GUI scale 2 | none | `[784,374,352,332]` native size, as `target_gui.png` |

## 4. Showcase GIF (112 frames)

`art/captures/showcase/0001.png` .. `0112.png`: seven looks of 4.0 s each, **16 frames per look, 0.25 s apart (source_fps 4)**, 1920 x 1080 as
every other capture, on the same white / cream, bright setup as every other shot. One camera for the whole clip, no mouse or key input between frames.
Per look the generator shoots a burst of **50 screenshots** back to back (about 4.4 s at 89 ms per screenshot; 3.75 s are needed) and keeps the 16 frames that lie 0.25 s apart.

* Scene: room B stand at (12.5, 100, 38.5) with the room B showcase panels behind it (wall panel at z 34.05, floor panel), spectator, F1, **FOV 30**, `tp 12.5 99.367 44.19 180 0` (d 5.69).
  The whole stand is about 700 px high (65 percent) and centred. The builder crops `[236,125,1440,810]` (75 percent, centred on the measured stand: x 730 .. 1182, y 171 .. 889 over all frames), scales it by `down: 2` to 720 x 405, adds a 36 px caption bar (720 x 441) and keeps every `step`-th frame
  (with 16 frames at 4 fps `step` is 1: every frame, 0.25 s each). It raises the step by itself if the file would pass 8 MB.
* For each look: write the profile, wait 0.5 s for the reload, then shoot the burst and keep 16 frames. Profiles are BASE plus (for every look except Vanilla also `ARMOR_ON`, so the stand's armor is outlined):

| Frames | Caption (in the GIF) | Config |
| --- | --- | --- |
| 0001-0016 | Vanilla | `VANILLA` |
| 0017-0032 | Outline only | `T(6)` (violet, vanilla glint) |
| 0033-0048 | Rainbow glint + rainbow outline | `GL("rainbow")` + `T(6)` + `RAINBOW()` |
| 0049-0064 | Fire glint + pulsing outline | `GL("fire")` + `T(6)` + `OUT("PULSE", ["#FF8A1F"], 1)` |
| 0065-0080 | Frost glint + soft cyan outline | `GL("frost")` + `T(8)` + `OUT("STATIC", ["#1AA6DC"], 1)` + `SOFT(0.6)` |
| 0081-0096 | Aurora glint + gradient outline | `GL("aurora")` + `T(6)` + `OUT("GRADIENT", ["#1FC77A", "#3D6BFF", "#A050F0"], 1)` |
| 0097-0112 | Void glint + enchantment colours | `GL("void")` + `T(6)` + `OUT("ENCHANTMENT", [], 1)` + `ENCH_DEEP` |

Looks 3 to 7 use the tuned preset glint (`GL(p)`, section 2); looks 1 and 2 keep the vanilla glint. The stills are cuts only (no
interpolation), 16 per look at a fixed 0.25 s interval. The `showcase.gif` entry of `cards.json` must match: `frames_per_caption` 16,
`source_fps` 4, `step` 1, `crop` `[236,125,1440,810]`, `down` 2 (the GIF builder derives the frame delay, 25 cs, from them).

## 5. Checklist

1. Rooms built, stand and frames summoned, rig options set, GUI scale 2.
2. Capture in this order to avoid resource reloads: all shots that need `gapFix` on, except the gap pair; then the gap "off" pair (reload), then the gap "on" pair (reload).
3. Every file exists with the exact name in `art/captures/` (`py -B art/gallery/build.py --list`). Then `py -B art/logo.py` (logo, icon and animated logo from the hero shots) and look at `src/main/resources/assets/glintsmith/icon.png`.
4. `py -B art/gallery/build.py --strict` and look at each card; move crops in `cards.json` where the subject is off.
5. `py -B art/gallery/gif.py --strict` for the GIF (the card build already calls it); then `py -B art/verify.py`.

Counts: hero shots 7, universal 1, glint presets 9, outline styles 10, targets 3, gap fix 4, rules 2, config 2, Sodium/Iris 4 = **42 stills**; GIF 112 frames (7 looks x 16 frames, 0.25 s apart, from bursts of 50 shots).

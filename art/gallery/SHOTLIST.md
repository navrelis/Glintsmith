# Glintsmith capture shot list

Every image of the CurseForge gallery is a real render of the mod (see `art/README.md`). There are two kinds:

* **14 in-world captures** (sections 0 to 3): unedited 1920 x 1080 screenshots of the running game, for the cards that show the game as it is: targets,
  rules, config and shaders. They are listed below with their rig, scene and config. `py -B art/gallery/build.py --list` prints the same names and tells you
  which ones are still missing.
* **Studio renders** (section 4): the dev-only studio (`docs/STUDIO.md`, `./gradlew runStudio -Pscene=<scene>`) renders turntables and named stills of items
  and armor through the real Glintsmith pipeline, every frame over pure black and pure white, into `art/captures/studio/<scene>/`. The banner, the logo, the
  icon, the four GIFs and the universal, glint preset, outline style, gap fix and default look cards are built from them (`art/studio/`, `art/gallery/gifs.py`).

Nothing here may be faked, drawn or composited by hand: an in-world capture is one unedited screenshot, a studio frame is what the game rendered. The only
things set by code are layout (the card frame, panels and text), the studio matte, the cream backdrop with its soft shadows and the GIF encoding.

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
dark edge, so the rooms are built of white blocks and lit to full brightness. Hero studio (room H): a box of white blocks (`snow_block`).
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
| room H, south wall | x 0 .. 24, y 98 .. 110, plane z 79.995 | 14.4 98 79.995 | 180, 0 | 192, 43.6364 |

Each close-up camera is placed so that its whole view lies inside the panels. Panels are summoned before the section that needs them and killed when it is
done. The wide FOV 70 shots (`shaders_sodium`, `shaders_iris_*`), the target shots and the screen shots have **no panel**: the quartz wall stays as it is.

**Close-ups** are the shots that fill the frame with one subject: `outline_static` and `hero_armor`. They use the vanilla FOV option **30** (Options > Video
Settings > FOV, or the Director `options fov=30`); the camera distances below were computed for it. Vertical FOV f at d blocks gives 1080 / (2 d tan(f / 2))
pixels per block on the 1080 px high capture. Wide shots (shaders, targets, the screens, `hero_firstperson`) stay at FOV 70 (the first person hand is drawn with
a fixed FOV of 70 anyway).

Outline thickness is in framebuffer pixels and does not grow with the subject. Since 2.1.0 the hero shots and the item frame shot use the built-in default thickness (7.5 px).

Expected subject sizes on the 1920 x 1080 capture (from the generator, not measured; the first full run measured the real capture at about 0.885 of the formula):

| Shot | Camera `x y(feet) z (yaw, pitch)` | d | FOV | Subject | Expected px high |
| --- | --- | --- | --- | --- | --- |
| `outline_static` | 13.5 100.88 10.59 (180, 0) | 9.54 | 30 | netherite sword in the frame at (13, 102); the 0.75 block frame is as high as the 160 px cell | 95 |
| `hero_armor` | 12.5 99.705 75.7 (0, 0) | 3.50 | 30 | stand at (12.5, 79.2), helmet top to chestplate bottom | 720 (67 %) |
| `hero_firstperson` | 12.5 100 74.7 (0, 10) | 5.30 | 70 | held sword (hand FOV is fixed 70) | - |
| `shaders_sodium`, `shaders_iris_world` | 18.5 99.73 42.1 (180, 0) | 3.60 | 70 | whole stand | 423 (39 %) |

Coordinates: `/tp @s X Y Z YAW PITCH` with **Y = feet**, eye = Y + 1.62, yaw 0 = south (+Z), 90 = west (-X), 180 = north (-Z),
-90 = east (+X), pitch positive = looking down. "d" is the distance from the camera to the subject (the item plane of a frame lies 0.05 block in front of its wall).

### Hero shots rig (differs from the table above)

The two hero shots (`hero_armor`, `hero_firstperson`, section 3) are product photos, not showroom shots, and use their own rig:

| Setting | Value |
| --- | --- |
| FOV | **30**, the vanilla FOV option (the slider minimum; no override needed), except `hero_firstperson` (70, see its row) |
| Backdrop | **white**: the hero room H is a closed box of `snow_block`, and the stand stands in front of a full-bright white panel on the south wall, see "Backdrop and close-ups". Nothing else in frame: no corner, no ceiling, no pillar |
| Light | bright and even: gamma 1.0 + Night Vision, every air cell of room H is a level 15 light block (invisible, so none shows in a picture), `/time set noon`. The glint and the outline must still read clearly against the white |
| HUD | F1 (hidden), GUI hidden, no crosshair, mouse pointer parked at the very top left |
| Resolution | 1920 x 1080 framebuffer, unscaled |
| Framing | the subject is **centred** and leaves a margin of at least 10 percent of the height on every side |
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

Room A (the item frame shot and the screens, z 0..16) and room B (stand, dropped items, hand shots, z 32..48). A bright shell of smooth quartz with a
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

The backdrop panels of the previous section are summoned in front of the walls.

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
# the armor stand of room B for the Sodium and Iris shots, standing at (18.5, 100, 38.5), facing south (towards the camera at z 44.25)
/summon minecraft:armor_stand 18.5 100 38.5 {Rotation:[0f,0f],ShowArms:1b,NoBasePlate:1b,Invulnerable:1b,NoGravity:1b,Pose:{LeftArm:[0f,0f,-12f],RightArm:[-18f,0f,12f]},ArmorItems:[{boots},{leggings},{chestplate},{helmet}],HandItems:[{netherite_sword},{}]}
```

Armor and sword are the enchanted netherite pieces of the table above (ArmorItems order is boots, leggings, chestplate, helmet).
The hero stand stands at (12.5, 100, 79.2) in room H, in front of the south wall panel, facing **north**
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
| `GUION` | `{"outline": {"targets": {"gui": true}}}` (since 2.1.0 the built-in default is `true` as well; BASE sets it `false`, so the shots that show the inventory switch it on) |
| `ARMOR_ON` | `{"outline": {"targets": {"armor": true}}}` (worn armor and the elytra are outlined with this on; since 2.1.0 the built-in default is also `true`, and BASE does not follow the defaults, so the shots keep it explicit. Item frames are not the armor target, they follow `itemFrame`) |

**Glint scale.** The capture pack's block atlas is 8192 x 4096, so at the default scale 1.0 an item sprite shows only a few texels of
the 128 px preset pattern and the glint reads as a flat colour wash. Every in-world shot with a preset glint therefore uses `GL(p)` as written
above (scale 0.13 item, 0.22 armor and entity, 0.1 sparkle, strength 1.0, the maximum): the four Sodium and Iris
shots (`hero_armor` and `hero_firstperson` did until 2.1.0). The vanilla baselines (the other target shots) keep vanilla settings; since 2.1.0 the
eight shots of the note in section 3 (among them `hero_armor`, `hero_firstperson` and `config_glint`) use the built-in default config instead. The studio
renders carry their own look settings (section 4).

Colours (deeper than in the first version, because pale colours vanish on white): violet `#A050F0` (default, last stop of the logo gradient), amber `#FF8A1F`,
ice `#1AA6DC`, gold `#F0A500`, red `#FF4D4D`; logo gradient `GRAD_LOGO` = mint `#1FC77A`, blue `#3D6BFF`, violet `#A050F0`. The default enchantment colours of the stand's
helmet and boots are pale, so the Void look adds `ENCH_DEEP` = `{"enchantmentColors": {"minecraft:feather_falling": "#2F8FE8", "minecraft:respiration": "#12A8C0"}}`.

Changing `gapFix` needs a resource reload (F3 + T, wait until it is done).

## 3. In-world captures (14)

Since 2.1.0 eight of these shots (`outline_static`, `hero_armor`, `hero_firstperson`, `target_third_person`, `target_dropped`, `target_gui`, `config_outline`,
`config_glint`) are taken with the mod's built-in default config (owner defaults, no `glintsmith.json` profile overrides) by the `gallerydef` capture script, run
2026-10-19. The other in-world captures keep their BASE profiles. Where a row below still names BASE or a macro, it applies to those other shots only.

All camera lines assume the rooms above. "Crop" is where the card looks (source pixels, centred unless given); it only
has to be roughly right, the lead moves the crop in `cards.json` after looking at the capture.

### Item frame (1)

One frame at X 13, Y 102 holding `minecraft:netherite_sword` with sharpness 5, unbreaking 3, mending 1, in room A with the room A panels behind the wall.
Spectator, FOV 30, `tp 13.5 100.88 10.59 180 0`, d 9.54, the frame centred on the screen (the sword is about 95 px high, the 0.75 block frame 160 px).
The glint is the default one (neutral with the rainbow tint, since 2.1.0). The card crops 154 x 160 native, centred on the item frame (the other frames at the left edge x < 100 are out of the crop).

| File | Config |
| --- | --- |
| `outline_static.png` | the mod's built-in default config (2.1.0 owner defaults: outline static `#E6B2FF`, 7.5 px, 25 % opacity, on for armor and GUI; neutral glint with the rainbow tint at strength 0.9 on all four layers), no profile; re-shot 2026-10-19. Used by the Item frames cell of `card_targets` |

### Hero shots (2)

The hero shots of `card_targets`. Rig: "Hero shots rig" in section 0 (FOV 30, white snow-block room H with a full-bright white wall panel, bright light, HUD hidden,
1920 x 1080). Every shot is a single unedited screenshot, subject centred.

| File | In frame | Scene, camera | Config |
| --- | --- | --- | --- |
| `hero_armor.png` | the armor stand's netherite helmet and chestplate, head and chest centred, white behind, about 720 px from helmet top to chestplate bottom (67 percent). The card crops `[585,100,750,800]` (head to the top of the leggings; the stand continues below the picture) | room H: the stand at (12.5, 100, 79.2) facing north (`Rotation:[180f,0f]`, otherwise the summon of section 1) in front of the south wall panel. Spectator, `tp 12.5 99.705 75.7 0 0` (the camera looks south), d 3.50 | the mod's built-in default config (2.1.0 owner defaults: outline static `#E6B2FF`, 7.5 px, 25 % opacity, on for armor and GUI; neutral glint with the rainbow tint at strength 0.9 on all four layers), no profile; re-shot 2026-10-19 |
| `hero_firstperson.png` | first person, the netherite sword in the right hand, blade pointing up and left across the picture, whole blade in frame, hand at the lower right, the white wall behind. The card crops `[98,0,1822,1080]` (the sword is cut by the right edge of the capture itself) | room H, **creative, hand visible**, F1, **FOV 70** (the hand is drawn with a fixed FOV of 70, so the option is put back to 70 first). `/item replace entity @s weapon.mainhand with minecraft:netherite_sword[ench]`, `tp 12.5 100 74.7 0 10` (about 5.3 blocks from the south wall, the camera looks south at the south wall panel) | the mod's built-in default config (2.1.0 owner defaults: outline static `#E6B2FF`, 7.5 px, 25 % opacity, on for armor and GUI; neutral glint with the rainbow tint at strength 0.9 on all four layers), no profile; re-shot 2026-10-19 |

### Targets (3, plus `outline_static.png` and the two hero shots above)

The built-in default config for all of them (`target_gui` and the armor and item frame cells included; since 2.1.0 the GUI and armor targets are on by default, so no `GUION` is needed). All three are wide shots at FOV 70 in the quartz room B with no panel. The card is a layout of five cells plus the inventory at native size (see `cards.json`). The first person and armor cells of this card are the hero shots `hero_firstperson.png` and `hero_armor.png`, the item frames cell is `outline_static.png`.

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

### Rules and config screens (4)

All four: GUI scale 2, FOV 70, open with `/glintsmith`, F1 not needed. Spectator in room A at `tp 21 100 8 -90 0`, looking at the plain east wall so the world
behind the screen is the white quartz wall (no panel; the screen darkens the world behind it). The builder halves these (crops 904 wide, 540 to 630 high, see `cards.json`) so the whole screen should sit inside the
middle 904 x 600 of the capture; if the screen is larger, tell the lead.

| File | Screen | Config |
| --- | --- | --- |
| `rules_tab.png` | Rules tab, the four rules below visible, none selected | BASE + the four rules |
| `rule_edit.png` | the editor of rule 1 (press Edit): name, items `#minecraft:swords`, the outline override with the red colour and thickness 3 visible | BASE + the four rules |
| `config_outline.png` | Outline tab, top of the page, nothing hovered; since 2.0.0 the screen also shows the live preview panel (the held netherite sword with the outline). Re-shot 2026-10-19 by the `gallerydef` capture script (1920 x 1080); it shows the 2.1.0 built-in default config (static `#E6B2FF`, 7.5 px, outline on for armor and GUI) | the mod's built-in default config (2.1.0 owner defaults: outline static `#E6B2FF`, 7.5 px, 25 % opacity, on for armor and GUI; neutral glint with the rainbow tint at strength 0.9 on all four layers), no profile |
| `config_glint.png` | Glint tab, layer Item, the default neutral texture with the rainbow tint at strength 90 %, live preview panel showing the sword with the default glint and the outline. Re-shot 2026-10-19 by the `gallerydef` capture script (1920 x 1080); it shows the 2.1.0 built-in default config | the mod's built-in default config (2.1.0 owner defaults: outline static `#E6B2FF`, 7.5 px, 25 % opacity, on for armor and GUI; neutral glint with the rainbow tint at strength 0.9 on all four layers), no profile (the screen shows the values) |

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

## 4. Studio renders

These come from the dev-only studio (`docs/STUDIO.md`), not from screenshots. Run `./gradlew runStudio -Pscene=<scene>` once per scene; each writes
`art/captures/studio/<scene>/` (git-ignored). **The names, sizes and shots below are the contract** the gallery builds from: `py -B art/studio/studio.py check` validates the
folders, `py -B art/gallery/build.py --list` shows which ones are still missing, and `py -B art/studio/synth_studio.py DIR --contract` writes synthetic stand-ins of all eleven
scenes (to test the layout; see `art/README.md`).

Every frame is a pair `NNNN_k.png` (over black) and `NNNN_w.png` (over white), `width * supersample` by `height * supersample` pixels, and the scene's
`scene.json` lists the frames (`look`, `caption`, `name`). The matte, the cream backdrop and the soft shadows are made by `art/studio/`; the subject must
therefore stand **clear of the frame edges** (the card builders fit the whole frame, so a subject cut by the edge stays cut) and be lit by the studio's key and fill
light, with no ground or background of its own.

### Turntables (3, plus `logo_anim_a`)

A turntable is every frame of every look, in order, `name` null. A GIF plays them at the scene's fps and loops, so one revolution per look.

| Scene | Size | Content | GIF made from it |
| --- | --- | --- | --- |
| `hero_sword` | 512 x 512 | one enchanted netherite sword, 3/4 pitch, 5 to 6 looks (vanilla, the default look of 2.1.0 (`"base": "defaults"`: the mod's own built-in config, nothing else), rainbow glint and outline, fire glint and pulse, nebula glint and gradient, inferno glint and soft glow), 75 frames per look at 30 fps (one turn per look), a caption per look | `showcase.gif` (384 px wide with the caption bar), at most 2,000,000 bytes |
| `lineup` | 1200 x 400 | six items in a row (sword, pickaxe, trident, shield, chestplate, bow), each with its own glint and outline, swinging +-35 degrees of yaw (not a full turn) over 120 frames, one look, caption "Every item its own look" | `lineup.gif` (810 x 270 picture with the caption bar, at most 2,000,000 bytes) |
| `armor_stand` | 480 x 600 | a stand in full enchanted netherite armor holding the sword, the armor outline target on, 3 to 4 looks (vanilla, the default look of 2.1.0 (`"base": "defaults"`), rainbow glint and outline, inferno glint and soft glow), a caption per look | `armor.gif` (320 x 400 picture with the caption bar, at most 2,000,000 bytes) |
| `logo_anim_a` | 256 x 256, supersample 4 | the pose of `concept_a`, not turning, 165 frames at 20 fps: fire glint at speed 10 and a violet / magenta / blue gradient outline, both repeating after exactly 8.25 s (a seamless loop) | `logo_animated.gif` (256 px, no caption, at most 2,000,000 bytes), made by `art/logo.py` |

A GIF that does not fit its budget steps its frame rate down and then its picture size down (`art/studio/pipeline.py`); the build says which step it took.

### Still scenes (7)

A still scene has **one frame (one K / W pair) per shot**; the frame's `name` in `scene.json` is the shot name (`studio.py still SCENE SHOT` and `scene.find_frame`
resolve it), and `look` and `caption` may repeat the name. Every shot of a scene has the same size.

| Scene | Size | Shots (frame `name`) | Used by |
| --- | --- | --- | --- |
| `stills_hero` | 1024 x 1024 | `banner_hero`: the netherite sword in the default look of 2.1.0 (`"base": "defaults"`), 3/4 view, the whole sword with at least 8 percent margin; `logo`: the old logo still (aurora glint, violet outline), no longer used | `banner.png` (the banner picture) |
| `logo_concepts` | 512 x 512 | `concept_a`: the golden blade, an enchanted golden sword on the diagonal (fire glint, violet outline), yaw 20, pitch 8, filling about 90 % of the frame, a big silhouette that must read at 32 px; `concept_b` (crossed gold and diamond swords, fire and frost) and `concept_c` (diamond sword on an anvil): the logo concepts not chosen, for reference | `art/logo.py` (logo 512, icon 128, logo 32) |
| `stills_wide` | 1200 x 400 | `universal_vanilla`: six vanilla items in a row (sword, bow, trident, shield, elytra, chestplate), outlined; `universal_modded`: six items of mods (`cataclysm:black_steel_targe`, `eternal_starlight:glistering_morning_star`, `advancednetherite:netherite_diamond_ingot`, `immersive_armors:warrior_helmet`, `immersive_armors:warrior_chestplate` and one more of the pack), outlined, `[glint]` | `card_universal.png` (two rows; the builder trims the empty rows above and below each row and sets both rows at the same scale, so the items may sit in the middle band of the frame) |
| `stills_tiles` | 512 x 512 | fourteen glint shots `glint_vanilla`, `glint_neutral` (with the default tint: rainbow, strength 0.9), the seven clean presets `glint_rainbow`, `glint_aurora`, `glint_fire`, `glint_frost`, `glint_sparkle`, `glint_gold`, `glint_void` and the five rich presets `glint_inferno`, `glint_nebula`, `glint_starlight`, `glint_opal`, `glint_storm` (the same item, outline off, `glint_vanilla` is the vanilla glint) and ten outline styles `outline_vanilla` (nothing of the mod), `outline_static` (the default colour `#E6B2FF`, 3 px on the card; see below), `outline_rainbow`, `outline_pulse`, `outline_gradient`, `outline_enchant` (the colours of the item's enchantments), `outline_thick_1`, `outline_thick_3`, `outline_thick_6`, `outline_soft_80` (softness 0.8). The outline shots are rendered from a farther camera (`view_height` 3.6, the sword 185 px high in the 512 px frame) and their thickness is set in matte pixels so that the outline measures about 3 card pixels (1, 3 and 6 for the thickness tiles) when the 235 x 213 centre crop is set at 176 x 160 on `card_outline_styles.png` (scale 0.749: a studio thickness of 4 gives 3 card px, 1.34 gives 1, 8 gives 6; the config caps the thickness at 16 render pixels, i.e. 8 matte pixels at supersample 2, which is why the sword is small in the frame). The glint shots use the tuned `GL(p)` scale of section 2 (a pattern, not a wash). One item for all twenty-four, same camera, centred, at least 12 percent margin on every side | `card_glint_presets.png` (14 tiles), `card_outline_styles.png` (10 tiles); each tile is shown at 150 px, so the subject must be large in the frame and the outline thick enough to read at that size |
| `stills_default` | 1024 x 1024 | `default_vanilla` (nothing of the mod) and `default_look` (the mod's built-in defaults of 2.1.0, `"base": "defaults"` with no override: a pale violet outline `#E6B2FF`, 7.5 px, opacity 25 %, proportional, and the neutral glint with the rainbow tint at strength 0.9, scale 1), the stills_hero sword and camera (yaw 28, pitch 22), the same clock time 7.1 s in both (a violet tint) | `card_default_look.png` (left and middle) |
| `stills_default_armor` | 480 x 600 | `default_armor`: the armor stand of `armor_stand` in full enchanted netherite armor with the enchanted sword, the default look of 2.1.0 (`"base": "defaults"`), yaw 20, the same clock time as `stills_default` | `card_default_look.png` (right) |
| `stills_gap` | 800 x 800 | `gap_sword_off`, `gap_sword_on` (a diamond sword), `gap_modded_off`, `gap_modded_on` (an enchanted netherite sword and an enchanted `immersive_armors:warrior_chestplate`): outline off, vanilla glint on (`glint: true`), the same clock time in every shot, the gap fix off versus auto, the same oblique camera (yaw 30, pitch 45, perspective, supersample 1), a macro in which the cracks are visible | `card_gap_fix.png` (four 150 x 66 matte pixel crops, enlarged 3x by nearest neighbour with the element's `crop` + `up`) |

The order of the frames inside a still scene does not matter (the shot name is looked up), but the indexes must run 1..N without gaps.

## 5. Checklist

1. Rooms built, stand and frames summoned, rig options set, GUI scale 2 (in-world captures).
2. Capture the 14 in-world shots (no gap fix switch is needed any more).
3. Render the studio scenes: `./gradlew runStudio -Pscene=hero_sword` and the same for `lineup`, `armor_stand`, `stills_hero`, `stills_wide`, `stills_tiles`, `stills_gap`, `stills_default`, `stills_default_armor`, `logo_concepts`, `logo_anim_a`.
   Then `py -B art/studio/studio.py check` (every scene ok) and `py -B art/gallery/build.py --list` (nothing MISSING).
4. `py -B art/logo.py` (logo, icon, animated logo), `py -B art/gallery/build.py --strict` (cards and the four GIFs; the GIFs take minutes), and look at each image; move crops in `cards.json` where the subject is off.
5. `py -B art/verify.py --full`, `py -B docs/curseforge/preview.py`.

Counts: 14 in-world captures (hero 2, item frame 1, targets 3, rules 2, config 2, Sodium/Iris 4) and 11 studio scenes (4 turntables, 7 still scenes with 2 + 3 + 2 + 24 + 4 + 2 + 1 = 38 named shots).

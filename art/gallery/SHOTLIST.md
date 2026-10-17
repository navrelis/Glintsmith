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
| Mods | the test pack with `advancednetherite`, `eternal_starlight`, `cataclysm`, `immersive_armors`; Sodium if the pack has it (leave it on); Iris installed, **no shader pack selected** except in the four shader shots |
| Resource packs | none besides the mods' own |
| Options | FOV 70 (vertical) for the wide shots, **FOV 30 for every close-up** (the vanilla FOV slider minimum, no override needed; see "Backdrop and close-ups" below), render distance 12, view bobbing off, entity shadows off, particles minimal, brightness **Bright (gamma 1.0)**, mipmap levels 4, graphics Fancy, clouds off, language English (US) |
| GUI scale | **2** for every shot (world shots hide the GUI with F1 anyway). GUI shots rely on 2 so that the builder can halve them losslessly (`down: 2`) |
| Account | offline default skin (Steve), name `GS` |
| HUD | F1 on (hidden) for every world shot; GUI shots show the screen only; the mouse pointer is parked at the very top left of the window |
| Light | every capture is shot **bright**: gamma 1.0 (Bright) **and** Night Vision (`/effect give @s minecraft:night_vision infinite 0 true`, give it again after a respawn), plus light level 15 in every room (the light blocks below), so the white backdrop really is white and nothing is dim or shaded |
| Warm-up | after every `tp`, config change or screen change wait 2 s (40 ticks) before the screenshot |
| Animated colours | rainbow, pulse, gradient and enchantment colours move with time; for those take the shot when the colour looks best (three screenshots 0.4 s apart is fine, keep one) |
| Output | `art/captures/<name>.png`, 8-bit RGB or RGBA PNG (the game's own screenshot format) |

### Backdrop and close-ups

**The backdrop is white / cream, never black.** The gallery is a light theme (`art/README.md`, "Theme"): the captures sit on cream cards inside a thin
dark edge, so the studio and the rooms are built of white blocks and lit to full brightness. Hero studio: white blocks (`snow_block`). Rooms A and B:
smooth quartz with a birch floor. No dark block may show behind a subject or a screen.

**Close-ups** are the shots that fill the frame with one subject: the seven hero shots, the glint presets, the outline styles and the gap-fix pairs.
They use the vanilla FOV option **30** (Options > Video Settings > FOV; no command or override needed). The camera lines below (`d about ...`, `tp ...`)
were written for FOV 70, the hero shots for FOV 25. At FOV 30 keep the point looked at and move the camera along the same line to
d x tan(35 deg) / tan(15 deg) = **d x 2.6** of the FOV 70 values (hero shots, from FOV 25: d x 0.83), or move in or out until the subject fills the crop
as the shot says. The rooms are 17 blocks deep, so the glint presets (d 2.2 -> 5.7) and the outline styles (d 3.6 -> 9.4) fit. Wide shots (the universal wall,
third person, dropped items, the GUI shots, the showcase GIF) stay at FOV 70.

Coordinates: `/tp @s X Y Z YAW PITCH` with **Y = feet**, eye = Y + 1.62, yaw 0 = south (+Z), 90 = west (-X), 180 = north (-Z),
-90 = east (+X), pitch positive = looking down. "d" is the distance from the camera to the subject.

### Hero shots rig (differs from the table above)

The seven hero shots (section 3, "Hero shots") are product photos, not showroom shots, and use their own rig:

| Setting | Value |
| --- | --- |
| FOV | **30**, the vanilla FOV option (the slider minimum; no override needed). The distances below were written for FOV 25: at 30 multiply them by tan(12.5 deg) / tan(15 deg) = 0.83 (the framing rule of each shot wins over the number) |
| Backdrop | **white blocks**: the hero room H (section 1) is a closed box of `snow_block`, nothing else in frame |
| Light | bright and even: gamma 1.0 + Night Vision, light blocks (level 15) **out of frame** (behind or above the camera, never in the picture), `/time set noon`. The glint and the outline must still read clearly against the white |
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
/time set noon
/weather clear
/difficulty peaceful
/tp @s 12 101 8
```

Room A (item frame shots, z 0..16) and room B (stand, dropped items, hand shots, z 32..48). A bright shell of smooth quartz with a
birch floor, stripped birch corner columns and an invisible ceiling of light blocks (level 15) for even, full light:

```
# room A
/fill 0 98 0 24 109 16 minecraft:smooth_quartz hollow
/fill 1 99 1 23 99 15 minecraft:birch_planks
/fill 1 100 1 1 108 1 minecraft:stripped_birch_log
/fill 23 100 1 23 108 1 minecraft:stripped_birch_log
/fill 1 100 15 1 108 15 minecraft:stripped_birch_log
/fill 23 100 15 23 108 15 minecraft:stripped_birch_log
/fill 2 108 2 22 108 14 minecraft:light[level=15]
# room B (same, 32 blocks further south)
/fill 0 98 32 24 109 48 minecraft:smooth_quartz hollow
/fill 1 99 33 23 99 47 minecraft:birch_planks
/fill 1 100 33 1 108 33 minecraft:stripped_birch_log
/fill 23 100 33 23 108 33 minecraft:stripped_birch_log
/fill 1 100 47 1 108 47 minecraft:stripped_birch_log
/fill 23 100 47 23 108 47 minecraft:stripped_birch_log
/fill 2 108 34 22 108 46 minecraft:light[level=15]
```

Room H (the hero studio, z 64..80) is a plain white box of snow blocks; its light blocks sit behind the camera positions of the hero shots:

```
/fill 0 98 64 24 109 80 minecraft:snow_block hollow
/setblock 12 104 68 minecraft:light[level=15]
/setblock 9 104 70 minecraft:light[level=15]
/setblock 12 104 73 minecraft:light[level=15]
/time set noon
```

The light blocks stay behind the camera lines of the hero shots below (z 67 and further south); if one shows up in a picture, move it.
Frames for hero shots are invisible: add `Invisible:1b` to the frame summon.

The walking floor is y = 100. Light blocks are invisible unless you hold one. Use `/gamemode spectator` for the camera shots that
say "spectator" (no hand, free flying) and `/gamemode creative` or `survival` where a shot says so.

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
# item frame on the WEST wall of room A (wall block at x = 0, frame block x = 1), Z Y = frame block
/summon minecraft:item_frame 1.5 Y.5 Z.5 {Facing:5b,Invulnerable:1b,Fixed:1b,Item:{...}}
# the armor stand of room B, standing at (18.5, 100, 38.5), facing south (towards the camera)
/summon minecraft:armor_stand 18.5 100 38.5 {Rotation:[0f,0f],ShowArms:1b,NoBasePlate:1b,Invulnerable:1b,NoGravity:1b,Pose:{LeftArm:[0f,0f,-12f],RightArm:[-18f,0f,12f]},ArmorItems:[{boots},{leggings},{chestplate},{helmet}],HandItems:[{netherite_sword},{}]}
```

Armor and sword are the enchanted netherite pieces of the table above (ArmorItems order is boots, leggings, chestplate, helmet).

## 2. Glintsmith config

The config file is `config/glintsmith.json`; it reloads while the game runs (or run `/glintsmith reload`). `/glintsmith` opens the
settings screen. Every shot lists a profile; write the whole profile (deep merge over the defaults) before the shot. **`rules` is
`[]` in every profile except the two Rules shots**, so the default example rules do not interfere.

**BASE** (the plain starting point, outline on):

```json
{"rules": [], "outline": {"enabled": true, "condition": "ENCHANTED", "color": {"mode": "STATIC", "colors": ["#D088FC"]},
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
| `GL(p)` | `{"glint": {"enabled": true, "item": {"texture": "preset:p"}, "armor": {"texture": "preset:p"}, "entity": {"texture": "preset:p"}, "gui": {"texture": "preset:p"}}}` (all four layers) |
| `GUION` | `{"outline": {"targets": {"gui": true}}}` |
| `ARMOR_ON` | `{"outline": {"targets": {"armor": true}}}` (worn armor and the elytra are outlined only with this; the default is `false`. Item frames are not the armor target, they follow `itemFrame`) |

Colours: violet `#D088FC` (default), amber `#FF8A1F`, ice `#7DE8FF`, gold `#FFD36B`, red `#FF4D4D`.

Changing `gapFix` needs a resource reload (F3 + T, wait until it is done).

## 3. Stills (42)

All camera lines assume the rooms above. "Crop" is where the card looks (source pixels, centred unless given); it only
has to be roughly right, the lead moves the crop in `cards.json` after looking at the capture.

### Universal (1)

| File | Scene, camera, mode | Config | Notes |
| --- | --- | --- | --- |
| `universal_wall.png` | Room A, two rows of five frames. Y 102 (vanilla) X 4..8: `diamond_sword`, `bow`, `trident`, `shield`, `elytra`. Y 103 (modded) X 4..8: `cataclysm:black_steel_targe`, `eternal_starlight:glistering_morning_star`, `advancednetherite:netherite_diamond_ingot`, `immersive_armors:warrior_helmet`, `immersive_armors:warrior_chestplate`. Spectator, `tp 6.5 101.38 6.2 180 0`, d about 5.1 | BASE + `T(3)` | crop 920 x 300 centred: exactly the two rows (about 150 px per block) |

### Hero shots (7)

The big images: they make the logo, the icon, `logo_animated.gif`, the banner and two cells of `card_targets`. Rig: "Hero shots rig" in
section 0 (FOV 30, white snow-block room H, bright light with the light blocks out of frame, HUD hidden, 1920 x 1080). Every shot is a single unedited
screenshot, subject centred. Outline and glint are the real mod; take animated colours at their best moment (three screenshots 0.4 s apart,
keep one). The three sword shots and the logo shot use one invisible item frame at block `(12, 102, 65)` on the north wall of room H holding
`minecraft:netherite_sword` with sharpness 5, unbreaking 3, mending 1; the camera is spectator at x 12.5 looking north (yaw 180, pitch 0), eye
height 102.5 (feet 100.88), d about 1.8 blocks: `tp 12.5 100.88 67.3 180 0`. Move the camera in or out until the sword (its diagonal) fills about
70 percent of the picture height and stays at least 10 percent away from every edge.

| File | In frame | Scene, camera | Config |
| --- | --- | --- | --- |
| `hero_sword_rainbow.png` | the netherite sword, centred, diagonal across the picture | sword frame as above | BASE + `GL("rainbow")` + `T(3)` + `OUT("RAINBOW", [], 1)`. Used for the banner picture (the middle 1080 x 1080, shown at 180 x 180) and as the first look of `logo_animated.gif` |
| `hero_sword_void.png` | the same sword, same camera, same framing as `hero_sword_rainbow` (no camera move between the shots) | same | BASE + `GL("void")` + `T(3)` + `OUT("ENCHANTMENT", [], 1)`. Second look of `logo_animated.gif` |
| `hero_sword_fire.png` | the same sword, same camera, same framing | same | BASE + `GL("fire")` + `T(4)` + `OUT("PULSE", ["#FF8A1F"], 1)`, shot at the bright phase of the pulse. Third look of `logo_animated.gif` |
| `logo_sword.png` | **the logo**: the sword exactly centred on the picture centre, blade diagonal about 70 percent of the height. The centred 1080 x 1080 square must read on its own at 128 px and 32 px, so keep the silhouette simple and nothing else in frame (square framing) | same frame and camera as the three above | BASE + `GL("aurora")` + `T(3)` + `OUT("GRADIENT", ["#7DFFB2", "#7D9BFF", "#D088FC"], 1)`. The icon is the area-filtered square crop of this picture. If one of the three shots above is already exactly centred and strong, it may be saved a second time as `logo_sword.png` |
| `hero_armor.png` | the armor stand's netherite helmet and chestplate, head and chest centred, white behind. The card crops the middle 1480 x 800 | room H: `/summon minecraft:armor_stand 12.5 100 66.5 {Rotation:[0f,0f],ShowArms:1b,NoBasePlate:1b,Invulnerable:1b,NoGravity:1b,Pose:{LeftArm:[0f,0f,-12f],RightArm:[-18f,0f,12f]},ArmorItems:[{boots},{leggings},{chestplate},{helmet}],HandItems:[{netherite_sword},{}]}` (armor and sword as in section 1). Spectator, `tp 12.5 99.83 69.4 180 0`, d about 2.9 | BASE + `GL("rainbow")` + `T(3)` + `OUT("RAINBOW", [], 1)` + `ARMOR_ON` |
| `hero_lineup.png` | six enchanted items in a row, evenly spaced, vertically centred, each about 150 px high; the row spans the middle 1832 px of the width and the middle 200 px of the height (the banner strip) | six invisible frames at Y 102, X 6..11 on the north wall of room H (frame blocks at `z = 65`): `diamond_sword[ench]`, `bow[ench]`, `shield[ench]`, `cataclysm:black_steel_targe[glint]`, `eternal_starlight:glistering_morning_star[glint]`, `immersive_armors:warrior_chestplate[glint]`. Spectator, `tp 9.0 100.88 73.5 180 0`, d about 8 | BASE + `GL("aurora")` + `T(3)` + `OUT("RAINBOW", [], 1)` |
| `hero_firstperson.png` | first person, the netherite sword in the right hand, blade pointing up and left across the picture, whole blade in frame, hand at the lower right, the white wall behind. The card crops the full width at 1920 x 1038 | room H, **creative, hand visible**, F1, FOV 30. `/item replace entity @s weapon.mainhand with minecraft:netherite_sword[ench]`, `tp 12.5 100 71.0 180 10` (about 6 blocks from the north wall) | BASE + `GL("rainbow")` + `T(3)` + `OUT("RAINBOW", [], 1)` |

Frames and stand summons are the ones of section 1, moved into room H as the table says.

### Glint presets (9)

Scene: room B stand (section 1), spectator, `tp 18.5 99.58 40.7 180 0`, d about 2.2, the camera looks at the netherite chestplate.
Config for all nine: BASE + `NOOUT`, plus the glint patch below. Crop 176 x 160 centred (the chestplate fills it).

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

Scene: room A, one frame at X 13, Y 102 holding `minecraft:netherite_sword` with sharpness 5, unbreaking 3, mending 1.
Spectator, `tp 13.5 100.88 4.7 180 0`, d about 3.6, the frame centred on the screen. Crop 176 x 160 centred. Glint is vanilla
(`glint.enabled = false`) everywhere here. Outline-only changes:

| File | Config |
| --- | --- |
| `outline_vanilla.png` | `VANILLA` (no outline at all, the "before") |
| `outline_static.png` | BASE (violet, 2 px). **Also used by the Item frames cell of `card_targets`** |
| `outline_rainbow.png` | BASE + `T(3)` + `OUT("RAINBOW", [], 1)` |
| `outline_pulse.png` | BASE + `T(3)` + `OUT("PULSE", ["#FF8A1F"], 1)` (shoot at the bright phase) |
| `outline_gradient.png` | BASE + `T(3)` + `OUT("GRADIENT", ["#FF4D4D", "#FFD36B", "#7DE8FF"], 0.5)` |
| `outline_enchant.png` | BASE + `T(3)` + `OUT("ENCHANTMENT", [], 0.5)` (the sword has three enchantments, so three colours blend) |
| `outline_thick_1.png` | BASE + `T(1)` |
| `outline_thick_3.png` | BASE + `T(3)` |
| `outline_thick_6.png` | BASE + `T(6)` |
| `outline_soft_80.png` | BASE + `T(4)` + `{"outline": {"softness": 0.8}}` (soft glow) |

### Targets (3, plus `outline_static.png` above; first person and armor are hero shots)

BASE + `T(3)` for all of them (the GUI one adds `GUION` and uses thickness 2). Crop 296 x 160. The first person and armor cells of this card are the hero shots `hero_firstperson.png` and `hero_armor.png`.

| File | Scene, camera, mode | Notes |
| --- | --- | --- |
| `target_third_person.png` | Room B, creative, sword in hand, F1. `tp 12.0 100 40.5 0 0`, then F5 twice (front view, the player faces the camera) | crop around the sword hand; default crop `[812,420,296,160]` is a guess |
| `target_dropped.png` | Room B slab floor: `/fill 4 100 40 9 100 45 minecraft:smooth_quartz_slab[type=bottom]`, then two floating items: `/summon minecraft:item 6.15 100.62 42.5 {Item:{id:"minecraft:diamond_sword",count:1,components:{...}},NoGravity:1b,Age:-32768,PickupDelay:32767,Motion:[0.0,0.0,0.0]}` and `/summon minecraft:item 6.85 100.62 42.5 {Item:{id:"minecraft:netherite_chestplate",...},...}`. Spectator, `tp 6.5 100.48 39.0 0 20` | both items fit in the crop; items bob, take the shot with both clearly outlined |
| `target_gui.png` | Survival, standard inventory open (E), GUI scale 2, recipe book closed. Fill the hotbar and the first inventory row with enchanted items (below). Config adds `GUION` | the card uses `crop [c,500,592,320]` halved: main inventory and hotbar visible with outlines |

Inventory content (`/item replace entity @s hotbar.0 with ...`, `inventory.0` is the first row of the main inventory):
hotbar 0..8 = `diamond_sword[ench]`, `netherite_pickaxe[ench]`, `bow[ench]`, `trident[ench]`, `shield[ench]`, `elytra[ench]`,
`enchanted_golden_apple`, `cataclysm:black_steel_targe[glint]`, `eternal_starlight:glistering_morning_star[glint]`;
inventory 0..8 = `immersive_armors:warrior_helmet[glint]`, `immersive_armors:warrior_chestplate[glint]`,
`advancednetherite:netherite_diamond_ingot[glint]`, `netherite_helmet[ench]`, `netherite_chestplate[ench]`,
`enchanted_book` (stored mending), `totem_of_undying`, a plain `diamond_sword` (no enchantments, so no outline), `iron_pickaxe[ench]`.

### Gap fix, before and after (4)

The seam is MC-73186: hairline gaps between the faces of flat generated item models, most visible at an oblique angle under the
vanilla glint. **Outline off, vanilla glint on** (`NOOUT`, `glint.enabled = false` so the vanilla glint draws), so nothing hides the seam.
The "off" shots use `gapFix.enabled = false`, the "on" shots `gapFix.enabled = true` (AUTO); reload resources (F3 + T) after each switch,
and take all "off" shots first, then all "on" shots, from the **identical** camera.

Scene: room A, west wall. Sword frame at block (1, 102, 6), ingot frame at block (1, 102, 10). Spectator, FOV 30.

| File | Scene, camera | Notes |
| --- | --- | --- |
| `gap_sword_off.png` / `gap_sword_on.png` | `minecraft:diamond_sword[ench]` in the frame at (1, 102, 6); `tp 2.1 100.88 7.7 140 0` (about 1.6 blocks away, 40 degrees off the wall normal) | macro: the item is about 600 px across. Crop 452 x 150 centred on the diagonal of the blade |
| `gap_modded_off.png` / `gap_modded_on.png` | `advancednetherite:netherite_diamond_ingot[glint]` in the frame at (1, 102, 10); `tp 2.1 100.88 11.7 140 0` | same framing |

If the seam is not visible, try other angles (yaw 120..160, 5 degree steps), FOV 20, or `mipmapLevels 0`, and items with many
transparent holes (`minecraft:bow`, `minecraft:fishing_rod`). Whatever item shows the clearest difference may replace the one
above; tell the lead so the card text stays true.

### Rules and config screens (4)

All four: GUI scale 2, open with `/glintsmith`, F1 not needed. Spectator or creative in room A looking at the north wall so the world
behind the screen is the white quartz wall. The builder halves these (crop 904 x 600 → 452 x 300) so the whole screen should sit inside the
middle 904 x 600 of the capture; if the screen is larger, tell the lead.

| File | Screen | Config |
| --- | --- | --- |
| `rules_tab.png` | Rules tab, the four rules below visible, none selected | BASE + the four rules |
| `rule_edit.png` | the editor of rule 1 (press Edit): name, items `#minecraft:swords`, the outline override with the red colour and thickness 3 visible | BASE + the four rules |
| `config_outline.png` | Outline tab, top of the page, nothing hovered | BASE |
| `config_glint.png` | Glint tab, layer Item, texture `preset:aurora` selected (cycle with the arrow button) | BASE + `GL("aurora")` for the item layer only |

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

Same scene and config for all four so they compare: BASE + `T(3)` + `OUT("RAINBOW", [], 1)` + `GL("aurora")` + `GUION` + `ARMOR_ON`
(`handWithShaderPack` true, `disableWithShaders` false). Capture what the game really does and tell the lead if the card text
("glint keeps working, outlines in the inventory work, hand outlines best effort, world outlines step aside") is not true.
The shader pack is any one popular pack that is installed (Complementary Reimagined or BSL, for instance); note its name.

| File | Setup | Camera | Crop |
| --- | --- | --- | --- |
| `shaders_sodium.png` | Sodium on, Iris with **no** pack | room B stand, spectator, `tp 18.5 99.73 42.1 180 0` | 452 x 170 centred |
| `shaders_iris_world.png` | the same with the shader pack active | same camera | 452 x 170 centred |
| `shaders_iris_hand.png` | shader pack active, creative, first person, F1, `/item replace entity @s weapon.mainhand with minecraft:netherite_sword[ench]` (FOV 70 as in the rig) | `tp 12.0 100 41.0 180 12` | 452 x 170 centred; adjust to the sword |
| `shaders_iris_gui.png` | shader pack active, survival, inventory open with the same items as `target_gui.png`, GUI scale 2 | none | `[c,480,904,340]` halved |

## 4. Showcase GIF (112 frames)

`art/captures/showcase/0001.png` .. `0112.png`: seven looks of 4.0 s each, **16 frames per look, 0.25 s apart (source_fps 4)**, 1920 x 1080 as
every other capture, on the same white / cream, bright setup as every other shot. One camera for the whole clip, no mouse or key input between frames.
Per look shoot a burst of about **46-50 shots** and keep the 16 frames that lie 0.25 s apart.

* Scene: room B stand, spectator, F1, `tp 18.5 99.38 43.1 180 0` (d about 4.6): the whole stand fits the middle 720 x 405 of the screen.
  The builder crops `[c,c,720,405]`, adds a 36 px caption bar and keeps every `step`-th frame (with 16 frames at 4 fps `step` is 1: every frame, 0.25 s each). It raises the step by itself if the file would pass 8 MB.
* For each look: write the profile, wait 0.5 s for the reload, then shoot the burst and keep 16 frames. Profiles are BASE plus (for every look except Vanilla also `ARMOR_ON`, so the stand's armor is outlined):

| Frames | Caption (in the GIF) | Config |
| --- | --- | --- |
| 0001-0016 | Vanilla | `VANILLA` |
| 0017-0032 | Outline only | `T(3)` (violet, vanilla glint) |
| 0033-0048 | Rainbow glint + rainbow outline | `GL("rainbow")` + `T(3)` + `OUT("RAINBOW", [], 1)` |
| 0049-0064 | Fire glint + pulsing outline | `GL("fire")` + `T(3)` + `OUT("PULSE", ["#FF8A1F"], 1)` |
| 0065-0080 | Frost glint + soft cyan outline | `GL("frost")` + `T(4)` + `OUT("STATIC", ["#7DE8FF"], 1)` + `softness 0.6` |
| 0081-0096 | Aurora glint + gradient outline | `GL("aurora")` + `T(3)` + `OUT("GRADIENT", ["#7DFFB2", "#7D9BFF", "#D088FC"], 1)` |
| 0097-0112 | Void glint + enchantment colours | `GL("void")` + `T(3)` + `OUT("ENCHANTMENT", [], 1)` |

The stills are cuts only (no interpolation), 16 per look at a fixed 0.25 s interval. The `showcase.gif` entry of `cards.json` must match:
`frames_per_caption` 16, `source_fps` 4, `step` 1 (the GIF builder derives the frame delay, 25 cs, from them).

## 5. Checklist

1. Rooms built, stand and frames summoned, rig options set, GUI scale 2.
2. Capture in this order to avoid resource reloads: all shots that need `gapFix` on, except the gap pair; then the gap "off" pair (reload), then the gap "on" pair (reload).
3. Every file exists with the exact name in `art/captures/` (`py -B art/gallery/build.py --list`). Then `py -B art/logo.py` (logo, icon and animated logo from the hero shots) and look at `src/main/resources/assets/glintsmith/icon.png`.
4. `py -B art/gallery/build.py --strict` and look at each card; move crops in `cards.json` where the subject is off.
5. `py -B art/gallery/gif.py --strict` for the GIF (the card build already calls it); then `py -B art/verify.py`.

Counts: hero shots 7, universal 1, glint presets 9, outline styles 10, targets 3, gap fix 4, rules 2, config 2, Sodium/Iris 4 = **42 stills**; GIF 112 frames (7 looks x 16 frames, 0.25 s apart).

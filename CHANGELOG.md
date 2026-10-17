# Changelog

## 1.0.0 (Fabric 1.21.1)

First release. Client-side only. The jar is `glintsmith-1.0.0.jar`.

### Added

- Item outlines for every item of every mod, computed from the geometry the game really draws (screen-space mask and exact
  distance transform, depth aware). Places: first-person hand, third-person held items, worn armor and elytra, dropped
  items, item frames, and optionally the GUI and other places. Each place has a switch.
- Outline look: thickness 0.5 to 16 screen pixels, softness, opacity, distance fade (0 to 256 blocks), scaling with the window
  height, "only my items", and a low-durability alert.
- Colour styles for outlines and tints: static, rainbow, pulse, gradient (up to 16 colours) and enchantment colours, with a
  built-in colour for every vanilla enchantment, a stable generated colour for other enchantments, and your own overrides.
- Enchantment glint with four independent layers (items, armor, entities, GUI), each can be turned off. Textures: vanilla,
  seven generated presets (rainbow, aurora, fire, frost, sparkle, gold, void), `neutral` for tinting, your own PNG files from
  `config/glintsmith/glints/` (up to 1024 x 1024 px) and resource-pack textures. Tint, speed, scale, strength and rotation.
  Vanilla Glint Speed and Glint Strength still apply.
- Rules by item id, tag, mod, wildcard, negation, enchantment, enchanted state and place, with an exception list. A rule
  overrides the outline and the glint; later rules override earlier ones.
- Model gap fix for flat generated item models: scaling of the UV shrink, side face expansion and indent, optional precise
  spans. Modes off, automatic and custom. Automatic is meant to stay off when the mod `modelfix` is installed.
- Iris support: world outlines are turned off while a shader pack is active, hand outlines are best effort and have a switch,
  GUI outlines work, and there is an option to turn all outlines off under shaders.
- Settings screen on vanilla widgets with tabs Outline, Glint, Model fix, Rules and Enchantments, colour picker and tooltips.
  English and German.
- Mod Menu entry (Mod Menu optional), `/glintsmith` with `reload` and `toggle`, and two key bindings, both unbound.
- `config/glintsmith.json` with live reload (checked about once a second), atomic saving, clamping of out-of-range values and
  repair of invalid ones with a log entry.
- Failsafes: an error in the outline, glint or gap fix code turns that feature off for the session and logs it once.
- Unit tests for the config, rules, colours, glint presets and slot pool, and the gap fix math.

### Notes

- Written from scratch under the MIT licence. No code, texture or data of Custom Glow Enchantment Outline, Model Gap Fix or
  the Rainbow Enchantment Glint and Enchantment Outlines resource packs is used.
- Minecraft 1.21.1 and Fabric only. <!-- VERIFY IN GAME: the in-game results listed in the README and on the CurseForge page -->

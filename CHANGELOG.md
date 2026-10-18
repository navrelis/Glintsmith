# Changelog

## 2.0.0 (Fabric 1.21.1) — unreleased

### Added

- Glint option "Atlas-independent scale" (on by default, also for existing configs): customised item glints keep the
  vanilla pattern density when mods or resource packs make the block texture atlas larger. Before, in big modpacks
  (for example an 8192 x 4096 atlas) a preset at Scale 1.00 looked like a flat colour wash on items and Scale had to be
  lowered to about 0.13 by hand. The correction is read from the block atlas after every resource reload and applies to
  items in hand, on the ground, in item frames and in the GUI; armor and entity models use their own textures and are
  not changed. High-resolution resource packs are not affected. Glints you did not customise stay exactly vanilla. If
  you lowered Scale for a large atlas in 1.0.0, set it back to 1.00.

### Fixed

- With ImmediatelyFast installed, items in inventories, menus and the hotbar kept the vanilla glint whatever the GUI
  layer said: ImmediatelyFast draws the GUI and the batched HUD into its own buffer sources, which bypassed
  Glintsmith's glint swap. Both are hooked now; the HUD batch is still drawn only by ImmediatelyFast, so the hotbar is
  not affected. Without ImmediatelyFast nothing changes.

## 1.0.0 (Fabric 1.21.1)

First release. Client-side only. The jar is `glintsmith-1.0.0.jar`.

### Added

- Item outlines for every item of every mod, computed from the geometry the game really draws (screen-space mask and exact
  distance transform, depth aware). Places: first-person hand, third-person held items, dropped items and item
  frames (on by default), and optionally worn armor and elytra, the GUI and other places (off by default). Each place has a
  switch.
- Outline look: thickness 0.5 to 16 screen pixels, softness, opacity, distance fade (0 to 256 blocks), scaling with the window
  height, "only my items", and a low-durability alert.
- Colour styles for outlines and tints: static, rainbow, pulse, gradient (up to 16 colours) and enchantment colours, with a
  built-in colour for every vanilla enchantment, a stable generated colour for other enchantments, and your own overrides.
- Enchantment glint with four independent layers (items, armor, entities, GUI), each can be turned off. Textures:
  vanilla, seven generated presets (rainbow, aurora, fire, frost, sparkle, gold, void), `neutral` for tinting, your own
  PNG files from `config/glintsmith/glints/` (up to 1024 x 1024 px) and resource-pack textures. A resource-pack texture
  is given as `namespace:folder/path.png` with at least one folder, for example `mymod:textures/glint/x.png`;
  `config:foo.png` and `mymod:glint` are rejected. Tint, speed, scale, strength and rotation. Vanilla Glint Speed and
  Glint Strength still apply.
- Rules by item id, tag, mod, wildcard, negation, enchantment, enchanted state and place, with an exception list. A rule
  overrides the outline and the glint; later rules override earlier ones.
- Model gap fix for flat generated item models: scaling of the UV shrink, side face expansion and indent, optional precise
  spans. Modes off, automatic and custom. Automatic mode slightly widens the side faces and closes the 1 px step-corner cracks of generated item models. Next to the mod `modelfix`, automatic mode stays off, custom mode stacks on top of it, and only precise
  spans are inactive.
- Compatibility with first-person animation mods such as Punchy: the held item keeps its first-person outline and glint rules.
- Compatibility with ImmediatelyFast HUD batching: an outlined GUI item flushes the batch before its outline is drawn.
- Compatibility with Sodium: works; the log has one expected Sodium warning about optimized vertex writing.
- Iris support: world outlines are turned off while a shader pack is active, hand outlines stay on and have a switch
  (the arm hides them), GUI outlines work, and there is an option to turn all outlines off under shaders.
- Iris support for the glint: with Iris installed and shaders off, custom glints also work in the world (armor, held items
  of other entities, item frames, dropped items). With a shader pack active the world and hand glint stay vanilla and the
  GUI glint is customised.
- Settings screen on vanilla widgets with tabs Outline, Glint, Model fix, Rules and Enchantments, colour picker and tooltips.
  English and German.
- Mod Menu entry (Mod Menu optional), `/glintsmith` with `reload` and `toggle`, and two key bindings, both unbound.
- `config/glintsmith.json` with live reload (checked about once a second), atomic saving, clamping of out-of-range values and
  repair of invalid ones with a log entry.
- Failsafes: an error in the outline, glint or gap fix code turns that feature off for the session and logs it once.
- Unit tests for the config, rules, colours, glint presets and slot pool, and the gap fix math.

### Fixed

- With ImmediatelyFast installed, custom glints were never applied to the first-person hand (and, without Iris, to the
  world): ImmediatelyFast replaces the game's main buffer source with its own, which bypassed Glintsmith's glint swap, so
  the held item kept the vanilla glint whatever the Glint tab said. The swap now also hooks ImmediatelyFast's buffer
  source.

### Notes

- Written from scratch under the MIT licence. No code, texture or data of Custom Glow Enchantment Outline, Model Gap Fix or
  the Rainbow Enchantment Glint and Enchantment Outlines resource packs is used.
- Minecraft 1.21.1 and Fabric only.
- Tested in game with Sodium 0.8.13, Iris 1.8.14-beta.1, Punchy 2.8f and ImmediatelyFast 1.6.15, and with the shader
  pack Complementary Unbound with Euphoria Patches. Fabulous graphics was tested in game without a shader pack. Not
  tested in game for 1.0.0: `modelfix` present, and the held items of other players.
- Performance: on one test scene and one machine the outline cost was within run-to-run noise (without the mod 656, 470
  and 531 fps, with the mod 496, 643 and 587 fps). No claim beyond that scene.

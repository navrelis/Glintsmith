# Plan (Glintsmith 1.0.0)

Status: open / in progress / in review / done. Size S = short (1 unit), L = long (2 units).

| # | Task | Definition of done | Deps | Model | Size | Status |
|---|------|--------------------|------|-------|------|--------|
| T1 | Project scaffold | Gradle wrapper + Loom build (mojmap, Java 21), fabric.mod.json (client, MIT), empty client entrypoint, mixin config, JUnit with fabric-loader-junit, .gitignore, LICENSE, `gradlew build` passes, sources generated | - | Sonnet | S | done |
| T2 | Render architecture design | `docs/DESIGN.md`: outline pipeline (universal, any item geometry), glint system, exact 1.21.1 mixin targets, Sodium/Iris strategy, interfaces for T5/T6/T8 | T1 | Opus | L | open |
| T3 | Config, rule engine, colour animation | JSON config with defaults, validation, live reload; rule matching (id, tag, enchantment, mod, any-enchanted, allow/deny); colour evaluator (static, rainbow, pulse, gradient, per-enchantment, durability alert); unit tests pass | T1 | Sonnet | L | open |
| T4 | Model gap fix | UV-shrink control on the blocks atlas, side-quad expansion and indent for generated items, no @Overwrite, vanilla path kept when off, WEST sign bug fixed, no extra quads by default, macOS default, unit test of the math | T1 | Sonnet | S | open |
| T5 | Outline renderer core | Implementation per DESIGN.md; outlines on held, armor, dropped, item frame, GUI items of any mod; fails safe under Iris; builds | T2 | Opus | L | open |
| T6 | Glint system + textures | Generated glint presets (no third-party art), custom textures, tint, speed, scale, strength, per-category settings, per-rule colour; builds | T2, T3 | Sonnet | L | open |
| T7 | Config screen, Mod Menu, keybind, lang en/de | Vanilla-widget screens for all options and rules, colour picker, Mod Menu entry (optional dep), keybind, en_us + de_de complete | T3 | Sonnet | L | open |
| T8 | Wiring and integration | Config drives T4/T5/T6, reload applies live, no dead options | T4-T7 | Sonnet | S | open |
| T9 | Docs and mod page | README, CHANGELOG, mod page summary/description, icon | T8 | Sonnet | S | open |
| T10 | In-game test | Nytheria client with `--add-mod`, Sodium + Iris, screenshots of all features | T8 | Lead | - | open |
| T11 | Final acceptance | Full build/tests, Graphify, report, push | T10 | Lead | - | open |

Parallel: T2 (Opus) with T3 + T4 (Sonnet) after T1. T5 (Opus) with T6/T7 (Sonnet), files disjoint.

## Usage tally (work units)
Planned: Opus 4 (T2 L, T5 L), Sonnet 10. Opus share about 29 %.
Spent so far: Sonnet 3 (two research S, T1 S), Opus 0.

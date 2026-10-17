# Plan (Glintsmith 1.0.0)

Status: open / in progress / in review / done. Size S = short (1 unit), L = long (2 units).

| # | Task | Definition of done | Deps | Model | Size | Status |
|---|------|--------------------|------|-------|------|--------|
| T1 | Project scaffold | Gradle wrapper + Loom build (mojmap, Java 21), fabric.mod.json (client, MIT), empty client entrypoint, mixin config, JUnit with fabric-loader-junit, .gitignore, LICENSE, `gradlew build` passes, sources generated | - | Sonnet | S | done |
| T2 | Render architecture design | `docs/DESIGN.md`: outline pipeline (universal, any item geometry), glint system, exact 1.21.1 mixin targets, Sodium/Iris strategy, interfaces for T5/T6/T8 | T1 | Opus | L | in review (DESIGN.md accepted with lead deviations, see handoff) |
| T3 | Config, rule engine, colour animation | JSON config with defaults, validation, live reload; rule matching (id, tag, enchantment, mod, any-enchanted, allow/deny); colour evaluator (static, rainbow, pulse, gradient, per-enchantment, durability alert); unit tests pass | T1 | Sonnet | L | done; correction accepted (129 tests, commit pending quiet build) |
| T4 | Model gap fix | UV-shrink control on the blocks atlas, side-quad expansion and indent for generated items, no @Overwrite, vanilla path kept when off, WEST sign bug fixed, no extra quads by default, macOS default, unit test of the math | T1 | Sonnet | S | done |
| T5 | Outline renderer core | Implementation per DESIGN.md; outlines on held, armor, dropped, item frame, GUI items of any mod; fails safe under Iris; builds | T2 | Opus | L | in progress (session 2) |
| T6 | Glint system + textures | Generated glint presets (no third-party art), custom textures, tint, speed, scale, strength, per-category settings, per-rule colour; builds | T2, T3 | Sonnet | L | in progress (session 2) |
| T7 | Config screen, Mod Menu, keybind, lang en/de | Vanilla-widget screens for all options and rules, colour picker, Mod Menu entry (optional dep), keybind, en_us + de_de complete | T3 | Sonnet | L | in progress (update round, session 2) |
| T8 | Wiring and integration | Config drives T4/T5/T6, reload applies live, no dead options | T4-T7 | Sonnet | S | open |
| T9 | Docs and mod page | README, CHANGELOG, `docs/curseforge/` (description.md with image markers, summary.txt, preview.html, upload-checklist.md, comparison vs originals) in the style of Lootr Remastered | T8, T10, T12 | Sonnet | S | open |
| T12 | Art pipeline | `art/` stdlib Python: original pixel-art blade, logo + icon, animated showcase GIF cycling glint/outline looks, banner, feature cards; reproducible | - | Sonnet | L | in progress (session 2) |
| T10p | In-game test scripts | `.dev/ingame/` smoke.json, smoke_owner.json, README; dry-run passes | - | Sonnet | S | in progress |
| T10 | In-game test | Nytheria client with `--add-mod`, Sodium + Iris, screenshots of all features (also used as showcase images) | T8 | Lead | - | open |
| T11 | Final acceptance | Full build/tests, Graphify, report, push | T10 | Lead | - | open |

Parallel: T2 (Opus) with T3 + T4 (Sonnet) after T1. T5 (Opus) with T6/T7 (Sonnet), files disjoint.

## Usage tally (work units, S = 1, L = 2)
Session 1: Sonnet ~10, Opus ~3.
Session 2 so far: Sonnet T3-fix 1, T6 2, T12 2, T10p 1, T7 update 1; Opus T5 2.
Total: Sonnet ~17, Opus ~5 (about 23 % Opus). Next planned: T8 S, T9 S (Sonnet).

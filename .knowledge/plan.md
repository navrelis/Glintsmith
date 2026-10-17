# Plan (Glintsmith 1.0.0)

Status: open / in progress / in review / done. Size S = short (1 unit), L = long (2 units).

| # | Task | Definition of done | Deps | Model | Size | Status |
|---|------|--------------------|------|-------|------|--------|
| T1 | Project scaffold | Gradle wrapper + Loom build (mojmap, Java 21), fabric.mod.json (client, MIT), empty client entrypoint, mixin config, JUnit with fabric-loader-junit, .gitignore, LICENSE, `gradlew build` passes, sources generated | - | Sonnet | S | done |
| T2 | Render architecture design | `docs/DESIGN.md`: outline pipeline (universal, any item geometry), glint system, exact 1.21.1 mixin targets, Sodium/Iris strategy, interfaces for T5/T6/T8 | T1 | Opus | L | in review (DESIGN.md accepted with lead deviations, see handoff) |
| T3 | Config, rule engine, colour animation | JSON config with defaults, validation, live reload; rule matching (id, tag, enchantment, mod, any-enchanted, allow/deny); colour evaluator (static, rainbow, pulse, gradient, per-enchantment, durability alert); unit tests pass | T1 | Sonnet | L | done (correction accepted, committed) |
| T4 | Model gap fix | UV-shrink control on the blocks atlas, side-quad expansion and indent for generated items, no @Overwrite, vanilla path kept when off, WEST sign bug fixed, no extra quads by default, macOS default, unit test of the math | T1 | Sonnet | S | done |
| T5 | Outline renderer core | Implementation per DESIGN.md; outlines on held, armor, dropped, item frame, GUI items of any mod; fails safe under Iris; builds | T2 | Opus | L | done (merged into dev, session 4) |
| T6 | Glint system + textures | Generated glint presets (no third-party art), custom textures, tint, speed, scale, strength, per-category settings, per-rule colour; builds | T2, T3 | Sonnet | L | done (committed; in-game check in T10) |
| T7 | Config screen, Mod Menu, keybind, lang en/de | Vanilla-widget screens for all options and rules, colour picker, Mod Menu entry (optional dep), keybind, en_us + de_de complete | T3 | Sonnet | L | done (update round accepted; in-game check in T10) |
| T8 | Wiring and integration | Config drives T4/T5/T6, reload applies live, no dead options | T4-T7 | Sonnet | S | done (session 4) |
| T9 | Docs and mod page | README, CHANGELOG, `docs/curseforge/` (description.md with image markers, summary.txt, preview.html, upload-checklist.md, comparison vs originals) in the style of Lootr Remastered | T8, T10, T12 | Sonnet | S | in progress (session 6: T9a armor-default/modelfix/lang tooltip pass running; T9b VERIFY markers + preview regen after T10) |
| T12 | Art pipeline (redo) | Logo, icon, banner hero and gallery = real in-game renders of the mod composed into Lootr-style cards (frame, vanilla font read from the MC jar at build time, nothing vanilla committed), showcase GIF from captured frames; reproducible `py -B art/...` | T10 captures for final images | Sonnet | L | pipeline done (committed); T12b logo/banner from renders done (session 4); final gallery after T10 captures |
| T10h | Shadow harness | Working copy of the client harness in the lead's scratchpad; dry-run of smoke + owner scripts passes | - | Sonnet | S | done |
| T10g | Gallery capture script | `.dev/ingame/gallery.json` (+ showcase frames) from `art/gallery/SHOTLIST.md`, dry-run passes in the shadow harness | T12 shot list | Sonnet | S | done (session 4; run via run_ingame.py) |
| T10p | In-game test scripts | `.dev/ingame/` smoke.json, smoke_owner.json, README; dry-run passes | - | Sonnet | S | done (dry-run green against a shadow harness copy; real harness broken) |
| T10 | In-game test | Nytheria client with `--add-mod`, Sodium + Iris, screenshots of all features (also used as showcase images) | T8 | Lead | - | in progress (session 5: modelfix crash fixed a43f7e7, modelfix removed from pack; smoke run OK except: fins on held generated items (gap fix) and no first-person hand outline; both with Opus) |
| T11 | Final acceptance | Full build/tests, Graphify, report, push | T10 | Lead | - | open |
| T13 | Gap fix fins | Root cause + failing test + fix in GapFix; held items normal in game with AUTO | T10 | Opus | S | done (session 6; in game: no fins with AUTO) |
| T14 | Hand outline missing | Root cause + fix in worktree Glintsmith-hand (branch fix-hand-outline); first-person outlines visible in game | T10 | Opus | L | merged b9bc988 (cause: Punchy draws held items with THIRD_PERSON context; HAND phase now maps hand contexts to FIRST_PERSON); verified in game (held run) |
| T17 | Gap corner cracks | in-game sweep of sideExpansion (gapsweep.json), pick the smallest value that closes step-corner cracks, set it as AUTO default + tests | T13 | Sonnet | S | sweep done (0.005 chosen); T17b done (session 7, 327 green; in-game check in T10 smoke) |
| T18 | Glint hand-pass target | glint classifies hand-context renders inside renderHandsWithItems as first person (Punchy), independent of outlines; tests; build green | T14 | Sonnet | S | done (in-game glint rule check in T10 rerun) |
| T19 | Outline bleeds over the first-person arm | root cause + fix: outline fragments (dashes) drawn over the arm where the held item goes behind it (Punchy, smoke2 010_fp_sword); occlusion behaviour kept; build green; verified in game | T14 | Opus | S | committed (cause: mask passed hidden item pixels via polygon offset, composite had no item depth; fix: mask stores item depth, seeds only where visible); 339 green; verify in game |
| T20 | HUD hotbar erased with GUI outlines on | root cause + fix in worktree Glintsmith-hud (branch fix-hud-outline): hotbar sprite/items/count drawn and outlined with targets.gui on; screens unaffected; build green; verified in game | - | Opus | S | committed 1b96935 on fix-hud-outline (cause: ImmediatelyFast HUD batch force-drawn into the mask by our mask draw; fix flushes outlined GUI items at O1 end); 332 green; merge after T19, verify in game |
| T16 | In-game script prep | held.json (first-person check) + gallery/showcase configs enable targets.armor where armor is shown; README expectations; dry-runs pass | - | Sonnet | S | done (session 6; .dev is git-ignored, SHOTLIST committed) |
| T15 | Armor outline default off (owner) | targets.armor = false, tests | - | Sonnet | S | done (6645192); docs in T9 final |

Parallel: T2 (Opus) with T3 + T4 (Sonnet) after T1. T5 (Opus) with T6/T7 (Sonnet), files disjoint.

## Usage tally (work units, S = 1, L = 2)
Session 1: Sonnet ~10, Opus ~3.
Session 2 so far: Sonnet T3-fix 1, T6 2, T12 2, T10p 1, T7 update 1; Opus T5 2.
Total before session 3: Sonnet ~17, Opus ~5 (about 23 % Opus).
Session 3: T5 resume Opus L 2, T6 finish Sonnet S 1, T12 redo Sonnet L 2, T10h Sonnet S 1, T6 corr S 1, T10g S 1, T12 follow-up S 1 → Sonnet ~24, Opus ~7 (about 23 % Opus). Next: T8 S, T10g S, T9 S (Sonnet).
Session 4: T5 finish Opus L 2, T10g Sonnet S 1, T12b Sonnet S 1 → Sonnet ~27, Opus ~9 (about 25 % Opus) incl. T9 draft S 1; + merge fix + T8 S 1 → Sonnet ~28, Opus ~9 (about 24 %).
Session 5: modelfix fix Sonnet S 1, armor default Sonnet S 1 (+ small); T13/T14 Opus stopped at start (not spent) → Sonnet ~30, Opus ~9 (about 23 % Opus).
Session 6: T13 Opus S 1, T14 Opus L 2, T9a Sonnet S 1, T16 Sonnet S 1, T17 sweep script S 1 (+corr), T18 S 1 → Sonnet ~34, Opus ~12 (about 26 % Opus).
Session 7: T17b Sonnet S 1 (+corr), T9b-1 Sonnet S 1, T19 Opus S 1, T20 Opus S 1 → Sonnet ~36, Opus ~14 (about 28 % Opus).

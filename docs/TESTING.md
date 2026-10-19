# Testing

Besides the unit tests (`./gradlew build`, run by every loader build) there is an automatic in-game smoke test: it launches a
dev client, creates a test world, takes screenshots of the mod's features and exits. It is meant to run unattended for every
jar of every Minecraft version and loader, and for a person to look at the screenshots.

## Run it

```powershell
# from the repository root, PowerShell 7
pwsh scripts/smoke.ps1 -Loader fabric                      # one loader
pwsh scripts/smoke.ps1 -Loader fabric,neoforge,forge       # several (a loader folder that does not exist is skipped)
pwsh scripts/smoke.ps1 -Loader fabric -OutDir D:\smoke     # results somewhere else (default: build/smoke)
```

or only the game, without the checks: `cd fabric; .\gradlew.bat runSmoke` (writes `fabric/run-smoke/smoke-result.json`; the task
fails unless every step passed). A run takes about 1 minute with a warm Gradle (about 30 s of it in the game).

The game window opens without taking the keyboard focus (1280x720). Do not close it by hand.

## What it does

1. The run task deletes the old result, screenshots and worlds of `<loader>/run-smoke` and copies
   `common/src/smoke/resources/glintsmith-smoke.json` to `run-smoke/config/glintsmith.json`: outlines on every target
   (including armor and GUI), static outline colour `#00FF40`, thickness 6, opacity 1, and the `preset:rainbow` glint on all four
   layers, so every effect is clearly visible.
2. From the title screen the runner creates a fresh superflat creative world `GlintsmithSmoke-<timestamp>` (peaceful, no
   structures, noon, no weather, render distance 5) and waits until the chunks around the player are rendered.
3. On the server thread it builds the scene: a snow block wall as light backdrop, an enchanted netherite sword in the main
   hand and an enchanted shield in the off hand, a full enchanted diamond armor set, a dropped enchanted golden sword
   (hovering, never despawns), an item frame with an enchanted diamond pickaxe, and more enchanted items in the inventory. All
   stacks are built in code, no commands.
4. Screenshots (`<run-smoke>/screenshots/`, taken from the main render target after the scene had 15 to 40 ticks to settle):

   | file | shows |
   | --- | --- |
   | `01_first_person.png` | held sword and shield, dropped sword, framed pickaxe |
   | `05_dropped_closeup.png` | the dropped sword close up |
   | `06_item_frame_closeup.png` | the framed pickaxe close up |
   | `03_third_person_front.png` | F5 front view: worn armor and the held items |
   | `02_inventory.png` | the survival inventory screen: GUI items with glint and outline, worn armor |
   | `04_glintsmith_screen.png` | the Glintsmith screen with its live preview |

5. It writes `run-smoke/smoke-result.json` (per step ok or error, screenshot statistics, Minecraft version, loader, Glintsmith
   version, duration) and stops the client. Every step is guarded: an exception is recorded and the run goes on. A watchdog
   (7 minutes) writes a failed result and halts the process; `-Dglintsmith.smoke.timeout=<seconds>` changes it.

## What `scripts/smoke.ps1` checks

| check | passes when |
| --- | --- |
| gradle runSmoke | the Gradle exit code is 0 (within the timeout) |
| steps | `smoke-result.json` exists and every step is ok |
| config | the smoke config was loaded without repairs or warnings |
| screenshots | all six PNGs exist, are valid, are not blank (at least 200 distinct colours) and contain at least 50 pixels of the outline colour |
| log: errors | `run-smoke/logs/latest.log` has no `ERROR` or `Exception` line that mentions glintsmith or navrelis |
| log: outline hook self-check | no `Some outline hooks were not applied` warning (OutlineSystem logs it at startup when an outline mixin did not apply) |
| log: warnings | no other WARN line of Glintsmith |
| log: mixins | no mixin apply failure line |

It prints a PASS/FAIL table with the details of every check and exits with 0 only if every requested loader passed. Screenshots,
`smoke-result.json`, `latest.log` and the Gradle output are copied to `<OutDir>/<minecraft version>-<loader>/`
(default `build/smoke/1.21.1-fabric/`).

The outline pixel count is only a safety net against a missing outline; glints (a colour animation) and the look of everything
else still have to be judged on the pictures.

## Layout

| path | what |
| --- | --- |
| `common/src/smoke/java/navrelis/glintsmith/smoke/` | loader-neutral runner: `SmokeRunner` (state machine, one `tick(Minecraft)` per client tick), `SmokeScene` (server side scene and camera poses), `SmokeResult` (JSON) |
| `common/src/smoke/resources/glintsmith-smoke.json` | the config of every run |
| `<loader>/src/smoke/` | the loader wrapper: a dev-only mod `glintsmith_smoke` that calls the runner, plus a window mixin that creates the game window without focus |
| `<loader>/build.gradle` | the `smoke` source set, the `glintsmith_smoke` mod, the `smoke` run (`runSmoke`, run folder `run-smoke`, git-ignored) and the clean-up and result check of the task |

The smoke source set is compiled by `check` (`smokeClasses`) so it cannot rot, and is never part of the released jar.

## Wiring a loader

A loader wrapper needs: a dev-only mod (own metadata, depends on `glintsmith`) with a client entry point that calls
`SmokeRunner.init()`, registers `SmokeRunner.configureOptions` for "client started" and `SmokeRunner.tick` for the end of every
client tick; an optional window mixin; a `smoke` source set (`common/src/smoke/java` plus the loader's `src/smoke`) on top of the
main one; a client run `smoke` with run folder `run-smoke`, program args `--width 1280 --height 720` and system properties
`glintsmith.smoke.minecraft` and `glintsmith.smoke.version`; and the `runSmoke` task logic of `fabric/build.gradle` (clean the run
folder, copy the config, check the result). The script only needs the `runSmoke` task and `<loader>/run-smoke/`.

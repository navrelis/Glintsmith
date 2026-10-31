# sh3b-armor-glint: armor glint under Complementary

**Result: no defect, no code change.** Helmet, leggings and boots do get the custom glint under the pack. The missing glint in `03_third_person_front.png` is the phase of the animated pattern. No fix, and no pack or Iris limitation to document.

Root cause of the report: armor maps its UVs at 0.16 of a `preset:fire` tile. That tile has wide black gaps (`GlintPresets.fire`, ramp from 0 at v=0), so a whole piece sits in a bright or a dark band for seconds. Chest UVs are about half a period out of phase with helmet, legs and boots. Step 03 fires at a similar time after start, which likely explains why "chest only" kept recurring (inferred; one run landed in another phase).

Evidence: 14 extra shots after step 03, 1.25 s apart, mean colour per piece (temporary debug, removed). Contact sheets in the shared notes folder, `sh3b-armor-glint/`.
| run | helmet | shoulder | legs | boots |
|---|---|---|---|---|
| Pack, custom fire (R) | 60-145 | 65-137 | 20-88 | 28-110 |
| No shaders, custom fire (R) | 54-163 | 65-167 | 43-158 | 55-168 |
| Pack, vanilla glint (B) | 155-171 | 144-158 | 108-118 | 107-123 |
| Pack, fire at scale 0.2 (R) | 62-145 | 68-136 | 21-88 | 35-108 |

- Every piece pulses in every run; shoulder is high when the others are low; no-shader behaves the same.
- Pictures: `shaders_03_helmet_legs_boots_lit.png` (outlines on), `shaders_fire_scale0.2_all_four_lit.png` (all four in one frame), `shaders_vanilla_glint_timeseries.png` (vanilla on all four, first step of the brief).
- Hypotheses refuted: first-piece-only and per-piece wrapped types. All four share one `glint` draw of 240 vertices with depth func EQUAL; positions match the base armor bit for bit (earlier debug log, `cmp` lines).
- Not proven: my stored-vs-mesh depth readback gave no usable data (stored=1.0, no quad hits). Not needed, since the glint is seen on all four, so the depth test passes.
- False lead: the pale face and orange hair in some shots are a different random default skin per run, not glint.

Changed files: only this report. `git diff claude/s22-shaders -- common/src/smoke` is empty. No debug code left.
Tests on the clean tree, outlines on (first build also at the branch tip):
- `pwsh scripts/smoke.ps1 -Loader fabric -Shaders`: PASS. `build/smoke/1.21.1-fabric-shaders/03_third_person_front.png`
- `pwsh scripts/smoke.ps1 -Loader fabric`: PASS. `build/smoke/1.21.1-fabric/03_third_person_front.png`
- `gradlew --no-daemon build`: green in `fabric/`, `neoforge/`, `forge/`.

Open points:
- The final shader `03` again shows chest lit and the other three dark. That is the phase, not a failure. One fixed shot cannot show all four with this smoke look, so this item of the definition of done is met by the time series, not by `03`.
- Option: a finer smoke armor look (scale 0.2) helps `03` but guarantees nothing; left out to keep the smoke unchanged.
- Docs: no behaviour changed. `docs/TESTING.md` already calls the glint a sparse animation. It could add that chest and the other pieces run out of phase, so a dark piece in `03` is expected.
- Debug runs with 14 extra screenshots report FAIL in the script; I did not read the check table. Likely the screenshot-step count in the "shader pack in use" check, since all steps were ok. Clean runs pass.

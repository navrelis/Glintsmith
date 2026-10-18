# Graph Report - eloquent-wing-a06858  (2026-10-18)

## Corpus Check
- 256 files · ~559,060 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 14 file(s) not represented in the graph (top: (none) 5, .fsh 3, .properties 2)

## Summary
- 3347 nodes · 10054 edges · 143 communities (105 shown, 38 thin omitted)
- Extraction: 91% EXTRACTED · 9% INFERRED · 0% AMBIGUOUS · INFERRED: 903 edges (avg confidence: 0.84)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `a1b56a47`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- GlintTexturesTest
- logo.py
- GlintPresets
- GlintsmithConfig
- .t
- .parseHex
- org.junit.jupiter.api.Test
- OutlineMixinApplicationTest
- OutlineSystem
- org.jetbrains.annotations.Nullable
- org.spongepowered.asm.mixin.Mixin
- ConfigTest
- Glintsmith
- lib.py
- ColorStyleButton
- ConfigManager.java
- StyleTable
- EnchantmentColorsTab
- Canvas
- verify.py
- TeeBufferSource
- .sanitize
- GlintShards
- GlintContext
- net.minecraft.client.renderer.RenderType
- ColorEvaluatorTest
- net.minecraft.client.gui.GuiGraphics
- OutlineScaleTest
- gifenc.py
- Override
- OutlineCompositor
- ScreenBounds
- PatternBox
- .acquire
- RuleMatcherTest
- at
- FloatSlider
- Glintsmith
- LangFilesTest.java
- SideElementBakeTest.java
- OutlineTarget
- OutlineDepthTest
- Glintsmith.java
- assertequals
- OutlineShaders
- GlintsmithScreen.java
- studio.py
- TextureAtlasSpriteMixin.java
- RulesTab
- SideElementBakeTest
- com.mojang.blaze3d.vertex.VertexConsumer
- .clamp
- GapFix
- GlintNoise
- composite.py
- PreviewPanel
- GlintPresetsTest
- OutlineRuntimeSettings
- net.minecraft.client.Minecraft
- ItemModelGeneratorSpanMixin.java
- RuleEngine
- Glintsmith render architecture (T2)
- IrisCompat
- .drawItem
- Handoff: Glintsmith 2.0.1 (stray-dot fixes, release) — finished
- PhaseState
- Tri
- PreviewPose
- preview.py
- GlintParams
- MaskTypesTest
- LangFilesTest
- StudioScreen.java
- Glintsmith capture shot list
- Glintsmith
- Glint (T6)
- RuleEngineScopeTest
- Glintsmith Studio (dev-only render mode)
- synth_studio.py
- common.py
- enchantment
- Scene
- OutlineSystem.java
- CompositeStateAccessor
- SpanMixin
- .read
- ConfigSubScreen
- MaskTypes.java
- StudioMod
- rendersystem
- CurseForge upload checklist: Glintsmith 2.1.0
- net.minecraft.resources.ResourceLocation
- org.spongepowered.asm.mixin.injection.Redirect
- GlintKind
- .validateTexture
- Entry
- .window
- CurseForge page material for Glintsmith
- gradlew
- Condition
- Plan (Glintsmith 1.0.0)
- decisions.md
- log.md
- PreviewNotesTest
- PreviewLayoutTest
- GlintsmithKeys.java
- GlintAtlasScaleTest
- TeeCaptureTest
- StudioScreen
- ColorPickerScreen
- StudioBootstrap.java
- Recorder
- gif.py
- OutlineShaderResourcesTest
- ConfigManager
- MaskTypes
- ModMenuIntegration.java
- .drawSubjects
- PendingEditsTest
- Changelog
- Handoff: Glintsmith 2.1.0 (owner config as the built-in defaults, then upload)
- ItemFacts
- GapFix.java
- pipeline.py
- .update
- Final report: Glintsmith 2.0.0
- GlintLook
- .build
- Glintsmith art
- Handoff: Glintsmith lineup stray outline dot (root cause + outline fix)
- Type
- Versioning
- renderstateshard
- ColorStyleScreen
- .thicknessScale
- com.google.gson.JsonElement
- ColorMode
- ReloadCounter
- TextureReloadListener
- org.junit.jupiter.api.AfterAll

## God Nodes (most connected - your core abstractions)
1. `GlintsmithConfig` - 133 edges
2. `OutlineSystem` - 72 edges
3. `OutlineTarget` - 53 edges
4. `Rule` - 46 edges
5. `GlintSystem` - 44 edges
6. `ColorStyle` - 43 edges
7. `Canvas` - 42 edges
8. `ConfigManager` - 42 edges
9. `ConfigTest` - 42 edges
10. `GlintCategory` - 38 edges

## Surprising Connections (you probably didn't know these)
- `Compatibility notes` --references--> `TeeVertexConsumer`  [INFERRED]
  docs/curseforge/description.md → src/main/java/navrelis/glintsmith/render/outline/TeeVertexConsumer.java
- `Compatibility notes` --references--> `TeeVertexConsumer`  [INFERRED]
  docs/curseforge/description_paste.md → src/main/java/navrelis/glintsmith/render/outline/TeeVertexConsumer.java
- `Compatibility` --references--> `TeeVertexConsumer`  [INFERRED]
  README.md → src/main/java/navrelis/glintsmith/render/outline/TeeVertexConsumer.java
- `Files` --references--> `StudioMod`  [INFERRED]
  docs/STUDIO.md → src/studio/java/navrelis/glintsmith/studio/StudioMod.java
- `3. Done (all committed and pushed; main = dev = b05998a, tag v2.0.1)` --references--> `MaskTypesTest`  [INFERRED]
  handoff-2026-10-18-51238096.md → src/test/java/navrelis/glintsmith/render/outline/MaskTypesTest.java

## Import Cycles
- None detected.

## Communities (143 total, 38 thin omitted)

### Community 1 - "logo.py"
Cohesion: 0.10
Nodes (32): build(), _compose_task(), frame(), _frame_task(), glow_grids(), glow_layer(), _init_compose(), _init_frames() (+24 more)

### Community 2 - "GlintPresets"
Cohesion: 0.26
Nodes (4): 10. Manual in-game test checklist, 5.6 Procedural presets (no third-party art), Enchantment glint, GlintPresets

### Community 3 - "GlintsmithConfig"
Cohesion: 0.10
Nodes (12): com.google.gson.Gson, set, GlintsmithConfig, RuleEditScreen, GlintTab, ColorStyle, Override, GlintSettings (+4 more)

### Community 4 - ".t"
Cohesion: 0.10
Nodes (10): net.minecraft.client.gui.components.CycleButton, net.minecraft.client.gui.components.Tooltip, OnPress, SettingRows, Override, Override, ModelFixTab, Override (+2 more)

### Community 5 - ".parseHex"
Cohesion: 0.09
Nodes (5): DurabilityAlert, Parsed, EnchantmentColors, ColorsTest, DurabilityAndEnchantmentColorsTest

### Community 6 - "org.junit.jupiter.api.Test"
Cohesion: 0.07
Nodes (7): org.junit.jupiter.api.Test, GapFixTest, GlintsmithMixinPluginTest, MixinApplicationTest, OutlineContextsTest, PreviewBoundsTest, SmokeTest

### Community 7 - "OutlineMixinApplicationTest"
Cohesion: 0.07
Nodes (9): org.objectweb.asm.tree.AnnotationNode, org.objectweb.asm.tree.ClassNode, org.objectweb.asm.tree.MethodNode, org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin, org.spongepowered.asm.mixin.extensibility.IMixinInfo, GlintsmithMixinPlugin, Override, GlintMixinApplicationTest (+1 more)

### Community 8 - "OutlineSystem"
Cohesion: 0.08
Nodes (6): 12.2 The PREVIEW outline phase, How the real glint and outline reach the subjects, net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext, BufferSource, OutlineSystem, OutlineFailsafeTest

### Community 9 - "org.jetbrains.annotations.Nullable"
Cohesion: 0.08
Nodes (31): abstracttexture, button, chatformatting, commoncomponents, consumer, cyclebutton, formattedcharsequence, function (+23 more)

### Community 10 - "org.spongepowered.asm.mixin.Mixin"
Cohesion: 0.12
Nodes (34): com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod, com.llamalad7.mixinextras.injector.wrapoperation.Operation, com.mojang.blaze3d.vertex.PoseStack, Outline (T5), equipmentslot, itemtags, net.minecraft.client.model.HumanoidModel, net.minecraft.client.player.LocalPlayer (+26 more)

### Community 12 - "Glintsmith"
Cohesion: 0.17
Nodes (9): Environment facts (verified 2026-10-17), Later owner additions (chat, 2026-10-17), Owner answers (2026-10-17), Owner answers (2026-10-18, 2.0.1 follow-up), Owner task (2026-10-17), Requirements (Glintsmith, session 2026-10-17), Session 12 owner additions (2026-10-18), Glintsmith (+1 more)

### Community 13 - "lib.py"
Cohesion: 0.05
Nodes (69): anchor_x(), build_all(), build_card(), chip_list(), Ctx, el_bullets(), el_capture(), el_chips() (+61 more)

### Community 14 - "ColorStyleButton"
Cohesion: 0.27
Nodes (3): net.minecraft.client.gui.components.AbstractButton, ColorStyleButton, Override

### Community 15 - "ConfigManager.java"
Cohesion: 0.08
Nodes (21): abstractappender, assertdoesnotthrow, assumptions, atomicinteger, atomicmovenotsupportedexception, basicfileattributes, copyonwritearraylist, filechannel (+13 more)

### Community 16 - "StyleTable"
Cohesion: 0.16
Nodes (4): 3.4.1 Proportional thickness (2.0.0), 3.4 Per-item data encoding, StyleTable, StyleTableTest

### Community 17 - "EnchantmentColorsTab"
Cohesion: 0.23
Nodes (3): EnchantmentColorsTab, EnchantmentRow, Override

### Community 18 - "Canvas"
Cohesion: 0.10
Nodes (11): Canvas, An RGBA canvas on a bytearray. Coordinates outside the canvas are clipped,…, A new canvas of the region; parts outside this canvas are transparent., Nearest-neighbour upscale by the whole factor k., Box-filter downscale by the whole factor n (every output pixel is the mean of…, Area-filter resize to ow x oh: every output pixel is the exact mean of the…, Draw src at (x, y): transparent pixels are skipped, half-transparent ones…, PNG bytes: RGB when fully opaque, RGBA otherwise (text: optional tEXt comment). (+3 more)

### Community 19 - "verify.py"
Cohesion: 0.08
Nodes (49): _chunk(), parse_gif(), png_bytes(), pixels: bytes-like, w*h*channels, row-major. channels 3 = RGB, 4 = RGBA. text:…, Parse back a GIF written by gif_bytes (or any simple GIF). Returns dict with…, contrast(), luminance(), WCAG relative luminance of an RGB(A) colour (0 black .. 1 white). (+41 more)

### Community 20 - "TeeBufferSource"
Cohesion: 0.16
Nodes (6): com.mojang.blaze3d.vertex.ByteBufferBuilder, object2objectsortedmaps, Override, TeeBufferSource, TeePool, GuiItemFlushTest

### Community 22 - "GlintShards"
Cohesion: 0.16
Nodes (6): Black/white pairs, Camera and light, Render target and supersampling, Studio clock, What happens during a run, GlintShards

### Community 23 - "GlintContext"
Cohesion: 0.15
Nodes (3): 3.1 Phases, GlintContext, GlintContextTest

### Community 24 - "net.minecraft.client.renderer.RenderType"
Cohesion: 0.10
Nodes (10): 12.1 Mechanism, 5.2 Strategy, 5.4.1 Atlas-independent scale (2.0.0), 8.2 T6: glint system (`navrelis.glintsmith.render.glint`), net.minecraft.client.renderer.RenderType, GlintRuntimeSettings, GlintSystem, BufferSource (+2 more)

### Community 26 - "net.minecraft.client.gui.GuiGraphics"
Cohesion: 0.10
Nodes (15): builtinregistries, component, font, net.minecraft.client.gui.components.AbstractWidget, net.minecraft.client.gui.components.ContainerObjectSelectionList, net.minecraft.client.gui.components.events.GuiEventListener, net.minecraft.client.gui.GuiGraphics, net.minecraft.client.gui.narration.NarratableEntry (+7 more)

### Community 27 - "OutlineScaleTest"
Cohesion: 0.18
Nodes (5): net.minecraft.client.renderer.block.model.ItemTransform, org.joml.Matrix4f, org.joml.Matrix4fc, OutlineScale, OutlineScaleTest

### Community 28 - "gifenc.py"
Cohesion: 0.04
Nodes (60): colour_histogram(), encode_gif_frames(), gif_bytes(), gif_compose(), lzw_decode(), lzw_encode(), median_cut(), Dependency-free image encoders (PNG, animated GIF89a) and a small GIF decoder… (+52 more)

### Community 29 - "Override"
Cohesion: 0.18
Nodes (4): Override, Recorder, RecordingSource, Throwing

### Community 30 - "OutlineCompositor"
Cohesion: 0.10
Nodes (11): com.mojang.blaze3d.pipeline.RenderTarget, com.mojang.blaze3d.pipeline.TextureTarget, 3.3 Targets, depth and resize, 3.6 Composite procedure (`OutlineCompositor.composite(PhaseState, Params)`), GlState, MaskDepth, CLEAR, COPY (+3 more)

### Community 32 - "PatternBox"
Cohesion: 0.10
Nodes (7): net.minecraft.network.chat.Style, Hsv, PatternText, TextureNames, Override, PatternBox, GuiHelpersTest

### Community 33 - ".acquire"
Cohesion: 0.15
Nodes (5): 4. Studio renders, Still scenes (4), Turntables (3), FakeType, GlintSlotPoolTest

### Community 35 - "at"
Cohesion: 0.14
Nodes (16): at, com.mojang.blaze3d.vertex.BufferBuilder, net.minecraft.client.DeltaTracker, net.minecraft.client.renderer.GameRenderer, net.minecraft.client.renderer.texture.TextureAtlas, org.spongepowered.asm.mixin.injection.callback.CallbackInfo, org.spongepowered.asm.mixin.injection.Inject, spriteloader (+8 more)

### Community 36 - "FloatSlider"
Cohesion: 0.19
Nodes (8): java.util.function.DoubleConsumer, java.util.function.DoubleFunction, java.util.function.DoubleToIntFunction, net.minecraft.client.gui.components.AbstractSliderButton, FloatSlider, Override, GradientSlider, Override

### Community 37 - "Glintsmith"
Cohesion: 0.08
Nodes (22): Commands and keys, Compatibility notes, Credits and licence, Glintsmith, How it compares, Performance, Requirements, Sodium and Iris (+14 more)

### Community 38 - "LangFilesTest.java"
Cohesion: 0.09
Nodes (40): abstractinsnnode, arraylist, classreader, datetimeformatter, directorystream, enumeration, executors, executorservice (+32 more)

### Community 39 - "SideElementBakeTest.java"
Cohesion: 0.09
Nodes (18): arrays, assertinstanceof, blockelementface, bytebufferbuilder, byteorder, direction, facebakery, framesize (+10 more)

### Community 40 - "OutlineTarget"
Cohesion: 0.09
Nodes (18): list, predicate, OutlineContexts, GlintCategory, ARMOR, ENTITY, GUI, ITEM (+10 more)

### Community 42 - "Glintsmith.java"
Cohesion: 0.10
Nodes (13): commonlifecycleevents, jsonarray, jsonprimitive, GapFixMode, AUTO, CUSTOM, GapFix, OutlineCondition (+5 more)

### Community 43 - "assertequals"
Cohesion: 0.13
Nodes (16): assertarrayequals, assertequals, assertfalse, assertnotequals, assertnotnull, assertnotsame, assertnull, assertsame (+8 more)

### Community 44 - "OutlineShaders"
Cohesion: 0.15
Nodes (9): com.mojang.blaze3d.vertex.VertexBuffer, com.mojang.blaze3d.vertex.VertexFormat, defaultvertexformat, gl20, net.minecraft.client.renderer.ShaderInstance, net.minecraft.server.packs.resources.ResourceProvider, resourcemanager, FullscreenQuad (+1 more)

### Community 45 - "GlintsmithScreen.java"
Cohesion: 0.09
Nodes (12): confirmscreen, createworldscreen, net.minecraft.client.gui.components.Button, net.minecraft.client.gui.components.tabs.Tab, net.minecraft.client.gui.components.tabs.TabManager, net.minecraft.client.gui.components.tabs.TabNavigationBar, screenrectangle, GlintsmithScreen (+4 more)

### Community 46 - "studio.py"
Cohesion: 0.10
Nodes (30): read_png_size(), The studio scenes (the contract), find_frame(), load(), Studio scenes: finding, loading and validating the frame contract. Python 3…, The frame dict for `ref`: a still name, a frame index (1 based) or a file stem…, A scene that is not there (kind 'missing') or not readable (kind 'error')., (K path, W path) of a frame dict. (+22 more)

### Community 47 - "TextureAtlasSpriteMixin.java"
Cohesion: 0.29
Nodes (6): com.llamalad7.mixinextras.injector.ModifyReturnValue, inventorymenu, net.minecraft.client.renderer.texture.SpriteContents, net.minecraft.client.renderer.texture.TextureAtlasSprite, ItemModelGeneratorMixin, TextureAtlasSpriteMixin

### Community 48 - "RulesTab"
Cohesion: 0.20
Nodes (4): net.minecraft.client.gui.components.Checkbox, Override, RuleRow, RulesTab

### Community 49 - "SideElementBakeTest"
Cohesion: 0.21
Nodes (4): net.minecraft.client.renderer.block.model.BakedQuad, net.minecraft.client.renderer.block.model.BlockElement, net.minecraft.client.resources.model.ModelState, SideElementBakeTest

### Community 50 - "com.mojang.blaze3d.vertex.VertexConsumer"
Cohesion: 0.17
Nodes (5): com.mojang.blaze3d.vertex.VertexConsumer, Override, MaskVertexConsumer, Override, TeeVertexConsumer

### Community 51 - ".clamp"
Cohesion: 0.09
Nodes (6): 5. Open work (ordered), Glint, GlintCategorySettings, Outline, Targets, TargetThickness

### Community 54 - "composite.py"
Cohesion: 0.08
Nodes (39): alpha_bbox(), ascii_text(), backdrop(), bar(), _blend_plane(), blur_grid(), _blur_rows(), compose() (+31 more)

### Community 55 - "PreviewPanel"
Cohesion: 0.10
Nodes (9): net.minecraft.client.gui.Font, net.minecraft.client.sounds.SoundManager, Override, PreviewPanel, PreviewSession, Source, HELD, SAMPLE (+1 more)

### Community 58 - "net.minecraft.client.Minecraft"
Cohesion: 0.20
Nodes (6): net.minecraft.client.Minecraft, net.minecraft.resources.ResourceKey, RegistryLookup, PreviewItems, Sample, MinecraftExitMixin

### Community 59 - "ItemModelGeneratorSpanMixin.java"
Cohesion: 0.23
Nodes (7): coerce, com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation, local, net.minecraft.client.renderer.block.model.ItemModelGenerator, ItemModelGeneratorSpanMixin, SpanAccess, unique

### Community 60 - "RuleEngine"
Cohesion: 0.15
Nodes (10): 3.7 Performance and allocation rules (T5), 9. Risks and open questions (each with a recommendation), net.minecraft.world.item.enchantment.ItemEnchantments, net.minecraft.world.item.Item, ResolvedStyle, EnchantKey, Override, RuleEngine (+2 more)

### Community 61 - "Glintsmith render architecture (T2)"
Cohesion: 0.11
Nodes (17): 1. Decision summary, 2. Frame timeline in 1.21.1 and where Glintsmith acts, 3.5 Shaders, 3.8 Failsafe (T5), 3. Outline pipeline in detail (T5), 4. Hook table, 5.1 How vanilla 1.21.1 draws glint (verified), 5.3 Categories, context and resolution (+9 more)

### Community 62 - "IrisCompat"
Cohesion: 0.22
Nodes (3): IrisApi, Api, IrisCompat

### Community 63 - ".drawItem"
Cohesion: 0.15
Nodes (7): FunctionalInterface, PreviewBounds, BoundsKey, BufferSource, Override, Margin, PreviewRenderer

### Community 64 - "Handoff: Glintsmith 2.0.1 (stray-dot fixes, release) — finished"
Cohesion: 0.15
Nodes (12): 10. How to resume, 1. Goal, 2. Requirements and constraints, 3. Done (all committed and pushed; main = dev = b05998a, tag v2.0.1), 4. In progress when stopped, 5. Open work (owner chores, ordered), 6. Known problems and blockers, 7. Decisions made (+4 more)

### Community 65 - "PhaseState"
Cohesion: 0.09
Nodes (10): BufferSource, 5.5 Texture management (`GlintTextures`), MaskBufferSource, OutlinePhase, GUI, HAND, NONE, PREVIEW (+2 more)

### Community 66 - "Tri"
Cohesion: 0.33
Nodes (4): Tri, INHERIT, OFF, ON

### Community 67 - "PreviewPose"
Cohesion: 0.16
Nodes (6): axis, org.joml.Matrix3f, quaternionf, Fit, PreviewPose, PreviewPoseTest

### Community 69 - "preview.py"
Cohesion: 0.23
Nodes (12): build(), check(), inline(), main(), pipeline_outputs(), Builds docs/curseforge/preview.html from description.md and checks the marker…, The image names the art pipeline builds (the "out" of every card and GIF of…, render() (+4 more)

### Community 72 - "LangFilesTest"
Cohesion: 0.14
Nodes (8): 12.3 The snapshot and the pending edits chain, 12.4 What is shown, 12.5 Panel and layout, 12.6 Limits (known and by design), 12.7 Tests and what is still open in game, 12. Live preview (2.0.0), PreviewNotes, LangFilesTest

### Community 73 - "StudioScreen.java"
Cohesion: 0.06
Nodes (34): bootstrap, compoundtag, datacomponents, enchantments, entityrenderdispatcher, entitytype, fogrenderer, hashmap (+26 more)

### Community 74 - "Glintsmith capture shot list"
Cohesion: 0.12
Nodes (15): 0. Rig, 1. World and rooms, 2. Glintsmith config, 3. In-world captures (14), 5. Checklist, Backdrop and close-ups, Enchantment shorthand, Frames and stand templates (+7 more)

### Community 75 - "Glintsmith"
Cohesion: 0.20
Nodes (9): Commands and keys, Compatibility notes, Credits and licence, Glintsmith, How it compares, Performance, Requirements, Sodium and Iris (+1 more)

### Community 76 - "Glint (T6)"
Cohesion: 0.24
Nodes (9): Glint (T6), org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable, org.spongepowered.asm.mixin.injection.ModifyVariable, org.spongepowered.asm.mixin.Pseudo, ImmediatelyFastBufferSourceGlintMixin, ImmediatelyFastGuiSourceGlintMixin, BufferSource, ImmediatelyFastHudGlintMixin (+1 more)

### Community 78 - "Glintsmith Studio (dev-only render mode)"
Cohesion: 0.20
Nodes (8): Example scenes, Files, Frame contract (output), Glint scale, Glintsmith Studio (dev-only render mode), Limits, Running a scene, Scene format (input)

### Community 79 - "synth_studio.py"
Cohesion: 0.10
Nodes (24): draw_sword(), Frame, main(), make_contract(), turntable(), make_scene(), Fill a convex polygon; stripe = (centre at y=0, slope, half width, colour)…, Draw one extruded sword centred on (cx, cy) with unit length u (supersampled… (+16 more)

### Community 80 - "common.py"
Cohesion: 0.06
Nodes (41): main(), Captures, cut(), cut_el(), load_manifest(), out_size(), picture_size(), Shared helpers of the gallery builders: manifest loading, capture loading, the… (+33 more)

### Community 82 - "Scene"
Cohesion: 0.19
Nodes (10): com.google.gson.JsonObject, org.joml.Vector3f, Camera, Kind, ARMOR_STAND, ITEM, Look, Scene (+2 more)

### Community 83 - "OutlineSystem.java"
Cohesion: 0.07
Nodes (29): clientlifecycleevents, collection, fabricloader, graphicsstatus, itemstack, java.util.SequencedMap, loggerfactory, method (+21 more)

### Community 84 - "CompositeStateAccessor"
Cohesion: 0.13
Nodes (12): 3.2 Teeing (capture), EmptyTextureStateShard, org.spongepowered.asm.mixin.gen.Accessor, RenderType.CompositeState, BufferSource, CompositeStateAccessor, CullStateShard, DepthTestStateShard (+4 more)

### Community 85 - "SpanMixin"
Cohesion: 0.43
Nodes (3): org.spongepowered.asm.mixin.Shadow, Override, SpanMixin

### Community 88 - "MaskTypes.java"
Cohesion: 0.21
Nodes (9): CompositeState, optional, org.spongepowered.asm.mixin.gen.Invoker, RenderStateShard.EmptyTextureStateShard, rendertype, RenderType.CompositeRenderType, CompositeRenderTypeAccessor, EmptyTextureStateShardAccessor (+1 more)

### Community 89 - "StudioMod"
Cohesion: 0.14
Nodes (8): net.fabricmc.api.ClientModInitializer, State, LOADING, MENU, RUNNING, SETTLING, StudioBootstrap, StudioMod

### Community 90 - "rendersystem"
Cohesion: 0.24
Nodes (7): gl11, gl13, gl14, gl30, glstatemanager, rendersystem, shaderinstance

### Community 91 - "CurseForge upload checklist: Glintsmith 2.1.0"
Cohesion: 0.12
Nodes (16): Before 2.0.0 went public, Before 2.0.1 went public, Before 2.0.2 went public, Before 2.1.0 goes public, CurseForge upload checklist: Glintsmith 2.1.0, Earlier release: Glintsmith 1.0.0, Earlier release: Glintsmith 2.0.0 (uploaded 2026-10-18, file 9235330), Earlier release: Glintsmith 2.0.1 (uploaded 2026-10-18, file 9238522) (+8 more)

### Community 92 - "net.minecraft.resources.ResourceLocation"
Cohesion: 0.22
Nodes (4): net.minecraft.client.renderer.texture.TextureManager, net.minecraft.resources.ResourceLocation, FileEntry, GlintTextures

### Community 93 - "org.spongepowered.asm.mixin.injection.Redirect"
Cohesion: 0.17
Nodes (7): net.minecraft.client.renderer.RenderStateShard, org.spongepowered.asm.mixin.injection.Redirect, GlintShardsClockMixin, GlintSystemClockMixin, OutlineSystemClockMixin, RenderStateShardClockMixin, StudioClock

### Community 94 - "GlintKind"
Cohesion: 0.09
Nodes (14): CompositeStateBuilder, GlintKind, ARMOR_ENTITY_GLINT, ENTITY_GLINT, ENTITY_GLINT_DIRECT, GLINT, GLINT_TRANSLUCENT, ShaderStateShard (+6 more)

### Community 96 - "Entry"
Cohesion: 0.23
Nodes (8): Mode, FOOTER, HIDDEN, SIDE, Entry, CullStateShard, LayeringStateShard, CullStateShard

### Community 98 - "CurseForge page material for Glintsmith"
Cohesion: 0.40
Nodes (4): CurseForge page material for Glintsmith, Images, Putting the description on CurseForge, Rebuilding

### Community 99 - "gradlew"
Cohesion: 0.83
Nodes (3): gradlew script, die(), warn()

### Community 100 - "Condition"
Cohesion: 0.50
Nodes (4): Condition, ANY, ENCHANTED, NOT_ENCHANTED

### Community 106 - "PreviewLayoutTest"
Cohesion: 0.22
Nodes (3): Layout, PreviewLayout, PreviewLayoutTest

### Community 107 - "GlintsmithKeys.java"
Cohesion: 0.11
Nodes (13): clientcommandmanager, clientcommandregistrationcallback, clienttickevents, com.mojang.brigadier.CommandDispatcher, inputconstants, keybindinghelper, net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource, net.minecraft.client.KeyMapping (+5 more)

### Community 108 - "GlintAtlasScaleTest"
Cohesion: 0.24
Nodes (3): GlintAtlasScale, Preparations, GlintAtlasScaleTest

### Community 109 - "TeeCaptureTest"
Cohesion: 0.18
Nodes (4): java.nio.ByteBuffer, Override, OccluderBufferSource, TeeCaptureTest

### Community 110 - "StudioScreen"
Cohesion: 0.13
Nodes (8): com.google.gson.JsonArray, com.mojang.blaze3d.platform.NativeImage, net.minecraft.core.RegistryAccess, FrameWriter, Override, Job, Prop, StudioScreen

### Community 111 - "ColorPickerScreen"
Cohesion: 0.14
Nodes (4): java.util.function.IntConsumer, net.minecraft.client.gui.screens.Screen, ColorPickerScreen, Override

### Community 112 - "StudioBootstrap.java"
Cohesion: 0.13
Nodes (14): accessibilityonboardingscreen, difficulty, flatlevelgeneratorpreset, flatlevelgeneratorpresets, flatlevelsource, gamerules, gametype, levelsettings (+6 more)

### Community 113 - "Recorder"
Cohesion: 0.24
Nodes (4): Session 18: lineup.gif on the page, trident preview fix, 2.0.2 release, Override, Recorder, View

### Community 114 - "gif.py"
Cohesion: 0.23
Nodes (12): argparse, bar(), build_all(), build_gif(), caption_for(), encode(), frame_files(), main() (+4 more)

### Community 117 - "MaskTypes"
Cohesion: 0.18
Nodes (7): OutputStateShard, DepthTestStateShard, ShaderStateShard, TexturingStateShard, TransparencyStateShard, WriteMaskStateShard, MaskTypes

### Community 118 - "ModMenuIntegration.java"
Cohesion: 0.47
Nodes (4): com.terraformersmc.modmenu.api.ConfigScreenFactory, com.terraformersmc.modmenu.api.ModMenuApi, Override, ModMenuIntegration

### Community 119 - ".drawSubjects"
Cohesion: 0.25
Nodes (3): java.lang.reflect.Field, java.lang.reflect.Method, PipelineBridge

### Community 122 - "Changelog"
Cohesion: 0.12
Nodes (15): 1.0.0 (Fabric 1.21.1), 2.0.0 (Fabric 1.21.1) — 2026-10-18, 2.0.1 (Fabric 1.21.1) — 2026-10-18, 2.0.2 (Fabric 1.21.1) — 2026-10-18, 2.1.0 (Fabric 1.21.1) — 2026-10-18, Added, Added, Changed (+7 more)

### Community 123 - "Handoff: Glintsmith 2.1.0 (owner config as the built-in defaults, then upload)"
Cohesion: 0.17
Nodes (11): 10. How to resume, 1. Goal, 2. Requirements and constraints, 3. Done (this session, all committed and pushed unless noted), 4. In progress when stopped (UNCOMMITTED, unreviewed), 6. Known problems and blockers, 7. Decisions made, 8. Manual test checklist (+3 more)

### Community 124 - "ItemFacts"
Cohesion: 0.09
Nodes (10): ItemFacts, Override, Kind, ALL, EXACT, GLOB, NAMESPACE, TAG (+2 more)

### Community 125 - "GapFix.java"
Cohesion: 0.25
Nodes (3): 11. Model gap fix: AUTO values, Edge guard (2.0.1), net.minecraft.core.Direction

### Community 126 - "pipeline.py"
Cohesion: 0.08
Nodes (35): array, DEFAULT_STYLE with `overrides` (a nested dict, same keys) merged in., style(), A group of worker processes with an initializer, or the same thing in this…, Stage, box_sums(), _int(), matte_files() (+27 more)

### Community 128 - "Final report: Glintsmith 2.0.0"
Cohesion: 0.17
Nodes (11): 2.0.1 release (same day, owner go), Changed files (2.0.0, sessions 13 to 15), Checks and tests run (session 15), Final report: Glintsmith 2.0.0, Key decisions (full list in `decisions.md`), Known limitations and recommendations, Manual test checklist (owner; not yet done by a human), Model usage (+3 more)

### Community 129 - "GlintLook"
Cohesion: 0.24
Nodes (3): GlintCategories, Frame, GlintLook

### Community 131 - "Glintsmith art"
Cohesion: 0.18
Nodes (10): Glintsmith art, Licence and sources, Missing input is never faked, Output files, Testing the layout without renders, The GIFs, The logo and the icon, Theme (+2 more)

### Community 132 - "Handoff: Glintsmith lineup stray outline dot (root cause + outline fix)"
Cohesion: 0.17
Nodes (11): 10. How to resume, 1. Goal, 2. Requirements and constraints, 4. In progress when stopped, 5. Open work (ordered), 6. Known problems and blockers, 7. Decisions made, 8. Manual test checklist (+3 more)

### Community 133 - "Type"
Cohesion: 0.67
Nodes (3): Type, STILL, TURNTABLE

### Community 134 - "Versioning"
Cohesion: 0.29
Nodes (6): Glintsmith so far, How to choose, Release steps, Rules, Versioning, What each number means

### Community 139 - "ColorMode"
Cohesion: 0.11
Nodes (9): ColorEvaluator, Compiled, Override, ColorMode, ENCHANTMENT, GRADIENT, PULSE, RAINBOW (+1 more)

## Knowledge Gaps
- **181 isolated node(s):** `AUTO`, `CUSTOM`, `ENCHANTED`, `ALL`, `NONE` (+176 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 628 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **38 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `GlintsmithConfig` connect `GlintsmithConfig` to `.build`, `.t`, `.parseHex`, `ColorStyleScreen`, `org.jetbrains.annotations.Nullable`, `OutlineSystem`, `ConfigTest`, `Glintsmith`, `com.google.gson.JsonElement`, `EnchantmentColorsTab`, `.sanitize`, `net.minecraft.client.renderer.RenderType`, `net.minecraft.client.gui.GuiGraphics`, `RuleMatcherTest`, `LangFilesTest.java`, `OutlineTarget`, `Glintsmith.java`, `assertequals`, `GlintsmithScreen.java`, `RulesTab`, `.clamp`, `PreviewPanel`, `OutlineRuntimeSettings`, `RuleEngine`, `.drawItem`, `.load`, `LangFilesTest`, `StudioScreen.java`, `RuleEngineScopeTest`, `OutlineSystem.java`, `net.minecraft.resources.ResourceLocation`, `PreviewNotesTest`, `GlintsmithKeys.java`, `StudioScreen`, `ColorPickerScreen`, `ConfigManager`, `.drawSubjects`, `PendingEditsTest`, `.update`?**
  _High betweenness centrality (0.123) - this node is a cross-community bridge._
- **Why does `The manifest` connect `lib.py` to `GlintPresets`, `OutlineDepthTest`, `org.spongepowered.asm.mixin.Mixin`, `StyleTable`, `Canvas`?**
  _High betweenness centrality (0.082) - this node is a cross-community bridge._
- **Why does `OutlineSystem` connect `OutlineSystem` to `PhaseState`, `at`, `GlintsmithConfig`, `OutlineTarget`, `org.jetbrains.annotations.Nullable`, `Glintsmith.java`, `org.spongepowered.asm.mixin.Mixin`, `StudioScreen.java`, `TeeCaptureTest`, `OutlineSystem.java`, `TeeBufferSource`, `.drawSubjects`, `OutlineRuntimeSettings`, `OutlineScaleTest`, `org.spongepowered.asm.mixin.injection.Redirect`, `IrisCompat`, `.drawItem`?**
  _High betweenness centrality (0.062) - this node is a cross-community bridge._
- **What connects `AUTO`, `CUSTOM`, `ENCHANTED` to the rest of the system?**
  _181 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `logo.py` be split into smaller, more focused modules?**
  _Cohesion score 0.10160427807486631 - nodes in this community are weakly interconnected._
- **Should `GlintsmithConfig` be split into smaller, more focused modules?**
  _Cohesion score 0.10195035460992907 - nodes in this community are weakly interconnected._
- **Should `.t` be split into smaller, more focused modules?**
  _Cohesion score 0.09711779448621553 - nodes in this community are weakly interconnected._
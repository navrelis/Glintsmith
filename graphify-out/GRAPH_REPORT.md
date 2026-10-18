# Graph Report - eloquent-wing-a06858  (2026-10-18)

## Corpus Check
- 253 files · ~555,441 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 14 file(s) not represented in the graph (top: (none) 5, .fsh 3, .properties 2)

## Summary
- 3323 nodes · 10010 edges · 144 communities (108 shown, 36 thin omitted)
- Extraction: 91% EXTRACTED · 9% INFERRED · 0% AMBIGUOUS · INFERRED: 897 edges (avg confidence: 0.84)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `c0f759dc`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- GlintTexturesTest
- logo.py
- GlintPresets
- list
- .t
- .compile
- org.junit.jupiter.api.Test
- OutlineMixinApplicationTest
- OutlineSystem
- org.jetbrains.annotations.Nullable
- net.minecraft.client.renderer.MultiBufferSource
- .sanitize
- Glintsmith
- lib.py
- ColorStyleButton
- ConfigManager.java
- Indexer
- ConfigTab
- Canvas
- verify.py
- TeeBufferSource
- .from
- .trip
- GlintContext
- net.minecraft.client.renderer.RenderType
- ColorEvaluatorTest
- OptionList
- StyleTable
- gifenc.py
- com.mojang.blaze3d.vertex.VertexConsumer
- PhaseState
- ScreenBounds
- PatternBox
- .acquire
- RuleMatcherTest
- at
- FloatSlider
- Glintsmith
- LangFilesTest.java
- GlintMixinApplicationTest
- GlintsmithConfig
- OutlineDepthTest
- Glintsmith.java
- asserttrue
- OutlineShaders
- GlintsmithScreen
- studio.py
- ItemModelGeneratorMixin.java
- RulesTab
- SideElementBakeTest
- TeeVertexConsumer
- .clamp
- GapFix
- .resolve
- composite.py
- PreviewPanel
- StudioMod.java
- GuiHelpersTest
- net.minecraft.client.Minecraft
- ItemModelGeneratorSpanMixin.java
- RuleEngine
- Glintsmith render architecture (T2)
- IrisCompat
- net.minecraft.world.item.ItemStack
- Handoff: Glintsmith 2.0.1 (stray-dot fixes, release) — finished
- MaskBufferSource
- Tri
- .drawItem
- ColorMode
- preview.py
- GlintParams
- MaskTypesTest
- LangFilesTest
- StudioScreen.java
- .passes
- Glintsmith
- MaskVertexConsumer
- RuleEngineScopeTest
- Glintsmith Studio (dev-only render mode)
- synth_studio.py
- common.py
- enchantment
- Scene
- GlintSystem.java
- CompositeStateAccessor
- SpanMixin
- net.minecraft.resources.ResourceLocation
- ConfigSubScreen
- org.slf4j.Logger
- StudioMod
- rendersystem
- CurseForge upload checklist: Glintsmith 2.0.2
- GlintTextures
- org.spongepowered.asm.mixin.Mixin
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
- View
- OutlinePhase
- OutlineShaderResourcesTest
- GameRendererGlintMixin.java
- MaskTypes
- ModMenuIntegration.java
- PipelineBridge
- GlintCategorySettings
- PendingEditsTest
- Changelog
- FrameWriter
- Pattern
- PatternTest
- matte.py
- .update
- Final report: Glintsmith 2.0.0
- Requirements (Glintsmith, session 2026-10-17)
- .build
- Glintsmith art
- Handoff: Glintsmith lineup stray outline dot (root cause + outline fix)
- Type
- Versioning
- renderstateshard
- OccluderBufferSource
- ItemFacts
- GlintLook
- net.minecraft.server.packs.resources.ResourceManager
- Kind
- State

## God Nodes (most connected - your core abstractions)
1. `GlintsmithConfig` - 132 edges
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
- `Owner answers (2026-10-17)` --references--> `Glintsmith`  [INFERRED]
  .knowledge/requirements.md → src/main/java/navrelis/glintsmith/Glintsmith.java
- `Compatibility notes` --references--> `TeeVertexConsumer`  [INFERRED]
  docs/curseforge/description.md → src/main/java/navrelis/glintsmith/render/outline/TeeVertexConsumer.java
- `Compatibility notes` --references--> `TeeVertexConsumer`  [INFERRED]
  docs/curseforge/description_paste.md → src/main/java/navrelis/glintsmith/render/outline/TeeVertexConsumer.java
- `Compatibility` --references--> `TeeVertexConsumer`  [INFERRED]
  README.md → src/main/java/navrelis/glintsmith/render/outline/TeeVertexConsumer.java
- `Files` --references--> `StudioMod`  [INFERRED]
  docs/STUDIO.md → src/studio/java/navrelis/glintsmith/studio/StudioMod.java

## Import Cycles
- None detected.

## Communities (144 total, 36 thin omitted)

### Community 1 - "logo.py"
Cohesion: 0.10
Nodes (32): build(), _compose_task(), frame(), _frame_task(), glow_grids(), glow_layer(), _init_compose(), _init_frames() (+24 more)

### Community 2 - "GlintPresets"
Cohesion: 0.05
Nodes (22): 0. Rig, 1. World and rooms, 2. Glintsmith config, 3. In-world captures (14), 5. Checklist, Backdrop and close-ups, Enchantment shorthand, Frames and stand templates (+14 more)

### Community 3 - "list"
Cohesion: 0.11
Nodes (23): arraylist, assertthrows, collections, com.google.gson.Gson, directorystream, files, filetime, hashset (+15 more)

### Community 4 - ".t"
Cohesion: 0.09
Nodes (12): net.minecraft.client.gui.components.CycleButton, net.minecraft.client.gui.components.Tooltip, net.minecraft.network.chat.Component, OnPress, Override, RuleEditScreen, SettingRows, Override (+4 more)

### Community 5 - ".compile"
Cohesion: 0.07
Nodes (6): ColorEvaluator, DurabilityAlert, Parsed, EnchantmentColors, ColorsTest, DurabilityAndEnchantmentColorsTest

### Community 6 - "org.junit.jupiter.api.Test"
Cohesion: 0.08
Nodes (6): org.junit.jupiter.api.Test, GapFixTest, MixinApplicationTest, OutlineContextsTest, PreviewBoundsTest, SmokeTest

### Community 7 - "OutlineMixinApplicationTest"
Cohesion: 0.15
Nodes (3): org.objectweb.asm.tree.AnnotationNode, org.objectweb.asm.tree.MethodNode, OutlineMixinApplicationTest

### Community 8 - "OutlineSystem"
Cohesion: 0.09
Nodes (7): 12.2 The PREVIEW outline phase, 3.1 Phases, How the real glint and outline reach the subjects, net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext, net.minecraft.client.resources.model.ModelResourceLocation, BufferSource, OutlineSystem

### Community 9 - "org.jetbrains.annotations.Nullable"
Cohesion: 0.08
Nodes (34): builtinregistries, button, chatformatting, commoncomponents, component, confirmscreen, consumer, createworldscreen (+26 more)

### Community 10 - "net.minecraft.client.renderer.MultiBufferSource"
Cohesion: 0.13
Nodes (26): com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod, com.llamalad7.mixinextras.injector.wrapoperation.Operation, com.mojang.blaze3d.vertex.PoseStack, 4. Hook table, Glint (T6), Outline (T5), equipmentslot, net.minecraft.client.model.HumanoidModel (+18 more)

### Community 11 - ".sanitize"
Cohesion: 0.08
Nodes (3): ConfigManager, Holder, ConfigTest

### Community 13 - "lib.py"
Cohesion: 0.05
Nodes (72): anchor_x(), build_all(), build_card(), chip_list(), Ctx, el_bullets(), el_capture(), el_chips() (+64 more)

### Community 14 - "ColorStyleButton"
Cohesion: 0.21
Nodes (4): Theme, net.minecraft.client.gui.components.AbstractButton, ColorStyleButton, Override

### Community 15 - "ConfigManager.java"
Cohesion: 0.10
Nodes (18): abstractappender, assumptions, atomicinteger, atomicmovenotsupportedexception, basicfileattributes, copyonwritearraylist, filechannel, jsonparseexception (+10 more)

### Community 16 - "Indexer"
Cohesion: 0.09
Nodes (15): bayer8(), Indexer, init_indexer(), _Lookup, Palette, key -> palette index when the nearest colour is close enough to be used as it…, Turns RGB frames into palette indices (see the module docstring, step 3). base:…, Nearest palette index of every pixel, no dither. (+7 more)

### Community 17 - "ConfigTab"
Cohesion: 0.09
Nodes (9): net.minecraft.client.gui.components.tabs.Tab, net.minecraft.client.gui.navigation.ScreenRectangle, ConfigTab, Override, EnchantmentColorsTab, EnchantmentRow, Override, GlintTab (+1 more)

### Community 18 - "Canvas"
Cohesion: 0.09
Nodes (11): Canvas, An RGBA canvas on a bytearray. Coordinates outside the canvas are clipped,…, A new canvas of the region; parts outside this canvas are transparent., Nearest-neighbour upscale by the whole factor k., Box-filter downscale by the whole factor n (every output pixel is the mean of…, Area-filter resize to ow x oh: every output pixel is the exact mean of the…, Draw src at (x, y): transparent pixels are skipped, half-transparent ones…, PNG bytes: RGB when fully opaque, RGBA otherwise (text: optional tEXt comment). (+3 more)

### Community 19 - "verify.py"
Cohesion: 0.08
Nodes (49): _chunk(), parse_gif(), png_bytes(), pixels: bytes-like, w*h*channels, row-major. channels 3 = RGB, 4 = RGBA. text:…, Parse back a GIF written by gif_bytes (or any simple GIF). Returns dict with…, contrast(), luminance(), WCAG relative luminance of an RGB(A) colour (0 black .. 1 white). (+41 more)

### Community 20 - "TeeBufferSource"
Cohesion: 0.17
Nodes (4): Override, TeeBufferSource, TeePool, GuiItemFlushTest

### Community 22 - ".trip"
Cohesion: 0.09
Nodes (8): Black/white pairs, Camera and light, Render target and supersampling, Studio clock, What happens during a run, GlintShards, Preparations, GlintFailsafeTest

### Community 23 - "GlintContext"
Cohesion: 0.14
Nodes (4): GlintCategories, Frame, GlintContext, GlintContextTest

### Community 24 - "net.minecraft.client.renderer.RenderType"
Cohesion: 0.13
Nodes (8): 12.1 Mechanism, 5.2 Strategy, 8.2 T6: glint system (`navrelis.glintsmith.render.glint`), net.minecraft.client.renderer.RenderType, GlintRuntimeSettings, GlintSystem, BufferSource, GlintSystemTest

### Community 26 - "OptionList"
Cohesion: 0.13
Nodes (10): net.minecraft.client.gui.components.AbstractWidget, net.minecraft.client.gui.components.ContainerObjectSelectionList, net.minecraft.client.gui.components.events.GuiEventListener, net.minecraft.client.gui.narration.NarratableEntry, net.minecraft.util.FormattedCharSequence, Override, LabeledRow, OptionList (+2 more)

### Community 27 - "StyleTable"
Cohesion: 0.05
Nodes (11): 3.4.1 Proportional thickness (2.0.0), 3.4 Per-item data encoding, org.joml.Matrix4f, org.joml.Matrix4fc, OutlineRuntimeSettings, OutlineScale, StyleTable, OutlineRuntimeSettingsTest (+3 more)

### Community 28 - "gifenc.py"
Cohesion: 0.04
Nodes (56): colour_histogram(), encode_gif_frames(), gif_bytes(), gif_compose(), lzw_decode(), lzw_encode(), median_cut(), Dependency-free image encoders (PNG, animated GIF89a) and a small GIF decoder… (+48 more)

### Community 29 - "com.mojang.blaze3d.vertex.VertexConsumer"
Cohesion: 0.22
Nodes (4): com.mojang.blaze3d.vertex.VertexConsumer, Override, Recorder, Throwing

### Community 30 - "PhaseState"
Cohesion: 0.14
Nodes (8): 3.3 Targets, depth and resize, GlState, MaskDepth, CLEAR, COPY, NONE, OutlineCompositor, PhaseState

### Community 32 - "PatternBox"
Cohesion: 0.21
Nodes (4): net.minecraft.network.chat.Style, PatternText, Override, PatternBox

### Community 33 - ".acquire"
Cohesion: 0.15
Nodes (5): 4. Studio renders, Still scenes (4), Turntables (3), FakeType, GlintSlotPoolTest

### Community 35 - "at"
Cohesion: 0.11
Nodes (20): at, com.mojang.blaze3d.vertex.BufferBuilder, net.minecraft.client.renderer.texture.TextureAtlas, org.spongepowered.asm.mixin.injection.callback.CallbackInfo, org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable, org.spongepowered.asm.mixin.injection.Inject, org.spongepowered.asm.mixin.injection.ModifyVariable, org.spongepowered.asm.mixin.Pseudo (+12 more)

### Community 36 - "FloatSlider"
Cohesion: 0.17
Nodes (10): function, java.util.function.DoubleConsumer, java.util.function.DoubleFunction, java.util.function.DoubleToIntFunction, mth, net.minecraft.client.gui.components.AbstractSliderButton, FloatSlider, Override (+2 more)

### Community 37 - "Glintsmith"
Cohesion: 0.08
Nodes (22): Commands and keys, Compatibility notes, Credits and licence, Glintsmith, How it compares, Performance, Requirements, Sodium and Iris (+14 more)

### Community 38 - "LangFilesTest.java"
Cohesion: 0.09
Nodes (33): abstractinsnnode, classreader, datetimeformatter, enumeration, executors, executorservice, fabriclauncherbase, fieldinsnnode (+25 more)

### Community 40 - "GlintsmithConfig"
Cohesion: 0.09
Nodes (19): nullable, GlintsmithConfig, OutlineContexts, GlintCategory, ARMOR, ENTITY, GUI, ITEM (+11 more)

### Community 42 - "Glintsmith.java"
Cohesion: 0.09
Nodes (14): arrays, commonlifecycleevents, jsonarray, jsonprimitive, GapFixMode, AUTO, CUSTOM, GapFix (+6 more)

### Community 43 - "asserttrue"
Cohesion: 0.10
Nodes (24): assertarrayequals, assertdoesnotthrow, assertequals, assertfalse, assertinstanceof, assertnotequals, assertnotnull, assertnotsame (+16 more)

### Community 44 - "OutlineShaders"
Cohesion: 0.15
Nodes (9): com.mojang.blaze3d.vertex.VertexBuffer, com.mojang.blaze3d.vertex.VertexFormat, defaultvertexformat, gl20, net.minecraft.client.renderer.ShaderInstance, net.minecraft.server.packs.resources.ResourceProvider, resourcemanager, FullscreenQuad (+1 more)

### Community 45 - "GlintsmithScreen"
Cohesion: 0.14
Nodes (5): net.minecraft.client.gui.components.tabs.TabManager, net.minecraft.client.gui.components.tabs.TabNavigationBar, GlintsmithScreen, Override, SystemToastId

### Community 46 - "studio.py"
Cohesion: 0.10
Nodes (29): The studio scenes (the contract), find_frame(), load(), Studio scenes: finding, loading and validating the frame contract. Python 3…, The frame dict for `ref`: a still name, a frame index (1 based) or a file stem…, A scene that is not there (kind 'missing') or not readable (kind 'error')., (K path, W path) of a frame dict., The scene from art/captures/studio/<scene_id>/scene.json. Raises SceneError. (+21 more)

### Community 47 - "ItemModelGeneratorMixin.java"
Cohesion: 0.20
Nodes (9): blockelementface, com.llamalad7.mixinextras.injector.ModifyReturnValue, direction, inventorymenu, local, net.minecraft.client.renderer.texture.SpriteContents, net.minecraft.client.renderer.texture.TextureAtlasSprite, ItemModelGeneratorMixin (+1 more)

### Community 48 - "RulesTab"
Cohesion: 0.20
Nodes (4): net.minecraft.client.gui.components.Checkbox, Override, RuleRow, RulesTab

### Community 49 - "SideElementBakeTest"
Cohesion: 0.19
Nodes (5): net.minecraft.client.renderer.block.model.BakedQuad, net.minecraft.client.renderer.block.model.BlockElement, net.minecraft.client.resources.model.ModelState, org.junit.jupiter.api.AfterAll, SideElementBakeTest

### Community 51 - ".clamp"
Cohesion: 0.15
Nodes (3): Outline, Targets, TargetThickness

### Community 52 - "GapFix"
Cohesion: 0.14
Nodes (5): 11. Model gap fix: AUTO values, Edge guard (2.0.1), net.minecraft.core.Direction, GapFix, Settings

### Community 53 - ".resolve"
Cohesion: 0.22
Nodes (6): net.minecraft.world.item.Item, OutlineLook, ResolvedStyle, Override, StyleKey, RuleEngineKeyTest

### Community 54 - "composite.py"
Cohesion: 0.05
Nodes (57): alpha_bbox(), ascii_text(), backdrop(), _blend_plane(), blur_grid(), _blur_rows(), compose(), contact_plane() (+49 more)

### Community 55 - "PreviewPanel"
Cohesion: 0.09
Nodes (12): net.minecraft.client.gui.Font, net.minecraft.client.gui.narration.NarrationElementOutput, net.minecraft.client.sounds.SoundManager, ColorStyleScreen, Override, PreviewHost, Override, PreviewPanel (+4 more)

### Community 56 - "StudioMod.java"
Cohesion: 0.20
Nodes (9): clientlifecycleevents, collection, graphicsstatus, net.fabricmc.api.ClientModInitializer, packtype, resourcemanagerhelper, resourcereloadlistenerkeys, soundsource (+1 more)

### Community 57 - "GuiHelpersTest"
Cohesion: 0.18
Nodes (3): Hsv, TextureNames, GuiHelpersTest

### Community 58 - "net.minecraft.client.Minecraft"
Cohesion: 0.23
Nodes (4): net.minecraft.client.Minecraft, PreviewItems, Sample, MinecraftExitMixin

### Community 59 - "ItemModelGeneratorSpanMixin.java"
Cohesion: 0.25
Nodes (6): coerce, com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation, net.minecraft.client.renderer.block.model.ItemModelGenerator, ItemModelGeneratorSpanMixin, SpanAccess, unique

### Community 60 - "RuleEngine"
Cohesion: 0.27
Nodes (4): 9. Risks and open questions (each with a recommendation), net.minecraft.world.item.enchantment.ItemEnchantments, EnchantKey, RuleEngine

### Community 61 - "Glintsmith render architecture (T2)"
Cohesion: 0.18
Nodes (10): 1. Decision summary, 2. Frame timeline in 1.21.1 and where Glintsmith acts, 3.5 Shaders, 3.6 Composite procedure (`OutlineCompositor.composite(PhaseState, Params)`), 3.7 Performance and allocation rules (T5), 3.8 Failsafe (T5), 3. Outline pipeline in detail (T5), 6. Compatibility matrix and failure behaviour (+2 more)

### Community 62 - "IrisCompat"
Cohesion: 0.17
Nodes (4): 12.5 Panel and layout, IrisApi, Api, IrisCompat

### Community 63 - "net.minecraft.world.item.ItemStack"
Cohesion: 0.12
Nodes (20): itemrenderer, itemtags, lighting, lighttexture, method, net.minecraft.client.renderer.block.model.ItemTransform, net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer, net.minecraft.client.renderer.entity.ItemRenderer (+12 more)

### Community 64 - "Handoff: Glintsmith 2.0.1 (stray-dot fixes, release) — finished"
Cohesion: 0.15
Nodes (12): 10. How to resume, 1. Goal, 2. Requirements and constraints, 3. Done (all committed and pushed; main = dev = b05998a, tag v2.0.1), 4. In progress when stopped, 5. Open work (owner chores, ordered), 6. Known problems and blockers, 7. Decisions made (+4 more)

### Community 66 - "Tri"
Cohesion: 0.33
Nodes (4): Tri, INHERIT, OFF, ON

### Community 67 - ".drawItem"
Cohesion: 0.11
Nodes (10): axis, FunctionalInterface, org.joml.Matrix3f, quaternionf, Fit, PreviewPose, BufferSource, Margin (+2 more)

### Community 68 - "ColorMode"
Cohesion: 0.29
Nodes (6): ColorMode, ENCHANTMENT, GRADIENT, PULSE, RAINBOW, STATIC

### Community 69 - "preview.py"
Cohesion: 0.21
Nodes (13): build(), check(), inline(), main(), pipeline_outputs(), Builds docs/curseforge/preview.html from description.md and checks the marker…, The image names the art pipeline builds (the "out" of every card and GIF of…, render() (+5 more)

### Community 70 - "GlintParams"
Cohesion: 0.14
Nodes (8): 5.1 How vanilla 1.21.1 draws glint (verified), 5.3 Categories, context and resolution, 5.4.1 Atlas-independent scale (2.0.0), 5.4 Slot render types, 5.7 Edge cases T6 must handle, 5. Glint system in detail (T6), GlintParams, GlintParamsTest

### Community 71 - "MaskTypesTest"
Cohesion: 0.14
Nodes (4): LayeringStateShard, WriteMaskStateShard, CullStateShard, MaskTypesTest

### Community 72 - "LangFilesTest"
Cohesion: 0.22
Nodes (6): 12.3 The snapshot and the pending edits chain, 12.4 What is shown, 12.6 Limits (known and by design), 12.7 Tests and what is still open in game, 12. Live preview (2.0.0), LangFilesTest

### Community 73 - "StudioScreen.java"
Cohesion: 0.05
Nodes (39): bootstrap, compoundtag, datacomponents, enchantments, entityrenderdispatcher, entitytype, facebakery, fogrenderer (+31 more)

### Community 74 - ".passes"
Cohesion: 0.33
Nodes (3): com.mojang.blaze3d.pipeline.RenderTarget, com.mojang.blaze3d.pipeline.TextureTarget, OutlineTargets

### Community 75 - "Glintsmith"
Cohesion: 0.20
Nodes (9): Commands and keys, Compatibility notes, Credits and licence, Glintsmith, How it compares, Performance, Requirements, Sodium and Iris (+1 more)

### Community 78 - "Glintsmith Studio (dev-only render mode)"
Cohesion: 0.18
Nodes (8): Example scenes, Files, Frame contract (output), Glint scale, Glintsmith Studio (dev-only render mode), Limits, Running a scene, Scene format (input)

### Community 79 - "synth_studio.py"
Cohesion: 0.10
Nodes (25): draw_sword(), Frame, main(), make_contract(), turntable(), make_scene(), Fill a convex polygon; stripe = (centre at y=0, slope, half width, colour)…, Draw one extruded sword centred on (cx, cy) with unit length u (supersampled… (+17 more)

### Community 80 - "common.py"
Cohesion: 0.05
Nodes (41): argparse, main(), Captures, cut(), cut_el(), load_manifest(), out_size(), picture_size() (+33 more)

### Community 82 - "Scene"
Cohesion: 0.19
Nodes (11): com.google.gson.JsonElement, com.google.gson.JsonObject, org.joml.Vector3f, Camera, Kind, ARMOR_STAND, ITEM, Look (+3 more)

### Community 83 - "GlintSystem.java"
Cohesion: 0.18
Nodes (9): com.mojang.blaze3d.vertex.ByteBufferBuilder, itemstack, java.util.SequencedMap, net.minecraft.client.renderer.RenderBuffers, object2objectsortedmaps, BufferSourceAccessor, MultiBufferSource.BufferSource, RenderBuffersAccessor (+1 more)

### Community 84 - "CompositeStateAccessor"
Cohesion: 0.16
Nodes (10): 3.2 Teeing (capture), EmptyTextureStateShard, org.spongepowered.asm.mixin.gen.Accessor, RenderType.CompositeState, BufferSource, CompositeStateAccessor, CullStateShard, DepthTestStateShard (+2 more)

### Community 85 - "SpanMixin"
Cohesion: 0.43
Nodes (3): org.spongepowered.asm.mixin.Shadow, Override, SpanMixin

### Community 86 - "net.minecraft.resources.ResourceLocation"
Cohesion: 0.30
Nodes (6): java.util.jar.JarFile, net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener, net.minecraft.resources.ResourceLocation, Override, ReloadCounter, Sprite

### Community 88 - "org.slf4j.Logger"
Cohesion: 0.10
Nodes (10): fabricloader, loggerfactory, org.slf4j.Logger, org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin, org.spongepowered.asm.mixin.extensibility.IMixinInfo, GlintsmithMixinPlugin, Override, OutlineFailsafe (+2 more)

### Community 90 - "rendersystem"
Cohesion: 0.18
Nodes (9): abstracttexture, gl11, gl13, gl14, gl30, glstatemanager, options, rendersystem (+1 more)

### Community 91 - "CurseForge upload checklist: Glintsmith 2.0.2"
Cohesion: 0.14
Nodes (13): Before 2.0.0 went public, Before 2.0.1 went public, Before 2.0.2 goes public, CurseForge upload checklist: Glintsmith 2.0.2, Earlier release: Glintsmith 1.0.0, Earlier release: Glintsmith 2.0.0 (uploaded 2026-10-18, file 9235330), Earlier release: Glintsmith 2.0.1 (uploaded 2026-10-18, file 9238522), Relations (+5 more)

### Community 92 - "GlintTextures"
Cohesion: 0.22
Nodes (3): net.minecraft.client.renderer.texture.TextureManager, FileEntry, GlintTextures

### Community 93 - "org.spongepowered.asm.mixin.Mixin"
Cohesion: 0.11
Nodes (16): CompositeState, net.minecraft.client.renderer.RenderStateShard, org.spongepowered.asm.mixin.gen.Invoker, org.spongepowered.asm.mixin.injection.Redirect, org.spongepowered.asm.mixin.Mixin, RenderStateShard.EmptyTextureStateShard, rendertype, RenderType.CompositeRenderType (+8 more)

### Community 94 - "GlintKind"
Cohesion: 0.10
Nodes (14): CompositeStateBuilder, GlintKind, ARMOR_ENTITY_GLINT, ENTITY_GLINT, ENTITY_GLINT_DIRECT, GLINT, GLINT_TRANSLUCENT, ShaderStateShard (+6 more)

### Community 96 - "Entry"
Cohesion: 0.30
Nodes (7): Mode, FOOTER, HIDDEN, SIDE, Entry, CullStateShard, LayeringStateShard

### Community 98 - "CurseForge page material for Glintsmith"
Cohesion: 0.40
Nodes (4): CurseForge page material for Glintsmith, Images, Putting the description on CurseForge, Rebuilding

### Community 99 - "gradlew"
Cohesion: 0.83
Nodes (3): gradlew script, die(), warn()

### Community 100 - "Condition"
Cohesion: 0.50
Nodes (4): Condition, ANY, ENCHANTED, NOT_ENCHANTED

### Community 107 - "GlintsmithKeys.java"
Cohesion: 0.11
Nodes (13): clientcommandmanager, clientcommandregistrationcallback, clienttickevents, com.mojang.brigadier.CommandDispatcher, inputconstants, keybindinghelper, net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource, net.minecraft.client.KeyMapping (+5 more)

### Community 109 - "TeeCaptureTest"
Cohesion: 0.21
Nodes (4): 5.5 Texture management (`GlintTextures`), java.nio.ByteBuffer, RecordingSource, TeeCaptureTest

### Community 110 - "StudioScreen"
Cohesion: 0.17
Nodes (5): net.minecraft.core.RegistryAccess, Override, Job, Prop, StudioScreen

### Community 111 - "ColorPickerScreen"
Cohesion: 0.14
Nodes (4): java.util.function.IntConsumer, net.minecraft.client.gui.screens.Screen, ColorPickerScreen, Override

### Community 112 - "StudioBootstrap.java"
Cohesion: 0.13
Nodes (14): accessibilityonboardingscreen, difficulty, flatlevelgeneratorpreset, flatlevelgeneratorpresets, flatlevelsource, gamerules, gametype, levelsettings (+6 more)

### Community 113 - "View"
Cohesion: 0.27
Nodes (3): Override, Recorder, View

### Community 114 - "OutlinePhase"
Cohesion: 0.18
Nodes (6): OutlinePhase, GUI, HAND, NONE, PREVIEW, WORLD

### Community 116 - "GameRendererGlintMixin.java"
Cohesion: 0.39
Nodes (4): net.minecraft.client.DeltaTracker, net.minecraft.client.renderer.GameRenderer, GameRendererGlintMixin, GameRendererOutlineMixin

### Community 117 - "MaskTypes"
Cohesion: 0.18
Nodes (7): OutputStateShard, DepthTestStateShard, ShaderStateShard, TexturingStateShard, TransparencyStateShard, WriteMaskStateShard, MaskTypes

### Community 118 - "ModMenuIntegration.java"
Cohesion: 0.47
Nodes (4): com.terraformersmc.modmenu.api.ConfigScreenFactory, com.terraformersmc.modmenu.api.ModMenuApi, Override, ModMenuIntegration

### Community 119 - "PipelineBridge"
Cohesion: 0.38
Nodes (3): java.lang.reflect.Field, java.lang.reflect.Method, PipelineBridge

### Community 122 - "Changelog"
Cohesion: 0.20
Nodes (9): 2.0.0 (Fabric 1.21.1) — 2026-10-18, 2.0.1 (Fabric 1.21.1) — 2026-10-18, 2.0.2 (Fabric 1.21.1) — 2026-10-18, Added, Changed, Changelog, Fixed, Fixed (+1 more)

### Community 123 - "FrameWriter"
Cohesion: 0.32
Nodes (3): com.google.gson.JsonArray, com.mojang.blaze3d.platform.NativeImage, FrameWriter

### Community 126 - "matte.py"
Cohesion: 0.20
Nodes (13): array, box_sums(), _int(), matte_files(), matte_pair(), premultiplied_planes(), Studio matte: turns a black / white render pair into a straight-alpha RGBA…, Straight-alpha RGBA Canvas (width // S x height // S) from the K and W renders… (+5 more)

### Community 128 - "Final report: Glintsmith 2.0.0"
Cohesion: 0.17
Nodes (11): 2.0.1 release (same day, owner go), Changed files (2.0.0, sessions 13 to 15), Checks and tests run (session 15), Final report: Glintsmith 2.0.0, Key decisions (full list in `decisions.md`), Known limitations and recommendations, Manual test checklist (owner; not yet done by a human), Model usage (+3 more)

### Community 129 - "Requirements (Glintsmith, session 2026-10-17)"
Cohesion: 0.25
Nodes (7): Environment facts (verified 2026-10-17), Later owner additions (chat, 2026-10-17), Owner answers (2026-10-17), Owner answers (2026-10-18, 2.0.1 follow-up), Owner task (2026-10-17), Requirements (Glintsmith, session 2026-10-17), Session 12 owner additions (2026-10-18)

### Community 131 - "Glintsmith art"
Cohesion: 0.20
Nodes (9): Glintsmith art, Licence and sources, Missing input is never faked, Output files, Testing the layout without renders, The GIFs, The logo and the icon, Verify (+1 more)

### Community 132 - "Handoff: Glintsmith lineup stray outline dot (root cause + outline fix)"
Cohesion: 0.15
Nodes (12): 10. How to resume, 1. Goal, 2. Requirements and constraints, 3. Done, 4. In progress when stopped, 5. Open work (ordered), 6. Known problems and blockers, 7. Decisions made (+4 more)

### Community 133 - "Type"
Cohesion: 0.67
Nodes (3): Type, STILL, TURNTABLE

### Community 134 - "Versioning"
Cohesion: 0.29
Nodes (6): Glintsmith so far, How to choose, Release steps, Rules, Versioning, What each number means

### Community 139 - "GlintLook"
Cohesion: 0.14
Nodes (6): 8.1 T5: outline core (`navrelis.glintsmith.render.outline`), 8.3 T8: exact wiring, 8. Package and class spec, resources, and T8 hooks, Compiled, Override, GlintLook

### Community 141 - "net.minecraft.server.packs.resources.ResourceManager"
Cohesion: 0.16
Nodes (9): 1.0.0 (Fabric 1.21.1), Added, Fixed, Notes, net.minecraft.server.packs.resources.ResourceManager, Override, TextureReloadListener, Override (+1 more)

### Community 142 - "Kind"
Cohesion: 0.29
Nodes (6): Kind, ALL, EXACT, GLOB, NAMESPACE, TAG

### Community 144 - "State"
Cohesion: 0.40
Nodes (5): State, LOADING, MENU, RUNNING, SETTLING

## Knowledge Gaps
- **169 isolated node(s):** `AUTO`, `CUSTOM`, `ENCHANTED`, `ALL`, `NONE` (+164 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 616 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **36 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `GlintsmithConfig` connect `GlintsmithConfig` to `.build`, `list`, `.t`, `.compile`, `OutlineSystem`, `org.jetbrains.annotations.Nullable`, `ItemFacts`, `.sanitize`, `Glintsmith`, `.outlineAt`, `ConfigTab`, `.from`, `.trip`, `net.minecraft.client.renderer.RenderType`, `StyleTable`, `RuleMatcherTest`, `Glintsmith.java`, `asserttrue`, `GlintsmithScreen`, `RulesTab`, `.clamp`, `.resolve`, `PreviewPanel`, `RuleEngine`, `IrisCompat`, `net.minecraft.world.item.ItemStack`, `.drawItem`, `StudioScreen.java`, `RuleEngineScopeTest`, `GlintSystem.java`, `GlintTextures`, `PreviewNotesTest`, `GlintsmithKeys.java`, `StudioScreen`, `ColorPickerScreen`, `PipelineBridge`, `GlintCategorySettings`, `PendingEditsTest`, `.update`?**
  _High betweenness centrality (0.134) - this node is a cross-community bridge._
- **Why does `The manifest` connect `lib.py` to `GlintPresets`, `OutlineDepthTest`, `common.py`, `Canvas`, `StyleTable`, `net.minecraft.world.item.ItemStack`?**
  _High betweenness centrality (0.079) - this node is a cross-community bridge._
- **Why does `OutlineSystem` connect `OutlineSystem` to `at`, `GlintsmithConfig`, `org.jetbrains.annotations.Nullable`, `Glintsmith.java`, `net.minecraft.client.renderer.MultiBufferSource`, `OccluderBufferSource`, `.sanitize`, `net.minecraft.server.packs.resources.ResourceManager`, `StudioScreen.java`, `GameRendererGlintMixin.java`, `TeeBufferSource`, `PipelineBridge`, `org.slf4j.Logger`, `IrisCompat`, `StyleTable`, `org.spongepowered.asm.mixin.Mixin`, `PhaseState`, `net.minecraft.world.item.ItemStack`?**
  _High betweenness centrality (0.048) - this node is a cross-community bridge._
- **What connects `AUTO`, `CUSTOM`, `ENCHANTED` to the rest of the system?**
  _169 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `logo.py` be split into smaller, more focused modules?**
  _Cohesion score 0.10160427807486631 - nodes in this community are weakly interconnected._
- **Should `GlintPresets` be split into smaller, more focused modules?**
  _Cohesion score 0.05307017543859649 - nodes in this community are weakly interconnected._
- **Should `list` be split into smaller, more focused modules?**
  _Cohesion score 0.11103896103896103 - nodes in this community are weakly interconnected._
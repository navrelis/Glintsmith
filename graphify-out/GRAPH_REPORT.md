# Graph Report - eloquent-wing-a06858  (2026-10-18)

## Corpus Check
- 250 files · ~551,070 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 14 file(s) not represented in the graph (top: (none) 5, .fsh 3, .properties 2)

## Summary
- 3284 nodes · 9961 edges · 145 communities (102 shown, 43 thin omitted)
- Extraction: 91% EXTRACTED · 9% INFERRED · 0% AMBIGUOUS · INFERRED: 895 edges (avg confidence: 0.84)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `743467f1`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- GlintTexturesTest
- logo.py
- GlintNoise
- list
- .t
- .parseHex
- org.junit.jupiter.api.Test
- OutlineMixinApplicationTest
- OutlineSystem
- GlintsmithConfig
- net.minecraft.world.item.ItemStack
- .sanitize
- Glintsmith
- lib.py
- PreviewPanel.java
- ConfigManager.java
- gif.py
- EnchantmentColorsTab
- Canvas
- verify.py
- TeeBufferSource
- .from
- GlintShards
- GlintContext
- GlintSystem
- ColorEvaluatorTest
- net.minecraft.network.chat.Component
- OutlineScaleTest
- gifenc.py
- com.mojang.blaze3d.vertex.VertexConsumer
- PhaseState
- ScreenBounds
- PatternBox
- .acquire
- RuleMatcherTest
- org.spongepowered.asm.mixin.injection.Inject
- FloatSlider
- Glintsmith
- LangFilesTest.java
- MaskTypes.java
- OutlineTarget
- OutlineDepthTest
- StyleTable
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
- GlintPresets
- composite.py
- PreviewPanel
- OutlineSystem.java
- GuiHelpersTest
- GlintPresetsTest
- ItemModelGeneratorSpanMixin.java
- RuleEngine
- GlintLook
- IrisCompat
- BoundsKey
- OutlineRuntimeSettings
- MaskBufferSource
- org.jetbrains.annotations.Nullable
- .drawItem
- ColorStyle
- sys
- .pick
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
- OccluderBufferSource.java
- CompositeStateAccessor
- SpanMixin
- net.minecraft.resources.ResourceLocation
- ConfigSubScreen
- OutlineFailsafe
- StudioMod
- rendersystem
- CurseForge upload checklist: Glintsmith 2.0.0
- GlintTextures
- org.spongepowered.asm.mixin.Mixin
- GlintKind
- .validateTexture
- net.minecraft.client.renderer.RenderType
- .window
- CurseForge page material for Glintsmith
- gradlew
- Condition
- Plan (Glintsmith 1.0.0)
- decisions.md
- log.md
- PreviewNotesTest
- PreviewLayoutTest
- .build
- GlintAtlasScaleTest
- TeeCaptureTest
- StudioScreen
- ColorPickerScreen
- net.minecraft.client.Minecraft
- Glintsmith capture shot list
- OutlinePhase
- OutlineShaderResourcesTest
- SideElementBakeTest.java
- MaskTypes
- ModMenuIntegration.java
- PipelineBridge
- GlintCategorySettings
- PendingEditsTest
- 1.0.0 (Fabric 1.21.1)
- FrameWriter
- Pattern
- PatternTest
- matte.py
- .update
- Final report: Glintsmith 2.0.0
- net.minecraft.client.gui.GuiGraphics
- .build
- Glintsmith art
- Handoff: Glintsmith lineup stray outline dot (root cause + outline fix)
- Type
- DurabilityAlert
- renderstateshard
- org.joml.Vector3f
- EnchantmentColors
- ColorEvaluator
- Compiled
- .thicknessScale
- .buildNeutral
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
- `Compatibility notes` --references--> `TeeVertexConsumer`  [INFERRED]
  docs/curseforge/description.md → src/main/java/navrelis/glintsmith/render/outline/TeeVertexConsumer.java
- `Compatibility notes` --references--> `TeeVertexConsumer`  [INFERRED]
  docs/curseforge/description_paste.md → src/main/java/navrelis/glintsmith/render/outline/TeeVertexConsumer.java
- `Compatibility` --references--> `TeeVertexConsumer`  [INFERRED]
  README.md → src/main/java/navrelis/glintsmith/render/outline/TeeVertexConsumer.java
- `Files` --references--> `StudioMod`  [INFERRED]
  docs/STUDIO.md → src/studio/java/navrelis/glintsmith/studio/StudioMod.java
- `Owner answers (2026-10-17)` --references--> `Glintsmith`  [INFERRED]
  .knowledge/requirements.md → src/main/java/navrelis/glintsmith/Glintsmith.java

## Import Cycles
- None detected.

## Communities (145 total, 43 thin omitted)

### Community 1 - "logo.py"
Cohesion: 0.10
Nodes (32): build(), _compose_task(), frame(), _frame_task(), glow_grids(), glow_layer(), _init_compose(), _init_frames() (+24 more)

### Community 3 - "list"
Cohesion: 0.13
Nodes (22): arraylist, assertthrows, datetimeformatter, directorystream, executors, executorservice, files, filetime (+14 more)

### Community 4 - ".t"
Cohesion: 0.12
Nodes (9): net.minecraft.client.gui.components.CycleButton, net.minecraft.client.gui.components.Tooltip, OnPress, Override, Override, ModelFixTab, Override, OutlineTab (+1 more)

### Community 6 - "org.junit.jupiter.api.Test"
Cohesion: 0.08
Nodes (6): org.junit.jupiter.api.Test, GapFixTest, GlintsmithMixinPluginTest, MixinApplicationTest, OutlineContextsTest, SmokeTest

### Community 7 - "OutlineMixinApplicationTest"
Cohesion: 0.07
Nodes (9): org.objectweb.asm.tree.AnnotationNode, org.objectweb.asm.tree.ClassNode, org.objectweb.asm.tree.MethodNode, org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin, org.spongepowered.asm.mixin.extensibility.IMixinInfo, GlintsmithMixinPlugin, Override, GlintMixinApplicationTest (+1 more)

### Community 8 - "OutlineSystem"
Cohesion: 0.09
Nodes (6): 12.2 The PREVIEW outline phase, How the real glint and outline reach the subjects, net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext, OccluderBufferSource, BufferSource, OutlineSystem

### Community 9 - "GlintsmithConfig"
Cohesion: 0.08
Nodes (29): button, chatformatting, collections, com.google.gson.Gson, commoncomponents, component, confirmscreen, consumer (+21 more)

### Community 10 - "net.minecraft.world.item.ItemStack"
Cohesion: 0.10
Nodes (32): com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod, com.llamalad7.mixinextras.injector.wrapoperation.Operation, com.mojang.blaze3d.vertex.PoseStack, Outline (T5), equipmentslot, itemtags, net.minecraft.client.model.HumanoidModel, net.minecraft.client.player.LocalPlayer (+24 more)

### Community 12 - "Glintsmith"
Cohesion: 0.17
Nodes (9): Environment facts (verified 2026-10-17), Later owner additions (chat, 2026-10-17), Owner answers (2026-10-17), Owner answers (2026-10-18, 2.0.1 follow-up), Owner task (2026-10-17), Requirements (Glintsmith, session 2026-10-17), Session 12 owner additions (2026-10-18), Glintsmith (+1 more)

### Community 13 - "lib.py"
Cohesion: 0.05
Nodes (72): anchor_x(), build_all(), build_card(), chip_list(), Ctx, el_bullets(), el_capture(), el_chips() (+64 more)

### Community 14 - "PreviewPanel.java"
Cohesion: 0.09
Nodes (15): formattedcharsequence, glfw, java.util.function.IntSupplier, narratedelementtype, net.minecraft.client.gui.ComponentPath, net.minecraft.client.gui.components.AbstractButton, net.minecraft.client.gui.narration.NarrationElementOutput, net.minecraft.client.gui.navigation.FocusNavigationEvent (+7 more)

### Community 15 - "ConfigManager.java"
Cohesion: 0.10
Nodes (19): abstractappender, assertdoesnotthrow, assumptions, atomicinteger, atomicmovenotsupportedexception, basicfileattributes, copyonwritearraylist, filechannel (+11 more)

### Community 16 - "gif.py"
Cohesion: 0.27
Nodes (10): build_all(), build_gif(), caption_for(), encode(), frame_files(), main(), LEGACY, no longer used by the build (the GIFs now come from the studio…, The caption of the n-th (1 based) source frame: `captions` are the looks in… (+2 more)

### Community 17 - "EnchantmentColorsTab"
Cohesion: 0.21
Nodes (4): net.minecraft.client.gui.navigation.ScreenRectangle, EnchantmentColorsTab, EnchantmentRow, Override

### Community 18 - "Canvas"
Cohesion: 0.07
Nodes (16): Canvas, An RGBA canvas on a bytearray. Coordinates outside the canvas are clipped,…, A new canvas of the region; parts outside this canvas are transparent., Nearest-neighbour upscale by the whole factor k., Box-filter downscale by the whole factor n (every output pixel is the mean of…, Area-filter resize to ow x oh: every output pixel is the exact mean of the…, Draw src at (x, y): transparent pixels are skipped, half-transparent ones…, PNG bytes: RGB when fully opaque, RGBA otherwise (text: optional tEXt comment). (+8 more)

### Community 19 - "verify.py"
Cohesion: 0.08
Nodes (49): _chunk(), parse_gif(), png_bytes(), pixels: bytes-like, w*h*channels, row-major. channels 3 = RGB, 4 = RGBA. text:…, Parse back a GIF written by gif_bytes (or any simple GIF). Returns dict with…, contrast(), luminance(), WCAG relative luminance of an RGB(A) colour (0 black .. 1 white). (+41 more)

### Community 20 - "TeeBufferSource"
Cohesion: 0.18
Nodes (3): Override, TeeBufferSource, GuiItemFlushTest

### Community 21 - ".from"
Cohesion: 0.17
Nodes (3): GlintCategoryRuntime, GlintRuntimeSettings, GlintRuntimeSettingsTest

### Community 23 - "GlintContext"
Cohesion: 0.15
Nodes (3): 3.1 Phases, GlintContext, GlintContextTest

### Community 24 - "GlintSystem"
Cohesion: 0.09
Nodes (9): 12.1 Mechanism, 8.2 T6: glint system (`navrelis.glintsmith.render.glint`), GlintSystem, BufferSource, Override, Preparations, TextureReloadListener, GlintFailsafeTest (+1 more)

### Community 26 - "net.minecraft.network.chat.Component"
Cohesion: 0.12
Nodes (12): font, net.minecraft.client.gui.components.AbstractWidget, net.minecraft.client.gui.components.ContainerObjectSelectionList, net.minecraft.client.gui.components.events.GuiEventListener, net.minecraft.client.gui.narration.NarratableEntry, net.minecraft.network.chat.Component, net.minecraft.util.FormattedCharSequence, SettingRows (+4 more)

### Community 27 - "OutlineScaleTest"
Cohesion: 0.14
Nodes (5): net.minecraft.client.renderer.block.model.ItemTransform, org.joml.Matrix4f, org.joml.Matrix4fc, OutlineScale, OutlineScaleTest

### Community 28 - "gifenc.py"
Cohesion: 0.03
Nodes (61): colour_histogram(), encode_gif_frames(), gif_bytes(), gif_compose(), lzw_decode(), lzw_encode(), median_cut(), Dependency-free image encoders (PNG, animated GIF89a) and a small GIF decoder… (+53 more)

### Community 29 - "com.mojang.blaze3d.vertex.VertexConsumer"
Cohesion: 0.16
Nodes (6): com.mojang.blaze3d.vertex.VertexConsumer, Override, Recorder, Override, Recorder, Throwing

### Community 30 - "PhaseState"
Cohesion: 0.15
Nodes (8): 3.3 Targets, depth and resize, GlState, MaskDepth, CLEAR, COPY, NONE, OutlineCompositor, PhaseState

### Community 32 - "PatternBox"
Cohesion: 0.21
Nodes (4): net.minecraft.network.chat.Style, PatternText, Override, PatternBox

### Community 33 - ".acquire"
Cohesion: 0.16
Nodes (5): 4. Studio renders, Still scenes (4), Turntables (3), FakeType, GlintSlotPoolTest

### Community 35 - "org.spongepowered.asm.mixin.injection.Inject"
Cohesion: 0.14
Nodes (15): com.mojang.blaze3d.vertex.BufferBuilder, net.minecraft.client.DeltaTracker, net.minecraft.client.renderer.GameRenderer, net.minecraft.client.renderer.texture.TextureAtlas, org.spongepowered.asm.mixin.injection.callback.CallbackInfo, org.spongepowered.asm.mixin.injection.Inject, spriteloader, BufferSourceGlintMixin (+7 more)

### Community 36 - "FloatSlider"
Cohesion: 0.17
Nodes (10): function, java.util.function.DoubleConsumer, java.util.function.DoubleFunction, java.util.function.DoubleToIntFunction, mth, net.minecraft.client.gui.components.AbstractSliderButton, FloatSlider, Override (+2 more)

### Community 37 - "Glintsmith"
Cohesion: 0.08
Nodes (22): Commands and keys, Compatibility notes, Credits and licence, Glintsmith, How it compares, Performance, Requirements, Sodium and Iris (+14 more)

### Community 38 - "LangFilesTest.java"
Cohesion: 0.09
Nodes (34): abstractinsnnode, classreader, enumeration, fabriclauncherbase, fieldinsnnode, imixintransformer, inputstream, inputstreamreader (+26 more)

### Community 39 - "MaskTypes.java"
Cohesion: 0.27
Nodes (6): CompositeState, optional, org.spongepowered.asm.mixin.gen.Invoker, RenderStateShard.EmptyTextureStateShard, EmptyTextureStateShardAccessor, vertexformatelement

### Community 40 - "OutlineTarget"
Cohesion: 0.08
Nodes (19): nullable, PreviewNotes, OutlineContexts, GlintCategory, ARMOR, ENTITY, GUI, ITEM (+11 more)

### Community 42 - "StyleTable"
Cohesion: 0.14
Nodes (7): 3.4.1 Proportional thickness (2.0.0), 3.4 Per-item data encoding, nativeimage, net.minecraft.client.renderer.texture.DynamicTexture, StyleTable, StyleTexture, StyleTableTest

### Community 43 - "asserttrue"
Cohesion: 0.10
Nodes (25): assertarrayequals, assertequals, assertfalse, assertinstanceof, assertnotequals, assertnotnull, assertnotsame, assertnull (+17 more)

### Community 44 - "OutlineShaders"
Cohesion: 0.15
Nodes (9): com.mojang.blaze3d.vertex.VertexBuffer, com.mojang.blaze3d.vertex.VertexFormat, defaultvertexformat, gl20, net.minecraft.client.renderer.ShaderInstance, net.minecraft.server.packs.resources.ResourceProvider, resourcemanager, FullscreenQuad (+1 more)

### Community 45 - "GlintsmithScreen"
Cohesion: 0.11
Nodes (8): net.minecraft.client.gui.components.tabs.Tab, net.minecraft.client.gui.components.tabs.TabManager, net.minecraft.client.gui.components.tabs.TabNavigationBar, GlintsmithScreen, Override, SystemToastId, ConfigTab, Override

### Community 46 - "studio.py"
Cohesion: 0.10
Nodes (29): read_png_size(), The studio scenes (the contract), find_frame(), load(), Studio scenes: finding, loading and validating the frame contract. Python 3…, The frame dict for `ref`: a still name, a frame index (1 based) or a file stem…, A scene that is not there (kind 'missing') or not readable (kind 'error')., (K path, W path) of a frame dict. (+21 more)

### Community 47 - "ItemModelGeneratorMixin.java"
Cohesion: 0.21
Nodes (9): blockelementface, com.llamalad7.mixinextras.injector.ModifyReturnValue, direction, inventorymenu, local, net.minecraft.client.renderer.texture.SpriteContents, net.minecraft.client.renderer.texture.TextureAtlasSprite, ItemModelGeneratorMixin (+1 more)

### Community 48 - "RulesTab"
Cohesion: 0.20
Nodes (4): net.minecraft.client.gui.components.Checkbox, Override, RuleRow, RulesTab

### Community 49 - "SideElementBakeTest"
Cohesion: 0.19
Nodes (5): net.minecraft.client.renderer.block.model.BakedQuad, net.minecraft.client.renderer.block.model.BlockElement, net.minecraft.client.resources.model.ModelState, org.junit.jupiter.api.AfterAll, SideElementBakeTest

### Community 51 - ".clamp"
Cohesion: 0.10
Nodes (4): GapFix, Outline, Targets, TargetThickness

### Community 53 - "GlintPresets"
Cohesion: 0.27
Nodes (4): 10. Manual in-game test checklist, 5.6 Procedural presets (no third-party art), Enchantment glint, GlintPresets

### Community 54 - "composite.py"
Cohesion: 0.06
Nodes (55): alpha_bbox(), ascii_text(), backdrop(), _blend_plane(), blur_grid(), _blur_rows(), compose(), contact_plane() (+47 more)

### Community 55 - "PreviewPanel"
Cohesion: 0.14
Nodes (8): net.minecraft.client.gui.Font, net.minecraft.client.sounds.SoundManager, Override, PreviewPanel, PreviewSession, Source, HELD, SAMPLE

### Community 56 - "OutlineSystem.java"
Cohesion: 0.11
Nodes (23): clientlifecycleevents, clienttickevents, collection, commonlifecycleevents, fabricloader, graphicsstatus, itemstack, loggerfactory (+15 more)

### Community 57 - "GuiHelpersTest"
Cohesion: 0.15
Nodes (4): GuiHooks, Hsv, TextureNames, GuiHelpersTest

### Community 59 - "ItemModelGeneratorSpanMixin.java"
Cohesion: 0.25
Nodes (6): coerce, com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation, net.minecraft.client.renderer.block.model.ItemModelGenerator, ItemModelGeneratorSpanMixin, SpanAccess, unique

### Community 60 - "RuleEngine"
Cohesion: 0.15
Nodes (10): 3.7 Performance and allocation rules (T5), 9. Risks and open questions (each with a recommendation), net.minecraft.world.item.enchantment.ItemEnchantments, net.minecraft.world.item.Item, ResolvedStyle, EnchantKey, Override, RuleEngine (+2 more)

### Community 61 - "GlintLook"
Cohesion: 0.14
Nodes (10): 1. Decision summary, 2. Frame timeline in 1.21.1 and where Glintsmith acts, 6. Compatibility matrix and failure behaviour, 7. Performance notes, 8.1 T5: outline core (`navrelis.glintsmith.render.outline`), 8.3 T8: exact wiring, 8. Package and class spec, resources, and T8 hooks, Glintsmith render architecture (T2) (+2 more)

### Community 62 - "IrisCompat"
Cohesion: 0.24
Nodes (3): IrisApi, Api, IrisCompat

### Community 66 - "org.jetbrains.annotations.Nullable"
Cohesion: 0.09
Nodes (10): org.jetbrains.annotations.Nullable, ColorStyleScreen, Override, PreviewHost, Override, RuleEditScreen, Tri, INHERIT (+2 more)

### Community 67 - ".drawItem"
Cohesion: 0.09
Nodes (14): axis, 12.5 Panel and layout, FunctionalInterface, org.joml.Matrix3f, posestack, quaternionf, random, Fit (+6 more)

### Community 68 - "ColorStyle"
Cohesion: 0.14
Nodes (10): ColorMode, ENCHANTMENT, GRADIENT, PULSE, RAINBOW, STATIC, ColorStyle, Override (+2 more)

### Community 69 - "sys"
Cohesion: 0.21
Nodes (13): build(), check(), inline(), main(), pipeline_outputs(), Builds docs/curseforge/preview.html from description.md and checks the marker…, The image names the art pipeline builds (the "out" of every card and GIF of…, render() (+5 more)

### Community 70 - ".pick"
Cohesion: 0.11
Nodes (11): 5.1 How vanilla 1.21.1 draws glint (verified), 5.2 Strategy, 5.3 Categories, context and resolution, 5.4.1 Atlas-independent scale (2.0.0), 5.4 Slot render types, 5.5 Texture management (`GlintTextures`), 5.7 Edge cases T6 must handle, 5. Glint system in detail (T6) (+3 more)

### Community 71 - "MaskTypesTest"
Cohesion: 0.14
Nodes (4): LayeringStateShard, WriteMaskStateShard, CullStateShard, MaskTypesTest

### Community 72 - "LangFilesTest"
Cohesion: 0.24
Nodes (5): 12.3 The snapshot and the pending edits chain, 12.6 Limits (known and by design), 12.7 Tests and what is still open in game, 12. Live preview (2.0.0), LangFilesTest

### Community 73 - "StudioScreen.java"
Cohesion: 0.08
Nodes (24): builtinregistries, compoundtag, datacomponents, entityrenderdispatcher, entitytype, fogrenderer, hashmap, holder (+16 more)

### Community 74 - ".passes"
Cohesion: 0.33
Nodes (3): com.mojang.blaze3d.pipeline.RenderTarget, com.mojang.blaze3d.pipeline.TextureTarget, OutlineTargets

### Community 75 - "Glintsmith"
Cohesion: 0.20
Nodes (9): Commands and keys, Compatibility notes, Credits and licence, Glintsmith, How it compares, Performance, Requirements, Sodium and Iris (+1 more)

### Community 78 - "Glintsmith Studio (dev-only render mode)"
Cohesion: 0.15
Nodes (11): Black/white pairs, Camera and light, Example scenes, Files, Glint scale, Glintsmith Studio (dev-only render mode), Limits, Render target and supersampling (+3 more)

### Community 79 - "synth_studio.py"
Cohesion: 0.10
Nodes (25): draw_sword(), Frame, main(), make_contract(), turntable(), make_scene(), Fill a convex polygon; stripe = (centre at y=0, slope, half width, colour)…, Draw one extruded sword centred on (cx, cy) with unit length u (supersampled… (+17 more)

### Community 80 - "common.py"
Cohesion: 0.05
Nodes (43): argparse, main(), Captures, cut(), cut_el(), load_manifest(), out_size(), picture_size() (+35 more)

### Community 82 - "Scene"
Cohesion: 0.21
Nodes (9): com.google.gson.JsonElement, com.google.gson.JsonObject, Camera, Kind, ARMOR_STAND, ITEM, Scene, Shot (+1 more)

### Community 83 - "OccluderBufferSource.java"
Cohesion: 0.38
Nodes (3): arrays, com.mojang.blaze3d.vertex.ByteBufferBuilder, object2objectsortedmaps

### Community 84 - "CompositeStateAccessor"
Cohesion: 0.13
Nodes (12): 3.2 Teeing (capture), EmptyTextureStateShard, java.util.SequencedMap, org.spongepowered.asm.mixin.gen.Accessor, rendertype, RenderType.CompositeState, BufferSource, CompositeStateAccessor (+4 more)

### Community 85 - "SpanMixin"
Cohesion: 0.43
Nodes (3): org.spongepowered.asm.mixin.Shadow, Override, SpanMixin

### Community 86 - "net.minecraft.resources.ResourceLocation"
Cohesion: 0.28
Nodes (5): java.util.jar.JarFile, net.minecraft.resources.ResourceLocation, Override, ShaderReloadListener, Sprite

### Community 89 - "StudioMod"
Cohesion: 0.16
Nodes (4): StudioBootstrap, Override, ReloadCounter, StudioMod

### Community 90 - "rendersystem"
Cohesion: 0.20
Nodes (9): abstracttexture, gl11, gl13, gl14, gl30, glstatemanager, options, rendersystem (+1 more)

### Community 91 - "CurseForge upload checklist: Glintsmith 2.0.0"
Cohesion: 0.25
Nodes (7): Before 2.0.0 goes public, CurseForge upload checklist: Glintsmith 2.0.0, Earlier release: Glintsmith 1.0.0, Relations, The file, The project page, What the owner has to decide or do by hand

### Community 93 - "org.spongepowered.asm.mixin.Mixin"
Cohesion: 0.08
Nodes (27): at, 4. Hook table, Glint (T6), net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer, net.minecraft.client.renderer.RenderBuffers, net.minecraft.client.renderer.RenderStateShard, org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable, org.spongepowered.asm.mixin.injection.ModifyVariable (+19 more)

### Community 94 - "GlintKind"
Cohesion: 0.10
Nodes (14): CompositeStateBuilder, GlintKind, ARMOR_ENTITY_GLINT, ENTITY_GLINT, ENTITY_GLINT_DIRECT, GLINT, GLINT_TRANSLUCENT, ShaderStateShard (+6 more)

### Community 96 - "net.minecraft.client.renderer.RenderType"
Cohesion: 0.18
Nodes (9): net.minecraft.client.renderer.RenderType, Mode, FOOTER, HIDDEN, SIDE, Entry, CullStateShard, LayeringStateShard (+1 more)

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
Cohesion: 0.17
Nodes (3): Layout, PreviewLayout, PreviewLayoutTest

### Community 107 - ".build"
Cohesion: 0.14
Nodes (9): clientcommandmanager, clientcommandregistrationcallback, com.mojang.brigadier.CommandDispatcher, net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource, net.minecraft.client.KeyMapping, net.minecraft.commands.CommandBuildContext, GlintsmithCommands, GlintsmithKeys (+1 more)

### Community 109 - "TeeCaptureTest"
Cohesion: 0.27
Nodes (4): java.nio.ByteBuffer, TeePool, RecordingSource, TeeCaptureTest

### Community 110 - "StudioScreen"
Cohesion: 0.15
Nodes (5): Look, Override, Job, Prop, StudioScreen

### Community 111 - "ColorPickerScreen"
Cohesion: 0.21
Nodes (4): java.util.function.IntConsumer, net.minecraft.client.gui.screens.Screen, ColorPickerScreen, Override

### Community 112 - "net.minecraft.client.Minecraft"
Cohesion: 0.09
Nodes (25): accessibilityonboardingscreen, difficulty, 12.4 What is shown, enchantments, flatlevelgeneratorpreset, flatlevelgeneratorpresets, flatlevelsource, gamerules (+17 more)

### Community 113 - "Glintsmith capture shot list"
Cohesion: 0.12
Nodes (15): 0. Rig, 1. World and rooms, 2. Glintsmith config, 3. In-world captures (14), 5. Checklist, Backdrop and close-ups, Enchantment shorthand, Frames and stand templates (+7 more)

### Community 114 - "OutlinePhase"
Cohesion: 0.13
Nodes (10): 3.5 Shaders, 3.6 Composite procedure (`OutlineCompositor.composite(PhaseState, Params)`), 3.8 Failsafe (T5), 3. Outline pipeline in detail (T5), OutlinePhase, GUI, HAND, NONE (+2 more)

### Community 116 - "SideElementBakeTest.java"
Cohesion: 0.15
Nodes (7): facebakery, framesize, itemmodelgenerator, org.junit.jupiter.api.BeforeAll, resourcelocation, resourcemetadata, GameBootstrap

### Community 117 - "MaskTypes"
Cohesion: 0.18
Nodes (7): OutputStateShard, DepthTestStateShard, ShaderStateShard, TexturingStateShard, TransparencyStateShard, WriteMaskStateShard, MaskTypes

### Community 118 - "ModMenuIntegration.java"
Cohesion: 0.47
Nodes (4): com.terraformersmc.modmenu.api.ConfigScreenFactory, com.terraformersmc.modmenu.api.ModMenuApi, Override, ModMenuIntegration

### Community 119 - "PipelineBridge"
Cohesion: 0.38
Nodes (3): java.lang.reflect.Field, java.lang.reflect.Method, PipelineBridge

### Community 122 - "1.0.0 (Fabric 1.21.1)"
Cohesion: 0.18
Nodes (10): 1.0.0 (Fabric 1.21.1), 2.0.0 (Fabric 1.21.1) — 2026-10-18, 2.0.1 (Fabric 1.21.1) — Unreleased, Added, Changed, Changelog, Fixed, Fixed (+2 more)

### Community 123 - "FrameWriter"
Cohesion: 0.24
Nodes (4): com.google.gson.JsonArray, com.mojang.blaze3d.platform.NativeImage, Frame contract (output), FrameWriter

### Community 126 - "matte.py"
Cohesion: 0.20
Nodes (13): array, box_sums(), _int(), matte_files(), matte_pair(), premultiplied_planes(), Studio matte: turns a black / white render pair into a straight-alpha RGBA…, Straight-alpha RGBA Canvas (width // S x height // S) from the K and W renders… (+5 more)

### Community 128 - "Final report: Glintsmith 2.0.0"
Cohesion: 0.20
Nodes (9): Changed files (2.0.0, sessions 13 to 15), Checks and tests run (session 15), Final report: Glintsmith 2.0.0, Key decisions (full list in `decisions.md`), Known limitations and recommendations, Manual test checklist (owner; not yet done by a human), Model usage, Session 16 follow-up (owner, after the 2.0.0 upload) (+1 more)

### Community 129 - "net.minecraft.client.gui.GuiGraphics"
Cohesion: 0.22
Nodes (3): net.minecraft.client.gui.GuiGraphics, Override, Override

### Community 131 - "Glintsmith art"
Cohesion: 0.33
Nodes (5): Glintsmith art, Licence and sources, Output files, Theme, Verify

### Community 132 - "Handoff: Glintsmith lineup stray outline dot (root cause + outline fix)"
Cohesion: 0.15
Nodes (12): 10. How to resume, 1. Goal, 2. Requirements and constraints, 3. Done, 4. In progress when stopped, 5. Open work (ordered), 6. Known problems and blockers, 7. Decisions made (+4 more)

### Community 133 - "Type"
Cohesion: 0.67
Nodes (3): Type, STILL, TURNTABLE

### Community 134 - "DurabilityAlert"
Cohesion: 0.23
Nodes (3): DurabilityAlert, Parsed, DurabilityAndEnchantmentColorsTest

### Community 136 - "org.joml.Vector3f"
Cohesion: 0.29
Nodes (4): 11. Model gap fix: AUTO values, Edge guard (2.0.1), net.minecraft.core.Direction, org.joml.Vector3f

### Community 142 - "Kind"
Cohesion: 0.33
Nodes (6): Kind, ALL, EXACT, GLOB, NAMESPACE, TAG

### Community 144 - "State"
Cohesion: 0.40
Nodes (5): State, LOADING, MENU, RUNNING, SETTLING

## Knowledge Gaps
- **149 isolated node(s):** `AUTO`, `CUSTOM`, `ENCHANTED`, `ALL`, `NONE` (+144 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 593 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **43 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `GlintsmithConfig` connect `GlintsmithConfig` to `.build`, `list`, `.t`, `DurabilityAlert`, `OutlineSystem`, `.sanitize`, `Glintsmith`, `PreviewPanel.java`, `EnchantmentColorsTab`, `.from`, `GlintSystem`, `net.minecraft.network.chat.Component`, `RuleMatcherTest`, `LangFilesTest.java`, `OutlineTarget`, `asserttrue`, `GlintsmithScreen`, `RulesTab`, `.clamp`, `PreviewPanel`, `OutlineSystem.java`, `RuleEngine`, `OutlineRuntimeSettings`, `org.jetbrains.annotations.Nullable`, `.drawItem`, `ColorStyle`, `StudioScreen.java`, `RuleEngineScopeTest`, `GlintTextures`, `PreviewNotesTest`, `StudioScreen`, `ColorPickerScreen`, `PipelineBridge`, `GlintCategorySettings`, `PendingEditsTest`, `.update`?**
  _High betweenness centrality (0.127) - this node is a cross-community bridge._
- **Why does `The manifest` connect `lib.py` to `OutlineDepthTest`, `net.minecraft.world.item.ItemStack`, `common.py`, `Canvas`, `GlintPresets`?**
  _High betweenness centrality (0.081) - this node is a cross-community bridge._
- **Why does `OutlineSystem` connect `OutlineSystem` to `OutlineRuntimeSettings`, `org.spongepowered.asm.mixin.injection.Inject`, `OutlineTarget`, `GlintsmithConfig`, `net.minecraft.world.item.ItemStack`, `.sanitize`, `StudioScreen.java`, `TeeCaptureTest`, `TeeBufferSource`, `net.minecraft.resources.ResourceLocation`, `PipelineBridge`, `OutlineSystem.java`, `OutlineScaleTest`, `org.spongepowered.asm.mixin.Mixin`, `PhaseState`?**
  _High betweenness centrality (0.059) - this node is a cross-community bridge._
- **What connects `AUTO`, `CUSTOM`, `ENCHANTED` to the rest of the system?**
  _149 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `logo.py` be split into smaller, more focused modules?**
  _Cohesion score 0.10160427807486631 - nodes in this community are weakly interconnected._
- **Should `GlintNoise` be split into smaller, more focused modules?**
  _Cohesion score 0.10837438423645321 - nodes in this community are weakly interconnected._
- **Should `list` be split into smaller, more focused modules?**
  _Cohesion score 0.1282051282051282 - nodes in this community are weakly interconnected._
# Graph Report - Glintsmith  (2026-10-18)

## Corpus Check
- 251 files · ~551,418 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 13 file(s) not represented in the graph (top: (none) 4, .fsh 3, .properties 2)

## Summary
- 3275 nodes · 9910 edges · 133 communities (100 shown, 33 thin omitted)
- Extraction: 91% EXTRACTED · 9% INFERRED · 0% AMBIGUOUS · INFERRED: 890 edges (avg confidence: 0.84)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `b846ff6d`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- GlintTexturesTest
- logo.py
- GlintPresets
- list
- .t
- Colors
- .forPhase
- OutlineMixinApplicationTest
- OutlineSystem
- org.jetbrains.annotations.Nullable
- net.minecraft.client.renderer.MultiBufferSource
- .sanitize
- Glintsmith
- lib.py
- ColorStyleButton
- ConfigManager.java
- sys
- ConfigTab
- Canvas
- verify.py
- TeeCaptureTest
- .from
- GlintSlot
- GlintContext
- net.minecraft.client.renderer.RenderType
- org.junit.jupiter.api.Test
- OptionList
- StyleTable
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
- GlintsmithConfig
- OutlineDepthTest
- PreviewRenderer.java
- asserttrue
- OutlineShaders
- GlintsmithScreen
- studio.py
- ItemModelGeneratorMixin.java
- RulesTab
- SideElementBakeTest
- TeeVertexConsumer
- ColorStyle
- GapFix
- .tripped
- composite.py
- net.minecraft.client.gui.GuiGraphics
- OutlineSystem.java
- Pattern
- GapFixTest
- ItemModelGeneratorSpanMixin.java
- RuleEngine
- Glintsmith render architecture (T2)
- IrisCompat
- net.minecraft.world.item.ItemStack
- .trip
- MaskBufferSource
- PreviewSession
- .drawItem
- GlintLook
- preview.py
- GlintParams
- MaskTypes
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
- OccluderBufferSource
- Handoff: Glintsmith 2.0.0 post-release (GIFs under 2 MB, new logo) — session 15
- SpanMixin
- GlintAtlasScaleTest
- ConfigSubScreen
- OutlineFailsafe
- StudioMod
- rendersystem
- CurseForge upload checklist: Glintsmith 2.0.0
- net.minecraft.resources.ResourceLocation
- org.spongepowered.asm.mixin.Mixin
- GlintKind
- .validateTexture
- Tri
- OutlineDepth
- CurseForge page material for Glintsmith
- gradlew
- Condition
- Plan (Glintsmith 1.0.0)
- decisions.md
- log.md
- PreviewNotesTest
- PreviewLayoutTest
- .build
- .u
- GlintsmithKeys.java
- StudioScreen
- ColorPickerScreen
- StudioBootstrap.java
- net.minecraft.client.Minecraft
- OutlinePhase
- OutlineShaderResourcesTest
- org.junit.jupiter.api.BeforeAll
- Handoff: Glintsmith 2.0.0 release (session 14)
- ModMenuIntegration.java
- .drawSubjects
- GlintCategorySettings
- PendingEditsTest
- 1.0.0 (Fabric 1.21.1)
- FrameWriter.java
- ShaderReloadListener
- ReloadCounter
- .update
- Final report: Glintsmith 2.0.0
- .build
- Glintsmith art
- Type
- SideElementBakeTest.java
- renderstateshard

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

## Communities (133 total, 33 thin omitted)

### Community 1 - "logo.py"
Cohesion: 0.10
Nodes (33): build(), _compose_task(), frame(), _frame_task(), glow_grids(), glow_layer(), _init_compose(), _init_frames() (+25 more)

### Community 2 - "GlintPresets"
Cohesion: 0.05
Nodes (22): 0. Rig, 1. World and rooms, 2. Glintsmith config, 3. In-world captures (14), 5. Checklist, Backdrop and close-ups, Enchantment shorthand, Frames and stand templates (+14 more)

### Community 3 - "list"
Cohesion: 0.13
Nodes (20): arraylist, assertarrayequals, assertthrows, collections, directorystream, files, filetime, hashset (+12 more)

### Community 4 - ".t"
Cohesion: 0.10
Nodes (10): net.minecraft.client.gui.components.CycleButton, net.minecraft.client.gui.components.Tooltip, OnPress, RuleEditScreen, SettingRows, Override, ModelFixTab, Override (+2 more)

### Community 5 - "Colors"
Cohesion: 0.05
Nodes (14): ColorEvaluator, Compiled, Override, ColorMode, ENCHANTMENT, GRADIENT, PULSE, RAINBOW (+6 more)

### Community 7 - "OutlineMixinApplicationTest"
Cohesion: 0.09
Nodes (5): org.objectweb.asm.tree.AnnotationNode, org.objectweb.asm.tree.ClassNode, org.objectweb.asm.tree.MethodNode, GlintMixinApplicationTest, OutlineMixinApplicationTest

### Community 8 - "OutlineSystem"
Cohesion: 0.12
Nodes (7): 12.2 The PREVIEW outline phase, 3.1 Phases, How the real glint and outline reach the subjects, net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext, net.minecraft.client.resources.model.ModelResourceLocation, BufferSource, OutlineSystem

### Community 9 - "org.jetbrains.annotations.Nullable"
Cohesion: 0.10
Nodes (24): button, chatformatting, commoncomponents, component, confirmscreen, consumer, createworldscreen, cyclebutton (+16 more)

### Community 10 - "net.minecraft.client.renderer.MultiBufferSource"
Cohesion: 0.12
Nodes (22): com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod, com.llamalad7.mixinextras.injector.wrapoperation.Operation, equipmentslot, net.minecraft.client.model.HumanoidModel, net.minecraft.client.player.LocalPlayer, net.minecraft.client.renderer.entity.layers.ElytraLayer, net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer, net.minecraft.client.renderer.ItemInHandRenderer (+14 more)

### Community 11 - ".sanitize"
Cohesion: 0.08
Nodes (3): ConfigManager, Holder, ConfigTest

### Community 12 - "Glintsmith"
Cohesion: 0.15
Nodes (9): Environment facts (verified 2026-10-17), Later owner additions (chat, 2026-10-17), Owner answers (2026-10-17), Owner task (2026-10-17), Requirements (Glintsmith, session 2026-10-17), Session 12 owner additions (2026-10-18), Glintsmith, Override (+1 more)

### Community 13 - "lib.py"
Cohesion: 0.05
Nodes (69): anchor_x(), build_all(), build_card(), chip_list(), Ctx, el_bullets(), el_capture(), el_chips() (+61 more)

### Community 14 - "ColorStyleButton"
Cohesion: 0.29
Nodes (3): net.minecraft.client.gui.components.AbstractButton, ColorStyleButton, Override

### Community 15 - "ConfigManager.java"
Cohesion: 0.10
Nodes (18): abstractappender, assumptions, atomicinteger, atomicmovenotsupportedexception, basicfileattributes, copyonwritearraylist, filechannel, jsonparseexception (+10 more)

### Community 16 - "sys"
Cohesion: 0.23
Nodes (12): bar(), build_all(), build_gif(), caption_for(), encode(), frame_files(), main(), LEGACY, no longer used by the build (the GIFs now come from the studio… (+4 more)

### Community 17 - "ConfigTab"
Cohesion: 0.09
Nodes (9): net.minecraft.client.gui.components.tabs.Tab, net.minecraft.client.gui.navigation.ScreenRectangle, ConfigTab, Override, EnchantmentColorsTab, EnchantmentRow, Override, GlintTab (+1 more)

### Community 18 - "Canvas"
Cohesion: 0.09
Nodes (12): Canvas, An RGBA canvas on a bytearray. Coordinates outside the canvas are clipped,…, A new canvas of the region; parts outside this canvas are transparent., Nearest-neighbour upscale by the whole factor k., Box-filter downscale by the whole factor n (every output pixel is the mean of…, Area-filter resize to ow x oh: every output pixel is the exact mean of the…, Draw src at (x, y): transparent pixels are skipped, half-transparent ones…, PNG bytes: RGB when fully opaque, RGBA otherwise (text: optional tEXt comment). (+4 more)

### Community 19 - "verify.py"
Cohesion: 0.08
Nodes (49): _chunk(), parse_gif(), png_bytes(), pixels: bytes-like, w*h*channels, row-major. channels 3 = RGB, 4 = RGBA. text:…, Parse back a GIF written by gif_bytes (or any simple GIF). Returns dict with…, contrast(), luminance(), WCAG relative luminance of an RGB(A) colour (0 black .. 1 white). (+41 more)

### Community 20 - "TeeCaptureTest"
Cohesion: 0.14
Nodes (6): java.nio.ByteBuffer, Override, TeeBufferSource, TeePool, GuiItemFlushTest, TeeCaptureTest

### Community 21 - ".from"
Cohesion: 0.18
Nodes (3): 3. Done (this session), Changed files (2.0.0, sessions 13 to 15), GlintRuntimeSettingsTest

### Community 22 - "GlintSlot"
Cohesion: 0.14
Nodes (6): Studio clock, GlintShards, GlintSlot, Factory, GlintSlotPool, SuppressWarnings

### Community 24 - "net.minecraft.client.renderer.RenderType"
Cohesion: 0.11
Nodes (19): com.mojang.blaze3d.vertex.BufferBuilder, 12.1 Mechanism, 5.2 Strategy, 8.2 T6: glint system (`navrelis.glintsmith.render.glint`), Glint (T6), net.minecraft.client.renderer.RenderType, org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable, org.spongepowered.asm.mixin.injection.ModifyVariable (+11 more)

### Community 25 - "org.junit.jupiter.api.Test"
Cohesion: 0.09
Nodes (6): org.junit.jupiter.api.Test, GlintsmithMixinPluginTest, MixinApplicationTest, SmokeTest, ColorEvaluatorTest, DurabilityAndEnchantmentColorsTest

### Community 26 - "OptionList"
Cohesion: 0.14
Nodes (11): font, net.minecraft.client.gui.components.AbstractWidget, net.minecraft.client.gui.components.ContainerObjectSelectionList, net.minecraft.client.gui.components.events.GuiEventListener, net.minecraft.client.gui.narration.NarratableEntry, net.minecraft.util.FormattedCharSequence, Override, LabeledRow (+3 more)

### Community 27 - "StyleTable"
Cohesion: 0.05
Nodes (15): 3.4.1 Proportional thickness (2.0.0), 3.4 Per-item data encoding, nativeimage, net.minecraft.client.renderer.block.model.ItemTransform, net.minecraft.client.renderer.texture.DynamicTexture, org.joml.Matrix4f, org.joml.Matrix4fc, OutlineRuntimeSettings (+7 more)

### Community 28 - "gifenc.py"
Cohesion: 0.03
Nodes (61): colour_histogram(), encode_gif_frames(), gif_bytes(), gif_compose(), lzw_decode(), lzw_encode(), median_cut(), Dependency-free image encoders (PNG, animated GIF89a) and a small GIF decoder… (+53 more)

### Community 29 - "com.mojang.blaze3d.vertex.VertexConsumer"
Cohesion: 0.14
Nodes (7): com.mojang.blaze3d.vertex.VertexConsumer, Override, Recorder, Override, Recorder, RecordingSource, Throwing

### Community 30 - "PhaseState"
Cohesion: 0.16
Nodes (7): GlState, MaskDepth, CLEAR, COPY, NONE, OutlineCompositor, PhaseState

### Community 32 - "PatternBox"
Cohesion: 0.38
Nodes (3): net.minecraft.network.chat.Style, Override, PatternBox

### Community 33 - ".acquire"
Cohesion: 0.17
Nodes (5): 4. Studio renders, Still scenes (4), Turntables (3), FakeType, GlintSlotPoolTest

### Community 35 - "org.spongepowered.asm.mixin.injection.Inject"
Cohesion: 0.18
Nodes (12): net.minecraft.client.DeltaTracker, net.minecraft.client.renderer.GameRenderer, net.minecraft.client.renderer.texture.TextureAtlas, org.spongepowered.asm.mixin.injection.callback.CallbackInfo, org.spongepowered.asm.mixin.injection.Inject, spriteloader, GameRendererGlintMixin, Preparations (+4 more)

### Community 36 - "FloatSlider"
Cohesion: 0.17
Nodes (10): function, java.util.function.DoubleConsumer, java.util.function.DoubleFunction, java.util.function.DoubleToIntFunction, mth, net.minecraft.client.gui.components.AbstractSliderButton, FloatSlider, Override (+2 more)

### Community 37 - "Glintsmith"
Cohesion: 0.08
Nodes (22): Commands and keys, Compatibility notes, Credits and licence, Glintsmith, How it compares, Performance, Requirements, Sodium and Iris (+14 more)

### Community 38 - "LangFilesTest.java"
Cohesion: 0.14
Nodes (24): abstractinsnnode, classreader, enumeration, fabriclauncherbase, fieldinsnnode, imixintransformer, inputstream, inputstreamreader (+16 more)

### Community 39 - "MaskTypes.java"
Cohesion: 0.17
Nodes (11): CompositeState, Outline (T5), net.minecraft.client.renderer.RenderStateShard, optional, org.spongepowered.asm.mixin.gen.Invoker, RenderStateShard.EmptyTextureStateShard, rendertype, RenderType.CompositeRenderType (+3 more)

### Community 40 - "GlintsmithConfig"
Cohesion: 0.09
Nodes (19): com.google.gson.Gson, nullable, GlintsmithConfig, GlintCategory, ARMOR, ENTITY, GUI, ITEM (+11 more)

### Community 42 - "PreviewRenderer.java"
Cohesion: 0.18
Nodes (9): itemrenderer, lighting, loggerfactory, net.minecraft.world.item.Item, org.slf4j.Logger, org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin, org.spongepowered.asm.mixin.extensibility.IMixinInfo, GlintsmithMixinPlugin (+1 more)

### Community 43 - "asserttrue"
Cohesion: 0.10
Nodes (21): assertdoesnotthrow, assertequals, assertfalse, assertinstanceof, assertnotequals, assertnotnull, assertnotsame, assertnull (+13 more)

### Community 44 - "OutlineShaders"
Cohesion: 0.15
Nodes (9): com.mojang.blaze3d.vertex.VertexBuffer, com.mojang.blaze3d.vertex.VertexFormat, defaultvertexformat, gl20, net.minecraft.client.renderer.ShaderInstance, net.minecraft.server.packs.resources.ResourceProvider, resourcemanager, FullscreenQuad (+1 more)

### Community 45 - "GlintsmithScreen"
Cohesion: 0.16
Nodes (3): GlintsmithScreen, Override, SystemToastId

### Community 46 - "studio.py"
Cohesion: 0.10
Nodes (29): read_png_size(), The studio scenes (the contract), find_frame(), load(), Studio scenes: finding, loading and validating the frame contract. Python 3…, The frame dict for `ref`: a still name, a frame index (1 based) or a file stem…, A scene that is not there (kind 'missing') or not readable (kind 'error')., (K path, W path) of a frame dict. (+21 more)

### Community 47 - "ItemModelGeneratorMixin.java"
Cohesion: 0.23
Nodes (8): blockelementface, com.llamalad7.mixinextras.injector.ModifyReturnValue, direction, inventorymenu, net.minecraft.client.renderer.block.model.ItemModelGenerator, net.minecraft.client.renderer.texture.TextureAtlasSprite, ItemModelGeneratorMixin, TextureAtlasSpriteMixin

### Community 48 - "RulesTab"
Cohesion: 0.20
Nodes (4): net.minecraft.client.gui.components.Checkbox, Override, RuleRow, RulesTab

### Community 49 - "SideElementBakeTest"
Cohesion: 0.29
Nodes (4): net.minecraft.client.renderer.block.model.BakedQuad, net.minecraft.client.renderer.block.model.BlockElement, net.minecraft.core.Direction, SideElementBakeTest

### Community 51 - "ColorStyle"
Cohesion: 0.07
Nodes (17): jsonarray, jsonprimitive, objects, GapFixMode, AUTO, CUSTOM, GapFix, Outline (+9 more)

### Community 52 - "GapFix"
Cohesion: 0.16
Nodes (3): 11. Model gap fix: AUTO values, GapFix, Settings

### Community 54 - "composite.py"
Cohesion: 0.06
Nodes (56): alpha_bbox(), ascii_text(), backdrop(), bar(), _blend_plane(), blur_grid(), _blur_rows(), compose() (+48 more)

### Community 55 - "net.minecraft.client.gui.GuiGraphics"
Cohesion: 0.11
Nodes (14): formattedcharsequence, glfw, narratedelementtype, net.minecraft.client.gui.ComponentPath, net.minecraft.client.gui.Font, net.minecraft.client.gui.GuiGraphics, net.minecraft.client.gui.navigation.FocusNavigationEvent, net.minecraft.client.sounds.SoundManager (+6 more)

### Community 56 - "OutlineSystem.java"
Cohesion: 0.11
Nodes (22): clientlifecycleevents, clienttickevents, collection, commonlifecycleevents, fabricloader, graphicsstatus, itemstack, method (+14 more)

### Community 57 - "Pattern"
Cohesion: 0.05
Nodes (14): GuiHooks, Hsv, PatternText, TextureNames, Override, Kind, ALL, EXACT (+6 more)

### Community 59 - "ItemModelGeneratorSpanMixin.java"
Cohesion: 0.24
Nodes (6): coerce, com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation, local, ItemModelGeneratorSpanMixin, SpanAccess, unique

### Community 60 - "RuleEngine"
Cohesion: 0.17
Nodes (7): 9. Risks and open questions (each with a recommendation), net.minecraft.world.item.enchantment.ItemEnchantments, EnchantKey, Override, RuleEngine, StyleKey, RuleEngineKeyTest

### Community 61 - "Glintsmith render architecture (T2)"
Cohesion: 0.13
Nodes (14): 1. Decision summary, 2. Frame timeline in 1.21.1 and where Glintsmith acts, 3.5 Shaders, 3.6 Composite procedure (`OutlineCompositor.composite(PhaseState, Params)`), 3.7 Performance and allocation rules (T5), 3.8 Failsafe (T5), 3. Outline pipeline in detail (T5), 4. Hook table (+6 more)

### Community 62 - "IrisCompat"
Cohesion: 0.20
Nodes (3): IrisApi, Api, IrisCompat

### Community 63 - "net.minecraft.world.item.ItemStack"
Cohesion: 0.15
Nodes (13): com.mojang.blaze3d.vertex.PoseStack, itemtags, net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer, net.minecraft.client.renderer.entity.ItemRenderer, net.minecraft.client.resources.model.BakedModel, net.minecraft.world.item.ItemDisplayContext, net.minecraft.world.item.ItemStack, BlockEntityWithoutLevelRendererGlintMixin (+5 more)

### Community 65 - "MaskBufferSource"
Cohesion: 0.15
Nodes (3): BufferSource, 5.5 Texture management (`GlintTextures`), MaskBufferSource

### Community 66 - "PreviewSession"
Cohesion: 0.11
Nodes (8): ColorStyleScreen, Override, PreviewHost, PreviewSession, Source, HELD, SAMPLE, Override

### Community 67 - ".drawItem"
Cohesion: 0.11
Nodes (9): axis, FunctionalInterface, quaternionf, Fit, PreviewPose, BufferSource, Margin, PreviewRenderer (+1 more)

### Community 68 - "GlintLook"
Cohesion: 0.19
Nodes (4): GlintCategories, Frame, GlintCategoryRuntime, GlintLook

### Community 69 - "preview.py"
Cohesion: 0.23
Nodes (12): build(), check(), inline(), main(), pipeline_outputs(), Builds docs/curseforge/preview.html from description.md and checks the marker…, The image names the art pipeline builds (the "out" of every card and GIF of…, render() (+4 more)

### Community 70 - "GlintParams"
Cohesion: 0.17
Nodes (8): 5.1 How vanilla 1.21.1 draws glint (verified), 5.3 Categories, context and resolution, 5.4.1 Atlas-independent scale (2.0.0), 5.4 Slot render types, 5.7 Edge cases T6 must handle, 5. Glint system in detail (T6), GlintParams, GlintParamsTest

### Community 71 - "MaskTypes"
Cohesion: 0.05
Nodes (31): 3.2 Teeing (capture), EmptyTextureStateShard, java.util.SequencedMap, org.spongepowered.asm.mixin.gen.Accessor, OutputStateShard, RenderType.CompositeState, Mode, FOOTER (+23 more)

### Community 72 - "LangFilesTest"
Cohesion: 0.14
Nodes (8): 12.3 The snapshot and the pending edits chain, 12.4 What is shown, 12.5 Panel and layout, 12.6 Limits (known and by design), 12.7 Tests and what is still open in game, 12. Live preview (2.0.0), PreviewNotes, LangFilesTest

### Community 73 - "StudioScreen.java"
Cohesion: 0.06
Nodes (34): bootstrap, builtinregistries, compoundtag, datacomponents, enchantments, entityrenderdispatcher, entitytype, fogrenderer (+26 more)

### Community 74 - ".passes"
Cohesion: 0.33
Nodes (3): com.mojang.blaze3d.pipeline.RenderTarget, com.mojang.blaze3d.pipeline.TextureTarget, OutlineTargets

### Community 75 - "Glintsmith"
Cohesion: 0.20
Nodes (9): Commands and keys, Compatibility notes, Credits and licence, Glintsmith, How it compares, Performance, Requirements, Sodium and Iris (+1 more)

### Community 78 - "Glintsmith Studio (dev-only render mode)"
Cohesion: 0.14
Nodes (12): Black/white pairs, Camera and light, Example scenes, Files, Frame contract (output), Glint scale, Glintsmith Studio (dev-only render mode), Limits (+4 more)

### Community 79 - "synth_studio.py"
Cohesion: 0.07
Nodes (38): array, box_sums(), _int(), matte_files(), matte_pair(), premultiplied_planes(), Studio matte: turns a black / white render pair into a straight-alpha RGBA…, Straight-alpha RGBA Canvas (width // S x height // S) from the K and W renders… (+30 more)

### Community 80 - "common.py"
Cohesion: 0.05
Nodes (43): argparse, main(), Captures, cut(), cut_el(), load_manifest(), out_size(), picture_size() (+35 more)

### Community 82 - "Scene"
Cohesion: 0.21
Nodes (10): com.google.gson.JsonElement, com.google.gson.JsonObject, org.joml.Vector3f, Camera, Kind, ARMOR_STAND, ITEM, Scene (+2 more)

### Community 83 - "OccluderBufferSource"
Cohesion: 0.18
Nodes (5): arrays, com.mojang.blaze3d.vertex.ByteBufferBuilder, object2objectsortedmaps, Override, OccluderBufferSource

### Community 84 - "Handoff: Glintsmith 2.0.0 post-release (GIFs under 2 MB, new logo) — session 15"
Cohesion: 0.15
Nodes (12): 10. How to resume, 1. Goal, 2. Requirements and constraints, 3. Done (session 15), 4. In progress when stopped, 5. Open work (ordered; G2MB and L1 can run in parallel — files disjoint), 6. Known problems and blockers, 7. Decisions made (session 15) (+4 more)

### Community 85 - "SpanMixin"
Cohesion: 0.43
Nodes (3): org.spongepowered.asm.mixin.Shadow, Override, SpanMixin

### Community 86 - "GlintAtlasScaleTest"
Cohesion: 0.64
Nodes (3): java.util.jar.JarFile, GlintAtlasScaleTest, Sprite

### Community 89 - "StudioMod"
Cohesion: 0.16
Nodes (7): State, LOADING, MENU, RUNNING, SETTLING, StudioBootstrap, StudioMod

### Community 90 - "rendersystem"
Cohesion: 0.20
Nodes (9): abstracttexture, gl11, gl13, gl14, gl30, glstatemanager, options, rendersystem (+1 more)

### Community 91 - "CurseForge upload checklist: Glintsmith 2.0.0"
Cohesion: 0.25
Nodes (7): Before 2.0.0 goes public, CurseForge upload checklist: Glintsmith 2.0.0, Earlier release: Glintsmith 1.0.0, Relations, The file, The project page, What the owner has to decide or do by hand

### Community 92 - "net.minecraft.resources.ResourceLocation"
Cohesion: 0.24
Nodes (4): net.minecraft.client.renderer.texture.TextureManager, net.minecraft.resources.ResourceLocation, FileEntry, GlintTextures

### Community 93 - "org.spongepowered.asm.mixin.Mixin"
Cohesion: 0.18
Nodes (11): at, net.minecraft.client.renderer.RenderBuffers, org.spongepowered.asm.mixin.injection.Redirect, org.spongepowered.asm.mixin.Mixin, RenderBuffersAccessor, GlintShardsClockMixin, GlintSystemClockMixin, MinecraftExitMixin (+3 more)

### Community 94 - "GlintKind"
Cohesion: 0.11
Nodes (10): CompositeStateBuilder, GlintKind, ARMOR_ENTITY_GLINT, ENTITY_GLINT, ENTITY_GLINT_DIRECT, GLINT, GLINT_TRANSLUCENT, ShaderStateShard (+2 more)

### Community 96 - "Tri"
Cohesion: 0.33
Nodes (4): Tri, INHERIT, OFF, ON

### Community 98 - "CurseForge page material for Glintsmith"
Cohesion: 0.40
Nodes (4): CurseForge page material for Glintsmith, Images, Putting the description on CurseForge, Rebuilding

### Community 99 - "gradlew"
Cohesion: 0.83
Nodes (3): gradlew script, die(), warn()

### Community 100 - "Condition"
Cohesion: 0.50
Nodes (4): Condition, ANY, ENCHANTED, NOT_ENCHANTED

### Community 107 - ".build"
Cohesion: 0.31
Nodes (6): clientcommandmanager, clientcommandregistrationcallback, com.mojang.brigadier.CommandDispatcher, net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource, net.minecraft.commands.CommandBuildContext, GlintsmithCommands

### Community 109 - "GlintsmithKeys.java"
Cohesion: 0.17
Nodes (6): inputconstants, keybindinghelper, net.minecraft.client.KeyMapping, GlintsmithKeys, SystemToastId, systemtoast

### Community 110 - "StudioScreen"
Cohesion: 0.15
Nodes (6): net.minecraft.core.RegistryAccess, Look, Override, Job, Prop, StudioScreen

### Community 111 - "ColorPickerScreen"
Cohesion: 0.16
Nodes (4): java.util.function.IntConsumer, net.minecraft.client.gui.screens.Screen, ColorPickerScreen, Override

### Community 112 - "StudioBootstrap.java"
Cohesion: 0.13
Nodes (14): accessibilityonboardingscreen, difficulty, flatlevelgeneratorpreset, flatlevelgeneratorpresets, flatlevelsource, gamerules, gametype, levelsettings (+6 more)

### Community 113 - "net.minecraft.client.Minecraft"
Cohesion: 0.35
Nodes (3): net.minecraft.client.Minecraft, PreviewItems, Sample

### Community 114 - "OutlinePhase"
Cohesion: 0.18
Nodes (6): OutlinePhase, GUI, HAND, NONE, PREVIEW, WORLD

### Community 117 - "Handoff: Glintsmith 2.0.0 release (session 14)"
Cohesion: 0.17
Nodes (11): 10. How to resume, 1. Goal, 2. Requirements and constraints, 4. In progress when stopped, 5. Open work (ordered), 6. Known problems and blockers, 7. Decisions made (session 14), 8. Manual test checklist (owner, still untested by a human) (+3 more)

### Community 118 - "ModMenuIntegration.java"
Cohesion: 0.47
Nodes (4): com.terraformersmc.modmenu.api.ConfigScreenFactory, com.terraformersmc.modmenu.api.ModMenuApi, Override, ModMenuIntegration

### Community 119 - ".drawSubjects"
Cohesion: 0.25
Nodes (3): java.lang.reflect.Field, java.lang.reflect.Method, PipelineBridge

### Community 122 - "1.0.0 (Fabric 1.21.1)"
Cohesion: 0.20
Nodes (9): 1.0.0 (Fabric 1.21.1), 2.0.0 (Fabric 1.21.1) — 2026-10-18, Added, Added, Changed, Changelog, Fixed, Fixed (+1 more)

### Community 123 - "FrameWriter.java"
Cohesion: 0.14
Nodes (12): com.google.gson.JsonArray, com.mojang.blaze3d.platform.NativeImage, datetimeformatter, executors, executorservice, future, gsonbuilder, jsonnull (+4 more)

### Community 128 - "Final report: Glintsmith 2.0.0"
Cohesion: 0.25
Nodes (7): Checks and tests run (session 15), Final report: Glintsmith 2.0.0, Key decisions (full list in `decisions.md`), Known limitations and recommendations, Manual test checklist (owner; not yet done by a human), Model usage, What was implemented, per requirement

### Community 131 - "Glintsmith art"
Cohesion: 0.20
Nodes (9): Glintsmith art, Licence and sources, Missing input is never faked, Output files, Testing the layout without renders, The GIFs, Theme, Verify (+1 more)

### Community 133 - "Type"
Cohesion: 0.67
Nodes (3): Type, STILL, TURNTABLE

### Community 134 - "SideElementBakeTest.java"
Cohesion: 0.22
Nodes (7): facebakery, framesize, itemmodelgenerator, net.minecraft.client.renderer.texture.SpriteContents, net.minecraft.client.resources.model.ModelState, org.junit.jupiter.api.AfterAll, resourcemetadata

## Knowledge Gaps
- **156 isolated node(s):** `AUTO`, `CUSTOM`, `ENCHANTED`, `ALL`, `NONE` (+151 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 602 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **33 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `GlintsmithConfig` connect `GlintsmithConfig` to `.build`, `list`, `.t`, `Colors`, `OutlineSystem`, `org.jetbrains.annotations.Nullable`, `.sanitize`, `Glintsmith`, `ConfigTab`, `.from`, `net.minecraft.client.renderer.RenderType`, `StyleTable`, `RuleMatcherTest`, `PreviewRenderer.java`, `asserttrue`, `GlintsmithScreen`, `RulesTab`, `ColorStyle`, `net.minecraft.client.gui.GuiGraphics`, `OutlineSystem.java`, `RuleEngine`, `.trip`, `PreviewSession`, `.drawItem`, `LangFilesTest`, `StudioScreen.java`, `RuleEngineScopeTest`, `net.minecraft.resources.ResourceLocation`, `PreviewNotesTest`, `GlintsmithKeys.java`, `StudioScreen`, `ColorPickerScreen`, `.drawSubjects`, `GlintCategorySettings`, `PendingEditsTest`, `.update`?**
  _High betweenness centrality (0.127) - this node is a cross-community bridge._
- **Why does `The manifest` connect `lib.py` to `GlintPresets`, `OutlineDepthTest`, `common.py`, `Canvas`, `net.minecraft.world.item.ItemStack`?**
  _High betweenness centrality (0.088) - this node is a cross-community bridge._
- **Why does `OutlineSystem` connect `OutlineSystem` to `org.spongepowered.asm.mixin.injection.Inject`, `GlintsmithConfig`, `org.jetbrains.annotations.Nullable`, `net.minecraft.client.renderer.MultiBufferSource`, `PreviewRenderer.java`, `.sanitize`, `StudioScreen.java`, `OccluderBufferSource`, `TeeCaptureTest`, `.drawSubjects`, `OutlineSystem.java`, `IrisCompat`, `StyleTable`, `ShaderReloadListener`, `org.spongepowered.asm.mixin.Mixin`, `PhaseState`, `net.minecraft.world.item.ItemStack`?**
  _High betweenness centrality (0.060) - this node is a cross-community bridge._
- **What connects `AUTO`, `CUSTOM`, `ENCHANTED` to the rest of the system?**
  _156 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `logo.py` be split into smaller, more focused modules?**
  _Cohesion score 0.09747899159663866 - nodes in this community are weakly interconnected._
- **Should `GlintPresets` be split into smaller, more focused modules?**
  _Cohesion score 0.05307017543859649 - nodes in this community are weakly interconnected._
- **Should `list` be split into smaller, more focused modules?**
  _Cohesion score 0.12626262626262627 - nodes in this community are weakly interconnected._
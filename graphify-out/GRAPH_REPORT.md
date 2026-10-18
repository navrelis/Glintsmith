# Graph Report - Glintsmith  (2026-10-18)

## Corpus Check
- 248 files · ~1,107,762 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 13 file(s) not represented in the graph (top: (none) 4, .fsh 3, .properties 2)

## Summary
- 3234 nodes · 9836 edges · 136 communities (104 shown, 32 thin omitted)
- Extraction: 91% EXTRACTED · 9% INFERRED · 0% AMBIGUOUS · INFERRED: 889 edges (avg confidence: 0.84)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `491d1714`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- .validateTexture
- MaskTypesTest
- GlintNoise
- list
- .t
- Colors
- .forPhase
- OutlineMixinApplicationTest
- OutlineSystem
- net.minecraft.client.gui.GuiGraphics
- org.spongepowered.asm.mixin.Mixin
- ConfigTest
- Glintsmith
- colour
- ColorStyleButton
- StudioScreen.java
- lib.py
- ConfigTab
- Canvas
- verify.py
- TeeCaptureTest
- .from
- GlintSlot
- GlintContext
- net.minecraft.client.renderer.RenderType
- ColorEvaluatorTest
- OptionList
- StyleTable
- imgio.py
- Override
- PhaseState
- ScreenBounds
- PatternBox
- .acquire
- RuleMatcherTest
- at
- FloatSlider
- Glintsmith
- OutlineMixinApplicationTest.java
- MaskTypes.java
- GlintsmithConfig
- OutlineDepthTest
- Glintsmith capture shot list
- asserttrue
- OutlineShaders
- GlintsmithScreen
- studio.py
- TextureAtlasSpriteMixin.java
- RulesTab
- SideElementBakeTest
- TeeVertexConsumer
- ColorStyle
- GapFix
- pipeline.py
- composite.py
- PreviewPanel
- OutlineSystem.java
- Pattern
- org.junit.jupiter.api.Test
- ItemModelGeneratorMixin.java
- RuleEngine
- CompositeStateAccessor
- Glint (T6)
- net.minecraft.world.item.ItemStack
- PatternTest
- OccluderBufferSource
- org.jetbrains.annotations.Nullable
- .drawItem
- GlintLook
- preview.py
- GlintParams
- MaskTypes
- LangFilesTest
- SideElementBakeTest.java
- .onFrameBegin
- Glintsmith
- com.mojang.blaze3d.vertex.VertexConsumer
- PreviewNotesTest.java
- Glintsmith Studio (dev-only render mode)
- synth_studio.py
- Captures
- enchantment
- Scene
- Glintsmith.java
- GlintPresets
- SpanMixin
- net.minecraft.resources.ResourceLocation
- GlintPresetsTest
- StudioMod
- card
- CurseForge upload checklist: Glintsmith 2.0.0
- GlintTextures
- org.spongepowered.asm.mixin.injection.Redirect
- GlintKind
- GapFix.java
- Tri
- Kind
- CurseForge page material for Glintsmith
- gradlew
- Condition
- Plan (Glintsmith 1.0.0)
- decisions.md
- log.md
- ItemFacts
- PreviewLayoutTest
- .build
- GlintAtlasScaleTest
- .instance
- StudioScreen
- ColorPickerScreen
- StudioBootstrap.java
- net.minecraft.client.Minecraft
- ColorSwatch
- GuiHelpersTest
- Handoff: Glintsmith 2.0.0 release (session 14)
- Glintsmith
- .drawSubjects
- GlintCategorySettings
- PendingEditsTest
- 1.0.0 (Fabric 1.21.1)
- FrameWriter.java
- Recorder
- FrameWriter
- ConfigManager
- .update
- Final report: Glintsmith 2.0.0
- .build
- Glintsmith art
- State
- Type
- org.junit.jupiter.api.AfterAll
- renderstateshard

## God Nodes (most connected - your core abstractions)
1. `GlintsmithConfig` - 132 edges
2. `OutlineSystem` - 72 edges
3. `OutlineTarget` - 53 edges
4. `Rule` - 46 edges
5. `GlintSystem` - 44 edges
6. `ColorStyle` - 43 edges
7. `ConfigManager` - 42 edges
8. `ConfigTest` - 42 edges
9. `Canvas` - 38 edges
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

## Communities (136 total, 32 thin omitted)

### Community 1 - "MaskTypesTest"
Cohesion: 0.14
Nodes (4): LayeringStateShard, WriteMaskStateShard, CullStateShard, MaskTypesTest

### Community 3 - "list"
Cohesion: 0.10
Nodes (29): arraylist, arrays, assertarrayequals, assertnotsame, assertthrows, collections, directorystream, files (+21 more)

### Community 4 - ".t"
Cohesion: 0.10
Nodes (10): net.minecraft.client.gui.components.CycleButton, net.minecraft.network.chat.Component, OnPress, SettingRows, GlintTab, Override, Override, ModelFixTab (+2 more)

### Community 5 - "Colors"
Cohesion: 0.05
Nodes (9): ColorEvaluator, Compiled, Override, Colors, DurabilityAlert, Parsed, EnchantmentColors, ColorsTest (+1 more)

### Community 6 - ".forPhase"
Cohesion: 0.13
Nodes (7): OutlinePhase, GUI, HAND, NONE, PREVIEW, WORLD, OutlineContextsTest

### Community 7 - "OutlineMixinApplicationTest"
Cohesion: 0.07
Nodes (9): org.objectweb.asm.tree.AnnotationNode, org.objectweb.asm.tree.ClassNode, org.objectweb.asm.tree.MethodNode, org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin, org.spongepowered.asm.mixin.extensibility.IMixinInfo, GlintsmithMixinPlugin, Override, GlintMixinApplicationTest (+1 more)

### Community 8 - "OutlineSystem"
Cohesion: 0.10
Nodes (7): 12.2 The PREVIEW outline phase, How the real glint and outline reach the subjects, net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext, OutlineFailsafe, BufferSource, OutlineSystem, OutlineFailsafeTest

### Community 9 - "net.minecraft.client.gui.GuiGraphics"
Cohesion: 0.09
Nodes (34): abstracttexture, builtinregistries, button, chatformatting, commoncomponents, component, confirmscreen, consumer (+26 more)

### Community 10 - "org.spongepowered.asm.mixin.Mixin"
Cohesion: 0.15
Nodes (26): com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod, com.llamalad7.mixinextras.injector.wrapoperation.Operation, com.mojang.blaze3d.vertex.PoseStack, equipmentslot, net.minecraft.client.model.HumanoidModel, net.minecraft.client.player.LocalPlayer, net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer, net.minecraft.client.renderer.entity.layers.ElytraLayer (+18 more)

### Community 12 - "Glintsmith"
Cohesion: 0.18
Nodes (8): Environment facts (verified 2026-10-17), Later owner additions (chat, 2026-10-17), Owner answers (2026-10-17), Owner task (2026-10-17), Requirements (Glintsmith, session 2026-10-17), Session 12 owner additions (2026-10-18), Glintsmith, Override

### Community 13 - "colour"
Cohesion: 0.08
Nodes (37): el_bullets(), el_heading(), el_panel(), el_rule(), el_strip(), _advance(), balanced_wrap(), bay() (+29 more)

### Community 14 - "ColorStyleButton"
Cohesion: 0.27
Nodes (3): net.minecraft.client.gui.components.AbstractButton, ColorStyleButton, Override

### Community 15 - "StudioScreen.java"
Cohesion: 0.08
Nodes (23): atomicmovenotsupportedexception, basicfileattributes, compoundtag, entityrenderdispatcher, entitytype, fabricloader, filechannel, fogrenderer (+15 more)

### Community 16 - "lib.py"
Cohesion: 0.05
Nodes (60): argparse, anchor_x(), chip_list(), el_capture(), el_chips(), el_studio(), el_text(), label() (+52 more)

### Community 17 - "ConfigTab"
Cohesion: 0.11
Nodes (7): net.minecraft.client.gui.components.tabs.Tab, net.minecraft.client.gui.navigation.ScreenRectangle, ConfigTab, Override, EnchantmentColorsTab, EnchantmentRow, Override

### Community 18 - "Canvas"
Cohesion: 0.07
Nodes (22): bar(), caption_for(), encode(), The caption of the n-th (1 based) source frame: `captions` are the looks in…, The GIF made of every `step`-th frame; one kept frame lasts step / fps seconds,…, Canvas, _glyphs(), An RGBA canvas on a bytearray. Coordinates outside the canvas are clipped,… (+14 more)

### Community 19 - "verify.py"
Cohesion: 0.08
Nodes (48): _chunk(), lzw_decode(), parse_gif(), png_bytes(), pixels: bytes-like, w*h*channels, row-major. channels 3 = RGB, 4 = RGBA. text:…, Parse back a GIF written by gif_bytes (or any simple GIF). Returns dict with…, contrast(), luminance() (+40 more)

### Community 20 - "TeeCaptureTest"
Cohesion: 0.13
Nodes (6): java.nio.ByteBuffer, Override, TeeBufferSource, TeePool, GuiItemFlushTest, TeeCaptureTest

### Community 21 - ".from"
Cohesion: 0.16
Nodes (4): 3. Done (this session), Changed files (2.0.0, sessions 13 to 15), GlintCategoryRuntime, GlintRuntimeSettingsTest

### Community 22 - "GlintSlot"
Cohesion: 0.11
Nodes (9): Black/white pairs, Camera and light, Render target and supersampling, Studio clock, What happens during a run, GlintShards, GlintSlot, Factory (+1 more)

### Community 23 - "GlintContext"
Cohesion: 0.12
Nodes (5): 3.1 Phases, GlintCategories, Frame, GlintContext, GlintContextTest

### Community 24 - "net.minecraft.client.renderer.RenderType"
Cohesion: 0.10
Nodes (10): 12.1 Mechanism, 5.2 Strategy, 8.2 T6: glint system (`navrelis.glintsmith.render.glint`), net.minecraft.client.renderer.RenderType, GlintRuntimeSettings, GlintSystem, BufferSource, Preparations (+2 more)

### Community 26 - "OptionList"
Cohesion: 0.13
Nodes (10): net.minecraft.client.gui.components.AbstractWidget, net.minecraft.client.gui.components.ContainerObjectSelectionList, net.minecraft.client.gui.components.events.GuiEventListener, net.minecraft.client.gui.narration.NarratableEntry, net.minecraft.util.FormattedCharSequence, Override, LabeledRow, OptionList (+2 more)

### Community 27 - "StyleTable"
Cohesion: 0.05
Nodes (14): 3.4.1 Proportional thickness (2.0.0), 3.4 Per-item data encoding, nativeimage, net.minecraft.client.renderer.texture.DynamicTexture, org.joml.Matrix4f, org.joml.Matrix4fc, OutlineRuntimeSettings, OutlineScale (+6 more)

### Community 28 - "imgio.py"
Cohesion: 0.04
Nodes (57): colour_histogram(), encode_gif_frames(), gif_bytes(), gif_compose(), lzw_encode(), median_cut(), Dependency-free image encoders (PNG, animated GIF89a) and a small GIF decoder…, GIF flavoured variable-width LZW. indices: bytes of palette indices (each <… (+49 more)

### Community 29 - "Override"
Cohesion: 0.18
Nodes (4): Override, Recorder, RecordingSource, Throwing

### Community 30 - "PhaseState"
Cohesion: 0.16
Nodes (9): com.mojang.blaze3d.pipeline.RenderTarget, 3.3 Targets, depth and resize, GlState, MaskDepth, CLEAR, COPY, NONE, OutlineCompositor (+1 more)

### Community 32 - "PatternBox"
Cohesion: 0.21
Nodes (4): net.minecraft.network.chat.Style, PatternText, Override, PatternBox

### Community 33 - ".acquire"
Cohesion: 0.14
Nodes (6): 4. Studio renders, Still scenes (4), Turntables (3), GlintSlotPool, FakeType, GlintSlotPoolTest

### Community 35 - "at"
Cohesion: 0.14
Nodes (16): at, com.mojang.blaze3d.vertex.BufferBuilder, net.minecraft.client.DeltaTracker, net.minecraft.client.renderer.GameRenderer, net.minecraft.client.renderer.texture.TextureAtlas, org.spongepowered.asm.mixin.injection.callback.CallbackInfo, org.spongepowered.asm.mixin.injection.Inject, spriteloader (+8 more)

### Community 36 - "FloatSlider"
Cohesion: 0.19
Nodes (8): java.util.function.DoubleConsumer, java.util.function.DoubleFunction, java.util.function.DoubleToIntFunction, net.minecraft.client.gui.components.AbstractSliderButton, FloatSlider, Override, GradientSlider, Override

### Community 37 - "Glintsmith"
Cohesion: 0.15
Nodes (13): Building, Commands and keys, Compared with other mods and packs, Compatibility, Configuration, Credits and acknowledgements, Features, Glintsmith (+5 more)

### Community 38 - "OutlineMixinApplicationTest.java"
Cohesion: 0.17
Nodes (22): abstractinsnnode, assertnotnull, classreader, enumeration, fabriclauncherbase, fieldinsnnode, imixintransformer, inputstream (+14 more)

### Community 39 - "MaskTypes.java"
Cohesion: 0.11
Nodes (16): CompositeState, gl11, gl13, gl14, gl30, glstatemanager, net.minecraft.client.renderer.RenderStateShard, org.spongepowered.asm.mixin.gen.Invoker (+8 more)

### Community 40 - "GlintsmithConfig"
Cohesion: 0.09
Nodes (20): com.google.gson.Gson, nullable, objects, GlintsmithConfig, OutlineContexts, GlintCategory, ARMOR, ENTITY (+12 more)

### Community 41 - "OutlineDepthTest"
Cohesion: 0.16
Nodes (4): 3.6 Composite procedure (`OutlineCompositor.composite(PhaseState, Params)`), OutlineDepth, Grid, OutlineDepthTest

### Community 42 - "Glintsmith capture shot list"
Cohesion: 0.12
Nodes (15): 0. Rig, 1. World and rooms, 2. Glintsmith config, 3. In-world captures (14), 5. Checklist, Backdrop and close-ups, Enchantment shorthand, Frames and stand templates (+7 more)

### Community 43 - "asserttrue"
Cohesion: 0.09
Nodes (28): abstractappender, assertdoesnotthrow, assertequals, assertfalse, assertinstanceof, assertnotequals, assertnull, assertsame (+20 more)

### Community 44 - "OutlineShaders"
Cohesion: 0.11
Nodes (10): com.mojang.blaze3d.vertex.VertexBuffer, com.mojang.blaze3d.vertex.VertexFormat, defaultvertexformat, gl20, net.minecraft.client.renderer.ShaderInstance, net.minecraft.server.packs.resources.ResourceProvider, resourcemanager, FullscreenQuad (+2 more)

### Community 45 - "GlintsmithScreen"
Cohesion: 0.12
Nodes (9): com.terraformersmc.modmenu.api.ConfigScreenFactory, com.terraformersmc.modmenu.api.ModMenuApi, net.minecraft.client.gui.components.tabs.TabManager, net.minecraft.client.gui.components.tabs.TabNavigationBar, GlintsmithScreen, Override, SystemToastId, Override (+1 more)

### Community 46 - "studio.py"
Cohesion: 0.10
Nodes (29): read_png_size(), The studio scenes (the contract), find_frame(), load(), Studio scenes: finding, loading and validating the frame contract. Python 3…, The frame dict for `ref`: a still name, a frame index (1 based) or a file stem…, A scene that is not there (kind 'missing') or not readable (kind 'error')., (K path, W path) of a frame dict. (+21 more)

### Community 47 - "TextureAtlasSpriteMixin.java"
Cohesion: 0.38
Nodes (4): com.llamalad7.mixinextras.injector.ModifyReturnValue, inventorymenu, net.minecraft.client.renderer.texture.TextureAtlasSprite, TextureAtlasSpriteMixin

### Community 48 - "RulesTab"
Cohesion: 0.20
Nodes (4): net.minecraft.client.gui.components.Checkbox, Override, RuleRow, RulesTab

### Community 49 - "SideElementBakeTest"
Cohesion: 0.22
Nodes (5): net.minecraft.client.renderer.block.model.BakedQuad, net.minecraft.client.renderer.block.model.BlockElement, net.minecraft.client.renderer.texture.SpriteContents, net.minecraft.client.resources.model.ModelState, SideElementBakeTest

### Community 51 - "ColorStyle"
Cohesion: 0.12
Nodes (5): Outline, Targets, TargetThickness, ColorStyle, Override

### Community 53 - "pipeline.py"
Cohesion: 0.09
Nodes (34): array, DEFAULT_STYLE with `overrides` (a nested dict, same keys) merged in., style(), box_sums(), _int(), matte_files(), matte_pair(), premultiplied_planes() (+26 more)

### Community 54 - "composite.py"
Cohesion: 0.09
Nodes (33): alpha_bbox(), backdrop(), _blend_plane(), blur_grid(), _blur_rows(), compose(), contact_plane(), drop_plane() (+25 more)

### Community 55 - "PreviewPanel"
Cohesion: 0.11
Nodes (9): net.minecraft.client.gui.Font, net.minecraft.client.gui.narration.NarrationElementOutput, net.minecraft.client.sounds.SoundManager, Override, PreviewPanel, PreviewSession, Source, HELD (+1 more)

### Community 56 - "OutlineSystem.java"
Cohesion: 0.07
Nodes (23): clientlifecycleevents, collection, com.mojang.blaze3d.vertex.ByteBufferBuilder, graphicsstatus, IrisApi, itemstack, java.util.SequencedMap, loggerfactory (+15 more)

### Community 58 - "org.junit.jupiter.api.Test"
Cohesion: 0.09
Nodes (5): org.junit.jupiter.api.Test, GapFixTest, GlintsmithMixinPluginTest, MixinApplicationTest, SmokeTest

### Community 59 - "ItemModelGeneratorMixin.java"
Cohesion: 0.17
Nodes (10): blockelementface, coerce, com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation, direction, local, net.minecraft.client.renderer.block.model.ItemModelGenerator, ItemModelGeneratorMixin, ItemModelGeneratorSpanMixin (+2 more)

### Community 60 - "RuleEngine"
Cohesion: 0.08
Nodes (19): 1. Decision summary, 2. Frame timeline in 1.21.1 and where Glintsmith acts, 3.5 Shaders, 3.7 Performance and allocation rules (T5), 3.8 Failsafe (T5), 3. Outline pipeline in detail (T5), 4. Hook table, 6. Compatibility matrix and failure behaviour (+11 more)

### Community 61 - "CompositeStateAccessor"
Cohesion: 0.12
Nodes (13): 3.2 Teeing (capture), Outline (T5), EmptyTextureStateShard, net.minecraft.client.renderer.RenderBuffers, org.spongepowered.asm.mixin.gen.Accessor, RenderType.CompositeState, BufferSource, RenderBuffersAccessor (+5 more)

### Community 62 - "Glint (T6)"
Cohesion: 0.24
Nodes (9): Glint (T6), org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable, org.spongepowered.asm.mixin.injection.ModifyVariable, org.spongepowered.asm.mixin.Pseudo, ImmediatelyFastBufferSourceGlintMixin, ImmediatelyFastGuiSourceGlintMixin, BufferSource, ImmediatelyFastHudGlintMixin (+1 more)

### Community 63 - "net.minecraft.world.item.ItemStack"
Cohesion: 0.14
Nodes (15): itemrenderer, itemtags, lighting, lighttexture, net.minecraft.client.renderer.block.model.ItemTransform, net.minecraft.client.renderer.entity.ItemRenderer, net.minecraft.client.resources.model.BakedModel, net.minecraft.world.item.ItemDisplayContext (+7 more)

### Community 65 - "OccluderBufferSource"
Cohesion: 0.13
Nodes (5): BufferSource, 5.5 Texture management (`GlintTextures`), MaskBufferSource, Override, OccluderBufferSource

### Community 66 - "org.jetbrains.annotations.Nullable"
Cohesion: 0.08
Nodes (10): java.util.function.IntConsumer, net.minecraft.client.gui.screens.Screen, org.jetbrains.annotations.Nullable, ColorStyleScreen, Override, ConfigSubScreen, Override, PreviewHost (+2 more)

### Community 67 - ".drawItem"
Cohesion: 0.11
Nodes (10): axis, FunctionalInterface, org.joml.Matrix3f, quaternionf, Fit, PreviewPose, BufferSource, Margin (+2 more)

### Community 68 - "GlintLook"
Cohesion: 0.27
Nodes (6): 8.1 T5: outline core (`navrelis.glintsmith.render.outline`), 8.3 T8: exact wiring, 8. Package and class spec, resources, and T8 hooks, GlintLook, OutlineLook, ResolvedStyle

### Community 69 - "preview.py"
Cohesion: 0.21
Nodes (13): build(), check(), inline(), main(), pipeline_outputs(), Builds docs/curseforge/preview.html from description.md and checks the marker…, The image names the art pipeline builds (the "out" of every card and GIF of…, render() (+5 more)

### Community 70 - "GlintParams"
Cohesion: 0.17
Nodes (8): 5.1 How vanilla 1.21.1 draws glint (verified), 5.3 Categories, context and resolution, 5.4.1 Atlas-independent scale (2.0.0), 5.4 Slot render types, 5.7 Edge cases T6 must handle, 5. Glint system in detail (T6), GlintParams, GlintParamsTest

### Community 71 - "MaskTypes"
Cohesion: 0.11
Nodes (15): OutputStateShard, Mode, FOOTER, HIDDEN, SIDE, DepthTestStateShard, Entry, CullStateShard (+7 more)

### Community 72 - "LangFilesTest"
Cohesion: 0.17
Nodes (7): 12.3 The snapshot and the pending edits chain, 12.4 What is shown, 12.5 Panel and layout, 12.6 Limits (known and by design), 12.7 Tests and what is still open in game, 12. Live preview (2.0.0), LangFilesTest

### Community 73 - "SideElementBakeTest.java"
Cohesion: 0.09
Nodes (20): bootstrap, datacomponents, enchantments, facebakery, framesize, hashmap, holder, holderlookup (+12 more)

### Community 75 - "Glintsmith"
Cohesion: 0.20
Nodes (9): Commands and keys, Compatibility notes, Credits and licence, Glintsmith, How it compares, Performance, Requirements, Sodium and Iris (+1 more)

### Community 76 - "com.mojang.blaze3d.vertex.VertexConsumer"
Cohesion: 0.27
Nodes (3): com.mojang.blaze3d.vertex.VertexConsumer, Override, MaskVertexConsumer

### Community 77 - "PreviewNotesTest.java"
Cohesion: 0.23
Nodes (6): set, OutlineCondition, ALL, ENCHANTED, NONE, TestConfigs

### Community 78 - "Glintsmith Studio (dev-only render mode)"
Cohesion: 0.18
Nodes (8): Example scenes, Files, Frame contract (output), Glint scale, Glintsmith Studio (dev-only render mode), Limits, Running a scene, Scene format (input)

### Community 79 - "synth_studio.py"
Cohesion: 0.10
Nodes (25): draw_sword(), Frame, main(), make_contract(), turntable(), make_scene(), Fill a convex polygon; stripe = (centre at y=0, slope, half width, colour)…, Draw one extruded sword centred on (cx, cy) with unit length u (supersampled… (+17 more)

### Community 80 - "Captures"
Cohesion: 0.08
Nodes (23): main(), Captures, load_manifest(), Every capture the manifest needs, in manifest order: [(image name, relative…, ok', 'MISSING' or 'BROKEN' for a "studio/<scene>/[<frame>]" entry of…, True when every studio frame a card (a manifest entry) shows is on disk and…, Collects what could not be built from real captures (it is never faked)., Reads capture PNGs from one folder and keeps them decoded for the length of a… (+15 more)

### Community 82 - "Scene"
Cohesion: 0.21
Nodes (10): com.google.gson.JsonElement, com.google.gson.JsonObject, org.joml.Vector3f, Camera, Kind, ARMOR_STAND, ITEM, Scene (+2 more)

### Community 83 - "Glintsmith.java"
Cohesion: 0.12
Nodes (10): commonlifecycleevents, java.util.IdentityHashMap, jsonarray, jsonprimitive, GapFixMode, AUTO, CUSTOM, GapFix (+2 more)

### Community 84 - "GlintPresets"
Cohesion: 0.27
Nodes (4): 10. Manual in-game test checklist, 5.6 Procedural presets (no third-party art), Enchantment glint, GlintPresets

### Community 85 - "SpanMixin"
Cohesion: 0.43
Nodes (3): org.spongepowered.asm.mixin.Shadow, Override, SpanMixin

### Community 86 - "net.minecraft.resources.ResourceLocation"
Cohesion: 0.15
Nodes (11): java.util.jar.JarFile, net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener, net.minecraft.resources.ResourceLocation, net.minecraft.server.packs.resources.ResourceManager, Override, TextureReloadListener, Override, ShaderReloadListener (+3 more)

### Community 89 - "StudioMod"
Cohesion: 0.17
Nodes (3): net.fabricmc.api.ClientModInitializer, StudioBootstrap, StudioMod

### Community 90 - "card"
Cohesion: 0.17
Nodes (10): build_all(), build_card(), Ctx, Build every card of the manifest. Returns {file name: PNG bytes}., card(), mix(), noise(), A tiny fixed generator, so the dither is the same on every machine and Python… (+2 more)

### Community 91 - "CurseForge upload checklist: Glintsmith 2.0.0"
Cohesion: 0.25
Nodes (7): Before 2.0.0 goes public, CurseForge upload checklist: Glintsmith 2.0.0, Earlier release: Glintsmith 1.0.0, Relations, The file, The project page, What the owner has to decide or do by hand

### Community 92 - "GlintTextures"
Cohesion: 0.21
Nodes (3): net.minecraft.client.renderer.texture.TextureManager, FileEntry, GlintTextures

### Community 93 - "org.spongepowered.asm.mixin.injection.Redirect"
Cohesion: 0.17
Nodes (7): org.spongepowered.asm.mixin.injection.Redirect, GlintShardsClockMixin, GlintSystemClockMixin, MinecraftExitMixin, OutlineSystemClockMixin, RenderStateShardClockMixin, StudioClock

### Community 94 - "GlintKind"
Cohesion: 0.12
Nodes (10): CompositeStateBuilder, GlintKind, ARMOR_ENTITY_GLINT, ENTITY_GLINT, ENTITY_GLINT_DIRECT, GLINT, GLINT_TRANSLUCENT, ShaderStateShard (+2 more)

### Community 96 - "Tri"
Cohesion: 0.33
Nodes (4): Tri, INHERIT, OFF, ON

### Community 97 - "Kind"
Cohesion: 0.33
Nodes (6): Kind, ALL, EXACT, GLOB, NAMESPACE, TAG

### Community 98 - "CurseForge page material for Glintsmith"
Cohesion: 0.40
Nodes (4): CurseForge page material for Glintsmith, Images, Putting the description on CurseForge, Rebuilding

### Community 99 - "gradlew"
Cohesion: 0.83
Nodes (3): gradlew script, die(), warn()

### Community 100 - "Condition"
Cohesion: 0.50
Nodes (4): Condition, ANY, ENCHANTED, NOT_ENCHANTED

### Community 105 - "ItemFacts"
Cohesion: 0.22
Nodes (3): PreviewNotes, ItemFacts, PreviewNotesTest

### Community 107 - ".build"
Cohesion: 0.13
Nodes (6): clientcommandmanager, clientcommandregistrationcallback, com.mojang.brigadier.CommandDispatcher, net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource, net.minecraft.commands.CommandBuildContext, GlintsmithCommands

### Community 109 - ".instance"
Cohesion: 0.14
Nodes (7): clienttickevents, inputconstants, keybindinghelper, net.minecraft.client.KeyMapping, GlintsmithKeys, SystemToastId, systemtoast

### Community 110 - "StudioScreen"
Cohesion: 0.22
Nodes (6): net.minecraft.world.entity.decoration.ArmorStand, Look, Override, Job, Prop, StudioScreen

### Community 112 - "StudioBootstrap.java"
Cohesion: 0.13
Nodes (14): accessibilityonboardingscreen, difficulty, flatlevelgeneratorpreset, flatlevelgeneratorpresets, flatlevelsource, gamerules, gametype, levelsettings (+6 more)

### Community 113 - "net.minecraft.client.Minecraft"
Cohesion: 0.24
Nodes (6): net.minecraft.client.Minecraft, net.minecraft.core.RegistryAccess, net.minecraft.resources.ResourceKey, RegistryLookup, PreviewItems, Sample

### Community 114 - "ColorSwatch"
Cohesion: 0.23
Nodes (5): java.util.function.IntSupplier, net.minecraft.client.gui.ComponentPath, net.minecraft.client.gui.navigation.FocusNavigationEvent, ColorSwatch, Override

### Community 116 - "GuiHelpersTest"
Cohesion: 0.18
Nodes (3): Hsv, TextureNames, GuiHelpersTest

### Community 117 - "Handoff: Glintsmith 2.0.0 release (session 14)"
Cohesion: 0.17
Nodes (11): 10. How to resume, 1. Goal, 2. Requirements and constraints, 4. In progress when stopped, 5. Open work (ordered), 6. Known problems and blockers, 7. Decisions made (session 14), 8. Manual test checklist (owner, still untested by a human) (+3 more)

### Community 118 - "Glintsmith"
Cohesion: 0.18
Nodes (9): Commands and keys, Compatibility notes, Credits and licence, Glintsmith, How it compares, Performance, Requirements, Sodium and Iris (+1 more)

### Community 119 - ".drawSubjects"
Cohesion: 0.25
Nodes (3): java.lang.reflect.Field, java.lang.reflect.Method, PipelineBridge

### Community 122 - "1.0.0 (Fabric 1.21.1)"
Cohesion: 0.20
Nodes (9): 1.0.0 (Fabric 1.21.1), 2.0.0 (Fabric 1.21.1) — 2026-10-18, Added, Added, Changed, Changelog, Fixed, Fixed (+1 more)

### Community 123 - "FrameWriter.java"
Cohesion: 0.20
Nodes (9): datetimeformatter, executors, executorservice, future, gsonbuilder, jsonnull, jsonobject, offsetdatetime (+1 more)

### Community 125 - "FrameWriter"
Cohesion: 0.28
Nodes (3): com.google.gson.JsonArray, com.mojang.blaze3d.platform.NativeImage, FrameWriter

### Community 128 - "Final report: Glintsmith 2.0.0"
Cohesion: 0.25
Nodes (7): Checks and tests run (session 15), Final report: Glintsmith 2.0.0, Key decisions (full list in `decisions.md`), Known limitations and recommendations, Manual test checklist (owner; not yet done by a human), Model usage, What was implemented, per requirement

### Community 131 - "Glintsmith art"
Cohesion: 0.33
Nodes (5): Glintsmith art, Licence and sources, Output files, Theme, Verify

### Community 132 - "State"
Cohesion: 0.40
Nodes (5): State, LOADING, MENU, RUNNING, SETTLING

### Community 133 - "Type"
Cohesion: 0.67
Nodes (3): Type, STILL, TURNTABLE

## Knowledge Gaps
- **146 isolated node(s):** `AUTO`, `CUSTOM`, `ENCHANTED`, `ALL`, `NONE` (+141 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 580 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **32 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `GlintsmithConfig` connect `GlintsmithConfig` to `.build`, `list`, `.t`, `Colors`, `OutlineSystem`, `net.minecraft.client.gui.GuiGraphics`, `ConfigTest`, `Glintsmith`, `StudioScreen.java`, `ConfigTab`, `.from`, `net.minecraft.client.renderer.RenderType`, `StyleTable`, `RuleMatcherTest`, `asserttrue`, `GlintsmithScreen`, `RulesTab`, `ColorStyle`, `PreviewPanel`, `OutlineSystem.java`, `org.junit.jupiter.api.Test`, `RuleEngine`, `net.minecraft.world.item.ItemStack`, `org.jetbrains.annotations.Nullable`, `.drawItem`, `LangFilesTest`, `SideElementBakeTest.java`, `PreviewNotesTest.java`, `Glintsmith.java`, `GlintTextures`, `ItemFacts`, `.build`, `.instance`, `StudioScreen`, `ColorPickerScreen`, `.toJson`, `.drawSubjects`, `GlintCategorySettings`, `PendingEditsTest`, `ConfigManager`, `.update`?**
  _High betweenness centrality (0.130) - this node is a cross-community bridge._
- **Why does `The manifest` connect `colour` to `OutlineDepthTest`, `Captures`, `lib.py`, `Canvas`, `GlintPresets`, `net.minecraft.world.item.ItemStack`?**
  _High betweenness centrality (0.080) - this node is a cross-community bridge._
- **Why does `OutlineSystem` connect `OutlineSystem` to `.onClientStopping`, `net.minecraft.client.gui.GuiGraphics`, `org.spongepowered.asm.mixin.Mixin`, `StudioScreen.java`, `TeeCaptureTest`, `StyleTable`, `PhaseState`, `at`, `GlintsmithConfig`, `OutlineSystem.java`, `CompositeStateAccessor`, `net.minecraft.world.item.ItemStack`, `OccluderBufferSource`, `.onFrameBegin`, `Glintsmith.java`, `net.minecraft.resources.ResourceLocation`, `org.spongepowered.asm.mixin.injection.Redirect`, `.instance`, `.drawSubjects`?**
  _High betweenness centrality (0.050) - this node is a cross-community bridge._
- **What connects `AUTO`, `CUSTOM`, `ENCHANTED` to the rest of the system?**
  _146 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `.validateTexture` be split into smaller, more focused modules?**
  _Cohesion score 0.14666666666666667 - nodes in this community are weakly interconnected._
- **Should `MaskTypesTest` be split into smaller, more focused modules?**
  _Cohesion score 0.14285714285714285 - nodes in this community are weakly interconnected._
- **Should `GlintNoise` be split into smaller, more focused modules?**
  _Cohesion score 0.10837438423645321 - nodes in this community are weakly interconnected._
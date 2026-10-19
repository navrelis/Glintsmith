# Graph Report - eloquent-wing-a06858  (2026-10-19)

## Corpus Check
- 259 files · ~562,340 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 14 file(s) not represented in the graph (top: (none) 5, .fsh 3, .properties 2)

## Summary
- 3365 nodes · 10083 edges · 139 communities (109 shown, 30 thin omitted)
- Extraction: 91% EXTRACTED · 9% INFERRED · 0% AMBIGUOUS · INFERRED: 906 edges (avg confidence: 0.84)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `aeb1e94a`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- GlintTexturesTest
- logo.py
- GlintPresets
- Rule
- .t
- .parseHex
- org.junit.jupiter.api.Test
- OutlineMixinApplicationTest
- OutlineSystem
- PreviewPanel.java
- net.minecraft.world.item.ItemStack
- .toJson
- Glintsmith
- lib.py
- ColorStyleButton.java
- ConfigManager.java
- org.jetbrains.annotations.Nullable
- EnchantmentColorsTab
- Canvas
- verify.py
- TeeCaptureTest
- .sanitize
- GlintSlot
- GlintContext
- GlintSystem
- ColorEvaluatorTest
- net.minecraft.client.gui.GuiGraphics
- StyleTable
- gifenc.py
- com.mojang.blaze3d.vertex.VertexConsumer
- PhaseState
- ScreenBounds
- PatternBox.java
- .acquire
- RuleMatcherTest
- org.spongepowered.asm.mixin.injection.Inject
- FloatSlider
- Glintsmith
- LangFilesTest.java
- SideElementBakeTest.java
- OutlineTarget
- OutlineDepthTest
- Looks.java
- assertequals
- OutlineShaders
- GlintsmithScreen
- studio.py
- ItemModelGeneratorMixin.java
- RulesTab
- SideElementBakeTest
- TeeVertexConsumer
- .clamp
- GapFix
- list
- composite.py
- PreviewPanel
- .forPhase
- FrameWriter.java
- PreviewItems.java
- ItemModelGeneratorSpanMixin.java
- GlintCategory
- GuiHelpersTest
- IrisCompat
- BoundsKey
- Handoff: Glintsmith 2.0.1 (stray-dot fixes, release) — finished
- MaskBufferSource
- Tri
- .drawItem
- GlintsmithConfig
- preview.py
- GlintParams
- MaskTypesTest
- LangFilesTest
- StudioScreen.java
- RuleEditScreen
- Glintsmith
- net.minecraft.client.renderer.RenderType
- Handoff: Glintsmith page images re-render with the 2.1.0 default look, then CurseForge update
- Glintsmith Studio (dev-only render mode)
- synth_studio.py
- common.py
- enchantment
- Scene
- OutlineSystem.java
- CompositeStateAccessor
- SpanMixin
- net.minecraft.resources.ResourceLocation
- ConfigSubScreen
- MaskTypes.java
- net.minecraft.client.Minecraft
- rendersystem
- CurseForge upload checklist: Glintsmith 2.1.0
- GlintTextures
- org.spongepowered.asm.mixin.Mixin
- GlintKind
- .validateTexture
- Entry
- .onFrameBegin
- CurseForge page material for Glintsmith
- gradlew
- MaskVertexConsumer
- Plan (Glintsmith 1.0.0)
- decisions.md
- log.md
- PreviewNotesTest
- PreviewLayoutTest
- GlintsmithKeys.java
- GlintAtlasScaleTest
- OccluderBufferSource
- StudioScreen
- ColorPickerScreen
- StudioBootstrap.java
- Recorder
- gif.py
- Glintsmith
- Requirements (Glintsmith, session 2026-10-17)
- MaskTypes
- .drawSubjects
- OutlinePhase
- PendingEditsTest
- Changelog
- Handoff: Glintsmith 2.1.0 (owner config as the built-in defaults, then upload)
- Pattern
- org.joml.Vector3f
- pipeline.py
- ItemInHandRendererGlintMixin.java
- Final report: Glintsmith 2.0.0
- com.google.gson.JsonObject
- Handoff: Glintsmith lineup stray outline dot (root cause + outline fix)
- Type
- Versioning
- renderstateshard
- ColorStyleScreen
- DefaultConfigTest
- ColorMode
- StudioMod
- TextureReloadListener

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

## Communities (139 total, 30 thin omitted)

### Community 1 - "logo.py"
Cohesion: 0.10
Nodes (32): build(), _compose_task(), frame(), _frame_task(), glow_grids(), glow_layer(), _init_compose(), _init_frames() (+24 more)

### Community 2 - "GlintPresets"
Cohesion: 0.05
Nodes (22): 0. Rig, 1. World and rooms, 2. Glintsmith config, 3. In-world captures (14), 5. Checklist, Backdrop and close-ups, Enchantment shorthand, Frames and stand templates (+14 more)

### Community 3 - "Rule"
Cohesion: 0.16
Nodes (11): assertnotsame, assertthrows, filetime, ColorStyle, Override, OutlineSettings, Condition, ANY (+3 more)

### Community 4 - ".t"
Cohesion: 0.11
Nodes (9): net.minecraft.client.gui.components.Tooltip, net.minecraft.network.chat.Component, OnPress, SettingRows, Override, OutlineTab, Ui, OptionList (+1 more)

### Community 5 - ".parseHex"
Cohesion: 0.09
Nodes (5): DurabilityAlert, Parsed, EnchantmentColors, ColorsTest, DurabilityAndEnchantmentColorsTest

### Community 6 - "org.junit.jupiter.api.Test"
Cohesion: 0.07
Nodes (7): Session 18: lineup.gif on the page, trident preview fix, 2.0.2 release, org.junit.jupiter.api.Test, GapFixTest, GlintsmithMixinPluginTest, MixinApplicationTest, PreviewBoundsTest, SmokeTest

### Community 7 - "OutlineMixinApplicationTest"
Cohesion: 0.07
Nodes (9): org.objectweb.asm.tree.AnnotationNode, org.objectweb.asm.tree.ClassNode, org.objectweb.asm.tree.MethodNode, org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin, org.spongepowered.asm.mixin.extensibility.IMixinInfo, GlintsmithMixinPlugin, Override, GlintMixinApplicationTest (+1 more)

### Community 8 - "OutlineSystem"
Cohesion: 0.10
Nodes (7): 12.2 The PREVIEW outline phase, How the real glint and outline reach the subjects, net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext, OutlineFailsafe, BufferSource, OutlineSystem, OutlineFailsafeTest

### Community 9 - "PreviewPanel.java"
Cohesion: 0.09
Nodes (21): chatformatting, component, font, formattedcharsequence, glfw, i18n, java.util.function.IntSupplier, narratedelementtype (+13 more)

### Community 10 - "net.minecraft.world.item.ItemStack"
Cohesion: 0.11
Nodes (27): com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod, com.llamalad7.mixinextras.injector.wrapoperation.Operation, com.mojang.blaze3d.vertex.PoseStack, equipmentslot, itemtags, net.minecraft.client.model.HumanoidModel, net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer, net.minecraft.client.renderer.entity.ItemRenderer (+19 more)

### Community 13 - "lib.py"
Cohesion: 0.05
Nodes (72): anchor_x(), build_all(), build_card(), chip_list(), Ctx, el_bullets(), el_capture(), el_chips() (+64 more)

### Community 14 - "ColorStyleButton.java"
Cohesion: 0.14
Nodes (4): net.minecraft.client.gui.components.AbstractButton, ColorStyleButton, Override, ColorEvaluator

### Community 15 - "ConfigManager.java"
Cohesion: 0.10
Nodes (19): abstractappender, assertdoesnotthrow, assumptions, atomicinteger, atomicmovenotsupportedexception, basicfileattributes, copyonwritearraylist, filechannel (+11 more)

### Community 16 - "org.jetbrains.annotations.Nullable"
Cohesion: 0.10
Nodes (23): abstracttexture, button, commoncomponents, confirmscreen, consumer, createworldscreen, cyclebutton, minecraft (+15 more)

### Community 17 - "EnchantmentColorsTab"
Cohesion: 0.25
Nodes (3): EnchantmentColorsTab, EnchantmentRow, Override

### Community 18 - "Canvas"
Cohesion: 0.07
Nodes (16): Canvas, An RGBA canvas on a bytearray. Coordinates outside the canvas are clipped,…, A new canvas of the region; parts outside this canvas are transparent., Nearest-neighbour upscale by the whole factor k., Box-filter downscale by the whole factor n (every output pixel is the mean of…, Area-filter resize to ow x oh: every output pixel is the exact mean of the…, Draw src at (x, y): transparent pixels are skipped, half-transparent ones…, PNG bytes: RGB when fully opaque, RGBA otherwise (text: optional tEXt comment). (+8 more)

### Community 19 - "verify.py"
Cohesion: 0.08
Nodes (49): _chunk(), parse_gif(), png_bytes(), pixels: bytes-like, w*h*channels, row-major. channels 3 = RGB, 4 = RGBA. text:…, Parse back a GIF written by gif_bytes (or any simple GIF). Returns dict with…, contrast(), luminance(), WCAG relative luminance of an RGB(A) colour (0 black .. 1 white). (+41 more)

### Community 20 - "TeeCaptureTest"
Cohesion: 0.12
Nodes (7): java.nio.ByteBuffer, Override, TeeBufferSource, TeePool, GuiItemFlushTest, RecordingSource, TeeCaptureTest

### Community 21 - ".sanitize"
Cohesion: 0.11
Nodes (7): 5. Open work (ordered), Session 19: owner config as the built-in defaults, 2.1.0 release, Glint, GlintCategorySettings, GlintCategoryRuntime, GlintRuntimeSettings, GlintRuntimeSettingsTest

### Community 22 - "GlintSlot"
Cohesion: 0.14
Nodes (6): Studio clock, GlintShards, GlintSlot, Factory, GlintSlotPool, SuppressWarnings

### Community 23 - "GlintContext"
Cohesion: 0.14
Nodes (4): 3.1 Phases, BufferSource, GlintContext, GlintContextTest

### Community 24 - "GlintSystem"
Cohesion: 0.11
Nodes (9): 12.1 Mechanism, 5.2 Strategy, 5.4.1 Atlas-independent scale (2.0.0), 8.2 T6: glint system (`navrelis.glintsmith.render.glint`), GlintSystem, BufferSource, Preparations, GlintFailsafeTest (+1 more)

### Community 26 - "net.minecraft.client.gui.GuiGraphics"
Cohesion: 0.15
Nodes (7): net.minecraft.client.gui.components.events.GuiEventListener, net.minecraft.client.gui.GuiGraphics, net.minecraft.client.gui.narration.NarratableEntry, Override, Override, LabeledRow, Row

### Community 27 - "StyleTable"
Cohesion: 0.05
Nodes (15): 3.4.1 Proportional thickness (2.0.0), 3.4 Per-item data encoding, nativeimage, net.minecraft.client.renderer.block.model.ItemTransform, net.minecraft.client.renderer.texture.DynamicTexture, org.joml.Matrix4f, org.joml.Matrix4fc, OutlineRuntimeSettings (+7 more)

### Community 28 - "gifenc.py"
Cohesion: 0.03
Nodes (62): colour_histogram(), encode_gif_frames(), gif_bytes(), gif_compose(), lzw_decode(), lzw_encode(), median_cut(), Dependency-free image encoders (PNG, animated GIF89a) and a small GIF decoder… (+54 more)

### Community 29 - "com.mojang.blaze3d.vertex.VertexConsumer"
Cohesion: 0.25
Nodes (4): com.mojang.blaze3d.vertex.VertexConsumer, Override, Recorder, Throwing

### Community 30 - "PhaseState"
Cohesion: 0.16
Nodes (9): com.mojang.blaze3d.pipeline.RenderTarget, 3.3 Targets, depth and resize, GlState, MaskDepth, CLEAR, COPY, NONE, OutlineCompositor (+1 more)

### Community 32 - "PatternBox.java"
Cohesion: 0.20
Nodes (5): net.minecraft.network.chat.Style, net.minecraft.util.FormattedCharSequence, PatternText, Override, PatternBox

### Community 33 - ".acquire"
Cohesion: 0.15
Nodes (5): 4. Studio renders, Still scenes (7), Turntables (3, plus `logo_anim_a`), FakeType, GlintSlotPoolTest

### Community 35 - "org.spongepowered.asm.mixin.injection.Inject"
Cohesion: 0.18
Nodes (11): net.minecraft.client.DeltaTracker, net.minecraft.client.renderer.GameRenderer, net.minecraft.client.renderer.texture.TextureAtlas, org.spongepowered.asm.mixin.injection.callback.CallbackInfo, org.spongepowered.asm.mixin.injection.Inject, GameRendererGlintMixin, Preparations, TextureAtlasGlintMixin (+3 more)

### Community 36 - "FloatSlider"
Cohesion: 0.17
Nodes (10): function, java.util.function.DoubleConsumer, java.util.function.DoubleFunction, java.util.function.DoubleToIntFunction, mth, net.minecraft.client.gui.components.AbstractSliderButton, FloatSlider, Override (+2 more)

### Community 37 - "Glintsmith"
Cohesion: 0.15
Nodes (13): Building, Commands and keys, Compared with other mods and packs, Compatibility, Configuration, Credits and acknowledgements, Features, Glintsmith (+5 more)

### Community 38 - "LangFilesTest.java"
Cohesion: 0.14
Nodes (25): abstractinsnnode, classreader, enumeration, fabriclauncherbase, fieldinsnnode, imixintransformer, inputstream, inputstreamreader (+17 more)

### Community 39 - "SideElementBakeTest.java"
Cohesion: 0.11
Nodes (15): assertinstanceof, bytebufferbuilder, byteorder, facebakery, framesize, itemmodelgenerator, java.util.IdentityHashMap, linkedhashmap (+7 more)

### Community 40 - "OutlineTarget"
Cohesion: 0.10
Nodes (13): PreviewNotes, ItemFacts, Num, OutlineTarget, ARMOR, DROPPED, FIRST_PERSON, GUI (+5 more)

### Community 41 - "OutlineDepthTest"
Cohesion: 0.12
Nodes (4): OutlineDepth, Grid, OutlineDepthTest, OutlineShaderResourcesTest

### Community 42 - "Looks.java"
Cohesion: 0.18
Nodes (9): jsonarray, jsonprimitive, GapFixMode, AUTO, CUSTOM, OutlineCondition, ALL, ENCHANTED (+1 more)

### Community 43 - "assertequals"
Cohesion: 0.10
Nodes (22): assertarrayequals, assertequals, assertfalse, assertnotequals, assertnotnull, assertnull, assertsame, asserttrue (+14 more)

### Community 44 - "OutlineShaders"
Cohesion: 0.15
Nodes (9): com.mojang.blaze3d.vertex.VertexBuffer, com.mojang.blaze3d.vertex.VertexFormat, defaultvertexformat, gl20, net.minecraft.client.renderer.ShaderInstance, net.minecraft.server.packs.resources.ResourceProvider, resourcemanager, FullscreenQuad (+1 more)

### Community 45 - "GlintsmithScreen"
Cohesion: 0.10
Nodes (8): net.minecraft.client.gui.components.Button, net.minecraft.client.gui.components.tabs.Tab, net.minecraft.client.gui.navigation.ScreenRectangle, GlintsmithScreen, Override, SystemToastId, ConfigTab, Override

### Community 46 - "studio.py"
Cohesion: 0.10
Nodes (29): read_png_size(), The studio scenes (the contract), find_frame(), load(), Studio scenes: finding, loading and validating the frame contract. Python 3…, The frame dict for `ref`: a still name, a frame index (1 based) or a file stem…, A scene that is not there (kind 'missing') or not readable (kind 'error')., (K path, W path) of a frame dict. (+21 more)

### Community 47 - "ItemModelGeneratorMixin.java"
Cohesion: 0.23
Nodes (8): blockelementface, com.llamalad7.mixinextras.injector.ModifyReturnValue, direction, inventorymenu, net.minecraft.client.renderer.texture.SpriteContents, net.minecraft.client.renderer.texture.TextureAtlasSprite, ItemModelGeneratorMixin, TextureAtlasSpriteMixin

### Community 48 - "RulesTab"
Cohesion: 0.20
Nodes (4): net.minecraft.client.gui.components.Checkbox, Override, RuleRow, RulesTab

### Community 49 - "SideElementBakeTest"
Cohesion: 0.18
Nodes (5): net.minecraft.client.renderer.block.model.BakedQuad, net.minecraft.client.renderer.block.model.BlockElement, net.minecraft.client.resources.model.ModelState, org.junit.jupiter.api.AfterAll, SideElementBakeTest

### Community 51 - ".clamp"
Cohesion: 0.10
Nodes (4): GapFix, Outline, Targets, TargetThickness

### Community 53 - "list"
Cohesion: 0.19
Nodes (13): arraylist, directorystream, files, hashset, list, locale, nullable, path (+5 more)

### Community 54 - "composite.py"
Cohesion: 0.08
Nodes (35): alpha_bbox(), ascii_text(), backdrop(), _blend_plane(), blur_grid(), _blur_rows(), compose(), contact_plane() (+27 more)

### Community 55 - "PreviewPanel"
Cohesion: 0.12
Nodes (8): net.minecraft.client.gui.Font, net.minecraft.client.sounds.SoundManager, Override, PreviewPanel, PreviewSession, Source, HELD, SAMPLE

### Community 57 - "FrameWriter.java"
Cohesion: 0.15
Nodes (12): com.google.gson.Gson, com.google.gson.JsonArray, com.mojang.blaze3d.platform.NativeImage, datetimeformatter, executors, executorservice, future, gsonbuilder (+4 more)

### Community 58 - "PreviewItems.java"
Cohesion: 0.22
Nodes (10): datacomponents, enchantments, holderlookup, net.minecraft.core.RegistryAccess, net.minecraft.resources.ResourceKey, net.minecraft.world.item.enchantment.Enchantment, registries, RegistryLookup (+2 more)

### Community 59 - "ItemModelGeneratorSpanMixin.java"
Cohesion: 0.23
Nodes (7): coerce, com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation, local, net.minecraft.client.renderer.block.model.ItemModelGenerator, ItemModelGeneratorSpanMixin, SpanAccess, unique

### Community 60 - "GlintCategory"
Cohesion: 0.06
Nodes (34): 1. Decision summary, 2. Frame timeline in 1.21.1 and where Glintsmith acts, 3.5 Shaders, 3.6 Composite procedure (`OutlineCompositor.composite(PhaseState, Params)`), 3.7 Performance and allocation rules (T5), 3.8 Failsafe (T5), 3. Outline pipeline in detail (T5), 5.1 How vanilla 1.21.1 draws glint (verified) (+26 more)

### Community 61 - "GuiHelpersTest"
Cohesion: 0.15
Nodes (4): GuiHooks, Hsv, TextureNames, GuiHelpersTest

### Community 62 - "IrisCompat"
Cohesion: 0.24
Nodes (3): IrisApi, Api, IrisCompat

### Community 64 - "Handoff: Glintsmith 2.0.1 (stray-dot fixes, release) — finished"
Cohesion: 0.15
Nodes (12): 10. How to resume, 1. Goal, 2. Requirements and constraints, 3. Done (all committed and pushed; main = dev = b05998a, tag v2.0.1), 4. In progress when stopped, 5. Open work (owner chores, ordered), 6. Known problems and blockers, 7. Decisions made (+4 more)

### Community 66 - "Tri"
Cohesion: 0.33
Nodes (4): Tri, INHERIT, OFF, ON

### Community 67 - ".drawItem"
Cohesion: 0.10
Nodes (10): axis, 12.5 Panel and layout, FunctionalInterface, quaternionf, Fit, PreviewPose, BufferSource, Margin (+2 more)

### Community 68 - "GlintsmithConfig"
Cohesion: 0.09
Nodes (10): clientcommandmanager, clientcommandregistrationcallback, com.mojang.brigadier.CommandDispatcher, net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource, net.minecraft.commands.CommandBuildContext, ConfigManager, Holder, GlintsmithConfig (+2 more)

### Community 69 - "preview.py"
Cohesion: 0.23
Nodes (12): build(), check(), inline(), main(), pipeline_outputs(), Builds docs/curseforge/preview.html from description.md and checks the marker…, The image names the art pipeline builds (the "out" of every card and GIF of…, render() (+4 more)

### Community 70 - "GlintParams"
Cohesion: 0.24
Nodes (3): 5.4 Slot render types, GlintParams, GlintParamsTest

### Community 73 - "StudioScreen.java"
Cohesion: 0.08
Nodes (23): builtinregistries, compoundtag, entityrenderdispatcher, entitytype, fogrenderer, hashmap, holder, iteminput (+15 more)

### Community 75 - "Glintsmith"
Cohesion: 0.20
Nodes (9): Commands and keys, Compatibility notes, Credits and licence, Glintsmith, How it compares, Performance, Requirements, Sodium and Iris (+1 more)

### Community 76 - "net.minecraft.client.renderer.RenderType"
Cohesion: 0.14
Nodes (17): at, com.mojang.blaze3d.vertex.BufferBuilder, Glint (T6), java.util.SequencedMap, net.minecraft.client.renderer.RenderType, org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable, org.spongepowered.asm.mixin.injection.ModifyVariable, org.spongepowered.asm.mixin.Pseudo (+9 more)

### Community 77 - "Handoff: Glintsmith page images re-render with the 2.1.0 default look, then CurseForge update"
Cohesion: 0.15
Nodes (12): 10. How to resume, 1. Goal, 2. Requirements and constraints, 3. Done, 4. In progress when stopped, 5. Open work (ordered), 6. Known problems and blockers, 7. Decisions made (+4 more)

### Community 78 - "Glintsmith Studio (dev-only render mode)"
Cohesion: 0.14
Nodes (12): Black/white pairs, Camera and light, Example scenes, Files, Frame contract (output), Glint scale, Glintsmith Studio (dev-only render mode), Limits (+4 more)

### Community 79 - "synth_studio.py"
Cohesion: 0.10
Nodes (25): draw_sword(), Frame, main(), make_contract(), turntable(), make_scene(), Fill a convex polygon; stripe = (centre at y=0, slope, half width, colour)…, Draw one extruded sword centred on (cx, cy) with unit length u (supersampled… (+17 more)

### Community 80 - "common.py"
Cohesion: 0.05
Nodes (48): argparse, main(), Captures, cut(), cut_el(), load_manifest(), out_size(), picture_size() (+40 more)

### Community 82 - "Scene"
Cohesion: 0.20
Nodes (8): com.google.gson.JsonElement, Camera, Kind, ARMOR_STAND, ITEM, Scene, Shot, Subject

### Community 83 - "OutlineSystem.java"
Cohesion: 0.11
Nodes (21): clientlifecycleevents, collection, commonlifecycleevents, fabricloader, graphicsstatus, itemstack, loggerfactory, method (+13 more)

### Community 84 - "CompositeStateAccessor"
Cohesion: 0.12
Nodes (13): 3.2 Teeing (capture), EmptyTextureStateShard, org.spongepowered.asm.mixin.gen.Accessor, rendertype, RenderType.CompositeState, BufferSource, CompositeStateAccessor, CullStateShard (+5 more)

### Community 85 - "SpanMixin"
Cohesion: 0.43
Nodes (3): org.spongepowered.asm.mixin.Shadow, Override, SpanMixin

### Community 86 - "net.minecraft.resources.ResourceLocation"
Cohesion: 0.25
Nodes (6): 5.5 Texture management (`GlintTextures`), java.util.jar.JarFile, net.minecraft.resources.ResourceLocation, Override, ShaderReloadListener, Sprite

### Community 88 - "MaskTypes.java"
Cohesion: 0.18
Nodes (11): CompositeState, 4. Hook table, Outline (T5), net.minecraft.client.renderer.RenderStateShard, optional, org.spongepowered.asm.mixin.gen.Invoker, RenderStateShard.EmptyTextureStateShard, RenderType.CompositeRenderType (+3 more)

### Community 89 - "net.minecraft.client.Minecraft"
Cohesion: 0.16
Nodes (8): net.minecraft.client.Minecraft, MinecraftExitMixin, State, LOADING, MENU, RUNNING, SETTLING, StudioBootstrap

### Community 90 - "rendersystem"
Cohesion: 0.28
Nodes (7): gl11, gl13, gl14, gl30, glstatemanager, rendersystem, shaderinstance

### Community 91 - "CurseForge upload checklist: Glintsmith 2.1.0"
Cohesion: 0.12
Nodes (16): Before 2.0.0 went public, Before 2.0.1 went public, Before 2.0.2 went public, Before 2.1.0 goes public, CurseForge upload checklist: Glintsmith 2.1.0, Earlier release: Glintsmith 1.0.0, Earlier release: Glintsmith 2.0.0 (uploaded 2026-10-18, file 9235330), Earlier release: Glintsmith 2.0.1 (uploaded 2026-10-18, file 9238522) (+8 more)

### Community 92 - "GlintTextures"
Cohesion: 0.19
Nodes (3): net.minecraft.client.renderer.texture.TextureManager, FileEntry, GlintTextures

### Community 93 - "org.spongepowered.asm.mixin.Mixin"
Cohesion: 0.19
Nodes (9): net.minecraft.client.renderer.RenderBuffers, org.spongepowered.asm.mixin.injection.Redirect, org.spongepowered.asm.mixin.Mixin, RenderBuffersAccessor, GlintShardsClockMixin, GlintSystemClockMixin, OutlineSystemClockMixin, RenderStateShardClockMixin (+1 more)

### Community 94 - "GlintKind"
Cohesion: 0.09
Nodes (11): CompositeStateBuilder, GlintCategories, GlintKind, ARMOR_ENTITY_GLINT, ENTITY_GLINT, ENTITY_GLINT_DIRECT, GLINT, GLINT_TRANSLUCENT (+3 more)

### Community 96 - "Entry"
Cohesion: 0.23
Nodes (8): Mode, FOOTER, HIDDEN, SIDE, Entry, CullStateShard, LayeringStateShard, CullStateShard

### Community 98 - "CurseForge page material for Glintsmith"
Cohesion: 0.40
Nodes (4): CurseForge page material for Glintsmith, Images, Putting the description on CurseForge, Rebuilding

### Community 99 - "gradlew"
Cohesion: 0.83
Nodes (3): gradlew script, die(), warn()

### Community 105 - "PreviewNotesTest"
Cohesion: 0.20
Nodes (6): 12.3 The snapshot and the pending edits chain, 12.4 What is shown, 12.6 Limits (known and by design), 12.7 Tests and what is still open in game, 12. Live preview (2.0.0), PreviewNotesTest

### Community 106 - "PreviewLayoutTest"
Cohesion: 0.15
Nodes (6): com.terraformersmc.modmenu.api.ConfigScreenFactory, com.terraformersmc.modmenu.api.ModMenuApi, Layout, Override, ModMenuIntegration, PreviewLayoutTest

### Community 107 - "GlintsmithKeys.java"
Cohesion: 0.17
Nodes (7): clienttickevents, inputconstants, keybindinghelper, net.minecraft.client.KeyMapping, GlintsmithKeys, SystemToastId, systemtoast

### Community 109 - "OccluderBufferSource"
Cohesion: 0.21
Nodes (5): arrays, com.mojang.blaze3d.vertex.ByteBufferBuilder, object2objectsortedmaps, Override, OccluderBufferSource

### Community 110 - "StudioScreen"
Cohesion: 0.16
Nodes (5): Look, Override, Job, Prop, StudioScreen

### Community 111 - "ColorPickerScreen"
Cohesion: 0.18
Nodes (4): java.util.function.IntConsumer, ColorPickerScreen, Override, Colors

### Community 112 - "StudioBootstrap.java"
Cohesion: 0.14
Nodes (13): accessibilityonboardingscreen, difficulty, flatlevelgeneratorpreset, flatlevelgeneratorpresets, flatlevelsource, gamerules, gametype, levelsettings (+5 more)

### Community 113 - "Recorder"
Cohesion: 0.27
Nodes (3): Override, Recorder, View

### Community 114 - "gif.py"
Cohesion: 0.27
Nodes (10): build_all(), build_gif(), caption_for(), encode(), frame_files(), main(), LEGACY, no longer used by the build (the GIFs now come from the studio…, The caption of the n-th (1 based) source frame: `captions` are the looks in… (+2 more)

### Community 115 - "Glintsmith"
Cohesion: 0.18
Nodes (9): Commands and keys, Compatibility notes, Credits and licence, Glintsmith, How it compares, Performance, Requirements, Sodium and Iris (+1 more)

### Community 116 - "Requirements (Glintsmith, session 2026-10-17)"
Cohesion: 0.25
Nodes (7): Environment facts (verified 2026-10-17), Later owner additions (chat, 2026-10-17), Owner answers (2026-10-17), Owner answers (2026-10-18, 2.0.1 follow-up), Owner task (2026-10-17), Requirements (Glintsmith, session 2026-10-17), Session 12 owner additions (2026-10-18)

### Community 117 - "MaskTypes"
Cohesion: 0.18
Nodes (7): OutputStateShard, DepthTestStateShard, ShaderStateShard, TexturingStateShard, TransparencyStateShard, WriteMaskStateShard, MaskTypes

### Community 119 - ".drawSubjects"
Cohesion: 0.25
Nodes (3): java.lang.reflect.Field, java.lang.reflect.Method, PipelineBridge

### Community 120 - "OutlinePhase"
Cohesion: 0.25
Nodes (6): OutlinePhase, GUI, HAND, NONE, PREVIEW, WORLD

### Community 122 - "Changelog"
Cohesion: 0.12
Nodes (15): 1.0.0 (Fabric 1.21.1), 2.0.0 (Fabric 1.21.1) — 2026-10-18, 2.0.1 (Fabric 1.21.1) — 2026-10-18, 2.0.2 (Fabric 1.21.1) — 2026-10-18, 2.1.0 (Fabric 1.21.1) — 2026-10-18, Added, Added, Changed (+7 more)

### Community 123 - "Handoff: Glintsmith 2.1.0 (owner config as the built-in defaults, then upload)"
Cohesion: 0.17
Nodes (11): 10. How to resume, 1. Goal, 2. Requirements and constraints, 3. Done (this session, all committed and pushed unless noted), 4. In progress when stopped (UNCOMMITTED, unreviewed), 6. Known problems and blockers, 7. Decisions made, 8. Manual test checklist (+3 more)

### Community 124 - "Pattern"
Cohesion: 0.10
Nodes (9): Override, Kind, ALL, EXACT, GLOB, NAMESPACE, TAG, Pattern (+1 more)

### Community 125 - "org.joml.Vector3f"
Cohesion: 0.33
Nodes (4): 11. Model gap fix: AUTO values, Edge guard (2.0.1), net.minecraft.core.Direction, org.joml.Vector3f

### Community 126 - "pipeline.py"
Cohesion: 0.09
Nodes (34): array, DEFAULT_STYLE with `overrides` (a nested dict, same keys) merged in., style(), box_sums(), _int(), matte_files(), matte_pair(), premultiplied_planes() (+26 more)

### Community 127 - "ItemInHandRendererGlintMixin.java"
Cohesion: 0.38
Nodes (5): net.minecraft.client.player.LocalPlayer, net.minecraft.client.renderer.ItemInHandRenderer, ItemInHandRendererGlintMixin, ItemInHandRendererOutlineMixin, BufferSource

### Community 128 - "Final report: Glintsmith 2.0.0"
Cohesion: 0.17
Nodes (11): 2.0.1 release (same day, owner go), Changed files (2.0.0, sessions 13 to 15), Checks and tests run (session 15), Final report: Glintsmith 2.0.0, Key decisions (full list in `decisions.md`), Known limitations and recommendations, Manual test checklist (owner; not yet done by a human), Model usage (+3 more)

### Community 130 - "com.google.gson.JsonObject"
Cohesion: 0.36
Nodes (3): com.google.gson.JsonObject, GapFix, Looks

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
Cohesion: 0.16
Nodes (8): Compiled, Override, ColorMode, ENCHANTMENT, GRADIENT, PULSE, RAINBOW, STATIC

### Community 140 - "StudioMod"
Cohesion: 0.20
Nodes (4): net.fabricmc.api.ClientModInitializer, Override, ReloadCounter, StudioMod

## Knowledge Gaps
- **191 isolated node(s):** `AUTO`, `CUSTOM`, `ENCHANTED`, `ALL`, `NONE` (+186 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 639 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **30 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `GlintsmithConfig` connect `GlintsmithConfig` to `com.google.gson.JsonObject`, `Rule`, `.t`, `.parseHex`, `ColorStyleScreen`, `PreviewPanel.java`, `OutlineSystem`, `.toJson`, `Glintsmith`, `DefaultConfigTest`, `org.jetbrains.annotations.Nullable`, `EnchantmentColorsTab`, `.sanitize`, `GlintSystem`, `StyleTable`, `RuleMatcherTest`, `OutlineTarget`, `Looks.java`, `assertequals`, `GlintsmithScreen`, `RulesTab`, `.clamp`, `list`, `PreviewPanel`, `FrameWriter.java`, `GlintCategory`, `.drawItem`, `StudioScreen.java`, `RuleEditScreen`, `OutlineSystem.java`, `GlintTextures`, `PreviewNotesTest`, `GlintsmithKeys.java`, `StudioScreen`, `ColorPickerScreen`, `.drawSubjects`, `PendingEditsTest`?**
  _High betweenness centrality (0.113) - this node is a cross-community bridge._
- **Why does `The manifest` connect `lib.py` to `GlintPresets`, `OutlineDepthTest`, `net.minecraft.world.item.ItemStack`, `common.py`, `Canvas`?**
  _High betweenness centrality (0.075) - this node is a cross-community bridge._
- **Why does `OutlineSystem` connect `OutlineSystem` to `.onFrameBegin`, `org.spongepowered.asm.mixin.injection.Inject`, `GlintsmithConfig`, `OutlineTarget`, `StudioScreen.java`, `net.minecraft.world.item.ItemStack`, `OccluderBufferSource`, `org.jetbrains.annotations.Nullable`, `OutlineSystem.java`, `TeeCaptureTest`, `.onClientStopping`, `.drawSubjects`, `net.minecraft.resources.ResourceLocation`, `StyleTable`, `org.spongepowered.asm.mixin.Mixin`, `PhaseState`?**
  _High betweenness centrality (0.064) - this node is a cross-community bridge._
- **What connects `AUTO`, `CUSTOM`, `ENCHANTED` to the rest of the system?**
  _191 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `logo.py` be split into smaller, more focused modules?**
  _Cohesion score 0.10160427807486631 - nodes in this community are weakly interconnected._
- **Should `GlintPresets` be split into smaller, more focused modules?**
  _Cohesion score 0.05307017543859649 - nodes in this community are weakly interconnected._
- **Should `.t` be split into smaller, more focused modules?**
  _Cohesion score 0.108708357685564 - nodes in this community are weakly interconnected._
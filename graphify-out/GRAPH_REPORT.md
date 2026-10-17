# Graph Report - Glintsmith  (2026-10-18)

## Corpus Check
- 188 files · ~242,059 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 13 file(s) not represented in the graph (top: (none) 4, .fsh 3, .properties 2)

## Summary
- 2351 nodes · 7012 edges · 105 communities (81 shown, 24 thin omitted)
- Extraction: 91% EXTRACTED · 9% INFERRED · 0% AMBIGUOUS · INFERRED: 650 edges (avg confidence: 0.83)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `2b902ff1`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- net.minecraft.resources.ResourceLocation
- MaskTypes
- GlintPresetsTest
- list
- .t
- ColorPickerScreen
- OutlineTarget
- OutlineMixinApplicationTest
- OutlineSystem
- net.minecraft.client.gui.GuiGraphics
- org.spongepowered.asm.mixin.Mixin
- ConfigManager
- Glintsmith
- lib.py
- ColorStyle
- ConfigManager.java
- common.py
- GlintsmithConfig
- Canvas
- verify.py
- TeeCaptureTest
- GlintRuntimeSettingsTest
- GlintKind
- GlintContext
- GlintSystem
- org.junit.jupiter.api.Test
- OptionList
- StyleTable
- imgio.py
- com.mojang.blaze3d.vertex.VertexConsumer
- PhaseState
- ScreenBounds
- PatternBox
- .acquire
- RuleMatcherTest
- org.spongepowered.asm.mixin.injection.callback.CallbackInfo
- GradientSlider
- Glintsmith
- OutlineMixinApplicationTest.java
- MaskTypesTest.java
- GlintCategory
- OutlineDepthTest
- 3. Stills (42)
- asserttrue
- org.jetbrains.annotations.Nullable
- GlintsmithScreen
- gif.py
- SideElementBakeTest.java
- RulesTab
- SideElementBakeTest
- TeeVertexConsumer
- .clamp
- GapFix
- .window
- build.py
- assertfalse
- .capture
- Pattern
- GapFixTest
- ItemModelGeneratorSpanMixin.java
- RuleEngine
- Glint (T6)
- net.minecraft.client.renderer.RenderType
- PatternTest
- MaskBufferSource
- ConfigSubScreen
- org.junit.jupiter.api.AfterEach
- GlintSystem.java
- preview.py
- Glintsmith render architecture (T2)
- OutlineShaderResourcesTest
- LangFilesTest
- org.junit.jupiter.api.BeforeAll
- .passes
- Glintsmith
- OutlineCompositor.java
- OutlineCondition
- Compiled
- OutlineFailsafeTest.java
- Captures
- RuleEngine.java
- StyleKey
- TeeCaptureTest.java
- 3. Outline pipeline in detail (T5)
- SpanMixin
- .getBuffer
- ItemFacts
- EnchantKey
- OccluderBufferSource.java
- card
- CurseForge upload checklist: Glintsmith 1.0.0
- MixinApplicationTest
- ModMenuIntegration.java
- GlintRenderTypes
- org.joml.Vector3f
- Tri
- Kind
- CurseForge page material for Glintsmith
- gradlew
- Condition
- Plan (Glintsmith 1.0.0)
- decisions.md
- log.md

## God Nodes (most connected - your core abstractions)
1. `GlintsmithConfig` - 74 edges
2. `OutlineSystem` - 58 edges
3. `OutlineTarget` - 44 edges
4. `ColorStyle` - 39 edges
5. `Rule` - 38 edges
6. `ConfigManager` - 37 edges
7. `PhaseState` - 35 edges
8. `GlintCategory` - 35 edges
9. `Canvas` - 33 edges
10. `ConfigTest` - 31 edges

## Surprising Connections (you probably didn't know these)
- `Compatibility notes` --references--> `TeeVertexConsumer`  [INFERRED]
  docs/curseforge/description.md → src/main/java/navrelis/glintsmith/render/outline/TeeVertexConsumer.java
- `Compatibility notes` --references--> `TeeVertexConsumer`  [INFERRED]
  docs/curseforge/description_paste.md → src/main/java/navrelis/glintsmith/render/outline/TeeVertexConsumer.java
- `Compatibility` --references--> `TeeVertexConsumer`  [INFERRED]
  README.md → src/main/java/navrelis/glintsmith/render/outline/TeeVertexConsumer.java
- `5.6 Procedural presets (no third-party art)` --references--> `rgb()`  [INFERRED]
  docs/DESIGN.md → art/lib.py
- `Owner answers (2026-10-17)` --references--> `Glintsmith`  [INFERRED]
  .knowledge/requirements.md → src/main/java/navrelis/glintsmith/Glintsmith.java

## Import Cycles
- None detected.

## Communities (105 total, 24 thin omitted)

### Community 0 - "net.minecraft.resources.ResourceLocation"
Cohesion: 0.06
Nodes (18): 1.0.0 (Fabric 1.21.1), Added, Changelog, Notes, net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener, net.minecraft.client.renderer.texture.TextureManager, net.minecraft.resources.ResourceLocation, net.minecraft.server.packs.resources.ResourceManager (+10 more)

### Community 1 - "MaskTypes"
Cohesion: 0.06
Nodes (28): 3.2 Teeing (capture), EmptyTextureStateShard, java.util.SequencedMap, Mode, org.spongepowered.asm.mixin.gen.Accessor, OutputStateShard, RenderType.CompositeState, BufferSourceAccessor (+20 more)

### Community 2 - "GlintPresetsTest"
Cohesion: 0.07
Nodes (13): Glintsmith art, Licence and sources, Output files, The gallery pipeline, Theme, Verify, 10. Manual in-game test checklist, 5.6 Procedural presets (no third-party art) (+5 more)

### Community 3 - "list"
Cohesion: 0.08
Nodes (30): arraylist, collections, com.google.gson.Gson, commonlifecycleevents, confirmscreen, directorystream, files, filetime (+22 more)

### Community 4 - ".t"
Cohesion: 0.10
Nodes (10): net.minecraft.client.gui.components.CycleButton, net.minecraft.client.gui.components.Tooltip, net.minecraft.network.chat.Component, OnPress, Override, RuleEditScreen, SettingRows, Override (+2 more)

### Community 5 - "ColorPickerScreen"
Cohesion: 0.06
Nodes (8): java.util.function.IntConsumer, ColorPickerScreen, Override, DurabilityAlert, Parsed, EnchantmentColors, ColorsTest, DurabilityAndEnchantmentColorsTest

### Community 6 - "OutlineTarget"
Cohesion: 0.07
Nodes (17): OutlineContexts, OutlinePhase, GUI, HAND, NONE, WORLD, OutlineRuntimeSettings, OutlineTarget (+9 more)

### Community 7 - "OutlineMixinApplicationTest"
Cohesion: 0.10
Nodes (5): org.objectweb.asm.tree.AnnotationNode, org.objectweb.asm.tree.ClassNode, org.objectweb.asm.tree.MethodNode, GlintMixinApplicationTest, OutlineMixinApplicationTest

### Community 8 - "OutlineSystem"
Cohesion: 0.08
Nodes (6): Outline (T5), net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext, Override, OccluderBufferSource, BufferSource, OutlineSystem

### Community 9 - "net.minecraft.client.gui.GuiGraphics"
Cohesion: 0.11
Nodes (26): button, chatformatting, commoncomponents, component, consumer, createworldscreen, cyclebutton, i18n (+18 more)

### Community 10 - "org.spongepowered.asm.mixin.Mixin"
Cohesion: 0.16
Nodes (27): com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod, com.llamalad7.mixinextras.injector.wrapoperation.Operation, com.mojang.blaze3d.vertex.PoseStack, equipmentslot, net.minecraft.client.model.HumanoidModel, net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer, net.minecraft.client.renderer.entity.ItemRenderer, net.minecraft.client.renderer.entity.layers.ElytraLayer (+19 more)

### Community 11 - "ConfigManager"
Cohesion: 0.13
Nodes (3): ConfigManager, Holder, ConfigTest

### Community 12 - "Glintsmith"
Cohesion: 0.06
Nodes (24): clientcommandmanager, clientcommandregistrationcallback, clienttickevents, com.mojang.brigadier.CommandDispatcher, 8.3 T8: exact wiring, inputconstants, keybindinghelper, Environment facts (verified 2026-10-17) (+16 more)

### Community 13 - "lib.py"
Cohesion: 0.09
Nodes (42): el_bullets(), el_heading(), el_panel(), el_rule(), el_strip(), _advance(), balanced_wrap(), bay() (+34 more)

### Community 14 - "ColorStyle"
Cohesion: 0.08
Nodes (17): net.minecraft.client.gui.components.AbstractButton, net.minecraft.client.gui.narration.NarrationElementOutput, nullable, ColorStyleScreen, Override, ColorStyleButton, Override, ColorEvaluator (+9 more)

### Community 15 - "ConfigManager.java"
Cohesion: 0.08
Nodes (19): atomicmovenotsupportedexception, basicfileattributes, fabricloader, filechannel, jsonparseexception, jsonreader, jsontoken, loggerfactory (+11 more)

### Community 16 - "common.py"
Cohesion: 0.08
Nodes (33): argparse, main(), load_manifest(), out_size(), picture_size(), Shared helpers of the gallery builders: manifest loading, capture loading, the…, Size on the card of a capture element or GIF entry: `size` with `resample:…, Every capture path the manifest needs, in manifest order: [(image name,… (+25 more)

### Community 17 - "GlintsmithConfig"
Cohesion: 0.08
Nodes (12): net.minecraft.client.gui.components.tabs.Tab, net.minecraft.client.gui.navigation.ScreenRectangle, GlintsmithConfig, ConfigTab, Override, EnchantmentColorsTab, EnchantmentRow, Override (+4 more)

### Community 18 - "Canvas"
Cohesion: 0.08
Nodes (17): 4. Showcase GIF (112 frames), Canvas, An RGBA canvas on a bytearray. Coordinates outside the canvas are clipped,…, A new canvas of the region; parts outside this canvas are transparent., Nearest-neighbour upscale by the whole factor k., Box-filter downscale by the whole factor n (every output pixel is the mean of…, Area-filter resize to ow x oh: every output pixel is the exact mean of the…, Draw src at (x, y): transparent pixels are skipped, half-transparent ones… (+9 more)

### Community 19 - "verify.py"
Cohesion: 0.11
Nodes (33): contrast(), luminance(), WCAG relative luminance of an RGB(A) colour (0 black .. 1 white)., WCAG contrast ratio of two colours (1 .. 21); body text needs 4.5., build_everything(), check(), contrast_report(), darkest_speck() (+25 more)

### Community 20 - "TeeCaptureTest"
Cohesion: 0.16
Nodes (6): java.nio.ByteBuffer, Override, TeeBufferSource, TeePool, GuiItemFlushTest, TeeCaptureTest

### Community 21 - "GlintRuntimeSettingsTest"
Cohesion: 0.15
Nodes (5): Glint, GlintCategorySettings, GlintCategoryRuntime, GlintRuntimeSettings, GlintRuntimeSettingsTest

### Community 22 - "GlintKind"
Cohesion: 0.11
Nodes (11): GlintKind, ARMOR_ENTITY_GLINT, ENTITY_GLINT, ENTITY_GLINT_DIRECT, GLINT, GLINT_TRANSLUCENT, GlintShards, GlintSlot (+3 more)

### Community 23 - "GlintContext"
Cohesion: 0.17
Nodes (3): 3.1 Phases, GlintContext, GlintContextTest

### Community 24 - "GlintSystem"
Cohesion: 0.12
Nodes (6): 8.2 T6: glint system (`navrelis.glintsmith.render.glint`), GlintFailsafe, GlintSystem, BufferSource, GlintFailsafeTest, GlintSystemTest

### Community 25 - "org.junit.jupiter.api.Test"
Cohesion: 0.16
Nodes (4): org.junit.jupiter.api.Test, GlintsmithMixinPluginTest, SmokeTest, ColorEvaluatorTest

### Community 26 - "OptionList"
Cohesion: 0.12
Nodes (11): font, net.minecraft.client.gui.components.AbstractWidget, net.minecraft.client.gui.components.ContainerObjectSelectionList, net.minecraft.client.gui.components.events.GuiEventListener, net.minecraft.client.gui.narration.NarratableEntry, net.minecraft.util.FormattedCharSequence, Override, LabeledRow (+3 more)

### Community 27 - "StyleTable"
Cohesion: 0.16
Nodes (5): com.mojang.blaze3d.platform.NativeImage, net.minecraft.client.renderer.texture.DynamicTexture, StyleTable, StyleTexture, StyleTableTest

### Community 28 - "imgio.py"
Cohesion: 0.08
Nodes (27): _chunk(), colour_histogram(), encode_gif_frames(), gif_bytes(), gif_compose(), lzw_decode(), lzw_encode(), median_cut() (+19 more)

### Community 29 - "com.mojang.blaze3d.vertex.VertexConsumer"
Cohesion: 0.16
Nodes (6): com.mojang.blaze3d.vertex.VertexConsumer, Override, MaskVertexConsumer, Override, Recorder, Throwing

### Community 30 - "PhaseState"
Cohesion: 0.16
Nodes (8): 3.3 Targets, depth and resize, GlState, MaskDepth, CLEAR, COPY, NONE, OutlineCompositor, PhaseState

### Community 31 - "ScreenBounds"
Cohesion: 0.17
Nodes (5): com.mojang.blaze3d.vertex.VertexBuffer, org.joml.Matrix4f, FullscreenQuad, ScreenBounds, ScreenBoundsTest

### Community 32 - "PatternBox"
Cohesion: 0.10
Nodes (7): net.minecraft.network.chat.Style, Hsv, PatternText, TextureNames, Override, PatternBox, GuiHelpersTest

### Community 35 - "org.spongepowered.asm.mixin.injection.callback.CallbackInfo"
Cohesion: 0.18
Nodes (12): at, com.mojang.blaze3d.vertex.BufferBuilder, net.minecraft.client.DeltaTracker, net.minecraft.client.renderer.GameRenderer, org.spongepowered.asm.mixin.injection.callback.CallbackInfo, org.spongepowered.asm.mixin.injection.Inject, BufferSourceGlintMixin, MultiBufferSource.BufferSource (+4 more)

### Community 36 - "GradientSlider"
Cohesion: 0.14
Nodes (12): abstracttexture, function, java.util.function.DoubleConsumer, java.util.function.DoubleFunction, java.util.function.DoubleToIntFunction, mth, net.minecraft.client.gui.components.AbstractSliderButton, options (+4 more)

### Community 37 - "Glintsmith"
Cohesion: 0.08
Nodes (22): Commands and keys, Compatibility notes, Credits and licence, Glintsmith, How it compares, Performance, Requirements, Sodium and Iris (+14 more)

### Community 38 - "OutlineMixinApplicationTest.java"
Cohesion: 0.18
Nodes (18): abstractinsnnode, classreader, com.google.gson.JsonObject, enumeration, fabriclauncherbase, fieldinsnnode, imixintransformer, inputstream (+10 more)

### Community 39 - "MaskTypesTest.java"
Cohesion: 0.16
Nodes (14): assertnotsame, com.mojang.blaze3d.vertex.VertexFormat, CompositeState, defaultvertexformat, optional, org.spongepowered.asm.mixin.gen.Invoker, proxy, renderstateshard (+6 more)

### Community 40 - "GlintCategory"
Cohesion: 0.14
Nodes (10): 5.3 Categories, context and resolution, GlintCategories, Frame, GlintCategory, ARMOR, ENTITY, GUI, ITEM (+2 more)

### Community 42 - "3. Stills (42)"
Cohesion: 0.11
Nodes (18): 0. Rig, 1. World and rooms, 2. Glintsmith config, 3. Stills (42), 5. Checklist, Backdrop and close-ups, Enchantment shorthand, Frames and stand templates (+10 more)

### Community 43 - "asserttrue"
Cohesion: 0.20
Nodes (8): assertarrayequals, assertequals, assertnotequals, assertthrows, asserttrue, direction, hashset, matrix4f

### Community 44 - "org.jetbrains.annotations.Nullable"
Cohesion: 0.22
Nodes (6): gl20, net.minecraft.client.renderer.ShaderInstance, net.minecraft.server.packs.resources.ResourceProvider, org.jetbrains.annotations.Nullable, resourcemanager, OutlineShaders

### Community 45 - "GlintsmithScreen"
Cohesion: 0.19
Nodes (5): net.minecraft.client.gui.components.tabs.TabManager, net.minecraft.client.gui.components.tabs.TabNavigationBar, GlintsmithScreen, Override, SystemToastId

### Community 46 - "gif.py"
Cohesion: 0.15
Nodes (17): cut(), cut_el(), The picture of a capture element: crop, then the resampling the element asks…, crop is [x, y, w, h]; x and y may be "c" to centre the rectangle on the image., The crop of a capture, then either box-downscaled by `down` and scaled up by…, resolve_crop(), bar(), build_all() (+9 more)

### Community 47 - "SideElementBakeTest.java"
Cohesion: 0.13
Nodes (13): blockelementface, com.llamalad7.mixinextras.injector.ModifyReturnValue, facebakery, framesize, inventorymenu, itemmodelgenerator, java.lang.reflect.Method, net.minecraft.client.renderer.texture.SpriteContents (+5 more)

### Community 48 - "RulesTab"
Cohesion: 0.20
Nodes (4): net.minecraft.client.gui.components.Checkbox, Override, RuleRow, RulesTab

### Community 49 - "SideElementBakeTest"
Cohesion: 0.28
Nodes (3): net.minecraft.client.renderer.block.model.BakedQuad, net.minecraft.client.renderer.block.model.BlockElement, SideElementBakeTest

### Community 54 - "build.py"
Cohesion: 0.19
Nodes (13): anchor_x(), build_all(), build_card(), chip_list(), Ctx, el_capture(), el_chips(), el_text() (+5 more)

### Community 55 - "assertfalse"
Cohesion: 0.34
Nodes (6): assertfalse, assertnotnull, assertnull, assertsame, itemdisplaycontext, set

### Community 56 - ".capture"
Cohesion: 0.19
Nodes (3): IrisApi, Api, IrisCompat

### Community 59 - "ItemModelGeneratorSpanMixin.java"
Cohesion: 0.21
Nodes (8): coerce, com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation, local, net.minecraft.client.renderer.block.model.ItemModelGenerator, ItemModelGeneratorMixin, ItemModelGeneratorSpanMixin, SpanAccess, unique

### Community 60 - "RuleEngine"
Cohesion: 0.33
Nodes (4): 3.7 Performance and allocation rules (T5), 9. Risks and open questions (each with a recommendation), net.minecraft.world.item.enchantment.ItemEnchantments, RuleEngine

### Community 61 - "Glint (T6)"
Cohesion: 0.18
Nodes (10): 4. Hook table, Glint (T6), net.minecraft.client.player.LocalPlayer, net.minecraft.client.renderer.ItemInHandRenderer, net.minecraft.client.renderer.RenderBuffers, ItemInHandRendererGlintMixin, BufferSource, RenderBuffersAccessor (+2 more)

### Community 62 - "net.minecraft.client.renderer.RenderType"
Cohesion: 0.22
Nodes (5): net.minecraft.client.renderer.RenderType, org.spongepowered.asm.mixin.injection.ModifyVariable, org.spongepowered.asm.mixin.Pseudo, IrisFullyBufferedGlintMixin, Vanilla

### Community 67 - "org.junit.jupiter.api.AfterEach"
Cohesion: 0.33
Nodes (3): assertdoesnotthrow, org.junit.jupiter.api.AfterEach, org.junit.jupiter.api.BeforeEach

### Community 68 - "GlintSystem.java"
Cohesion: 0.22
Nodes (8): clientlifecycleevents, 8.1 T5: outline core (`navrelis.glintsmith.render.outline`), 8. Package and class spec, resources, and T8 hooks, itemstack, packtype, resourcemanagerhelper, OutlineLook, ResolvedStyle

### Community 69 - "preview.py"
Cohesion: 0.27
Nodes (10): build(), check(), inline(), main(), Builds docs/curseforge/preview.html from description.md and checks the marker…, render(), split_row(), html (+2 more)

### Community 70 - "Glintsmith render architecture (T2)"
Cohesion: 0.18
Nodes (10): 1. Decision summary, 2. Frame timeline in 1.21.1 and where Glintsmith acts, 5.1 How vanilla 1.21.1 draws glint (verified), 5.4 Slot render types, 5.5 Texture management (`GlintTextures`), 5.7 Edge cases T6 must handle, 5. Glint system in detail (T6), 6. Compatibility matrix and failure behaviour (+2 more)

### Community 73 - "org.junit.jupiter.api.BeforeAll"
Cohesion: 0.22
Nodes (4): bootstrap, org.junit.jupiter.api.BeforeAll, sharedconstants, GameBootstrap

### Community 74 - ".passes"
Cohesion: 0.33
Nodes (3): com.mojang.blaze3d.pipeline.RenderTarget, com.mojang.blaze3d.pipeline.TextureTarget, OutlineTargets

### Community 75 - "Glintsmith"
Cohesion: 0.20
Nodes (9): Commands and keys, Compatibility notes, Credits and licence, Glintsmith, How it compares, Performance, Requirements, Sodium and Iris (+1 more)

### Community 76 - "OutlineCompositor.java"
Cohesion: 0.24
Nodes (7): gl11, gl13, gl14, gl30, glstatemanager, rendersystem, shaderinstance

### Community 77 - "OutlineCondition"
Cohesion: 0.20
Nodes (6): Outline, Targets, OutlineCondition, ALL, ENCHANTED, NONE

### Community 79 - "OutlineFailsafeTest.java"
Cohesion: 0.22
Nodes (8): abstractappender, assumptions, atomicinteger, copyonwritearraylist, logevent, logmanager, org.apache.logging.log4j.Level, property

### Community 80 - "Captures"
Cohesion: 0.28
Nodes (4): Captures, Reads capture PNGs from one folder and keeps them decoded for the length of a…, Forget a decoded capture (frames of a GIF are read once; this keeps memory…, The capture as a Canvas, or None when the file is not there. A file that is…

### Community 81 - "RuleEngine.java"
Cohesion: 0.22
Nodes (8): builtinregistries, datacomponents, enchantment, hashmap, holder, object2intmap, objects, tagkey

### Community 82 - "StyleKey"
Cohesion: 0.44
Nodes (3): net.minecraft.world.item.Item, StyleKey, RuleEngineKeyTest

### Community 83 - "TeeCaptureTest.java"
Cohesion: 0.32
Nodes (6): assertinstanceof, bytebufferbuilder, byteorder, java.util.IdentityHashMap, meshdata, object2objectlinkedopenhashmap

### Community 84 - "3. Outline pipeline in detail (T5)"
Cohesion: 0.25
Nodes (5): 3.4 Per-item data encoding, 3.5 Shaders, 3.6 Composite procedure (`OutlineCompositor.composite(PhaseState, Params)`), 3.8 Failsafe (T5), 3. Outline pipeline in detail (T5)

### Community 85 - "SpanMixin"
Cohesion: 0.43
Nodes (3): org.spongepowered.asm.mixin.Shadow, Override, SpanMixin

### Community 89 - "OccluderBufferSource.java"
Cohesion: 0.38
Nodes (3): arrays, com.mojang.blaze3d.vertex.ByteBufferBuilder, object2objectsortedmaps

### Community 90 - "card"
Cohesion: 0.29
Nodes (6): card(), mix(), noise(), A tiny fixed generator, so the dither is the same on every machine and Python…, The framed card: gold border with corner ornaments round a cream-white panel., A colour between a and b (t = 0 gives a, 1 gives b).

### Community 91 - "CurseForge upload checklist: Glintsmith 1.0.0"
Cohesion: 0.29
Nodes (6): Before anything goes public, CurseForge upload checklist: Glintsmith 1.0.0, Relations, The file, The project page, What the owner has to decide or do by hand

### Community 93 - "ModMenuIntegration.java"
Cohesion: 0.47
Nodes (4): com.terraformersmc.modmenu.api.ConfigScreenFactory, com.terraformersmc.modmenu.api.ModMenuApi, Override, ModMenuIntegration

### Community 94 - "GlintRenderTypes"
Cohesion: 0.33
Nodes (3): CompositeStateBuilder, ShaderStateShard, GlintRenderTypes

### Community 95 - "org.joml.Vector3f"
Cohesion: 0.47
Nodes (3): 11. Model gap fix: AUTO values, net.minecraft.core.Direction, org.joml.Vector3f

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

## Knowledge Gaps
- **105 isolated node(s):** `AUTO`, `CUSTOM`, `ENCHANTED`, `ALL`, `NONE` (+100 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 377 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **24 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `The manifest` connect `Canvas` to `GlintPresetsTest`, `RuleMatcherTest`, `lib.py`, `build.py`, `.capture`?**
  _High betweenness centrality (0.080) - this node is a cross-community bridge._
- **Why does `GlintsmithConfig` connect `GlintsmithConfig` to `net.minecraft.resources.ResourceLocation`, `list`, `.t`, `ColorPickerScreen`, `OutlineTarget`, `net.minecraft.client.gui.GuiGraphics`, `ConfigManager`, `Glintsmith`, `GlintRuntimeSettingsTest`, `GlintSystem`, `RuleMatcherTest`, `GlintCategory`, `GlintsmithScreen`, `RulesTab`, `.clamp`, `assertfalse`, `RuleEngine`, `.sanitize`, `GlintSystem.java`, `OutlineCondition`, `RuleEngine.java`?**
  _High betweenness centrality (0.069) - this node is a cross-community bridge._
- **Why does `OutlineSystem` connect `OutlineSystem` to `net.minecraft.resources.ResourceLocation`, `MaskBufferSource`, `list`, `org.spongepowered.asm.mixin.injection.callback.CallbackInfo`, `OutlineTarget`, `net.minecraft.client.gui.GuiGraphics`, `org.spongepowered.asm.mixin.Mixin`, `ConfigManager`, `ConfigManager.java`, `TeeCaptureTest`, `.capture`, `PhaseState`?**
  _High betweenness centrality (0.062) - this node is a cross-community bridge._
- **What connects `AUTO`, `CUSTOM`, `ENCHANTED` to the rest of the system?**
  _105 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `net.minecraft.resources.ResourceLocation` be split into smaller, more focused modules?**
  _Cohesion score 0.055087719298245616 - nodes in this community are weakly interconnected._
- **Should `MaskTypes` be split into smaller, more focused modules?**
  _Cohesion score 0.057297297297297295 - nodes in this community are weakly interconnected._
- **Should `GlintPresetsTest` be split into smaller, more focused modules?**
  _Cohesion score 0.06846635367762129 - nodes in this community are weakly interconnected._
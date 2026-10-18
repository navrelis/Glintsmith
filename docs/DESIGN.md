# Glintsmith render architecture (T2)

Target: Minecraft 1.21.1, Fabric Loader 0.19.5 (bundles MixinExtras 0.5.5), Fabric API 0.116.17+1.21.1
(fabric-rendering-v1 5.2.1, lifecycle-events 2.6.0, resource-loader-v0 1.3.1), official Mojang mappings, Java 21.
Optional at runtime: Sodium 0.8.13, Iris 1.8.14. Hard dependencies: Fabric API only.

Everything below was checked against the decompiled 1.21.1 sources, the vanilla shader resources in the
1.21.1 client jar, the Fabric API 5.2.1 rendering sources, and the bytecode of the Iris 1.8.14 and
Sodium 0.8.13 jars. Statements I could not verify are marked **[UNVERIFIED]**.

Naming used in this document:

* `StyleTarget` means T3's enum `navrelis.glintsmith.style.RenderTarget` { FIRST_PERSON, THIRD_PERSON,
  ARMOR, DROPPED, ITEM_FRAME, GUI, OTHER }. **I recommend renaming it to `StyleTarget`**, because glint
  resolution uses it too, which makes `OutlineTarget` misleading, and because it clashes with
  `com.mojang.blaze3d.pipeline.RenderTarget`, which every render class in this design imports. If T3
  keeps the old name, read `StyleTarget` as `navrelis.glintsmith.style.RenderTarget` and always write
  Mojang's class fully qualified.
* "main source" means the vanilla `MultiBufferSource.BufferSource` instance held in the field
  `RenderBuffers.bufferSource`. The world, the first-person hand and the GUI all draw through it.

---

## 1. Decision summary

**Outline technique: screen-space silhouette mask plus an exact separable distance transform, depth-aware.**

While a qualifying item renders, every `RenderType` it requests through its `MultiBufferSource` is
duplicated ("teed") into a per-phase *mask buffer source*. The mask buffer source draws the same
vertices with a tiny mask shader. That shader samples the item's own texture, discards texels with
alpha below 0.1 (the vanilla cutout threshold) and writes the item's per-frame *style index*. At the
end of the phase (world, first-person hand, or one GUI item), the mask is drawn into an off-screen
target. In the world and the vanilla hand the mask is not tested against the scene: it records every
item pixel, visible or hidden, together with the depth of the nearest item surface. The scene depth is
copied into the mask target's depth attachment only after the mask draw. Two full-screen passes,
clipped to the bounding box of the mask vertices, then run:

1. A horizontal nearest-seed search. A mask pixel is a seed only where the item is visible there
   (the scene is not in front of the item).
2. A vertical search that finishes the exact Euclidean distance transform, looks up colour,
   thickness, softness and opacity in a 256x2 style texture, suppresses pixels where scene geometry is
   in front of the outlined item (or, over hidden item pixels, where the occluder is not clearly
   behind the item), and alpha-blends the outline onto the main target.

Why this approach:

* **Universal by construction.** Anything that reaches a `MultiBufferSource` is captured:
  `item/generated` quads, JSON/3D resource-pack models, FRAPI models on Sodium, BEWLR and Fabric
  `BuiltinItemRendererRegistry` output (shield, trident, banners, chests, mod renderers) and armor
  geometry. There are no per-item special cases and no hull caps.
* **Screen-space thickness and soft glow.** Each outlined item on screen gets its own colour,
  thickness and softness, with no re-rendering of the item and no "late replay". The thickness is in screen
  pixels, in proportion to the item's size on screen by default (3.4.1).
* **Cost scales with what is on screen.** Nothing at all happens on frames where no outlined item
  is drawn. Otherwise the cost is one mask draw plus two clipped full-screen passes per phase (and a
  scissored depth clear and depth blit in the world and the vanilla hand). Seeding only from visible
  item pixels does not add a pass.
* **No ordinal injections into `LevelRenderer.renderLevel`.** The world composite is triggered at
  run time when vanilla flushes `RenderType.waterMask()` on the main source.

Rejected alternatives:

| Alternative | Why it was rejected |
|---|---|
| Inverted hull (back-face shell) | Thickness is in world units, so it shrinks with distance. Flat generated items have no volume (needs alpha tracing, which is the prior art's mess). BEWLR topology is unknown. Z-fighting. No GUI support. |
| Reusing vanilla `OutlineBufferSource` / `entity_outline` post chain | No depth test (outlines show through walls). One fixed blurred look. Shares the glowing-entity target. No per-item thickness or softness. Runs after the level, so it cannot handle the hand or the GUI. *Borrowed idea:* texture-alpha-discarding mask render types keyed by texture. |
| Geometry tracing (prior art) | Per-shape special cases, caps, dropped slivers and per-vertex CPU cost. Not universal. |
| Stencil outlines | The main framebuffer has no stencil buffer. Thickness is world-based. |
| Jump flooding | Needs log2(R)+1 passes. Only worth it above about 48 px. The separable exact search with early exit is cheaper for R ≤ 32 and exact. |
| Brute-force disc search | (2R+1)² taps per pixel. Too slow above R=8. |

**Glint technique: substitute the five vanilla glint render types on the main source with pooled
"slot" render types whose state is read at draw time.**

* The slot types clone vanilla's state shards exactly, including `GLINT_TRANSPARENCY`, the shader
  shard, the output target and the layering. They swap only the texture shard and the texturing
  shard.
* The texturing shard sets the texture matrix (speed, scale, rotation), `ColorModulator` (tint, via
  `RenderSystem.setShaderColor`) and `GlintAlpha` (strength, via `RenderSystem.setShaderGlintAlpha`).
* Vanilla's own glint shaders are reused. `ColorModulator` already multiplies the glint texture in
  `rendertype_*glint*.fsh`, so **no custom glint shaders are needed**. Because the vanilla shader
  shards are reused, Iris can still swap in its own glint programs.
* A slot is a fixed render type. Its parameters are assigned per flush cycle and it is released when
  its batch is drawn. This gives per-item looks (including animated tints) without breaking batching
  and without allocating `RenderType`s at run time.

---

## 2. Frame timeline in 1.21.1 and where Glintsmith acts

Bracketed tags such as [O6] and [G5] refer to rows of the hook table in section 4.

```
Minecraft.runTick → GameRenderer.render(DeltaTracker, boolean)
 ├─ [O7, G6 HEAD] frame begin: frame++, timeSeconds = Util.getMillis()/1000.0, release stale slots
 ├─ GameRenderer.renderLevel(DeltaTracker)
 │   ├─ LevelRenderer.renderLevel(DeltaTracker, boolean, Camera, GameRenderer, LightTexture, Matrix4f, Matrix4f)
 │   │   ├─ [WorldRenderEvents.START] open the WORLD phase (only if world targets are on and no shader pack is active)
 │   │   ├─ clear, sky, terrain solid / cutout_mipped / cutout (replaced by Sodium; irrelevant to us)
 │   │   ├─ Fabulous: itemEntityTarget.clear + copyDepthFrom(main)
 │   │   ├─ modelViewStack.push().mul(viewRotation); RenderSystem.applyModelViewMatrix()
 │   │   ├─ entity loop (main source, or OutlineBufferSource for glowing entities):
 │   │   │     ItemEntityRenderer.render         → ItemRenderer.render(ctx GROUND)        = DROPPED
 │   │   │     ItemFrameRenderer.render          → ItemRenderer.renderStatic(ctx FIXED)   = ITEM_FRAME
 │   │   │     ItemInHandLayer.render → ItemInHandRenderer.renderItem(entity, ctx THIRD_PERSON_*) = THIRD_PERSON
 │   │   │     HumanoidArmorLayer.render → renderArmorPiece(...)                           = ARMOR
 │   │   │     ElytraLayer.render                                                          = ARMOR (chest)
 │   │   │     CustomHeadLayer → ItemInHandRenderer.renderItem(ctx HEAD)                    = OTHER
 │   │   │   [O1/O2/O3/O4 tee qualifying geometry into the WORLD mask source]
 │   │   │   [G1..G4 push glint context; G5 substitutes glint types in getBuffer]
 │   │   ├─ endLastBatch(); endBatch(entitySolid/Cutout/CutoutNoCull/SmoothCutout of BLOCKS)
 │   │   ├─ [WorldRenderEvents.AFTER_ENTITIES]  (too early for us; see section 3.6)
 │   │   ├─ block entities (campfire FIXED, lectern, mod pedestals …) → also teed
 │   │   ├─ endBatch(solid, endPortal, endGateway, solidBlockSheet, cutoutBlockSheet, bed, shulker, sign, hangingSign, chest)
 │   │   ├─ outlineBufferSource.endOutlineBatch(); entityEffect (glowing) post chain
 │   │   ├─ destroy progress, block outline, debug renderer
 │   │   ├─ endLastBatch(); endBatch(translucentCullBlockSheet ← flat world items, bannerSheet, shieldSheet,
 │   │   │                             armorEntityGlint, glint, glintTranslucent, entityGlint, entityGlintDirect)
 │   │   │     └─ [G5 TAIL endBatch(entityGlintDirect)] flush all Glintsmith glint slot types (after vanilla glint)
 │   │   ├─ endBatch(waterMask)
 │   │   │     └─ [O6 TAIL endBatch(waterMask), main source, not inside endBatch()] WORLD COMPOSITE, close phase
 │   │   ├─ crumbling; translucent terrain; tripwire; particles; clouds; weather/world border
 │   │   │     (Fabulous: translucent/particles/clouds/weather go to their targets, then transparencyChain.process())
 │   │   ├─ [WorldRenderEvents.LAST] fallback WORLD composite if the phase is still open
 │   │   ├─ modelViewStack.pop()
 │   │   └─ (Iris with a pack: HandRenderer.renderSolid / renderTranslucent inside renderLevel; pipeline.finalizeLevelRendering() at RETURN)
 │   ├─ if renderHand: RenderSystem.clear(DEPTH) → renderItemInHand(camera, partial, viewRotation)
 │   │     resetProjectionMatrix(hand FOV); modelViewStack.push().mul(viewRotation)
 │   │     ItemInHandRenderer.renderHandsWithItems(...)  [O2 wrap: HAND phase]
 │   │         renderArmWithItem → renderItem(ctx FIRST_PERSON_*) → ItemRenderer.render   (filled maps bypass ItemRenderer)
 │   │         bufferSource.endBatch()
 │   │       [O2 after original] vanilla: HAND COMPOSITE now (same matrices, before screen effects)
 │   │                           Iris:    flush the HAND mask only; composite is deferred
 │   │     ScreenEffectRenderer.renderScreenEffect (fire / underwater / suffocation overlay)
 │   └─ [O7 RETURN renderLevel] Iris only: deferred HAND COMPOSITE onto main (Iris final image is in main now)
 ├─ levelRenderer.doEntityOutline(); postEffect (spectator shaders)
 ├─ RenderSystem.clear(DEPTH); ortho GUI projection; new GuiGraphics(minecraft, main source)
 │   Gui.render → renderSlot → GuiGraphics.renderItem(...)  ─┐
 │   Screen.renderWithTooltip → slots → GuiGraphics.renderItem ─┤ [O5 wrap private renderItem(...)]
 │                                                              │   GUI phase: tee → ItemRenderer.render(ctx GUI) → this.flush()
 │                                                              │   [O1 end, outlined GUI item only, before this.flush()] endBatch of the item's source
 │                                                              │     (depth test off, then as before): draws what HUD batching held back
 │                                                              └─ [O5 after original] per-item GUI COMPOSITE, clipped to its bbox
 │   toasts; guiGraphics.flush()
 └─ [O7 RETURN render] frame end: discard leftover mask geometry; close any open phase
```

Composite locations at a glance:

| Phase | Mask depth | Composite point | Composite target |
|---|---|---|---|
| WORLD | mask LEQUAL against its own depth (cleared to far), then a copy of main depth for the passes | `endBatch(waterMask)` TAIL (before translucent terrain, particles, weather) | main |
| HAND, vanilla | same, with the main depth as it is after the hand's depth clear | after `renderHandsWithItems` returns (before screen effects) | main |
| HAND, Iris pack | mask depth cleared to far; no copy of the main depth; the hand's occluder geometry is drawn into it after the item masks (3.3) | `GameRenderer.renderLevel` RETURN | main (contains Iris's final image) |
| GUI | none (no depth test, every mask pixel is a seed) | after each `GuiGraphics.renderItem` (the item is drawn by then: by its own flush, or under HUD batching by the O1-end flush of outlined GUI items) | main |

---

## 3. Outline pipeline in detail (T5)

### 3.1 Phases

`OutlinePhase { NONE, WORLD, HAND, GUI, PREVIEW }` (PREVIEW since 2.0.0, section 12). A tee is created only while a
phase is open. Each phase has its own `PhaseState`: a mask buffer source, a style table, a bbox accumulator, a matrix
snapshot and a `hasGeometry` flag. Phases never share a buffer source, so nesting (for example Iris rendering hands
inside `renderLevel`) cannot flush another phase's geometry. Only one phase is "current" at a time; the
innermost open phase wins.

| Phase | Opened by | Closed by | Item targets accepted |
|---|---|---|---|
| WORLD | `WorldRenderEvents.START`, if any world target is enabled, no shader pack is active and the failsafe has not tripped | world composite (O6), else `WorldRenderEvents.LAST`, else frame end (discard) | THIRD_PERSON, ARMOR, DROPPED, ITEM_FRAME, OTHER. Any other display context seen in world is treated as OTHER. |
| HAND | O2 wrap of `renderHandsWithItems`, if FIRST_PERSON is enabled | end of that wrap (vanilla), or `GameRenderer.renderLevel` RETURN (Iris) | FIRST_PERSON. Both hand pairs count: FIRST_PERSON_* and THIRD_PERSON_* contexts drawn inside the phase (see below). |
| GUI | O5 wrap of private `GuiGraphics.renderItem(...)`, if GUI is enabled | end of that wrap | GUI |
| PREVIEW (2.0.0) | `OutlineSystem.beginPreview`, called by `PreviewRenderer` around one item render of the settings screen | `OutlineSystem.endPreview` | FIRST_PERSON, whatever the context (section 12.2) |

Display context to target mapping (used by T5, and separately by T6):

* FIRST_PERSON_LEFT_HAND, FIRST_PERSON_RIGHT_HAND → FIRST_PERSON
* THIRD_PERSON_LEFT_HAND, THIRD_PERSON_RIGHT_HAND → THIRD_PERSON
* GROUND → DROPPED (this also covers thrown-item sprites and mod pedestals that use GROUND)
* FIXED → ITEM_FRAME (this also covers campfires and item display entities)
* GUI → GUI
* HEAD, NONE → OTHER
* The armor and elytra hooks always produce ARMOR.

The table above is the context-only mapping. `OutlineContexts.forPhase(phase, context)` then lets the
phase decide where the context is ambiguous:

* WORLD: FIRST_PERSON and GUI contexts count as OTHER.
* HAND: the phase, not the context, decides. Everything drawn inside `renderHandsWithItems` is the local
  player's first-person view, so FIRST_PERSON_* **and** THIRD_PERSON_* hand contexts both map to
  FIRST_PERSON. Any other context (GUI, GROUND, FIXED, HEAD, NONE) is not captured in the HAND phase.
* GUI: only the GUI context is captured.

Why HAND accepts THIRD_PERSON_* contexts: first-person animation mods such as Punchy 2.8f cancel the vanilla
arm rendering and draw the held item themselves, from inside `ItemInHandRenderer.renderHandsWithItems`,
through `ItemRenderer.render` with `THIRD_PERSON_LEFT_HAND` / `THIRD_PERSON_RIGHT_HAND`, because the item is
attached to a player-model arm. Without the phase rule those items would be classified as THIRD_PERSON and
get no first-person outline (or the third-person rules). `OutlineSystem` logs one DEBUG line per session
(logger `Glintsmith/Outline`) the first time hand items are captured in the HAND phase, so this path can be
confirmed in a log.

The glint does not use the outline phase. `GlintContext` keeps a depth counter for the hand pass
(`beginHands` / `endHands` / `inHands`), set by its own hook around `renderHandsWithItems` (G8, section 4)
and reset every frame in `GlintSystem.onFrameBegin`. `GlintCategories.targetFor` (through
`GlintContext.targetOf(context, inHands)`) maps THIRD_PERSON_* contexts to FIRST_PERSON while the counter is
above zero, the same rule as the HAND phase. The glint therefore does not depend on outlines being enabled.

Not covered by design, and documented as such:

* The inventory "paper doll" (an entity rendered in the GUI pass, so no phase is open).
* First-person filled maps (they are not drawn through `ItemRenderer`).
* Items that a first-person animation mod draws without `ItemRenderer.render`. Punchy does this for its
  bedrock-geometry items, chests and boats: they get no first-person outline.
* With player-animation-lib or Better Combat in first-person mode, the held item is drawn in the world pass
  (not inside `renderHandsWithItems`) with a THIRD_PERSON context. It is outlined as a third-person item,
  governed by the THIRD_PERSON switch and rules, and under a shader pack it gets no outline.
* Geometry that a mod draws with its own Tesselator outside any `MultiBufferSource`.
* EMI/JEI panels that batch items without `GuiGraphics.renderItem`.

### 3.2 Teeing (capture)

The `ItemRenderer.render` wrapper (O1) runs this:

```
MultiBufferSource src2 = OutlineSystem.beginItem(stack, displayContext, src);   // returns src if not qualifying
try { original.call(stack, ctx, leftHand, pose, src2, light, overlay, model); }
finally { OutlineSystem.endItem(src2); }
```

`beginItem` makes these checks in order, returning `src` at the first failure:

1. The system is active and the failsafe has not tripped.
2. `stack` is not empty.
3. `src` is not already a `TeeBufferSource`. If it is, this is a nested render and inherits the
   outer tee.
4. A phase is open.
5. We are not in Iris's shadow pass.
6. The target is enabled for this phase.
7. "Only local player" is respected for THIRD_PERSON and ARMOR. The holder is taken from O2's
   `renderItem` wrap, the entity from O3/O4.
8. `RuleEngine.resolve(stack, target).outline()` is not null and has thickness > 0 and opacity > 0. The thickness
   of the look is resolved in this order, later wins: the global `outline.thickness`, the own thickness of the
   place (`outline.targetThickness`, one optional value per `OutlineTarget`; null means the place uses the global
   one), the `thickness` of a matching rule (`RuleMatcher`). Colour, softness and opacity have no per-place value.
9. `argb = RuleEngine.outlineColor(stack, look, timeSeconds)` has alpha × opacity > 0.
10. The width of this item is `settings.itemThickness(thickness, pixelsPerUnit, height)` (3.4.1), and
    `idx = phase.styles.indexFor(argb, width, softness)` is not 0.

If all pass, it returns a pooled `TeeBufferSource` bound to (delegate=`src`, phase, idx).

`TeeBufferSource extends MultiBufferSource.BufferSource`. It is a subclass so that mod renderers that
`instanceof`-check or cast to `BufferSource` keep working. The super constructor gets one shared dummy
`ByteBufferBuilder(64)` and `Object2ObjectSortedMaps.emptyMap()`. It overrides `getBuffer`,
`endBatch()`, `endBatch(RenderType)` and `endLastBatch()`. The three `endBatch*` methods forward to the
delegate if the delegate is a `BufferSource`, and otherwise do nothing. `getBuffer(rt)` works like this:

```
VertexConsumer real = delegate.getBuffer(rt);
RenderType maskType = MaskTypes.forSource(rt, phase.depthTested);  // identity-cached; null = not maskable
if (maskType == null) return real;
return pool.tee(real, pool.mask(phase.maskSource.getBuffer(maskType), idx, needsDefaultUv(rt), phase.bbox));
```

Within one item render, the tee caches (rt, real) → tee so repeated `getBuffer(rt)` calls return the
same pair. The pools are preallocated, grow on exhaustion, and are reset in `endItem`. Steady state is
zero allocations.

`TeeVertexConsumer implements VertexConsumer` writes to **real first, then mask**, and every mask write
is wrapped in `try/catch (Throwable)`. If the mask side fails, the tee stops forwarding to the mask and
trips the failsafe, so the real buffer is never left half-written by us. Methods:

* `addVertex(x,y,z)` → `real.addVertex(x,y,z)`, then `mask.addVertex(x,y,z)`.
* `setColor(r,g,b,a)`, `setUv1`, `setUv2`, `setNormal` → real only (then return this).
* `setUv(u,v)` → real, then mask.
* The 11-argument bulk `addVertex(x,y,z,color,u,v,overlay,light,nx,ny,nz)` is overridden. It calls the
  real bulk method (which keeps BufferBuilder's fast path), then `mask.addVertex(x,y,z)` and
  `mask.setUv(u,v)`.
* `putBulkData` is not overridden. The interface default calls the bulk `addVertex` above.

`MaskVertexConsumer` is modelled on vanilla `OutlineBufferSource.EntityOutlineGenerator`:

* `addVertex(x,y,z)` → `b.addVertex(x,y,z); b.setColor(idx, 0, 0, 255); if (defaultUv) b.setUv(0.5f, 0.5f); bbox.include(x,y,z)`.
* `setUv(u,v)` → `if (!defaultUv) b.setUv(u,v)`. A 1.21 BufferBuilder ignores a second write to the same
  element, so the default UV may only be written for untextured source formats.
* Everything else is a no-op that returns `this`.
* `bbox.include` projects with the phase matrix `P·MV`. The snapshot is taken from
  `RenderSystem.getProjectionMatrix()` × `getModelViewMatrix()` at the first tee creation in the
  phase, multiplied into a preallocated `Matrix4f`. It tracks NDC min/max. A vertex with w ≤ 0 makes
  the bbox full-screen.

**Mask render types** (`MaskTypes`):

Eligibility of a source `RenderType` is decided once and cached in an `IdentityHashMap`. The cache is
cleared above 4096 entries, which protects against mods that create render types every frame. A source
type is maskable only if all of these hold:

* It is a `RenderType.CompositeRenderType`.
* `!isOutline()`.
* Its mode is `QUADS` or `TRIANGLES`.
* Its format contains `VertexFormatElement.POSITION`.
* Its transparency state is not `RenderStateShard.GLINT_TRANSPARENCY`, and `toString()` does not
  contain `"glint"`. This excludes every glint, including mods' own glints and our slots.
* Its texture is known: `cutoutTexture()` of its texture shard is present, read through accessors. If
  the format has no UV0, the texture is the 1x1 white `glintsmith:outline/white` and `defaultUv=true`.
  If the format has UV0 but the texture is unknown (a custom lambda shard), the type is **not** masked,
  because a quad-shaped outline would look wrong.

Each maskable source type owns its mask types: one depth-tested and one flat (GUI and PREVIEW). They are created
lazily and never shared with another source type, even for the same texture, so two source buffers
used at the same time (for example through a `VertexMultiConsumer`) cannot interleave their vertices
in one mask buffer. A mask type is built like this:

```
RenderType.create("glintsmith_outline_mask", DefaultVertexFormat.POSITION_TEX_COLOR, mode, 1536, false, false,
  RenderType.CompositeState.builder()
    .setShaderState(new RenderStateShard.ShaderStateShard(OutlineShaders::mask))
    .setTextureState(textureShard)    // EmptyTextureStateShard: binds the texture, leaves its filter alone
    .setTransparencyState(RenderStateShard.NO_TRANSPARENCY)
    .setCullState(sourceCull)         // the source's cull state, NO_CULL if unreadable (both types, since 2.0.1)
    .setDepthTestState(depth ? RenderStateShard.LEQUAL_DEPTH_TEST : RenderStateShard.NO_DEPTH_TEST)
    .setLayeringState(depth ? sourceLayering : RenderStateShard.NO_LAYERING)
    .setWriteMaskState(depth ? RenderStateShard.COLOR_DEPTH_WRITE : RenderStateShard.COLOR_WRITE)
    .setOutputState(MASK_TARGET)      // binds the mask target
    .createCompositeState(false));
```

* **Depth-tested type** (WORLD and HAND). It does not test against the scene. It tests `LEQUAL` against
  the mask target's own depth attachment, which the compositor clears to the far plane before the mask
  draw, and it writes depth. The nearest item surface therefore wins at every pixel, and the mask holds
  all item pixels, visible or hidden by the scene. It copies the layering of its source type
  (`sourceLayering`, read through `CompositeStateAccessor`; for example the view offset of armor
  types), so that its depth equals the depth of the item's main draw. There is no polygon offset: the
  mask is not meant to pass against the scene.

  The depth-tested item mask, the flat mask and the first-person occluder repeat the source render type's
  face culling, as the depth-tested mask and the occluder repeat its layering. Generated item models contain
  one-sided thin geometry (back quad, inner and merged side quads); drawn double-sided into the mask, its
  back-facing slivers became isolated seed pixels and stray 1-px rings. If the cull state cannot be read,
  the masks fall back to double-sided drawing (`NO_CULL`). The cull state is read through `CompositeStateAccessor`
  (`outlineCullState`) by `MaskTypes.cullOf`. Verified in game (T24, Iris 1.8.14-beta.1 installed,
  shaders off and on): no stray 1-px rings around GUI items in the inventory captures, and the dashed
  line inside the GUI sword under the Iris capture is gone.
* **Flat type** (GUI and PREVIEW). `NO_DEPTH_TEST`, no layering, `COLOR_WRITE`, the source's cull state
  (`NO_CULL` only if it cannot be read).

  Until 2.0.0 the flat type was always double-sided (`NO_CULL`), on the reasoning that in orthographic GUI space
  back faces only fill pinholes inside the icon. That holds only while an item faces the camera head-on. On a
  turntable (the live preview of 2.0.0, the dev studio scenes) the back quad of a generated (flat) item model
  sticks out a few pixels past the front quad. Pixels exactly on its edge, where u or v equals the sprite
  boundary, sample the neighbouring sprite of the block atlas, because the gap fix's AUTO mode on Windows and Linux
  sets the UV shrink to 0 (section 11). When that texel is opaque, the double-sided mask got a pixel that the colour
  pass, which culls back faces, did not draw, and the outline drew a full ring around it: the stray dot next to the
  bow in the lineup GIF of 2.0.0. 2.0.1 gives the flat type the source's cull state, like the other two. Measured
  in the studio lineup scene; head-on GUI items in the inventory are unchanged, because front and back quad
  cover the same pixels there.
* The mask vertex shader uses the exact vanilla expression `ProjMat * ModelViewMat * vec4(Position, 1.0)`,
  the same invariance that vanilla glint relies on for its EQUAL depth test. The seed rule in 3.3
  depends on it.

**Mask buffer source** (one per phase): `MultiBufferSource.immediateWithBuffers(new Object2ObjectLinkedOpenHashMap<>(), new ByteBufferBuilder(256))`.

* Before the first `getBuffer(maskType)`, the type is added to the map with
  `new ByteBufferBuilder(maskType.bufferSize())`. That makes every mask type a fixed buffer, so nothing
  ever draws early through the shared buffer.
* The map is capped at 128 types per phase. A new type beyond the cap is not masked.
* The `ByteBufferBuilder`s live for the session (native memory, about 1.5 KB each initially, growing
  as needed).

### 3.3 Targets, depth and resize

`OutlineTargets` holds two `com.mojang.blaze3d.pipeline.TextureTarget`s, created lazily on first use:

* `mask = new TextureTarget(w, h, true, Minecraft.ON_OSX)`: RGBA8 colour plus a `GL_DEPTH_COMPONENT`
  float depth texture (vanilla format, so `copyDepthFrom` works).
* `edt = new TextureTarget(w, h, false, Minecraft.ON_OSX)`: RGBA8.
* Before every use, `ensureSize(main)` compares the target size with `Minecraft.getMainRenderTarget()`
  width and height and calls `resize(w, h, ON_OSX)` on mismatch. No resize hook is needed. A main
  size ≤ 0 (minimised window) skips the phase.
* Both targets are released (`destroyBuffers`) after 600 consecutive frames without use, so their
  memory only costs while outlines are on screen. At 1080p that is about 25 MB in use; at 4K about
  100 MB.

Why the mask is not tested against the scene. An earlier design drew the mask `LEQUAL` against a copy
of the scene depth, with a polygon offset so that the mask would pass against the item's own depth.
The offset also let item pixels through that lie slightly behind a nearby surface. With Punchy, a blade
drawn partly inside the first-person arm left 1 to 2 px islands of hidden item pixels in the mask.
They became seeds and drew outline rings on the sleeve. The composite had only the scene depth and no
item depth, so it could not hide the outline over the hidden part of the blade. The same flaw applied
in the world phase (an item inside a block or leaning on one). The mask now records the item's own
depth, and the two passes decide per pixel whether the item is visible.

Depth handling per phase (`OutlineCompositor.MaskDepth`):

* **WORLD** (`COPY`). In order:
  1. Bind the mask target, set the scissor box to the phase region, clear colour to 0 and depth to
     1.0 (far), depth writes on.
  2. Draw the mask: `phase.maskSource.endBatch()`. The depth-tested mask types test against the mask's
     own depth and write it, so the colour holds, per pixel, the style index and the depth of the
     nearest item surface (3.4).
  3. Blit the main depth into the mask target's depth attachment (`mask.copyDepthFrom(main)`, inside
     the scissor box; it leaves framebuffer 0 bound, so re-bind with `mask.bindWrite(false)`). From
     here on the depth attachment is the scene depth, and the mask colour is the item depth.
  4. Run the EDT pass and the composite with `UseDepth=1`.

  By the composite point, all item batches have been flushed into main depth:
  `translucentCullBlockSheet` (flat world items), the shield/banner sheets and the entity sheets. So
  the scene depth contains the item itself. Fabulous translucent block items, which live in
  `itemEntityTarget`, are not in main depth, which is fine: the scene there is farther than the item,
  so the item counts as visible.
* **HAND, vanilla** (`COPY`). The same as WORLD. The main depth was cleared before the hand and now
  contains hand plus arm depth.
* **HAND, Iris** (`CLEAR` at the first hand flush, then `NONE`). No copy (main depth is not Iris's
  depth, and it holds no hand at all). The first flush of the frame clears mask colour and depth, with
  no scissor. Under Iris, `renderHandsWithItems` runs twice per frame (solid and translucent passes),
  so the mask accumulates across both calls. The scene depth for this phase is made by the occluder
  mechanism below. The deferred composite uses `UseDepth=1`.

  **Hand occluders (shader pack).** Without a pack, the arm hides the first-person outline
  because the main depth holds the arm. Under a pack the hand goes into the pack's own targets, so
  the arm has to reach the mask depth another way. Before this mechanism every mask pixel was a seed
  and the outline drew over the arm wherever the item went behind it.
  1. `OutlineSystem.beginHands(source)` is called by the O2b wrap. Under a shader pack it returns an
     `OccluderBufferSource` (one reused instance) wrapping the hand buffer source `source`; the wrap
     passes that to `renderHandsWithItems` instead of `source`. Without a pack, or if the phase
     cannot open, it returns `source` unchanged. The wrap needs no other change: vanilla's arm, and a
     first-person animation mod's arm code injected into the wrapped body, draw into the same source.
  2. `OccluderBufferSource.getBuffer(type)` returns the real buffer, or a `TeeVertexConsumer` that
     also writes into `PhaseState.occluderSource`, for every render type that writes depth itself
     (depth test on, depth write on; `MaskTypes.occluderFor`): arm, sleeve, armor on the arm, maps
     and the held items. The occluder type (`glintsmith_outline_occluder`) is `DEPTH_WRITE` only,
     depth test `LEQUAL`, repeats the source's layering and face culling and draws the source's
     texture through the mask shader, so cutout pixels do not occlude. Style index 0 and no screen
     bounds: an occluder is never a seed and never widens the region, and occluders alone do not set
     `hasGeometry` (no composite for an arm without an outlined item). At most 64 (render type,
     buffer) pairs per pass; more are passed through without occluding.
  3. `drawMask` draws the occluders after the item masks (`occluderSource.endBatch()`). Drawn first, an
     occluder would keep the item pixels behind it out of the mask colour, and an outline on the arm
     could no longer be told from an outline next to the item. After both draws the mask colour holds
     the item depth and the depth attachment holds the nearest of item and hand: the same scene
     depth the vanilla phase gets from the main depth copy. Item pixels behind the arm are hidden
     item pixels, so the rules below apply as they do without a pack.
  4. `drawMaskOnly` stores the z terms of the projection it draws with (`PhaseState.captureProjection`),
     because Iris renders the hand with its own projection and the composite runs later at
     `renderLevel` RETURN under another one. `resolve` runs `passes` with `UseDepth=1` and these terms.
     A projection that `OutlineDepth.invertible` rejects leaves them unset, and the composite falls
     back to the vanilla near and far planes.
  5. The composite linearises depth from the projection z terms. The uniform
     `DepthProj = (m22, m32)` replaces `NearFar`: `v = (2z - 1) + m22`; the distance is `m32 / v` for
     `v < 0` and `OutlineDepth.BEYOND_FAR` (1e9) otherwise. With the vanilla terms this is the
     previous formula. Iris scales the first-person projection by 1/8 in z; its own terms keep the
     distances in blocks right, and a window depth past its scaled far end, in particular the cleared
     depth 1.0, means "nothing there".

  Rejected: reading Iris's own hand depth (it is pack-specific and needs Iris internals), and turning
  hand outlines off under a shader pack (the feature stays, with the arm now hiding it).

  Known limits: (a) with both Iris hand passes in one frame the mask depth is shared, so an arm
  drawn in pass 1 can cover an item drawn in pass 2 that is in front of it in the scene; (b) hand
  geometry drawn outside the buffer source passed to `renderHandsWithItems` (a direct draw, or a mod
  drawing its arm elsewhere) does not occlude; (c) a pack that moves or distorts the hand after the
  pass leaves the occluders where the hand was drawn, like the mask itself. Limits (a) to (c) are
  not checked in game. Verified in game (Iris 1.8.14-beta.1 + Complementary Unbound with Euphoria
  Patches): the arm hides the outline, the visible blade is fully outlined and nothing is traced
  along the sleeve.
* **GUI** (`NONE`). Scissored clear of the mask colour inside the item's bbox; the mask depth is not
  touched. Mask types use `NO_DEPTH_TEST`. The composite uses `UseDepth=0`: every mask pixel is a seed.

**Depth rules** (`UseDepth=1`; implemented in `outline_edt.fsh` and `outline_composite.fsh`, mirrored
in plain Java by `OutlineDepth` so the rules can be tested on the JVM). Notation: the scene depth is
the mask target's depth attachment after the copy; the item depth is unpacked from G, B and A of the
mask colour (24-bit window depth).

* **Visible item pixel.** The scene depth at the pixel is not smaller than the item depth minus
  `SeedEpsilon`, that is, `sceneZ >= itemZ - SeedEpsilon`. `SeedEpsilon = 32 / 0xFFFFFF` in window
  depth (32 steps of a 24-bit depth buffer). It only absorbs quantisation and rounding differences
  between the item's own draw and the mask draw. It does not grow with the depth slope, as the old
  polygon offset did.
* **Seeds.** In the EDT pass, a mask pixel is a seed only if the item is visible there. Hidden item
  pixels are no seeds. They are invisible to the distance search, as if the mask were empty there.
* **Composite, per output pixel p.** After the distance search the nearest seed s is known. `zp` and
  `zs` are the linearised scene depths at p and s, using `DepthProj = (m22, m32)` of the projection
  the depth was drawn with (vanilla near 0.05 and `GameRenderer.getDepthFar()`, or under a shader pack the
  captured hand projection, see the hand occluders above).
  Since s is a visible item pixel, `zs` is also the item's distance.
  * p is a visible item pixel: no outline (inside the silhouette).
  * p is a hidden item pixel (the mask has an item there, but the scene is in front): the item
    continues behind an occluder at p, for example a blade going into the arm or a sword behind a
    pillar. The outline is drawn only if the scene at p is clearly behind the seed:
    discard if `zp < zs + max(CoverTolerance, RelativeTolerance·zs)`. Otherwise the outline would be
    drawn on the occluder.
  * Any other p: discard if `zp < zs - max(DepthTolerance, RelativeTolerance·zs)`. This is the rule the
    pipeline always had. It removes outlines drawn onto things in front of the item: an arm in
    front of a held sword, a wall edge in front of a dropped item, a mob standing in front of an item
    frame. It keeps outlines over things behind the item: the holder's body, the floor, the sky.
* **Constants.**

  | Name | Value | Used for |
  |---|---|---|
  | `SEED_EPSILON` | 32 / 0xFFFFFF (window depth) | visible-item test, `SeedEpsilon` uniform |
  | `WORLD_TOLERANCE` | 0.15 blocks | `DepthTolerance` in the WORLD phase (dropped items next to slightly nearer floor) |
  | `HAND_TOLERANCE` | 0.03 blocks | `DepthTolerance` in the HAND phase (the hand scene is under one block deep) |
  | `COVER_TOLERANCE` | 0.15 blocks | `CoverTolerance`: outline on a hidden item pixel |
  | `RELATIVE_TOLERANCE` | 0.02 (fraction of `zs`) | added to every tolerance as `max(tolerance, 0.02·zs)` |

  `DepthTolerance` is set per phase by `OutlineCompositor.frontTolerance(phase)`.

Cost: unchanged. One mask draw plus two clipped full-screen passes per phase, plus the scissored depth
clear and the depth blit that WORLD and vanilla HAND already had. The item depth is stored in the mask
colour, so no extra target and no extra pass is needed.

Known risk: the seed rule needs the item's main draw and the mask draw to produce the same depth within
32 steps of 24-bit depth. This holds when the mask uses the same vertex expression, the same matrices
and the same layering as the source type. If a source type changes the depth in a way the mask does not
copy, its item pixels would look hidden and the item would get no outline, or only a partial one.
The visible-seed depth match was checked in game on the modded items of the Nytheria test pack (advancednetherite,
eternal_starlight, cataclysm, immersive_armors) and on the vanilla netherite sword, trident and shield, with no
missing segments. The risk remains for exotic vertex paths.

**Frame-buffer stencil caveat.** If a mod gives the main target a `DEPTH24_STENCIL8` attachment, the
depth blit into our `DEPTH_COMPONENT` target fails with `GL_INVALID_OPERATION`. Check `glGetError()`
once after the first copy each session (after draining earlier errors). On error, log once and disable
WORLD and HAND (vanilla) outlines; GUI keeps working.

### 3.4 Per-item data encoding

* **Mask target, RGBA8 colour plus a depth attachment.**
  * Colour: R = style index / 255 (0 = empty, 1..255 = style). G, B and A hold the window depth
    (`gl_FragCoord.z`) of the item surface at the pixel as a 24-bit integer, rounded to nearest and
    clamped so that 1.0 does not wrap to 0, most significant byte in G (`OutlineDepth.pack`). The mask
    shader writes it for every mask type; only the passes with `UseDepth=1` read it.
  * Depth attachment: during the mask draw it is the mask's own depth (cleared to far, nearest item
    surface wins). In WORLD and vanilla HAND it is then overwritten with a copy of the main depth, so
    that the passes read the item depth from the colour and the scene depth from the depth attachment
    (3.3). Under a shader pack (HAND) it is only cleared, and the hand occluders are then drawn into
    it (3.3). In GUI it is not used.
* **Style table** (`StyleTable`, one per phase). Open addressing over `int[]` keys
  (argb, packed thickness/softness/opacity bytes). It is reset after each composite. At most 255
  entries per composite. A new width or softness of a colour that is already in a full table reuses the nearest
  entry of that colour (`StyleTable.nearestOfColor`: the smallest summed difference of the encoded thickness and
  softness); a colour that is not in the table yet is rejected (index 0, the item gets no outline).
  * The look's thickness is a setting of 0.5 to 16 px (`OutlineSettings.MAX_THICKNESS = 16`). The width of one item
    is clamped to `[0.5, 31.875]` px and to `(MAX_RADIUS - 1) / (1 + softness)` (`StyleTable.clampThickness`).
  * Thickness definition: a look's thickness is the width of the outline band outward from the silhouette, in
    framebuffer pixels. What it means for one item depends on `proportional` (3.4.1): by default it is the width on
    the item held in first person at a window height of 1080 px, and every other item gets a width in proportion to
    its size on screen. With `proportional` off it is the same width for every item: multiplied by
    `OutlineRuntimeSettings.thicknessScale(height)`, which is `framebufferHeight / 1080` with `scaleWithResolution`
    and 1 without it, the same for WORLD, HAND, GUI and PREVIEW; the GUI scale plays no part. The composite draws full
    alpha up to `t` and a 0.5 px ramp (about `t + 0.25` px on a straight edge).
  * Encoding is a 256x2 RGBA8 `DynamicTexture` (`NativeImage(256, 2, false)`):
    * row 0 = (R, G, B, A = round(colourAlpha × opacity)),
    * row 1 = (round(thickness × 8), round(softness × 255), 0, 255).
  * Upload only the used columns: `dyn.bind(); img.upload(0, 0, 0, 0, 0, n+1, 2, false, false, false, false)`.
  * `maxRadius = max over used styles of ceil(t·(1+s) + 1)`, clamped to `MAX_RADIUS = 32`. This is the
    search radius `R` uniform. It is **dynamic**: typical looks give R = 3 to 8.

#### 3.4.1 Proportional thickness (2.0.0)

`OutlineRuntimeSettings.itemThickness(T, pixelsPerUnit, H)` decides the width of each captured item before it goes into
the style table (`OutlineSystem.capture`). `T` is the thickness of the resolved look (rule, place or global) and `H` the
height of the main render target in pixels.

* With `proportional` (default, `outline.proportional`): `max(T × pixelsPerUnit / 807.6, min(minThickness, T) × H / 1080)`.
  `minThickness` is `outline.minThickness` (default 1, range 0.5 to 8), in pixels at 1080 like `T`, so it grows with the
  window, and it is never larger than `T` itself.
* `pixelsPerUnit` (`OutlineScale.pixelsPerUnit`) is the size of the item on screen per model unit at its centre,
  `s × |row1(P).xyz| × (H / 2) / w`. `M = MV × pose × display transform` is the matrix of the item at capture time
  (`MV` the model-view matrix and `P` the projection matrix in effect; for armor the pose of the layer and no display
  transform); `s` is the longest model axis of `M`; `w` is the clip `w` of the item's origin. The origin is the centre
  of the item model. The display transform is the one `ItemRenderer.render` applies, including its swap of the trident
  and the spyglass to their flat models in the GUI, on the ground and in frames. The vertical axis of the projection
  keeps the value independent of the aspect ratio. A GUI icon is `16 × guiScale` px per unit.
* The reference 807.6 (`OutlineScale.REFERENCE_PIXELS_PER_UNIT`) is the first-person right-hand item at rest: hand
  projection 70°, depth 0.649375, display scale 0.68, `H` = 1080. So `T` is the number of pixels on the held item, and
  every other item keeps the same band relative to its size.
* No usable measure (no pose, an item centre at or behind the eye, a degenerate scale): the item counts as the
  first-person reference item and gets `T × H / 1080`.
* With `proportional` off: `T × thicknessScale(H)` (the 3.4 definition). `scaleWithResolution` only applies there,
  because proportional widths follow the window size by themselves.
* The result is quantised to 1/8 px and capped by `MAX_RADIUS` (32) in the style table, as before. Items of one look at
  different widths are separate style-table entries; when the table is full, a new width of a known colour maps to the
  nearest entry of that colour (3.4).
* The preview margin (12.4) is computed for the item fitted without margin, which is an upper bound of its size.

`PhaseState` carries the settings (`settings`) and the target height (`framebufferHeight`) that the phase was opened
with, so a phase uses one rule set from open to composite. The preview phase uses the settings of the snapshot it draws.

### 3.5 Shaders

Loading: `OutlineShaders` creates the `ShaderInstance`s **lazily on the render thread at first use
after each resource reload**, inside try/catch.

* Our `SimpleSynchronousResourceReloadListener` (`glintsmith:outline_shaders`) only marks them dirty.
  This is deliberate: `GameRenderer.reloadShaders` closes **every** entry in the global `Program`
  caches, including ours, so creating them before vanilla's reload finished would leave dead programs.
* Instances are constructed as `new ShaderInstance(provider, "glintsmith_mask", DefaultVertexFormat.POSITION_TEX_COLOR)`
  (and the same for `glintsmith_outline_edt` and `glintsmith_outline_composite` with
  `DefaultVertexFormat.POSITION`).
* `provider` is a `ResourceProvider` lambda over the current `ResourceManager`. It maps
  `minecraft:shaders/core/glintsmith_<x>` to `glintsmith:shaders/core/<x>` and passes everything else
  through. The vanilla constructor prepends the minecraft namespace, and only Fabric's impl class
  `FabricShaderProgram` gets namespace support. We do not use impl classes.
* A compile or link failure (old driver, broken resource pack) logs one line and disables outlines.
  It never crashes the game. This is the reason for not using `CoreShaderRegistrationCallback`: an
  error there propagates out of `GameRenderer.reloadShaders` as an exception and fails the reload.
* On dirty, close the old instance in try/catch (double close is harmless), then create the new one.

Files under `src/main/resources/assets/glintsmith/shaders/core/`:

`mask.json`
```json
{
  "vertex": "glintsmith_mask",
  "fragment": "glintsmith_mask",
  "samplers": [ { "name": "Sampler0" } ],
  "uniforms": [
    { "name": "ModelViewMat", "type": "matrix4x4", "count": 16, "values": [1,0,0,0, 0,1,0,0, 0,0,1,0, 0,0,0,1] },
    { "name": "ProjMat",      "type": "matrix4x4", "count": 16, "values": [1,0,0,0, 0,1,0,0, 0,0,1,0, 0,0,0,1] }
  ]
}
```

`mask.vsh`
```glsl
#version 150
in vec3 Position;
in vec2 UV0;
in vec4 Color;
uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
out vec2 texCoord0;
out vec4 vertexColor;
void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);   // identical to vanilla: depth invariance
    texCoord0 = UV0;
    vertexColor = Color;
}
```

`mask.fsh` (R = style index; G, B, A = 24-bit window depth of the item surface, most significant first)
```glsl
#version 150

uniform sampler2D Sampler0;

in vec2 texCoord0;
in vec4 vertexColor;

out vec4 fragColor;

void main() {
    // Vanilla cutout threshold: only the item's own silhouette becomes mask, visible or hidden by the scene.
    if (texture(Sampler0, texCoord0).a < 0.1) {
        discard;
    }
    // R = style index / 255, written by the mask vertex consumer. G, B, A = window depth of this item surface,
    // 24 bits, most significant first (OutlineDepth.pack). The depth test against the mask's own depth keeps the
    // nearest item surface; the passes compare it with the scene depth to tell visible from hidden item pixels.
    uint z = min(uint(clamp(gl_FragCoord.z, 0.0, 1.0) * 16777215.0 + 0.5), 16777215u); // 1.0 must not wrap to 0
    fragColor = vec4(vertexColor.r, float((z >> 16u) & 255u) / 255.0, float((z >> 8u) & 255u) / 255.0,
        float(z & 255u) / 255.0);
}
```

`fullscreen.vsh` (shared by both passes, `DefaultVertexFormat.POSITION`, NDC quad)
```glsl
#version 150
in vec3 Position;
void main() { gl_Position = vec4(Position.xy, 0.0, 1.0); }
```

`outline_edt.json`
```json
{
    "vertex": "glintsmith_fullscreen",
    "fragment": "glintsmith_outline_edt",
    "samplers": [
        { "name": "MaskSampler" },
        { "name": "DepthSampler" }
    ],
    "uniforms": [
        { "name": "Radius", "type": "int", "count": 1, "values": [ 4 ] },
        { "name": "Region", "type": "float", "count": 4, "values": [ 0.0, 0.0, 1.0, 1.0 ] },
        { "name": "UseDepth", "type": "int", "count": 1, "values": [ 0 ] },
        { "name": "SeedEpsilon", "type": "float", "count": 1, "values": [ 0.0000019 ] }
    ]
}
```

`outline_edt.fsh` (horizontal pass: nearest seed on the same row, signed; with `UseDepth`, only visible item pixels are seeds)
```glsl
#version 150

// Pass 1 of the exact separable distance transform: for every pixel the signed horizontal offset to the
// nearest mask seed on the same row, searched up to Radius pixels. With UseDepth, a mask pixel is a seed only
// where the item is visible (OutlineDepth.visible): item pixels hidden behind the scene are no seeds.

uniform sampler2D MaskSampler;
uniform sampler2D DepthSampler;
uniform int Radius;
uniform vec4 Region; // x0, y0, x1, y1 inclusive, framebuffer pixels (GL origin bottom-left)
uniform int UseDepth;
uniform float SeedEpsilon;

out vec4 fragColor;

float unpackDepth(vec3 gba) {
    ivec3 b = ivec3(gba * 255.0 + 0.5);
    return float((b.x << 16) | (b.y << 8) | b.z) / 16777215.0;
}

// Style id of the seed at q, or 0.0 if q is no seed.
float seedId(ivec2 q) {
    vec4 m = texelFetch(MaskSampler, q, 0);
    if (m.r > 0.0 && UseDepth == 1 && texelFetch(DepthSampler, q, 0).r < unpackDepth(m.gba) - SeedEpsilon) {
        return 0.0; // the scene is in front of the item here
    }
    return m.r;
}

void main() {
    ivec2 p = ivec2(gl_FragCoord.xy);
    int x0 = int(Region.x);
    int x1 = int(Region.z);
    for (int i = 0; i <= Radius; i++) {
        int xr = p.x + i;
        if (xr <= x1) {
            float id = seedId(ivec2(xr, p.y));
            if (id > 0.0) {
                fragColor = vec4((float(i) + 128.0) / 255.0, id, 0.0, 1.0);
                return;
            }
        }
        int xl = p.x - i;
        if (i > 0 && xl >= x0) {
            float id = seedId(ivec2(xl, p.y));
            if (id > 0.0) {
                fragColor = vec4((128.0 - float(i)) / 255.0, id, 0.0, 1.0);
                return;
            }
        }
    }
    // A = 0: no seed within Radius on this row.
    fragColor = vec4(0.0);
}
```

`outline_composite.json`
```json
{
    "vertex": "glintsmith_fullscreen",
    "fragment": "glintsmith_outline_composite",
    "samplers": [
        { "name": "MaskSampler" },
        { "name": "EdtSampler" },
        { "name": "DepthSampler" },
        { "name": "StyleSampler" }
    ],
    "uniforms": [
        { "name": "Radius", "type": "int", "count": 1, "values": [ 4 ] },
        { "name": "Region", "type": "float", "count": 4, "values": [ 0.0, 0.0, 1.0, 1.0 ] },
        { "name": "UseDepth", "type": "int", "count": 1, "values": [ 0 ] },
        { "name": "DepthProj", "type": "float", "count": 2, "values": [ -1.0000977, -0.1000049 ] },
        { "name": "DepthTolerance", "type": "float", "count": 1, "values": [ 0.15 ] },
        { "name": "CoverTolerance", "type": "float", "count": 1, "values": [ 0.15 ] },
        { "name": "SeedEpsilon", "type": "float", "count": 1, "values": [ 0.0000019 ] },
        { "name": "DistanceFade", "type": "float", "count": 2, "values": [ 0.0, 0.0 ] }
    ]
}
```

`outline_composite.fsh` (vertical pass: exact EDT, style lookup, depth rules, output)
```glsl
#version 150

// Pass 2: finishes the exact Euclidean distance transform vertically, looks up the style of the nearest
// seed, hides the outline where scene geometry is in front of the item and blends it onto the target.
// The mask holds every item pixel, visible or hidden, with the item's own depth in G, B, A; DepthSampler is the
// scene depth. The depth rules are mirrored in OutlineDepth (Java) and tested there.

uniform sampler2D MaskSampler;
uniform sampler2D EdtSampler;
uniform sampler2D DepthSampler;
uniform sampler2D StyleSampler;
uniform int Radius;
uniform vec4 Region;
uniform int UseDepth;
uniform vec2 DepthProj; // z terms (m22, m32) of the projection the depth was drawn with
uniform float DepthTolerance; // front tolerance in blocks (per phase)
uniform float CoverTolerance; // tolerance in blocks on hidden item pixels
uniform float SeedEpsilon; // window depth
uniform vec2 DistanceFade; // (start, end) in blocks; end <= start disables

out vec4 fragColor;

// Distance in blocks (OutlineDepth.linear). Iris scales the hand projection in z; past its far end lies nothing.
float linearDepth(float z) {
    float v = (2.0 * z - 1.0) + DepthProj.x;
    return v < 0.0 ? DepthProj.y / v : 1.0e9;
}

float unpackDepth(vec3 gba) {
    ivec3 b = ivec3(gba * 255.0 + 0.5);
    return float((b.x << 16) | (b.y << 8) | b.z) / 16777215.0;
}

void main() {
    ivec2 p = ivec2(gl_FragCoord.xy);
    vec4 m = texelFetch(MaskSampler, p, 0);
    bool onHiddenItem = false;
    if (m.r > 0.0) {
        if (UseDepth != 1 || texelFetch(DepthSampler, p, 0).r >= unpackDepth(m.gba) - SeedEpsilon) {
            discard; // inside the visible silhouette: no outline
        }
        onHiddenItem = true; // the item continues behind an occluder here
    }
    float best = 1.0e9;
    float bestId = 0.0;
    ivec2 seed = p;
    int y0 = int(Region.y);
    int y1 = int(Region.w);
    for (int j = 0; j <= Radius; j++) {
        if (float(j * j) >= best) {
            break; // no closer seed possible
        }
        for (int s = -1; s <= 1; s += 2) {
            if (j == 0 && s > 0) {
                continue;
            }
            int y = p.y + s * j;
            if (y < y0 || y > y1) {
                continue; // outside the region the data is stale
            }
            vec4 h = texelFetch(EdtSampler, ivec2(p.x, y), 0);
            if (h.a < 0.5) {
                continue;
            }
            float dx = h.r * 255.0 - 128.0;
            float d2 = dx * dx + float(j * j);
            if (d2 < best) {
                best = d2;
                bestId = h.g;
                seed = ivec2(p.x + int(round(dx)), y);
            }
        }
    }
    if (bestId <= 0.0) {
        discard;
    }
    int idx = int(bestId * 255.0 + 0.5);
    vec4 col = texelFetch(StyleSampler, ivec2(idx, 0), 0);
    vec4 par = texelFetch(StyleSampler, ivec2(idx, 1), 0);
    float thickness = par.r * 255.0 / 8.0;
    float softness = par.g;
    float d = sqrt(best) - 0.5; // distance to the edge of the seed pixel
    float inner = thickness * (1.0 - softness);
    float outer = thickness * (1.0 + softness) + 0.5;
    float a = (1.0 - smoothstep(inner, outer, d)) * col.a;
    if (UseDepth == 1) {
        float zp = linearDepth(texelFetch(DepthSampler, p, 0).r);
        float zs = linearDepth(texelFetch(DepthSampler, seed, 0).r);
        // zs is the item's depth: seeds are visible item pixels. On a hidden item pixel the occluder must be clearly
        // behind the seed, elsewhere it must not be clearly in front of it.
        float relative = zs * 0.02;
        if (onHiddenItem ? zp < zs + max(CoverTolerance, relative) : zp < zs - max(DepthTolerance, relative)) {
            discard; // something is in front of the item here
        }
        if (DistanceFade.y > DistanceFade.x) {
            a *= 1.0 - smoothstep(DistanceFade.x, DistanceFade.y, zs);
        }
    }
    if (a <= 0.003) {
        discard;
    }
    fragColor = vec4(col.rgb, a);
}
```

Correctness of the EDT: pass 1 stores, for every pixel, the signed offset to the nearest seed on its
row within R. For any pixel, the true nearest seed lies on some row y+dy. On that row the nearest seed
is at most as far horizontally, so min over dy of (dx(y+dy)² + dy²) is the exact squared Euclidean
distance. The early exit is valid because dy² alone already exceeds the best value.

### 3.6 Composite procedure (`OutlineCompositor.composite(PhaseState, Params)`)

All of this runs on the render thread inside try/catch. On any exception: trip the failsafe, restore
state, return.

1. If `!phase.hasGeometry`, return. If the shaders are unavailable, discard the mask geometry: call
   `maskSource.endBatch()` while the mask target is bound, then return.
2. `main = Minecraft.getMainRenderTarget()`; `targets.ensureSize(main)`. Save
   `GL_DRAW_FRAMEBUFFER_BINDING` and the viewport with `GL11.glGetIntegerv`. Under Iris (and in general)
   the bound framebuffer is not necessarily `main`.
3. Region: NDC bbox → pixels, grown by `R + 2`, clamped to the target. Use full-screen if the bbox
   is invalid.
4. **Mask.** Bind `mask` for write (`bindWrite(false)`), set the scissor box to the region (not in the
   Iris hand flush) and clear according to the phase (section 3.3: colour and depth for WORLD,
   vanilla HAND and the first Iris hand flush; colour only for GUI). Then `phase.maskSource.endBatch()`.
   This draws every mask type using the *current* `RenderSystem` projection and model-view, which are
   the same matrices vanilla just used. That is why each composite point sits exactly where vanilla
   flushed the corresponding geometry. Only after that draw, for WORLD and vanilla HAND, blit the main
   depth into the mask depth attachment (`copyDepthFrom`) and re-bind. The order is: clear colour and
   depth, mask draw, depth copy, then the passes.
5. **Pass H.** Bind `edt` for write. `RenderSystem.enableScissor(region)`. Disable depth test; set
   `depthMask(false)`; disable blend. Set uniforms `Radius`, `Region`, `UseDepth` and `SeedEpsilon`, and
   `setSampler("MaskSampler", mask.getColorTextureId())` and `setSampler("DepthSampler", mask depth texture)`.
   Draw with `FullscreenQuad.draw(edtShader)`, a static `VertexBuffer(Usage.STATIC)` holding a
   POSITION quad (-1,-1)(1,-1)(1,1)(-1,1), drawn with `drawWithShader(IDENTITY, IDENTITY, shader)`.
6. **Pass V + composite.** Bind `main` for write (`bindWrite(false)`; the same size, so the viewport is
   unchanged). Keep the scissor. Upload the style table. Set samplers:
   * `MaskSampler` = mask colour
   * `EdtSampler` = edt colour
   * `DepthSampler` = `Integer.valueOf(mask.getDepthTextureId())` (the scene depth after the copy)
   * `StyleSampler` = style `DynamicTexture`

   Set uniforms `UseDepth`, `DepthProj`, `DepthTolerance` (`frontTolerance` of the phase: 0.15 blocks in
   WORLD, 0.03 in HAND), `CoverTolerance` (0.15), `SeedEpsilon` and `DistanceFade`. Enable blend with
   `blendFuncSeparate(SRC_ALPHA, ONE_MINUS_SRC_ALPHA, ZERO, ONE)`, which keeps the main target's alpha.
   Draw.
7. **Restore.** Disable scissor; disable blend; `defaultBlendFunc()`; `depthMask(true)`;
   `enableDepthTest()`; `depthFunc(515)`. Re-bind the saved framebuffer with
   `GlStateManager._glBindFramebuffer(GL_FRAMEBUFFER, saved)` and restore the viewport.
   `ShaderInstance.clear()` already unbinds sampler units.
8. Reset `phase.styles`, `phase.bbox`, `phase.hasGeometry` and the matrix snapshot.

Why the world composite sits at `endBatch(waterMask)`:

* At AFTER_ENTITIES (the `"blockentities"` profiler constant), block entities and the item sheets
  `translucentCullBlockSheet`, `shieldSheet` and `bannerSheet` are still unflushed. Those flushes
  would later overwrite the outline behind them, because the outline writes no depth.
* LAST and END come after translucent terrain, particles and weather, so outlines would sit on top of
  rain. END has also already popped the view model-view matrix.
* `waterMask` is the last fixed flush before translucent terrain, in both the Fabulous and the
  non-Fabulous branch.

O6 also tracks `inFullFlush` (HEAD/TAIL of `BufferSource.endBatch()`). The no-argument `endBatch()`
loops over all fixed types, including `waterMask`. Without the flag, a mod that flushes everything in
the middle of the entity loop would trigger an early composite.

### 3.7 Performance and allocation rules (T5)

* Zero cost when nothing is outlined:
  * No `TeeBufferSource` is created, so `hasGeometry` stays false and no GPU work is issued.
  * Targets and shaders are created lazily.
  * When outlines are globally disabled, the O1 wrapper calls `original.call` immediately.
* Hot paths (tee, mask consumer, `getBuffer`) allocate nothing in steady state: pooled consumers,
  preallocated matrices and identity caches. There is no reflection anywhere. Mixin accessors are
  compiled access, and the Iris API instance is cached once.
* `RuleEngine.resolve` and `outlineColor` are called once per item render. T3 must keep the no-outline
  path allocation-free (see section 9).
* `IrisApi.getInstance().isShaderPackInUse()` is read once per frame in `onFrameBegin`.
  `isRenderingShadowPass()` is read only when a phase is open and an item qualifies.

### 3.8 Failsafe (T5)

`OutlineFailsafe.trip(String where, Throwable t)`:

* The first trip logs exactly one line at ERROR level with the stack trace:
  `"[Glintsmith] Outline rendering disabled for this session after an error in {where}"`.
* It sets a volatile `tripped = true`, which all entry points check first.
* It discards the phase buffer sources. They may hold a half-written vertex, so they are never used
  again; their `ByteBufferBuilder`s are closed at shutdown (`ClientLifecycleEvents.CLIENT_STOPPING`).

Rules for every entry point:

* Every mixin handler body that runs our code is wrapped in try/catch(Throwable) → trip, then fall
  through to vanilla behaviour.
* Exceptions thrown by `original.call(...)` (vanilla or other mods) are **rethrown unchanged**. We do
  not hide other mods' bugs. Our own code inside the tee cannot throw, because of the per-call catch
  described in section 3.2.

---

## 4. Hook table

Every injector carries `require = 0`. If another mod removes or overwrites the target, the injector
silently does not apply and the "fallback" column describes the effect. `@WrapMethod` wraps whatever
body ends up in the method, including other mods' `@Overwrite` or injected code, so it survives most
conflicts. Accessor and invoker mixins target pinned vanilla members, so they cannot be "removed".

Recommendation for T8: set `"defaultRequire": 0` in `glintsmith.mixins.json`, so a forgotten `require`
attribute cannot crash the game.

All mixins live in package `navrelis.glintsmith.mixin` (sub-packages `outline`, `glint`, `compat`).

### Outline (T5)

| ID | Mixin class | Target | Method (Mojmap) + descriptor | Injection | Purpose | Fallback if missing |
|---|---|---|---|---|---|---|
| O1 | `outline.ItemRendererOutlineMixin` | `net.minecraft.client.renderer.entity.ItemRenderer` | `render(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IILnet/minecraft/client/resources/model/BakedModel;)V` | `@WrapMethod` | Replace the source argument with the tee (`beginItem` / `endItem`). At the end of an outlined GUI item, flush the source it was handed (HUD batching, see section 6) | No outlines for held, dropped, framed or GUI items. Armor still works. A diagnostic logs once if outlines are enabled but O1 never fired after 600 in-world frames. |
| O2a | `outline.ItemInHandRendererOutlineMixin` | `net.minecraft.client.renderer.ItemInHandRenderer` | `renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V` | `@WrapMethod` | Push/pop the holder entity (for "only local player") | The holder is unknown, so "only local player" treats every holder as non-local and THIRD_PERSON outlines are suppressed when the option is on |
| O2b | same class | same | `renderHandsWithItems(FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lnet/minecraft/client/player/LocalPlayer;I)V` | `@WrapMethod` | Open the HAND phase; after `original.call`, flush the mask and composite (vanilla) or flush only (Iris) | No first-person outlines |
| O3 | `outline.HumanoidArmorLayerOutlineMixin` | `net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer` | `renderArmorPiece(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;ILnet/minecraft/client/model/HumanoidModel;)V` (private) | `@WrapMethod` | Tee the armor geometry with style `resolve(entity.getItemBySlot(slot), ARMOR)`. Wraps Fabric's `ArmorRenderer` HEAD cancellation too, so mod armor renderers get the tee. | No armor outlines |
| O4 | `outline.ElytraLayerOutlineMixin` | `net.minecraft.client.renderer.entity.layers.ElytraLayer` | `render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V` (the LivingEntity overload, not the bridge) | `@WrapMethod` | Tee the elytra with style of the CHEST item, target ARMOR | No elytra outlines |
| O5 | `outline.GuiGraphicsOutlineMixin` | `net.minecraft.client.gui.GuiGraphics` | `renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;IIII)V` (private, the 7-argument one; every public `renderItem` / `renderFakeItem` goes through it) | `@WrapMethod` | Open the GUI phase; after `original.call` (the item is drawn by then, see O1), composite clipped to the bbox | No GUI outlines |
| O6 | `outline.BufferSourceOutlineMixin` | `net.minecraft.client.renderer.MultiBufferSource$BufferSource` | `endBatch()V` HEAD and TAIL; `endBatch(Lnet/minecraft/client/renderer/RenderType;)V` TAIL | `@Inject` | Track `inFullFlush`. Trigger the WORLD composite when `this == mainSource && type == RenderType.waterMask() && !inFullFlush && phase == WORLD`. | WORLD composite happens at `WorldRenderEvents.LAST` (outlines then sit over water and particles) |
| O7a | `outline.GameRendererOutlineMixin` | `net.minecraft.client.renderer.GameRenderer` | `render(Lnet/minecraft/client/DeltaTracker;Z)V` HEAD | `@Inject` | `onFrameBegin` (time, Iris state, idle counters) | Time is taken lazily per call; no other effect |
| O7b | same | same | `render(Lnet/minecraft/client/DeltaTracker;Z)V` RETURN | `@Inject` | `onFrameEnd`: discard leftover mask geometry, close stray phases | Leftovers are discarded at the next phase open instead |
| O7c | same | same | `renderLevel(Lnet/minecraft/client/DeltaTracker;)V` RETURN | `@Inject` | Iris only: deferred HAND composite | No hand outlines under a shader pack |
| A1 | `outline.CompositeRenderTypeAccessor` | `net.minecraft.client.renderer.RenderType$CompositeRenderType` | `state()` | `@Invoker("state")` | Read the composite state | — |
| A2 | `outline.CompositeStateAccessor` | `net.minecraft.client.renderer.RenderType$CompositeState` | fields `textureState`, `transparencyState`, `texturingState`, `writeMaskState`, `layeringState` | `@Accessor` | Texture shard, glint detection, invisible depth-only geometry, and the layering the depth-tested mask type copies | — |
| A3 | `outline.EmptyTextureStateShardAccessor` | `net.minecraft.client.renderer.RenderStateShard$EmptyTextureStateShard` | `cutoutTexture()Ljava/util/Optional;` | `@Invoker("cutoutTexture")` | Texture for the mask | — |
| E1 | Fabric event | `WorldRenderEvents.START` | — | event | Open the WORLD phase (gated) | — |
| E2 | Fabric event | `WorldRenderEvents.LAST` | — | event | Fallback WORLD composite (the model-view is still pushed at LAST) | — |
| E3 | Fabric event | `ClientLifecycleEvents.CLIENT_STARTED` / `CLIENT_STOPPING` | — | event | Capture the main source, create the white texture, release native buffers | — |
| E4 | Fabric resource loader | `ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(SimpleSynchronousResourceReloadListener id glintsmith:outline_shaders)` | — | listener | Mark shaders dirty | — |

### Glint (T6)

| ID | Mixin class | Target | Method + descriptor | Injection | Purpose | Fallback if missing |
|---|---|---|---|---|---|---|
| G1 | `glint.ItemRendererGlintMixin` | `ItemRenderer` | `render(...)` (same descriptor as O1) | `@WrapMethod` | `GlintContext.pushItem(stack, ctx)`; `pop()` in `finally` | No GUI category (GUI glints use ITEM settings); no per-rule looks for items |
| G2 | `glint.HumanoidArmorLayerGlintMixin` | `HumanoidArmorLayer` | `renderArmorPiece(...)` (same as O3) | `@WrapMethod` | `pushArmor(entity.getItemBySlot(slot))` | Armor uses ARMOR category defaults (the `armorEntityGlint` kind already maps to ARMOR), but no per-rule looks |
| G3 | `glint.ElytraLayerGlintMixin` | `ElytraLayer` | `render(...)` (same as O4) | `@WrapMethod` | `pushArmor(entity.getItemBySlot(EquipmentSlot.CHEST))` | Same as G2 |
| G4 | `glint.BlockEntityWithoutLevelRendererGlintMixin` | `net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer` | `renderByItem(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V` | `@WrapMethod` | `pushBewlr(stack, ctx)`. Fabric's builtin item renderers run inside, because Fabric injects at HEAD of this method. | Shield, trident and BEWLR glints use the ITEM category |
| G5a | `glint.BufferSourceGlintMixin` | `MultiBufferSource$BufferSource` | `getBuffer(Lnet/minecraft/client/renderer/RenderType;)Lcom/mojang/blaze3d/vertex/VertexConsumer;` | `@ModifyVariable(at = @At("HEAD"), argsOnly = true)` | Substitute vanilla glint types with slot or off types (main source only); a request of ImmediatelyFast's non-batching GUI source (G11) goes to `substituteGui` instead | Vanilla glint everywhere; the feature is effectively off |
| G5b | same | same | `endBatch(Lnet/minecraft/client/renderer/RenderType;)V` TAIL | `@Inject` | When `this == main && type == vanilla entityGlintDirect`, flush every registered slot and off type | Slot glints are flushed by the later `endBatch()`. In non-Fabulous mode that is after water, so items seen through water lose their glint. |
| G5c | same | same | `endBatch(Lnet/minecraft/client/renderer/RenderType;Lcom/mojang/blaze3d/vertex/BufferBuilder;)V` (private) TAIL | `@Inject` | `GlintSystem.onBatchDrawn(this, type)` releases the slot; for a subclassed main source (ImmediatelyFast, G9) it also does the G5b flush after the vanilla `entityGlintDirect` | Slots are released only at frame start, so at most 32 distinct looks per kind per frame |
| G6 | `glint.GameRendererGlintMixin` | `GameRenderer` | `render(Lnet/minecraft/client/DeltaTracker;Z)V` HEAD | `@Inject` | `GlintSystem.onFrameBegin()` | Frame counter is not advanced, so stale slots are never auto-released; `onBatchDrawn` still releases them, and G7 stays off (its shader-pack flag starts as "pack in use" and is only refreshed here), so with Iris the world keeps the vanilla glint |
| GA1 | `glint.BufferSourceAccessor` | `MultiBufferSource$BufferSource` | field `fixedBuffers` | `@Accessor` | Register slot types as fixed buffers | — |
| GA2 | `glint.RenderBuffersAccessor` | `net.minecraft.client.renderer.RenderBuffers` | field `bufferSource` | `@Accessor` | Get the vanilla main source without going through Iris's `bufferSource()` replacement | — |
| G8 | `glint.ItemInHandRendererGlintMixin` | `net.minecraft.client.renderer.ItemInHandRenderer` | `renderHandsWithItems(...)` (same descriptor as O2b) | `@WrapMethod`, `require = 0` | `GlintContext.beginHands()`; `endHands()` in `finally`. Held items drawn inside count as FIRST_PERSON for rules, also with a THIRD_PERSON hand context (first-person animation mods). Independent of the outline hook O2b. | Items drawn by a first-person animation mod use the THIRD_PERSON rules; vanilla hands are unaffected |
| G9 | `glint.compat.ImmediatelyFastBufferSourceGlintMixin` | `@Pseudo @Mixin(targets = "net.raphimc.immediatelyfast.feature.core.BatchableBufferSource", remap = false)` | `getBuffer` by bare name (verified in the ImmediatelyFast 1.6.15+1.21.1 jar: `public class_4588 getBuffer(class_1921)`, the only `getBuffer` of the class; same naming argument as G7) | `@ModifyVariable(HEAD, argsOnly, require = 0, remap = false)` | `GlintSystem.substitute(this, requested)`: G5a for ImmediatelyFast's main source (5.2, point 7). The G5b flush for it runs in G5c. Not verified in game yet. | With ImmediatelyFast installed, glints of the main source (first-person hand; the world without Iris) keep the vanilla look |
| G10 | `glint.TextureAtlasGlintMixin` | `net.minecraft.client.renderer.texture.TextureAtlas` | `upload(Lnet/minecraft/client/renderer/texture/SpriteLoader$Preparations;)V` TAIL | `@Inject` | `GlintSystem.onAtlasUploaded(location(), preparations)`: for the block atlas, stores the atlas size and the size of the reference sprite for the atlas factor (5.4.1). Runs after every stitch (every resource reload). | The atlas factor stays 1: item glints are atlas dependent as in 1.0.0 |
| G11 | `glint.compat.ImmediatelyFastGuiSourceGlintMixin` | `@Pseudo @Mixin(targets = "net.raphimc.immediatelyfast.feature.batching.BatchingBuffers", remap = false)` | `getNonBatchingEntityVertexConsumers` by bare name, RETURN (verified in the ImmediatelyFast 1.6.15+1.21.1 jar: `public static class_4597$class_4598 getNonBatchingEntityVertexConsumers()`, the only method of that name) | static `@Inject(RETURN, require = 0, remap = false)` | `GlintSystem.onGuiSource(returned)`: remembers ImmediatelyFast's non-batching GUI source. G5a then hands that source's requests to `substituteGui` (5.2, point 8). Not verified in game yet. | With ImmediatelyFast, GUI glints outside batched HUD layers (inventories, menus) keep the vanilla look |
| G12 | `glint.compat.ImmediatelyFastHudGlintMixin` | `@Pseudo @Mixin(targets = "net.raphimc.immediatelyfast.feature.batching.HudBatchingBufferSource", remap = false)` | `getBuffer` by bare name (verified in the same jar: `public class_4588 getBuffer(class_1921)`, the only `getBuffer` of the class; same naming argument as G7) | `@ModifyVariable(HEAD, argsOnly, require = 0, remap = false)` | `GlintSystem.substituteGui(this, requested)` for the HUD batch (hotbar, offhand slot; also ImmediatelyFast's experimental screen batching), 5.2, point 8. Not verified in game yet. | With ImmediatelyFast HUD batching, HUD item glints keep the vanilla look |
| G7 | `glint.compat.IrisFullyBufferedGlintMixin` | `@Pseudo @Mixin(targets = "net.irisshaders.batchedentityrendering.impl.FullyBufferedMultiBufferSource", remap = false)` | `getBuffer` by bare name. Verified in the shipped Iris 1.8.14-beta.1 jar: `public class_4588 getBuffer(class_1921)`, the only `getBuffer` of the class; vanilla does not obfuscate `MultiBufferSource.getBuffer` (no intermediary entry), so the name is the same in dev and production while the descriptor is not. | `@ModifyVariable(HEAD, argsOnly, require = 0, remap = false)` | `GlintSystem.substituteBuffered`: the G5a swap for Iris's world buffer source, with pinned slots (5.2, point 6). Off while a shader pack is in use. Verified in game (Iris 1.8.14-beta.1, shaders off): preset glints on armor stands and on the showcase stand show their own colours; under Complementary Unbound the world glint stays vanilla. | With Iris installed (shaders on or off), world glints (armor, entity-held items, item frames, dropped items, armor stands) keep the vanilla look; hand and GUI unaffected |

Fabric events for T6: `ClientLifecycleEvents.CLIENT_STARTED` (capture the main source, generate the
presets) and the reload listener `glintsmith:glint_textures`.

---

## 5. Glint system in detail (T6)

### 5.1 How vanilla 1.21.1 draws glint (verified)

* **`ItemRenderer.render`**, for non-custom models:
  * `renderType = ItemBlockRenderTypes.getRenderType(stack, bl3)`. Non-block items give
    `Sheets.translucentCullBlockSheet()` in world, GUI and first person.
  * Compass and clock with foil → `getCompassFoilBuffer` (uses `RenderType.glint()` through a
    `SheetedDecalTextureGenerator`).
  * `bl3` → `getFoilBufferDirect(src, rt, true, foil)` → `glint()`.
  * Otherwise → `getFoilBuffer(src, rt, true, foil)` → `glintTranslucent()` if
    `Minecraft.useShaderTransparency() && rt == Sheets.translucentItemSheet()`, else `glint()`.
  * Custom renderers (BEWLR) call `getFoilBufferDirect(..., true, ...)` → `glint()`. This applies to
    the shield and trident in `BlockEntityWithoutLevelRenderer`, and to `Material.buffer`.
* **`ThrownTridentRenderer`** → `getFoilBufferDirect(..., false, ...)` → `entityGlintDirect()`.
* **`HumanoidArmorLayer.renderGlint`** → `src.getBuffer(RenderType.armorEntityGlint())`.
  **`ElytraLayer`** → `getArmorFoilBuffer` → `armorEntityGlint()`.
* **Render types.** All five are `POSITION_TEX` QUADS with: `COLOR_WRITE`, `NO_CULL`,
  `EQUAL_DEPTH_TEST`, `GLINT_TRANSPARENCY` (`blendFuncSeparate(SRC_COLOR, ONE, ZERO, ONE)`, i.e.
  additive with rgb squared), `TextureStateShard(tex, blur=true, mipmap=false)`, plus:

  | Type | Texture | Texturing | Output | Layering |
  |---|---|---|---|---|
  | `glint` | `ENCHANTED_GLINT_ITEM` | `GLINT_TEXTURING` (scale 8) | — | — |
  | `glint_translucent` | `ENCHANTED_GLINT_ITEM` | `GLINT_TEXTURING` (scale 8) | `ITEM_ENTITY_TARGET` | — |
  | `entity_glint` | `ENCHANTED_GLINT_ENTITY` | `ENTITY_GLINT_TEXTURING` (scale 0.16) | `ITEM_ENTITY_TARGET` | — |
  | `entity_glint_direct` | `ENCHANTED_GLINT_ENTITY` | `ENTITY_GLINT_TEXTURING` (scale 0.16) | — | — |
  | `armor_entity_glint` | `ENCHANTED_GLINT_ENTITY` | `ENTITY_GLINT_TEXTURING` (scale 0.16) | — | `VIEW_OFFSET_Z_LAYERING` |

* **`setupGlintTexturing(scale)`**:
  * `l = (long)(Util.getMillis() * options.glintSpeed().get() * 8.0)`
  * `g = (l % 110000) / 110000f`, `h = (l % 30000) / 30000f`
  * `RenderSystem.setTextureMatrix(new Matrix4f().translation(-g, h, 0).rotateZ(π/18).scale(scale))`
  * Clear is `RenderSystem.resetTextureMatrix()`.
* **Shaders** `rendertype_{glint,glint_translucent,entity_glint,entity_glint_direct,armor_entity_glint}`:
  * vsh: `texCoord0 = (TextureMat * vec4(UV0, 0, 1)).xy`
  * fsh: `color = texture(Sampler0, uv) * ColorModulator; if (color.a < 0.1) discard; fragColor = vec4(color.rgb * fogFade * GlintAlpha, color.a)`

  **So `ColorModulator` reaches the glint shaders.** `ShaderInstance.setDefaultUniforms` sets it from
  `RenderSystem.getShaderColor()` and sets `GlintAlpha` from `RenderSystem.getShaderGlintAlpha()`.
  `Minecraft` initialises the latter from `options.glintStrength()` at startup, and the option's
  change listener updates it.
* **Batching.** All five vanilla glint types are **fixed buffers** in `RenderBuffers`.
  `LevelRenderer` flushes them explicitly after the item sheets. The hand and GUI flush them through
  `bufferSource.endBatch()`, which iterates fixed buffers in insertion order, so glint comes after the
  item sheets. Glint must draw **after** its base geometry because of `EQUAL` depth.
* **Iris.** Iris overrides `GameRenderer.getRendertype*GlintShader`, and its batching classifies
  composite types whose transparency shard is `GLINT_TRANSPARENCY` as `DECAL` (drawn after opaque).
  Its `VanillaTransformer` routes `iris_ColorModulator` and `iris_GlintAlpha` into the glint program's
  input colour, and `TextureMat` into `iris_TextureMat`. So reusing vanilla's shader shard and
  `GLINT_TRANSPARENCY` keeps our types working under Iris. Tint and strength reach the pack's
  `gbuffers_armor_glint`; whether the pack multiplies by it is pack-dependent **[UNVERIFIED per pack]**.
* **Iris world buffer source (verified in the Iris 1.8.14-beta.1 bytecode).** Iris's
  `batchedentityrendering.mixin.MixinLevelRenderer` calls `beginLevelRendering()` at HEAD of
  `LevelRenderer.renderLevel` and `endLevelRendering()` at its RETURN, **unconditionally: also with
  shaders off.** While that counter is non-zero, `MixinRenderBuffers` makes `RenderBuffers.bufferSource()`
  return Iris's `FullyBufferedMultiBufferSource` (and `outlineBufferSource()` / `crumblingBufferSource()`
  wrappers around it). Its `getBuffer` override buffers per render type in its own segments and never
  calls the vanilla `BufferSource.getBuffer`, so G5a never sees world glints. Its `endBatch(RenderType)`
  is empty; it draws only in `endBatch()` / `endBatchWithType` (inside `renderLevel`), through
  `RenderType.draw(MeshData)`, ordered by transparency class (`OPAQUE`, `OPAQUE_DECAL`,
  `GENERAL_TRANSPARENT`, `DECAL`, ...), so G5b and G5c never run for it either. The hand and the GUI
  render outside `renderLevel` (shaders off) and keep using the vanilla main source. G7 covers the world.
* **ImmediatelyFast main buffer source (verified in the ImmediatelyFast 1.6.15+1.21.1 bytecode).**
  `core.MixinVertexConsumerProvider` `@Overwrite`s `MultiBufferSource.immediateWithBuffers`, which
  `RenderBuffers` uses to build its `bufferSource`, and returns a `BatchableBufferSource` (unless its
  debug option `disable_universal_batching` is set). So with ImmediatelyFast installed the main source
  that GA2 captures **is** that subclass. Its `getBuffer`, `endBatch()`, `endLastBatch()` and
  `endBatch(RenderType)` are overrides that never call the vanilla bodies: G5a and G5b never run for
  the main source. It keeps the vanilla `fixedBuffers` semantics (a fixed type gets its own builder on
  the fixed `ByteBufferBuilder`; `endBatch()` draws the non-fixed layers, then every fixed type in map
  order) and draws each batch through the vanilla private `endBatch(RenderType, BufferBuilder)`, so G5c
  still runs. Without G9 every glint of the main source keeps the vanilla look, whatever the config
  says: the first-person hand (any setup with ImmediatelyFast), and the world when Iris is not
  installed. G9 (5.2, point 7) fixes this.
* **ImmediatelyFast GUI buffer sources (verified in the same bytecode).** `core.MixinGameRenderer`
  redirects the `renderBuffers.bufferSource()` call of `GameRenderer.render` that builds the frame's
  `GuiGraphics` to `BatchingBuffers.getNonBatchingEntityVertexConsumers()`: a plain vanilla
  `BufferSource` (built with the constructor, not `immediateWithBuffers`), created once with fresh
  builders for the key set of main's fixed map at that moment. Inventories and menus draw into it,
  `GuiGraphics.renderItem` flushes it per item. While a HUD layer is batched (`hud_batching`, on by
  default; also `experimental_screen_batching`), `BatchingBuffers.runBatched` / `beginHudBatching` swap
  `GuiGraphics.bufferSource` for the `HudBatchingBufferSource` (a `BatchableBufferSource` subclass built
  the same way). Its `getBuffer` sends every type whose name contains `glint` straight to the parent;
  other types requested while `renderingItem` is set (inside `ItemRenderer.render`) are wrapped into
  per-lighting `WrappedRenderLayer`s, which are not fixed. `renderItem` does not flush; the batch is drawn
  at the end of each layer (`MixinLayeredDrawer`), or earlier by `tryForceDrawHudBuffers` (on
  `BufferUploader` draws, a render target call, and model-view and projection matrix changes).
  `BatchableBufferSource.endBatch()` draws the non-fixed layers first (sorted by its layer order, stable),
  then every fixed type in map order, each through the vanilla private `endBatch(RenderType,
  BufferBuilder)`, so G5c runs. Neither source is main, so before 2.0.0 the GUI glint stayed vanilla with
  ImmediatelyFast. G11 and G12 (5.2, point 8) fix this.

### 5.2 Strategy

1. **Never create render types per frame.** For each `GlintKind` (5 kinds) build `SLOTS_PER_KIND = 32`
   slot types and 1 "off" type, lazily on first use. Slot state lives in a mutable `GlintParams` that
   the slot's shards read at draw time.
2. **Substitute on the main source only** (G5a). Only on the main source can we make our types
   fixed buffers. Our types in a source where they are not fixed would get flushed early, and could
   even close a `BufferBuilder` that is still being written. Other sources, which are rare, get
   vanilla glint.
3. **Register each slot type into the main source's fixed map** (via GA1) the first time it is
   acquired: `fixed.put(type, new ByteBufferBuilder(type.bufferSize()))`. Insertion is after all
   vanilla entries, so `endBatch()` draws our glint after the base sheets. This is a one-time
   allocation per type, at most 165 × 1536 bytes.
4. **Flush our types right after vanilla's glint flush** in the world (G5b), keeping vanilla's
   ordering relative to water.
5. **Release a slot when its batch is drawn** (G5c). Also release all slots whose `frame` is stale at
   frame start (G6) as a safety net.
6. **Iris's fully buffered world source is the one exception to point 2** (G7, `substituteBuffered`).
   The early-flush hazard does not exist there: it keeps one segment per render type for the whole
   level pass and draws only at its own flush, our slot types are classified `DECAL` like the vanilla
   glint (same `GLINT_TRANSPARENCY` instance) and so drawn after the base layers. Same checks as G5a
   (custom looks, known kind, render thread, failsafe, main source captured) minus `source == main`,
   plus "no shader pack in use this frame" (queried once per frame in G6). Slot lifetime: Iris never
   passes G5c, so the slot is acquired **pinned**. `release` leaves a pinned slot in use (a main-source
   draw of a shared look cannot free it), so its params cannot be handed to another look before Iris
   draws it later in the same `renderLevel`; G6 frees and unpins it at the next frame start. The slot
   is not registered in `main` (Iris keeps its own buffers); G5b skips unregistered slots. Plain
   vanilla `BufferSource` instances other than main still get the vanilla glint. Verified in game
   (Iris 1.8.14-beta.1, shaders off): armor on armor stands showed every preset in its colour against
   the purple vanilla glint, and the 7-look showcase stand showed a different colour per look.
7. **ImmediatelyFast's main source** (G9). When the main source is ImmediatelyFast's
   `BatchableBufferSource` (5.1), G9 calls `substitute` from its `getBuffer` with the unchanged G5a
   rules (`source == main`, slot registered as a fixed buffer of main). The subclass honours the fixed
   map, so registration, ordering (slots after the vanilla fixed types in `endBatch()`) and release
   (G5c) work as in vanilla. G5b's flush after the vanilla `entityGlintDirect` batch is done from G5c
   instead, because the public `endBatch(RenderType)` is overridden: `onBatchDrawn(source, type)` flushes
   when `source == main`, the type is the vanilla `entityGlintDirect` and the source's class is not the
   plain vanilla `BufferSource` (for that class G5b keeps doing it, so the vanilla path is unchanged).
   Note on the T10/T12 captures: `hero_firstperson` (rainbow look) shows a uniform purple wash on the
   held sword while the same look in an item frame (`hero_sword_rainbow`, G7) shows the rainbow, i.e.
   the first-person capture was the vanilla glint; the hand was never verified with a custom look
   before G9.
8. **ImmediatelyFast's GUI sources** (G11, G12; `GlintSystem.substituteGui`). Point 2's hazard is
   about non-fixed glint types in someone else's source, so the same remedy works there: the GUI source
   itself plays the role of main. Same checks as G5a (custom looks, known kind, render thread, failsafe,
   client started) with `source != main`; the slot type is registered as a fixed buffer **of that
   source** (`fixedBuffers.containsKey` first, because ImmediatelyFast copies main's fixed keys when it
   creates the source, so types registered in main before that are already there). Fixed types come
   after the vanilla fixed entries (the vanilla glint, the item sheets) in insertion order, and in the
   HUD source after all non-fixed layers (the lighting-wrapped item bases), so our glint is drawn after
   its base like the vanilla glint. Our types are named `glintsmith_*glint*`, so the HUD source treats
   them like the vanilla glint (no lighting wrapping). The slots are not pinned: only one source holds
   their geometry at a time (the world and the hand are flushed before the GUI), and they are released by
   G5c when that source draws them (per item for the non-batching source, at the end of the HUD layer for
   the HUD source; up to 32 looks per kind per HUD layer). Nothing in this path draws or flushes, so the
   HUD batch is still drawn only when ImmediatelyFast draws it (the T20 hotbar rule in section 6 is not
   touched). Which source is which: G11 reports the non-batching instance (G5a compares one reference and
   calls `substituteGui` for it, `substitute` otherwise, so the main path of `substitute` is unchanged);
   G12 calls `substituteGui` from the HUD class's own `getBuffer`. A plain `BufferSource` of anybody else
   still gets the vanilla glint (no per-source registration in transient sources, which would leak
   native buffers).

### 5.3 Categories, context and resolution

`GlintCategory { ITEM, ARMOR, ENTITY, GUI }`. `GlintContext` is a fixed-capacity static stack of
`Frame` objects (capacity 16, preallocated; on overflow push is ignored and pop is counted so it stays
balanced). It is touched only on the render thread. Each frame has:

```
ItemStack stack;              // may be ItemStack.EMPTY
ItemDisplayContext ctx;       // null for armor
byte kind;                    // ITEM_FRAME_KIND, ARMOR_FRAME, BEWLR_FRAME
boolean guiAncestor, armorAncestor, bewlrAncestor;   // inherited from parent + own
boolean lookResolved; GlintLook look;                // per-frame cache of the rule result
```

Category decision for a requested vanilla kind `k` and top frame `f`, null allowed:

```
if (f != null && f.guiAncestor)                                     return GUI;
if (k == ARMOR_ENTITY_GLINT || (f != null && f.armorAncestor))      return ARMOR;
if (k == ENTITY_GLINT || k == ENTITY_GLINT_DIRECT || (f != null && f.bewlrAncestor)) return ENTITY;
return ITEM;
```

Rule target for the frame:

* GUI → `StyleTarget.GUI`
* ARMOR → `StyleTarget.ARMOR`
* otherwise map `f.ctx` with the table in section 3.1, except that while `GlintContext.inHands()` is true the
  THIRD_PERSON_* contexts map to FIRST_PERSON (hand pass rule, see section 3.1)
* no frame or an empty stack → no rule lookup (category defaults)

The look is resolved once per frame:

```
if (!f.lookResolved) { ResolvedStyle rs = stack.isEmpty() ? null : RuleEngine.resolve(stack, target);
                       f.look = rs == null ? null : rs.glint(); f.lookResolved = true; }
```

Effective parameters, filled into a scratch `GlintParams`. `cs` is the category snapshot from config.

| Field | Rule |
|---|---|
| off | `!cs.enabled \|\| (look != null && !look.enabled())` → return the kind's **off type** |
| texture spec | `look != null && look.texture() != null ? look.texture() : cs.texture` |
| tintArgb | `look != null && look.tint() != null` → `RuleEngine.glintTint(stack, look, t)`; else `cs.tint != null` → `RuleEngine.glintTint(stackOrEmpty, cs.categoryLook, t)`; else `NO_TINT = 0xFFFFFFFF` |
| speed, scale, strength | from `look` if `look != null`, else from `cs` (T3 pre-merges defaults into `GlintLook`) |
| rotationDeg | `cs.rotation` (`GlintLook` has no rotation) |
| texture | `GlintTextures.resolve(spec, kind.entityFamily, tintArgb != NO_TINT)` → `ResourceLocation` |

`cs.categoryLook` is built once per config change:
`new GlintLook(true, cs.tint, cs.texture, cs.speed, cs.scale, cs.strength)`.

**Vanilla-equivalence shortcut.** If texture == the kind's vanilla texture, tint == NO_TINT, speed == 1,
scale == 1, strength == 1 and rotationDeg == 10, return the requested vanilla type unchanged.
`settings.anyCustom` (false when every category is default and no rule defines a glint look, as
reported by T3) short-circuits G5a entirely.

### 5.4 Slot render types

```java
// for kind k, slot s (s = 0..31); the off type uses WriteMaskStateShard(false,false), vanilla texture, DEFAULT_TEXTURING
RenderType.CompositeState.CompositeStateBuilder b = RenderType.CompositeState.builder()
    .setShaderState(k.shaderShard)                                   // RenderStateShard.RENDERTYPE_*GLINT*_SHADER (vanilla instance!)
    .setTextureState(new RenderStateShard.EmptyTextureStateShard(slot::setupTexture, () -> {}))
    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
    .setCullState(RenderStateShard.NO_CULL)
    .setDepthTestState(RenderStateShard.EQUAL_DEPTH_TEST)
    .setTransparencyState(RenderStateShard.GLINT_TRANSPARENCY)       // vanilla instance: Iris DECAL ordering
    .setTexturingState(new RenderStateShard.TexturingStateShard("glintsmith_glint_texturing", slot::setupTexturing, slot::clearTexturing));
if (k.itemEntityTarget) b.setOutputState(RenderStateShard.ITEM_ENTITY_TARGET);   // GLINT_TRANSLUCENT, ENTITY_GLINT
if (k.viewOffset)       b.setLayeringState(RenderStateShard.VIEW_OFFSET_Z_LAYERING); // ARMOR_ENTITY_GLINT
RenderType type = RenderType.create("glintsmith_" + k.id + "_" + s, DefaultVertexFormat.POSITION_TEX,
                                    VertexFormat.Mode.QUADS, 1536, false, false, b.createCompositeState(false));
```

All shard constants, `RenderType.create`, `CompositeRenderType` and `CompositeState` are made accessible
by Fabric API's transitive access wideners (verified in fabric-transitive-access-wideners-v1 6.2.0).
`CompositeState.builder()` and the builder's setters are public in vanilla. Shard setup order follows
`CompositeState.states`, with texture before texturing.

Slot shard bodies. Each is wrapped in try/catch: on failure, trip the glint failsafe and fall back to
vanilla binding and the vanilla matrix.

```java
private static final Matrix4f MATRIX = new Matrix4f();     // render thread only

void setupTexture() {
    AbstractTexture tex = Minecraft.getInstance().getTextureManager().getTexture(params.texture);
    tex.setFilter(true, false);                            // like vanilla glint: blur, no mipmap
    RenderSystem.setShaderTexture(0, tex.getId());
}
void setupTexturing() {
    Options o = Minecraft.getInstance().options;
    double t = Util.getMillis() * o.glintSpeed().get() * 8.0 * params.speed;
    float g = (float) ((t % 110000.0) / 110000.0);
    float h = (float) ((t % 30000.0) / 30000.0);
    MATRIX.identity().translation(-g, h, 0.0F)
          .rotateZ(params.rotationDeg * Mth.DEG_TO_RAD)
          .scale(kind.baseScale / Math.max(0.05F, params.scale));    // scale 2 = pattern twice as large
    RenderSystem.setTextureMatrix(MATRIX);                           // copies the matrix
    int c = params.tintArgb;                                         // sqrt compensates the SRC_COLOR squaring
    RenderSystem.setShaderColor((float) Math.sqrt(((c >> 16) & 255) / 255.0),
                                (float) Math.sqrt(((c >> 8) & 255) / 255.0),
                                (float) Math.sqrt((c & 255) / 255.0), 1.0F);
    RenderSystem.setShaderGlintAlpha(o.glintStrength().get() * params.strength);
}
void clearTexturing() {
    RenderSystem.resetTextureMatrix();
    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    RenderSystem.setShaderGlintAlpha(Minecraft.getInstance().options.glintStrength().get());
}
```

The vanilla accessibility sliders "Glint Speed" and "Glint Strength" stay respected as multipliers.
Rotation is in degrees (vanilla = 10).

Large block atlases: item glints depend on the size of the block atlas; since 2.0.0 the atlas factor
corrects that (5.4.1). The matrix above is the 1.0.0 one; with the factor the scale is per axis.

Slot acquire (`GlintSlotPool.acquire(kind, scratch)`), on the render thread:

1. Scan the kind's 32 slots. If `slot.inUse && slot.params.sameAs(scratch)`, return `slot.type`.
2. Otherwise take the first slot with `!slot.inUse || slot.frame != currentFrame`. Copy `scratch`
   into its params, set `inUse = true` and `frame = currentFrame`, register it as fixed if not yet
   registered, and return its type.
3. If all slots are busy, return the requested vanilla type. Count the overflow and log it once at
   debug level.

`sameAs` compares the texture by identity (resolved `ResourceLocation`s are interned through the
resolver map), the int tint, and the floats with `==`. Release: `onBatchDrawn(type)` →
`byType.get(type)` (an `IdentityHashMap`) → `inUse = false`.

Why this bounds everything: GUI items flush per item, so slots recycle per item. World and hand flush
once, so up to 32 distinct looks per kind per frame; the same holds for one ImmediatelyFast HUD layer,
which is drawn once at its end. Animated tints change their ARGB every frame but reuse the same slots.
Memory and the render-type count are constant.

#### 5.4.1 Atlas-independent scale (2.0.0)

Problem. Item glints are textured through the item's own UVs on the block atlas, like vanilla
(`texCoord0 = TextureMat * UV0`). How much of the glint texture lands on an item therefore depends on the
UV range the item's sprite covers: in a pack whose block atlas is very large (the owner's Nytheria pack:
8192 x 4096 px with 16 px item textures), each item covers an eighth of the vanilla UV range per axis, so
at Scale 1.00 it shows only a few texels of the pattern and a preset becomes a flat colour wash instead of
bands or sparkles. Vanilla's own glint has the same dependence (less visible because its texture is
smooth). The 1.0.0 workaround was a lower Scale: about 0.13 for items.

Factor. `GlintAtlasScale` keeps one factor per axis,

```
f_u = (REFERENCE_WIDTH  / atlasWidth)  * (spriteWidth  / 16)
f_v = (REFERENCE_HEIGHT / atlasHeight) * (spriteHeight / 16)
```

clamped to 1/64..64, 1 for any non-positive size. The first term is the atlas ratio; the second is the
size of a reference sprite (`minecraft:item/diamond_sword`) in the current atlas against its vanilla 16
px, so high-resolution packs stay neutral: a 32x pack doubles the atlas and the sprites, the UV range of an
item and so the vanilla look do not change, and the atlas ratio alone (0.5) would make the glint twice as
dense there. When the reference sprite is missing the second term is 1. For the owner's pack both factors
are 0.125, which matches the manual 0.13. The glint scale is multiplied by the factor (the matrix divides
by it).

Reference size. The vanilla 1.21.1 block atlas at the default mipmap level 4 is **1024 x 512**. Determined
by running the vanilla `Stitcher` (`new Stitcher<>(max, max, 4)`, as `SpriteLoader.stitch` does) over
exactly what `assets/minecraft/atlases/blocks.json` of the 1.21.1 client jar lists: the `block`, `item`
and `entity/conduit` folders, the single sprites `entity/bell/bell_body`,
`entity/decorated_pot/decorated_pot_side` and `entity/enchanting_table_book`, the 4 x 14 armor trim item
permutations and `missingno`, 1684 sprites, with animated sprites at their frame size. Development runs
with Fabric API and small mods log the same `Created: 1024x512x4 minecraft:textures/atlas/blocks.png-atlas`.
`GlintAtlasScaleTest.referenceIsTheStitchedVanillaBlockAtlas` repeats the computation from the client jar on
the test class path, so the constants cannot drift. The atlas is not square, so u and v are kept apart.

Reading the actual size (G10). `TextureAtlas.upload(SpriteLoader.Preparations)` is where vanilla takes the
size of a freshly stitched atlas (the model manager's apply step, on the render thread, after every
resource reload). G10 injects at its TAIL; `GlintSystem.onAtlasUploaded` ignores every atlas but
`InventoryMenu.BLOCK_ATLAS`, reads `preparations.width()` / `height()` and the reference sprite from
`preparations.regions()`, and logs one INFO line when a factor changes. A reload listener would not do:
Fabric's listeners have no defined order against the model manager's apply step. Before the first upload
both factors are 1.

Where it applies (`GlintCategories.blockAtlasUvs`). By the UV source, not by the category:

| Draw | UVs | Factor |
|---|---|---|
| `glint` / `glint_translucent` requested while the innermost frame is an item frame (G1): held, dropped, framed and GUI items, block items, the GUI/ground/frame trident and spyglass models | the baked model's block-atlas UVs | yes, in every category (GUI included) |
| compass and clock with foil (`ItemRenderer.hasAnimatedTexture`) | `SheetedDecalTextureGenerator`, from the position | no |
| innermost frame BEWLR (G4): shield, trident in hand, banners, chests, skulls, mod renderers | entity textures or other atlases (shield patterns, banners, chests) | no |
| armor and elytra (G2, G3, `armor_entity_glint`) | 0..1 UVs of the armor layer or elytra texture | no |
| `entity_glint`, `entity_glint_direct` (thrown trident and other entity models) | the entity texture | no |
| no frame | unknown drawer | no |

So armor is not corrected: its UVs do not depend on the block atlas (the 0.2 armor Scale of the gallery
captures is a taste for denser patterns, not an atlas correction). The other atlases (shield patterns,
banners) can also grow with mods; that is not corrected (small atlases, out of scope).

Only when customised. The factor is set after the vanilla-equivalence check of `pick`
(`SCRATCH.isVanilla(kind)` runs on the unscaled params), so a stack whose look is vanilla keeps the
requested vanilla type; with `anyCustom` false nothing runs at all. The switch reaches the renderer only
through `GlintRuntimeSettings.atlasScale` (the config listener's snapshot of `glint.atlasIndependentScale`,
default on; configs without the field keep the Gson default, i.e. on), never through
`ConfigManager.current()`. `substitute`'s `source == main` path is unchanged.

Matrix. The factor is part of the slot params (`atlasU`, `atlasV`; `sameAs` compares them, so two looks
that differ only by it never share a slot). `setupTexturing` scales u and v before the rotation, in atlas
UV space: `translation(-g, h, 0).rotateZ(rotation).scale(s / atlasU, s / atlasV, s)` with
`s = baseScale / max(0.05, scale)`; with factor 1 this is the 1.0.0 `scale(s)`.

Users who lowered Scale for a large atlas in 1.0.0 should set it back to 1.00 (the factor and the manual
value would multiply).

### 5.5 Texture management (`GlintTextures`)

Spec grammar for `texture` strings (config and `GlintLook.texture`):

| Spec | Meaning |
|---|---|
| `null`, `""`, `"default"` | Vanilla per kind family: `minecraft:textures/misc/enchanted_glint_item.png` (GLINT, GLINT_TRANSLUCENT) or `..._entity.png` (others). **If a tint is set**, use `glintsmith:glint/preset/neutral_item` or `neutral_entity` instead, because tinting the purple vanilla texture gives mud. |
| `preset:<name>` | Generated preset, `glintsmith:glint/preset/<name>` |
| `file:<name>.png` | `<configDir>/glintsmith/glints/<name>.png`, loaded into a `DynamicTexture` at `glintsmith:glint/file/<sanitized>` (lowercase, `[a-z0-9_.-]`, others become `_`). Maximum 1024x1024. NPOT is fine (GL 3.2 core). |
| `<namespace>:<path>` | Resource-pack texture, used as is. Validated with `resourceManager.getResource(loc).isPresent()`. |

Rules:

* An invalid spec logs one WARN per distinct spec and falls back to the vanilla texture.
* Resolution results are cached in `HashMap<String, ResourceLocation[2]>` (index = kind family). The
  cache is cleared on config change and on resource reload. Never resolve per frame without the cache.
* Wrap mode: `DynamicTexture.upload()` uploads with `clamp=false`, so wrap is REPEAT. Resource-pack
  textures are `SimpleTexture` with clamp from `.mcmeta` (default false).
* The blur filter is set by the slot's texture shard every draw, like vanilla.

Lifecycle:

* `CLIENT_STARTED` (render thread): generate all procedural presets once, then build the neutral
  textures from the current resources.
* Reload listener `glintsmith:glint_textures` (`SimpleSynchronousResourceReloadListener`, which
  applies on the render thread): rebuild `neutral_item` and `neutral_entity`, which depend on resource
  packs; reload `file:` textures; clear the resolve cache.
* `ConfigManager.addListener`: rebuild the settings snapshot, clear the resolve cache, reload `file:`
  textures if the set of referenced files changed. If called off the render thread, use
  `RenderSystem.recordRenderCall`.
* Registration: `textureManager.register(id, dynamicTexture)`. `TextureManager.register` closes the
  previous texture under the same id.

### 5.6 Procedural presets (no third-party art)

Source: `GlintPresets` (the looks), `GlintNoise` (the helpers), `GlintTextures` (registration). Everything here describes
the code as it stands after the preset redesign (2.0.0), and `GlintPresetsTest` enforces the numbers in the budget below.

All generated presets are 128x128, tileable, deterministic (same bytes on every run and platform: hashes are integer
arithmetic, transcendental functions use `StrictMath`), and built as `int[128 * 128]` of ABGR pixels, then uploaded with
`new NativeImage(128, 128, false)` + `setPixelRGBA` + `new DynamicTexture(img)`. Alpha is always 255. Black means "no
glint", because the blend is additive. Every channel is capped at `PEAK_BYTE = 229` (0.9 of 255). All 12 are generated
in about 80 ms at start-up. `TextureManager.register` closes the previous texture under the same id.

**Brightness budget.** The glint blend squares the texture colour and adds it (`src * src + dst`), so what an item
receives is the mean of the squared channels, the glint energy. A texture that is lit everywhere washes the item out
in one colour, and one that is black in most places leaves patches of glint on black. The presets are shaped like the
vanilla glint: a soft glow over the whole tile with streaks on top, so the whole item shimmers. Each generated preset
must satisfy (`lum = 0.2126 r + 0.7152 g + 0.0722 b`, channels 0..1):

| Property | Bound |
|---|---|
| Strength (mean squared RGB) | 0.10 to 0.26 |
| Spread | 95th minus 5th percentile of the luminance at least 0.25 |
| Peak channel | at least 200 and at most 229 |
| Periodicity | `f(x + 128, y) == f(x, y)` in both axes, so it tiles |
| Seams | the mean difference across the tile edge is at most twice the mean difference between neighbours inside the tile, plus one byte (both axes) |

**Helpers** (`GlintNoise`, pure Java, `TILE = 128`; `u = x / 128`, `v = y / 128` below):

* `hash(x, y, seed)` returns a float in [0, 1): `h = x * 374761393 + y * 668265263 + seed * 1442695041; h = (h ^ (h >>> 13)) * 1274126177; h ^= h >>> 16; return (h & 0xFFFFFF) / 16777216`.
* `value(x, y, px, py, seed)` is value noise with `px` by `py` lattice cells across the tile (smooth interpolation, lattice indices wrap with `floorMod`, so it is periodic).
* `fbm(x, y, px, py, octaves, seed)` sums `octaves` value noises, octave `o` with periods `px << o`, `py << o`, amplitude `0.5^o`, seed `seed + 101 * o`, normalised to [0, 1).
* `ridged(x, y, px, py, octaves, seed)` is the same sum of octaves with each octave `(1 - |2 value - 1|)^2`: sharp bright ridges where the noise crosses its middle (veins, lightning).
* `streaks(x, y, kx, ky, count, width, offset, seed)` is a sum of soft parallel streaks perpendicular to the integer normal `(kx, ky)`, `count` per wrap of the tile along it, each with its own brightness, width and position from the hash; integer `kx`, `ky` keep it periodic, and a periodic noise as `offset` makes the streaks wavy.
* `worley(x, y, g, seed, out)` is toroidal Worley noise on a `g` by `g` grid (one feature point per cell, 3x3 wrapped neighbourhood). It returns F1 and F2 in cell units, and the wrapped cell of the nearest point when `out` has four entries.
* `smoothstep(e0, e1, x)`, `lerp(t, a, b)`, `hsv(h, s, v)` (hue wraps), `frac`.

**Presets.** Items and armor show the texture squashed to half its height (the glint texture matrix), so stars, sparkles and
flames are drawn twice as tall in the texture as they should look.

| Name | Look |
|---|---|
| `neutral_item` / `neutral_entity` (not generated; `preset:neutral`) | Read vanilla `enchanted_glint_item.png` / `_entity.png` from the current `ResourceManager`. Per pixel `L = (0.2126 r + 0.7152 g + 0.0722 b) * alpha`, divided by the image's maximum `L` and multiplied by 0.9; output grey `(L, L, L)`. Same size as the source. Rebuilt on every resource reload. This is the base for tinting. |
| shared base of the seven vanilla-like presets (`sheen`) | A soft glow with a slow wave over the whole tile, plus two crossing sets of wavy diagonal streaks (`streaks`, normals `(2, 1)` and `(-1, 1)`, made wavy by periodic noise), squashed to 0..1 with `1 - exp(-1.8 v)`. A preset only chooses the colours the brightness is mapped to. |
| `rainbow` | The base with a hue that shifts diagonally, so the colours cycle as the texture scrolls; the cores of the streaks turn whiter. |
| `aurora` | The base in green, teal and violet: the hue swings between green and violet in wavy diagonal bands. |
| `fire` | The base mapped through an ember ramp: dark red glow, orange streaks, yellow-white cores. |
| `frost` | The base in near-white, pale ice with crisp white streaks, so that a tint (the item layer's default rainbow tint) colours it cleanly. |
| `sparkle` | A silver base plus fine glitter specks. |
| `gold` | The base in amber and gold, bright streaks like light on metal. |
| `void` | The base in deep violet with magenta streaks and a few faint stars. |
| `inferno` | Rich: flame tongues, one per cell of a 12 by 4 grid, each with its own position, length and width, hot at the base and narrowing to a tip; periodic noise sways them, a faint ember glow fills the gaps. The texture scrolls upwards, so the tongues rise. |
| `nebula` | Rich: domain-warped clouds in magenta, blue and cyan over a deep indigo glow, with stars. |
| `starlight` | Rich: four-pointed sparkles of many sizes over a cool shimmer. |
| `opal` | Rich: a milky blue-grey glow with flowing flashes of iridescent colour along the ridges of a warped noise. |
| `storm` | Rich: branching cyan-white lightning veins (ridged noise) with a violet halo. |

Full preset list: `rainbow`, `aurora`, `fire`, `frost`, `sparkle`, `gold`, `void`, `inferno`, `nebula`, `starlight`, `opal`,
`storm` (generated) and `neutral` (derived from the vanilla texture). Together with `vanilla` and `file:` / resource-pack
entries this is exactly what the config and the UI accept: `vanilla` (alias `default`), `preset:neutral` (this resolves
to the item or entity variant automatically), `preset:rainbow`, `preset:aurora`, `preset:fire`, `preset:frost`,
`preset:sparkle`, `preset:gold`, `preset:void`, `preset:inferno`, `preset:nebula`, `preset:starlight`, `preset:opal`,
`preset:storm`, `file:<name>.png` and `namespace:path`. `preset:neutral_item` and `preset:neutral_entity` are internal
names, not valid specs.

### 5.7 Edge cases T6 must handle

1. No context frame (thrown trident, mods drawing glint directly on the main source): use the
   category from the kind with category defaults. No rule lookup (the stack is EMPTY).
2. `RuleEngine.resolve` returns null or throws: treat it as `look = null`. On a throw, trip the glint
   failsafe.
3. A slot texture vanishes after a reload: `TextureManager.getTexture` returns the missing texture.
   That does not crash, and the next config or reload event re-resolves the slot.
4. Config changes mid-frame: the settings snapshot is swapped atomically (volatile field). Slots
   already assigned keep their params until drawn.
5. Iris installed: the world always uses Iris's `FullyBufferedMultiBufferSource` (shaders on or off),
   which G7 handles (5.2, point 6). With shaders off, hand and GUI use the main source (G5a). Shader
   pack on: substitution still runs on the main source (GUI); the hand is drawn inside `renderLevel`
   by Iris's `HandRenderer` through its own `FullyBufferedMultiBufferSource` instance (also a G7
   target), and G7 is a no-op under a pack, so world and hand keep the vanilla look
   (unchanged behaviour; enabling G7 under a pack is a separate, untested step).
6. A category is disabled: return the kind's off type. It is registered fixed like slots. The colour
   and depth masks are off, so the geometry is drawn but invisible. Optional optimisation, not
   required: also `@ModifyVariable` the `boolean` foil parameter of
   `ItemRenderer.getFoilBuffer` / `getFoilBufferDirect` / `getArmorFoilBuffer` to skip the vertices.
7. Never call `RenderType.glint()` and friends from inside G5a with substitution enabled; it cannot
   recurse, but keep the vanilla instances in static finals captured at class init.
8. Thread safety: all of this is render-thread only. Assert with `RenderSystem.isOnRenderThread()` in
   debug builds; in release builds, skip substitution when off-thread.

---

## 6. Compatibility matrix and failure behaviour

| Environment | Outlines: world (3rd person, armor, dropped, frames) | Outlines: first person | Outlines: GUI | Glint |
|---|---|---|---|---|
| Vanilla (Fast/Fancy) | Full | Full | Full | Full |
| Fabulous | Full. Composite goes into main before the transparency chain; translucent layers combine over it correctly. `glintTranslucent` / `entityGlint` slots keep `ITEM_ENTITY_TARGET`. Outlines tested in game without a shader pack (held items, item frames, dropped items, occlusion, stained glass from the front and side, water, rain, GUI on and off). | Full | Full | Full |
| Sodium 0.8.13 | Full. Sodium replaces terrain only. Its item fast path (`ItemRendererMixin`) and `putBulkData` intrinsics fall back to vanilla code for our tee because it is not a `BufferBuilder` or `VertexBufferWriter`. Sodium logs **one** WARN per class (`VertexConsumerTracker`: "does not support optimized vertex writing code paths"). FRAPI models render through the `MultiBufferSource` we pass. | Full | Full | Full |
| Iris installed, shaders off | Full (Iris hand pathway inactive) | Full | Full | Full. Iris's batching is active even without a pack: the world draws through its `FullyBufferedMultiBufferSource`, which G7 covers (5.2, point 6); hand and GUI through the main source (G5a; with ImmediatelyFast see its row). Verified in game (Iris 1.8.14-beta.1, shaders off): preset glints on armor stands and the showcase stand show their colours. |
| Iris, shader pack on | **Off** (log once per pack activation, at INFO). Main depth is not the scene depth and entities go to Iris gbuffers. The shadow pass is skipped too (phase closed, plus an explicit `isRenderingShadowPass()` check). | **Best effort, default on, option to disable.** The mask is flushed at TAIL of `renderHandsWithItems` with Iris's hand matrices; the composite happens after Iris's final pass at `GameRenderer.renderLevel` RETURN. The arm hides the outline through hand occluders: under a pack the hand buffer source is wrapped in an `OccluderBufferSource` that records every depth-writing render type of the hand pass (arm, sleeve, held items, maps) as depth-only geometry (`DEPTH_WRITE`, `LEQUAL`, the source's layering and cutout), drawn into the mask depth after the item masks; the composite runs with `UseDepth=1` and linearises depth from the hand projection terms (`DepthProj`; Iris scales the hand projection by 1/8 in z), see 3.3. Known limits: with both Iris hand passes an arm from pass 1 can cover an item from pass 2; hand geometry drawn outside the passed buffer source does not occlude. Packs that move or distort the hand (DOF, hand sway) may misalign it. Verified in game (Iris 1.8.14-beta.1 + Complementary Unbound): the arm hides the outline, the visible blade is fully outlined. | Full | GUI full. World and hand use the vanilla look: G7 is a no-op while a shader pack is in use. Allowing it there would send our slots through Iris's glint program (texture and matrix honoured; tint and strength pack-dependent), not done yet (9, point 4). |
| First-person animation mods (Punchy 2.8f) | n/a | Full for items drawn through `ItemRenderer.render` inside `renderHandsWithItems`: THIRD_PERSON hand contexts there count as FIRST_PERSON. Not outlined: items Punchy draws without `ItemRenderer.render` (bedrock-geometry items, chests, boats). Verified in game: first-person outlines and glint rules on Punchy-held items drawn through `ItemRenderer.render` (main hand and offhand). | n/a | Full: the hand pass marker (G8) applies first-person rules to the same items |
| First-person mode of player-animation-lib / Better Combat | The held item is drawn in the world pass as THIRD_PERSON and is outlined as a third-person item; none under a shader pack | Not applicable (the item is not drawn in the HAND phase) | n/a | THIRD_PERSON rules |
| Other glint mods | Unaffected | Unaffected | Unaffected | If they replace the `RenderType.glint()` getters, our G5a never sees vanilla types and their look wins. If they also hook `BufferSource.getBuffer`, the last modifier wins. Document as incompatible-by-design. |
| Mods with a stencil main framebuffer (e.g. portals) | Disabled after the first failed depth blit (one log line) | Vanilla: disabled likewise | Full | Full |
| Multiple `renderLevel` per frame (portal mods) | The phase opens and composites per call | Unchanged | Unchanged | Unchanged |
| Mods drawing items without `MultiBufferSource`, or GUI items without `GuiGraphics.renderItem` (EMI batching) | Not outlined | Not outlined | Not outlined | Vanilla glint if not on the main source |
| ImmediatelyFast HUD batching (1.6.x) | Unaffected | Unaffected | Full. IF swaps the `GuiGraphics` buffer source for its `HudBatchingBufferSource`, skips the flush inside `renderItem` and draws its batch at the end of each HUD layer, or earlier when anything calls `BufferUploader.draw` / `_drawWithShader` (its `tryForceDrawHudBuffers`). Our mask draw is such a call, made with the mask target bound and scissored; without a flush first, the batch so far (hotbar sprite, earlier slots, counts) would be drawn into the mask and lost. Therefore, when `ItemRenderer.render` of an outlined GUI item ends (O1, the point where vanilla flushes, still with the item's lighting), its buffer source is flushed (depth test off as in `GuiGraphics.flush()`, then restored), which draws the batch onto main in order before the composite; non-outlined items keep batching. The same applies to IF screen batching (`experimental_screen_batching`, off by default). | Glint: ImmediatelyFast's `BatchableBufferSource` is the main source and bypasses G5a/G5b (5.1); G9 covers it, so the first-person hand (and the world without Iris) get the custom look (not yet verified in game). GUI glints: its non-batching GUI `BufferSource` and `HudBatchingBufferSource` are not the main source; G11 and G12 (5.2, point 8, since 2.0.0) register the slot types as fixed buffers of those sources, so inventories, menus and the batched hotbar get the custom look too (not yet verified in game). They never draw or flush the HUD batch themselves, so the hotbar flush rule above is unchanged. |

Detection (`navrelis.glintsmith.render.outline.compat.IrisCompat`, T5):

* `FabricLoader.getInstance().isModLoaded("iris")` is checked once.
* If loaded, a nested holder class is touched that references `net.irisshaders.iris.api.v0.IrisApi`.
  It is never loaded otherwise, so there is no `NoClassDefFoundError`.
* `boolean shaderPackInUse()` is cached per frame in `onFrameBegin`. `boolean shadowPass()` is called
  directly.
* Any `Throwable` from the Iris API → treat as "pack in use" (safe side) and log once.
* No Sodium API is used at all. Sodium needs no detection.

Failure behaviour summary:

* Outline and glint each have an independent failsafe. One log line at ERROR level with the stack
  trace, then the feature is off for the session. Vanilla rendering continues untouched.
* A shader compile failure disables outlines with one line; a reload retries.
* A hook that is missing degrades as described in the section 4 tables.

---

## 7. Performance notes

* **Nothing outlined on screen:** per item render the cost is one `RuleEngine.resolve` call plus a few
  branches. No tees, no GPU work, no allocations. Targets are freed after 600 idle frames.
* **CPU per outlined item:** one extra vertex write per vertex (mask: position, colour, UV) plus a
  16-FMA projection for the bbox. A flat generated item has a few hundred vertices, so this is a few
  µs. Real buffers lose Sodium's intrinsics for outlined items only.
* **GPU per phase:** mask draw (a few draw calls, one per distinct texture) plus pass H and pass V over
  the clipped region, and in WORLD and vanilla HAND a scissored depth clear and a depth blit. The
  visible-item rule (3.3) adds no pass and no target. Taps per pixel are at most 2(R+1) per pass, with early exit. Estimates
  **[UNVERIFIED, not measured]**:
  * 1080p, R = 6, region about 25 % of the screen: about 10 M texel fetches, under 0.1 ms on a GTX
    1060-class GPU.
  * Worst case 4K, R = 32, full screen: about 0.5 G fetches, 2 to 4 ms.

  So the default thickness is 8.5 px and the largest setting is 16 px (`OutlineSettings.MAX_THICKNESS`). Softness
  extends the radius to `t·(1+s)`, and the hard cap is 32.
* **GUI:** about 4 draws per outlined slot (scissored clear, mask, H, V), each over roughly 60x60 px.
  An inventory with 36 enchanted items costs about 150 small draws.
* **Glint:** zero extra draws when all looks are vanilla (shortcut). Otherwise one draw per used slot
  per flush (GUI: per item, like vanilla). No per-frame `RenderType` or texture allocation.
  `RenderSystem.setTextureMatrix` copies a `Matrix4f` per draw (vanilla does the same).
* **No reflection.** Accessors are compiled. The Iris API is reached through a cached instance in a
  lazily loaded holder.
* **Resize** is handled by comparing sizes before each composite. Resource reload marks shaders dirty
  (outline) and rebuilds the neutral textures (glint).

---

## 8. Package and class spec, resources, and T8 hooks

**Ownership rule: T5 and T6 share no classes.** Both depend only on T3 (`ConfigManager`, `RuleEngine`,
`ResolvedStyle`, `OutlineLook`, `GlintLook`, `ColorStyle`, `StyleTarget`), vanilla and Fabric API. The
display-context → target mapping is duplicated on purpose (a few lines each). Each owns its mixins,
its failsafe and its init method. T8 only edits `glintsmith.mixins.json` and `Glintsmith.java`.

### 8.1 T5: outline core (`navrelis.glintsmith.render.outline`)

```java
public final class OutlineSystem {                          // static façade; render thread only
    public static void init();                              // called once by T8 from onInitializeClient
    // O1
    public static MultiBufferSource beginItem(ItemStack stack, ItemDisplayContext ctx, MultiBufferSource src);
    public static void endItem(MultiBufferSource returnedByBegin);
    // O2
    public static void pushHolder(@Nullable LivingEntity holder);
    public static void popHolder();
    public static void beginHands();                        // opens HAND phase
    public static void endHands();                          // flush mask; composite (vanilla) or mark pending (Iris)
    // O3, O4
    public static MultiBufferSource beginArmor(LivingEntity entity, EquipmentSlot slot, MultiBufferSource src);
    public static void endArmor(MultiBufferSource returnedByBegin);
    // O5
    public static void beginGuiItem();
    public static void endGuiItem();
    // O6
    public static void onFullFlush(MultiBufferSource.BufferSource source, boolean begin);
    public static void onEndBatch(MultiBufferSource.BufferSource source, RenderType type);
    // O7
    public static void onFrameBegin();
    public static void onFrameEnd();
    public static void onLevelRendered();                   // GameRenderer.renderLevel RETURN
}
enum OutlinePhase { NONE, WORLD, HAND, GUI }
final class PhaseState {                                    // one per phase
    final OutlinePhase phase; final boolean depthTested;
    final MultiBufferSource.BufferSource maskSource;        // immediateWithBuffers(new Object2ObjectLinkedOpenHashMap<>(), new ByteBufferBuilder(256))
    final StyleTable styles = new StyleTable();
    final ScreenBounds bbox = new ScreenBounds();           // NDC min/max + projection snapshot
    boolean hasGeometry; boolean open;
    RenderType ensureMaskType(RenderType maskType);         // registers fixed buffer (cap 128)
}
record OutlineRuntimeSettings(boolean enabled, boolean firstPerson, boolean thirdPerson, boolean armor,
                       boolean dropped, boolean itemFrame, boolean gui, boolean other, boolean onlyLocalPlayer,
                       boolean disableWithShaders, boolean handWithShaderPack, boolean scaleWithResolution,
                       boolean proportional, float minThickness, float fadeStart, float fadeEnd) {
    static OutlineRuntimeSettings from(GlintsmithConfig cfg);   // the ONLY place that reads outline config fields
    boolean anyWorld();
    boolean accepts(OutlinePhase phase, OutlineTarget target);
    float thicknessScale(int framebufferHeight);                // proportional off: T × this (3.4)
    float itemThickness(float thickness, float pixelsPerUnit, int framebufferHeight);   // 3.4.1
    int outlineReachPixels(float thickness, float softness, int framebufferHeight, float pixelsPerUnit);
}
final class OutlineScale {                                      // 3.4.1, pure functions on JOML matrices
    static final float REFERENCE_PIXELS_PER_UNIT;               // about 807.6
    static float pixelsPerUnit(Matrix4fc projection, Matrix4fc modelToView, int framebufferHeight);   // NaN: no measure
    static float thickness(float thickness, float pixelsPerUnit, float minThickness, int framebufferHeight);
    static Matrix4f itemPose(Matrix4f out, Matrix4fc pose, @Nullable ItemTransform transform, boolean leftHand);
    static float guiPixelsPerUnit(double guiScale);
}
final class StyleTable {
    int indexFor(int argb, float thickness, float softness);   // 1..255, 0 = reject; opacity is in the alpha of argb
    int maxRadius();                                                          // ≤ 32
    void upload();                                                            // to its own DynamicTexture(256x2)
    int textureId();
    void reset();
}
final class MaskTypes {
    static @Nullable RenderType forSource(RenderType source, boolean depthTested);
    static boolean needsDefaultUv(RenderType source);
    static void clearCaches();
}
final class TeeBufferSource extends MultiBufferSource.BufferSource { /* see section 3.2, pooled */ }
final class TeeVertexConsumer implements VertexConsumer { /* real first, mask guarded */ }
final class MaskVertexConsumer implements VertexConsumer { /* EntityOutlineGenerator-like + bbox */ }
final class ScreenBounds { void snapshot(); void include(float x, float y, float z); boolean toPixels(int w, int h, int pad, int[] out4); void reset(); }
final class OutlineTargets { com.mojang.blaze3d.pipeline.RenderTarget mask(); com.mojang.blaze3d.pipeline.RenderTarget edt(); void ensureSize(com.mojang.blaze3d.pipeline.RenderTarget main); void tickIdle(boolean used); void release(); }
final class OutlineShaders { static @Nullable ShaderInstance mask(); static @Nullable ShaderInstance edt(); static @Nullable ShaderInstance composite(); static void markDirty(); }
final class FullscreenQuad { static void draw(ShaderInstance shader); static void close(); }
final class OutlineCompositor {
    enum MaskDepth { COPY, CLEAR, NONE }                    // how the mask target's depth is prepared
    static void composite(PhaseState phase, MaskDepth depth, boolean useDepth, float fadeStart, float fadeEnd);
    static boolean drawMaskOnly(PhaseState phase, boolean clear);   // Iris hand: mask now, composite later
    static void resolve(PhaseState phase);                          // passes over an earlier mask, UseDepth 1
}
final class OutlineDepth { /* constants and the depth rules as plain Java; mirrored by the shaders, tested on the JVM */ }
final class OutlineFailsafe { static boolean tripped(); static void trip(String where, Throwable t); }
// navrelis.glintsmith.render.outline.compat
final class IrisCompat { static boolean loaded(); static boolean shaderPackInUse(); static boolean shadowPass(); static void refreshPerFrame(); }
```

Mixins (T5): O1–O7, A1–A3 as in section 4. Handler pattern, which must be followed exactly:

```java
@Mixin(ItemRenderer.class)
abstract class ItemRendererOutlineMixin {
    @WrapMethod(method = "render(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IILnet/minecraft/client/resources/model/BakedModel;)V", require = 0)
    private void glintsmith$outline(ItemStack stack, ItemDisplayContext ctx, boolean left, PoseStack pose,
                                    MultiBufferSource src, int light, int overlay, BakedModel model, Operation<Void> original) {
        MultiBufferSource s = OutlineSystem.beginItem(stack, ctx, src);    // never throws (internal catch → src)
        try { original.call(stack, ctx, left, pose, s, light, overlay, model); }
        finally { OutlineSystem.endItem(s); }                             // never throws
    }
}
```

Resources (T5), under `src/main/resources/assets/glintsmith/shaders/core/`: `mask.json`, `mask.vsh`,
`mask.fsh`, `fullscreen.vsh`, `outline_edt.json`, `outline_edt.fsh`, `outline_composite.json`,
`outline_composite.fsh`. The contents are in section 3.5; JSON stage names use the `glintsmith_`
prefix that the resource provider remaps. The white texture is generated at run time
(`glintsmith:outline/white`, a 1x1 `DynamicTexture`).

Config fields T5 needs. `OutlineSettings.from` adapts to whatever T3 named them; T3, please provide
these:

* master `enabled`
* per target `firstPerson`, `thirdPerson`, `onlyLocalPlayer`, `armor`, `dropped`, `itemFrame`, `gui`,
  `other` (default off)
* `handWithShaderPack` (default true)
* `thickness` 0.5 to 16 (`OutlineSettings.MAX_THICKNESS = 16`; there is no `maxThickness` field), default 8.5
* `proportional` (2.0.0, default true), `minThickness` (2.0.0, default 1, range 0.5 to 8) and `scaleWithResolution`
  (default false, only used while `proportional` is off); see 3.4.1
* `targetThickness` (2.0.0): an optional own thickness per place (`firstPerson`, `thirdPerson`, `armor`, `dropped`,
  `itemFrame`, `gui`, `other`; null = use the global `thickness`). It is read by the rule resolution
  (`RuleMatcher`, section 3.2 step 8) and is not part of `OutlineRuntimeSettings`.
* `fadeStart` / `fadeEnd` (blocks, default 0/0 = off)

### 8.2 T6: glint system (`navrelis.glintsmith.render.glint`)

```java
public final class GlintSystem {
    public static void init();                                                         // T8 calls once
    public static RenderType substitute(MultiBufferSource.BufferSource source, RenderType requested); // G5a
    public static void afterEndBatch(MultiBufferSource.BufferSource source, RenderType type);         // G5b
    public static void onBatchDrawn(MultiBufferSource.BufferSource source, RenderType type);          // G5c (+ G5b for G9)
    public static void onFrameBegin();                                                                // G6
    public static RenderType substituteBuffered(RenderType requested);                                // G7
    public static void onAtlasUploaded(ResourceLocation atlas, SpriteLoader.Preparations p);          // G10
    public static void onGuiSource(MultiBufferSource.BufferSource source);                            // G11
    public static boolean isGuiSource(MultiBufferSource.BufferSource source);                         // G5a dispatch
    public static RenderType substituteGui(MultiBufferSource.BufferSource source, RenderType requested); // G11, G12
}
final class GlintAtlasScale { static float factor(int refAtlas, int atlas, int refSprite, int sprite);  // 5.4.1
                              static boolean update(int w, int h, int spriteW, int spriteH); static float u(), v(); }
public final class GlintContext {
    public static void pushItem(ItemStack stack, ItemDisplayContext ctx);   // G1
    public static void pushArmor(ItemStack stack);                          // G2, G3
    public static void pushBewlr(ItemStack stack, ItemDisplayContext ctx);  // G4
    public static void pop();
    static @Nullable Frame top();
}
enum GlintKind {
    GLINT(8.0f, false, RenderStateShard.RENDERTYPE_GLINT_SHADER, false, false),
    GLINT_TRANSLUCENT(8.0f, false, RenderStateShard.RENDERTYPE_GLINT_TRANSLUCENT_SHADER, true, false),
    ENTITY_GLINT(0.16f, true, RenderStateShard.RENDERTYPE_ENTITY_GLINT_SHADER, true, false),
    ENTITY_GLINT_DIRECT(0.16f, true, RenderStateShard.RENDERTYPE_ENTITY_GLINT_DIRECT_SHADER, false, false),
    ARMOR_ENTITY_GLINT(0.16f, true, RenderStateShard.RENDERTYPE_ARMOR_ENTITY_GLINT_SHADER, false, true);
    // (baseScale, entityFamily, shaderShard, itemEntityTarget, viewOffset)
    final RenderType vanilla;                         // captured: RenderType.glint() etc.
    static @Nullable GlintKind of(RenderType t);      // 5 identity compares, cheapest-first
}
enum GlintCategory { ITEM, ARMOR, ENTITY, GUI }
final class GlintParams { ResourceLocation texture; int tintArgb; float speed, scale, strength, rotationDeg, atlasU, atlasV;
                          boolean sameAs(GlintParams o); void copyFrom(GlintParams o); boolean isVanilla(GlintKind k); }
final class GlintSlot { final GlintKind kind; final int index; final RenderType type; final GlintParams params;
                        boolean inUse, registered; long frame;
                        void setupTexture(); void setupTexturing(); void clearTexturing(); }
final class GlintSlotPool { RenderType acquire(GlintKind k, GlintParams p); RenderType offType(GlintKind k);
                            void release(RenderType t); void releaseStale(long frame); void flushAll(MultiBufferSource.BufferSource main); }
record CategorySettings(boolean enabled, @Nullable String texture, @Nullable ColorStyle tint, float speed,
                        float scale, float strength, float rotation, GlintLook categoryLook) {}
record GlintSettings(CategorySettings item, CategorySettings armor, CategorySettings entity, CategorySettings gui, boolean anyCustom) {
    static GlintSettings from(GlintsmithConfig cfg);   // reads cfg.glint.item/armor/entity/gui
    CategorySettings get(GlintCategory c);
}
final class GlintTextures { static void init(); static void reload(ResourceManager rm); static void onConfigChanged();
                            static ResourceLocation resolve(@Nullable String spec, boolean entityFamily, boolean tinted); }
final class GlintPresets { static int[] generate(String name);   // the 12 generated presets (section 5.6)
                           static int[] neutral(int[] abgr); }
final class GlintNoise { /* section 5.6 helpers */ }
final class GlintFailsafe { static boolean tripped(); static void trip(String where, Throwable t); }
```

T6 data flow, step by step:

1. `init()`:
   * Register `ClientLifecycleEvents.CLIENT_STARTED`. Its handler captures
     `main = ((RenderBuffersAccessor) mc.renderBuffers()).glintsmith$getBufferSource()`, then calls
     `GlintTextures.init()` and builds the settings.
   * Register the reload listener (`GlintTextures.reload`).
   * `ConfigManager.addListener(cfg -> settings = GlintSettings.from(cfg); GlintTextures.onConfigChanged())`.
2. G1–G4 push and pop context frames around the vanilla calls.
3. G5a `substitute`:
   1. Fast exit: `GlintKind.of(requested) == null`, or `source != main`, or `!settings.anyCustom`, or
      tripped → return `requested`.
   2. Category (section 5.3) → settings → off? → resolve the look → fill scratch params → vanilla
      equivalent? → `pool.acquire`.
   3. Before returning a slot or off type, make sure it is registered in `main`'s fixed map.
4. G5b `afterEndBatch`: `if (source == main && type == GlintKind.ENTITY_GLINT_DIRECT.vanilla) pool.flushAll(main)`.
   `flushAll` calls `main.endBatch(t)` for every registered slot and off type.
5. G5c `onBatchDrawn` → `pool.release(type)`.
6. G6 `onFrameBegin` → `frame++`; `timeSeconds = Util.getMillis() / 1000.0`;
   `shaderPack = IrisCompat.queryShaderPack()`; `pool.releaseStale(frame)` (frees and unpins).
7. G7 `substituteBuffered(requested)` (Iris's world source): fast exit unless
   `bufferedSwapAllowed(anyCustom, main captured, !shaderPack, !tripped)` and a known kind on the render
   thread; then the same resolution as G5a step 2, with `pool.acquire(..., pin = true)` and without
   registering the slot in `main`.
8. Every public entry point catches Throwable → `GlintFailsafe.trip` → vanilla behaviour.
9. G10 `onAtlasUploaded` (block atlas only) → `GlintAtlasScale.update(width, height, reference sprite)`.
   In step 3.2, after the vanilla-equivalence check, `settings.atlasScale && blockAtlasUvs(kind, frame,
   decalFoil)` → `scratch.setAtlas(u, v)` (5.4.1).
10. G11 `onGuiSource` keeps ImmediatelyFast's non-batching GUI source; G5a calls `substituteGui` for it,
    G12 for the HUD source: step 3 with `source` instead of main (registration in its fixed map,
    `containsKey` first), not pinned (5.2, point 8).

Mixins (T6): G1–G8, GA1, GA2. Later: G9 (1.0.0), G10–G12 (2.0.0). Resources (T6): none. Presets are generated at run time;
the user folder `<configDir>/glintsmith/glints/` is created if missing.

### 8.3 T8: exact wiring

1. `Glintsmith.onInitializeClient()`, after T3's `ConfigManager` is initialised:

   ```java
   OutlineSystem.init();
   GlintSystem.init();
   ```

   Each `init()` registers its own Fabric events, reload listener and config listener. Each catches
   its own exceptions and trips its own failsafe, so one feature failing does not affect the other.
2. `glintsmith.mixins.json`: set `"defaultRequire": 0` (recommended) and put all entries in the
   `"client"` array:

   ```
   outline.ItemRendererOutlineMixin, outline.ItemInHandRendererOutlineMixin, outline.HumanoidArmorLayerOutlineMixin,
   outline.ElytraLayerOutlineMixin, outline.GuiGraphicsOutlineMixin, outline.BufferSourceOutlineMixin,
   outline.GameRendererOutlineMixin, outline.CompositeRenderTypeAccessor, outline.CompositeStateAccessor,
   outline.EmptyTextureStateShardAccessor,
   glint.ItemRendererGlintMixin, glint.HumanoidArmorLayerGlintMixin, glint.ElytraLayerGlintMixin,
   glint.BlockEntityWithoutLevelRendererGlintMixin, glint.BufferSourceGlintMixin, glint.GameRendererGlintMixin,
   glint.ItemInHandRendererGlintMixin, glint.BufferSourceAccessor, glint.RenderBuffersAccessor,
   glint.TextureAtlasGlintMixin (G10)
   glint.compat.IrisFullyBufferedGlintMixin (G7; @Pseudo, so it is skipped without Iris)
   glint.compat.ImmediatelyFastBufferSourceGlintMixin (G9; @Pseudo, skipped without ImmediatelyFast)
   glint.compat.ImmediatelyFastGuiSourceGlintMixin, glint.compat.ImmediatelyFastHudGlintMixin
   (G11, G12; @Pseudo, skipped without ImmediatelyFast)
   ```
3. No access widener and no `build.gradle` change are needed. Fabric API's transitive access wideners
   cover the render-state classes, and the accessors cover the rest.
4. The config screen should show, when `IrisCompat.shaderPackInUse()`, a note: "World outlines are
   disabled while a shader pack is active".

---

## 9. Risks and open questions (each with a recommendation)

1. **`RuleEngine.resolve` cost and allocations.** It is called per item render (hundreds per frame
   with an open inventory) and returns records.
   *Recommendation:* T3 returns a shared `ResolvedStyle.NONE` constant when nothing matches, memoises
   by (item, components hash) with a small LRU, and keeps `outlineColor` / `glintTint` allocation-free.
2. **Iris hand outlines** depend on Iris's hand matrices still being current at TAIL of
   `renderHandsWithItems`, and on the main target holding the final image at `renderLevel` RETURN.
   Both are supported by bytecode inspection (HandRenderer sets the projection, calls
   `renderHandsWithItems` with an unflushable wrapper and flushes right after; the hand renders inside
   `LevelRenderer.renderLevel`; `finalizeLevelRendering` runs at its RETURN).
   Resolved: the arm hiding the outline is done by hand occluders (3.3), and the feature ships on by
   default with `handWithShaderPack` as an off switch. Verified in game with Iris 1.8.14-beta.1 and
   Complementary Unbound with Euphoria Patches: hand outlines stay on, the arm hides them, no outline is
   traced along the sleeve. Remaining limits: two Iris hand passes share one mask depth, hand geometry
   drawn outside the passed buffer source does not occlude, and packs that move or distort the hand
   (DOF, hand sway) may misalign the outline.
   *Recommendation:* keep as is; test BSL and a DOF pack if users report problems.
3. **World outlines under shader packs** are off. Making them work would need either rendering the
   mask from Iris's depth texture or an Iris API that 1.21.1 does not have.
   *Recommendation:* accept for v1. Revisit with Iris's depth texture via its internals only if users
   ask; this would be fragile.
4. **Glint customisation in world and hand under shader packs.** G7 (`@Pseudo` into Iris's
   `FullyBufferedMultiBufferSource`) is implemented, because Iris uses that source for the world even
   with shaders off (5.1). It is a no-op while a shader pack is in use, so the pack behaviour is
   unchanged. No `IMixinConfigPlugin` version guard: `@Pseudo` and `require = 0` make it inert without
   Iris or without the method, and the hook only relies on "one `getBuffer(RenderType)`, drawn later in
   the same frame", which the pinned slots tolerate.
   *Recommendation:* after the in-game check of the shaders-off world glint, try lifting the shader-pack
   gate (test armor, entity-held items, item frames and the first-person hand with Complementary).
5. **The mask cannot see geometry drawn outside `MultiBufferSource`** (direct Tesselator draws in some
   mod BEWLRs).
   *Recommendation:* accept and document. GL-level interception would be far worse.
6. **Outline drawn onto a non-outlined occluder** is handled by the depth rules in WORLD and vanilla
   HAND (3.3): hidden item pixels are no seeds, and an outline over a hidden item pixel needs the
   occluder to be clearly behind the item. In GUI, overlapping neighbours simply overdraw each other in
   draw order.
   *Recommendation:* fine as is.
7. **Thickness on high-DPI and 4K screens** is in physical pixels, so it looks thinner at 4K.
   *Recommendation:* T3 adds an optional `scaleWithResolution` flag (thickness × framebufferHeight /
   1080). Decided (2.0.0): the outline width is proportional to the item's size on screen by default (3.4.1), measured
   on the first-person held item at a window height of 1080, for every phase (WORLD, HAND, GUI, PREVIEW). With
   `proportional` off the old rule applies: one factor for every phase, and the GUI scale plays no part, because world
   and hand outlines cannot follow it and a GUI-only factor would make the same look wider or thinner in the GUI (3.4).
8. **"Only local player"** semantics for armor.
   *Recommendation:* one flag applies to both THIRD_PERSON held items and ARMOR. The lead confirms or
   asks for two flags.
9. **Texture spec grammar** (section 5.5) must match what T3 validates and what the UI offers.
   *Recommendation:* T3 adopts this grammar verbatim: `default`, `preset:<name>`, `file:<name>.png`,
   `<ns>:<path>`.
10. **The `GlintLook` merge semantics** (look values replace category values; rotation always from the
    category) are an assumption.
    *Recommendation:* T3 documents that `GlintLook` values are already merged with category defaults.
11. **Memory of full-resolution RGBA8 targets** (about 25 MB at 1080p).
    *Recommendation:* keep the idle release. Phase 2 could switch to custom R8 and RG8 FBOs (a 75 %
    saving).
12. **Sodium WARN line** for our consumer class.
    *Recommendation:* accept. Phase 2 could add an optional Sodium `VertexBufferWriter` implementation
    in a compat class loaded only when Sodium is present.
13. **Stencil main framebuffers** cause a failed depth blit.
    *Recommendation:* detect once and disable world and hand outlines, as specified.
14. **GLSL portability** (`texelFetch` on a depth `sampler2D`, int loops with uniform bounds). This is
    valid GLSL 1.50 **[UNVERIFIED on old Intel or macOS drivers]**.
    *Recommendation:* compile failures already degrade cleanly; test on macOS once.
15. **Item depth in the mask must match the item's depth in the scene.** The seed rule (3.3) treats an
    item pixel as visible only if the scene depth is at most 32 steps of 24-bit depth in front of the
    mask's item depth. The item's main draw and the mask draw therefore have to produce the same depth
    within that window: same vertex expression, same matrices, same layering. If they do not for some
    source type, the item looks hidden and loses its outline (fully or in part).
    *Recommendation:* the mask copies the layering of its source type. Check new item types in game; if
    a type fails, widen `SEED_EPSILON` or copy more of its state. **[UNVERIFIED for mod render types]**

---

## 10. Manual in-game test checklist

Setup: dev client with Fabric API only. Then repeat the marked rows with Sodium 0.8.13; with Iris
1.8.14 and shaders off; and with Iris plus Complementary Reimagined (shaders on). Video setting
Fabulous for the rows marked **F**. Fabulous outline rows (7, 14) were tested in game without
shaders: passed.

Outlines:

1. An enchanted diamond sword in the main hand (first person): outline around the visible sword only,
   with no box around transparent texels. Thickness 1, 4 and 8 px looks right; softness 0 is crisp,
   softness 1 is a glow.
2. An enchanted shield (BEWLR) and trident in first person; then a 3D resource-pack sword model; then
   a modded item with a builtin renderer: all outlined correctly.
3. A bow while drawing, a crossbow charged, a spyglass in use, an offhand totem: outlines follow the
   animation. A filled map is not outlined (expected).
4. Fire overlay or underwater: the outline sits under the screen-effect overlay.
5. F5 third person: your own held item is outlined; the outline is hidden where your arm is in front.
   A zombie holding an enchanted sword: outlined (with "only local player" off), not outlined (with it
   on).
6. Armor stand and player with full enchanted netherite armor plus trims: not outlined by default (the
   "armor" target is off by default). With the "armor" target switched on, armor silhouettes are
   outlined; the body behind shows the outline; switching it off again removes it. Elytra
   (enchanted) is outlined with the "armor" target on, and not outlined with it off.
7. A dropped enchanted book and a dropped enchanted block item: outlined. Behind a wall: **not**
   visible. Half behind a wall corner: outline only around the visible part, none on the wall. Under
   water: visible through the water surface (Fast and Fancy) **F** (tested in game, passed).
8. An item frame with an enchanted pickaxe; a glow item frame; a campfire with items (FIXED → frame
   target).
9. Several outlined items with different rules on screen at once (rainbow, pulse, per-enchantment,
   durability alert): each keeps its own colour and thickness, and animation is smooth.
10. GUI on: hotbar, inventory, chest, creative tab, a tooltip over an outlined item (the tooltip covers
    the outline), dragging a stack, item count text over the outline. GUI off: none.
11. Resize the window, toggle fullscreen, change GUI scale, minimise and restore: no stretched or
    offset outlines, no errors.
12. F3+T resource reload and resource-pack change: outlines come back (shaders recreated).
13. Iris shaders on: world outlines are off (one INFO line), hand outlines on (option off works), GUI
    outlines on, no errors in the log. Toggle the shader pack while in game. Hand part passed in game
    (Iris 1.8.14-beta.1 + Complementary Unbound with Euphoria Patches): hand outlines stay on, the arm
    hides them, world outlines off with one INFO line, GUI outlines work.
14. Fabulous **F**: dropped stained glass block (`itemEntityTarget`), translucent items, rain in
    front of an outlined item (rain over the outline). Tested in game, passed (stained glass seen
    from the front and side).
15. Performance: an empty world with nothing outlined shows no FPS difference against Glintsmith with
    all features off. With 50 enchanted dropped items on screen, check the F3 frame time.
16. Failsafe: in a debug build, force an exception in the compositor (system property
    `glintsmith.debug.failOutline=true`). One ERROR line, outlines stop, the game keeps running, and
    glint is unaffected.

Glint:

17. Each category set to `preset:rainbow`, `aurora`, `fire`, `frost`, `sparkle`, `gold`, `void`, `inferno`, `nebula`,
    `starlight`, `opal`, `storm`, a `file:` PNG from
    the config folder, and a resource-pack texture: correct texture on items in hand, GUI, dropped,
    armor, trident (thrown and held) and shield.
18. A tint on the default texture switches to the neutral variant and the colour matches the chosen
    hue (not muddy). Animated tint is smooth with no stutter. Per-enchantment tint differs between two
    books.
19. Speed 0 (static), 1, 3; scale 0.5 and 2 (pattern larger at 2); rotation 0, 10, 45; strength 0.2
    and 1. The vanilla Glint Speed and Strength sliders still scale the result.
20. Disable each category: GUI (no glint in inventory, but glint in hand), item, armor, entity
    (thrown trident and shield have no glint).
21. Fabulous **F**: glint on translucent items (`glintTranslucent` path) and on items seen through
    water.
22. 40 different glint looks on screen (creative inventory full of tinted books): no flicker, no
    wrong looks; at most a debug log line about slot overflow in the world.
23. Iris shaders on: GUI glint customised; world and hand vanilla glint (G7 is a no-op under a pack);
    no visual breakage, no errors.
24. Resource reload with a pack that replaces `enchanted_glint_item.png`: the neutral variant is
    rebuilt from the new texture.
25. Punchy 2.8f installed: an enchanted sword in the main hand has a first-person outline and the
    first-person glint rules; the log (logger `Glintsmith/Outline`, DEBUG) shows the hand-items line once.
    Items Punchy draws without `ItemRenderer.render` have no outline (expected).
26. Gap fix, AUTO on Windows or Linux: the 1 px step-corner cracks of a generated item (for example a
    dropped diamond sword) are closed and no jagged slivers show along the step edges.
27. Iris 1.8.14 installed, shaders **off** (G7), with custom presets and a tint per category: enchanted
    armor on yourself in F5 and on an armor stand, an enchanted sword held by a zombie or another player,
    an item frame with an enchanted pickaxe, a dropped enchanted book. All show the custom look (not the
    vanilla purple), the glint sits on the item (no glint drawn before or beside its base), and 40+
    different looks in the world fall back to vanilla only past 32 per kind (debug log). Switch a pack on
    and off again in game: world glint turns vanilla under the pack and custom again without it.
    Verified in game (Iris 1.8.14-beta.1, shaders off) for armor stands and the showcase stand: every
    preset shows in its colour against the purple vanilla glint. Under Complementary Unbound the world
    glint stays vanilla, GUI and hand outlines work, world outlines stay off. Not checked: zombie or
    player held items, item frames, dropped items.
28. ImmediatelyFast 1.6.15 installed (G9), shaders off, Item layer with a preset at Scale 0.13 and a
    RAINBOW tint: the enchanted sword in the first-person hand shows the custom look (the same colours
    as that look in an item frame, not the uniform purple vanilla wash), also with Punchy. Without Iris,
    armor stands and item frames show the custom look too. Passed in game 2026-10-18 (V1 run, ImmediatelyFast
    1.6.15, Sodium 0.8.13, Iris 1.8.14-beta.1 with shaders off, Punchy 2.8f) for the first-person hand with Punchy:
    custom look; armor stands and item frames were not recorded separately in that run. (Before 2.0.0 the GUI glint
    stayed vanilla here; see 30.)
29. Atlas-independent scale (2.0.0, 5.4.1), in a pack with a large block atlas (the log line
    `Glint atlas factor 0.125 x 0.125 (block atlas 8192x4096, ...)` after loading and after F3+T), Item layer
    `preset:rainbow` at Scale 1.00: with the option on, the held, dropped and framed sword shows the
    rainbow bands as densely as in a vanilla-size atlas; off, the 1.0.0 flat wash. Armor (F5) looks the
    same on and off. With every layer at its defaults and no rule glint, the glint is vanilla whatever the
    option. Passed in game 2026-10-18 (V1 run, block atlas 8192 x 4096, log line `Glint atlas factor 0.125 x 0.125`):
    option on, Scale 1.00 shows the preset pattern on held items at vanilla density; option off, the 1.0.0 flat wash;
    armor unaffected. The rule "glint stays vanilla when nothing is customised" was not looked at separately.
30. ImmediatelyFast 1.6.15 (G11, G12), GUI layer with a preset: enchanted items in the inventory, a chest,
    the creative tabs and the hotbar show the custom look; the hotbar sprite, item counts and durability
    bars stay intact (no erased hotbar, the T20 rule), also with GUI outlines on. Without
    ImmediatelyFast the GUI glint is unchanged. Passed in game 2026-10-18 (V1 run, ImmediatelyFast 1.6.15, Sodium
    0.8.13, Iris shaders off, Punchy): the inventory and the hotbar show the custom GUI glint (fire preset), not the
    vanilla purple, and the hotbar is intact. A chest and the creative tabs were not looked at separately; the
    without-ImmediatelyFast case was not repeated.
31. Live preview (2.0.0, section 12), in a world with an enchanted sword in the main hand and the settings screen
    (`/glintsmith`) open. a and b passed in game 2026-10-18 (V1 run, ImmediatelyFast 1.6.15, Sodium 0.8.13, Iris
    shaders off, Punchy) on the Outline and Glint tabs with the held enchanted netherite sword: the panel stands right
    of the options, shows the sword with its outline and glint, animated, nothing overlapping or clipped, and it
    follows the unsaved settings on screen. Still to be checked by hand: dragging and right click reset (a), c (rule
    editor and colour picker), d (source strip), e (narrow window, GUI scale 4) and f (shader pack):
    a. The panel stands right of the options at GUI scale 2 and a 1920 x 1080 window, shows the sword in first-person
       look (upright, centred, not clipped, outline not cut off at the frame), turns slowly, and a drag turns it by
       hand; after a release it resumes after about 1.5 s; right click resets the view.
    b. Changing a colour, thickness, the glint texture or tint in the tabs changes the preview at once; typing a texture
       spec (`preset:fi`, `preset:fire`) shows no WARN lines in the log. The held sword in the world is unchanged until
       Done. Cancel and discard leave the saved look untouched.
    c. Open a rule that matches the sword, change its outline colour or glint, open the colour picker from it: the
       preview follows the unsaved rule and the unsaved pick; Cancel in the picker or the editor takes it back. The
       rule editor note shows for a rule that does not match or has no first-person target.
    d. Source strip: held item / sample sword (kept after closing the screen); empty hand shows the sword and the note;
       a filled map shows no outline and a note; the title screen (Mod Menu) shows a plain glinting sword and the
       `No world loaded` note. Master outline switch off, first person off: the notes say so.
    e. Narrow window or GUI scale 4: the panel shrinks, moves next to the footer buttons of the main screen and is
       hidden on the sub screens. F1 and a resize do not break it.
    f. Repeat with Iris and a shader pack (notes `Shader pack: ...`, outline and glint still appear), with
       ImmediatelyFast (no erased hotbar behind the screen, glint in the preview) and with Punchy.
32. Proportional outlines (2.0.0, section 3.4.1) and the new presets. a, b and e passed in game 2026-10-18 (V1 run,
    ImmediatelyFast 1.6.15, Sodium 0.8.13, Iris shaders off, Punchy); c (1440 and 2160 px windows) and d (300 colours)
    were not checked:
    a. With Proportional on (default), thickness 8.5: the held sword has the band of the old first-person look; a
       dropped item gets a thinner band the farther away it is; inventory and hotbar icons get a thin line, never
       thinner than Minimum width; armor on a nearby player or armor stand is in proportion to its size.
    b. Proportional off: every item has the same band in screen pixels, the GUI included, at any GUI scale; "Scale
       with window size" is active only then and Minimum width only while Proportional is on.
    c. A window of 1440 or 2160 px height: the held item's band grows with the window, the minimum width too.
    d. 300 distinct enchantment colours or widths in view (a chest full of enchanted books at several distances): no
       item loses its outline while its colour is on screen; no errors in the log.
    e. All twelve presets on the item layer, with and without tint: the whole item shimmers, no patches of glint on
       black, no seams; inferno rises, storm looks like lightning, stars and sparkles are not stretched. Passed: all
       twelve presets and neutral with a rainbow tint show a pattern over the whole item, animated, in first person; fire
       on armor shows the fire pattern. On armor every preset reads as a slow colour wash because of the vanilla armor
       glint scale (by design). Known, not fixed: in the Nytheria pack a 3D mace model from a resource pack keeps a
       purple glint on its handle while the head shows the custom glint (not investigated).

---

## 11. Model gap fix: AUTO values

Package `navrelis.glintsmith.modelfix` (`GapFix`), settings in `config/GlintsmithConfig.java`. The fix
addresses the hairline gaps between the faces of generated item models (Mojang bug MC-73186). Three
values act on it: the UV shrink multiplier on the block atlas, the side expansion and the side indent
(model units, 1/16 block), plus the `preciseSpans` switch.

| Mode | UV shrink multiplier | Side expansion | Side indent | Precise spans |
|---|---|---|---|---|
| AUTO, Windows and Linux | 0 | 0.005 | 0.0001 | off |
| AUTO, macOS | 1.0 (vanilla shrink) | 0 | 0.0099 | off |
| CUSTOM | stored value, default 0 | stored value, default 0.005 | stored value, default 0.0001 | stored value, default off |

* The CUSTOM defaults mirror AUTO on Windows and Linux.
* macOS keeps vanilla's UV shrink and a larger indent, because its drivers bleed without the shrink.
* UV shrink 0 on the other platforms keeps the front quad's texels exactly on the side quads, but on its own it
  bleeds: pixels on the front quad's outer edge sample the neighbouring sprite of the atlas, and the thickness edges of
  side quads sample the neighbouring texel or sprite. Each leaves a single wrongly coloured pixel that then gets its own
  outline ring. The edge guard below fixes that (2.0.1); the AUTO values are unchanged.
* On the other platforms the side quads grow by 0.005 on both in-plane axes. An in-game sweep showed that
  0.005 closes the 1 px step-corner cracks of generated item models, while 0.01 and above left jagged
  slivers along the step edges.
* Side elements keep vanilla's corner order: `GapFix.adjustSideElement` never sorts `from` and `to`. The
  `16 - y` flip in vanilla's `createSideElements` makes the baked quads face outwards, and the signs of
  the expansion and indent depend on that order.
* AUTO is switched off while the mod `modelfix` is installed; CUSTOM stacks on top of it and precise spans
  are inactive (README, section Compatibility).

### Edge guard (2.0.1)

Always on while the gap fix is enabled (AUTO and CUSTOM, every platform); no setting. Constants in `GapFix`, applied in
`ItemModelGeneratorMixin`:

* `EDGE_GUARD_TEXELS` = 1/64 texel. `processFrames`: the layer element (front and back face) is pulled in by that much
  on all four in-plane sides, and both faces' UVs by the same amount (`guardLayerElement`). Position and UV move
  together, so every texel stays where the side quads expect it; only a 1/64 texel sliver at the sprite border is cut.
* `createSideElements`: side elements on the sprite border (EAST at x = 0, WEST at x = 16, UP at y = 16, DOWN at y = 0,
  checked on vanilla's coordinates) move inwards by the same 1/64 texel, so they meet the cut front face and no rim gap
  opens (`isOnSpriteBorder`, then `adjustSideElement` with indent only, before the regular expansion and indent).
* `SIDE_THICKNESS_INSET_TEXELS` = 0.25 texel: the UVs of every side face move inwards across the item's thickness only
  (`insetSideThickness`; v for UP/DOWN, u for EAST/WEST). That axis samples one texel row or column, so no colour
  changes. The UVs along the edge stay exact: an inset there opened cracks in the studio (+25 % crack pixels at 1/256).
* Why geometric and not a smaller multiplier: the multiplier's inset is `m * 32 / atlas size` texels at the front edge,
  so it shrinks on large atlases, and every m > 0 misaligns the front texels (cracks). Why 1/64: 1/256 already measured
  clean on every atlas; 1/64 leaves margin for float precision on 16k atlases (a UV ulp near 1.0 is about 0.001
  texel there). Without the border indent, 1/64 left a visible 1 px line along opaque borders in close-ups.
* Measured in the studio (2026-10-18; lineup scene, 120 frames: frames with stray pixels; crack scene, 60 perspective
  stills of six items at 800 x 800: background pixels a 3 x 3 closing fills):

| Variant | Strays, atlas 2048 | Strays, 8192 | Strays, 16384 | Crack px (2048) |
|---|---|---|---|---|
| Gap fix off | 0 | | | 8329 |
| AUTO without guard (2.0.0) | 20 | 47 | 94 | 1994 |
| Multiplier 1.0 (vanilla shrink) | 0 | 4 | 14 | 5531 |
| Multiplier 0.125 | 0 | 4 | 87 | 2136 |
| AUTO with edge guard | 0 | 0 | 0 | 1991 |

Large atlases came from a test resource pack with one 4096 or 8192 px block texture (atlas 8192 x 4096 and
16384 x 8192).

---

## 12. Live preview (2.0.0)

The settings screen draws the item in the player's main hand as it looks in first person under the settings that are
on screen but not saved yet. The preview does not run the live pipeline on a copy of the world: it renders one item
through the real `ItemRenderer.render` into the real glint and outline systems, with the settings of all three
systems swapped for the duration of that one call. There are no mixins and no shaders of its own. Packages
`navrelis.glintsmith.render.preview` (rendering) and `navrelis.glintsmith.gui.preview` (panel, state, layout).
The panel, its position and look, the animation, and the following of unsaved settings were checked in game on
2026-10-18 (section 10, item 31 a and b); the rest rests on unit tests and on reading the code. What is still to be
checked by hand is listed in 12.7 and in section 10, item 31.

### 12.1 Mechanism

`PreviewRenderer.draw(GuiGraphics, x0, y0, x1, y1, stack, snapshot, yaw, pitch, margin, outline)` per frame (`margin` is a
`Margin` function from the item's scale to the room for the outline, 12.4):

1. The model is `ItemRenderer.getModel(stack, level, player, 0)`; the context is `FIRST_PERSON_RIGHT_HAND`, so the
   rules, the glint category and the outline target classify the item exactly like the item in the hand (FIRST_PERSON,
   category ITEM). The display transform of that context (`translate · rotationXYZ · scale`) is cancelled again by
   `PreviewPose.applyInverse` on the pose, so the item is shown upright and centred, not as held. A transform that
   cannot be inverted (a scale axis below 1e-4, a non-finite component; `PreviewPose.invertible`) falls back to
   context `NONE` without cancelling.
2. The size comes from the item's real bounds: `PreviewBounds.measure` renders the item once into a recording
   `MultiBufferSource` that keeps only vertex positions (an item that throws or draws nothing gives the unit cube).
   Every `getBuffer` call hands out a recording view of its own, because an enchanted item joins its glint and main
   buffers with `VertexMultiConsumer.create`, which throws on two identical delegates (2.0.0 and 2.0.1 handed out one
   consumer, so every enchanted item got the unit cube and the held trident, which sits far from the origin, was drawn
   partly below the panel). The result is cached per (item, model identity, context), at most 16 entries, cleared when the
   settings screen closes (`PreviewRenderer.clearCache`). The measuring render is skipped while an outline phase is
   open (unit cube for that frame), because it would be captured.
3. `PreviewPose.fit` returns the largest scale at which the bounds stay inside the box less the margin for every yaw
   at the current pitch (the radius of the x/z extent against the half height, mixed by the pitch) and within +-110 GUI
   units of depth, at least 1. The turntable is `scale(s, -s, s) · pitch(X) · yaw(Y) · translate(-centre)` on a pose
   at the box centre with z = 150 like GUI items, so the item stays between z 40 and 260. The pose is a private
   `PoseStack` seeded with the GUI pose, so an item renderer that throws cannot unbalance `GuiGraphics.pose()`.
4. `g.flush()`, scissor to the box, `Lighting.setupForEntityInInventory()`; the item is drawn into
   `GlintSystem.previewSource()`, the game's main source (not `g.bufferSource()`): the glint swap (G5a, and G9 for
   ImmediatelyFast's subclass) only acts on that source. After the render the source is flushed with the depth test
   off, the way `GuiGraphics.flush()` does it.
5. Three scopes surround exactly that one render and are closed in reverse order in `finally`:
   * `RuleEngine.beginScope(snapshot)`: every `resolve`, `outlineColor` and `enchantmentColorsOf` call reads the
     snapshot. Scoped lookups use their own caches (`SCOPE_STYLES`, `SCOPE_COLORS`, 256 entries each, cleared when the
     snapshot instance changes) and never touch the live caches or the one-entry fast path, so the world render
     after the preview still hits its cached results.
   * `GlintSystem.beginPreview(snapshot)`: `substitute` takes its `GlintRuntimeSettings` from
     `active(previewSettings, settings)`, built once per snapshot instance. The atlas factor reaches the renderer only
     through those settings (5.4.1), so the preview shows the option "Atlas-independent scale" like the hand. The
     texture manager is put in quiet mode, so a texture spec that is being typed (invalid most of the time) logs no
     warnings. The other entry points (`substituteBuffered`, `substituteGui`) keep the live settings; the preview never
     draws into Iris's or ImmediatelyFast's GUI sources. `GlintSystem.onFrameBegin` clears the preview settings as a
     safety net.
   * `OutlineSystem.beginPreview(snapshot)`: opens the PREVIEW phase (12.2), unless the caller asked for no outline
     (a filled map) or the failsafe has tripped; it never nests into an open phase. The live settings do not gate the
     phase: the scoped rule engine decides (outline master switch, first-person target, rules).
6. A throw anywhere in `draw` sets `PreviewRenderer.disabled`, logs one ERROR line (`Glintsmith/Preview`: "Preview
   disabled for this session after an error") and leaves everything else alone; the scopes are closed by their
   `finally`. The outline and glint failsafes stay separate: a tripped `OutlineFailsafe` still draws the item and
   the glint and shows the note `unavailable`.

### 12.2 The PREVIEW outline phase

`OutlinePhase` gets a fifth value, `PREVIEW`, with its own lazily created `PhaseState` (the section 3.1 table has the
other phases). `OutlineSystem.beginPreview` opens it and `endPreview` closes it; both are called from `PreviewRenderer`
only, and the phase needs no mixin:

* `OutlineContexts.forPhase(PREVIEW, any context)` is FIRST_PERSON; `phaseAccepts(PREVIEW, t)` holds only for
  FIRST_PERSON. `capture` skips the live `accepts` gate for this phase (the live settings are not asked); the style
  comes from the scoped rule engine.
* The thickness rules are those of the snapshot (`PhaseState.settings` = `OutlineRuntimeSettings.from(snapshot)`, cached
  per snapshot instance): `itemThickness`, section 3.4.1. With `proportional` the preview item is measured like any
  other item from its pose in the preview box, so it gets the width its size on screen calls for; with `proportional`
  off, `thicknessScale(height)` of section 3.4 applies ("scale with window height" works as in the hand).
* The composite is the GUI one: a flat mask without scene depth (`MaskDepth.NONE`, no depth test, no distance fade),
  clipped to the current scissor box, which is the preview box. There is no depth copy, no occluder and no
  shader-pack special case. `finishItem` flushes the item's buffer source at the end of the render for PREVIEW as for
  GUI items (the ImmediatelyFast rule of section 6).
* `onFrameEnd` discards leftover geometry of the phase; `onClientStopping` closes it.
* The signatures of `world`, `open(PhaseState)`, `compositeWorld`, `PhaseState.open` and `discard()` are unchanged and
  no `Util.getMillis()` call was added to `onFrameBegin` or `time()`: the dev studio reflects on or redirects them
  (`docs/STUDIO.md`).

### 12.3 The snapshot and the pending edits chain

The renderer never sees a `GlintsmithConfig` that a screen is still editing. `PreviewSnapshot.update(live)` serialises
the config with `ConfigManager.toJson`; if the JSON equals the last one it returns the cached instance, otherwise
`ConfigManager.fromJson(json).sanitize()`. The snapshot is therefore detached (later edits cannot change a render in
progress), sanitised the way a saved config is (out-of-range values clamped, invalid ones repaired), and keeps its
identity while nothing changes; every scoped cache above is keyed by that identity. If serialising throws, the previous
snapshot stays (a `copy()`, or defaults, if there was none). Cost: one Gson `toJson` per frame while a panel is
visible; a 20 Hz throttle is the planned answer if that shows up in a profile.

Edits that are not in the working config yet (rule editor, colour style screen, colour picker) are written in only for
the moment the snapshot is taken. Each of these screens implements `PreviewHost`:

```
PreviewSession previewSession();
void withPendingEdits(Consumer<GlintsmithConfig> action);   // action sees the working config with every pending edit written in
default Component previewNote(ItemStack shown)               // a note of the screen itself, or null
```

The root `GlintsmithScreen` is the base of the chain: its `withPendingEdits` passes the working config, which already is
the edited copy. `RuleEditScreen` calls its parent's `withPendingEdits` and inside it
`PendingEdits.withRule(cfg, original, edited, action)`: the edited rule replaces the original (found by identity) or is
appended when it is new, `cfg.rules` is swapped for a new list and the old list reference is restored in `finally`.
`ColorStyleScreen` and `ColorPickerScreen` do the same for one value with `PendingEdits.withValue(get, set, pending,
action)`, through a raw getter and setter that `SettingRows` and `OutlineTab` pass in (a screen opened without them
shows no pending edit). The chain nests (picker, style screen, rule editor, root); every level restores in `finally`,
innermost first, also when the action throws. Cancel needs no undo, because nothing was written permanently.

`PreviewSession` is created once per root screen and handed down to every sub screen, so the turntable keeps its angle
across screens. It holds the yaw (30 degrees per second, a step of at most 100 ms, paused for 1.5 s after a drag), the
pitch (starts at 15, dragging moves it within +-60), the snapshot, and the notes (recomputed only when the snapshot,
the shown stack, the shader pack state or a fallback flag changes). The choice held item / sample sword is a static
field of the session class: it survives closing and reopening the screen for the rest of the game.

### 12.4 What is shown

* Held item: `PreviewItems.held`, the main hand; with an empty hand the sample sword and the note `empty_hand`.
* Sample sword: a netherite sword with Sharpness V, Unbreaking III and Mending, built once per registry access
  (`level.registryAccess()`, else the connection's). Without a world, or when no enchantment can be applied, a plain
  netherite sword with `ENCHANTMENT_GLINT_OVERRIDE` is used, so the glint still shows; note `no_world`.
* A filled map is shown without an outline (it is not outlined in the hand either) and the note `map`.
* Notes (`PreviewNotes.keys`, pure Java, in priority order): `unavailable` (outline failsafe tripped or preview
  disabled); one reason why there is no outline, which exclude each other (`outlines_off`, `first_person_off`,
  `no_outline`, `map`); `shader_hand_off` (shader pack active and the hand phase not allowed under it); `shader_glint`
  (shader pack active and the item has a custom glint); `no_world`; `empty_hand`. The panel shows the first note under
  the item name and lists all of them in its tooltip. The rule editor adds `rule_not_first_person` (the rule has
  targets but not first person) or `rule_no_match` (the edited rule does not match the shown item) through
  `previewNote`.
* The margin around the item, in GUI pixels, leaves room for the outline:
  `ceil(reach / guiScale) + 2`, with `reach = OutlineRuntimeSettings.outlineReachPixels` (the style-table radius), or
  2 without an outline. With `proportional` the reach grows with the size of the item, which in turn depends on the
  margin; the margin is therefore computed for the item fitted without margin (an upper bound of its size), and the
  smaller item that then fits has room to spare.

### 12.5 Panel and layout

`PreviewPanel` is a widget that the screens draw after the option list, because the list paints its background over
the whole width and would cover it. It draws the frame, the item, a two-line caption (hover name, first note) and the
strip "◀ Held item ▶" / "◀ Sample sword ▶". A left drag turns the item, a click on the strip switches the source (UI
click sound), a right click resets the view; a release outside the panel is found by polling the mouse button. It never
takes keyboard focus. If `PreviewRenderer.available()` is false it draws the frame and the note `unavailable` only.
`PreviewLayout` (pure Java, GUI pixels):

| Mode | Where | Rule |
|---|---|---|
| SIDE | right of the 310 px option column: x = width/2 + 177, y = header + 4 | width = min(180, width - 6 - x), hidden below 72. Item box height = min(width, available - 22 caption - 14 strip - 4); below 40 the strip is dropped, then the caption; hidden below 40 |
| FOOTER | main screen only, when SIDE does not fit: bottom left, x = 4 | a square of min(space left of the centred footer rows - 8, footer height - 8); hidden below 32; no caption, no strip |
| HIDDEN | too small | the panel is not drawn and takes no input |

The main screen tries SIDE, then FOOTER; the rule editor, the colour style screen and the colour picker try SIDE only.
Resizing and GUI scale changes lay it out again; the session survives. `GlintsmithScreen` clears the bounds cache when
it closes (Done, or discarding on Cancel).

### 12.6 Limits (known and by design)

* **Model gap fix** is not previewed: it changes baked models and only takes effect after a resource reload.
* **Shader packs**: the preview shows the configured look, drawn by the vanilla programs. A pack changes the real hand
  (hand outlines depend on the "Hand with shader pack" switch, the glint stays vanilla); the notes `shader_hand_off`
  and `shader_glint` say so, the preview itself does not imitate the pack. Whether a GUI-time render of a first-person
  context shows glint and outline correctly while Iris has a pack active is **[UNVERIFIED in game]**.
* **First-person animation mods** that hook first-person rendering may add transforms; the bounds fit absorbs offsets
  **[UNVERIFIED in game]**. The lighting is a taste (the entity-in-inventory setup) and may be tuned.
* **Depth** relies on the GUI background being drawn at z of about 0 and on the item staying within z 40 to 260.
* Items whose renderer draws outside `MultiBufferSource` have no outline, as in the hand. A BEWLR that casts the source
  to a `BufferSource` throws in the measuring render and gets the unit cube.
* **Cost** while the panel is visible: one item render, one small mask and two distance passes over about the box
  size, plus the JSON fingerprint (12.3). Nothing runs when no panel is visible; the PREVIEW phase is created at the
  first use.
* The dev studio does not use the preview; it has its own scene render (`docs/STUDIO.md`).

### 12.7 Tests and what is still open in game

Unit tests (headless): `PreviewPoseTest` (the inverse cancels `ItemTransform.apply`, invertibility, fit keeps the
corners inside the box for every yaw and several pitches), `RuleEngineScopeTest` (scope isolation and caches),
`OutlineContextsTest` (PREVIEW mapping), `GlintSystemTest` (`active`), `GlintTexturesTest` (quiet mode),
`PendingEditsTest`, `PreviewSnapshotTest`, `PreviewLayoutTest`, `PreviewNotesTest` and `LangFilesTest` (the keys
`glintsmith.preview.*` in both languages). The render path itself is not covered by tests. It was run in game on
2026-10-18 for the panel, the animation and the unsaved settings (item 31 a, b); dragging, right click reset, the
rule editor and colour picker, the source strip, narrow windows and GUI scale 4, and a shader pack are still to be
checked by hand; the check list is section 10, item 31.

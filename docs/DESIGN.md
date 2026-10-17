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
target that holds a copy of the scene depth. Two full-screen passes, clipped to the bounding box of
the mask vertices, then run:

1. A horizontal nearest-seed search.
2. A vertical search that finishes the exact Euclidean distance transform, looks up colour,
   thickness, softness and opacity in a 256x2 style texture, suppresses pixels where scene geometry is
   in front of the outlined item, and alpha-blends the outline onto the main target.

Why this approach:

* **Universal by construction.** Anything that reaches a `MultiBufferSource` is captured:
  `item/generated` quads, JSON/3D resource-pack models, FRAPI models on Sodium, BEWLR and Fabric
  `BuiltinItemRendererRegistry` output (shield, trident, banners, chests, mod renderers) and armor
  geometry. There are no per-item special cases and no hull caps.
* **Pixel-constant thickness and soft glow.** Each outlined item on screen gets its own colour,
  thickness and softness, with no re-rendering of the item and no "late replay".
* **Cost scales with what is on screen.** Nothing at all happens on frames where no outlined item
  is drawn. Otherwise the cost is one mask draw plus two clipped full-screen passes per phase.
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
| WORLD | copy of main depth, mask LEQUAL + polygon offset | `endBatch(waterMask)` TAIL (before translucent terrain, particles, weather) | main |
| HAND, vanilla | copy of main depth after the hand's depth clear | after `renderHandsWithItems` returns (before screen effects) | main |
| HAND, Iris pack | cleared (no scene depth available) | `GameRenderer.renderLevel` RETURN | main (contains Iris's final image) |
| GUI | none (no depth test) | after each `GuiGraphics.renderItem` (the item is drawn by then: by its own flush, or under HUD batching by the O1-end flush of outlined GUI items) | main |

---

## 3. Outline pipeline in detail (T5)

### 3.1 Phases

`OutlinePhase { NONE, WORLD, HAND, GUI }`. A tee is created only while a phase is open. Each phase has
its own `PhaseState`: a mask buffer source, a style table, a bbox accumulator, a matrix snapshot and a
`hasGeometry` flag. Phases never share a buffer source, so nesting (for example Iris rendering hands
inside `renderLevel`) cannot flush another phase's geometry. Only one phase is "current" at a time; the
innermost open phase wins.

| Phase | Opened by | Closed by | Item targets accepted |
|---|---|---|---|
| WORLD | `WorldRenderEvents.START`, if any world target is enabled, no shader pack is active and the failsafe has not tripped | world composite (O6), else `WorldRenderEvents.LAST`, else frame end (discard) | THIRD_PERSON, ARMOR, DROPPED, ITEM_FRAME, OTHER. Any other display context seen in world is treated as OTHER. |
| HAND | O2 wrap of `renderHandsWithItems`, if FIRST_PERSON is enabled | end of that wrap (vanilla), or `GameRenderer.renderLevel` RETURN (Iris) | FIRST_PERSON. Both hand pairs count: FIRST_PERSON_* and THIRD_PERSON_* contexts drawn inside the phase (see below). |
| GUI | O5 wrap of private `GuiGraphics.renderItem(...)`, if GUI is enabled | end of that wrap | GUI |

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
8. `RuleEngine.resolve(stack, target).outline()` is not null and has thickness > 0 and opacity > 0.
9. `argb = RuleEngine.outlineColor(stack, look, timeSeconds)` has alpha × opacity > 0.
10. `idx = phase.styles.indexFor(argb, thickness, softness, opacity)` is not 0.

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

The mask type is memoized by (texture, mode, depthTested) and built like this:

```
RenderType.create("glintsmith_outline_mask", DefaultVertexFormat.POSITION_TEX_COLOR, mode, 1536, false, false,
  RenderType.CompositeState.builder()
    .setShaderState(new RenderStateShard.ShaderStateShard(OutlineShaders::mask))
    .setTextureState(new RenderStateShard.TextureStateShard(texture, false, false))
    .setTransparencyState(RenderStateShard.NO_TRANSPARENCY)
    .setCullState(RenderStateShard.NO_CULL)
    .setDepthTestState(depthTested ? RenderStateShard.LEQUAL_DEPTH_TEST : RenderStateShard.NO_DEPTH_TEST)
    .setLayeringState(depthTested ? RenderStateShard.POLYGON_OFFSET_LAYERING : RenderStateShard.NO_LAYERING)
    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
    .createCompositeState(false));
```

There is no output shard. The compositor binds the mask target itself before calling
`maskSource.endBatch()`. `POLYGON_OFFSET_LAYERING` is `polygonOffset(-1, -10)`. It makes the mask's
fragments pass LEQUAL against the item's own depth even with precision noise. The mask vertex shader
also uses the exact vanilla expression `ProjMat * ModelViewMat * vec4(Position, 1.0)`, the same
invariance that vanilla glint relies on for its EQUAL depth test.

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

Depth handling per phase:

* **WORLD.** Bind the mask target, clear colour to 0, then `mask.copyDepthFrom(main)` (a depth blit;
  it leaves framebuffer 0 bound, so re-bind with `mask.bindWrite(false)`). Then draw the mask types
  with `LEQUAL` and no depth write. By the composite point, all item batches have been flushed into
  main depth: `translucentCullBlockSheet` (flat world items), the shield/banner sheets and the entity
  sheets. So:
  * The seed depth is the item's depth.
  * Walls occlude the mask, so nothing outlines through walls.
  * Fabulous translucent block items, which live in `itemEntityTarget`, still pass, because main
    depth behind them is farther away.
* **HAND, vanilla.** The same as WORLD. The main depth was cleared before the hand and now contains
  hand plus arm depth.
* **HAND, Iris.** Do not copy (main depth is not Iris's depth). Clear the mask depth to 1.0 once per
  frame at the first hand flush. Under Iris, `renderHandsWithItems` runs twice per frame (solid and
  translucent passes), so the mask accumulates across both calls. The composite uses `UseDepth=0`.
* **GUI.** Scissored clear of the mask colour inside the item's bbox. Mask types use `NO_DEPTH_TEST`.
  The composite uses `UseDepth=0`.

**Occluder suppression** (`UseDepth=1`). The composite reads the mask target's depth texture at the
outline pixel p (`zp`, the scene) and at its nearest seed s (`zs`, the item). After linearising both
with `NearFar = (0.05, GameRenderer.getDepthFar())`, the outline pixel is discarded if
`zp < zs - max(DepthTolerance, 0.02·zs)` (DepthTolerance = 0.15 blocks). This removes outlines drawn
onto things in front of the item: an arm in front of a held sword, a wall edge in front of a dropped
item, a mob standing in front of an item frame. It keeps outlines over things behind the item: the
holder's body, the floor, the sky.

**Frame-buffer stencil caveat.** If a mod gives the main target a `DEPTH24_STENCIL8` attachment, the
depth blit into our `DEPTH_COMPONENT` target fails with `GL_INVALID_OPERATION`. Check `glGetError()`
once after the first copy each session (after draining earlier errors). On error, log once and disable
WORLD and HAND (vanilla) outlines; GUI keeps working.

### 3.4 Per-item data encoding

* **Mask target, RGBA8.** R = style index / 255 (0 = empty, 1..255 = style). G, B and A are unused.
* **Style table** (`StyleTable`, one per phase). Open addressing over `int[]` keys
  (argb, packed thickness/softness/opacity bytes). It is reset after each composite. At most 255
  entries per composite; further distinct styles reuse index 255 (rare, logged at debug level).
  * Thickness is clamped to `[0.5, settings.maxThickness]` and to at most 31.875 px.
  * Encoding is a 256x2 RGBA8 `DynamicTexture` (`NativeImage(256, 2, false)`):
    * row 0 = (R, G, B, A = round(colourAlpha × opacity)),
    * row 1 = (round(thickness × 8), round(softness × 255), 0, 255).
  * Upload only the used columns: `dyn.bind(); img.upload(0, 0, 0, 0, 0, n+1, 2, false, false, false, false)`.
  * `maxRadius = max over used styles of ceil(t·(1+s) + 1)`, clamped to `MAX_RADIUS = 32`. This is the
    search radius `R` uniform. It is **dynamic**: typical looks give R = 3 to 8.

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

`mask.fsh`
```glsl
#version 150
uniform sampler2D Sampler0;
in vec2 texCoord0;
in vec4 vertexColor;
out vec4 fragColor;
void main() {
    if (texture(Sampler0, texCoord0).a < 0.1) discard;   // vanilla cutout threshold → visible silhouette
    fragColor = vertexColor;                              // R = style index / 255
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
  "samplers": [ { "name": "MaskSampler" } ],
  "uniforms": [
    { "name": "Radius", "type": "int",   "count": 1, "values": [ 4 ] },
    { "name": "Region", "type": "float", "count": 4, "values": [ 0, 0, 1, 1 ] }
  ]
}
```

`outline_edt.fsh` (horizontal pass: nearest seed on the same row, signed)
```glsl
#version 150
uniform sampler2D MaskSampler;
uniform int Radius;
uniform vec4 Region;          // x0, y0, x1, y1 inclusive, framebuffer pixels (GL origin bottom-left)
out vec4 fragColor;
void main() {
    ivec2 p = ivec2(gl_FragCoord.xy);
    int x0 = int(Region.x), x1 = int(Region.z);
    for (int i = 0; i <= Radius; i++) {
        int xr = p.x + i;
        if (xr <= x1) {
            float id = texelFetch(MaskSampler, ivec2(xr, p.y), 0).r;
            if (id > 0.0) { fragColor = vec4((float(i) + 128.0) / 255.0, id, 0.0, 1.0); return; }
        }
        int xl = p.x - i;
        if (i > 0 && xl >= x0) {
            float id = texelFetch(MaskSampler, ivec2(xl, p.y), 0).r;
            if (id > 0.0) { fragColor = vec4((128.0 - float(i)) / 255.0, id, 0.0, 1.0); return; }
        }
    }
    fragColor = vec4(0.0);    // A = 0 → no seed within Radius on this row
}
```

`outline_composite.json`
```json
{
  "vertex": "glintsmith_fullscreen",
  "fragment": "glintsmith_outline_composite",
  "samplers": [ { "name": "MaskSampler" }, { "name": "EdtSampler" }, { "name": "DepthSampler" }, { "name": "StyleSampler" } ],
  "uniforms": [
    { "name": "Radius",         "type": "int",   "count": 1, "values": [ 4 ] },
    { "name": "Region",         "type": "float", "count": 4, "values": [ 0, 0, 1, 1 ] },
    { "name": "UseDepth",       "type": "int",   "count": 1, "values": [ 0 ] },
    { "name": "NearFar",        "type": "float", "count": 2, "values": [ 0.05, 1024.0 ] },
    { "name": "DepthTolerance", "type": "float", "count": 1, "values": [ 0.15 ] },
    { "name": "DistanceFade",   "type": "float", "count": 2, "values": [ 0.0, 0.0 ] }
  ]
}
```

`outline_composite.fsh` (vertical pass: exact EDT, style lookup, occluder test, output)
```glsl
#version 150
uniform sampler2D MaskSampler;
uniform sampler2D EdtSampler;
uniform sampler2D DepthSampler;
uniform sampler2D StyleSampler;
uniform int Radius;
uniform vec4 Region;
uniform int UseDepth;
uniform vec2 NearFar;
uniform float DepthTolerance;
uniform vec2 DistanceFade;    // (start, end) in blocks; end <= start disables
out vec4 fragColor;

float linearDepth(float z) {
    float n = NearFar.x, f = NearFar.y;
    return (2.0 * n * f) / (f + n - (2.0 * z - 1.0) * (f - n));
}

void main() {
    ivec2 p = ivec2(gl_FragCoord.xy);
    if (texelFetch(MaskSampler, p, 0).r > 0.0) discard;          // inside a silhouette: no outline
    float best = 1.0e9; float bestId = 0.0; ivec2 seed = p;
    int y0 = int(Region.y), y1 = int(Region.w);
    for (int j = 0; j <= Radius; j++) {
        if (float(j * j) >= best) break;                          // early exit: no closer seed possible
        for (int s = -1; s <= 1; s += 2) {
            if (j == 0 && s > 0) continue;
            int y = p.y + s * j;
            if (y < y0 || y > y1) continue;                       // outside region = stale data, ignore
            vec4 h = texelFetch(EdtSampler, ivec2(p.x, y), 0);
            if (h.a < 0.5) continue;
            float dx = h.r * 255.0 - 128.0;
            float d2 = dx * dx + float(j * j);
            if (d2 < best) { best = d2; bestId = h.g; seed = ivec2(p.x + int(round(dx)), y); }
        }
    }
    if (bestId <= 0.0) discard;
    int idx = int(bestId * 255.0 + 0.5);
    vec4 col = texelFetch(StyleSampler, ivec2(idx, 0), 0);
    vec4 par = texelFetch(StyleSampler, ivec2(idx, 1), 0);
    float thickness = par.r * 255.0 / 8.0;
    float softness  = par.g;
    float d = sqrt(best) - 0.5;                                   // distance to the seed pixel's edge
    float inner = thickness * (1.0 - softness);
    float outer = thickness * (1.0 + softness) + 0.5;
    float a = (1.0 - smoothstep(inner, outer, d)) * col.a;
    if (UseDepth == 1) {
        float zp = linearDepth(texelFetch(DepthSampler, p, 0).r);
        float zs = linearDepth(texelFetch(DepthSampler, seed, 0).r);
        if (zp < zs - max(DepthTolerance, zs * 0.02)) discard;   // something in front of the item here
        if (DistanceFade.y > DistanceFade.x) a *= 1.0 - smoothstep(DistanceFade.x, DistanceFade.y, zs);
    }
    if (a <= 0.003) discard;
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
4. **Mask.** Bind `mask` for write (`bindWrite(false)`). Clear according to the phase (section 3.3).
   For WORLD and vanilla HAND, copy depth and re-bind. Then `phase.maskSource.endBatch()`. This
   draws every mask type using the *current* `RenderSystem` projection and model-view, which are the
   same matrices vanilla just used. That is why each composite point sits exactly where vanilla
   flushed the corresponding geometry.
5. **Pass H.** Bind `edt` for write. `RenderSystem.enableScissor(region)`. Disable depth test; set
   `depthMask(false)`; disable blend. Set uniforms `Radius`, `Region` and `setSampler("MaskSampler", mask.getColorTextureId())`.
   Draw with `FullscreenQuad.draw(edtShader)`, a static `VertexBuffer(Usage.STATIC)` holding a
   POSITION quad (-1,-1)(1,-1)(1,1)(-1,1), drawn with `drawWithShader(IDENTITY, IDENTITY, shader)`.
6. **Pass V + composite.** Bind `main` for write (`bindWrite(false)`; the same size, so the viewport is
   unchanged). Keep the scissor. Upload the style table. Set samplers:
   * `MaskSampler` = mask colour
   * `EdtSampler` = edt colour
   * `DepthSampler` = `Integer.valueOf(mask.getDepthTextureId())`
   * `StyleSampler` = style `DynamicTexture`

   Set uniforms `UseDepth`, `NearFar`, `DepthTolerance`, `DistanceFade`. Enable blend with
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
| A2 | `outline.CompositeStateAccessor` | `net.minecraft.client.renderer.RenderType$CompositeState` | fields `textureState`, `transparencyState` | `@Accessor` | Texture shard, glint detection | — |
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
| G5a | `glint.BufferSourceGlintMixin` | `MultiBufferSource$BufferSource` | `getBuffer(Lnet/minecraft/client/renderer/RenderType;)Lcom/mojang/blaze3d/vertex/VertexConsumer;` | `@ModifyVariable(at = @At("HEAD"), argsOnly = true)` | Substitute vanilla glint types with slot or off types (main source only) | Vanilla glint everywhere; the feature is effectively off |
| G5b | same | same | `endBatch(Lnet/minecraft/client/renderer/RenderType;)V` TAIL | `@Inject` | When `this == main && type == vanilla entityGlintDirect`, flush every registered slot and off type | Slot glints are flushed by the later `endBatch()`. In non-Fabulous mode that is after water, so items seen through water lose their glint. |
| G5c | same | same | `endBatch(Lnet/minecraft/client/renderer/RenderType;Lcom/mojang/blaze3d/vertex/BufferBuilder;)V` (private) TAIL | `@Inject` | `GlintSystem.onBatchDrawn(type)` releases the slot | Slots are released only at frame start, so at most 32 distinct looks per kind per frame |
| G6 | `glint.GameRendererGlintMixin` | `GameRenderer` | `render(Lnet/minecraft/client/DeltaTracker;Z)V` HEAD | `@Inject` | `GlintSystem.onFrameBegin()` | Frame counter is not advanced, so stale slots are never auto-released; `onBatchDrawn` still releases them |
| GA1 | `glint.BufferSourceAccessor` | `MultiBufferSource$BufferSource` | field `fixedBuffers` | `@Accessor` | Register slot types as fixed buffers | — |
| GA2 | `glint.RenderBuffersAccessor` | `net.minecraft.client.renderer.RenderBuffers` | field `bufferSource` | `@Accessor` | Get the vanilla main source without going through Iris's `bufferSource()` replacement | — |
| G8 | `glint.ItemInHandRendererGlintMixin` | `net.minecraft.client.renderer.ItemInHandRenderer` | `renderHandsWithItems(...)` (same descriptor as O2b) | `@WrapMethod`, `require = 0` | `GlintContext.beginHands()`; `endHands()` in `finally`. Held items drawn inside count as FIRST_PERSON for rules, also with a THIRD_PERSON hand context (first-person animation mods). Independent of the outline hook O2b. | Items drawn by a first-person animation mod use the THIRD_PERSON rules; vanilla hands are unaffected |
| G7 (optional, phase 2) | `compat.IrisFullyBufferedGlintMixin` | `@Pseudo @Mixin(targets = "net.irisshaders.batchedentityrendering.impl.FullyBufferedMultiBufferSource")` | `getBuffer(Lnet/minecraft/client/renderer/RenderType;)Lcom/mojang/blaze3d/vertex/VertexConsumer;` **[UNVERIFIED runtime name; javap of the Iris jar shows the method literally named `getBuffer`]** | `@ModifyVariable(HEAD, argsOnly)` | Glint looks in world and hand while a shader pack is on | Vanilla glint in world and hand under a shader pack (GUI still customised) |

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
once, so up to 32 distinct looks per kind per frame. Animated tints change their ARGB every frame but
reuse the same slots. Memory and the render-type count are constant.

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

Source: `GlintPresets` (the formulas), `GlintNoise` (the helpers), `GlintTextures` (registration). Everything here describes
the code as it stands after the T6 brightness correction, and `GlintPresetsTest` enforces the numbers in the budget below.

All generated presets are 128x128, tileable, deterministic (same bytes on every run and platform: hashes are integer
arithmetic, transcendental functions use `StrictMath`), and built as `int[128 * 128]` of ABGR pixels, then uploaded with
`new NativeImage(128, 128, false)` + `setPixelRGBA` + `new DynamicTexture(img)`. Alpha is always 255. Black means "no
glint", because the blend is additive. Every channel is capped at `PEAK_BYTE = 229` (0.9 of 255).
`TextureManager.register` closes the previous texture under the same id.

**Brightness budget.** The glint blend is additive (`src * src + dst`), so a texture that is lit everywhere adds light to
the whole item and washes it out in one colour. An earlier version of the presets was brighter. The
corrected presets are shaped with `smoothstep` thresholds (no global darkening), so most texels are black and the peaks
stay bright. The vanilla item glint has a mean luminance of about 0.23; the presets are held near it. With
`lum = 0.2126 r + 0.7152 g + 0.0722 b` (channels 0..1), each generated preset must satisfy:

| Property | Bound |
|---|---|
| Mean luminance | 0.15 to 0.32 (`sparkle`: 0.03 to 0.32, it is deliberately sparse) |
| Dark texels (`lum < 0.08`) | at least 30 % of the tile |
| Peak channel | at least 200 and at most 229 |
| Spread | brightest minus darkest peak at least 60; at least 1 % of the texels lit; every 32x32 block of the tile has lit texels |
| Periodicity | `f(x + 128, y) == f(x, y)` in both axes, so it tiles |

**Helpers** (`GlintNoise`, pure Java, `TILE = 128`; `u = x / 128`, `v = y / 128` below):

* `hash(x, y, seed)` returns a float in [0, 1): `h = x * 374761393 + y * 668265263 + seed * 1442695041; h = (h ^ (h >>> 13)) * 1274126177; h ^= h >>> 16; return (h & 0xFFFFFF) / 16777216`.
* `value(x, y, px, py, seed)` is value noise with `px` by `py` lattice cells across the tile (smooth interpolation, lattice indices wrap with `floorMod`, so it is periodic).
* `fbm(x, y, px, py, octaves, seed)` sums `octaves` value noises, octave `o` with periods `px << o`, `py << o`, amplitude `0.5^o`, seed `seed + 101 * o`, normalised to [0, 1).
* `worley(x, y, g, seed, out)` is toroidal Worley noise on a `g` by `g` grid (one feature point per cell, 3x3 wrapped neighbourhood). It returns F1 and F2 in cell units, and the wrapped cell of the nearest point when `out` has four entries.
* `smoothstep(e0, e1, x)`, `lerp(t, a, b)`, `hsv(h, s, v)` (hue wraps), `frac`.
* `ramp(t; stops)` is a piecewise-linear colour gradient over `(position, r, g, b)` stops.

**Presets** (`rgb` before the cap at 229):

| Name | Algorithm |
|---|---|
| `neutral_item` / `neutral_entity` (not generated; `preset:neutral`) | Read vanilla `enchanted_glint_item.png` / `_entity.png` from the current `ResourceManager`. Per pixel `L = (0.2126 r + 0.7152 g + 0.0722 b) * alpha`, divided by the image's maximum `L` and multiplied by 0.9; output grey `(L, L, L)`. Same size as the source. Rebuilt on every resource reload. This is the base for tinting. |
| `rainbow` | `streak = fbm(x, y, 2, 2, 3, 11)`; `hue = frac(u + v + 0.25 * fbm(x, y, 2, 2, 2, 12))`; `I = 0.95 * smoothstep(0.37, 0.70, streak)`; `rgb = hsv(hue, 0.85, 1) * I`. `u + v` is periodic, so the hue bands run diagonally and tile. |
| `aurora` | `phase = 2u + 0.6 * fbm(x, y, 2, 2, 2, 21)`; `curtain = 0.5 + 0.5 * sin(2π * phase)`; `detail = fbm(x, y, 4, 4, 4, 22)`; `I = 0.95 * curtain² * (0.35 + 0.65 * smoothstep(0.2, 0.7, detail))`; `hue = lerp(fbm(x, y, 2, 2, 2, 23), 0.33, 0.78)` (green through cyan to violet); `rgb = hsv(hue, 0.7, 1) * I`. |
| `fire` | `heat = smoothstep(0.38, 0.74, fbm(x, y, 4, 2, 4, 31))`; `rgb = ramp(heat; 0: (0, 0, 0), 0.35: (0.55, 0.05, 0), 0.65: (0.9, 0.42, 0), 1: (0.9, 0.85, 0.55))`. Anisotropic (4 by 2 base cells). |
| `frost` | `F1, F2 = worley(x, y, 8, 41)`; `edge = 1 - smoothstep(0, 0.22, F2 - F1)` (crystal borders); `spark = hash(x mod 128, y mod 128, 42) > 0.996 ? 1 : 0`; `I = max(0.9 * edge, spark) * (0.55 + 0.45 * fbm(x, y, 4, 4, 3, 43))`; `rgb = ramp(I; 0: black, 0.5: (0.22, 0.5, 0.85), 1: (0.8, 0.9, 0.9))`. |
| `sparkle` | `F1 = worley(x, y, 12, 51)` with the owning cell `(cx, cy)`; `b = hash(cx, cy, 52)`; if `b > 0.35`: `I = b * (1 - smoothstep(0, 0.5, F1))²`, else `I = 0`; `rgb = (0.9, 0.86, 0.78) * I`. |
| `gold` | Warm diagonal streaks plus rare bright glints. `streak = fbm(x + y, x - y, 10, 2, 3, 61)` (value noise on the sheared coordinates, both periodic in the tile); `grain = fbm(x, y, 16, 16, 2, 62)`; `heat = smoothstep(0.3, 0.85, streak) * (0.7 + 0.3 * grain)`; `rgb = ramp(heat; 0: black, 0.35: (0.45, 0.28, 0.02), 0.75: (0.9, 0.7, 0.2), 1: (0.9, 0.86, 0.6))`. Glints: `F1 = worley(x, y, 12, 63)` with cell `c`; `cell = hash(cx, cy, 64)`; if `cell > 0.8`: `g = cell * (1 - smoothstep(0, 0.15, F1))³` and `rgb = max(rgb, (0.9, 0.86, 0.6) * g)` per channel. |
| `void` | Deep purple nebula, domain warped by periodic noise (so it still tiles), with sparse single-pixel stars. `wx = fbm(x, y, 2, 2, 3, 72)`, `wy = fbm(x, y, 2, 2, 3, 75)`; `t = fbm(x + 28 (wx - 0.5), y + 28 (wy - 0.5), 3, 3, 5, 73)`; `nebula = smoothstep(0.40, 0.85, t)`; `rgb = ramp(nebula; 0: black, 0.3: (0.12, 0, 0.25), 0.7: (0.45, 0.1, 0.7), 1: (0.75, 0.55, 0.9))`. Star at a texel: `hash(x mod 128, y mod 128, 74) > 0.9985`, brightness `0.6 + 0.4 * hash(x mod 128, y mod 128, 76)`; a texel with no star of its own takes 0.35 of the brightest of its four neighbours' stars (a faint cross). With star brightness `s`: `rgb = max(rgb, (0.9, 0.85, 0.9) * s)` per channel. |

Full preset list: `rainbow`, `aurora`, `fire`, `frost`, `sparkle`, `gold`, `void` (generated) and `neutral` (derived from the
vanilla texture). Together with `vanilla` and `file:` / resource-pack entries this is exactly what the config and the UI
accept: `vanilla` (alias `default`), `preset:neutral` (this resolves to the item or entity variant automatically),
`preset:rainbow`, `preset:aurora`, `preset:fire`, `preset:frost`, `preset:sparkle`, `preset:gold`, `preset:void`,
`file:<name>.png` and `namespace:path`. `preset:neutral_item` and `preset:neutral_entity` are internal names, not valid
specs. Generation cost at start-up is **[UNVERIFIED, not measured]**.

### 5.7 Edge cases T6 must handle

1. No context frame (thrown trident, mods drawing glint directly on the main source): use the
   category from the kind with category defaults. No rule lookup (the stack is EMPTY).
2. `RuleEngine.resolve` returns null or throws: treat it as `look = null`. On a throw, trip the glint
   failsafe.
3. A slot texture vanishes after a reload: `TextureManager.getTexture` returns the missing texture.
   That does not crash, and the next config or reload event re-resolves the slot.
4. Config changes mid-frame: the settings snapshot is swapped atomically (volatile field). Slots
   already assigned keep their params until drawn.
5. Iris shader pack on: substitution still runs on the main source (GUI). The world and hand use
   Iris's `FullyBufferedMultiBufferSource`, so the vanilla look applies there unless G7 is enabled.
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
| Fabulous | Full. Composite goes into main before the transparency chain; translucent layers combine over it correctly. `glintTranslucent` / `entityGlint` slots keep `ITEM_ENTITY_TARGET`. | Full | Full | Full |
| Sodium 0.8.13 | Full. Sodium replaces terrain only. Its item fast path (`ItemRendererMixin`) and `putBulkData` intrinsics fall back to vanilla code for our tee because it is not a `BufferBuilder` or `VertexBufferWriter`. Sodium logs **one** WARN per class (`VertexConsumerTracker`: "does not support optimized vertex writing code paths"). FRAPI models render through the `MultiBufferSource` we pass. | Full | Full | Full |
| Iris installed, shaders off | Full (Iris batching and hand pathway inactive) | Full | Full | Full |
| Iris, shader pack on | **Off** (log once per pack activation, at INFO). Main depth is not the scene depth and entities go to Iris gbuffers. The shadow pass is skipped too (phase closed, plus an explicit `isRenderingShadowPass()` check). | **Best effort, default on, option to disable.** The mask is flushed at TAIL of `renderHandsWithItems` with Iris's hand matrices; the composite happens after Iris's final pass at `GameRenderer.renderLevel` RETURN. No occluder test. Packs that move or distort the hand (DOF, hand sway) may misalign it. **[UNVERIFIED in game]** | Full | GUI full. World and hand use the vanilla look unless G7 is on; with G7 our slots go through Iris's glint program (texture and matrix honoured; tint and strength pack-dependent). |
| First-person animation mods (Punchy 2.8f) | n/a | Full for items drawn through `ItemRenderer.render` inside `renderHandsWithItems`: THIRD_PERSON hand contexts there count as FIRST_PERSON. Not outlined: items Punchy draws without `ItemRenderer.render` (bedrock-geometry items, chests, boats). **[UNVERIFIED in game]** | n/a | Full: the hand pass marker (G8) applies first-person rules to the same items |
| First-person mode of player-animation-lib / Better Combat | The held item is drawn in the world pass as THIRD_PERSON and is outlined as a third-person item; none under a shader pack | Not applicable (the item is not drawn in the HAND phase) | n/a | THIRD_PERSON rules |
| Other glint mods | Unaffected | Unaffected | Unaffected | If they replace the `RenderType.glint()` getters, our G5a never sees vanilla types and their look wins. If they also hook `BufferSource.getBuffer`, the last modifier wins. Document as incompatible-by-design. |
| Mods with a stencil main framebuffer (e.g. portals) | Disabled after the first failed depth blit (one log line) | Vanilla: disabled likewise | Full | Full |
| Multiple `renderLevel` per frame (portal mods) | The phase opens and composites per call | Unchanged | Unchanged | Unchanged |
| Mods drawing items without `MultiBufferSource`, or GUI items without `GuiGraphics.renderItem` (EMI batching) | Not outlined | Not outlined | Not outlined | Vanilla glint if not on the main source |
| ImmediatelyFast HUD batching (1.6.x) | Unaffected | Unaffected | Full. IF swaps the `GuiGraphics` buffer source for its `HudBatchingBufferSource`, skips the flush inside `renderItem` and draws its batch at the end of each HUD layer, or earlier when anything calls `BufferUploader.draw` / `_drawWithShader` (its `tryForceDrawHudBuffers`). Our mask draw is such a call, made with the mask target bound and scissored; without a flush first, the batch so far (hotbar sprite, earlier slots, counts) would be drawn into the mask and lost. Therefore, when `ItemRenderer.render` of an outlined GUI item ends (O1, the point where vanilla flushes, still with the item's lighting), its buffer source is flushed (depth test off as in `GuiGraphics.flush()`, then restored), which draws the batch onto main in order before the composite; non-outlined items keep batching. The same applies to IF screen batching (`experimental_screen_batching`, off by default). | Unaffected |

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
  the clipped region. Taps per pixel are at most 2(R+1) per pass, with early exit. Estimates
  **[UNVERIFIED, not measured]**:
  * 1080p, R = 6, region about 25 % of the screen: about 10 M texel fetches, under 0.1 ms on a GTX
    1060-class GPU.
  * Worst case 4K, R = 32, full screen: about 0.5 G fetches, 2 to 4 ms.

  So the default `maxThickness` is 8 px. Softness extends the radius to `t·(1+s)`, and the hard cap
  is 32.
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
record OutlineSettings(boolean enabled, boolean firstPerson, boolean thirdPerson, boolean onlyLocalPlayer,
                       boolean armor, boolean dropped, boolean itemFrame, boolean gui, boolean other,
                       boolean handWithShaderPack, float maxThickness, float fadeStart, float fadeEnd) {
    static OutlineSettings from(GlintsmithConfig cfg);      // the ONLY place that reads outline config fields
    boolean anyWorld();
    boolean accepts(OutlinePhase phase, StyleTarget target);
}
final class StyleTable {
    int indexFor(int argb, float thickness, float softness, float opacity);   // 1..255, 0 = reject
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
final class OutlineCompositor { static void composite(PhaseState phase, boolean useDepth); }
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
* `maxThickness` (default 8, cap 32)
* `fadeStart` / `fadeEnd` (blocks, default 0/0 = off)

### 8.2 T6: glint system (`navrelis.glintsmith.render.glint`)

```java
public final class GlintSystem {
    public static void init();                                                         // T8 calls once
    public static RenderType substitute(MultiBufferSource.BufferSource source, RenderType requested); // G5a
    public static void afterEndBatch(MultiBufferSource.BufferSource source, RenderType type);         // G5b
    public static void onBatchDrawn(RenderType type);                                                 // G5c
    public static void onFrameBegin();                                                                // G6
}
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
final class GlintParams { ResourceLocation texture; int tintArgb; float speed, scale, strength, rotationDeg;
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
final class GlintPresets { static NativeImage rainbow(); aurora(); fire(); frost(); sparkle(); static NativeImage neutral(NativeImage vanilla); }
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
6. G6 `onFrameBegin` → `frame++`; `timeSeconds = Util.getMillis() / 1000.0`; `pool.releaseStale(frame - 1)`.
7. Every public entry point catches Throwable → `GlintFailsafe.trip` → vanilla behaviour.

Mixins (T6): G1–G6, GA1, GA2 (G7 optional). Resources (T6): none. Presets are generated at run time;
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
   glint.ItemInHandRendererGlintMixin, glint.BufferSourceAccessor, glint.RenderBuffersAccessor
   (+ compat.IrisFullyBufferedGlintMixin only once G7 is implemented)
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
   *Recommendation:* ship it on by default with `handWithShaderPack` as an off switch; test with
   Complementary, BSL and a DOF pack.
3. **World outlines under shader packs** are off. Making them work would need either rendering the
   mask from Iris's depth texture or an Iris API that 1.21.1 does not have.
   *Recommendation:* accept for v1. Revisit with Iris's depth texture via its internals only if users
   ask; this would be fragile.
4. **Glint customisation in world and hand under shader packs** needs G7 (`@Pseudo` into an Iris impl
   class).
   *Recommendation:* implement G7 as phase 2, guarded by an `IMixinConfigPlugin` that applies it only
   when `iris` is loaded and its version starts with `1.8.`.
5. **The mask cannot see geometry drawn outside `MultiBufferSource`** (direct Tesselator draws in some
   mod BEWLRs).
   *Recommendation:* accept and document. GL-level interception would be far worse.
6. **Outline drawn onto a non-outlined occluder** is handled by depth suppression in WORLD and HAND.
   In GUI, overlapping neighbours simply overdraw each other in draw order.
   *Recommendation:* fine as is.
7. **Thickness on high-DPI and 4K screens** is in physical pixels, so it looks thinner at 4K.
   *Recommendation:* T3 adds an optional `scaleWithResolution` flag (thickness × framebufferHeight /
   1080) and, for GUI, `× guiScale / 2`. Decide in UI review.
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

---

## 10. Manual in-game test checklist

Setup: dev client with Fabric API only. Then repeat the marked rows with Sodium 0.8.13; with Iris
1.8.14 and shaders off; and with Iris plus Complementary Reimagined (shaders on). Video setting
Fabulous for the rows marked **F**.

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
   water: visible through the water surface (Fast and Fancy) **F**.
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
    outlines on, no errors in the log. Toggle the shader pack while in game.
14. Fabulous **F**: dropped stained glass block (`itemEntityTarget`), translucent items, rain in front
    of an outlined item (rain over the outline).
15. Performance: an empty world with nothing outlined shows no FPS difference against Glintsmith with
    all features off. With 50 enchanted dropped items on screen, check the F3 frame time.
16. Failsafe: in a debug build, force an exception in the compositor (system property
    `glintsmith.debug.failOutline=true`). One ERROR line, outlines stop, the game keeps running, and
    glint is unaffected.

Glint:

17. Each category set to `preset:rainbow`, `aurora`, `fire`, `frost`, `sparkle`, a `file:` PNG from
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
23. Iris shaders on: GUI glint customised; world and hand vanilla glint (unless G7); no visual
    breakage, no errors.
24. Resource reload with a pack that replaces `enchanted_glint_item.png`: the neutral variant is
    rebuilt from the new texture.
25. Punchy 2.8f installed: an enchanted sword in the main hand has a first-person outline and the
    first-person glint rules; the log (logger `Glintsmith/Outline`, DEBUG) shows the hand-items line once.
    Items Punchy draws without `ItemRenderer.render` have no outline (expected).
26. Gap fix, AUTO on Windows or Linux: the 1 px step-corner cracks of a generated item (for example a
    dropped diamond sword) are closed and no jagged slivers show along the step edges.

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
* On the other platforms the side quads grow by 0.005 on both in-plane axes. An in-game sweep showed that
  0.005 closes the 1 px step-corner cracks of generated item models, while 0.01 and above left jagged
  slivers along the step edges.
* Side elements keep vanilla's corner order: `GapFix.adjustSideElement` never sorts `from` and `to`. The
  `16 - y` flip in vanilla's `createSideElements` makes the baked quads face outwards, and the signs of
  the expansion and indent depend on that order.
* AUTO is switched off while the mod `modelfix` is installed; CUSTOM stacks on top of it and precise spans
  are inactive (README, section Compatibility).

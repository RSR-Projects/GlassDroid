package org.fdroid.ui.glass

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.tanh
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Spring physics tokens tuned after Apple iOS 26 Liquid Glass and Kyant0's AndroidLiquidGlass.
 */
object LiquidSprings {
  val BouncySpatial = spring<Float>(dampingRatio = 0.56f, stiffness = 460f)
  val SmoothSpatial = spring<Float>(dampingRatio = 0.84f, stiffness = 560f)
  val ListPlacement = spring<IntOffset>(dampingRatio = 0.72f, stiffness = 460f)
  val ListFade = spring<Float>(dampingRatio = 0.88f, stiffness = 580f)
  val NavFade = spring<Float>(dampingRatio = 0.88f, stiffness = 580f)
  val ExpandSize = spring<IntSize>(dampingRatio = 0.76f, stiffness = 460f)

  val ExpandEnter: EnterTransition =
    fadeIn(animationSpec = ListFade) +
      expandVertically(animationSpec = ExpandSize) +
      scaleIn(initialScale = 0.96f, animationSpec = SmoothSpatial)

  val ExpandExit: ExitTransition =
    fadeOut(animationSpec = ListFade) +
      shrinkVertically(animationSpec = ExpandSize) +
      scaleOut(targetScale = 0.96f, animationSpec = SmoothSpatial)

  @Composable
  fun expandEnter(config: LiquidGlassConfig = LocalLiquidGlassConfig.current): EnterTransition =
    fadeIn(animationSpec = config.smoothSpatialSpring()) +
      expandVertically(animationSpec = config.expandSizeSpring()) +
      scaleIn(initialScale = 0.95f, animationSpec = config.bouncySpatialSpring())

  @Composable
  fun expandExit(config: LiquidGlassConfig = LocalLiquidGlassConfig.current): ExitTransition =
    fadeOut(animationSpec = config.smoothSpatialSpring()) +
      shrinkVertically(animationSpec = config.expandSizeSpring()) +
      scaleOut(targetScale = 0.95f, animationSpec = config.smoothSpatialSpring())
}

/**
 * Ambient liquid touch [IndicationNodeFactory] replacing default flat Material grey ripples across
 * clickable controls (such as settings items and text rows) with a soft radial glass wave.
 */
object LiquidTouchIndication : IndicationNodeFactory {
  override fun create(interactionSource: InteractionSource): DelegatableNode =
    LiquidTouchIndicationNode(interactionSource)

  override fun hashCode(): Int = javaClass.hashCode()

  override fun equals(other: Any?): Boolean = other === this

  private class LiquidTouchIndicationNode(
    private val interactionSource: InteractionSource,
  ) : Modifier.Node(), DrawModifierNode {
    private var pressPosition: Offset = Offset.Unspecified
    private val rippleProgress = Animatable(0f)
    private val glowAlpha = Animatable(0f)
    private var animJob: Job? = null

    override fun onAttach() {
      coroutineScope.launch {
        interactionSource.interactions.collect { interaction ->
          when (interaction) {
            is PressInteraction.Press -> {
              pressPosition = interaction.pressPosition
              animJob?.cancel()
              animJob =
                coroutineScope.launch {
                  rippleProgress.snapTo(0.05f)
                  glowAlpha.snapTo(1f)
                  rippleProgress.animateTo(1f, tween(340, easing = FastOutSlowInEasing))
                }
            }
            is PressInteraction.Release,
            is PressInteraction.Cancel -> {
              coroutineScope.launch {
                glowAlpha.animateTo(0f, tween(240, easing = FastOutSlowInEasing))
              }
            }
          }
        }
      }
    }

    override fun ContentDrawScope.draw() {
      drawContent()
      val alpha = glowAlpha.value
      if (alpha > 0.01f && pressPosition != Offset.Unspecified) {
        val maxR = max(size.width, size.height).coerceAtLeast(64f) * 1.15f
        val r = (maxR * rippleProgress.value).coerceAtLeast(16f)
        val fillColor = Color.White.copy(alpha = 0.10f * alpha)
        drawRect(
          brush =
            Brush.radialGradient(
              colors = listOf(fillColor, fillColor.copy(alpha = 0.03f * alpha), Color.Transparent),
              center = pressPosition,
              radius = r,
            ),
          blendMode = BlendMode.Plus,
        )
        drawCircle(
          color = Color.White.copy(alpha = 0.14f * alpha * (1f - rippleProgress.value * 0.5f)),
          radius = r * 0.72f,
          center = pressPosition,
          style = Stroke(width = 1.5f),
          blendMode = BlendMode.Plus,
        )
      }
    }
  }
}

/**
 * Kyant0 `InteractiveHighlight` + `DampedDragAnimation`-inspired pointer squish state:
 * tracks press depth, long-press luminous bloom, elastic drag stretch, volume-preserving
 * release jiggle, and expanding liquid wave ripple originating at touch position.
 */
@Stable
class LiquidSquishState(private val scope: CoroutineScope) {
  val pressProgress = Animatable(0f)
  val bloomProgress = Animatable(0f)
  val jiggleAmplitude = Animatable(0f)
  val rippleProgress = Animatable(0f)
  val rippleAlpha = Animatable(0f)
  val dragOffset = Animatable(Offset.Zero, Offset.VectorConverter)
  var touchPosition by mutableStateOf(Offset.Unspecified)
    private set

  var isPressed by mutableStateOf(false)
    private set

  private var pressOrigin = Offset.Zero
  private var pressDownAtMillis = 0L
  private var bloomJob: Job? = null
  private var releaseJob: Job? = null
  private var rippleJob: Job? = null

  fun onPressDown(position: Offset, config: LiquidGlassConfig, reducedMotion: Boolean = false) {
    releaseJob?.cancel()
    bloomJob?.cancel()
    rippleJob?.cancel()
    isPressed = true
    pressOrigin = position
    touchPosition = position
    pressDownAtMillis = System.currentTimeMillis()
    scope.launch {
      jiggleAmplitude.snapTo(0f)
      if (reducedMotion) {
        pressProgress.snapTo(0.45f)
      } else {
        pressProgress.animateTo(1f, config.pressSpring())
      }
    }
    rippleJob =
      scope.launch {
        rippleProgress.snapTo(0.06f)
        rippleAlpha.snapTo(if (reducedMotion) 0.45f else 1f)
        rippleProgress.animateTo(1f, tween(durationMillis = if (reducedMotion) 140 else 420, easing = FastOutSlowInEasing))
      }
    if (!reducedMotion) {
      bloomJob =
        scope.launch {
          delay(145L)
          if (isPressed) {
            bloomProgress.animateTo(1f, config.bouncySpatialSpring())
          }
        }
    }
  }

  fun onDragMove(position: Offset) {
    touchPosition = position
    val delta = position - pressOrigin
    scope.launch { dragOffset.snapTo(delta) }
  }

  fun onRelease(
    config: LiquidGlassConfig,
    allowTapPulse: Boolean = true,
    reducedMotion: Boolean = false,
  ) {
    if (!isPressed) return
    isPressed = false
    bloomJob?.cancel()
    val elapsed = System.currentTimeMillis() - pressDownAtMillis
    releaseJob?.cancel()
    releaseJob =
      scope.launch {
        if (!reducedMotion && allowTapPulse && elapsed < 130L && pressProgress.value < 0.72f) {
          pressProgress.animateTo(1f, config.pressSpring())
        }
        val jiggleScale = config.effectiveReleaseJiggle()
        val jiggleImpulse =
          if (!reducedMotion && allowTapPulse && jiggleScale > 0.02f) {
            0.036f * jiggleScale.coerceIn(0f, 3.2f)
          } else {
            0f
          }
        if (reducedMotion) {
          pressProgress.snapTo(0f)
          bloomProgress.snapTo(0f)
          dragOffset.snapTo(Offset.Zero)
          rippleAlpha.snapTo(0f)
        } else {
          launch { pressProgress.animateTo(0f, config.releaseSpring()) }
          launch { bloomProgress.animateTo(0f, config.releaseSpring()) }
          launch { dragOffset.animateTo(Offset.Zero, config.offsetSpring()) }
          launch { rippleAlpha.animateTo(0f, tween(durationMillis = 260, easing = FastOutSlowInEasing)) }
          if (jiggleImpulse > 0f) {
            launch {
              jiggleAmplitude.snapTo(jiggleImpulse)
              jiggleAmplitude.animateTo(0f, config.jiggleSpring())
            }
          }
        }
      }
  }

  /** Triggers a full synthetic liquid press + wave + release jiggle pulse (used by Customizer "Try it"). */
  fun triggerDemoPulse(config: LiquidGlassConfig) {
    releaseJob?.cancel()
    bloomJob?.cancel()
    rippleJob?.cancel()
    isPressed = false
    if (touchPosition == Offset.Unspecified) {
      touchPosition = Offset.Zero
    }
    releaseJob =
      scope.launch {
        launch {
          rippleProgress.snapTo(0.05f)
          rippleAlpha.snapTo(1f)
          rippleProgress.animateTo(1f, tween(420, easing = FastOutSlowInEasing))
        }
        pressProgress.animateTo(1f, config.pressSpring())
        launch { bloomProgress.animateTo(0.85f, config.bouncySpatialSpring()) }
        delay(75L)
        val jiggleImpulse = 0.044f * config.effectiveReleaseJiggle().coerceIn(0.15f, 3.2f)
        launch { pressProgress.animateTo(0f, config.releaseSpring()) }
        launch { bloomProgress.animateTo(0f, config.releaseSpring()) }
        launch { rippleAlpha.animateTo(0f, tween(300, easing = FastOutSlowInEasing)) }
        launch {
          jiggleAmplitude.snapTo(jiggleImpulse)
          jiggleAmplitude.animateTo(0f, config.jiggleSpring())
        }
      }
  }
}

@Composable
fun rememberLiquidSquishState(): LiquidSquishState {
  val scope = rememberCoroutineScope()
  return remember(scope) { LiquidSquishState(scope) }
}

/**
 * Adds iOS 26-style tactile squish, elastic drag stretch, long-press bloom, asymmetric
 * release jiggle, adaptive rim brighten, and an expanding liquid wave ripple originating from the
 * user's touch position.
 *
 * IMPORTANT: Apply this modifier **OUTSIDE** (before) `.liquidGlass(...)`, `.clip(...)`,
 * `.background(...)`, and `.border(...)` so the entire container + border + shadow + content
 * physically squishes and moves together as one cohesive liquid glass body.
 */
fun Modifier.liquidSquish(
  enabled: Boolean = true,
  pressScale: Float = 0.925f,
  stretchAmount: Float = 0.06f,
  maxDragTranslationPx: Float = 12f,
  glowAlpha: Float = 0.24f,
  bloomScaleBoost: Float = 0.055f,
  externalState: LiquidSquishState? = null,
): Modifier = composed {
  val squishState = externalState ?: rememberLiquidSquishState()
  val isDark = LiquidGlassDefaults.palette().isDark
  val currentConfig by rememberUpdatedState(LocalLiquidGlassConfig.current)
  val reducedMotion = LocalReducedMotion.current

  this
    .pointerInput(enabled, reducedMotion) {
      if (!enabled) {
        squishState.onRelease(currentConfig, allowTapPulse = false, reducedMotion = reducedMotion)
        return@pointerInput
      }
      awaitPointerEventScope {
        while (true) {
          val event = awaitPointerEvent(PointerEventPass.Initial)
          val primary = event.changes.firstOrNull() ?: continue
          when {
            event.type == PointerEventType.Exit -> {
              squishState.onRelease(currentConfig, allowTapPulse = false, reducedMotion = reducedMotion)
            }
            primary.pressed && !squishState.isPressed -> {
              squishState.onPressDown(primary.position, currentConfig, reducedMotion = reducedMotion)
            }
            primary.pressed && squishState.isPressed -> {
              // Cancel squish cleanly if a parent scrollable (LazyColumn/Pager) claimed the gesture
              if (primary.isConsumed && (primary.position - primary.previousPosition).getDistance() > 3f) {
                squishState.onRelease(currentConfig, allowTapPulse = false, reducedMotion = reducedMotion)
              } else if (!reducedMotion) {
                squishState.onDragMove(primary.position)
              }
            }
            !primary.pressed && squishState.isPressed -> {
              squishState.onRelease(currentConfig, allowTapPulse = true, reducedMotion = reducedMotion)
            }
          }
        }
      }
    }
    .graphicsLayer {
      val cfg = currentConfig
      val progress = squishState.pressProgress.value
      val bloom = if (reducedMotion) 0f else squishState.bloomProgress.value
      val jiggle = if (reducedMotion) 0f else squishState.jiggleAmplitude.value
      val depthFactor = if (reducedMotion) 0.15f else cfg.effectivePressDepth().coerceIn(0.25f, 2.8f)
      val stretchFactor = if (reducedMotion) 0f else cfg.effectiveDragStretch().coerceIn(0f, 2.8f)
      val bloomFactor = if (reducedMotion) 0f else cfg.effectiveLongPressBloom().coerceIn(0.25f, 2.8f)

      val pressDelta = (pressScale - 1f) * depthFactor
      val baseScale = 1f + pressDelta * progress + (bloomScaleBoost * bloomFactor) * bloom
      val drag = squishState.dragOffset.value
      val normX = tanh((drag.x / max(size.width * 0.40f, 1f)).toDouble()).toFloat()
      val normY = tanh((drag.y / max(size.height * 0.40f, 1f)).toDouble()).toFloat()
      val effStretch = stretchAmount * stretchFactor
      val stretchX = abs(normX) * effStretch - abs(normY) * (effStretch * 0.48f)
      val stretchY = abs(normY) * effStretch - abs(normX) * (effStretch * 0.48f)
      scaleX = (baseScale + stretchX + jiggle).coerceIn(0.68f, 1.32f)
      scaleY = (baseScale + stretchY - jiggle * 0.72f).coerceIn(0.68f, 1.32f)
      val effTranslate = maxDragTranslationPx * stretchFactor
      translationX = normX * effTranslate
      translationY = normY * effTranslate
      transformOrigin = TransformOrigin.Center
    }
    .drawWithContent {
      drawContent()
      val p = squishState.pressProgress.value
      val b = squishState.bloomProgress.value
      val rAlpha = squishState.rippleAlpha.value
      val rProg = squishState.rippleProgress.value
      val touch = squishState.touchPosition
      val center =
        when {
          touch != Offset.Unspecified && touch != Offset.Zero -> touch
          else -> Offset(size.width * 0.5f, size.height * 0.5f)
        }
      if ((p > 0.01f || b > 0.01f || rAlpha > 0.01f) && glowAlpha > 0f) {
        val maxRadius = max(size.width, size.height).coerceAtLeast(64f) * (0.95f + 0.35f * b)
        val effectiveAlpha =
          (glowAlpha * (p * 0.85f + 0.45f * b * currentConfig.effectiveLongPressBloom() + 0.45f * rAlpha) *
            currentConfig.highlightIntensity.coerceIn(0.35f, 1.4f) *
            (if (isDark) 0.95f else 1f))
            .coerceIn(0f, 0.58f)
        val highlightColor = Color.White.copy(alpha = effectiveAlpha)
        drawRect(
          brush =
            Brush.radialGradient(
              colors = listOf(highlightColor, highlightColor.copy(alpha = 0f)),
              center = center,
              radius = maxRadius * (0.45f + 0.65f * rProg),
            ),
          blendMode = BlendMode.Plus,
        )
        // Subtle expanding liquid wave crest
        if (rAlpha > 0.02f && rProg in 0.05f..0.98f) {
          val waveRadius = (maxRadius * 0.82f * rProg).coerceAtLeast(8f)
          val ringAlpha = (0.18f * rAlpha * (1f - rProg * 0.55f)).coerceIn(0f, 0.24f)
          drawCircle(
            color = Color.White.copy(alpha = ringAlpha),
            radius = waveRadius,
            center = center,
            style = Stroke(width = 2.2f),
            blendMode = BlendMode.Plus,
          )
        }
      }
    }
}

/**
 * Gentler squish preset for full-width list rows and expandable cards so scrolling feels responsive
 * and tactile without excessive row distortion during vertical flings.
 */
fun Modifier.liquidRowSquish(enabled: Boolean = true): Modifier =
  liquidSquish(
    enabled = enabled,
    pressScale = 0.968f,
    stretchAmount = 0.025f,
    maxDragTranslationPx = 5f,
    glowAlpha = 0.14f,
    bloomScaleBoost = 0.022f,
  )

/**
 * Applies smooth spring placement and fade animations to items inside a
 * [androidx.compose.foundation.lazy.LazyColumn] or [androidx.compose.foundation.lazy.LazyRow],
 * dynamically synced to [LocalLiquidGlassConfig].
 */
fun Modifier.springListItem(scope: LazyItemScope): Modifier = composed {
  val cfg = LocalLiquidGlassConfig.current
  val fadeSpec = remember(cfg) { cfg.smoothSpatialSpring() }
  val placementSpec = remember(cfg) { cfg.navSlideSpring() }
  with(scope) {
    this@composed.animateItem(
      fadeInSpec = fadeSpec,
      placementSpec = placementSpec,
      fadeOutSpec = fadeSpec,
    )
  }
}

/**
 * Standard iOS 26-style bouncy button squish preset matching Kyant0's [LiquidButton] feel.
 */
fun Modifier.liquidButtonSquish(
  enabled: Boolean = true,
  externalState: LiquidSquishState? = null,
): Modifier =
  liquidSquish(
    enabled = enabled,
    pressScale = 0.905f,
    stretchAmount = 0.068f,
    maxDragTranslationPx = 10f,
    glowAlpha = 0.28f,
    bloomScaleBoost = 0.07f,
    externalState = externalState,
  )

/**
 * Coordinated one-shot staggered entrance for app cards and list items.
 * Tracks completion via [rememberSaveable] per [key] so recompositions and scroll-backs do not
 * repeatedly replay entrance animations.
 */
fun Modifier.staggeredLiquidEntrance(
  key: Any,
  index: Int,
  baseDelayMs: Long = 26L,
): Modifier = composed {
  val cfg = LocalLiquidGlassConfig.current
  val reducedMotion = LocalReducedMotion.current
  var hasAnimated by rememberSaveable(key) { mutableStateOf(false) }
  val alphaAnim = remember { Animatable(if (hasAnimated) 1f else 0f) }
  val progressAnim = remember { Animatable(if (hasAnimated) 1f else 0f) }

  LaunchedEffect(key) {
    if (!hasAnimated) {
      val delayMs = (index.coerceIn(0, 7) * baseDelayMs)
      if (delayMs > 0L) delay(delayMs)
      launch { alphaAnim.animateTo(1f, cfg.smoothSpatialSpring()) }
      if (reducedMotion) {
        progressAnim.snapTo(1f)
      } else {
        progressAnim.animateTo(1f, cfg.bouncySpatialSpring())
      }
      hasAnimated = true
    }
  }

  this.graphicsLayer {
    val p = progressAnim.value
    alpha = alphaAnim.value.coerceIn(0f, 1f)
    if (!reducedMotion && p < 0.999f) {
      val s = 0.94f + 0.06f * p
      scaleX = s
      scaleY = s
      translationY = (1f - p) * 20f
    }
  }
}

/**
 * One-shot smooth spring pop for app icons when first displayed.
 */
fun Modifier.iconPopEntrance(key: Any, index: Int = 0): Modifier = composed {
  val cfg = LocalLiquidGlassConfig.current
  val reducedMotion = LocalReducedMotion.current
  var hasPopped by rememberSaveable("icon_pop_$key") { mutableStateOf(false) }
  val scaleAnim = remember { Animatable(if (hasPopped || reducedMotion) 1f else 0.84f) }

  LaunchedEffect(key) {
    if (!hasPopped) {
      if (!reducedMotion) {
        val delayMs = (index.coerceIn(0, 6) * 24L)
        if (delayMs > 0L) delay(delayMs)
        scaleAnim.animateTo(1f, cfg.bouncySpatialSpring())
      }
      hasPopped = true
    }
  }

  this.graphicsLayer {
    val s = scaleAnim.value
    scaleX = s
    scaleY = s
  }
}

/**
 * Subtle one-shot reflective glass shimmer sweep that travels across highlighted cards once after
 * appearing and stops completely when finished so idle screens burn zero GPU/battery.
 */
fun Modifier.glassShimmerSweep(
  key: Any,
  enabled: Boolean = true,
  delayMillis: Long = 220L,
): Modifier = composed {
  val reducedMotion = LocalReducedMotion.current
  val config = LocalLiquidGlassConfig.current
  if (!enabled || reducedMotion || config.reducedVisualEffects) return@composed this

  var hasSwept by rememberSaveable("shimmer_sweep_$key") { mutableStateOf(false) }
  var sweepFraction by remember { mutableFloatStateOf(-0.45f) }
  val sweepAnim = remember { Animatable(-0.45f) }

  LaunchedEffect(key, enabled) {
    if (!hasSwept && enabled) {
      delay(delayMillis)
      sweepAnim.snapTo(-0.45f)
      sweepAnim.animateTo(1.45f, tween(durationMillis = 820, easing = FastOutSlowInEasing)) {
        sweepFraction = value
      }
      hasSwept = true
    }
  }

  if (hasSwept && sweepAnim.isRunning.not()) {
    this
  } else {
    this.drawWithContent {
      drawContent()
      val pos = sweepFraction
      if (pos > -0.4f && pos < 1.4f) {
        val startX = size.width * pos
        val band = size.width * 0.42f
        val sheenAlpha = (0.14f * config.highlightIntensity).coerceIn(0.04f, 0.24f)
        val sheen = Color.White.copy(alpha = sheenAlpha)
        drawRect(
          brush =
            Brush.linearGradient(
              colors = listOf(Color.Transparent, sheen, Color.Transparent),
              start = Offset(startX - band, 0f),
              end = Offset(startX + band, size.height),
            ),
          blendMode = BlendMode.Plus,
        )
      }
    }
  }
}

/**
 * Restrained scroll-linked vertical parallax layer (`graphicsLayer`-only) that respects reduced
 * motion and reduced visual effects without interfering with pointer/scroll handling.
 */
fun Modifier.liquidParallax(
  scrollOffsetPx: () -> Float,
  factor: Float = 0.14f,
  maxOffsetPx: Float = 42f,
): Modifier = composed {
  val reducedMotion = LocalReducedMotion.current
  val config = LocalLiquidGlassConfig.current
  if (reducedMotion || config.reducedVisualEffects) return@composed this
  val intensity = config.animationIntensity.coerceIn(0.25f, 1.50f)
  this.graphicsLayer {
    val shift = (scrollOffsetPx() * factor * intensity).coerceIn(-maxOffsetPx, maxOffsetPx)
    translationY = shift
  }
}

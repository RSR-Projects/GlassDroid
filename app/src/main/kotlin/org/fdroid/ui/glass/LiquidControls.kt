package org.fdroid.ui.glass

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.tanh
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Kyant0 `LiquidToggle` + iOS 26 Liquid Glass Switch.
 *
 * Features a pill-shaped capsule track with animated accent color and a draggable, squishable
 * capsule thumb that transforms into clear refractive Liquid Glass (`drawBackdrop` lens +
 * highlight + inner shadow) and blooms smoothly while pressed or dragged.
 */
@Composable
fun LiquidGlassSwitch(
  checked: Boolean,
  onCheckedChange: ((Boolean) -> Unit)?,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
) {
  val palette = LiquidGlassDefaults.palette()
  val config = LocalLiquidGlassConfig.current
  val reducedMotion = LocalReducedMotion.current
  val backdrop = LocalLiquidBackdrop.current ?: LocalAtmosphericBackdrop.current
  val scope = rememberCoroutineScope()
  val haptic = LocalHapticFeedback.current
  val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
  val currentChecked by rememberUpdatedState(checked)
  val currentOnCheckedChange by rememberUpdatedState(onCheckedChange)

  val trackWidth = 60.dp
  val trackHeight = 32.dp
  val thumbWidth = 34.dp
  val thumbHeight = 26.dp
  val thumbPadding = 3.dp

  val density = LocalDensity.current
  val travelPx = with(density) { (trackWidth - thumbWidth - thumbPadding * 2).toPx() }
  val fraction = remember { Animatable(if (checked) 1f else 0f) }
  val pressProgress = remember { Animatable(0f) }
  var isDragging by remember { mutableStateOf(false) }

  LaunchedEffect(checked, reducedMotion) {
    if (!isDragging) {
      if (reducedMotion) {
        fraction.snapTo(if (checked) 1f else 0f)
      } else {
        fraction.animateTo(if (checked) 1f else 0f, config.bouncySpatialSpring())
      }
    }
  }

  val activeTrackColor =
    if (palette.isDark) Color(0xFF34C759) else Color(0xFF28B44D)
  val inactiveTrackColor =
    if (palette.isDark) Color.White.copy(alpha = 0.16f) else Color.Black.copy(alpha = 0.14f)
  val trackColor by
    animateColorAsState(
      targetValue =
        when {
          !enabled && currentChecked -> activeTrackColor.copy(alpha = 0.45f)
          !enabled -> inactiveTrackColor.copy(alpha = 0.5f)
          fraction.value >= 0.5f -> activeTrackColor
          else -> inactiveTrackColor
        },
      animationSpec = config.colorSpring(),
    )

  val thumbShape = LiquidGlassShapes.CapsuleShape
  val trackShape = LiquidGlassShapes.CapsuleShape

  val interactionModifier =
    if (onCheckedChange != null && enabled) {
      Modifier.pointerInput(travelPx, isRtl) {
        var dragFractionStart = 0f
        var accumPx = 0f
        detectHorizontalDragGestures(
          onDragStart = {
            isDragging = true
            dragFractionStart = fraction.value
            accumPx = 0f
            scope.launch { pressProgress.animateTo(1f, config.pressSpring()) }
          },
          onDragEnd = {
            isDragging = false
            val target = fraction.value >= 0.5f
            scope.launch {
              launch { pressProgress.animateTo(0f, config.releaseSpring()) }
              launch { fraction.animateTo(if (target) 1f else 0f, config.bouncySpatialSpring()) }
            }
            if (target != currentChecked) {
              haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
              currentOnCheckedChange?.invoke(target)
            }
          },
          onDragCancel = {
            isDragging = false
            scope.launch {
              launch { pressProgress.animateTo(0f, config.releaseSpring()) }
              launch {
                fraction.animateTo(
                  if (currentChecked) 1f else 0f,
                  config.bouncySpatialSpring(),
                )
              }
            }
          },
          onHorizontalDrag = { _, dragAmount ->
            val signed = if (isRtl) -dragAmount else dragAmount
            accumPx += signed
            val next = (dragFractionStart + accumPx / travelPx.coerceAtLeast(1f)).coerceIn(0f, 1f)
            scope.launch { fraction.snapTo(next) }
          },
        )
      }.clickable(
        role = Role.Switch,
        onClick = {
          haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
          val target = !currentChecked
          scope.launch {
            pressProgress.animateTo(0.85f, config.pressSpring())
            pressProgress.animateTo(0f, config.releaseSpring())
          }
          currentOnCheckedChange?.invoke(target)
        },
      )
    } else {
      Modifier
    }

  Box(
    contentAlignment = Alignment.CenterStart,
    modifier =
      modifier
        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
        .liquidSquish(
          enabled = enabled && onCheckedChange != null,
          pressScale = 0.94f,
          stretchAmount = 0.05f,
          maxDragTranslationPx = 6f,
          glowAlpha = 0.16f,
          bloomScaleBoost = 0.04f,
        ),
  ) {
    Box(
      contentAlignment = Alignment.CenterStart,
      modifier =
        Modifier.size(trackWidth, trackHeight)
          .clip(trackShape)
          .background(trackColor, trackShape)
          .border(
            width = 0.8.dp,
            color =
              if (palette.isDark) Color.White.copy(alpha = 0.22f)
              else Color.White.copy(alpha = 0.65f),
            shape = trackShape,
          )
          .then(interactionModifier)
          .semantics {
            role = Role.Switch
            toggleableState = ToggleableState(currentChecked)
          }
          .padding(horizontal = thumbPadding),
    ) {
      val currentFraction = fraction.value.coerceIn(0f, 1f)
      val visualFraction = if (isRtl) 1f - currentFraction else currentFraction
      val p = if (reducedMotion) 0f else pressProgress.value.coerceIn(0f, 1f)
      val thumbScale = 1f + 0.28f * p * config.effectiveLongPressBloom().coerceIn(0.5f, 1.5f)
      val glassMode = p > 0.12f && backdrop != null && !config.reducedVisualEffects && !reducedMotion

      Box(
        modifier =
          Modifier.offset { IntOffset((visualFraction * travelPx).roundToInt(), 0) }
            .size(thumbWidth, thumbHeight)
            .graphicsLayer {
              scaleX = thumbScale
              scaleY = thumbScale
            }
            .then(
              if (glassMode && backdrop != null) {
                Modifier.drawBackdrop(
                  backdrop = backdrop,
                  shape = { thumbShape },
                  effects = {
                    vibrancy()
                    lens(
                      refractionHeight = (10.dp * config.refractionHeightScale).toPx() * p,
                      refractionAmount = (18.dp * config.refractionAmountScale).toPx() * p,
                      depthEffect = true,
                      chromaticAberration = config.chromaticAberration,
                    )
                  },
                  highlight = { LiquidGlassDefaults.highlight(palette.isDark, config.highlightIntensity) },
                  shadow = { LiquidGlassDefaults.floatingShadow(palette.isDark, 10.dp, 3.dp) },
                  innerShadow = { LiquidGlassDefaults.pillInnerShadow(palette.isDark) },
                  onDrawSurface = { drawRect(Color.White.copy(alpha = 1f - 0.80f * p)) },
                )
              } else {
                Modifier.shadow(4.dp, thumbShape, clip = false)
                  .clip(thumbShape)
                  .background(Color.White.copy(alpha = if (enabled) 0.98f else 0.72f), thumbShape)
              }
            )
      )
    }
  }
}

/**
 * Kyant0 `LiquidSlider` + iOS 26 Liquid Glass Slider.
 *
 * Features a capsule track with vivid primary fill and an elongated white pill thumb that morphs
 * into refractive clear Liquid Glass (`drawBackdrop` lens + highlight + inner shadow) and scales up
 * smoothly during drag/press, matching the customizer screenshots.
 */
@Composable
fun LiquidGlassSlider(
  value: Float,
  onValueChange: (Float) -> Unit,
  valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  activeTrackColor: Color = Color(0xFF0A84FF),
  onValueChangeFinished: (() -> Unit)? = null,
) {
  val palette = LiquidGlassDefaults.palette()
  val config = LocalLiquidGlassConfig.current
  val reducedMotion = LocalReducedMotion.current
  val backdrop = LocalLiquidBackdrop.current ?: LocalAtmosphericBackdrop.current
  val scope = rememberCoroutineScope()
  val haptic = LocalHapticFeedback.current
  val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
  val currentValue by rememberUpdatedState(value)
  val currentOnValueChange by rememberUpdatedState(onValueChange)
  val currentOnFinished by rememberUpdatedState(onValueChangeFinished)

  val span = (valueRange.endInclusive - valueRange.start).coerceAtLeast(1e-5f)
  val normalized = ((value - valueRange.start) / span).coerceIn(0f, 1f)
  val animatedFraction = remember { Animatable(normalized) }
  val pressProgress = remember { Animatable(0f) }
  var isDragging by remember { mutableStateOf(false) }
  var velocityStretch by remember { mutableFloatStateOf(0f) }

  LaunchedEffect(normalized, reducedMotion) {
    if (!isDragging) {
      if (reducedMotion) {
        animatedFraction.snapTo(normalized)
      } else {
        animatedFraction.animateTo(normalized, config.smoothSpatialSpring())
      }
    }
  }

  val thumbWidth = 42.dp
  val thumbHeight = 26.dp
  val trackHeight = 6.dp
  val inactiveColor =
    if (palette.isDark) Color.White.copy(alpha = 0.16f) else Color.Black.copy(alpha = 0.14f)

  BoxWithConstraints(
    contentAlignment = Alignment.CenterStart,
    modifier =
      modifier
        .fillMaxWidth()
        .height(44.dp)
        .pointerInput(valueRange, enabled, isRtl) {
          if (!enabled) return@pointerInput
          val widthPx = size.width.toFloat().coerceAtLeast(1f)
          detectTapGestures(
            onPress = { offset ->
              scope.launch { pressProgress.animateTo(1f, config.pressSpring()) }
              val rawRatio = (offset.x / widthPx).let { if (isRtl) 1f - it else it }.coerceIn(0f, 1f)
              val nextValue = valueRange.start + rawRatio * span
              currentOnValueChange(nextValue)
              tryAwaitRelease()
              scope.launch { pressProgress.animateTo(0f, config.releaseSpring()) }
              currentOnFinished?.invoke()
            }
          )
        }
        .pointerInput(valueRange, enabled, isRtl) {
          if (!enabled) return@pointerInput
          val thumbW = with(this) { thumbWidth.toPx() }
          val usableWidth = (size.width.toFloat() - thumbW).coerceAtLeast(1f)
          var dragFrac = 0f
          detectHorizontalDragGestures(
            onDragStart = { startOffset ->
              isDragging = true
              dragFrac =
                ((startOffset.x - thumbW / 2f) / usableWidth)
                  .let { if (isRtl) 1f - it else it }
                  .coerceIn(0f, 1f)
              scope.launch {
                pressProgress.animateTo(1f, config.pressSpring())
                animatedFraction.snapTo(dragFrac)
              }
              currentOnValueChange(valueRange.start + dragFrac * span)
            },
            onDragEnd = {
              isDragging = false
              velocityStretch = 0f
              scope.launch { pressProgress.animateTo(0f, config.releaseSpring()) }
              currentOnFinished?.invoke()
            },
            onDragCancel = {
              isDragging = false
              velocityStretch = 0f
              scope.launch { pressProgress.animateTo(0f, config.releaseSpring()) }
            },
            onHorizontalDrag = { _, dragAmount ->
              val signed = if (isRtl) -dragAmount else dragAmount
              val prev = dragFrac
              dragFrac = (dragFrac + signed / usableWidth).coerceIn(0f, 1f)
              if ((prev > 0f && dragFrac == 0f) || (prev < 1f && dragFrac == 1f)) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
              }
              velocityStretch = tanh((signed / 22f).toDouble()).toFloat() * 0.14f * config.dragStretch
              scope.launch { animatedFraction.snapTo(dragFrac) }
              currentOnValueChange(valueRange.start + dragFrac * span)
            },
          )
        },
  ) {
    val thumbW = thumbWidth
    val totalMaxWidth = maxWidth
    val usableDp = (totalMaxWidth - thumbW).coerceAtLeast(1.dp)
    val f = animatedFraction.value.coerceIn(0f, 1f)
    val p = pressProgress.value.coerceIn(0f, 1f)
    val visualFrac = if (isRtl) 1f - f else f
    val thumbShape = LiquidGlassShapes.CapsuleShape

    // Track background
    Box(
      modifier =
        Modifier.fillMaxWidth()
          .height(trackHeight)
          .clip(LiquidGlassShapes.CapsuleShape)
          .background(inactiveColor)
    ) {
      val fillRatio =
        ((f * usableDp.value + thumbW.value * 0.5f) / totalMaxWidth.value.coerceAtLeast(1f))
          .coerceIn(0f, 1f)
      Box(
        modifier =
          Modifier.fillMaxHeight()
            .fillMaxWidth(fillRatio)
            .clip(LiquidGlassShapes.CapsuleShape)
            .background(activeTrackColor)
      )
    }

    // Pill thumb: white capsule at rest, clear refractive Liquid Glass when pressed/dragged
    val stretch = if (reducedMotion) 0f else abs(velocityStretch).coerceIn(0f, 0.16f)
    val pSlider = if (reducedMotion) 0f else p
    val thumbScaleX = 1f + 0.28f * pSlider + stretch
    val thumbScaleY = 1f + 0.24f * pSlider - stretch * 0.35f
    val glassActive = pSlider > 0.12f && backdrop != null && !config.reducedVisualEffects

    Box(
      modifier =
        Modifier.offset(x = usableDp * visualFrac)
          .size(thumbWidth, thumbHeight)
          .graphicsLayer {
            scaleX = thumbScaleX
            scaleY = thumbScaleY
          }
          .then(
            if (glassActive && backdrop != null) {
              Modifier.drawBackdrop(
                backdrop = backdrop,
                shape = { thumbShape },
                effects = {
                  vibrancy()
                  lens(
                    refractionHeight = (11.dp * config.refractionHeightScale).toPx() * pSlider,
                    refractionAmount = (20.dp * config.refractionAmountScale).toPx() * pSlider,
                    depthEffect = true,
                    chromaticAberration = config.chromaticAberration,
                  )
                },
                highlight = { LiquidGlassDefaults.highlight(palette.isDark, config.highlightIntensity) },
                shadow = { LiquidGlassDefaults.floatingShadow(palette.isDark, 12.dp, 4.dp) },
                innerShadow = { LiquidGlassDefaults.pillInnerShadow(palette.isDark) },
                onDrawSurface = { drawRect(Color.White.copy(alpha = 1f - 0.82f * pSlider)) },
              )
            } else {
              Modifier.shadow(5.dp, thumbShape, clip = false)
                .clip(thumbShape)
                .background(Color.White, thumbShape)
            }
          )
    )
  }
}

enum class LiquidToastType {
  INFO,
  SUCCESS,
  WARNING,
  ERROR,
}

data class LiquidToastData(
  val id: Long,
  val message: String,
  val type: LiquidToastType = LiquidToastType.INFO,
  val durationMillis: Long = 2600L,
)

@Stable
class LiquidToastHostState {
  var currentToast by mutableStateOf<LiquidToastData?>(null)
    private set

  private var nextId = 1L

  fun show(
    message: String,
    type: LiquidToastType = LiquidToastType.INFO,
    durationMillis: Long = 2600L,
  ) {
    currentToast =
      LiquidToastData(
        id = nextId++,
        message = message,
        type = type,
        durationMillis = durationMillis,
      )
  }

  fun dismiss() {
    currentToast = null
  }
}

val LocalLiquidToastHost = staticCompositionLocalOf<LiquidToastHostState?> { null }

object LiquidToastBus {
  private val _events =
    kotlinx.coroutines.flow.MutableSharedFlow<Pair<String, LiquidToastType>>(
      extraBufferCapacity = 8
    )
  val events: kotlinx.coroutines.flow.SharedFlow<Pair<String, LiquidToastType>> = _events

  fun post(message: String, type: LiquidToastType = LiquidToastType.INFO) {
    _events.tryEmit(message to type)
  }
}

/**
 * Floating iOS 26 / Dynamic Island-inspired Liquid Glass Toast host.
 *
 * Uses Kyant0 `liquidGlass` over the live scene backdrop with spring enter/exit transitions,
 * interactive squish on tap, and swipe-up-to-dismiss.
 */
@Composable
fun LiquidToastOverlay(
  hostState: LiquidToastHostState,
  modifier: Modifier = Modifier,
) {
  val toast = hostState.currentToast
  val config = LocalLiquidGlassConfig.current
  val palette = LiquidGlassDefaults.palette()

  LaunchedEffect(toast?.id) {
    if (toast != null) {
      delay(toast.durationMillis)
      if (hostState.currentToast?.id == toast.id) {
        hostState.dismiss()
      }
    }
  }

  Box(
    contentAlignment = Alignment.TopCenter,
    modifier =
      modifier
        .fillMaxWidth()
        .statusBarsPadding()
        .padding(top = 66.dp, start = 20.dp, end = 20.dp),
  ) {
    AnimatedVisibility(
      visible = toast != null,
      enter =
        slideInVertically(animationSpec = config.navSlideSpring()) { -it } +
          scaleIn(initialScale = 0.86f, animationSpec = config.bouncySpatialSpring()) +
          fadeIn(config.smoothSpatialSpring()),
      exit =
        slideOutVertically(animationSpec = config.navSlideSpring()) { -it } +
          scaleOut(targetScale = 0.88f, animationSpec = config.smoothSpatialSpring()) +
          fadeOut(config.smoothSpatialSpring()),
    ) {
      val current = toast ?: return@AnimatedVisibility
      var dragOffsetY by remember(current.id) { mutableFloatStateOf(0f) }
      val accentColor =
        when (current.type) {
          LiquidToastType.SUCCESS -> Color(0xFF34C759)
          LiquidToastType.WARNING -> Color(0xFFFF9F0A)
          LiquidToastType.ERROR -> Color(0xFFFF453A)
          LiquidToastType.INFO -> MaterialTheme.colorScheme.primary
        }
      val icon =
        when (current.type) {
          LiquidToastType.SUCCESS -> Icons.Default.CheckCircle
          LiquidToastType.WARNING -> Icons.Default.Warning
          LiquidToastType.ERROR -> Icons.Default.Error
          LiquidToastType.INFO -> Icons.Default.Info
        }
      val backdrop = LocalLiquidBackdrop.current ?: LocalAtmosphericBackdrop.current

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier =
          Modifier.widthIn(max = 420.dp)
            .offset { IntOffset(0, dragOffsetY.roundToInt().coerceAtMost(24)) }
            .liquidButtonSquish()
            .liquidGlass(
              backdrop = backdrop,
              shape = LiquidGlassShapes.CapsuleShape,
              surfaceTint =
                if (palette.isDark) Color(0xFF1A1E28).copy(alpha = 0.72f)
                else Color.White.copy(alpha = 0.78f),
              blurRadius = 10.dp,
              refractionHeight = 16.dp,
              refractionAmount = 32.dp,
              chromaticAberration = true,
              shadowRadius = 22.dp,
              shadowOffsetY = 8.dp,
            )
            .pointerInput(current.id) {
              detectVerticalDragGestures(
                onVerticalDrag = { _, dragAmount ->
                  dragOffsetY = (dragOffsetY + dragAmount).coerceAtMost(24f)
                  if (dragOffsetY < -42f) {
                    hostState.dismiss()
                  }
                },
                onDragEnd = { dragOffsetY = 0f },
                onDragCancel = { dragOffsetY = 0f },
              )
            }
            .clickable { hostState.dismiss() }
            .padding(horizontal = 16.dp, vertical = 11.dp),
      ) {
        Box(
          contentAlignment = Alignment.Center,
          modifier =
            Modifier.size(28.dp)
              .clip(LiquidGlassShapes.CapsuleShape)
              .background(accentColor.copy(alpha = 0.22f)),
        ) {
          Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.size(18.dp),
          )
        }
        Spacer(Modifier.width(0.dp))
        Text(
          text = current.message,
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.Medium,
          color = if (palette.isDark) Color.White else MaterialTheme.colorScheme.onSurface,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}

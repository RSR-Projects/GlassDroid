package org.fdroid.ui.glass

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.BackdropEffectScope
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.highlight.HighlightStyle
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.capsule.ContinuousCapsule
import com.kyant.capsule.ContinuousRoundedRectangle

/**
 * Provides the [LayerBackdrop] recorded from scrolling screen content so floating top bars,
 * bottom navigation docks, rail containers, toasts, and floating buttons refract live content underneath.
 */
val LocalLiquidBackdrop = staticCompositionLocalOf<LayerBackdrop?> { null }

/**
 * Provides a root atmospheric [LayerBackdrop] behind scrollable scenes.
 */
val LocalAtmosphericBackdrop = staticCompositionLocalOf<LayerBackdrop?> { null }

/**
 * Extra bottom clearance reserved for the floating iOS-style bottom navigation dock when visible,
 * applied via list [androidx.compose.foundation.layout.PaddingValues] so content scrolls behind
 * the floating dock cleanly.
 */
val LocalBottomFloatingBarInset = staticCompositionLocalOf { 0.dp }

/**
 * Extra top clearance reserved when a screen uses [LiquidScaffold] with a floating top bar.
 */
val LocalTopFloatingBarInset = staticCompositionLocalOf { 0.dp }

@Immutable
data class LiquidGlassPalette(
  val isDark: Boolean,
  val dockSurfaceTint: Color,
  val pillSurfaceTint: Color,
  val barSurfaceTint: Color,
  val cardSurfaceTint: Color,
  val selectedPillTint: Color,
  val primaryButtonTint: Color,
  val chipSurfaceTint: Color,
  val chipSelectedTint: Color,
  val fallbackRimTop: Color,
  val fallbackRimBottom: Color,
  val innerGlowColor: Color,
  val innerScatterTop: Color,
  val focusedRimColor: Color,
  val canvasBackgroundGradient: List<Color>,
)

object LiquidGlassShapes {
  val CapsuleShape: ContinuousRoundedRectangle = ContinuousCapsule
  val BarPillShape: ContinuousRoundedRectangle = ContinuousCapsule
  val DockShape: ContinuousRoundedRectangle = ContinuousCapsule
  val CardShape: ContinuousRoundedRectangle = ContinuousRoundedRectangle(24.dp)
  val SmallCardShape: ContinuousRoundedRectangle = ContinuousRoundedRectangle(18.dp)
  val SheetShape: ContinuousRoundedRectangle = ContinuousRoundedRectangle(30.dp)
  val DialogShape: ContinuousRoundedRectangle = ContinuousRoundedRectangle(28.dp)
  val MenuShape: ContinuousRoundedRectangle = ContinuousRoundedRectangle(22.dp)
  val ChipShape: ContinuousRoundedRectangle = ContinuousCapsule
  val RailShape: ContinuousRoundedRectangle = ContinuousRoundedRectangle(32.dp)
}

object LiquidGlassDefaults {

  @Composable
  @ReadOnlyComposable
  fun palette(): LiquidGlassPalette {
    val colorScheme = MaterialTheme.colorScheme
    val config = LocalLiquidGlassConfig.current
    val isDark = isSystemInDarkTheme() || isLuminanceDark(colorScheme.background)
    val accent =
      when (config.glassTint) {
        GlassTintPreset.ADAPTIVE -> colorScheme.primary
        GlassTintPreset.NEUTRAL -> if (isDark) Color(0xFF8E9BAE) else Color(0xFF64748B)
        else -> config.glassTint.swatchColor
      }
    val tintWeight =
      when (config.glassTint) {
        GlassTintPreset.ADAPTIVE -> (0.04f + config.tintStrength * 0.08f).coerceIn(0f, 0.16f)
        GlassTintPreset.NEUTRAL -> 0f
        else -> (0.08f + config.tintStrength * 0.16f).coerceIn(0.04f, 0.28f)
      }
    val trFactor = (1.36f - config.transparency * 0.82f).coerceIn(0.24f, 1.42f)
    val hiBoost = config.highlightIntensity.coerceIn(0.2f, 1.6f)

    fun tintBase(base: Color): Color {
      val tinted =
        if (tintWeight > 0.005f) accent.copy(alpha = tintWeight).compositeOver(base.copy(alpha = 1f))
        else base
      return tinted.copy(alpha = (base.alpha * trFactor).coerceIn(0.08f, 0.96f))
    }

    val baseBg = colorScheme.background
    return if (isDark) {
      LiquidGlassPalette(
        isDark = true,
        dockSurfaceTint = tintBase(colorScheme.surfaceContainerHigh.copy(alpha = 0.56f)),
        pillSurfaceTint = tintBase(colorScheme.surfaceContainerHighest.copy(alpha = 0.58f)),
        barSurfaceTint = tintBase(colorScheme.surfaceContainerHigh.copy(alpha = 0.58f)),
        cardSurfaceTint = tintBase(colorScheme.surfaceContainer.copy(alpha = 0.68f)),
        selectedPillTint =
          if (config.glassTint == GlassTintPreset.ADAPTIVE || config.glassTint == GlassTintPreset.NEUTRAL) {
            Color.White.copy(alpha = (0.16f * (0.65f + 0.35f * hiBoost)).coerceIn(0.08f, 0.28f))
          } else {
            accent.copy(alpha = 0.26f)
          },
        primaryButtonTint = accent.copy(alpha = 0.88f),
        chipSurfaceTint = tintBase(colorScheme.surfaceContainerHigh.copy(alpha = 0.62f)),
        chipSelectedTint = accent.copy(alpha = 0.28f),
        fallbackRimTop = Color.White.copy(alpha = (0.40f * hiBoost).coerceIn(0.10f, 0.72f)),
        fallbackRimBottom = Color.White.copy(alpha = (0.09f * hiBoost).coerceIn(0.03f, 0.22f)),
        innerGlowColor = Color.White.copy(alpha = (0.16f * hiBoost).coerceIn(0.04f, 0.32f)),
        innerScatterTop = Color.White.copy(alpha = (0.08f * hiBoost).coerceIn(0.02f, 0.20f)),
        focusedRimColor = accent.copy(alpha = (0.72f * hiBoost).coerceIn(0.30f, 0.95f)),
        canvasBackgroundGradient =
          listOf(
            baseBg,
            accent.copy(alpha = 0.11f).compositeOver(baseBg),
            colorScheme.tertiary.copy(alpha = 0.08f).compositeOver(baseBg),
          ),
      )
    } else {
      LiquidGlassPalette(
        isDark = false,
        dockSurfaceTint = tintBase(Color.White.copy(alpha = 0.54f)),
        pillSurfaceTint = tintBase(Color.White.copy(alpha = 0.60f)),
        barSurfaceTint = tintBase(Color.White.copy(alpha = 0.58f)),
        cardSurfaceTint = tintBase(Color.White.copy(alpha = 0.74f)),
        selectedPillTint = accent.copy(alpha = 0.14f),
        primaryButtonTint = accent.copy(alpha = 0.90f),
        chipSurfaceTint = tintBase(Color.White.copy(alpha = 0.70f)),
        chipSelectedTint = accent.copy(alpha = 0.18f),
        fallbackRimTop = Color.White.copy(alpha = (0.95f * hiBoost).coerceIn(0.25f, 1.0f)),
        fallbackRimBottom = Color.White.copy(alpha = (0.34f * hiBoost).coerceIn(0.10f, 0.60f)),
        innerGlowColor = Color.White.copy(alpha = (0.42f * hiBoost).coerceIn(0.10f, 0.68f)),
        innerScatterTop = Color.White.copy(alpha = (0.28f * hiBoost).coerceIn(0.06f, 0.48f)),
        focusedRimColor = accent.copy(alpha = (0.65f * hiBoost).coerceIn(0.28f, 0.95f)),
        canvasBackgroundGradient =
          listOf(
            baseBg,
            accent.copy(alpha = 0.09f).compositeOver(baseBg),
            colorScheme.tertiary.copy(alpha = 0.06f).compositeOver(baseBg),
          ),
      )
    }
  }

  private fun isLuminanceDark(color: Color): Boolean {
    val luma = 0.2126f * color.red + 0.7152f * color.green + 0.0722f * color.blue
    return luma < 0.4f
  }

  fun highlight(isDark: Boolean, intensity: Float = 1f): Highlight {
    val boost = intensity.coerceIn(0.15f, 1.65f)
    return Highlight.Default.copy(
      width = if (isDark) 0.9.dp else 1.0.dp,
      blurRadius = 1.2.dp,
      alpha = ((if (isDark) 0.86f else 0.95f) * boost).coerceIn(0f, 1f),
      style =
        HighlightStyle.Default(
          color = Color.White.copy(alpha = if (isDark) 0.88f else 0.98f),
          blendMode = BlendMode.Plus,
          angle = 45f,
          falloff = 1.2f,
        ),
    )
  }

  fun ambientHighlight(isDark: Boolean, intensity: Float = 1f): Highlight =
    Highlight.Ambient.copy(
      width = 0.7.dp,
      blurRadius = 0.9.dp,
      alpha = ((if (isDark) 0.56f else 0.76f) * intensity.coerceIn(0.2f, 1.5f)).coerceIn(0f, 1f),
    )

  fun floatingShadow(isDark: Boolean, radius: Dp = 20.dp, offsetY: Dp = 8.dp): Shadow =
    Shadow.Default.copy(
      radius = radius,
      offset = DpOffset(0.dp, offsetY),
      color = Color.Black,
      alpha = if (isDark) 0.42f else 0.14f,
    )

  fun pillInnerShadow(isDark: Boolean): InnerShadow =
    InnerShadow.Default.copy(
      radius = 8.dp,
      offset = DpOffset(0.dp, 1.5.dp),
      color = Color.Black,
      alpha = if (isDark) 0.26f else 0.11f,
    )
}

/**
 * Lightweight, high-framerate iOS 26 specular glass surface for high-density scrolling list items,
 * cards, and chips. Renders translucent tinted glass, upper internal light scattering, and
 * adaptive rounded-corner rim illumination via [drawWithCache] without per-item RenderEffect blur
 * allocations so scrolling stays fluid on mid-range GPUs.
 */
@Composable
fun Modifier.fastLiquidSurface(
  shape: Shape = LiquidGlassShapes.CardShape,
  surfaceTint: Color = LiquidGlassDefaults.palette().cardSurfaceTint,
  withShadow: Boolean = false,
  respectUserConfig: Boolean = true,
  borderColor: Color? = null,
  isHighlighted: Boolean = false,
): Modifier {
  val palette = LiquidGlassDefaults.palette()
  val config = LocalLiquidGlassConfig.current
  val targetSurface =
    if (respectUserConfig) {
      val tintFactor = (0.35f + config.tintStrength * 1.15f).coerceIn(0.12f, 1.45f)
      val baseAlpha = (surfaceTint.alpha * tintFactor).coerceIn(0.08f, 0.96f)
      val brightOffset = (config.brightness * 0.25f).coerceIn(-0.10f, 0.12f)
      if (brightOffset > 0f) {
        Color.White.copy(alpha = brightOffset).compositeOver(surfaceTint.copy(alpha = baseAlpha))
      } else {
        surfaceTint.copy(alpha = baseAlpha)
      }
    } else {
      surfaceTint
    }
  val animatedSurface by
    animateColorAsState(
      targetValue = targetSurface,
      animationSpec = config.colorSpring(),
      label = "fast_glass_surface",
    )
  val targetRimTop =
    when {
      borderColor != null -> borderColor
      isHighlighted -> palette.focusedRimColor
      else -> palette.fallbackRimTop
    }
  val targetRimBottom =
    when {
      borderColor != null -> borderColor.copy(alpha = borderColor.alpha * 0.52f)
      isHighlighted -> palette.focusedRimColor.copy(alpha = 0.45f)
      else -> palette.fallbackRimBottom
    }
  val scatterColor =
    if (isHighlighted) palette.focusedRimColor.copy(alpha = if (palette.isDark) 0.16f else 0.22f)
    else palette.innerScatterTop
  val strokeWidth = if (borderColor != null || isHighlighted) 1.15.dp else 0.9.dp

  return this
    .then(
      if (withShadow && !config.reducedVisualEffects) {
        Modifier.shadow(elevation = 6.dp, shape = shape, clip = false)
      } else {
        Modifier
      }
    )
    .clip(shape)
    .drawWithCache {
      val rimBrush =
        Brush.verticalGradient(
          colors = listOf(targetRimTop, targetRimTop.copy(alpha = targetRimTop.alpha * 0.35f), targetRimBottom)
        )
      val scatterBrush =
        Brush.verticalGradient(
          colors = listOf(scatterColor, Color.Transparent),
          startY = 0f,
          endY = size.height * 0.46f,
        )
      val outline = shape.createOutline(size, layoutDirection, this)
      val strokePx = strokeWidth.toPx()
      onDrawBehind {
        drawOutline(outline = outline, color = animatedSurface)
        if (!config.reducedVisualEffects) {
          drawOutline(outline = outline, brush = scatterBrush)
        }
        drawOutline(outline = outline, brush = rimBrush, style = Stroke(width = strokePx))
      }
    }
}

/**
 * Official Kyant0 Liquid Glass surface modifier wrapping [drawBackdrop], dynamically driven by
 * [LocalLiquidGlassConfig] and optional scroll-depth adaptation ([scrollResponsive]).
 *
 * Uses optical [vibrancy], [colorControls], smooth bounded [blur], natural corner [lens] refraction
 * with subtle prism dispersion, upper-surface internal light scattering, specular [Highlight], and
 * floating [Shadow]. Falls back cleanly to [fastLiquidSurface] when no [Backdrop] is available or
 * [LiquidGlassConfig.reducedVisualEffects] is enabled.
 */
@Composable
fun Modifier.liquidGlass(
  backdrop: Backdrop? = LocalLiquidBackdrop.current ?: LocalAtmosphericBackdrop.current,
  shape: Shape = LiquidGlassShapes.CapsuleShape,
  surfaceTint: Color = LiquidGlassDefaults.palette().pillSurfaceTint,
  blurRadius: Dp = 6.dp,
  refractionHeight: Dp = 18.dp,
  refractionAmount: Dp = 36.dp,
  depthEffect: Boolean = true,
  chromaticAberration: Boolean = true,
  withShadow: Boolean = true,
  shadowRadius: Dp = 16.dp,
  shadowOffsetY: Dp = 6.dp,
  highlight: Highlight? = null,
  innerShadow: InnerShadow? = null,
  respectUserConfig: Boolean = true,
  scrollResponsive: Boolean = false,
  animateParameters: Boolean = false,
  layerBlock: (GraphicsLayerScope.() -> Unit)? = null,
  extraEffects: (BackdropEffectScope.() -> Unit)? = null,
): Modifier {
  val palette = LiquidGlassDefaults.palette()
  val config = LocalLiquidGlassConfig.current
  val scrollFraction = if (scrollResponsive) (LocalLiquidScrollState.current?.scrollFraction ?: 0f) else 0f

  val targetBlur =
    (if (respectUserConfig) blurRadius * config.blurScale else blurRadius) +
      (1.8.dp * scrollFraction)
  val boundedBlur = targetBlur.coerceIn(0.dp, 14.dp)
  val effectiveBlur =
    if (animateParameters) {
      animateFloatAsState(
          targetValue = boundedBlur.value,
          animationSpec = config.smoothSpatialSpring(),
          label = "glass_blur",
        )
        .value
        .dp
    } else {
      boundedBlur
    }

  val targetRefractionHeight =
    (if (respectUserConfig) refractionHeight * config.refractionHeightScale else refractionHeight)
      .coerceIn(0.dp, 24.dp)
  val targetRefractionAmount =
    (if (respectUserConfig) refractionAmount * config.refractionAmountScale else refractionAmount)
      .coerceIn(0.dp, 48.dp)
  val effectiveRefractionHeight =
    if (animateParameters) {
      animateFloatAsState(
          targetValue = targetRefractionHeight.value,
          animationSpec = config.smoothSpatialSpring(),
          label = "glass_rh",
        )
        .value
        .dp
    } else {
      targetRefractionHeight
    }
  val effectiveRefractionAmount =
    if (animateParameters) {
      animateFloatAsState(
          targetValue = targetRefractionAmount.value,
          animationSpec = config.smoothSpatialSpring(),
          label = "glass_ra",
        )
        .value
        .dp
    } else {
      targetRefractionAmount
    }

  val effectiveChromatic =
    if (respectUserConfig) chromaticAberration && config.chromaticAberration else chromaticAberration

  val targetSurfaceTint =
    if (respectUserConfig) {
      val tintFactor = (0.12f + config.tintStrength * 1.45f).coerceIn(0.02f, 1.45f)
      val scrollBoost = 0.08f * scrollFraction
      surfaceTint.copy(alpha = (surfaceTint.alpha * tintFactor + scrollBoost).coerceIn(0f, 0.96f))
    } else {
      surfaceTint.copy(alpha = (surfaceTint.alpha + 0.06f * scrollFraction).coerceIn(0f, 0.96f))
    }
  val effectiveSurfaceTint =
    if (animateParameters) {
      animateColorAsState(targetSurfaceTint, config.colorSpring(), label = "glass_tint").value
    } else {
      targetSurfaceTint
    }

  val highlightIntensity by
    animateFloatAsState(
      targetValue =
        (if (respectUserConfig) config.highlightIntensity else 1f) * (1f + 0.18f * scrollFraction),
      animationSpec = config.smoothSpatialSpring(),
      label = "glass_highlight_intensity",
    )
  val resolvedHighlight =
    remember(palette.isDark, highlight, highlightIntensity) {
      highlight ?: LiquidGlassDefaults.highlight(palette.isDark, highlightIntensity)
    }
  val resolvedShadow =
    remember(palette.isDark, withShadow, shadowRadius, shadowOffsetY) {
      if (withShadow) LiquidGlassDefaults.floatingShadow(palette.isDark, shadowRadius, shadowOffsetY)
      else null
    }
  val resolvedInnerShadow =
    remember(palette.isDark, innerShadow) {
      innerShadow ?: LiquidGlassDefaults.pillInnerShadow(palette.isDark)
    }

  if (backdrop == null || (respectUserConfig && config.reducedVisualEffects)) {
    return this.fastLiquidSurface(
      shape = shape,
      surfaceTint = effectiveSurfaceTint,
      withShadow = withShadow,
      respectUserConfig = false,
      isHighlighted = scrollFraction > 0.4f,
    )
  }

  val hiScale = if (respectUserConfig) config.highlightIntensity.coerceIn(0.2f, 1.6f) else 1f
  val scatterAlpha = ((if (palette.isDark) 0.065f else 0.22f) * hiScale).coerceIn(0f, 0.45f)
  return this.drawBackdrop(
    backdrop = backdrop,
    shape = { shape },
    effects = {
      if (!respectUserConfig || config.vibrancyScale >= 0.85f) {
        vibrancy()
      }
      if (respectUserConfig && (config.brightness != 0f || config.vibrancyScale != 1f)) {
        val sat = (0.65f + config.vibrancyScale * 0.45f).coerceIn(0.2f, 2.2f)
        colorControls(
          brightness = config.brightness.coerceIn(-0.35f, 0.35f),
          saturation = sat,
        )
      }
      if (effectiveBlur > 0.2.dp) {
        blur(effectiveBlur.toPx())
      }
      if (effectiveRefractionHeight > 0.5.dp && effectiveRefractionAmount > 0.5.dp) {
        lens(
          refractionHeight = effectiveRefractionHeight.toPx(),
          refractionAmount = effectiveRefractionAmount.toPx(),
          depthEffect = depthEffect,
          chromaticAberration = effectiveChromatic,
        )
      }
      extraEffects?.invoke(this)
    },
    highlight = { resolvedHighlight },
    shadow = { resolvedShadow },
    innerShadow = { resolvedInnerShadow },
    layerBlock = layerBlock,
    onDrawSurface = {
      if (effectiveSurfaceTint.alpha > 0.005f) {
        drawRect(effectiveSurfaceTint)
      }
      // Subtle upper-surface light scattering for optical glass thickness
      if (scatterAlpha > 0.004f) {
        drawRect(
          brush =
            Brush.verticalGradient(
              colors = listOf(Color.White.copy(alpha = scatterAlpha), Color.Transparent),
              startY = 0f,
              endY = size.height * 0.48f,
            )
        )
      }
    },
  )
}

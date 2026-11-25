package org.fdroid.ui.glass

import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

enum class GlassProfile {
  ADAPTIVE,
  CLEAR,
  FROSTED,
  DEEP,
  CUSTOM;

  companion object {
    fun fromPrefString(value: String?): GlassProfile? =
      when (value) {
        null -> null
        "DEFAULT" -> ADAPTIVE
        "CRYSTAL" -> CLEAR
        "READABLE" -> FROSTED
        else -> runCatching { valueOf(value) }.getOrNull()
      }
  }
}

enum class GlassTintPreset(val swatchColor: Color) {
  ADAPTIVE(Color(0xFF0A84FF)),
  OCEAN(Color(0xFF0A84FF)),
  VIOLET(Color(0xFF8E55FF)),
  EMERALD(Color(0xFF34C759)),
  AMBER(Color(0xFFFF9F0A)),
  ROSE(Color(0xFFFF375F)),
  NEUTRAL(Color(0xFFFFFFFF));

  companion object {
    fun fromPrefString(value: String?): GlassTintPreset =
      if (value == null) ADAPTIVE else runCatching { valueOf(value) }.getOrDefault(ADAPTIVE)
  }
}

enum class MotionProfile {
  NORMAL,
  PERFORMANCE,
  BOUNCY,
  CUSTOM,
}

@Immutable
data class LiquidGlassConfig(
  val glassProfile: GlassProfile = GlassProfile.ADAPTIVE,
  val glassTint: GlassTintPreset = GlassTintPreset.ADAPTIVE,
  val blurScale: Float = 0.85f,
  val transparency: Float = 0.52f,
  val refractionHeightScale: Float = 1.25f,
  val refractionAmountScale: Float = 1.25f,
  val tintStrength: Float = 0.45f,
  val highlightIntensity: Float = 1.00f,
  val vibrancyScale: Float = 1.50f,
  val brightness: Float = 0.0f,
  val chromaticAberration: Boolean = true,
  val reducedVisualEffects: Boolean = false,
  val motionProfile: MotionProfile = MotionProfile.BOUNCY,
  val animationsEnabled: Boolean = true,
  val animationIntensity: Float = 1.00f,
  val bounce: Float = 0.90f,
  val speed: Float = 1.05f,
  val pressDepth: Float = 1.55f,
  val releaseJiggle: Float = 1.75f,
  val dragStretch: Float = 1.60f,
  val longPressBloom: Float = 1.55f,
) {
  fun withGlassProfile(profile: GlassProfile): LiquidGlassConfig =
    when (profile) {
      GlassProfile.ADAPTIVE ->
        copy(
          glassProfile = GlassProfile.ADAPTIVE,
          blurScale = 0.85f,
          transparency = 0.52f,
          refractionHeightScale = 1.25f,
          refractionAmountScale = 1.25f,
          tintStrength = 0.45f,
          highlightIntensity = 1.00f,
          vibrancyScale = 1.50f,
          brightness = 0.0f,
          chromaticAberration = true,
        )
      GlassProfile.CLEAR ->
        copy(
          glassProfile = GlassProfile.CLEAR,
          blurScale = 0.0f,
          transparency = 0.88f,
          refractionHeightScale = 2.00f,
          refractionAmountScale = 2.00f,
          tintStrength = 0.14f,
          highlightIntensity = 1.32f,
          vibrancyScale = 2.20f,
          brightness = -0.02f,
          chromaticAberration = true,
        )
      GlassProfile.FROSTED ->
        copy(
          glassProfile = GlassProfile.FROSTED,
          blurScale = 1.48f,
          transparency = 0.28f,
          refractionHeightScale = 0.85f,
          refractionAmountScale = 0.85f,
          tintStrength = 0.72f,
          highlightIntensity = 0.92f,
          vibrancyScale = 1.15f,
          brightness = 0.02f,
          chromaticAberration = false,
        )
      GlassProfile.DEEP ->
        copy(
          glassProfile = GlassProfile.DEEP,
          blurScale = 1.65f,
          transparency = 0.36f,
          refractionHeightScale = 1.75f,
          refractionAmountScale = 1.80f,
          tintStrength = 0.84f,
          highlightIntensity = 1.22f,
          vibrancyScale = 1.85f,
          brightness = -0.10f,
          chromaticAberration = true,
        )
      GlassProfile.CUSTOM -> copy(glassProfile = GlassProfile.CUSTOM)
    }

  fun withMotionProfile(profile: MotionProfile): LiquidGlassConfig =
    when (profile) {
      MotionProfile.NORMAL ->
        copy(
          motionProfile = MotionProfile.NORMAL,
          animationIntensity = 0.85f,
          bounce = 0.55f,
          speed = 1.15f,
          pressDepth = 1.15f,
          releaseJiggle = 1.00f,
          dragStretch = 1.10f,
          longPressBloom = 1.15f,
        )
      MotionProfile.PERFORMANCE ->
        copy(
          motionProfile = MotionProfile.PERFORMANCE,
          animationIntensity = 0.65f,
          bounce = 0.12f,
          speed = 1.85f,
          pressDepth = 0.75f,
          releaseJiggle = 0.15f,
          dragStretch = 0.55f,
          longPressBloom = 0.70f,
        )
      MotionProfile.BOUNCY ->
        copy(
          motionProfile = MotionProfile.BOUNCY,
          animationIntensity = 1.00f,
          bounce = 1.00f,
          speed = 0.95f,
          pressDepth = 1.85f,
          releaseJiggle = 2.35f,
          dragStretch = 1.95f,
          longPressBloom = 2.05f,
        )
      MotionProfile.CUSTOM -> copy(motionProfile = MotionProfile.CUSTOM)
    }

  fun resetGlass(): LiquidGlassConfig =
    withGlassProfile(GlassProfile.ADAPTIVE)
      .copy(glassTint = GlassTintPreset.ADAPTIVE, reducedVisualEffects = false)

  fun resetMotion(): LiquidGlassConfig =
    withMotionProfile(MotionProfile.BOUNCY)
      .copy(animationsEnabled = true, animationIntensity = 1.00f)

  private fun intensityScale(): Float = animationIntensity.coerceIn(0.25f, 1.50f)

  fun effectivePressDepth(): Float = (pressDepth * intensityScale()).coerceIn(0.25f, 2.8f)

  fun effectiveReleaseJiggle(): Float = (releaseJiggle * intensityScale()).coerceIn(0f, 3.2f)

  fun effectiveDragStretch(): Float = (dragStretch * intensityScale()).coerceIn(0f, 2.8f)

  fun effectiveLongPressBloom(): Float = (longPressBloom * intensityScale()).coerceIn(0.25f, 2.8f)

  private fun speedSq(): Float = (speed * speed).coerceIn(0.16f, 4.2f)

  /** Maps [bounce] (0..1.2) modulated by [animationIntensity] safely within spring bounds. */
  fun pressDampingRatio(): Float =
    (0.94f - (bounce * intensityScale()).coerceIn(0f, 1.4f) * 0.44f).coerceIn(0.28f, 0.98f)

  fun releaseDampingRatio(): Float {
    val jiggleInfluence = (effectiveReleaseJiggle() / 3f).coerceIn(0f, 1f)
    return (0.96f - (bounce * intensityScale()).coerceIn(0f, 1.4f) * 0.52f - jiggleInfluence * 0.24f)
      .coerceIn(0.18f, 0.98f)
  }

  fun jiggleDampingRatio(): Float =
    (0.68f - (bounce * intensityScale()).coerceIn(0f, 1.4f) * 0.36f).coerceIn(0.14f, 0.92f)

  fun pressStiffness(): Float = (760f * speedSq()).coerceIn(140f, 2600f)

  fun releaseStiffness(): Float = (440f * speedSq()).coerceIn(85f, 1900f)

  fun jiggleStiffness(): Float = (360f * speedSq()).coerceIn(70f, 1700f)

  fun tabIndicatorSpring(): SpringSpec<Float> =
    spring(
      dampingRatio = (0.92f - (bounce * intensityScale()).coerceIn(0f, 1.4f) * 0.48f).coerceIn(0.22f, 0.96f),
      stiffness = (420f * speedSq()).coerceIn(90f, 1800f),
    )

  fun pressSpring(): SpringSpec<Float> =
    spring(dampingRatio = pressDampingRatio(), stiffness = pressStiffness())

  fun releaseSpring(): SpringSpec<Float> =
    spring(dampingRatio = releaseDampingRatio(), stiffness = releaseStiffness())

  fun jiggleSpring(): SpringSpec<Float> =
    spring(dampingRatio = jiggleDampingRatio(), stiffness = jiggleStiffness())

  fun offsetSpring(): SpringSpec<Offset> =
    spring(dampingRatio = releaseDampingRatio(), stiffness = releaseStiffness())

  fun bouncySpatialSpring(): SpringSpec<Float> =
    spring(
      dampingRatio = (0.90f - (bounce * intensityScale()).coerceIn(0f, 1.4f) * 0.46f).coerceIn(0.22f, 0.96f),
      stiffness = (440f * speedSq()).coerceIn(90f, 1850f),
    )

  fun smoothSpatialSpring(): SpringSpec<Float> =
    spring(
      dampingRatio = (0.96f - (bounce * intensityScale()).coerceIn(0f, 1.4f) * 0.14f).coerceIn(0.65f, 0.98f),
      stiffness = (540f * speedSq()).coerceIn(120f, 2200f),
    )

  fun navSlideSpring(): SpringSpec<IntOffset> =
    spring(
      dampingRatio = (0.94f - (bounce * intensityScale()).coerceIn(0f, 1.4f) * 0.26f).coerceIn(0.45f, 0.98f),
      stiffness = (460f * speedSq()).coerceIn(110f, 2000f),
    )

  fun expandSizeSpring(): SpringSpec<IntSize> =
    spring(
      dampingRatio = (0.92f - (bounce * intensityScale()).coerceIn(0f, 1.4f) * 0.28f).coerceIn(0.42f, 0.96f),
      stiffness = (460f * speedSq()).coerceIn(110f, 1900f),
    )

  fun colorSpring(): SpringSpec<Color> =
    spring(dampingRatio = 0.85f, stiffness = (560f * speedSq()).coerceIn(140f, 2200f))
}

val LocalLiquidGlassConfig = staticCompositionLocalOf { LiquidGlassConfig() }

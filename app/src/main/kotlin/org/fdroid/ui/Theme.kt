package org.fdroid.ui

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import org.fdroid.ui.glass.LiquidGlassConfig
import org.fdroid.ui.glass.LiquidGlassDefaults
import org.fdroid.ui.glass.LiquidTouchIndication
import org.fdroid.ui.glass.LocalAtmosphericBackdrop
import org.fdroid.ui.glass.LocalLiquidGlassConfig
import org.fdroid.ui.glass.LocalReducedMotion
import org.fdroid.ui.glass.rememberSystemReducedMotion

private val lightScheme =
  lightColorScheme(
    primary = primaryLight,
    onPrimary = onPrimaryLight,
    primaryContainer = primaryContainerLight,
    onPrimaryContainer = onPrimaryContainerLight,
    secondary = secondaryLight,
    onSecondary = onSecondaryLight,
    secondaryContainer = secondaryContainerLight,
    onSecondaryContainer = onSecondaryContainerLight,
    tertiary = tertiaryLight,
    onTertiary = onTertiaryLight,
    tertiaryContainer = tertiaryContainerLight,
    onTertiaryContainer = onTertiaryContainerLight,
    error = errorLight,
    onError = onErrorLight,
    errorContainer = errorContainerLight,
    onErrorContainer = onErrorContainerLight,
    background = backgroundLight,
    onBackground = onBackgroundLight,
    surface = surfaceLight,
    onSurface = onSurfaceLight,
    surfaceVariant = surfaceVariantLight,
    onSurfaceVariant = onSurfaceVariantLight,
    outline = outlineLight,
    outlineVariant = outlineVariantLight,
    scrim = scrimLight,
    inverseSurface = inverseSurfaceLight,
    inverseOnSurface = inverseOnSurfaceLight,
    inversePrimary = inversePrimaryLight,
    surfaceDim = surfaceDimLight,
    surfaceBright = surfaceBrightLight,
    surfaceContainerLowest = surfaceContainerLowestLight,
    surfaceContainerLow = surfaceContainerLowLight,
    surfaceContainer = surfaceContainerLight,
    surfaceContainerHigh = surfaceContainerHighLight,
    surfaceContainerHighest = surfaceContainerHighestLight,
  )

private val darkScheme =
  darkColorScheme(
    primary = primaryDark,
    onPrimary = onPrimaryDark,
    primaryContainer = primaryContainerDark,
    onPrimaryContainer = onPrimaryContainerDark,
    secondary = secondaryDark,
    onSecondary = onSecondaryDark,
    secondaryContainer = secondaryContainerDark,
    onSecondaryContainer = onSecondaryContainerDark,
    tertiary = tertiaryDark,
    onTertiary = onTertiaryDark,
    tertiaryContainer = tertiaryContainerDark,
    onTertiaryContainer = onTertiaryContainerDark,
    error = errorDark,
    onError = onErrorDark,
    errorContainer = errorContainerDark,
    onErrorContainer = onErrorContainerDark,
    background = backgroundDark,
    onBackground = onBackgroundDark,
    surface = surfaceDark,
    onSurface = onSurfaceDark,
    surfaceVariant = surfaceVariantDark,
    onSurfaceVariant = onSurfaceVariantDark,
    outline = outlineDark,
    outlineVariant = outlineVariantDark,
    scrim = scrimDark,
    inverseSurface = inverseSurfaceDark,
    inverseOnSurface = inverseOnSurfaceDark,
    inversePrimary = inversePrimaryDark,
    surfaceDim = surfaceDimDark,
    surfaceBright = surfaceBrightDark,
    surfaceContainerLowest = surfaceContainerLowestDark,
    surfaceContainerLow = surfaceContainerLowDark,
    surfaceContainer = surfaceContainerDark,
    surfaceContainerHigh = surfaceContainerHighDark,
    surfaceContainerHighest = surfaceContainerHighestDark,
  )

private val baseTypography = Typography()
private val LiquidGlassTypography =
  Typography(
    headlineSmall =
      baseTypography.headlineSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.2).sp),
    titleLarge =
      baseTypography.titleLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.15).sp),
    titleMedium =
      baseTypography.titleMedium.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp),
    titleSmall =
      baseTypography.titleSmall.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp),
    labelLarge =
      baseTypography.labelLarge.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.05.sp),
    labelSmall =
      baseTypography.labelSmall.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.1.sp),
  )

@Composable
private fun animateColorScheme(
  target: ColorScheme,
  config: LiquidGlassConfig,
): ColorScheme {
  val spec = config.colorSpring()
  val primary by animateColorAsState(target.primary, spec, label = "cs_primary")
  val onPrimary by animateColorAsState(target.onPrimary, spec, label = "cs_onPrimary")
  val primaryContainer by animateColorAsState(target.primaryContainer, spec, label = "cs_pc")
  val onPrimaryContainer by animateColorAsState(target.onPrimaryContainer, spec, label = "cs_opc")
  val secondary by animateColorAsState(target.secondary, spec, label = "cs_sec")
  val secondaryContainer by
    animateColorAsState(target.secondaryContainer, spec, label = "cs_sc")
  val onSecondaryContainer by
    animateColorAsState(target.onSecondaryContainer, spec, label = "cs_osc")
  val tertiary by animateColorAsState(target.tertiary, spec, label = "cs_tert")
  val tertiaryContainer by animateColorAsState(target.tertiaryContainer, spec, label = "cs_tc")
  val onTertiaryContainer by
    animateColorAsState(target.onTertiaryContainer, spec, label = "cs_otc")
  val background by animateColorAsState(target.background, spec, label = "cs_bg")
  val onBackground by animateColorAsState(target.onBackground, spec, label = "cs_obg")
  val surface by animateColorAsState(target.surface, spec, label = "cs_surf")
  val onSurface by animateColorAsState(target.onSurface, spec, label = "cs_osurf")
  val surfaceVariant by animateColorAsState(target.surfaceVariant, spec, label = "cs_sv")
  val onSurfaceVariant by animateColorAsState(target.onSurfaceVariant, spec, label = "cs_osv")
  val surfaceContainer by animateColorAsState(target.surfaceContainer, spec, label = "cs_scont")
  return target.copy(
    primary = primary,
    onPrimary = onPrimary,
    primaryContainer = primaryContainer,
    onPrimaryContainer = onPrimaryContainer,
    secondary = secondary,
    secondaryContainer = secondaryContainer,
    onSecondaryContainer = onSecondaryContainer,
    tertiary = tertiary,
    tertiaryContainer = tertiaryContainer,
    onTertiaryContainer = onTertiaryContainer,
    background = background,
    onBackground = onBackground,
    surface = surface,
    onSurface = onSurface,
    surfaceVariant = surfaceVariant,
    onSurfaceVariant = onSurfaceVariant,
    surfaceContainer = surfaceContainer,
  )
}

@Composable
fun FDroidContent(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColors: Boolean = false,
  liquidGlassConfig: LiquidGlassConfig = LiquidGlassConfig(),
  content: @Composable () -> Unit,
) {
  val context = LocalContext.current
  val targetScheme =
    when {
      dynamicColors && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }
      darkTheme -> darkScheme
      else -> lightScheme
    }
  val colorScheme = animateColorScheme(targetScheme, liquidGlassConfig)
  val reducedMotion = rememberSystemReducedMotion() || !liquidGlassConfig.animationsEnabled
  MaterialTheme(colorScheme = colorScheme, typography = LiquidGlassTypography) {
    CompositionLocalProvider(
      LocalLiquidGlassConfig provides liquidGlassConfig,
      LocalReducedMotion provides reducedMotion,
      LocalIndication provides LiquidTouchIndication,
    ) {
      val palette = LiquidGlassDefaults.palette()
      val atmosphericBrush = Brush.verticalGradient(palette.canvasBackgroundGradient)
      val bg = colorScheme.background
      val atmosphericBackdrop = rememberLayerBackdrop {
        drawRect(bg)
        drawRect(atmosphericBrush)
      }
      Box(propagateMinConstraints = true) {
        Box(
          modifier =
            Modifier.matchParentSize()
              .layerBackdrop(atmosphericBackdrop)
              .background(atmosphericBrush)
        )
        CompositionLocalProvider(
          LocalAtmosphericBackdrop provides atmosphericBackdrop,
          LocalContentColor provides colorScheme.onBackground,
        ) {
          Surface(
            color = Color.Transparent,
            contentColor = colorScheme.onBackground,
            content = content,
          )
        }
      }
    }
  }
}

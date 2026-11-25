package org.fdroid.ui.glass

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import org.fdroid.R
import org.fdroid.ui.FDroidContent
import org.fdroid.ui.utils.BackButton

@Composable
fun LiquidGlassCustomizer(
  config: LiquidGlassConfig,
  onConfigChange: (LiquidGlassConfig) -> Unit,
  onResetGlass: () -> Unit,
  onResetMotion: () -> Unit,
  onBackClicked: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val liveConfig = LocalLiquidGlassConfig.current
  var selectedTab by rememberSaveable { mutableIntStateOf(0) }
  val toastHost = LocalLiquidToastHost.current
  val tryItToastText = stringResource(R.string.liquid_glass_try_it_toast)
  val resetGlassToastText = stringResource(R.string.liquid_glass_reset_glass_toast)
  val resetMotionToastText = stringResource(R.string.liquid_glass_reset_motion_toast)

  LiquidScaffold(
    topBar = {
      LiquidFloatingTopBar(
        navigationIcon = { BackButton(onClick = onBackClicked) },
        title = { Text(text = stringResource(R.string.liquid_glass_title), maxLines = 1) },
        showActions = false,
      )
    },
    modifier = modifier,
  ) { paddingValues ->
    LazyColumn(
      contentPadding =
        PaddingValues(
          start = 16.dp,
          end = 16.dp,
          top = paddingValues.calculateTopPadding() + 8.dp,
          bottom = paddingValues.calculateBottomPadding() + 24.dp,
        ),
      verticalArrangement = Arrangement.spacedBy(14.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier.fillMaxSize(),
    ) {
      item(key = "tab_switcher") {
        CustomizerTabSwitcher(
          selectedTab = selectedTab,
          onSelectTab = { selectedTab = it },
          modifier = Modifier.widthIn(max = 560.dp),
        )
      }
      item(key = "tab_body") {
        AnimatedContent(
          targetState = selectedTab,
          transitionSpec = {
            fadeIn(liveConfig.smoothSpatialSpring()) togetherWith
              fadeOut(liveConfig.smoothSpatialSpring())
          },
          label = "customizer_tab",
          modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth(),
        ) { tab ->
          if (tab == 0) {
            GlassCustomizerTab(
              config = liveConfig,
              onConfigChange = onConfigChange,
              onResetGlass = {
                onResetGlass()
                toastHost?.show(resetGlassToastText, LiquidToastType.SUCCESS)
              },
            )
          } else {
            MotionCustomizerTab(
              config = liveConfig,
              onConfigChange = onConfigChange,
              onTryIt = {
                toastHost?.show(tryItToastText, LiquidToastType.INFO)
              },
              onResetMotion = {
                onResetMotion()
                toastHost?.show(resetMotionToastText, LiquidToastType.SUCCESS)
              },
            )
          }
        }
      }
    }
  }
}

@Composable
private fun CustomizerTabSwitcher(
  selectedTab: Int,
  onSelectTab: (Int) -> Unit,
  modifier: Modifier = Modifier,
) {
  val palette = LiquidGlassDefaults.palette()
  val config = LocalLiquidGlassConfig.current
  val backdrop = LocalAtmosphericBackdrop.current
  val tabs = listOf(
    stringResource(R.string.liquid_glass_tab_glass),
    stringResource(R.string.liquid_glass_tab_motion),
  )
  Row(
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    modifier =
      modifier
        .fillMaxWidth()
        .liquidGlass(
          backdrop = backdrop,
          shape = LiquidGlassShapes.CapsuleShape,
          surfaceTint = palette.cardSurfaceTint,
          blurRadius = 6.dp,
          refractionHeight = 14.dp,
          refractionAmount = 24.dp,
          withShadow = false,
        )
        .padding(5.dp)
        .testTag("customizer_tab_switcher"),
  ) {
    tabs.forEachIndexed { index, label ->
      val isSelected = selectedTab == index
      val pillColor by
        animateColorAsState(
          targetValue =
            if (isSelected) Color(0xFF8E55FF).copy(alpha = 0.88f)
            else Color.Transparent,
          animationSpec = config.colorSpring(),
        )
      val textColor =
        if (isSelected) Color.White
        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f)
      Box(
        contentAlignment = Alignment.Center,
        modifier =
          Modifier.weight(1f)
            .defaultMinSize(minHeight = 44.dp)
            .liquidButtonSquish()
            .clip(LiquidGlassShapes.CapsuleShape)
            .background(pillColor, LiquidGlassShapes.CapsuleShape)
            .clickable(role = Role.Tab) { onSelectTab(index) }
            .padding(vertical = 10.dp)
            .testTag("customizer_tab_$index"),
      ) {
        Text(
          text = label,
          style = MaterialTheme.typography.titleSmall,
          fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
          color = textColor,
        )
      }
    }
  }
}

@Composable
private fun GlassCustomizerTab(
  config: LiquidGlassConfig,
  onConfigChange: (LiquidGlassConfig) -> Unit,
  onResetGlass: () -> Unit,
) {
  val palette = LiquidGlassDefaults.palette()
  val textColor = if (palette.isDark) Color.White else MaterialTheme.colorScheme.onSurface
  val subTextColor =
    if (palette.isDark) Color.White.copy(alpha = 0.72f)
    else MaterialTheme.colorScheme.onSurfaceVariant

  Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
    LiveGlassPreviewCard(config = config)

    // Profile chips card (Adaptive | Clear | Frosted | Deep) + Glass tint color selector
    LiquidGlassCard(shape = LiquidGlassShapes.CardShape, modifier = Modifier.fillMaxWidth()) {
      Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
      ) {
        Row(
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          modifier =
            Modifier.fillMaxWidth()
              .horizontalScroll(rememberScrollState())
              .padding(horizontal = 16.dp),
        ) {
          ProfileChip(
            label = stringResource(R.string.liquid_glass_profile_adaptive),
            selected = config.glassProfile == GlassProfile.ADAPTIVE,
            onClick = { onConfigChange(config.withGlassProfile(GlassProfile.ADAPTIVE)) },
            modifier = Modifier.testTag("glass_profile_adaptive"),
          )
          ProfileChip(
            label = stringResource(R.string.liquid_glass_profile_clear),
            selected = config.glassProfile == GlassProfile.CLEAR,
            onClick = { onConfigChange(config.withGlassProfile(GlassProfile.CLEAR)) },
            modifier = Modifier.testTag("glass_profile_clear"),
          )
          ProfileChip(
            label = stringResource(R.string.liquid_glass_profile_frosted),
            selected = config.glassProfile == GlassProfile.FROSTED,
            onClick = { onConfigChange(config.withGlassProfile(GlassProfile.FROSTED)) },
            modifier = Modifier.testTag("glass_profile_frosted"),
          )
          ProfileChip(
            label = stringResource(R.string.liquid_glass_profile_deep),
            selected = config.glassProfile == GlassProfile.DEEP,
            onClick = { onConfigChange(config.withGlassProfile(GlassProfile.DEEP)) },
            modifier = Modifier.testTag("glass_profile_deep"),
          )
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = stringResource(R.string.liquid_glass_tint_color),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = textColor,
            modifier = Modifier.padding(horizontal = 16.dp),
          )
          Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier =
              Modifier.fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
          ) {
            GlassTintPreset.entries.forEach { preset ->
              GlassTintSwatchChip(
                preset = preset,
                selected = config.glassTint == preset,
                onClick = { onConfigChange(config.copy(glassTint = preset)) },
              )
            }
          }
        }
      }
    }

    // Sliders + Toggles + Restore default glass card
    LiquidGlassCard(shape = LiquidGlassShapes.CardShape, modifier = Modifier.fillMaxWidth()) {
      Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 18.dp),
      ) {
        CustomizerSliderRow(
          label = stringResource(R.string.liquid_glass_blur),
          value = config.blurScale,
          valueRange = 0f..2f,
          onValueChange = {
            onConfigChange(config.copy(glassProfile = GlassProfile.CUSTOM, blurScale = it))
          },
        )
        CustomizerSliderRow(
          label = stringResource(R.string.liquid_glass_transparency),
          value = config.transparency,
          valueRange = 0.15f..0.95f,
          onValueChange = {
            onConfigChange(config.copy(glassProfile = GlassProfile.CUSTOM, transparency = it))
          },
        )
        CustomizerSliderRow(
          label = stringResource(R.string.liquid_glass_tint),
          value = config.tintStrength,
          valueRange = 0f..1f,
          onValueChange = {
            onConfigChange(config.copy(glassProfile = GlassProfile.CUSTOM, tintStrength = it))
          },
        )
        CustomizerSliderRow(
          label = stringResource(R.string.liquid_glass_highlight_intensity),
          value = config.highlightIntensity,
          valueRange = 0.25f..1.50f,
          onValueChange = {
            onConfigChange(config.copy(glassProfile = GlassProfile.CUSTOM, highlightIntensity = it))
          },
        )
        CustomizerSliderRow(
          label = stringResource(R.string.liquid_glass_refraction_height),
          value = config.refractionHeightScale,
          valueRange = 0.25f..2f,
          onValueChange = {
            onConfigChange(
              config.copy(glassProfile = GlassProfile.CUSTOM, refractionHeightScale = it)
            )
          },
        )
        CustomizerSliderRow(
          label = stringResource(R.string.liquid_glass_refraction_amount),
          value = config.refractionAmountScale,
          valueRange = 0.25f..2f,
          onValueChange = {
            onConfigChange(
              config.copy(glassProfile = GlassProfile.CUSTOM, refractionAmountScale = it)
            )
          },
        )
        CustomizerSliderRow(
          label = stringResource(R.string.liquid_glass_vibrancy),
          value = config.vibrancyScale,
          valueRange = 0.5f..2.5f,
          onValueChange = {
            onConfigChange(config.copy(glassProfile = GlassProfile.CUSTOM, vibrancyScale = it))
          },
        )
        CustomizerSliderRow(
          label = stringResource(R.string.liquid_glass_brightness),
          value = config.brightness,
          valueRange = -0.25f..0.25f,
          onValueChange = {
            onConfigChange(config.copy(glassProfile = GlassProfile.CUSTOM, brightness = it))
          },
        )

        Spacer(Modifier.height(2.dp))
        CustomizerSwitchRow(
          title = stringResource(R.string.liquid_glass_chromatic_aberration),
          subtitle = null,
          checked = config.chromaticAberration,
          titleColor = textColor,
          subtitleColor = subTextColor,
          onCheckedChange = { enabled ->
            onConfigChange(
              config.copy(glassProfile = GlassProfile.CUSTOM, chromaticAberration = enabled)
            )
          },
          modifier = Modifier.testTag("chromatic_aberration_toggle"),
        )

        CustomizerSwitchRow(
          title = stringResource(R.string.liquid_glass_reduced_effects),
          subtitle = stringResource(R.string.liquid_glass_reduced_effects_summary),
          checked = config.reducedVisualEffects,
          titleColor = textColor,
          subtitleColor = subTextColor,
          onCheckedChange = { enabled ->
            onConfigChange(config.copy(reducedVisualEffects = enabled))
          },
          modifier = Modifier.testTag("reduced_visual_effects_toggle"),
        )

        Spacer(Modifier.height(4.dp))
        CustomizerActionButton(
          text = stringResource(R.string.liquid_glass_reset_glass),
          icon = Icons.Default.Refresh,
          onClick = onResetGlass,
          modifier = Modifier.fillMaxWidth().testTag("reset_glass_button"),
        )
      }
    }
  }
}

@Composable
private fun MotionCustomizerTab(
  config: LiquidGlassConfig,
  onConfigChange: (LiquidGlassConfig) -> Unit,
  onTryIt: () -> Unit,
  onResetMotion: () -> Unit,
) {
  val palette = LiquidGlassDefaults.palette()
  val demoCardSquish = rememberLiquidSquishState()
  val tryItSquish = rememberLiquidSquishState()
  Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
    LiquidGlassCard(
      shape = LiquidGlassShapes.CardShape,
      modifier =
        Modifier.fillMaxWidth()
          .liquidSquish(
            pressScale = 0.94f,
            stretchAmount = 0.045f,
            maxDragTranslationPx = 10f,
            glowAlpha = 0.20f,
            bloomScaleBoost = 0.045f,
            externalState = demoCardSquish,
          ),
    ) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 18.dp),
      ) {
        Text(
          text = stringResource(R.string.liquid_glass_motion_intro),
          style = MaterialTheme.typography.bodyLarge,
          fontWeight = FontWeight.Medium,
          color =
            if (palette.isDark) Color.White.copy(alpha = 0.88f)
            else MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.fillMaxWidth(),
        )
        Row(
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        ) {
          ProfileChip(
            label = stringResource(R.string.liquid_glass_motion_normal),
            selected = config.motionProfile == MotionProfile.NORMAL,
            onClick = {
              onConfigChange(config.withMotionProfile(MotionProfile.NORMAL))
            },
            modifier = Modifier.testTag("motion_profile_normal"),
          )
          ProfileChip(
            label = stringResource(R.string.liquid_glass_motion_performance),
            selected = config.motionProfile == MotionProfile.PERFORMANCE,
            onClick = {
              onConfigChange(config.withMotionProfile(MotionProfile.PERFORMANCE))
            },
            modifier = Modifier.testTag("motion_profile_performance"),
          )
          ProfileChip(
            label = stringResource(R.string.liquid_glass_motion_bouncy),
            selected = config.motionProfile == MotionProfile.BOUNCY,
            onClick = {
              onConfigChange(config.withMotionProfile(MotionProfile.BOUNCY))
            },
            modifier = Modifier.testTag("motion_profile_bouncy"),
          )
        }
        LiquidGlassButton(
          onClick = {
            demoCardSquish.triggerDemoPulse(config)
            tryItSquish.triggerDemoPulse(config)
            onTryIt()
          },
          externalSquishState = tryItSquish,
          containerColor = Color(0xFFB366FF),
          contentColor = Color.White,
          modifier =
            Modifier.defaultMinSize(minWidth = 148.dp, minHeight = 48.dp)
              .testTag("liquid_motion_try_it"),
        ) {
          Text(
            text = stringResource(R.string.liquid_glass_try_it),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
          )
        }
      }
    }

    LiquidGlassCard(shape = LiquidGlassShapes.CardShape, modifier = Modifier.fillMaxWidth()) {
      val textColor = if (palette.isDark) Color.White else MaterialTheme.colorScheme.onSurface
      val subTextColor =
        if (palette.isDark) Color.White.copy(alpha = 0.72f)
        else MaterialTheme.colorScheme.onSurfaceVariant
      Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 18.dp),
      ) {
        CustomizerSwitchRow(
          title = stringResource(R.string.liquid_glass_motion_enabled),
          subtitle = stringResource(R.string.liquid_glass_motion_enabled_summary),
          checked = config.animationsEnabled,
          titleColor = textColor,
          subtitleColor = subTextColor,
          onCheckedChange = { enabled ->
            onConfigChange(config.copy(animationsEnabled = enabled))
          },
          modifier = Modifier.testTag("animations_enabled_toggle"),
        )
        CustomizerSliderRow(
          label = stringResource(R.string.liquid_glass_motion_intensity),
          value = config.animationIntensity,
          valueRange = 0.25f..1.50f,
          onValueChange = {
            onConfigChange(config.copy(motionProfile = MotionProfile.CUSTOM, animationIntensity = it))
          },
        )
        CustomizerSliderRow(
          label = stringResource(R.string.liquid_glass_motion_bounce),
          value = config.bounce,
          valueRange = 0f..1.2f,
          onValueChange = {
            onConfigChange(config.copy(motionProfile = MotionProfile.CUSTOM, bounce = it))
          },
        )
        CustomizerSliderRow(
          label = stringResource(R.string.liquid_glass_motion_speed),
          value = config.speed,
          valueRange = 0.4f..2f,
          onValueChange = {
            onConfigChange(config.copy(motionProfile = MotionProfile.CUSTOM, speed = it))
          },
        )
        CustomizerSliderRow(
          label = stringResource(R.string.liquid_glass_motion_press_depth),
          value = config.pressDepth,
          valueRange = 0.4f..2.5f,
          onValueChange = {
            onConfigChange(config.copy(motionProfile = MotionProfile.CUSTOM, pressDepth = it))
          },
        )
        CustomizerSliderRow(
          label = stringResource(R.string.liquid_glass_motion_release_jiggle),
          value = config.releaseJiggle,
          valueRange = 0f..3f,
          onValueChange = {
            onConfigChange(config.copy(motionProfile = MotionProfile.CUSTOM, releaseJiggle = it))
          },
        )
        CustomizerSliderRow(
          label = stringResource(R.string.liquid_glass_motion_drag_stretch),
          value = config.dragStretch,
          valueRange = 0f..2.5f,
          onValueChange = {
            onConfigChange(config.copy(motionProfile = MotionProfile.CUSTOM, dragStretch = it))
          },
        )
        CustomizerSliderRow(
          label = stringResource(R.string.liquid_glass_motion_long_press_bloom),
          value = config.longPressBloom,
          valueRange = 0.4f..2.5f,
          onValueChange = {
            onConfigChange(config.copy(motionProfile = MotionProfile.CUSTOM, longPressBloom = it))
          },
        )

        Spacer(Modifier.height(4.dp))
        CustomizerActionButton(
          text = stringResource(R.string.liquid_glass_reset_motion),
          icon = Icons.Default.Refresh,
          onClick = onResetMotion,
          modifier = Modifier.fillMaxWidth().testTag("reset_motion_button"),
        )
      }
    }
  }
}

@Composable
private fun LiveGlassPreviewCard(config: LiquidGlassConfig) {
  val palette = LiquidGlassDefaults.palette()
  val scope = rememberCoroutineScope()
  val haptic = LocalHapticFeedback.current
  val pillOffset = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
  val gradientBrush =
    remember {
      Brush.linearGradient(
        colors =
          listOf(
            Color(0xFF0A84FF),
            Color(0xFF9C27B0),
            Color(0xFFFF2D55),
            Color(0xFFFF8A00),
            Color(0xFF34C759),
          )
      )
    }
  val previewBackdrop = rememberLayerBackdrop {
    drawRect(gradientBrush)
    drawContent()
  }

  BoxWithConstraints(
    contentAlignment = Alignment.Center,
    modifier =
      Modifier.fillMaxWidth()
        .height(172.dp)
        .clip(LiquidGlassShapes.CardShape)
        .border(
          width = 0.85.dp,
          color = Color.White.copy(alpha = if (palette.isDark) 0.22f else 0.55f),
          shape = LiquidGlassShapes.CardShape,
        ),
  ) {
    val maxX = with(LocalDensity.current) { (maxWidth / 2 - 84.dp).toPx().coerceAtLeast(10f) }
    val maxY = with(LocalDensity.current) { (maxHeight / 2 - 34.dp).toPx().coerceAtLeast(10f) }

    // Colorful text canvas recorded into previewBackdrop for live real-time glass refraction
    Column(
      verticalArrangement = Arrangement.SpaceEvenly,
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier =
        Modifier.fillMaxSize()
          .layerBackdrop(previewBackdrop)
          .background(gradientBrush)
          .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
      repeat(4) {
        Text(
          text = stringResource(R.string.liquid_glass_preview_sample),
          color = Color.White,
          fontSize = 15.sp,
          fontWeight = FontWeight.Bold,
          maxLines = 1,
          overflow = TextOverflow.Clip,
        )
      }
    }

    // Draggable + squishable live Liquid Glass capsule pill refracting the gradient & text underneath
    val pillTintColor =
      when (config.glassTint) {
        GlassTintPreset.ADAPTIVE -> MaterialTheme.colorScheme.primary
        GlassTintPreset.NEUTRAL -> Color.White
        else -> config.glassTint.swatchColor
      }
    val trFactor = (1.36f - config.transparency * 0.82f).coerceIn(0.24f, 1.42f)
    val tintAlpha = (config.tintStrength * 0.42f * trFactor).coerceIn(0.03f, 0.56f)
    val resolvedSurface =
      if (config.glassTint == GlassTintPreset.NEUTRAL) Color.White.copy(alpha = tintAlpha)
      else pillTintColor.copy(alpha = 0.25f).compositeOver(Color.White).copy(alpha = tintAlpha)

    Box(
      contentAlignment = Alignment.Center,
      modifier =
        Modifier.offset {
            IntOffset(pillOffset.value.x.roundToInt(), pillOffset.value.y.roundToInt())
          }
          .liquidButtonSquish()
          .pointerInput(maxX, maxY, config) {
            detectDragGestures(
              onDragStart = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) },
              onDragEnd = {
                scope.launch { pillOffset.animateTo(Offset.Zero, config.offsetSpring()) }
              },
              onDragCancel = {
                scope.launch { pillOffset.animateTo(Offset.Zero, config.offsetSpring()) }
              },
              onDrag = { change, dragAmount ->
                change.consume()
                val next =
                  Offset(
                    x = (pillOffset.value.x + dragAmount.x).coerceIn(-maxX, maxX),
                    y = (pillOffset.value.y + dragAmount.y).coerceIn(-maxY, maxY),
                  )
                scope.launch { pillOffset.snapTo(next) }
              },
            )
          }
          .then(
            if (config.reducedVisualEffects) {
              Modifier.fastLiquidSurface(
                shape = LiquidGlassShapes.CapsuleShape,
                surfaceTint = resolvedSurface,
                withShadow = true,
                respectUserConfig = false,
                isHighlighted = true,
              )
            } else {
              Modifier.drawBackdrop(
                backdrop = previewBackdrop,
                shape = { LiquidGlassShapes.CapsuleShape },
                effects = {
                  if (config.vibrancyScale >= 0.9f) {
                    vibrancy()
                  }
                  val sat = (0.65f + config.vibrancyScale * 0.45f).coerceIn(0.2f, 2.2f)
                  colorControls(
                    brightness = config.brightness.coerceIn(-0.35f, 0.35f),
                    saturation = sat,
                  )
                  val effectiveBlur = 8.dp * config.blurScale
                  if (effectiveBlur > 0.2.dp) {
                    blur(effectiveBlur.toPx())
                  }
                  lens(
                    refractionHeight = (16.dp * config.refractionHeightScale).toPx(),
                    refractionAmount = (36.dp * config.refractionAmountScale).toPx(),
                    depthEffect = true,
                    chromaticAberration = config.chromaticAberration,
                  )
                },
                highlight = {
                  LiquidGlassDefaults.highlight(isDark = true, intensity = config.highlightIntensity)
                },
                shadow = { LiquidGlassDefaults.floatingShadow(isDark = true, 18.dp, 6.dp) },
                innerShadow = { LiquidGlassDefaults.pillInnerShadow(isDark = true) },
                onDrawSurface = { drawRect(resolvedSurface) },
              )
            }
          )
          .padding(horizontal = 26.dp, vertical = 14.dp)
          .testTag("liquid_glass_preview_pill"),
    ) {
      Text(
        text = stringResource(R.string.liquid_glass_title),
        color = Color.White,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
      )
    }
  }
}

@Composable
private fun GlassTintSwatchChip(
  preset: GlassTintPreset,
  selected: Boolean,
  onClick: () -> Unit,
) {
  val palette = LiquidGlassDefaults.palette()
  val config = LocalLiquidGlassConfig.current
  val swatchColor =
    if (preset == GlassTintPreset.ADAPTIVE) MaterialTheme.colorScheme.primary
    else preset.swatchColor
  val labelRes =
    when (preset) {
      GlassTintPreset.ADAPTIVE -> R.string.liquid_glass_tint_adaptive
      GlassTintPreset.OCEAN -> R.string.liquid_glass_tint_ocean
      GlassTintPreset.VIOLET -> R.string.liquid_glass_tint_violet
      GlassTintPreset.EMERALD -> R.string.liquid_glass_tint_emerald
      GlassTintPreset.AMBER -> R.string.liquid_glass_tint_amber
      GlassTintPreset.ROSE -> R.string.liquid_glass_tint_rose
      GlassTintPreset.NEUTRAL -> R.string.liquid_glass_tint_neutral
    }
  val bg by
    animateColorAsState(
      targetValue = if (selected) swatchColor.copy(alpha = 0.24f) else palette.chipSurfaceTint,
      animationSpec = config.colorSpring(),
    )
  val border by
    animateColorAsState(
      targetValue =
        if (selected) swatchColor
        else Color.White.copy(alpha = if (palette.isDark) 0.24f else 0.62f),
      animationSpec = config.colorSpring(),
    )

  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    modifier =
      Modifier.defaultMinSize(minHeight = 40.dp)
        .liquidSquish(pressScale = 0.90f, stretchAmount = 0.06f, maxDragTranslationPx = 6f)
        .clip(LiquidGlassShapes.CapsuleShape)
        .background(bg, LiquidGlassShapes.CapsuleShape)
        .border(if (selected) 1.5.dp else 0.9.dp, border, LiquidGlassShapes.CapsuleShape)
        .clickable(role = Role.RadioButton, onClick = onClick)
        .padding(horizontal = 12.dp, vertical = 8.dp)
        .testTag("glass_tint_${preset.name.lowercase(Locale.US)}"),
  ) {
    Box(
      modifier =
        Modifier.size(16.dp)
          .clip(LiquidGlassShapes.CapsuleShape)
          .background(swatchColor, LiquidGlassShapes.CapsuleShape)
          .border(1.dp, Color.White.copy(alpha = 0.7f), LiquidGlassShapes.CapsuleShape)
    )
    Text(
      text = stringResource(labelRes),
      style = MaterialTheme.typography.labelLarge,
      fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
      color = if (palette.isDark || selected) Color.White else MaterialTheme.colorScheme.onSurface,
    )
  }
}

@Composable
private fun CustomizerSwitchRow(
  title: String,
  subtitle: String?,
  checked: Boolean,
  titleColor: Color,
  subtitleColor: Color,
  onCheckedChange: (Boolean) -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
    modifier =
      modifier
        .fillMaxWidth()
        .liquidRowSquish()
        .clip(LiquidGlassShapes.SmallCardShape)
        .clickable(role = Role.Switch) { onCheckedChange(!checked) }
        .padding(vertical = 4.dp),
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.weight(1f).padding(end = 12.dp)) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = titleColor,
      )
      if (subtitle != null) {
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = subtitleColor,
        )
      }
    }
    LiquidGlassSwitch(
      checked = checked,
      onCheckedChange = onCheckedChange,
    )
  }
}

@Composable
private fun ProfileChip(
  label: String,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val config = LocalLiquidGlassConfig.current
  val palette = LiquidGlassDefaults.palette()
  val bg by
    animateColorAsState(
      targetValue =
        if (selected) Color(0xFF8E55FF).copy(alpha = 0.28f)
        else palette.chipSurfaceTint,
      animationSpec = config.colorSpring(),
    )
  val border by
    animateColorAsState(
      targetValue =
        if (selected) Color(0xFFB388FF)
        else Color.White.copy(alpha = if (palette.isDark) 0.26f else 0.65f),
      animationSpec = config.colorSpring(),
    )

  Box(
    contentAlignment = Alignment.Center,
    modifier =
      modifier
        .defaultMinSize(minHeight = 42.dp)
        .liquidSquish(pressScale = 0.90f, stretchAmount = 0.06f, maxDragTranslationPx = 7f)
        .clip(LiquidGlassShapes.CapsuleShape)
        .background(bg, LiquidGlassShapes.CapsuleShape)
        .border(
          width = if (selected) 1.5.dp else 0.9.dp,
          color = border,
          shape = LiquidGlassShapes.CapsuleShape,
        )
        .clickable(role = Role.RadioButton, onClick = onClick)
        .padding(horizontal = 18.dp, vertical = 9.dp),
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.titleSmall,
      fontWeight = FontWeight.Bold,
      color = if (palette.isDark || selected) Color.White else MaterialTheme.colorScheme.onSurface,
    )
  }
}

@Composable
private fun CustomizerSliderRow(
  label: String,
  value: Float,
  valueRange: ClosedFloatingPointRange<Float>,
  onValueChange: (Float) -> Unit,
) {
  val palette = LiquidGlassDefaults.palette()
  val formatted = String.format(Locale.US, "%.2f", value)
  Column(modifier = Modifier.fillMaxWidth()) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
      modifier = Modifier.fillMaxWidth(),
    ) {
      Text(
        text = label,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = if (palette.isDark) Color.White else MaterialTheme.colorScheme.onSurface,
      )
      Text(
        text = formatted,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color =
          if (palette.isDark) Color.White.copy(alpha = 0.78f)
          else MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    LiquidGlassSlider(
      value = value,
      onValueChange = onValueChange,
      valueRange = valueRange,
      activeTrackColor = Color(0xFF0A84FF),
    )
  }
}

@Composable
private fun CustomizerActionButton(
  text: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  LiquidGlassButton(
    onClick = onClick,
    containerColor = Color(0xFFA955FF),
    contentColor = Color.White,
    modifier = modifier.defaultMinSize(minHeight = 48.dp),
  ) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      tint = Color.White,
      modifier = Modifier.size(18.dp),
    )
    Spacer(Modifier.width(8.dp))
    Text(
      text = text,
      style = MaterialTheme.typography.titleSmall,
      fontWeight = FontWeight.Bold,
      color = Color.White,
    )
  }
}

@Preview
@Composable
private fun GlassCustomizerPreview() {
  FDroidContent(darkTheme = true) {
    LiquidGlassCustomizer(
      config = LiquidGlassConfig(),
      onConfigChange = {},
      onResetGlass = {},
      onResetMotion = {},
      onBackClicked = {},
    )
  }
}

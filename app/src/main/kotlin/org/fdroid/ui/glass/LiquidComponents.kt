package org.fdroid.ui.glass

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.tanh
import kotlinx.coroutines.launch

/**
 * Edge-to-edge Liquid Glass Scaffold where scrolling body content is recorded into a live
 * [com.kyant.backdrop.backdrops.LayerBackdrop], allowing the floating [topBar] and
 * [floatingActionButton] to blur, saturate, and refract content scrolling directly underneath.
 */
@Composable
fun LiquidScaffold(
  modifier: Modifier = Modifier,
  topBar: @Composable () -> Unit = {},
  floatingActionButton: @Composable () -> Unit = {},
  content: @Composable (PaddingValues) -> Unit,
) {
  val backgroundColor = MaterialTheme.colorScheme.background
  val screenBackdrop = rememberLayerBackdrop {
    drawRect(backgroundColor)
    drawContent()
  }
  val density = LocalDensity.current
  val layoutDirection = LocalLayoutDirection.current
  val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
  val navBarInsets = WindowInsets.navigationBars.asPaddingValues()
  val extraBottomInset = LocalBottomFloatingBarInset.current
  var measuredTopBarHeightPx by remember { mutableIntStateOf(0) }

  val topPadding =
    if (measuredTopBarHeightPx > 0) {
      with(density) { measuredTopBarHeightPx.toDp() }
    } else {
      statusBarTop
    }

  val contentPadding =
    PaddingValues(
      start = navBarInsets.calculateStartPadding(layoutDirection),
      end = navBarInsets.calculateEndPadding(layoutDirection),
      top = topPadding,
      bottom = navBarInsets.calculateBottomPadding() + extraBottomInset,
    )

  val scrollState = remember { LiquidScrollState() }
  Box(modifier = modifier.fillMaxSize()) {
    CompositionLocalProvider(
      LocalLiquidScrollState provides scrollState,
    ) {
      Box(modifier = Modifier.fillMaxSize().layerBackdrop(screenBackdrop)) {
        content(contentPadding)
      }

      CompositionLocalProvider(LocalLiquidBackdrop provides screenBackdrop) {
        Box(
          modifier =
            Modifier.fillMaxWidth()
              .align(Alignment.TopCenter)
              .onSizeChanged { measuredTopBarHeightPx = it.height }
        ) {
          topBar()
        }

        Box(
          modifier =
            Modifier.align(Alignment.BottomEnd)
              .padding(
                end = navBarInsets.calculateEndPadding(layoutDirection) + 16.dp,
                bottom = navBarInsets.calculateBottomPadding() + extraBottomInset + 12.dp,
              )
        ) {
          floatingActionButton()
        }
      }
    }
  }
}

/**
 * Apple iOS-style floating Liquid Glass top navigation pane composed of detached glass pills:
 * - Floating circular back button on the left (when [navigationIcon] is provided)
 * - Floating capsule title/subtitle pill in the leading/center area (when [showTitlePill] is true)
 * - Floating capsule actions cluster on the right (when [showActions] is true)
 */
@Composable
fun LiquidFloatingTopBar(
  modifier: Modifier = Modifier,
  navigationIcon: (@Composable () -> Unit)? = null,
  showTitlePill: Boolean = true,
  title: (@Composable () -> Unit)? = null,
  subtitle: (@Composable () -> Unit)? = null,
  showActions: Boolean = true,
  actions: @Composable (RowScope.() -> Unit) = {},
) {
  val palette = LiquidGlassDefaults.palette()
  val screenBackdrop = LocalLiquidBackdrop.current ?: LocalAtmosphericBackdrop.current
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(10.dp),
    modifier =
      modifier
        .fillMaxWidth()
        .statusBarsPadding()
        .padding(horizontal = 12.dp, vertical = 8.dp),
  ) {
    if (navigationIcon != null) {
      Box(
        contentAlignment = Alignment.Center,
        modifier =
          Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .liquidButtonSquish()
            .liquidGlass(
              backdrop = screenBackdrop,
              shape = LiquidGlassShapes.CapsuleShape,
              surfaceTint = palette.barSurfaceTint,
              blurRadius = 7.dp,
              refractionHeight = 16.dp,
              refractionAmount = 32.dp,
              chromaticAberration = false,
              scrollResponsive = true,
              animateParameters = true,
            )
            .testTag("top_bar_nav_pill"),
      ) {
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
          navigationIcon()
        }
      }
    }

    Box(
      contentAlignment = Alignment.CenterStart,
      modifier = Modifier.weight(1f),
    ) {
      FloatingTitlePill(
        visible = showTitlePill && title != null,
        screenBackdrop = screenBackdrop,
        palette = palette,
        title = title,
        subtitle = subtitle,
      )
    }

    if (showActions) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        modifier =
          Modifier.heightIn(min = 48.dp)
            .liquidGlass(
              backdrop = screenBackdrop,
              shape = LiquidGlassShapes.BarPillShape,
              surfaceTint = palette.barSurfaceTint,
              blurRadius = 7.dp,
              refractionHeight = 16.dp,
              refractionAmount = 32.dp,
              chromaticAberration = false,
              scrollResponsive = true,
              animateParameters = true,
            )
            .padding(horizontal = 4.dp)
            .testTag("top_bar_actions_pill"),
      ) {
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
          actions()
        }
      }
    }
  }
}

@Composable
private fun FloatingTitlePill(
  visible: Boolean,
  screenBackdrop: com.kyant.backdrop.Backdrop?,
  palette: LiquidGlassPalette,
  title: (@Composable () -> Unit)?,
  subtitle: (@Composable () -> Unit)?,
) {
  val config = LocalLiquidGlassConfig.current
  AnimatedVisibility(
    visible = visible,
    enter =
      fadeIn(config.smoothSpatialSpring()) +
        scaleIn(initialScale = 0.9f, animationSpec = config.bouncySpatialSpring()),
    exit =
      fadeOut(config.smoothSpatialSpring()) +
        scaleOut(targetScale = 0.9f, animationSpec = config.smoothSpatialSpring()),
  ) {
    Column(
      verticalArrangement = Arrangement.Center,
      modifier =
        Modifier.heightIn(min = 48.dp)
          .liquidRowSquish()
          .liquidGlass(
            backdrop = screenBackdrop,
            shape = LiquidGlassShapes.BarPillShape,
            surfaceTint = palette.barSurfaceTint,
            blurRadius = 7.dp,
            refractionHeight = 16.dp,
            refractionAmount = 34.dp,
            chromaticAberration = false,
            scrollResponsive = true,
            animateParameters = true,
          )
          .padding(horizontal = 18.dp, vertical = if (subtitle != null) 6.dp else 10.dp)
          .testTag("top_bar_title_pill"),
    ) {
      CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
        ProvideTextStyle(MaterialTheme.typography.titleLarge) { title?.invoke() }
        if (subtitle != null) {
          ProvideTextStyle(
            MaterialTheme.typography.labelMedium.copy(
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          ) {
            subtitle()
          }
        }
      }
    }
  }
}

/**
 * Floating Liquid Glass search bar pane modeled after Apple iOS 26 floating search pills.
 */
@Composable
fun LiquidFloatingSearchBar(
  modifier: Modifier = Modifier,
  navigationIcon: @Composable () -> Unit,
  searchField: @Composable () -> Unit,
  showActions: Boolean = false,
  actions: @Composable (RowScope.() -> Unit) = {},
) {
  val palette = LiquidGlassDefaults.palette()
  val screenBackdrop = LocalLiquidBackdrop.current ?: LocalAtmosphericBackdrop.current
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    modifier =
      modifier
        .fillMaxWidth()
        .statusBarsPadding()
        .padding(horizontal = 12.dp, vertical = 8.dp),
  ) {
    Box(
      contentAlignment = Alignment.Center,
      modifier =
        Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
          .liquidButtonSquish()
          .liquidGlass(
            backdrop = screenBackdrop,
            shape = LiquidGlassShapes.CapsuleShape,
            surfaceTint = palette.barSurfaceTint,
            blurRadius = 7.dp,
            refractionHeight = 16.dp,
            refractionAmount = 32.dp,
            scrollResponsive = true,
            animateParameters = true,
          ),
    ) {
      CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
        navigationIcon()
      }
    }

    Box(
      contentAlignment = Alignment.CenterStart,
      modifier =
        Modifier.weight(1f)
          .heightIn(min = 52.dp)
          .liquidRowSquish()
          .liquidGlass(
            backdrop = screenBackdrop,
            shape = LiquidGlassShapes.BarPillShape,
            surfaceTint = palette.pillSurfaceTint,
            blurRadius = 8.dp,
            refractionHeight = 18.dp,
            refractionAmount = 36.dp,
            scrollResponsive = true,
            animateParameters = true,
          )
          .padding(horizontal = 4.dp)
          .testTag("top_search_glass_pill"),
    ) {
      CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
        searchField()
      }
    }

    val config = LocalLiquidGlassConfig.current
    AnimatedVisibility(
      visible = showActions,
      enter =
        fadeIn(config.smoothSpatialSpring()) +
          scaleIn(initialScale = 0.85f, animationSpec = config.bouncySpatialSpring()),
      exit =
        fadeOut(config.smoothSpatialSpring()) +
          scaleOut(targetScale = 0.85f, animationSpec = config.smoothSpatialSpring()),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
          Modifier.heightIn(min = 48.dp)
            .liquidGlass(
              backdrop = screenBackdrop,
              shape = LiquidGlassShapes.BarPillShape,
              surfaceTint = palette.barSurfaceTint,
              blurRadius = 7.dp,
              refractionHeight = 16.dp,
              refractionAmount = 32.dp,
              scrollResponsive = true,
              animateParameters = true,
            )
            .padding(horizontal = 4.dp),
      ) {
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
          actions()
        }
      }
    }
  }
}

/**
 * Kyant0 official `LiquidBottomTabs` pattern adapted for F-Droid Basic:
 * - Unclipped outer dock container so the active jelly lens pops out above & below the bar on hold
 * - Dual [rememberCombinedBackdrop] (`dockBackdrop` + `barSurfaceBackdrop`) capturing the live page
 *   and dock glass shell (excluding tab icons/labels) so inside the moving jelly droplet the
 *   navigation bar and background blur & refract while icons and text remain razor-sharp on top
 * - Immediate hold-and-swipe damped velocity jelly deformation (`78f / 56f` lift + volume stretch)
 */
@Composable
fun <T> LiquidFloatingBottomDock(
  items: List<T>,
  selectedIndex: Int,
  onSelect: (Int) -> Unit,
  modifier: Modifier = Modifier,
  itemSemantics: (Int, T) -> Modifier = { _, _ -> Modifier },
  itemContent:
    @Composable
    BoxScope.(index: Int, item: T, isSelected: Boolean, selectionFraction: Float) -> Unit,
) {
  if (items.isEmpty()) return
  val palette = LiquidGlassDefaults.palette()
  val config = LocalLiquidGlassConfig.current
  val reducedMotion = LocalReducedMotion.current
  val dockBackdrop = LocalLiquidBackdrop.current ?: LocalAtmosphericBackdrop.current
  val barSurfaceBackdrop = rememberLayerBackdrop()
  val indicatorBackdrop =
    if (dockBackdrop != null) {
      rememberCombinedBackdrop(dockBackdrop, barSurfaceBackdrop)
    } else {
      barSurfaceBackdrop
    }

  val scope = rememberCoroutineScope()
  val haptic = LocalHapticFeedback.current
  val density = LocalDensity.current
  val viewConfig = LocalViewConfiguration.current
  val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
  val safeSelectedIndex = selectedIndex.coerceIn(0, items.lastIndex)
  val currentSelectedIndex by rememberUpdatedState(safeSelectedIndex)
  val currentOnSelect by rememberUpdatedState(onSelect)

  BoxWithConstraints(
    modifier =
      modifier
        .navigationBarsPadding()
        .padding(horizontal = 20.dp, vertical = 10.dp)
        .widthIn(max = 460.dp)
        .fillMaxWidth()
        .height(66.dp)
        .testTag("liquid_floating_bottom_bar")
  ) {
    val dockPaddingPx = with(density) { 4.dp.toPx() }
    val dockWidthPx = with(density) { maxWidth.toPx() }
    val dockHeightPx = with(density) { maxHeight.toPx() }
    val trackWidthPx = (dockWidthPx - dockPaddingPx * 2f).coerceAtLeast(1f)
    val trackHeightPx = (dockHeightPx - dockPaddingPx * 2f).coerceAtLeast(1f)
    val tabWidthPx = trackWidthPx / items.size.coerceAtLeast(1)

    val dampedDragRatio = remember { Animatable(safeSelectedIndex.toFloat()) }
    val dragPressProgress = remember { Animatable(0f) }
    val jellyVelocity = remember { Animatable(0f) }
    val releaseWobble = remember { Animatable(0f) }
    var isInteracting by remember { mutableStateOf(false) }

    LaunchedEffect(safeSelectedIndex, reducedMotion) {
      if (!isInteracting) {
        if (reducedMotion) {
          dampedDragRatio.snapTo(safeSelectedIndex.toFloat())
        } else {
          dampedDragRatio.animateTo(safeSelectedIndex.toFloat(), config.tabIndicatorSpring())
        }
      }
    }

    val pillShape = LiquidGlassShapes.CapsuleShape
    val maxIdx = (items.size - 1).toFloat().coerceAtLeast(0f)
    val clampedRatio = dampedDragRatio.value.coerceIn(-0.22f, maxIdx + 0.22f)
    val p = if (reducedMotion) 0f else dragPressProgress.value.coerceIn(0f, 1f)
    val wobbleVal = if (reducedMotion) 0f else releaseWobble.value
    val visualRatio = if (isRtl) (maxIdx - clampedRatio) else clampedRatio
    val pillCenterX = dockPaddingPx + (visualRatio + 0.5f) * tabWidthPx

    // Layer 1: Dock Liquid Glass background shell ONLY (recorded into barSurfaceBackdrop; no icons/text)
    Box(
      modifier =
        Modifier.fillMaxSize()
          .then(if (config.reducedVisualEffects) Modifier else Modifier.layerBackdrop(barSurfaceBackdrop))
          .liquidGlass(
            backdrop = dockBackdrop,
            shape = LiquidGlassShapes.DockShape,
            surfaceTint = palette.dockSurfaceTint,
            blurRadius = 8.dp,
            refractionHeight = 20.dp,
            refractionAmount = 46.dp,
            depthEffect = true,
            chromaticAberration = true,
            shadowRadius = 24.dp,
            shadowOffsetY = 8.dp,
            scrollResponsive = true,
            animateParameters = true,
          )
    )

    // Layer 2: Selection Indicator + Unclipped Lifted Kyant0 Jelly Blur Droplet (UNDER tabs Row)
    // Pops larger than the bar (`82f / 56f`), blurs & refracts the navigation bar + page
    // backdrop inside the droplet while keeping the tab icons and labels crisp on top.
    val idleAlpha = (1f - p * 1.4f).coerceIn(0f, 1f)
    if (idleAlpha > 0.01f) {
      Box(
        modifier =
          Modifier.fillMaxSize()
            .padding(4.dp)
      ) {
        Box(
          modifier =
            Modifier.fillMaxHeight()
              .fillMaxWidth(1f / items.size.coerceAtLeast(1))
              .offset { IntOffset((visualRatio * tabWidthPx).roundToInt(), 0) }
              .graphicsLayer {
                alpha = idleAlpha
                val settleWobble = wobbleVal * (0.65f + 0.35f * config.effectiveReleaseJiggle().coerceIn(0.2f, 2.2f))
                scaleX = (1f + settleWobble * 0.22f).coerceIn(0.84f, 1.24f)
                scaleY = (1f - settleWobble * 0.14f).coerceIn(0.84f, 1.20f)
              }
              .clip(pillShape)
              .background(palette.selectedPillTint, pillShape)
              .border(
                width = 0.85.dp,
                brush =
                  Brush.verticalGradient(
                    listOf(
                      palette.fallbackRimTop.copy(alpha = palette.fallbackRimTop.alpha * 0.65f),
                      palette.fallbackRimBottom,
                    )
                  ),
                shape = pillShape,
              )
        )
      }
    }

    if (!config.reducedVisualEffects && !reducedMotion &&
      (p > 0.005f || abs(jellyVelocity.value) > 0.015f || abs(wobbleVal) > 0.015f)
    ) {
      val bloomMod = (0.88f + 0.14f * config.effectiveLongPressBloom().coerceIn(0.5f, 1.8f))
      val pressedScale = (82f / 56f) * bloomMod
      val baseScale = 1f + (pressedScale - 1f) * p

      // Morph width smoothly toward a plump circular/oval liquid droplet on hold & swipe
      val targetJellyWidthPx = min(tabWidthPx, trackHeightPx * 1.08f)
      val currentPillWidthPx = tabWidthPx + (targetJellyWidthPx - tabWidthPx) * (p * 0.85f)
      val pillOffsetX = (pillCenterX - currentPillWidthPx * 0.5f).roundToInt()

      val widthDp = with(density) { currentPillWidthPx.toDp() }
      val heightDp = with(density) { trackHeightPx.toDp() }

      Box(
        modifier =
          Modifier.offset { IntOffset(pillOffsetX, dockPaddingPx.roundToInt()) }
            .width(widthDp)
            .height(heightDp)
            .drawBackdrop(
              backdrop = indicatorBackdrop,
              shape = { pillShape },
              effects = {
                vibrancy()
                // Blurs & bends the navigation bar + page background inside the jelly droplet only
                val jellyBlur = (8.5.dp * config.blurScale.coerceAtLeast(0.78f)).toPx()
                if (jellyBlur > 0.5f) {
                  blur(jellyBlur)
                }
                lens(
                  refractionHeight = (25.dp * config.refractionHeightScale).toPx(),
                  refractionAmount = (50.dp * config.refractionAmountScale).toPx(),
                  depthEffect = true,
                  chromaticAberration = config.chromaticAberration,
                )
              },
              highlight = {
                LiquidGlassDefaults.highlight(
                  palette.isDark,
                  (1.40f * config.highlightIntensity).coerceIn(0.3f, 1.6f),
                )
              },
              shadow = { LiquidGlassDefaults.floatingShadow(palette.isDark, 24.dp, 9.dp) },
              innerShadow = { LiquidGlassDefaults.pillInnerShadow(palette.isDark) },
              layerBlock = {
                val stretchFactor = config.effectiveDragStretch().coerceIn(0.2f, 2.5f)
                val signedVel = (jellyVelocity.value * stretchFactor).coerceIn(-0.32f, 0.32f)
                val velMag = abs(signedVel)
                val settleWobble = wobbleVal * (0.65f + 0.35f * config.effectiveReleaseJiggle().coerceIn(0.2f, 2.2f))
                // Fluid volume-preserving jelly squash, horizontal velocity lean, and settling jiggle
                scaleX = ((baseScale / (1f - velMag * 0.85f)) + settleWobble * 0.28f).coerceIn(0.82f, 1.92f)
                scaleY = ((baseScale * (1f - velMag * 0.32f)) - settleWobble * 0.18f).coerceIn(0.78f, 1.66f)
                rotationZ = (signedVel * 11f).coerceIn(-4.5f, 4.5f)
                alpha = maxOf(p * 1.55f, abs(wobbleVal) * 2.1f).coerceIn(0f, 1f)
              },
              onDrawSurface = {
                drawRect(Color.White.copy(alpha = if (palette.isDark) 0.07f else 0.13f))
                // Glossy top specular sheen + subtle lower caustic reflection inside the liquid drop
                drawRect(
                  brush =
                    Brush.verticalGradient(
                      colors =
                        listOf(
                          Color.White.copy(alpha = if (palette.isDark) 0.22f else 0.42f),
                          Color.Transparent,
                          Color.White.copy(alpha = if (palette.isDark) 0.07f else 0.16f),
                        ),
                    )
                )
              },
            )
      )
    }

    // Layer 3 (Topmost): Crisp unblurred Tab Icons & Labels + Responsive Press/Hold/Swipe Controller
    Row(
      modifier =
        Modifier.fillMaxSize()
          .padding(4.dp)
          .pointerInput(items.size, tabWidthPx, isRtl) {
            if (tabWidthPx <= 0f) return@pointerInput
            val touchSlop = viewConfig.touchSlop
            awaitEachGesture {
              val down = awaitFirstDown(requireUnconsumed = false)
              isInteracting = true
              val downX = down.position.x
              val downDirectionalX = if (isRtl) (trackWidthPx - downX) else downX
              val downIndex = (downDirectionalX / tabWidthPx).toInt().coerceIn(0, items.lastIndex)
              val velocityTracker = VelocityTracker()
              velocityTracker.addPosition(down.uptimeMillis, down.position)

              var hasMovedBeyondSlop = false
              var maxHorizontalTravel = 0f
              var lastHapticBucket = currentSelectedIndex

              // Lift the liquid droplet right on press down & snap/spring smoothly to touched tab
              scope.launch {
                releaseWobble.snapTo(0f)
                launch { dragPressProgress.animateTo(1f, config.pressSpring()) }
                if (downIndex != currentSelectedIndex) {
                  lastHapticBucket = downIndex
                  haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                  dampedDragRatio.animateTo(downIndex.toFloat(), config.tabIndicatorSpring())
                }
              }

              var releasedNormally = true
              while (true) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull { it.id == down.id } ?: event.changes.firstOrNull()
                if (change == null) {
                  releasedNormally = false
                  break
                }
                if (!change.pressed) {
                  change.consume()
                  break
                }

                val horizDisp = abs(change.position.x - downX)
                if (horizDisp > maxHorizontalTravel) {
                  maxHorizontalTravel = horizDisp
                }
                if (maxHorizontalTravel > touchSlop * 1.25f) {
                  hasMovedBeyondSlop = true
                }

                velocityTracker.addPosition(change.uptimeMillis, change.position)
                if (hasMovedBeyondSlop) {
                  change.consume()
                  val curDirX =
                    if (isRtl) (trackWidthPx - change.position.x) else change.position.x
                  val rawTarget = (curDirX / tabWidthPx) - 0.5f
                  val dampedTarget =
                    when {
                      rawTarget < 0f -> -0.22f * tanh((-rawTarget * 2.2f).toDouble()).toFloat()
                      rawTarget > maxIdx ->
                        maxIdx + 0.22f * tanh(((rawTarget - maxIdx) * 2.2f).toDouble()).toFloat()
                      else -> rawTarget
                    }
                  val bucket = dampedTarget.roundToInt().coerceIn(0, items.lastIndex)
                  if (bucket != lastHapticBucket) {
                    lastHapticBucket = bucket
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                  }

                  val velPxPerSec =
                    velocityTracker.calculateVelocity().x.let { if (isRtl) -it else it }
                  val normVel = (velPxPerSec / 3200f).coerceIn(-1f, 1f)
                  scope.launch {
                    dampedDragRatio.snapTo(dampedTarget)
                    jellyVelocity.snapTo(normVel)
                  }
                }
              }

              isInteracting = false
              val targetIndex =
                when {
                  !releasedNormally -> currentSelectedIndex
                  !hasMovedBeyondSlop -> downIndex
                  else -> dampedDragRatio.value.roundToInt().coerceIn(0, items.lastIndex)
                }

              scope.launch {
                launch {
                  releaseWobble.snapTo(if (hasMovedBeyondSlop) 0.42f else 0.28f)
                  releaseWobble.animateTo(0f, config.jiggleSpring())
                }
                launch { dragPressProgress.animateTo(0f, config.releaseSpring()) }
                launch { jellyVelocity.animateTo(0f, config.jiggleSpring()) }
                launch {
                  dampedDragRatio.animateTo(targetIndex.toFloat(), config.tabIndicatorSpring())
                }
              }

              if (releasedNormally && targetIndex != currentSelectedIndex) {
                if (targetIndex != lastHapticBucket) {
                  haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
                currentOnSelect(targetIndex)
              } else if (releasedNormally && !hasMovedBeyondSlop && targetIndex == currentSelectedIndex) {
                // Keep double-tap on Search bringing up keyboard cleanly
                currentOnSelect(targetIndex)
              }
            }
          }
    ) {
      items.forEachIndexed { index, item ->
        val isSelected = index == safeSelectedIndex
        val selectionFraction = (1f - abs(dampedDragRatio.value - index)).coerceIn(0f, 1f)
        Box(
          contentAlignment = Alignment.Center,
          modifier =
            Modifier.weight(1f)
              .fillMaxHeight()
              .clip(pillShape)
              .semantics {
                role = Role.Tab
                selected = isSelected
                onClick {
                  haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                  currentOnSelect(index)
                  true
                }
              }
              .then(itemSemantics(index, item)),
        ) {
          itemContent(index, item, isSelected, selectionFraction)
        }
      }
    }
  }
}

/**
 * Floating vertical Liquid Glass navigation rail for large screens / tablets / foldables.
 */
@Composable
fun LiquidFloatingRailContainer(
  modifier: Modifier = Modifier,
  content: @Composable ColumnScope.() -> Unit,
) {
  val palette = LiquidGlassDefaults.palette()
  val railBackdrop = LocalLiquidBackdrop.current ?: LocalAtmosphericBackdrop.current
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(8.dp),
    modifier =
      modifier
        .safeDrawingPadding()
        .padding(start = 12.dp, top = 12.dp, bottom = 12.dp)
        .width(88.dp)
        .liquidGlass(
          backdrop = railBackdrop,
          shape = LiquidGlassShapes.RailShape,
          surfaceTint = palette.dockSurfaceTint,
          blurRadius = 8.dp,
          refractionHeight = 20.dp,
          refractionAmount = 40.dp,
          chromaticAberration = true,
          shadowRadius = 20.dp,
          shadowOffsetY = 6.dp,
          scrollResponsive = true,
          animateParameters = true,
        )
        .padding(vertical = 12.dp, horizontal = 6.dp)
        .testTag("liquid_floating_nav_rail"),
    content = content,
  )
}

/**
 * Translucent Liquid Glass card used for category groups, expandable summaries, details sections,
 * onboarding callouts, and hero containers.
 */
@Composable
fun LiquidGlassCard(
  modifier: Modifier = Modifier,
  shape: Shape = LiquidGlassShapes.CardShape,
  containerColor: Color = LiquidGlassDefaults.palette().cardSurfaceTint,
  squishable: Boolean = false,
  withShadow: Boolean = true,
  content: @Composable ColumnScope.() -> Unit,
) {
  Column(
    modifier =
      modifier
        .then(if (squishable) Modifier.liquidRowSquish() else Modifier)
        .fastLiquidSurface(
          shape = shape,
          surfaceTint = containerColor,
          withShadow = withShadow,
        ),
  ) {
    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
      content()
    }
  }
}

/**
 * Floating iOS Liquid Glass Action Button replacing flat Material FABs.
 */
@Composable
fun LiquidFloatingActionButton(
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  containerColor: Color = LiquidGlassDefaults.palette().primaryButtonTint,
  contentColor: Color = MaterialTheme.colorScheme.onPrimary,
  content: @Composable BoxScope.() -> Unit,
) {
  val backdrop = LocalLiquidBackdrop.current ?: LocalAtmosphericBackdrop.current
  val palette = LiquidGlassDefaults.palette()
  val config = LocalLiquidGlassConfig.current
  Box(
    contentAlignment = Alignment.Center,
    modifier =
      modifier
        .size(60.dp)
        .liquidButtonSquish()
        .liquidGlass(
          backdrop = backdrop,
          shape = LiquidGlassShapes.CapsuleShape,
          surfaceTint = containerColor,
          blurRadius = 7.dp,
          refractionHeight = 18.dp,
          refractionAmount = 36.dp,
          chromaticAberration = true,
          shadowRadius = 20.dp,
          shadowOffsetY = 8.dp,
          highlight = LiquidGlassDefaults.highlight(palette.isDark, config.highlightIntensity),
        )
        .clickable(role = Role.Button, onClick = onClick)
        .testTag("liquid_floating_fab"),
  ) {
    CompositionLocalProvider(LocalContentColor provides contentColor) {
      content()
    }
  }
}

/**
 * Official Kyant0-inspired `LiquidButton` pill with adaptive glass refraction, specular rim,
 * and pointer-tracking iOS squish.
 */
@Composable
fun LiquidGlassButton(
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  containerColor: Color = LiquidGlassDefaults.palette().primaryButtonTint,
  contentColor: Color = MaterialTheme.colorScheme.onPrimary,
  contentPadding: PaddingValues = PaddingValues(horizontal = 24.dp, vertical = 10.dp),
  externalSquishState: LiquidSquishState? = null,
  content: @Composable RowScope.() -> Unit,
) {
  val resolvedBg = if (enabled) containerColor else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
  val resolvedFg = if (enabled) contentColor else MaterialTheme.colorScheme.onSurfaceVariant
  Row(
    horizontalArrangement = Arrangement.Center,
    verticalAlignment = Alignment.CenterVertically,
    modifier =
      modifier
        .defaultMinSize(minHeight = 44.dp)
        .liquidButtonSquish(enabled = enabled, externalState = externalSquishState)
        .fastLiquidSurface(
          shape = LiquidGlassShapes.CapsuleShape,
          surfaceTint = resolvedBg,
          withShadow = enabled,
          respectUserConfig = false,
        )
        .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
        .padding(contentPadding),
  ) {
    CompositionLocalProvider(LocalContentColor provides resolvedFg) {
      ProvideTextStyle(MaterialTheme.typography.labelLarge) {
        content()
      }
    }
  }
}

/**
 * Frosted glass outline pill button with interactive squish.
 */
@Composable
fun LiquidGlassOutlineButton(
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  tintColor: Color = MaterialTheme.colorScheme.primary,
  contentPadding: PaddingValues = PaddingValues(horizontal = 24.dp, vertical = 10.dp),
  content: @Composable RowScope.() -> Unit,
) {
  val palette = LiquidGlassDefaults.palette()
  Row(
    horizontalArrangement = Arrangement.Center,
    verticalAlignment = Alignment.CenterVertically,
    modifier =
      modifier
        .defaultMinSize(minHeight = 44.dp)
        .liquidButtonSquish(enabled = enabled)
        .fastLiquidSurface(
          shape = LiquidGlassShapes.CapsuleShape,
          surfaceTint = palette.pillSurfaceTint,
          withShadow = false,
        )
        .border(
          BorderStroke(1.dp, tintColor.copy(alpha = if (enabled) 0.45f else 0.2f)),
          LiquidGlassShapes.CapsuleShape,
        )
        .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
        .padding(contentPadding),
  ) {
    CompositionLocalProvider(LocalContentColor provides tintColor) {
      ProvideTextStyle(MaterialTheme.typography.labelLarge) {
        content()
      }
    }
  }
}

/**
 * Tactile Liquid Glass chip container with spring color transition and iOS squish.
 */
@Composable
fun LiquidGlassChip(
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  selected: Boolean = false,
  leadingIcon: (@Composable () -> Unit)? = null,
  label: @Composable () -> Unit,
) {
  val palette = LiquidGlassDefaults.palette()
  val config = LocalLiquidGlassConfig.current
  val animatedSurface by
    animateColorAsState(
      targetValue = if (selected) palette.chipSelectedTint else palette.chipSurfaceTint,
      animationSpec = config.colorSpring(),
    )
  val borderColor by
    animateColorAsState(
      targetValue =
        if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
        else palette.fallbackRimTop.copy(alpha = if (palette.isDark) 0.30f else 0.7f),
      animationSpec = config.colorSpring(),
    )
  val contentColor =
    if (selected) MaterialTheme.colorScheme.onSecondaryContainer
    else MaterialTheme.colorScheme.onSurface

  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    modifier =
      modifier
        .defaultMinSize(minHeight = 40.dp)
        .liquidSquish(pressScale = 0.90f, stretchAmount = 0.06f, maxDragTranslationPx = 7f)
        .fastLiquidSurface(
          shape = LiquidGlassShapes.ChipShape,
          surfaceTint = animatedSurface,
          withShadow = false,
        )
        .border(0.85.dp, borderColor, LiquidGlassShapes.ChipShape)
        .clickable(role = Role.Button, onClick = onClick)
        .padding(horizontal = 14.dp, vertical = 8.dp),
  ) {
    CompositionLocalProvider(LocalContentColor provides contentColor) {
      if (leadingIcon != null) {
        Box(modifier = Modifier.size(18.dp), contentAlignment = Alignment.Center) {
          leadingIcon()
        }
      }
      ProvideTextStyle(MaterialTheme.typography.labelLarge) {
        label()
      }
    }
  }
}

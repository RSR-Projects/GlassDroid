package org.fdroid.ui.glass

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.capsule.ContinuousRoundedRectangle
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.fdroid.R

private object AppOpeningSessionState {
  var hasPlayed: Boolean = false
}

/**
 * Wraps the root scene with a 100% Kyant0 [Backdrop] Liquid Glass cold-open unfurl:
 * - Records the live UI scene into a [rememberLayerBackdrop]
 * - Expands a morphing Kyant0 [drawBackdrop] droplet lens (`vibrancy` + `blur` + `lens` with
 *   depth & chromatic aberration + specular highlight) smoothly from center out past viewport edges
 * - Blooms a translucent Kyant0 Liquid Glass app emblem capsule that dissolves into the live UI
 *
 * Non-blocking and one-shot per process launch, removing all overlay layers when settled (~760ms).
 */
@Composable
fun LiquidAppOpeningHost(
  modifier: Modifier = Modifier,
  content: @Composable () -> Unit,
) {
  val isPreview = LocalInspectionMode.current
  val reducedMotion = LocalReducedMotion.current
  val config = LocalLiquidGlassConfig.current
  val palette = LiquidGlassDefaults.palette()
  val density = LocalDensity.current
  val bgColor = MaterialTheme.colorScheme.background

  val shouldAnimate =
    remember {
      !isPreview && !reducedMotion && !config.reducedVisualEffects && !AppOpeningSessionState.hasPlayed
    }
  var showOverlay by remember { mutableStateOf(shouldAnimate) }
  var containerSize by remember { mutableStateOf(IntSize.Zero) }

  val openBackdrop = rememberLayerBackdrop {
    drawRect(bgColor)
    drawContent()
  }

  val sceneProgress = remember { Animatable(if (shouldAnimate) 0f else 1f) }
  val waveProgress = remember { Animatable(if (shouldAnimate) 0f else 1f) }
  val emblemAlpha = remember { Animatable(0f) }

  LaunchedEffect(shouldAnimate) {
    if (shouldAnimate) {
      AppOpeningSessionState.hasPlayed = true
      launch {
        emblemAlpha.animateTo(1f, tween(160, easing = FastOutSlowInEasing))
        emblemAlpha.animateTo(0f, tween(340, delayMillis = 120, easing = FastOutSlowInEasing))
      }
      launch {
        sceneProgress.animateTo(1f, config.bouncySpatialSpring())
      }
      waveProgress.animateTo(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 760, easing = CubicBezierEasing(0.18f, 0.85f, 0.25f, 1f)),
      )
      showOverlay = false
    }
  }

  val wave = waveProgress.value
  val scene = sceneProgress.value

  Box(modifier = modifier.fillMaxSize().onSizeChanged { containerSize = it }) {
    Box(
      modifier =
        Modifier.fillMaxSize()
          .layerBackdrop(openBackdrop)
          .graphicsLayer {
            if (showOverlay && wave < 0.996f) {
              val s = 0.945f + 0.055f * scene
              scaleX = s
              scaleY = s
              alpha = (0.38f + 0.62f * scene).coerceIn(0f, 1f)
            }
          }
    ) {
      content()
    }

    if (showOverlay && wave < 0.996f && containerSize.width > 0 && containerSize.height > 0) {
      val fadeOut = ((1f - wave) * 1.35f).coerceIn(0f, 1f)
      val bell = sin(wave * PI).toFloat().coerceIn(0f, 1f)
      val easeOut = 1f - (1f - wave) * (1f - wave) * (1f - wave)

      val fullW = with(density) { containerSize.width.toDp() }
      val fullH = with(density) { containerSize.height.toDp() }
      val curW = 148.dp + (fullW * 1.24f - 148.dp) * easeOut
      val curH = 88.dp + (fullH * 1.24f - 88.dp) * easeOut
      val cornerRadius = (44.dp * (1f - easeOut * 0.65f)).coerceAtLeast(16.dp)
      val dropletShape = ContinuousRoundedRectangle(cornerRadius)

      // Expanding Kyant0 Liquid Glass Lens Wave across the viewport
      Box(
        modifier =
          Modifier.align(Alignment.Center)
            .width(curW)
            .height(curH)
            .drawBackdrop(
              backdrop = openBackdrop,
              shape = { dropletShape },
              effects = {
                vibrancy()
                val portalBlur =
                  (9.dp * config.blurScale.coerceAtLeast(0.70f) * (1f - wave * 0.65f)).toPx()
                if (portalBlur > 0.5f) {
                  blur(portalBlur)
                }
                lens(
                  refractionHeight =
                    (24.dp * config.refractionHeightScale * (0.45f + 0.55f * bell))
                      .toPx()
                      .coerceAtLeast(1f),
                  refractionAmount =
                    (48.dp * config.refractionAmountScale * (0.40f + 0.60f * bell))
                      .toPx()
                      .coerceAtLeast(1f),
                  depthEffect = true,
                  chromaticAberration = config.chromaticAberration || bell > 0.15f,
                )
              },
              highlight = {
                LiquidGlassDefaults.highlight(
                  palette.isDark,
                  1.30f * config.highlightIntensity * fadeOut,
                )
              },
              shadow = { LiquidGlassDefaults.floatingShadow(palette.isDark, 28.dp, 10.dp) },
              innerShadow = { LiquidGlassDefaults.pillInnerShadow(palette.isDark) },
              layerBlock = { alpha = fadeOut },
              onDrawSurface = {
                drawRect(
                  palette.pillSurfaceTint.copy(
                    alpha = (palette.pillSurfaceTint.alpha * 0.36f * fadeOut).coerceIn(0f, 0.32f)
                  )
                )
                drawRect(
                  brush =
                    Brush.verticalGradient(
                      colors =
                        listOf(
                          Color.White.copy(alpha = (if (palette.isDark) 0.12f else 0.28f) * fadeOut),
                          Color.Transparent,
                        ),
                      startY = 0f,
                      endY = size.height * 0.45f,
                    )
                )
              },
            )
      )

      // Floating Kyant0 Liquid Glass Emblem Badge centered inside the expanding lens
      val embAlpha = emblemAlpha.value.coerceIn(0f, 1f)
      if (embAlpha > 0.01f) {
        val embScale = 0.78f + 0.46f * wave
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          modifier =
            Modifier.align(Alignment.Center)
              .graphicsLayer {
                scaleX = embScale
                scaleY = embScale
                alpha = embAlpha
              }
              .liquidGlass(
                backdrop = openBackdrop,
                shape = LiquidGlassShapes.CapsuleShape,
                surfaceTint = palette.pillSurfaceTint,
                blurRadius = 8.dp,
                refractionHeight = 18.dp,
                refractionAmount = 36.dp,
                chromaticAberration = true,
                shadowRadius = 22.dp,
                shadowOffsetY = 8.dp,
              )
              .padding(horizontal = 22.dp, vertical = 12.dp),
        ) {
          Image(
            painter = painterResource(R.drawable.ic_glassdroid_fdroid_icon),
            contentDescription = null,
            modifier = Modifier.size(38.dp).clip(LiquidGlassShapes.CapsuleShape),
          )
          Text(
            text = stringResource(R.string.app_name),
            color = if (palette.isDark) Color.White else MaterialTheme.colorScheme.onSurface,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
          )
        }
      }
    }
  }
}

/**
 * Coordinates the interactive Kyant0 Liquid Glass melting card-open effect when the user taps
 * an app anywhere in Discover, AppList, Search, or My Apps.
 */
@Stable
class LiquidMeltCoordinator(private val scope: CoroutineScope) {
  var activePackage by mutableStateOf<String?>(null)
    private set

  var tapOriginWindow by mutableStateOf(Offset.Unspecified)
    private set

  var cardBoundsWindow by mutableStateOf(Rect.Zero)
    private set

  val progress = Animatable(0f)

  private var activeJob: Job? = null
  private var lastLaunchAtMillis = 0L

  val isMelting: Boolean
    get() = activePackage != null && progress.value < 0.996f

  fun launchMelt(
    packageName: String,
    originWindow: Offset,
    boundsWindow: Rect,
    reducedMotion: Boolean,
    onNavigate: () -> Unit,
  ) {
    val now = System.currentTimeMillis()
    if (activePackage == packageName && now - lastLaunchAtMillis < 350L) {
      return
    }
    lastLaunchAtMillis = now

    if (reducedMotion) {
      activePackage = null
      onNavigate()
      return
    }

    activeJob?.cancel()
    activePackage = packageName
    tapOriginWindow =
      if (originWindow != Offset.Unspecified) originWindow
      else if (boundsWindow != Rect.Zero) boundsWindow.center
      else Offset.Unspecified
    cardBoundsWindow = boundsWindow

    activeJob =
      scope.launch {
        progress.snapTo(0f)
        // Phase 1 (~92ms): card liquefies & swells into a molten Kyant0 glass droplet under finger
        progress.animateTo(
          targetValue = 0.24f,
          animationSpec = tween(durationMillis = 92, easing = CubicBezierEasing(0.32f, 0f, 0.7f, 1f)),
        )
        onNavigate()
        // Phase 2 (~410ms): molten Kyant0 glass lens expands smoothly across viewport into App Details
        progress.animateTo(
          targetValue = 1f,
          animationSpec = tween(durationMillis = 410, easing = FastOutSlowInEasing),
        )
        activePackage = null
      }
  }
}

val LocalLiquidMeltCoordinator = staticCompositionLocalOf<LiquidMeltCoordinator?> { null }

@Composable
fun rememberLiquidMeltCoordinator(): LiquidMeltCoordinator {
  val scope = rememberCoroutineScope()
  return remember(scope) { LiquidMeltCoordinator(scope) }
}

/**
 * Tracks window bounds and exact finger down coordinates on an app card/row, returning a click
 * handler that launches the Kyant0 Liquid Glass melting card-open transition.
 */
@Composable
fun rememberLiquidMeltOpener(
  packageName: String,
  onOpen: () -> Unit,
): Pair<Modifier, () -> Unit> {
  val coordinator = LocalLiquidMeltCoordinator.current
  val reducedMotion = LocalReducedMotion.current
  val config = LocalLiquidGlassConfig.current
  val skipMelt = reducedMotion || config.reducedVisualEffects
  val currentPackage by rememberUpdatedState(packageName)
  val currentOnOpen by rememberUpdatedState(onOpen)

  var coords by remember { mutableStateOf<LayoutCoordinates?>(null) }
  var lastWindowTap by remember { mutableStateOf(Offset.Unspecified) }

  val trackingModifier =
    Modifier.onGloballyPositioned { coords = it }
      .pointerInput(packageName) {
        awaitPointerEventScope {
          while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            val primary = event.changes.firstOrNull() ?: continue
            if (primary.pressed) {
              val c = coords
              if (c != null && c.isAttached) {
                lastWindowTap = c.localToWindow(primary.position)
              }
            }
          }
        }
      }
      .liquidMeltSource(packageName)

  val trigger: () -> Unit = {
    val c = coords
    val bounds = if (c != null && c.isAttached) c.boundsInWindow() else Rect.Zero
    val origin =
      when {
        lastWindowTap != Offset.Unspecified -> lastWindowTap
        bounds != Rect.Zero -> bounds.center
        else -> Offset.Unspecified
      }
    if (coordinator != null) {
      coordinator.launchMelt(
        packageName = currentPackage,
        originWindow = origin,
        boundsWindow = bounds,
        reducedMotion = skipMelt,
        onNavigate = currentOnOpen,
      )
    } else {
      currentOnOpen()
    }
  }

  return trackingModifier to trigger
}

/**
 * Softens and stretches the tapped source card/row during Phase 1 (`0f..0.28f`) as it feeds
 * into the melting Kyant0 Liquid Glass lens overlay.
 */
fun Modifier.liquidMeltSource(packageName: String): Modifier = composed {
  val coordinator = LocalLiquidMeltCoordinator.current ?: return@composed this
  val isTarget = coordinator.activePackage == packageName
  if (!isTarget) return@composed this

  val p = coordinator.progress.value
  val phase1 = (p / 0.28f).coerceIn(0f, 1f)

  this.graphicsLayer {
    scaleX = 1f + 0.048f * phase1
    scaleY = 1f + 0.088f * phase1
    translationY = phase1 * 9.dp.toPx()
    alpha = (1f - ((p - 0.14f) / 0.18f).coerceIn(0f, 0.72f))
  }
}

/**
 * Viscous spatial settle reveal for the emerging App Details root as the Kyant0 Liquid Glass
 * melt droplet washes across the screen (`0.18f..1.0f`).
 */
fun Modifier.liquidMeltDestination(packageName: String): Modifier = composed {
  val coordinator = LocalLiquidMeltCoordinator.current ?: return@composed this
  val reducedMotion = LocalReducedMotion.current
  val isActive = !reducedMotion && coordinator.activePackage == packageName
  if (!isActive) return@composed this

  val p = coordinator.progress.value
  val reveal = ((p - 0.18f) / 0.82f).coerceIn(0f, 1f)

  this.graphicsLayer {
    if (p < 0.996f) {
      val settle = sin(reveal * PI * 0.5).toFloat()
      val s = 0.94f + 0.06f * settle
      scaleX = s
      scaleY = s + (1f - settle) * 0.022f
      translationY = (1f - settle) * 14.dp.toPx()
      alpha = (0.18f + 0.82f * reveal).coerceIn(0f, 1f)
    }
  }
}

/**
 * 100% Kyant0 [Backdrop] Liquid Glass card-melting overlay.
 *
 * Bridges the tapped source card bounds smoothly out to full screen using two layered Kyant0
 * [drawBackdrop] molten glass droplets (`vibrancy` + `blur` + deep optical `lens` + chromatic
 * aberration + specular `Highlight` + `InnerShadow`) melting downward slightly with fluid
 * rounded-corner morphing before dissolving cleanly into [AppDetails].
 */
@Composable
fun LiquidMeltOverlay(
  coordinator: LiquidMeltCoordinator,
  backdrop: Backdrop? = LocalLiquidBackdrop.current ?: LocalAtmosphericBackdrop.current,
  modifier: Modifier = Modifier,
) {
  val pkg = coordinator.activePackage ?: return
  val p = coordinator.progress.value
  if (p <= 0.001f || p >= 0.996f) return

  val palette = LiquidGlassDefaults.palette()
  val config = LocalLiquidGlassConfig.current
  val density = LocalDensity.current
  var rootCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
  var overlaySize by remember { mutableStateOf(IntSize.Zero) }

  Box(
    modifier =
      modifier
        .fillMaxSize()
        .onGloballyPositioned { rootCoords = it }
        .onSizeChanged { overlaySize = it }
  ) {
    if (backdrop != null && overlaySize.width > 0 && overlaySize.height > 0) {
      val screenW = overlaySize.width.toFloat()
      val screenH = overlaySize.height.toFloat()
      val coords = rootCoords?.takeIf { it.isAttached }

      val localOrigin =
        coords
          ?.takeIf { coordinator.tapOriginWindow != Offset.Unspecified }
          ?.windowToLocal(coordinator.tapOriginWindow)
          ?: Offset(screenW * 0.5f, screenH * 0.42f)

      val localStartRect =
        if (coords != null && coordinator.cardBoundsWindow != Rect.Zero) {
          val tl = coords.windowToLocal(coordinator.cardBoundsWindow.topLeft)
          val br = coords.windowToLocal(coordinator.cardBoundsWindow.bottomRight)
          Rect(tl, br)
        } else {
          val half = with(density) { 64.dp.toPx() }
          Rect(localOrigin.x - half, localOrigin.y - half, localOrigin.x + half, localOrigin.y + half)
        }

      // Viscous ease curve + downward gravity sag during melt
      val meltEase = 1f - (1f - p) * (1f - p) * (1f - p)
      val bell = sin(p * PI).toFloat().coerceIn(0f, 1f)
      val fade = ((1f - p) * 1.45f).coerceIn(0f, 1f)
      val gravitySagPx = with(density) { 26.dp.toPx() } * bell

      // Primary molten Kyant0 glass lens body morphing from card bounds -> overflowing screen lens
      val targetPad = with(density) { 48.dp.toPx() }
      val targetLeft = -targetPad
      val targetTop = -targetPad
      val targetW = screenW + targetPad * 2f
      val targetH = screenH + targetPad * 2f

      val startW = max(localStartRect.width, with(density) { 96.dp.toPx() })
      val startH = max(localStartRect.height, with(density) { 80.dp.toPx() })
      val curLeft = localStartRect.left + (targetLeft - localStartRect.left) * meltEase
      val curTop =
        localStartRect.top + (targetTop - localStartRect.top) * meltEase + gravitySagPx
      val curW = startW + (targetW - startW) * meltEase
      val curH = startH + (targetH - startH) * meltEase

      val meltRadiusDp = 20.dp + 38.dp * bell
      val mainMeltShape = ContinuousRoundedRectangle(meltRadiusDp)

      Box(
        modifier =
          Modifier.offset { IntOffset(curLeft.roundToInt(), curTop.roundToInt()) }
            .width(with(density) { curW.toDp() })
            .height(with(density) { curH.toDp() })
            .drawBackdrop(
              backdrop = backdrop,
              shape = { mainMeltShape },
              effects = {
                vibrancy()
                val meltBlur =
                  (9.5.dp * config.blurScale.coerceAtLeast(0.75f) * (0.45f + 0.55f * bell)).toPx()
                if (meltBlur > 0.5f) {
                  blur(meltBlur)
                }
                lens(
                  refractionHeight =
                    (22.dp * config.refractionHeightScale * (0.5f + 0.65f * bell))
                      .toPx()
                      .coerceAtLeast(1f),
                  refractionAmount =
                    (46.dp * config.refractionAmountScale * (0.5f + 0.70f * bell))
                      .toPx()
                      .coerceAtLeast(1f),
                  depthEffect = true,
                  chromaticAberration = true,
                )
              },
              highlight = { LiquidGlassDefaults.highlight(palette.isDark, 1.32f * fade) },
              shadow = { LiquidGlassDefaults.floatingShadow(palette.isDark, 24.dp, 12.dp) },
              innerShadow = { LiquidGlassDefaults.pillInnerShadow(palette.isDark) },
              layerBlock = {
                // Slight viscous vertical elongation in the middle of the melt
                scaleY = 1f + 0.055f * bell
                alpha = fade
              },
              onDrawSurface = {
                drawRect(
                  palette.pillSurfaceTint.copy(
                    alpha = (palette.pillSurfaceTint.alpha * 0.42f * fade).coerceIn(0f, 0.34f)
                  )
                )
                drawRect(
                  brush =
                    Brush.verticalGradient(
                      colors =
                        listOf(
                          Color.White.copy(alpha = (if (palette.isDark) 0.14f else 0.30f) * fade),
                          Color.Transparent,
                        ),
                      startY = 0f,
                      endY = size.height * 0.44f,
                    )
                )
              },
            )
      )

      // Trailing viscous Kyant0 liquid droplet lagging smoothly underneath the melting crest
      if (bell > 0.12f) {
        val dropDiameterDp = (64f + 56f * bell).dp
        val dropRadiusPx = with(density) { dropDiameterDp.toPx() * 0.5f }
        val dropX = localOrigin.x - dropRadiusPx
        val dropY = localOrigin.y - dropRadiusPx + with(density) { (78.dp * p * p).toPx() }
        val dropFade = (bell * fade).coerceIn(0f, 1f)
        Box(
          modifier =
            Modifier.offset { IntOffset(dropX.roundToInt(), dropY.roundToInt()) }
              .size(dropDiameterDp)
              .drawBackdrop(
                backdrop = backdrop,
                shape = { LiquidGlassShapes.CapsuleShape },
                effects = {
                  vibrancy()
                  blur((8.dp * config.blurScale.coerceAtLeast(0.7f)).toPx())
                  lens(
                    refractionHeight = (20.dp * config.refractionHeightScale).toPx(),
                    refractionAmount = (40.dp * config.refractionAmountScale).toPx(),
                    depthEffect = true,
                    chromaticAberration = true,
                  )
                },
                highlight = { LiquidGlassDefaults.highlight(palette.isDark, 1.35f * dropFade) },
                shadow = { LiquidGlassDefaults.floatingShadow(palette.isDark, 18.dp, 8.dp) },
                innerShadow = { LiquidGlassDefaults.pillInnerShadow(palette.isDark) },
                layerBlock = {
                  scaleX = 0.94f + 0.08f * bell
                  scaleY = 1.08f + 0.14f * bell
                  alpha = dropFade
                },
                onDrawSurface = {
                  drawRect(Color.White.copy(alpha = (if (palette.isDark) 0.08f else 0.16f) * dropFade))
                },
              )
        )
      }

      @Suppress("UNUSED_EXPRESSION")
      pkg
    }
  }
}

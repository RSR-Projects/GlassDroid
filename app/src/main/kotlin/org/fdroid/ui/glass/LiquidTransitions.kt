package org.fdroid.ui.glass

import android.animation.ValueAnimator
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Shape
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * Lightweight scroll-depth holder allowing floating glass bars (top bar & bottom dock) to adapt
 * blur, tint, and rim illumination smoothly as list content scrolls beneath them without extra
 * Backdrop capture passes.
 */
@Stable
class LiquidScrollState {
  var scrollFraction by mutableFloatStateOf(0f)
    internal set
}

val LocalLiquidScrollState = staticCompositionLocalOf<LiquidScrollState?> { null }

val LocalReducedMotion = staticCompositionLocalOf { false }

val LocalSharedTransitionScope = staticCompositionLocalOf<SharedTransitionScope?> { null }

val LocalAppAnimatedContentScope = staticCompositionLocalOf<AnimatedVisibilityScope?> { null }

val LocalIsBigScreen = staticCompositionLocalOf { false }

@Composable
fun rememberSystemReducedMotion(): Boolean =
  remember { runCatching { !ValueAnimator.areAnimatorsEnabled() }.getOrDefault(false) }

/**
 * Reports normalized scroll depth (0f..1f over the first ~140px of scroll) from a [ScrollState]
 * into [LocalLiquidScrollState] using [snapshotFlow] so fast scrolling causes zero extra
 * recompositions of the list itself.
 */
fun Modifier.reportLiquidScroll(scrollState: ScrollState): Modifier = composed {
  val liquidScroll = LocalLiquidScrollState.current
  if (liquidScroll != null) {
    LaunchedEffect(scrollState, liquidScroll) {
      snapshotFlow { (scrollState.value / 140f).coerceIn(0f, 1f) }
        .distinctUntilChanged()
        .collect { liquidScroll.scrollFraction = it }
    }
  }
  this
}

/**
 * Reports normalized scroll depth (0f..1f) from a [LazyListState] into [LocalLiquidScrollState].
 */
fun Modifier.reportLiquidScroll(lazyListState: LazyListState): Modifier = composed {
  val liquidScroll = LocalLiquidScrollState.current
  if (liquidScroll != null) {
    LaunchedEffect(lazyListState, liquidScroll) {
      snapshotFlow {
        if (lazyListState.firstVisibleItemIndex > 0) 1f
        else (lazyListState.firstVisibleItemScrollOffset / 140f).coerceIn(0f, 1f)
      }
        .distinctUntilChanged()
        .collect { liquidScroll.scrollFraction = it }
    }
  }
  this
}

/**
 * Wires Jetpack Compose shared-bounds hero expansion between list/carousel cards and [AppDetails]
 * when running in single-pane mode (`!LocalIsBigScreen.current`). Safely no-ops in previews,
 * dialogs, and dual-pane tablet layouts where source & target coexist on screen.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
fun Modifier.appHeroSharedBounds(
  key: String,
  clipShape: Shape = LiquidGlassShapes.CardShape,
): Modifier = composed {
  val sharedScope = LocalSharedTransitionScope.current
  val animScope = LocalAppAnimatedContentScope.current
  val isBigScreen = LocalIsBigScreen.current
  val reducedMotion = LocalReducedMotion.current
  if (sharedScope == null || animScope == null || isBigScreen || reducedMotion) {
    this
  } else {
    with(sharedScope) {
      this@composed.sharedBounds(
        sharedContentState = rememberSharedContentState(key = key),
        animatedVisibilityScope = animScope,
        resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds(),
        clipInOverlayDuringTransition = OverlayClip(clipShape),
      )
    }
  }
}

/**
 * Wires shared-element hero morphing for app icons and titles across card -> details transitions.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
fun Modifier.appHeroSharedElement(key: String): Modifier = composed {
  val sharedScope = LocalSharedTransitionScope.current
  val animScope = LocalAppAnimatedContentScope.current
  val isBigScreen = LocalIsBigScreen.current
  val reducedMotion = LocalReducedMotion.current
  if (sharedScope == null || animScope == null || isBigScreen || reducedMotion) {
    this
  } else {
    with(sharedScope) {
      this@composed.sharedElement(
        sharedContentState = rememberSharedContentState(key = key),
        animatedVisibilityScope = animScope,
      )
    }
  }
}

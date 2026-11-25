package org.fdroid.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.PaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.ui.NavDisplay
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.viktormykhailiv.compose.hints.hintAnchor
import com.viktormykhailiv.compose.hints.rememberHintAnchorState
import com.viktormykhailiv.compose.hints.rememberHintController
import org.fdroid.ui.glass.LiquidAppOpeningHost
import org.fdroid.ui.glass.LiquidMeltOverlay
import org.fdroid.ui.glass.LiquidToastHostState
import org.fdroid.ui.glass.LiquidToastOverlay
import org.fdroid.ui.glass.LocalIsBigScreen
import org.fdroid.ui.glass.LocalLiquidMeltCoordinator
import org.fdroid.ui.glass.LocalLiquidToastHost
import org.fdroid.ui.glass.LocalSharedTransitionScope
import org.fdroid.ui.glass.rememberLiquidMeltCoordinator
import org.fdroid.ui.navigation.BottomBar
import org.fdroid.ui.navigation.NavigationRail
import org.fdroid.ui.navigation.rememberResponsiveNavigationSceneDecoratorStrategy
import org.fdroid.ui.utils.HintOverlayContainer

@Composable
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
fun MainContent(
  model: MainModel,
  navEntries: List<NavEntry<NavKey>>,
  directive: PaneScaffoldDirective,
  isBigScreen: Boolean,
  showBottomBar: Boolean,
  currentNavKey: NavKey,
  onOnboardingSeen: () -> Unit,
  onNav: (NavKey) -> Unit,
  onBack: () -> Unit,
) =
  FDroidContent(
    dynamicColors = model.dynamicColors,
    liquidGlassConfig = model.liquidGlassConfig,
  ) {
    val config = model.liquidGlassConfig
    val toastHostState = remember { LiquidToastHostState() }
    val meltCoordinator = rememberLiquidMeltCoordinator()
    LaunchedEffect(toastHostState) {
      org.fdroid.ui.glass.LiquidToastBus.events.collect { (message, type) ->
        toastHostState.show(message, type)
      }
    }
    val listDetailStrategy = rememberListDetailSceneStrategy<NavKey>(directive = directive)
    CompositionLocalProvider(
      LocalLiquidToastHost provides toastHostState,
      LocalLiquidMeltCoordinator provides meltCoordinator,
      LocalIsBigScreen provides isBigScreen,
    ) {
      SharedTransitionLayout {
        CompositionLocalProvider(LocalSharedTransitionScope provides this) {
          HintOverlayContainer {
            val hintController = rememberHintController()
            val onOnboardingSeen = {
              hintController.dismiss()
              onOnboardingSeen()
            }
            val hintAnchor = rememberHintAnchorState(getMainOnboardingHint(onOnboardingSeen, onNav))
            LaunchedEffect(model.showOnboarding) {
              if (model.showOnboarding) {
                hintController.show(hintAnchor)
              }
            }
            val responsiveNavigationSceneDecoratorStrategy =
              rememberResponsiveNavigationSceneDecoratorStrategy<NavKey>(
                isBigScreen = isBigScreen,
                showBottomBar = showBottomBar,
                navBar = {
                  AnimatedVisibility(
                    visible = showBottomBar,
                    enter =
                      slideInVertically(animationSpec = config.navSlideSpring()) { it } +
                        scaleIn(
                          initialScale = 0.9f,
                          animationSpec = config.bouncySpatialSpring(),
                        ) +
                        fadeIn(config.smoothSpatialSpring()),
                    exit =
                      slideOutVertically(animationSpec = config.navSlideSpring()) { it } +
                        scaleOut(
                          targetScale = 0.9f,
                          animationSpec = config.smoothSpatialSpring(),
                        ) +
                        fadeOut(config.smoothSpatialSpring()),
                  ) {
                    BottomBar(model = model, currentNavKey = currentNavKey, onNav = onNav)
                  }
                },
                navRail = {
                  NavigationRail(
                    numUpdates = model.numUpdates,
                    hasIssues = model.hasAppIssues,
                    currentNavKey = currentNavKey,
                    onNav = onNav,
                  )
                },
                sharedTransitionScope = this,
              )
            LiquidAppOpeningHost(modifier = Modifier.fillMaxSize()) {
              val meltBg = MaterialTheme.colorScheme.background
              val meltBackdrop = rememberLayerBackdrop {
                drawRect(meltBg)
                drawContent()
              }
              Box(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.fillMaxSize().layerBackdrop(meltBackdrop)) {
                  NavDisplay(
                  entries = navEntries,
                  sceneDecoratorStrategies = listOf(responsiveNavigationSceneDecoratorStrategy),
                  sceneStrategies = listOf(listDetailStrategy),
                  transitionSpec = {
                    if (showBottomBar) {
                      fadeIn(config.smoothSpatialSpring()) +
                        scaleIn(
                          initialScale = 0.97f,
                          animationSpec = config.bouncySpatialSpring(),
                        ) togetherWith fadeOut(config.smoothSpatialSpring())
                    } else {
                      slideInHorizontally(animationSpec = config.navSlideSpring()) { it / 2 } +
                        scaleIn(
                          initialScale = 0.94f,
                          animationSpec = config.bouncySpatialSpring(),
                        ) +
                        fadeIn(config.smoothSpatialSpring()) togetherWith
                        slideOutHorizontally(animationSpec = config.navSlideSpring()) { -it / 4 } +
                        scaleOut(
                          targetScale = 0.94f,
                          animationSpec = config.smoothSpatialSpring(),
                        ) +
                        fadeOut(config.smoothSpatialSpring())
                    }
                  },
                  popTransitionSpec = {
                    if (showBottomBar) {
                      fadeIn(config.smoothSpatialSpring()) +
                        scaleIn(
                          initialScale = 0.97f,
                          animationSpec = config.bouncySpatialSpring(),
                        ) togetherWith fadeOut(config.smoothSpatialSpring())
                    } else {
                      slideInHorizontally(animationSpec = config.navSlideSpring()) { -it / 4 } +
                        scaleIn(
                          initialScale = 0.94f,
                          animationSpec = config.smoothSpatialSpring(),
                        ) +
                        fadeIn(config.smoothSpatialSpring()) togetherWith
                        slideOutHorizontally(animationSpec = config.navSlideSpring()) { it / 2 } +
                        scaleOut(
                          targetScale = 0.95f,
                          animationSpec = config.smoothSpatialSpring(),
                        ) +
                        fadeOut(config.smoothSpatialSpring())
                    }
                  },
                  predictivePopTransitionSpec = {
                    if (showBottomBar) {
                      fadeIn(config.smoothSpatialSpring()) togetherWith
                        fadeOut(config.smoothSpatialSpring())
                    } else {
                      slideInHorizontally(animationSpec = config.navSlideSpring()) { -it / 3 } +
                        scaleIn(
                          initialScale = 0.94f,
                          animationSpec = config.smoothSpatialSpring(),
                        ) +
                        fadeIn(config.smoothSpatialSpring()) togetherWith
                        slideOutHorizontally(animationSpec = config.navSlideSpring()) { it / 2 } +
                        fadeOut(config.smoothSpatialSpring())
                    }
                  },
                  onBack = onBack,
                  modifier = Modifier.fillMaxSize().hintAnchor(hintAnchor, fullScreen = true),
                )
                }
                LiquidMeltOverlay(coordinator = meltCoordinator, backdrop = meltBackdrop)
                LiquidToastOverlay(
                  hostState = toastHostState,
                  modifier = Modifier.align(Alignment.TopCenter),
                )
              }
            }
          }
        }
      }
    }
  }

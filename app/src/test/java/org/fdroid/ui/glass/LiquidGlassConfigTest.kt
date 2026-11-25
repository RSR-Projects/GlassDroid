package org.fdroid.ui.glass

import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class LiquidGlassConfigTest {

  @Test
  fun glassProfilesSetExpectedOpticalPresetsAndMigrateLegacyNames() {
    val base = LiquidGlassConfig()
    assertEquals(GlassProfile.ADAPTIVE, base.glassProfile)
    assertEquals(GlassTintPreset.ADAPTIVE, base.glassTint)

    val frosted = base.withGlassProfile(GlassProfile.FROSTED)
    assertEquals(GlassProfile.FROSTED, frosted.glassProfile)
    assertFalse(frosted.chromaticAberration)
    assertTrue(frosted.blurScale > base.blurScale)
    assertTrue(frosted.tintStrength > base.tintStrength)
    assertTrue(frosted.transparency < base.transparency)

    val clear = frosted.withGlassProfile(GlassProfile.CLEAR)
    assertEquals(GlassProfile.CLEAR, clear.glassProfile)
    assertEquals(0f, clear.blurScale)
    assertTrue(clear.chromaticAberration)
    assertTrue(clear.transparency > base.transparency)
    assertTrue(clear.refractionAmountScale > frosted.refractionAmountScale)

    val deep = clear.withGlassProfile(GlassProfile.DEEP)
    assertEquals(GlassProfile.DEEP, deep.glassProfile)
    assertTrue(deep.blurScale > frosted.blurScale)
    assertTrue(deep.tintStrength > frosted.tintStrength)
    assertTrue(deep.highlightIntensity > frosted.highlightIntensity)
    assertTrue(deep.chromaticAberration)

    val modified =
      deep.copy(
        glassTint = GlassTintPreset.VIOLET,
        reducedVisualEffects = true,
      )
    val reset = modified.resetGlass()
    assertEquals(GlassProfile.ADAPTIVE, reset.glassProfile)
    assertEquals(GlassTintPreset.ADAPTIVE, reset.glassTint)
    assertFalse(reset.reducedVisualEffects)
    assertEquals(base.blurScale, reset.blurScale)
    assertEquals(base.transparency, reset.transparency)

    assertEquals(GlassProfile.ADAPTIVE, GlassProfile.fromPrefString("DEFAULT"))
    assertEquals(GlassProfile.CLEAR, GlassProfile.fromPrefString("CRYSTAL"))
    assertEquals(GlassProfile.FROSTED, GlassProfile.fromPrefString("READABLE"))
    assertEquals(GlassProfile.DEEP, GlassProfile.fromPrefString("DEEP"))
    assertEquals(GlassTintPreset.EMERALD, GlassTintPreset.fromPrefString("EMERALD"))
    assertEquals(GlassTintPreset.ADAPTIVE, GlassTintPreset.fromPrefString("UNKNOWN_TINT"))
  }

  @Test
  fun motionProfilesAnimationIntensityAndSpringBoundsStaySafe() {
    val perf = LiquidGlassConfig().withMotionProfile(MotionProfile.PERFORMANCE)
    val bouncy = perf.withMotionProfile(MotionProfile.BOUNCY)
    val normal = bouncy.withMotionProfile(MotionProfile.NORMAL)

    assertTrue(perf.pressDampingRatio() > bouncy.pressDampingRatio())
    assertTrue(perf.pressStiffness() > bouncy.pressStiffness())
    assertEquals(MotionProfile.NORMAL, normal.motionProfile)

    val disabledAndScaled = normal.copy(animationsEnabled = false, animationIntensity = 0.5f)
    assertTrue(disabledAndScaled.effectivePressDepth() < normal.effectivePressDepth())
    val resetMotion = disabledAndScaled.resetMotion()
    assertEquals(MotionProfile.BOUNCY, resetMotion.motionProfile)
    assertTrue(resetMotion.animationsEnabled)
    assertEquals(1.00f, resetMotion.animationIntensity)

    val extremeLow =
      LiquidGlassConfig(
        bounce = -10f,
        speed = 0.01f,
        releaseJiggle = -5f,
        animationIntensity = -2f,
      )
    val extremeHigh =
      LiquidGlassConfig(
        bounce = 10f,
        speed = 10f,
        releaseJiggle = 10f,
        animationIntensity = 10f,
      )

    listOf(extremeLow, extremeHigh).forEach { cfg ->
      assertTrue(cfg.pressDampingRatio() in 0.28f..0.98f)
      assertTrue(cfg.releaseDampingRatio() in 0.18f..0.98f)
      assertTrue(cfg.jiggleDampingRatio() in 0.14f..0.92f)
      assertTrue(cfg.pressStiffness() in 140f..2600f)
      assertTrue(cfg.releaseStiffness() in 85f..1900f)
      assertTrue(cfg.jiggleStiffness() in 70f..1700f)

      val tabSpring = cfg.tabIndicatorSpring()
      assertTrue(tabSpring.dampingRatio in 0.22f..0.96f)
      assertTrue(tabSpring.stiffness in 90f..1800f)

      val navSpring = cfg.navSlideSpring()
      assertTrue(navSpring.dampingRatio in 0.45f..0.98f)
      assertTrue(navSpring.stiffness in 110f..2000f)
    }
  }

  @Test
  fun liquidScrollStateDefaultsToZeroAndUpdates() {
    val state = LiquidScrollState()
    assertEquals(0f, state.scrollFraction)
    state.scrollFraction = 0.65f
    assertEquals(0.65f, state.scrollFraction)
  }

  @Test
  fun liquidMeltCoordinatorReducedMotionNavigatesImmediately() {
    val scope = TestScope()
    val coordinator = LiquidMeltCoordinator(scope)
    var navigated = 0
    coordinator.launchMelt(
      packageName = "org.example.app",
      originWindow = Offset(100f, 200f),
      boundsWindow = Rect(50f, 150f, 150f, 250f),
      reducedMotion = true,
    ) {
      navigated++
    }
    assertEquals(1, navigated)
    assertNull(coordinator.activePackage)
    assertFalse(coordinator.isMelting)
  }

  @Test
  fun liquidMeltCoordinatorDebouncesSamePackageAndSettlesToNull() = runTest {
    var timeNanos = 0L
    val testClock =
      object : MonotonicFrameClock {
        override suspend fun <R> withFrameNanos(onFrame: (Long) -> R): R {
          yield()
          timeNanos += 16_000_000L
          return onFrame(timeNanos)
        }
      }
    val coordinator = LiquidMeltCoordinator(CoroutineScope(coroutineContext + testClock))
    var navigated = 0
    val bounds = Rect(0f, 0f, 200f, 200f)
    coordinator.launchMelt(
      packageName = "org.example.app",
      originWindow = Offset.Unspecified,
      boundsWindow = bounds,
      reducedMotion = false,
    ) {
      navigated++
    }
    assertEquals("org.example.app", coordinator.activePackage)
    assertEquals(bounds.center, coordinator.tapOriginWindow)

    // Immediate duplicate tap on same package should be ignored
    coordinator.launchMelt(
      packageName = "org.example.app",
      originWindow = Offset(10f, 10f),
      boundsWindow = bounds,
      reducedMotion = false,
    ) {
      navigated++
    }
    advanceUntilIdle()
    assertEquals(1, navigated)
    assertNull(coordinator.activePackage)
  }
}

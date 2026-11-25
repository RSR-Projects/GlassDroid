package org.fdroid.ui.settings

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
import android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED
import android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED
import android.content.pm.PackageManager.DONT_KILL_APP
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlin.test.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AppIconManagerTest {

  @Test
  fun defaultIconDetectedWhenNoOverrideSet() {
    val pm = mockk<PackageManager>(relaxed = true)
    val appContext = mockk<Context>(relaxed = true)
    val context = mockk<Context>(relaxed = true)
    every { context.applicationContext } returns appContext
    every { appContext.packageName } returns "com.rsrprojects.glassdroid"
    every { context.packageManager } returns pm
    every { pm.getComponentEnabledSetting(any()) } returns COMPONENT_ENABLED_STATE_DEFAULT

    val manager = AppIconManager(context)
    assertEquals(AppIcon.Default, manager.currentAppIcon.value)
    verify(exactly = 0) { pm.setComponentEnabledSetting(any(), any(), any()) }
  }

  @Test
  fun setAppIconEnablesTargetAndDisablesOthers() {
    val pm = mockk<PackageManager>(relaxed = true)
    val appContext = mockk<Context>(relaxed = true)
    val context = mockk<Context>(relaxed = true)
    every { context.applicationContext } returns appContext
    every { appContext.packageName } returns "com.rsrprojects.glassdroid"
    every { context.packageManager } returns pm
    every { pm.getComponentEnabledSetting(any()) } returns COMPONENT_ENABLED_STATE_DEFAULT
    every { pm.setComponentEnabledSetting(any(), any(), any()) } just runs

    val manager = AppIconManager(context)
    manager.setAppIcon(AppIcon.Calculator)

    assertEquals(AppIcon.Calculator, manager.currentAppIcon.value)
    verify {
      pm.setComponentEnabledSetting(
        ComponentName("com.rsrprojects.glassdroid", "org.fdroid.IconCalculatorActivity"),
        COMPONENT_ENABLED_STATE_ENABLED,
        DONT_KILL_APP,
      )
      pm.setComponentEnabledSetting(
        ComponentName("com.rsrprojects.glassdroid", "org.fdroid.IconActivity"),
        COMPONENT_ENABLED_STATE_DISABLED,
        DONT_KILL_APP,
      )
    }
  }

  @Test
  fun setAppIconHandlesPackageManagerExceptionWithoutCrashing() {
    val pm = mockk<PackageManager>(relaxed = true)
    val appContext = mockk<Context>(relaxed = true)
    val context = mockk<Context>(relaxed = true)
    every { context.applicationContext } returns appContext
    every { appContext.packageName } returns "com.rsrprojects.glassdroid"
    every { context.packageManager } returns pm
    every { pm.getComponentEnabledSetting(any()) } throws IllegalArgumentException("missing component")
    every { pm.setComponentEnabledSetting(any(), any(), any()) } throws
      IllegalArgumentException("missing component")

    val manager = AppIconManager(context)
    assertEquals(AppIcon.Default, manager.currentAppIcon.value)
    manager.setAppIcon(AppIcon.Calculator)
    assertEquals(AppIcon.Default, manager.currentAppIcon.value)
  }
}

package org.fdroid.settings

import androidx.annotation.StringRes
import org.fdroid.R
import org.fdroid.database.AppListSortOrder
import org.fdroid.settings.SettingsConstants.AppUpdateValues
import org.fdroid.settings.SettingsConstants.MirrorChooserValues
import org.fdroid.settings.SettingsConstants.RepoUpdateValues

object SettingsConstants {

  const val PREF_KEY_LAST_UPDATE_CHECK = "lastUpdateCheck"
  const val PREF_DEFAULT_LAST_UPDATE_CHECK = -1L

  const val PREF_KEY_THEME = "theme"
  const val PREF_DEFAULT_THEME = "followSystem"

  const val PREF_KEY_DYNAMIC_COLORS = "dynamicColors"
  const val PREF_DEFAULT_DYNAMIC_COLORS = false

  enum class RepoUpdateValues(val workerEnabled: Boolean) {
    OnlyWifi(true),
    Always(true),
    OnlyWhenOpenApp(false),
    Never(false),
  }

  enum class AppUpdateValues(val workerEnabled: Boolean) {
    Immediately(false),
    OnlyWifi(true),
    Always(true),
    Never(false),
  }

  const val PREF_KEY_REPO_AUTO_UPDATES = "repoAutoUpdates"
  val PREF_DEFAULT_REPO_AUTO_UPDATES = RepoUpdateValues.OnlyWifi.name

  const val PREF_KEY_APP_AUTO_UPDATES = "appAutoUpdates"
  val PREF_DEFAULT_APP_AUTO_UPDATES = AppUpdateValues.OnlyWifi.name

  enum class MirrorChooserValues {
    Random {
      override val res = R.string.pref_mirror_chooser_summary_random
    },
    PreferForeign {
      override val res = R.string.pref_mirror_chooser_summary_prefer_foreign
    };

    @get:StringRes abstract val res: Int
  }

  const val PREF_KEY_MIRROR_CHOOSER = "mirrorChooser"
  val PREF_DEFAULT_MIRROR_CHOOSER = MirrorChooserValues.Random.name

  const val PREF_KEY_PROXY = "proxy"
  const val PREF_DEFAULT_PROXY = ""

  const val PREF_USE_DNS_CACHE = "useDnsCache"
  const val PREF_USE_DNS_CACHE_DEFAULT = false

  const val PREF_DNS_CACHE = "dnsCache"
  const val PREF_DNS_CACHE_DEFAULT = ""

  const val PREF_ERROR_CACHE = "errorCache"
  const val PREF_ERROR_CACHE_DEFAULT = ""

  const val PREF_KEY_PREVENT_SCREENSHOTS = "preventScreenshots"
  const val PREF_DEFAULT_PREVENT_SCREENSHOTS = false

  const val PREF_KEY_WARN_WHEN_METERED = "warnWhenMetered"
  const val PREF_DEFAULT_WARN_WHEN_METERED = true

  const val PREF_KEY_SHOW_SEARCH_KEYBOARD = "showSearchKeyboard"
  const val PREF_DEFAULT_SHOW_SEARCH_KEYBOARD = false

  const val PREF_KEY_SHOW_INCOMPATIBLE = "incompatibleVersions"
  const val PREF_DEFAULT_SHOW_INCOMPATIBLE = true

  const val PREF_KEY_APP_LIST_SORT_ORDER = "appListSortOrder"
  const val PREF_DEFAULT_APP_LIST_SORT_ORDER = "lastUpdated"

  const val PREF_KEY_PRIVILEGED_EXTENSION_NEEDS_MIGRATION = "privilegedExtensionNeedsMigration"
  const val PREF_DEFAULT_PRIVILEGED_EXTENSION_NEEDS_MIGRATION = true

  fun getAppListSortOrder(s: String?) =
    when (s) {
      "name" -> AppListSortOrder.NAME
      else -> AppListSortOrder.LAST_UPDATED
    }

  fun AppListSortOrder.toSettings() =
    when (this) {
      AppListSortOrder.LAST_UPDATED -> "lastUpdated"
      AppListSortOrder.NAME -> "name"
    }

  const val PREF_KEY_MY_APPS_SORT_ORDER = "myAppsSortOrder"
  const val PREF_DEFAULT_MY_APPS_SORT_ORDER = "name"

  const val PREF_KEY_IGNORED_APP_ISSUES = "ignoredAppIssues"

  const val PREF_KEY_INSTALL_HISTORY = "keepInstallHistory"
  const val PREF_DEFAULT_INSTALL_HISTORY = false

  const val PREF_KEY_LAST_DB_REPAIR_CHECK = "lastDbRepairCheck"
  const val PREF_DEFAULT_LAST_DB_REPAIR_CHECK = -1L

  const val PREF_KEY_GLASS_PROFILE = "liquidGlassProfile"
  const val PREF_KEY_GLASS_TINT_COLOR = "liquidGlassTintColor"
  const val PREF_KEY_GLASS_BLUR = "liquidGlassBlur"
  const val PREF_KEY_GLASS_TRANSPARENCY = "liquidGlassTransparency"
  const val PREF_KEY_GLASS_REFRACTION_HEIGHT = "liquidGlassRefractionHeight"
  const val PREF_KEY_GLASS_REFRACTION_AMOUNT = "liquidGlassRefractionAmount"
  const val PREF_KEY_GLASS_TINT = "liquidGlassTint"
  const val PREF_KEY_GLASS_HIGHLIGHT = "liquidGlassHighlight"
  const val PREF_KEY_GLASS_VIBRANCY = "liquidGlassVibrancy"
  const val PREF_KEY_GLASS_BRIGHTNESS = "liquidGlassBrightness"
  const val PREF_KEY_GLASS_CHROMATIC = "liquidGlassChromatic"
  const val PREF_KEY_GLASS_REDUCED_FX = "liquidGlassReducedFx"
  const val PREF_KEY_MOTION_PROFILE = "liquidMotionProfile"
  const val PREF_KEY_MOTION_ENABLED = "liquidMotionEnabled"
  const val PREF_KEY_MOTION_INTENSITY = "liquidMotionIntensity"
  const val PREF_KEY_MOTION_BOUNCE = "liquidMotionBounce"
  const val PREF_KEY_MOTION_SPEED = "liquidMotionSpeed"
  const val PREF_KEY_MOTION_PRESS_DEPTH = "liquidMotionPressDepth"
  const val PREF_KEY_MOTION_RELEASE_JIGGLE = "liquidMotionReleaseJiggle"
  const val PREF_KEY_MOTION_DRAG_STRETCH = "liquidMotionDragStretch"
  const val PREF_KEY_MOTION_LONG_PRESS_BLOOM = "liquidMotionLongPressBloom"
}

fun String?.toRepoUpdateValue() =
  try {
    if (this == null) RepoUpdateValues.OnlyWifi else RepoUpdateValues.valueOf(this)
  } catch (_: IllegalArgumentException) {
    RepoUpdateValues.OnlyWifi
  }

fun String?.toAppUpdateValue() =
  try {
    if (this == null) AppUpdateValues.OnlyWifi else AppUpdateValues.valueOf(this)
  } catch (_: IllegalArgumentException) {
    AppUpdateValues.OnlyWifi
  }

fun String?.toMirrorChooserValue() =
  try {
    if (this == null) MirrorChooserValues.Random else MirrorChooserValues.valueOf(this)
  } catch (_: IllegalArgumentException) {
    MirrorChooserValues.Random
  }

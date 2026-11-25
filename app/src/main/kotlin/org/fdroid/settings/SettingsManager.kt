package org.fdroid.settings

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.client.engine.ProxyConfig
import java.net.InetSocketAddress
import java.net.Proxy
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import me.zhanghai.compose.preference.createPreferenceFlow
import me.zhanghai.compose.preference.isDefaultPreferenceFlowAndroidLongSupportEnabled
import mu.KotlinLogging
import org.fdroid.database.AppListSortOrder
import org.fdroid.settings.SettingsConstants.AppUpdateValues
import org.fdroid.settings.SettingsConstants.MirrorChooserValues
import org.fdroid.settings.SettingsConstants.PREF_DEFAULT_APP_AUTO_UPDATES
import org.fdroid.settings.SettingsConstants.PREF_DEFAULT_APP_LIST_SORT_ORDER
import org.fdroid.settings.SettingsConstants.PREF_DEFAULT_DYNAMIC_COLORS
import org.fdroid.settings.SettingsConstants.PREF_DEFAULT_INSTALL_HISTORY
import org.fdroid.settings.SettingsConstants.PREF_DEFAULT_LAST_DB_REPAIR_CHECK
import org.fdroid.settings.SettingsConstants.PREF_DEFAULT_LAST_UPDATE_CHECK
import org.fdroid.settings.SettingsConstants.PREF_DEFAULT_MIRROR_CHOOSER
import org.fdroid.settings.SettingsConstants.PREF_DEFAULT_MY_APPS_SORT_ORDER
import org.fdroid.settings.SettingsConstants.PREF_DEFAULT_PREVENT_SCREENSHOTS
import org.fdroid.settings.SettingsConstants.PREF_DEFAULT_PRIVILEGED_EXTENSION_NEEDS_MIGRATION
import org.fdroid.settings.SettingsConstants.PREF_DEFAULT_PROXY
import org.fdroid.settings.SettingsConstants.PREF_DEFAULT_REPO_AUTO_UPDATES
import org.fdroid.settings.SettingsConstants.PREF_DEFAULT_SHOW_INCOMPATIBLE
import org.fdroid.settings.SettingsConstants.PREF_DEFAULT_SHOW_SEARCH_KEYBOARD
import org.fdroid.settings.SettingsConstants.PREF_DEFAULT_THEME
import org.fdroid.settings.SettingsConstants.PREF_DEFAULT_WARN_WHEN_METERED
import org.fdroid.settings.SettingsConstants.PREF_DNS_CACHE
import org.fdroid.settings.SettingsConstants.PREF_DNS_CACHE_DEFAULT
import org.fdroid.settings.SettingsConstants.PREF_ERROR_CACHE
import org.fdroid.settings.SettingsConstants.PREF_ERROR_CACHE_DEFAULT
import org.fdroid.settings.SettingsConstants.PREF_KEY_APP_AUTO_UPDATES
import org.fdroid.settings.SettingsConstants.PREF_KEY_APP_LIST_SORT_ORDER
import org.fdroid.settings.SettingsConstants.PREF_KEY_DYNAMIC_COLORS
import org.fdroid.settings.SettingsConstants.PREF_KEY_GLASS_BLUR
import org.fdroid.settings.SettingsConstants.PREF_KEY_GLASS_BRIGHTNESS
import org.fdroid.settings.SettingsConstants.PREF_KEY_GLASS_CHROMATIC
import org.fdroid.settings.SettingsConstants.PREF_KEY_GLASS_HIGHLIGHT
import org.fdroid.settings.SettingsConstants.PREF_KEY_GLASS_PROFILE
import org.fdroid.settings.SettingsConstants.PREF_KEY_GLASS_REDUCED_FX
import org.fdroid.settings.SettingsConstants.PREF_KEY_GLASS_REFRACTION_AMOUNT
import org.fdroid.settings.SettingsConstants.PREF_KEY_GLASS_REFRACTION_HEIGHT
import org.fdroid.settings.SettingsConstants.PREF_KEY_GLASS_TINT
import org.fdroid.settings.SettingsConstants.PREF_KEY_GLASS_TINT_COLOR
import org.fdroid.settings.SettingsConstants.PREF_KEY_GLASS_TRANSPARENCY
import org.fdroid.settings.SettingsConstants.PREF_KEY_GLASS_VIBRANCY
import org.fdroid.settings.SettingsConstants.PREF_KEY_IGNORED_APP_ISSUES
import org.fdroid.settings.SettingsConstants.PREF_KEY_INSTALL_HISTORY
import org.fdroid.settings.SettingsConstants.PREF_KEY_LAST_DB_REPAIR_CHECK
import org.fdroid.settings.SettingsConstants.PREF_KEY_LAST_UPDATE_CHECK
import org.fdroid.settings.SettingsConstants.PREF_KEY_MIRROR_CHOOSER
import org.fdroid.settings.SettingsConstants.PREF_KEY_MOTION_BOUNCE
import org.fdroid.settings.SettingsConstants.PREF_KEY_MOTION_DRAG_STRETCH
import org.fdroid.settings.SettingsConstants.PREF_KEY_MOTION_ENABLED
import org.fdroid.settings.SettingsConstants.PREF_KEY_MOTION_INTENSITY
import org.fdroid.settings.SettingsConstants.PREF_KEY_MOTION_LONG_PRESS_BLOOM
import org.fdroid.settings.SettingsConstants.PREF_KEY_MOTION_PRESS_DEPTH
import org.fdroid.settings.SettingsConstants.PREF_KEY_MOTION_PROFILE
import org.fdroid.settings.SettingsConstants.PREF_KEY_MOTION_RELEASE_JIGGLE
import org.fdroid.settings.SettingsConstants.PREF_KEY_MOTION_SPEED
import org.fdroid.settings.SettingsConstants.PREF_KEY_MY_APPS_SORT_ORDER
import org.fdroid.settings.SettingsConstants.PREF_KEY_PREVENT_SCREENSHOTS
import org.fdroid.settings.SettingsConstants.PREF_KEY_PRIVILEGED_EXTENSION_NEEDS_MIGRATION
import org.fdroid.settings.SettingsConstants.PREF_KEY_PROXY
import org.fdroid.settings.SettingsConstants.PREF_KEY_REPO_AUTO_UPDATES
import org.fdroid.settings.SettingsConstants.PREF_KEY_SHOW_INCOMPATIBLE
import org.fdroid.settings.SettingsConstants.PREF_KEY_SHOW_SEARCH_KEYBOARD
import org.fdroid.settings.SettingsConstants.PREF_KEY_THEME
import org.fdroid.settings.SettingsConstants.PREF_KEY_WARN_WHEN_METERED
import org.fdroid.settings.SettingsConstants.PREF_USE_DNS_CACHE
import org.fdroid.settings.SettingsConstants.PREF_USE_DNS_CACHE_DEFAULT
import org.fdroid.settings.SettingsConstants.RepoUpdateValues
import org.fdroid.settings.SettingsConstants.getAppListSortOrder
import org.fdroid.settings.SettingsConstants.toSettings
import org.fdroid.ui.glass.GlassProfile
import org.fdroid.ui.glass.GlassTintPreset
import org.fdroid.ui.glass.LiquidGlassConfig
import org.fdroid.ui.glass.MotionProfile

@Singleton
class SettingsManager @Inject constructor(@param:ApplicationContext private val context: Context) {

  private val log = KotlinLogging.logger {}

  val prefs: SharedPreferences by lazy {
    context.getSharedPreferences("${context.packageName}_preferences", MODE_PRIVATE)
  }

  /** This is mutable, so the settings UI can make changes to it. */
  val prefsFlow by lazy {
    isDefaultPreferenceFlowAndroidLongSupportEnabled = true
    createPreferenceFlow(prefs)
  }
  val theme
    get() = prefs.getString(PREF_KEY_THEME, PREF_DEFAULT_THEME)!!

  val themeFlow = prefsFlow.map { it.get<String>(PREF_KEY_THEME) }.distinctUntilChanged()
  val dynamicColorFlow: Flow<Boolean> =
    prefsFlow
      .map { it.get<Boolean>(PREF_KEY_DYNAMIC_COLORS) ?: PREF_DEFAULT_DYNAMIC_COLORS }
      .distinctUntilChanged()
  val repoUpdates
    get() =
      prefs
        .getString(PREF_KEY_REPO_AUTO_UPDATES, PREF_DEFAULT_REPO_AUTO_UPDATES)
        .toRepoUpdateValue()

  val repoUpdatesFlow
    get() =
      prefsFlow
        .map { it.get<String>(PREF_KEY_REPO_AUTO_UPDATES).toRepoUpdateValue() }
        .distinctUntilChanged()

  val autoUpdateApps
    get() =
      prefs.getString(PREF_KEY_APP_AUTO_UPDATES, PREF_DEFAULT_APP_AUTO_UPDATES).toAppUpdateValue()

  val autoUpdateAppsFlow
    get() =
      prefsFlow
        .map { it.get<String>(PREF_KEY_APP_AUTO_UPDATES).toAppUpdateValue() }
        .distinctUntilChanged()

  var lastRepoUpdate: Long
    get() =
      try {
        val t = prefs.getLong(PREF_KEY_LAST_UPDATE_CHECK, PREF_DEFAULT_LAST_UPDATE_CHECK)
        // TODO for the stable release of 2.0 we can remove the migration code below
        if (t == PREF_DEFAULT_LAST_UPDATE_CHECK) {
          prefs.getLong("lastRepoUpdateCheck", PREF_DEFAULT_LAST_UPDATE_CHECK)
        } else t
      } catch (_: Exception) {
        PREF_DEFAULT_LAST_UPDATE_CHECK
      }
    set(value) {
      prefs.edit { putLong(PREF_KEY_LAST_UPDATE_CHECK, value) }
      _lastRepoUpdateFlow.update { value }
    }

  var lastDbRepairCheck: Long
    get() = prefs.getLong(PREF_KEY_LAST_DB_REPAIR_CHECK, PREF_DEFAULT_LAST_DB_REPAIR_CHECK)
    set(value) {
      prefs.edit { putLong(PREF_KEY_LAST_DB_REPAIR_CHECK, value) }
    }

  var useInstallHistory: Boolean
    get() = prefs.getBoolean(PREF_KEY_INSTALL_HISTORY, PREF_DEFAULT_INSTALL_HISTORY)
    set(value) {
      prefs.edit { putBoolean(PREF_KEY_INSTALL_HISTORY, value) }
      _useInstallHistoryFlow.update { value }
    }

  private val _useInstallHistoryFlow = MutableStateFlow(useInstallHistory)
  val useInstallHistoryFlow = _useInstallHistoryFlow.asStateFlow()

  private val _lastRepoUpdateFlow = MutableStateFlow(lastRepoUpdate)
  val lastRepoUpdateFlow = _lastRepoUpdateFlow.asStateFlow()
  val isFirstStart
    get() = lastRepoUpdate <= PREF_DEFAULT_LAST_UPDATE_CHECK

  /** A set of package name for which we should not show app issues. */
  var ignoredAppIssues: Map<String, Long>
    get() =
      try {
        prefs.getStringSet(PREF_KEY_IGNORED_APP_ISSUES, emptySet<String>())?.associate {
          val (packageName, versionCode) = it.split('|')
          Pair(packageName, versionCode.toLong())
        } ?: emptyMap()
      } catch (e: Exception) {
        log.error(e) { "Error parsing ignored app issues: " }
        emptyMap()
      }
    private set(value) {
      val newValue = value.map { (packageName, versionCode) -> "$packageName|$versionCode" }
      prefs.edit { putStringSet(PREF_KEY_IGNORED_APP_ISSUES, newValue.toSet()) }
    }

  val mirrorChooser
    get() =
      prefs.getString(PREF_KEY_MIRROR_CHOOSER, PREF_DEFAULT_MIRROR_CHOOSER).toMirrorChooserValue()

  val proxyConfig: ProxyConfig?
    get() {
      val proxyStr = prefs.getString(PREF_KEY_PROXY, PREF_DEFAULT_PROXY)
      return if (proxyStr.isNullOrBlank()) null
      else {
        val (host, port) = proxyStr.split(':')
        // don't resolve hostname here, or we get NetworkOnMainThreadException
        val address = InetSocketAddress.createUnresolved(host, port.toInt())
        Proxy(Proxy.Type.SOCKS, address)
      }
    }

  val preventScreenshotsFlow
    get() =
      prefsFlow
        .map { it.get<Boolean>(PREF_KEY_PREVENT_SCREENSHOTS) ?: PREF_DEFAULT_PREVENT_SCREENSHOTS }
        .distinctUntilChanged()

  var useDnsCache: Boolean
    get() {
      return prefs.getBoolean(PREF_USE_DNS_CACHE, PREF_USE_DNS_CACHE_DEFAULT)
    }
    set(value) {
      return prefs.edit { putBoolean(PREF_USE_DNS_CACHE, value) }
    }

  var dnsCache: String
    get() {
      return prefs.getString(PREF_DNS_CACHE, null) ?: PREF_DNS_CACHE_DEFAULT
    }
    set(value) {
      return prefs.edit { putString(PREF_DNS_CACHE, value) }
    }

  var errorCache: String
    get() {
      return prefs.getString(PREF_ERROR_CACHE, null) ?: PREF_ERROR_CACHE_DEFAULT
    }
    set(value) {
      return prefs.edit { putString(PREF_ERROR_CACHE, value) }
    }

  private val _warnWhenMeteredFlow =
    MutableStateFlow(prefs.getBoolean(PREF_KEY_WARN_WHEN_METERED, PREF_DEFAULT_WARN_WHEN_METERED))
  val warnWhenMeteredFlow = _warnWhenMeteredFlow.asStateFlow()

  private val _showSearchKeyboardFlow =
    MutableStateFlow(
      prefs.getBoolean(PREF_KEY_SHOW_SEARCH_KEYBOARD, PREF_DEFAULT_SHOW_SEARCH_KEYBOARD)
    )
  val showSearchKeyboardFlow = _showSearchKeyboardFlow.asStateFlow()

  val filterIncompatible: Boolean
    get() = !prefs.getBoolean(PREF_KEY_SHOW_INCOMPATIBLE, PREF_DEFAULT_SHOW_INCOMPATIBLE)

  val appListSortOrder: AppListSortOrder
    get() {
      val s = prefs.getString(PREF_KEY_APP_LIST_SORT_ORDER, PREF_DEFAULT_APP_LIST_SORT_ORDER)
      return getAppListSortOrder(s)
    }

  var myAppsSortOrder: AppListSortOrder
    get() {
      val s = prefs.getString(PREF_KEY_MY_APPS_SORT_ORDER, PREF_DEFAULT_MY_APPS_SORT_ORDER)
      return getAppListSortOrder(s)
    }
    set(value) {
      prefs.edit { putString(PREF_KEY_MY_APPS_SORT_ORDER, value.toSettings()) }
    }

  var privilegedExtensionNeedsMigration: Boolean
    get() {
      return prefs.getBoolean(
        PREF_KEY_PRIVILEGED_EXTENSION_NEEDS_MIGRATION,
        PREF_DEFAULT_PRIVILEGED_EXTENSION_NEEDS_MIGRATION,
      )
    }
    set(value) {
      return prefs.edit { putBoolean(PREF_KEY_PRIVILEGED_EXTENSION_NEEDS_MIGRATION, value) }
    }

  private val _liquidGlassConfigFlow by lazy { MutableStateFlow(loadLiquidGlassConfig()) }
  val liquidGlassConfigFlow: StateFlow<LiquidGlassConfig>
    get() = _liquidGlassConfigFlow.asStateFlow()

  private fun loadLiquidGlassConfig(): LiquidGlassConfig {
    val defaults = LiquidGlassConfig()
    val glassProfile =
      GlassProfile.fromPrefString(prefs.getString(PREF_KEY_GLASS_PROFILE, defaults.glassProfile.name))
        ?: defaults.glassProfile
    val glassTint =
      GlassTintPreset.fromPrefString(prefs.getString(PREF_KEY_GLASS_TINT_COLOR, defaults.glassTint.name))
    val motionProfile =
      prefs.getString(PREF_KEY_MOTION_PROFILE, defaults.motionProfile.name)?.let {
        runCatching { MotionProfile.valueOf(it) }.getOrNull()
      } ?: defaults.motionProfile
    return LiquidGlassConfig(
      glassProfile = glassProfile,
      glassTint = glassTint,
      blurScale = prefs.getFloat(PREF_KEY_GLASS_BLUR, defaults.blurScale),
      transparency = prefs.getFloat(PREF_KEY_GLASS_TRANSPARENCY, defaults.transparency),
      refractionHeightScale =
        prefs.getFloat(PREF_KEY_GLASS_REFRACTION_HEIGHT, defaults.refractionHeightScale),
      refractionAmountScale =
        prefs.getFloat(PREF_KEY_GLASS_REFRACTION_AMOUNT, defaults.refractionAmountScale),
      tintStrength = prefs.getFloat(PREF_KEY_GLASS_TINT, defaults.tintStrength),
      highlightIntensity = prefs.getFloat(PREF_KEY_GLASS_HIGHLIGHT, defaults.highlightIntensity),
      vibrancyScale = prefs.getFloat(PREF_KEY_GLASS_VIBRANCY, defaults.vibrancyScale),
      brightness = prefs.getFloat(PREF_KEY_GLASS_BRIGHTNESS, defaults.brightness),
      chromaticAberration =
        prefs.getBoolean(PREF_KEY_GLASS_CHROMATIC, defaults.chromaticAberration),
      reducedVisualEffects =
        prefs.getBoolean(PREF_KEY_GLASS_REDUCED_FX, defaults.reducedVisualEffects),
      motionProfile = motionProfile,
      animationsEnabled = prefs.getBoolean(PREF_KEY_MOTION_ENABLED, defaults.animationsEnabled),
      animationIntensity = prefs.getFloat(PREF_KEY_MOTION_INTENSITY, defaults.animationIntensity),
      bounce = prefs.getFloat(PREF_KEY_MOTION_BOUNCE, defaults.bounce),
      speed = prefs.getFloat(PREF_KEY_MOTION_SPEED, defaults.speed),
      pressDepth = prefs.getFloat(PREF_KEY_MOTION_PRESS_DEPTH, defaults.pressDepth),
      releaseJiggle = prefs.getFloat(PREF_KEY_MOTION_RELEASE_JIGGLE, defaults.releaseJiggle),
      dragStretch = prefs.getFloat(PREF_KEY_MOTION_DRAG_STRETCH, defaults.dragStretch),
      longPressBloom =
        prefs.getFloat(PREF_KEY_MOTION_LONG_PRESS_BLOOM, defaults.longPressBloom),
    )
  }

  fun updateLiquidGlassConfig(config: LiquidGlassConfig) {
    _liquidGlassConfigFlow.value = config
    prefs.edit {
      putString(PREF_KEY_GLASS_PROFILE, config.glassProfile.name)
      putString(PREF_KEY_GLASS_TINT_COLOR, config.glassTint.name)
      putFloat(PREF_KEY_GLASS_BLUR, config.blurScale)
      putFloat(PREF_KEY_GLASS_TRANSPARENCY, config.transparency)
      putFloat(PREF_KEY_GLASS_REFRACTION_HEIGHT, config.refractionHeightScale)
      putFloat(PREF_KEY_GLASS_REFRACTION_AMOUNT, config.refractionAmountScale)
      putFloat(PREF_KEY_GLASS_TINT, config.tintStrength)
      putFloat(PREF_KEY_GLASS_HIGHLIGHT, config.highlightIntensity)
      putFloat(PREF_KEY_GLASS_VIBRANCY, config.vibrancyScale)
      putFloat(PREF_KEY_GLASS_BRIGHTNESS, config.brightness)
      putBoolean(PREF_KEY_GLASS_CHROMATIC, config.chromaticAberration)
      putBoolean(PREF_KEY_GLASS_REDUCED_FX, config.reducedVisualEffects)
      putString(PREF_KEY_MOTION_PROFILE, config.motionProfile.name)
      putBoolean(PREF_KEY_MOTION_ENABLED, config.animationsEnabled)
      putFloat(PREF_KEY_MOTION_INTENSITY, config.animationIntensity)
      putFloat(PREF_KEY_MOTION_BOUNCE, config.bounce)
      putFloat(PREF_KEY_MOTION_SPEED, config.speed)
      putFloat(PREF_KEY_MOTION_PRESS_DEPTH, config.pressDepth)
      putFloat(PREF_KEY_MOTION_RELEASE_JIGGLE, config.releaseJiggle)
      putFloat(PREF_KEY_MOTION_DRAG_STRETCH, config.dragStretch)
      putFloat(PREF_KEY_MOTION_LONG_PRESS_BLOOM, config.longPressBloom)
    }
  }

  fun resetGlassConfig(): LiquidGlassConfig {
    val updated = _liquidGlassConfigFlow.value.resetGlass()
    updateLiquidGlassConfig(updated)
    return updated
  }

  fun resetMotionConfig(): LiquidGlassConfig {
    val updated = _liquidGlassConfigFlow.value.resetMotion()
    updateLiquidGlassConfig(updated)
    return updated
  }

  init {
    // update settings migration from 1.x can be removed after sufficient time has passed
    try {
      val updateAutoDownload = prefs.getBoolean("updateAutoDownload", true)
      val never = 0
      val manual = 1
      val always = 2
      val unset = 42
      val overWifi = prefs.getInt("overWifi", unset)
      val overData = prefs.getInt("overData", unset)
      // only do migration, if old settings still exist
      if (overWifi < unset || overData < unset) {
        log.info { "Migrating updateAutoDownload ($updateAutoDownload) to autoUpdates setting" }
        log.info { "  overWifi: $overWifi" }
        log.info { "  overData: $overData" }
        // update flow: UI will update and settings will auto-persist to disk
        prefsFlow.update {
          it.toMutablePreferences().apply {
            this[PREF_KEY_REPO_AUTO_UPDATES] =
              when {
                overWifi == never && overData == never -> RepoUpdateValues.Never.name
                overWifi == always && overData == always -> RepoUpdateValues.Always.name
                overWifi == always -> RepoUpdateValues.OnlyWifi.name
                overWifi != never || overData != never -> RepoUpdateValues.OnlyWhenOpenApp.name
                else -> RepoUpdateValues.Never.name
              }
            // OnlyWhenOpenApp doesn't exist for package updates
            this[PREF_KEY_APP_AUTO_UPDATES] =
              when {
                updateAutoDownload && (overWifi != never || overData != never) ->
                  when {
                    overWifi != never && overData != always -> AppUpdateValues.OnlyWifi.name
                    else -> AppUpdateValues.Always.name
                  }
                overWifi == always && overData == manual -> AppUpdateValues.OnlyWifi.name
                else -> AppUpdateValues.Never.name
              }
            if (overData == always) this[PREF_KEY_WARN_WHEN_METERED] = false
            remove("updateAutoDownload")
            remove("overWifi")
            remove("overData")
          }
        }
      }
    } catch (e: Exception) {
      log.error(e) { "Error migrating update settings" }
    }
    // foreign mirror migration from 1.x can be removed after sufficient time has passed
    try {
      if (prefs.getBoolean("preferForeign", false)) {
        log.info { "Migrating preferForeign to mirror chooser prefer foreign" }
        // update flow: UI will update and settings will auto-persist to disk
        prefsFlow.update {
          it.toMutablePreferences().apply {
            this[PREF_KEY_MIRROR_CHOOSER] = MirrorChooserValues.PreferForeign.name
            remove("preferForeign")
          }
        }
      }
    } catch (e: Exception) {
      log.error(e) { "Error migrating preferForeign settings" }
    }
    // proxy migration from 1.x can be removed after sufficient time has passed
    try {
      if (prefs.getBoolean("useTor", false)) {
        log.info { "Migrating useTor to proxy setting" }
        // update flow: UI will update and settings will auto-persist to disk
        prefsFlow.update {
          it.toMutablePreferences().apply {
            this[PREF_KEY_PROXY] = "127.0.0.1:9050"
            remove("useTor")
          }
        }
      }
      val proxyHost = prefs.getString("proxyHost", null)
      val proxyPort = prefs.getString("proxyPort", null)?.toIntOrNull()
      if (proxyHost != null && proxyPort != null && proxyPort in 1..65535) {
        log.info { "Migrating proxy settings to $proxyHost:$proxyPort" }
        // update flow: UI will update and settings will auto-persist to disk
        prefsFlow.update {
          it.toMutablePreferences().apply {
            this[PREF_KEY_PROXY] = "$proxyHost:$proxyPort"
            remove("proxyHost")
            remove("proxyPort")
          }
        }
      }
    } catch (e: Exception) {
      log.error(e) { "Error migrating proxy settings" }
    }
  }

  fun onDontWarnOnMeteredNetwork() {
    prefs.edit { putBoolean(PREF_KEY_WARN_WHEN_METERED, false) }
    _warnWhenMeteredFlow.update { false }
  }

  fun setShowSearchKeyboard(show: Boolean) {
    prefs.edit { putBoolean(PREF_KEY_SHOW_SEARCH_KEYBOARD, show) }
    _showSearchKeyboardFlow.update { show }
  }

  fun saveAppListFilter(sortOrder: AppListSortOrder, filterIncompatible: Boolean) {
    prefs.edit {
      putBoolean(PREF_KEY_SHOW_INCOMPATIBLE, !filterIncompatible)
      putString(PREF_KEY_APP_LIST_SORT_ORDER, sortOrder.toSettings())
    }
  }

  fun ignoreAppIssue(packageName: String, versionCode: Long) {
    val newMap = ignoredAppIssues.toMutableMap().apply { put(packageName, versionCode) }
    ignoredAppIssues = newMap
  }
}

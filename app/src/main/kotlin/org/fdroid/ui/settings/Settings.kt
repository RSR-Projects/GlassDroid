package org.fdroid.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Build.VERSION.SDK_INT
import android.provider.Settings.ACTION_APP_LOCALE_SETTINGS
import android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS
import android.provider.Settings.EXTRA_APP_PACKAGE
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Screenshot
import androidx.compose.material.icons.filled.SystemSecurityUpdate
import androidx.compose.material.icons.filled.SystemSecurityUpdateWarning
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.filled.UpdateDisabled
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.lang.System.currentTimeMillis
import java.util.concurrent.TimeUnit.HOURS
import kotlinx.coroutines.flow.MutableStateFlow
import me.zhanghai.compose.preference.MapPreferences
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import me.zhanghai.compose.preference.rememberPreferenceState
import org.fdroid.R
import org.fdroid.settings.SettingsConstants.AppUpdateValues
import org.fdroid.settings.SettingsConstants.MirrorChooserValues
import org.fdroid.settings.SettingsConstants.PREF_DEFAULT_APP_AUTO_UPDATES
import org.fdroid.settings.SettingsConstants.PREF_DEFAULT_DYNAMIC_COLORS
import org.fdroid.settings.SettingsConstants.PREF_DEFAULT_MIRROR_CHOOSER
import org.fdroid.settings.SettingsConstants.PREF_DEFAULT_PREVENT_SCREENSHOTS
import org.fdroid.settings.SettingsConstants.PREF_DEFAULT_PROXY
import org.fdroid.settings.SettingsConstants.PREF_DEFAULT_REPO_AUTO_UPDATES
import org.fdroid.settings.SettingsConstants.PREF_DEFAULT_THEME
import org.fdroid.settings.SettingsConstants.PREF_KEY_APP_AUTO_UPDATES
import org.fdroid.settings.SettingsConstants.PREF_KEY_DYNAMIC_COLORS
import org.fdroid.settings.SettingsConstants.PREF_KEY_MIRROR_CHOOSER
import org.fdroid.settings.SettingsConstants.PREF_KEY_PREVENT_SCREENSHOTS
import org.fdroid.settings.SettingsConstants.PREF_KEY_PROXY
import org.fdroid.settings.SettingsConstants.PREF_KEY_REPO_AUTO_UPDATES
import org.fdroid.settings.SettingsConstants.PREF_KEY_THEME
import org.fdroid.settings.SettingsConstants.PREF_USE_DNS_CACHE
import org.fdroid.settings.SettingsConstants.PREF_USE_DNS_CACHE_DEFAULT
import org.fdroid.settings.SettingsConstants.RepoUpdateValues
import org.fdroid.settings.toAppUpdateValue
import org.fdroid.settings.toMirrorChooserValue
import org.fdroid.settings.toRepoUpdateValue
import org.fdroid.ui.FDroidContent
import org.fdroid.ui.glass.LiquidFloatingTopBar
import org.fdroid.ui.glass.LiquidGlassDialog
import org.fdroid.ui.glass.LiquidGlassDivider
import org.fdroid.ui.glass.LiquidGlassRadioButton
import org.fdroid.ui.glass.LiquidGlassShapes
import org.fdroid.ui.glass.LiquidGlassSwitch
import org.fdroid.ui.glass.LiquidGlassTextButton
import org.fdroid.ui.glass.LiquidScaffold
import org.fdroid.ui.glass.LiquidToastType
import org.fdroid.ui.glass.LocalLiquidToastHost
import org.fdroid.ui.glass.liquidRowSquish
import org.fdroid.ui.utils.BackButton
import org.fdroid.ui.utils.asRelativeTimeString
import org.fdroid.ui.utils.launchSafe
import org.fdroid.ui.utils.startActivitySafe
import org.fdroid.utils.getLogName

@Composable
fun Settings(
  model: SettingsModel,
  onChangeAppIcon: (AppIcon) -> Unit,
  onSaveLogcat: (Uri?) -> Unit,
  onOpenLiquidGlassCustomizer: () -> Unit = {},
  onBackClicked: () -> Unit,
) {
  LiquidScaffold(
    topBar = {
      LiquidFloatingTopBar(
        navigationIcon = { BackButton(onClick = onBackClicked) },
        title = { Text(stringResource(R.string.menu_settings), maxLines = 1) },
        showActions = false,
      )
    }
  ) { paddingValues ->
    val launcher =
      rememberLauncherForActivityResult(CreateDocument("text/plain")) { onSaveLogcat(it) }
    val context = LocalContext.current
    val res = LocalResources.current
    val toastHost = LocalLiquidToastHost.current
    ProvidePreferenceLocals(model.prefsFlow) {
      val showProxyError = remember { mutableStateOf(false) }
      val proxyState = rememberPreferenceState(PREF_KEY_PROXY, PREF_DEFAULT_PROXY)
      LazyColumn(contentPadding = paddingValues, modifier = Modifier.fillMaxSize()) {
        liquidPreferenceCategory(
          key = "pref_category_display",
          title = { Text(stringResource(R.string.display)) },
        )
        liquidActionPreference(
          key = "liquid_glass_customizer",
          icon = {
            Icon(
              imageVector = Icons.Default.AutoAwesome,
              contentDescription = null,
              modifier = Modifier.semantics { hideFromAccessibility() },
            )
          },
          title = { Text(stringResource(R.string.liquid_glass_title)) },
          summary = { Text(stringResource(R.string.liquid_glass_summary)) },
          onClick = onOpenLiquidGlassCustomizer,
        )
        val themeToString = { value: String ->
          when (value) {
            "light" -> res.getString(R.string.theme_light)
            "night", // night was added because we had a crash with that
            "dark" -> res.getString(R.string.theme_dark)
            "followSystem" -> res.getString(R.string.theme_follow_system)
            else -> error("Unknown value: $value")
          }
        }
        liquidListPreference(
          key = PREF_KEY_THEME,
          values = listOf("light", "dark", "followSystem"),
          valueToText = themeToString,
          defaultValue = PREF_DEFAULT_THEME,
          title = { Text(text = stringResource(R.string.theme)) },
          icon = {
            Icon(
              imageVector = Icons.Default.BrightnessMedium,
              contentDescription = null,
              modifier = Modifier.semantics { hideFromAccessibility() },
            )
          },
          summary = { Text(text = themeToString(it)) },
        )
        if (SDK_INT >= 31)
          liquidSwitchPreference(
            key = PREF_KEY_DYNAMIC_COLORS,
            defaultValue = PREF_DEFAULT_DYNAMIC_COLORS,
            title = { Text(stringResource(R.string.pref_dyn_colors_title)) },
            icon = {
              Icon(
                imageVector = Icons.Default.Palette,
                contentDescription = null,
                modifier = Modifier.semantics { hideFromAccessibility() },
              )
            },
            summary = { Text(stringResource(R.string.pref_dyn_colors_summary)) },
          )
        if (SDK_INT >= 33)
          liquidActionPreference(
            key = "languages",
            icon = {
              Icon(
                imageVector = Icons.Default.Translate,
                contentDescription = null,
                modifier = Modifier.semantics { hideFromAccessibility() },
              )
            },
            title = { Text(stringResource(R.string.pref_language)) },
            summary = { Text(stringResource(R.string.pref_language_summary)) },
            onClick = {
              val intent =
                Intent(ACTION_APP_LOCALE_SETTINGS).apply {
                  setData(Uri.fromParts("package", context.packageName, null))
                }
              context.startActivitySafe(intent)
            },
          )
        if (SDK_INT >= 26)
          liquidActionPreference(
            key = "notifications",
            icon = {
              Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = null,
                modifier = Modifier.semantics { hideFromAccessibility() },
              )
            },
            title = { Text(stringResource(R.string.notification_title)) },
            summary = { Text(stringResource(R.string.notification_summary)) },
            onClick = {
              val intent =
                Intent(ACTION_APP_NOTIFICATION_SETTINGS).apply {
                  putExtra(EXTRA_APP_PACKAGE, context.packageName)
                }
              context.startActivitySafe(intent)
            },
          )
        liquidPreferenceCategory(
          key = "pref_category_updates",
          title = { Text(stringResource(R.string.updates)) },
        )
        liquidListPreference(
          key = PREF_KEY_REPO_AUTO_UPDATES,
          defaultValue = PREF_DEFAULT_REPO_AUTO_UPDATES,
          title = { Text(stringResource(R.string.pref_repo_updates_title)) },
          icon = { strValue ->
            if (strValue != RepoUpdateValues.Never.name)
              Icon(
                imageVector = Icons.Default.SystemSecurityUpdate,
                contentDescription = null,
                modifier = Modifier.semantics { hideFromAccessibility() },
              )
            else
              Icon(
                imageVector = Icons.Default.SystemSecurityUpdateWarning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.semantics { hideFromAccessibility() },
              )
          },
          summary = { strValue ->
            val value = strValue.toRepoUpdateValue()
            if (value.workerEnabled) {
              val nextUpdate = model.nextRepoUpdateFlow.collectAsState(Long.MAX_VALUE).value
              val nextUpdateStr =
                if (nextUpdate == Long.MAX_VALUE) {
                  stringResource(
                    R.string.auto_update_time,
                    stringResource(R.string.repositories_last_update_never),
                  )
                } else if (nextUpdate - currentTimeMillis() <= 0) {
                  stringResource(R.string.auto_update_time_past)
                } else {
                  stringResource(R.string.auto_update_time, nextUpdate.asRelativeTimeString())
                }
              val s =
                if (value == RepoUpdateValues.OnlyWifi) {
                  stringResource(R.string.pref_repo_updates_summary_only_wifi)
                } else if (value == RepoUpdateValues.Always) {
                  stringResource(R.string.pref_repo_updates_summary_always)
                } else error("Unknown value: $strValue")
              Text(s + "\n" + nextUpdateStr)
            } else {
              val s =
                if (value == RepoUpdateValues.OnlyWhenOpenApp) {
                  stringResource(R.string.pref_repo_updates_summary_only_when_open_app)
                } else {
                  stringResource(R.string.pref_repo_updates_summary_never)
                }
              Text(text = s, color = MaterialTheme.colorScheme.error)
            }
          },
          values = RepoUpdateValues.entries.map { it.name },
          valueToText = { value: String ->
            when (value.toRepoUpdateValue()) {
              RepoUpdateValues.OnlyWifi -> res.getString(R.string.pref_auto_updates_only_wifi)
              RepoUpdateValues.Always -> res.getString(R.string.pref_auto_updates_only_always)
              RepoUpdateValues.OnlyWhenOpenApp ->
                res.getString(R.string.pref_auto_updates_only_only_when_open_app)
              RepoUpdateValues.Never -> res.getString(R.string.pref_auto_updates_only_never)
            }
          },
        )
        liquidListPreference(
          key = PREF_KEY_APP_AUTO_UPDATES,
          defaultValue = PREF_DEFAULT_APP_AUTO_UPDATES,
          title = { Text(stringResource(R.string.update_auto_install)) },
          icon = { strValue ->
            Icon(
              imageVector =
                if (strValue != AppUpdateValues.Never.name) {
                  Icons.Default.Update
                } else {
                  Icons.Default.UpdateDisabled
                },
              contentDescription = null,
              tint =
                if (strValue != AppUpdateValues.Never.name) {
                  LocalContentColor.current
                } else {
                  MaterialTheme.colorScheme.error
                },
              modifier = Modifier.semantics { hideFromAccessibility() },
            )
          },
          summary = { strValue ->
            val value = strValue.toAppUpdateValue()
            val s =
              if (value.workerEnabled) {
                val nextUpdate = model.nextAppUpdateFlow.collectAsState(Long.MAX_VALUE).value
                val nextUpdateStr =
                  if (nextUpdate == Long.MAX_VALUE) {
                    stringResource(
                      R.string.auto_update_time,
                      stringResource(R.string.repositories_last_update_never),
                    )
                  } else if (nextUpdate - currentTimeMillis() <= 0) {
                    stringResource(R.string.auto_update_time_past)
                  } else {
                    stringResource(R.string.auto_update_time, nextUpdate.asRelativeTimeString())
                  }
                val preS =
                  when (value) {
                    AppUpdateValues.OnlyWifi ->
                      stringResource(R.string.pref_auto_updates_summary_only_wifi)
                    AppUpdateValues.Always ->
                      stringResource(R.string.pref_auto_updates_summary_always)
                    else -> error("Unknown value with worker: $strValue")
                  }
                preS + "\n" + nextUpdateStr
              } else {
                when (value) {
                  AppUpdateValues.Immediately ->
                    stringResource(R.string.pref_auto_updates_summary_immediately)
                  AppUpdateValues.Never -> stringResource(R.string.pref_auto_updates_summary_never)
                  else -> error("Unknown value: $strValue")
                }
              }
            val textColor =
              if (strValue != AppUpdateValues.Never.name) {
                Color.Unspecified
              } else {
                MaterialTheme.colorScheme.error
              }
            Text(s, color = textColor)
          },
          values = AppUpdateValues.entries.map { it.name },
          valueToText = { value: String ->
            when (value.toAppUpdateValue()) {
              AppUpdateValues.Immediately ->
                res.getString(R.string.pref_auto_updates_only_immediately)
              AppUpdateValues.OnlyWifi -> res.getString(R.string.pref_auto_app_updates_only_wifi)
              AppUpdateValues.Always -> res.getString(R.string.pref_auto_app_updates_only_always)
              AppUpdateValues.Never -> res.getString(R.string.pref_auto_updates_only_never)
            }
          },
        )
        liquidPreferenceCategory(
          key = "pref_category_network",
          title = { Text(stringResource(R.string.pref_category_network)) },
        )
        liquidListPreference(
          key = PREF_KEY_MIRROR_CHOOSER,
          defaultValue = PREF_DEFAULT_MIRROR_CHOOSER,
          title = { Text(stringResource(R.string.pref_mirror_chooser_title)) },
          icon = {
            Icon(
              imageVector = Icons.Default.Lan,
              contentDescription = null,
              modifier = Modifier.semantics { hideFromAccessibility() },
            )
          },
          summary = { strValue ->
            val strRes = strValue.toMirrorChooserValue().res
            Text(stringResource(strRes))
          },
          values = MirrorChooserValues.entries.map { it.name },
          valueToText = { value: String ->
            val strRes = value.toMirrorChooserValue().res
            res.getString(strRes)
          },
        )
        preferenceProxy(proxyState, showProxyError)
        extraNetworkSettings(context)
        liquidPreferenceCategory(
          key = "pref_category_privacy",
          title = { Text(stringResource(R.string.privacy)) },
        )
        liquidSwitchPreference(
          key = PREF_USE_DNS_CACHE,
          defaultValue = PREF_USE_DNS_CACHE_DEFAULT,
          title = { Text(stringResource(R.string.useDnsCache)) },
          icon = {
            Icon(
              imageVector = Icons.Default.Dns,
              contentDescription = null,
              modifier = Modifier.semantics { hideFromAccessibility() },
            )
          },
          summary = { Text(stringResource(R.string.useDnsCacheSummary)) },
        )
        liquidSwitchPreference(
          key = PREF_KEY_PREVENT_SCREENSHOTS,
          defaultValue = PREF_DEFAULT_PREVENT_SCREENSHOTS,
          title = { Text(stringResource(R.string.preventScreenshots_title)) },
          icon = {
            Icon(
              imageVector = Icons.Default.Screenshot,
              contentDescription = null,
              modifier = Modifier.semantics { hideFromAccessibility() },
            )
          },
          summary = { Text(text = stringResource(R.string.preventScreenshots_summary)) },
        )
        extraPrivacySettings(context)
        item { LiquidGlassDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) }
        liquidActionPreference(
          key = "debugLog",
          icon = {
            Icon(
              imageVector = Icons.Default.BugReport,
              contentDescription = null,
              modifier = Modifier.semantics { hideFromAccessibility() },
            )
          },
          title = { Text(stringResource(R.string.pref_export_log_title)) },
          summary = { Text(stringResource(R.string.pref_export_log_summary)) },
          onClick = {
            if (!launcher.launchSafe("${getLogName(context)}.txt")) {
              val msg = res.getString(R.string.no_handler_app_generic)
              if (toastHost != null) {
                toastHost.show(msg, LiquidToastType.WARNING)
              } else {
                Toast.makeText(context, msg, LENGTH_SHORT).show()
              }
            }
          },
        )
      }
    }
  }
}

private fun LazyListScope.liquidPreferenceCategory(
  key: String,
  title: @Composable () -> Unit,
) {
  item(key = key, contentType = "liquidPreferenceCategory") {
    ProvideTextStyle(
      MaterialTheme.typography.labelLarge.copy(
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
      )
    ) {
      Column(modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 6.dp)) {
        title()
      }
    }
  }
}

private fun LazyListScope.liquidActionPreference(
  key: String,
  icon: @Composable () -> Unit,
  title: @Composable () -> Unit,
  summary: @Composable () -> Unit,
  onClick: () -> Unit,
) {
  item(key = key, contentType = "liquidActionPreference") {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(16.dp),
      modifier =
        Modifier.fillMaxWidth()
          .padding(horizontal = 8.dp, vertical = 2.dp)
          .liquidRowSquish()
          .clip(LiquidGlassShapes.SmallCardShape)
          .clickable(role = Role.Button, onClick = onClick)
          .padding(horizontal = 12.dp, vertical = 14.dp),
    ) {
      icon()
      Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.weight(1f)) {
        ProvideTextStyle(MaterialTheme.typography.titleMedium) { title() }
        ProvideTextStyle(
          MaterialTheme.typography.bodyMedium.copy(
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        ) {
          summary()
        }
      }
    }
  }
}

private fun LazyListScope.liquidListPreference(
  key: String,
  defaultValue: String,
  values: List<String>,
  valueToText: (String) -> String,
  title: @Composable () -> Unit,
  icon: @Composable (String) -> Unit,
  summary: @Composable (String) -> Unit,
) {
  item(key = key, contentType = "liquidListPreference") {
    var currentValue by rememberPreferenceState(key = key, defaultValue = defaultValue)
    var showDialog by remember { mutableStateOf(false) }

    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(16.dp),
      modifier =
        Modifier.fillMaxWidth()
          .padding(horizontal = 8.dp, vertical = 2.dp)
          .liquidRowSquish()
          .clip(LiquidGlassShapes.SmallCardShape)
          .clickable(role = Role.Button) { showDialog = true }
          .padding(horizontal = 12.dp, vertical = 14.dp),
    ) {
      icon(currentValue)
      Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.weight(1f)) {
        ProvideTextStyle(MaterialTheme.typography.titleMedium) { title() }
        ProvideTextStyle(
          MaterialTheme.typography.bodyMedium.copy(
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        ) {
          summary(currentValue)
        }
      }
    }

    if (showDialog) {
      LiquidGlassDialog(
        onDismissRequest = { showDialog = false },
        title = title,
        text = {
          Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
            values.forEach { option ->
              val selected = option == currentValue || (option == "dark" && currentValue == "night")
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier =
                  Modifier.fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp)
                    .liquidRowSquish()
                    .clip(LiquidGlassShapes.SmallCardShape)
                    .clickable(role = Role.RadioButton) {
                      currentValue = option
                      showDialog = false
                    }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
              ) {
                LiquidGlassRadioButton(
                  selected = selected,
                  onClick = {
                    currentValue = option
                    showDialog = false
                  },
                )
                Text(
                  text = valueToText(option),
                  style = MaterialTheme.typography.bodyLarge,
                  color = MaterialTheme.colorScheme.onSurface,
                )
              }
            }
          }
        },
        confirmButton = {
          LiquidGlassTextButton(onClick = { showDialog = false }) {
            Text(stringResource(R.string.cancel))
          }
        },
      )
    }
  }
}

private fun LazyListScope.liquidSwitchPreference(
  key: String,
  defaultValue: Boolean,
  title: @Composable () -> Unit,
  icon: @Composable () -> Unit,
  summary: @Composable () -> Unit,
) {
  item(key = key, contentType = "liquidSwitchPreference") {
    var checked by rememberPreferenceState(key = key, defaultValue = defaultValue)
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(16.dp),
      modifier =
        Modifier.fillMaxWidth()
          .padding(horizontal = 8.dp, vertical = 2.dp)
          .liquidRowSquish()
          .clip(LiquidGlassShapes.SmallCardShape)
          .clickable(role = Role.Switch) { checked = !checked }
          .padding(horizontal = 12.dp, vertical = 14.dp),
    ) {
      icon()
      Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.weight(1f)) {
        ProvideTextStyle(MaterialTheme.typography.titleMedium) { title() }
        androidx.compose.material3.ProvideTextStyle(
          MaterialTheme.typography.bodyMedium.copy(
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        ) {
          summary()
        }
      }
      LiquidGlassSwitch(checked = checked, onCheckedChange = { checked = it })
    }
  }
}

@Preview
@Composable
fun SettingsPreview() {
  FDroidContent {
    val model =
      SettingsModel(
        prefsFlow = MutableStateFlow(MapPreferences()),
        nextRepoUpdateFlow = MutableStateFlow(Long.MAX_VALUE),
        nextAppUpdateFlow = MutableStateFlow(currentTimeMillis() - HOURS.toMillis(12)),
        currentAppIconFlow = MutableStateFlow(AppIcon.Default),
      )
    Settings(model, {}, {}) {}
  }
}

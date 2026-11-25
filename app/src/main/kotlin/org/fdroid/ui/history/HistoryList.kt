package org.fdroid.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DownloadForOffline
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.fdroid.R
import org.fdroid.history.InstallEvent
import org.fdroid.ui.glass.LiquidGlassBadgedBox
import org.fdroid.ui.glass.LiquidGlassCard
import org.fdroid.ui.glass.LiquidGlassShapes
import org.fdroid.ui.glass.LiquidGlassSwitch
import org.fdroid.ui.glass.springListItem
import org.fdroid.ui.utils.AsyncShimmerImage
import org.fdroid.ui.utils.BadgeIcon
import org.fdroid.ui.utils.VersionLine
import org.fdroid.ui.utils.asRelativeTimeString

@Composable
fun HistoryList(
  items: List<HistoryItem>,
  enabled: Boolean?,
  onEnabled: (Boolean) -> Unit,
  paddingValues: PaddingValues,
) {
  LazyColumn(contentPadding = paddingValues) {
    if (enabled != null)
      item(key = "toggle", contentType = "toggle") {
        LiquidGlassCard(
          shape = LiquidGlassShapes.CardShape,
          containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.78f),
          squishable = true,
          modifier =
            Modifier.springListItem(this)
              .padding(16.dp)
              .fillMaxWidth()
              .clickable { onEnabled(!enabled) },
        ) {
          CompositionLocalProvider(
            LocalContentColor provides MaterialTheme.colorScheme.onPrimaryContainer
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(16.dp)) {
              Text(
                text = stringResource(R.string.install_history_switch),
                fontSize = 19.sp,
                modifier = Modifier.weight(1f),
              )
              LiquidGlassSwitch(checked = enabled, onCheckedChange = onEnabled)
            }
          }
        }
      }
    if (items.isEmpty())
      item {
        val s =
          if (enabled == true) stringResource(R.string.install_history_empty_state)
          else stringResource(R.string.install_history_disabled_state)
        Text(
          text = s,
          textAlign = TextAlign.Center,
          modifier = Modifier.padding(16.dp).fillMaxWidth(),
        )
      }
    else
      items(
        items = items,
        key = { "${it.event.time}-${it.event.packageName}" },
        contentType = { "item" },
      ) { item ->
        ListItem(
          leadingContent = {
            LiquidGlassBadgedBox(
              badge = {
                BadgeIcon(
                  icon =
                    if (item.event is InstallEvent) {
                      Icons.Filled.DownloadForOffline
                    } else {
                      Icons.Filled.RemoveCircleOutline
                    },
                  color =
                    if (item.event is InstallEvent) {
                      MaterialTheme.colorScheme.secondary
                    } else {
                      MaterialTheme.colorScheme.error
                    },
                  contentDescription =
                    if (item.event is InstallEvent) {
                      stringResource(R.string.menu_install)
                    } else {
                      stringResource(R.string.menu_uninstall)
                    },
                )
              }
            ) {
              AsyncShimmerImage(
                model = item.iconModel,
                error = painterResource(R.drawable.ic_repo_app_default),
                contentDescription = null,
                modifier =
                  Modifier.size(48.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .semantics { hideFromAccessibility() },
              )
            }
          },
          headlineContent = { Text(item.event.name ?: item.event.packageName) },
          supportingContent = {
            val dateStr = item.event.time.asRelativeTimeString()
            if (item.event is InstallEvent) {
              VersionLine(item.event.oldVersionName, item.event.versionName, dateStr)
            } else Text(dateStr)
          },
          colors = ListItemDefaults.colors(containerColor = Color.Transparent),
          modifier =
            Modifier.springListItem(this).let {
              if (enabled == false) it.alpha(0.5f) else it
            },
        )
      }
  }
}

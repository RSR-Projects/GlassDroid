package org.fdroid.ui.details

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PermDeviceInformation
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.Bullet
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.fdroid.R
import org.fdroid.ui.FDroidContent
import org.fdroid.ui.glass.LiquidFloatingTopBar
import org.fdroid.ui.glass.LiquidGlassBadge
import org.fdroid.ui.glass.LiquidGlassBottomSheet
import org.fdroid.ui.glass.LiquidGlassCard
import org.fdroid.ui.glass.LiquidGlassDropdownMenu
import org.fdroid.ui.glass.LiquidGlassDropdownMenuItem
import org.fdroid.ui.glass.LiquidGlassShapes
import org.fdroid.ui.glass.LiquidGlassTextButton
import org.fdroid.ui.glass.LiquidScaffold
import org.fdroid.ui.glass.fastLiquidSurface
import org.fdroid.ui.glass.liquidRowSquish
import org.fdroid.ui.glass.springListItem
import org.fdroid.ui.utils.BackButton
import org.fdroid.ui.utils.ExpandableSection
import org.fdroid.ui.utils.testApp

@Composable
fun PermissionsSection(
  versions: List<VersionItem>,
  modifier: Modifier = Modifier,
) {
  var showScreen by remember { mutableStateOf(false) }
  val version = versions.find { it.isSuggested } ?: versions.first()
  ExpandableSection(
    icon = rememberVectorPainter(Icons.Default.PermDeviceInformation),
    title = stringResource(R.string.app_details_permissions_section),
    modifier = modifier,
  ) {
    Column {
      val sensitivePerms = version.permissions.filter { it.type == PermissionType.SENSITIVE }
      val numSensitive = sensitivePerms.size
      Text(
        text =
          if (numSensitive == 0) {
            stringResource(R.string.app_details_permissions_sensitive_none)
          } else {
            pluralStringResource(
              R.plurals.app_details_permissions_sensitive,
              numSensitive,
              numSensitive,
            )
          },
        modifier = Modifier.padding(horizontal = 16.dp),
      )
      if (numSensitive > 0) {
        Text(
          text =
            buildAnnotatedString {
              withBulletList(bullet = Bullet.Default.copy(padding = 8.sp)) {
                sensitivePerms.forEach { perm ->
                  withBulletListItem { append(perm.label ?: perm.name) }
                }
              }
            },
          modifier = Modifier.padding(top = 8.dp).padding(horizontal = 16.dp),
        )
      }
      LiquidGlassTextButton(
        onClick = { showScreen = true },
        modifier = Modifier.padding(horizontal = 4.dp),
      ) {
        Text(
          text =
            stringResource(
              R.string.app_details_permissions_button,
              version.permissions.size,
            )
        )
        Spacer(modifier = Modifier.width(8.dp))
        Icon(imageVector = Icons.AutoMirrored.Default.ArrowForward, contentDescription = null)
      }
    }
  }
  if (showScreen) {
    PermissionsSheet(version = version, versions = versions, onDismiss = { showScreen = false })
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PermissionsSheet(
  version: VersionItem,
  versions: List<VersionItem>,
  @Suppress("UNUSED_PARAMETER") initiallyHidden: Boolean = !LocalInspectionMode.current,
  onDismiss: () -> Unit,
) {
  LiquidGlassBottomSheet(
    onDismissRequest = onDismiss,
    showDragHandle = false,
  ) {
    var currentVersion by remember { mutableStateOf(version) }
    LiquidScaffold(
      topBar = {
        LiquidFloatingTopBar(
          navigationIcon = { BackButton(onClick = onDismiss) },
          title = {
            Text(
              stringResource(
                R.string.app_details_permissions_title_num,
                currentVersion.permissions.size,
              ),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          },
          showActions = true,
          actions = {
            VersionDropDown(
              currentVersion,
              versions,
              onVersionChanged = { currentVersion = it },
              modifier = Modifier.padding(horizontal = 8.dp),
            )
          },
        )
      },
    ) { paddingValues ->
      LazyColumn(contentPadding = paddingValues) {
        item(contentType = "dropdown") {}
        item(contentType = "warning") {
          LiquidGlassCard(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.88f),
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp, horizontal = 16.dp),
          ) {
            CompositionLocalProvider(
              LocalContentColor provides MaterialTheme.colorScheme.onErrorContainer
            ) {
              Row(
                horizontalArrangement = spacedBy(16.dp),
                verticalAlignment = CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
              ) {
                Icon(Icons.Default.Info, null)
                Text(
                  stringResource(R.string.app_details_permissions_warning),
                  style = MaterialTheme.typography.bodyLarge,
                )
              }
            }
          }
        }
        PermissionType.entries.forEach { type ->
          val permissions = currentVersion.permissionsByType[type].orEmpty()
          if (permissions.isNotEmpty() || type == PermissionType.SENSITIVE) {
            item(contentType = "type_header") {
              ListItem(colors = ListItemDefaults.colors(containerColor = Color.Transparent)) {
                Text(
                  text = stringResource(type.titleRes, permissions.size),
                  style = MaterialTheme.typography.titleMedium,
                  modifier = Modifier.padding(top = 8.dp),
                )
              }
            }
            if (permissions.isEmpty()) {
              item(contentType = "type_empty") {
                ListItem(colors = ListItemDefaults.colors(containerColor = Color.Transparent)) {
                  Text(text = stringResource(R.string.app_details_permissions_sensitive_none))
                }
              }
            } else {
              items(
                items = permissions,
                contentType = { "permission" },
              ) { permission ->
                PermissionsRow(permission, modifier = Modifier.springListItem(this))
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun VersionDropDown(
  currentVersion: VersionItem,
  versions: List<VersionItem>,
  onVersionChanged: (VersionItem) -> Unit,
  modifier: Modifier = Modifier,
) {
  var expanded by remember { mutableStateOf(false) }
  Box(modifier = modifier.minimumInteractiveComponentSize()) {
    Row(
      modifier =
        Modifier.liquidRowSquish()
          .clip(LiquidGlassShapes.SmallCardShape)
          .fastLiquidSurface(shape = LiquidGlassShapes.SmallCardShape, withShadow = false)
          .clickable { expanded = true }
          .padding(horizontal = 8.dp, vertical = 6.dp),
      verticalAlignment = CenterVertically,
    ) {
      Text(
        text = currentVersion.version.versionName,
        overflow = TextOverflow.MiddleEllipsis,
        style = MaterialTheme.typography.labelMedium,
        maxLines = 1,
        modifier = Modifier.padding(start = 4.dp),
      )
      Icon(
        imageVector = Icons.Default.ArrowDropDown,
        contentDescription = null,
        modifier = Modifier.padding(start = 2.dp),
      )
    }
    LiquidGlassDropdownMenu(
      expanded = expanded,
      onDismissRequest = { expanded = false },
    ) {
      versions.forEach { versionItem ->
        LiquidGlassDropdownMenuItem(
          text = {
            Text(
              versionItem.version.versionName,
              style = MaterialTheme.typography.labelMedium,
            )
          },
          onClick = {
            onVersionChanged(versionItem)
            expanded = false
          },
        )
      }
    }
  }
}

@Composable
private fun PermissionsRow(permission: Permission, modifier: Modifier = Modifier) {
  ListItem(
    modifier = modifier,
    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    supportingContent = {
      Column(verticalArrangement = spacedBy(4.dp)) {
        if (permission.description?.isNotBlank() == true) {
          Text(
            text = permission.description,
            style =
              LocalTextStyle.current.copy(
                hyphens = Hyphens.Auto,
                lineBreak = LineBreak.Paragraph.copy(strategy = LineBreak.Strategy.HighQuality),
              ),
          )
        }
        LiquidGlassBadge(
          containerColor = MaterialTheme.colorScheme.surfaceContainer,
          contentColor = MaterialTheme.colorScheme.onSurface,
        ) {
          Text(
            text = permission.name.substringAfter("android.permission."),
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(2.dp),
          )
        }
      }
    },
  ) {
    val text =
      if (permission.label != null && permission.label != permission.name) {
        permission.label
      } else {
        permission.name.substringAfter("android.permission.")
      }
    Text(text = text)
  }
}

@Composable
@PreviewLightDark
private fun Preview() {
  FDroidContent { PermissionsSection(testApp.versions!!) }
}

@Composable
@PreviewLightDark
private fun PreviewNoSensitivePermissions() {
  FDroidContent { PermissionsSection(testApp.versions!!.subList(1, 3)) }
}

@Composable
@PreviewLightDark
private fun PreviewScreen() {
  FDroidContent {
    Box(modifier = Modifier.fillMaxSize()) {
      PermissionsSheet(testApp.versions!!.first(), testApp.versions, initiallyHidden = false) {}
    }
  }
}

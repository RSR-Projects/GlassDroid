package org.fdroid.ui.glass

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import org.fdroid.R
import org.fdroid.ui.utils.startActivitySafe

/**
 * Floating iOS 26-style Liquid Glass contextual menu shown on long-pressing an app card or row.
 * Exposes real actions: open details, launch installed app (when a launcher activity exists),
 * share app link via system share sheet, and copy package ID with feedback toast.
 */
@Composable
fun AppContextMenu(
  expanded: Boolean,
  packageName: String,
  appName: String,
  isInstalled: Boolean,
  onDismissRequest: () -> Unit,
  onOpenDetails: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val toastHost = LocalLiquidToastHost.current
  val copiedMsg = stringResource(R.string.package_name_copied)
  val shareSubject = appName
  val shareUrl = "https://f-droid.org/packages/$packageName/"

  val launchIntent =
    remember(expanded, isInstalled, packageName) {
      if (expanded && isInstalled) {
        runCatching { context.packageManager.getLaunchIntentForPackage(packageName) }.getOrNull()
      } else {
        null
      }
    }

  LiquidGlassDropdownMenu(
    expanded = expanded,
    onDismissRequest = onDismissRequest,
    modifier = modifier,
  ) {
    LiquidGlassDropdownMenuItem(
      leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) },
      text = { Text(stringResource(R.string.menu_app_details)) },
      onClick = {
        onDismissRequest()
        onOpenDetails()
      },
    )
    if (launchIntent != null) {
      LiquidGlassDropdownMenuItem(
        leadingIcon = { Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null) },
        text = { Text(stringResource(R.string.menu_open)) },
        onClick = {
          onDismissRequest()
          context.startActivitySafe(launchIntent)
        },
      )
    }
    LiquidGlassDropdownMenuItem(
      leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
      text = { Text(stringResource(R.string.menu_share)) },
      onClick = {
        onDismissRequest()
        val sendIntent =
          Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, shareSubject)
            putExtra(Intent.EXTRA_TEXT, "$shareSubject\n$shareUrl")
          }
        context.startActivitySafe(Intent.createChooser(sendIntent, shareSubject))
      },
    )
    LiquidGlassDropdownMenuItem(
      leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
      text = { Text(stringResource(R.string.copy_package_name)) },
      onClick = {
        onDismissRequest()
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText(packageName, packageName))
        toastHost?.show("$copiedMsg: $packageName", LiquidToastType.INFO)
      },
    )
  }
}

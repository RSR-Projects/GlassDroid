package org.fdroid.ui.details

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import org.fdroid.R
import org.fdroid.ui.glass.LiquidFloatingTopBar
import org.fdroid.ui.utils.BackButton
import org.fdroid.ui.utils.TopAppBarButton
import org.fdroid.ui.utils.TopAppBarOverflowButton
import org.fdroid.ui.utils.startActivitySafe

@Composable
fun AppDetailsTopAppBar(
  item: AppDetailsItem,
  showTitle: Boolean,
  onBackNav: (() -> Unit)?,
) {
  LiquidFloatingTopBar(
    navigationIcon = if (onBackNav != null) { { BackButton(onClick = onBackNav) } } else null,
    showTitlePill = showTitle && item is LoadedAppDetailsItem,
    title = {
      if (item is LoadedAppDetailsItem) {
        Text(item.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
      }
    },
    showActions = item is LoadedAppDetailsItem,
    actions = {
      if (item is LoadedAppDetailsItem) {
        val context = LocalContext.current
        item.actions.shareIntent?.let { shareIntent ->
          TopAppBarButton(
            imageVector = Icons.Filled.Share,
            contentDescription = stringResource(R.string.menu_share),
            onClick = { context.startActivitySafe(shareIntent) },
          )
        }
        // the launcher needs to be at least here,
        // because if the menu is dismissed we don't get the result
        val uninstallLauncher =
          rememberLauncherForActivityResult(StartActivityForResult()) {
            item.actions.onUninstallResult(it)
          }
        TopAppBarOverflowButton { onDismissRequest ->
          AppDetailsMenu(item, uninstallLauncher, onDismissRequest)
        }
      }
    },
  )
}

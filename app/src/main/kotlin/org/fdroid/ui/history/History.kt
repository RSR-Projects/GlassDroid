package org.fdroid.ui.history

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import org.fdroid.R
import org.fdroid.ui.FDroidContent
import org.fdroid.ui.glass.LiquidFloatingTopBar
import org.fdroid.ui.glass.LiquidGlassDialog
import org.fdroid.ui.glass.LiquidGlassTextButton
import org.fdroid.ui.glass.LiquidScaffold
import org.fdroid.ui.utils.BackButton
import org.fdroid.ui.utils.BigLoadingIndicator
import org.fdroid.ui.utils.TopAppBarButton

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun History(
  items: List<HistoryItem>?,
  enabled: Boolean?,
  onEnabled: (Boolean) -> Unit,
  onDeleteAll: () -> Unit,
  onBackClicked: (() -> Unit)?,
) {
  var deleteAllDialogShown by remember { mutableStateOf(false) }
  LiquidScaffold(
    topBar = {
      LiquidFloatingTopBar(
        navigationIcon = if (onBackClicked != null) { { BackButton(onClick = onBackClicked) } } else null,
        title = { Text(stringResource(R.string.install_history), maxLines = 1) },
        showActions = !items.isNullOrEmpty(),
        actions = {
          if (!items.isNullOrEmpty())
            TopAppBarButton(
              imageVector = Icons.Filled.Delete,
              contentDescription = stringResource(R.string.install_history_delete_ally),
              onClick = { deleteAllDialogShown = true },
            )
        },
      )
    },
  ) { paddingValues ->
    if (items == null) BigLoadingIndicator(modifier = Modifier.padding(paddingValues))
    else HistoryList(items, enabled, onEnabled, paddingValues)
    val onDismiss = { deleteAllDialogShown = false }
    if (deleteAllDialogShown)
      LiquidGlassDialog(
        title = { Text(text = stringResource(R.string.install_history_delete_text)) },
        onDismissRequest = onDismiss,
        confirmButton = {
          LiquidGlassTextButton(
            onClick = {
              onDeleteAll()
              onDismiss()
            },
            contentColor = MaterialTheme.colorScheme.error,
          ) {
            Text(text = stringResource(R.string.delete))
          }
        },
        dismissButton = {
          LiquidGlassTextButton(onClick = onDismiss) {
            Text(stringResource(android.R.string.cancel))
          }
        },
      )
  }
}

@Preview
@Composable
private fun PreviewLoading() {
  FDroidContent { History(null, true, {}, {}) {} }
}

@Preview
@Composable
private fun PreviewEmpty() {
  FDroidContent { History(emptyList(), true, {}, {}) {} }
}

@Preview
@Composable
private fun PreviewEmptyDisabled() {
  FDroidContent { History(emptyList(), false, {}, {}) {} }
}

package org.fdroid.ui.apps

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.fdroid.R
import org.fdroid.ui.glass.LiquidGlassDialog
import org.fdroid.ui.glass.LiquidGlassTextButton

@Composable
fun IgnoreIssueDialog(appName: String, onIgnore: () -> Unit, onDismiss: () -> Unit) {
  LiquidGlassDialog(
    title = { Text(text = stringResource(R.string.my_apps_ignore_dialog_title)) },
    text = { Text(text = stringResource(R.string.my_apps_ignore_dialog_text, appName)) },
    onDismissRequest = onDismiss,
    confirmButton = {
      LiquidGlassTextButton(
        onClick = onIgnore,
        contentColor = MaterialTheme.colorScheme.error,
      ) {
        Text(text = stringResource(R.string.my_apps_ignore_dialog_button))
      }
    },
    dismissButton = {
      LiquidGlassTextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) }
    },
  )
}

package org.fdroid.ui.repositories.details

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.fdroid.R
import org.fdroid.ui.glass.LiquidGlassDialog
import org.fdroid.ui.glass.LiquidGlassTextButton

@Composable
fun DeleteDialog(onDismissDialog: () -> Unit, onDelete: () -> Unit) {
  LiquidGlassDialog(
    title = { Text(text = stringResource(R.string.repo_confirm_delete_title)) },
    text = { Text(text = stringResource(R.string.repo_confirm_delete_body)) },
    onDismissRequest = onDismissDialog,
    confirmButton = {
      LiquidGlassTextButton(onClick = onDelete, contentColor = MaterialTheme.colorScheme.error) {
        Text(text = stringResource(R.string.delete))
      }
    },
    dismissButton = {
      LiquidGlassTextButton(onClick = onDismissDialog) { Text(stringResource(android.R.string.cancel)) }
    },
  )
}

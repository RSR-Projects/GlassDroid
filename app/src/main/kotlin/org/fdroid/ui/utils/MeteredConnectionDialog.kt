package org.fdroid.ui.utils

import android.text.format.Formatter
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.fdroid.R
import org.fdroid.ui.glass.LiquidGlassCheckbox
import org.fdroid.ui.glass.LiquidGlassDialog
import org.fdroid.ui.glass.LiquidGlassShapes
import org.fdroid.ui.glass.LiquidGlassTextButton
import org.fdroid.ui.glass.liquidRowSquish

@Composable
fun MeteredConnectionDialog(
  numBytes: Long?,
  onConfirm: (Boolean) -> Unit,
  onDismiss: () -> Unit,
) {
  var dontShowAgain by remember { mutableStateOf(false) }
  LiquidGlassDialog(
    title = { Text(text = stringResource(R.string.dialog_metered_title)) },
    text = {
      val s =
        if (numBytes == null) {
          stringResource(R.string.dialog_metered_text_no_size)
        } else {
          val sizeStr = Formatter.formatFileSize(LocalContext.current, numBytes)
          stringResource(R.string.dialog_metered_text, sizeStr)
        }
      Column {
        Text(text = s)
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier =
            Modifier.padding(top = 12.dp)
              .liquidRowSquish()
              .clip(LiquidGlassShapes.SmallCardShape)
              .clickable(role = Role.Checkbox) { dontShowAgain = !dontShowAgain }
              .padding(vertical = 4.dp),
        ) {
          LiquidGlassCheckbox(
            checked = dontShowAgain,
            onCheckedChange = { dontShowAgain = it },
          )
          Text(
            text = stringResource(R.string.dialog_metered_dont_show),
            modifier = Modifier.padding(start = 6.dp),
          )
        }
      }
    },
    onDismissRequest = onDismiss,
    confirmButton = {
      LiquidGlassTextButton(
        onClick = {
          onDismiss()
          onConfirm(dontShowAgain)
        },
        contentColor = MaterialTheme.colorScheme.error,
      ) {
        Text(text = stringResource(R.string.dialog_metered_button))
      }
    },
    dismissButton = {
      LiquidGlassTextButton(onClick = onDismiss) {
        Text(stringResource(android.R.string.cancel))
      }
    },
  )
}

@Preview
@Composable
private fun Preview() {
  MeteredConnectionDialog(9_999_999, {}, {})
}

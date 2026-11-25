package org.fdroid.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.VpnLock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.net.InetSocketAddress
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import org.fdroid.R
import org.fdroid.settings.SettingsConstants.PREF_DEFAULT_PROXY
import org.fdroid.settings.SettingsConstants.PREF_KEY_PROXY
import org.fdroid.ui.FDroidContent
import org.fdroid.ui.glass.LiquidGlassDialog
import org.fdroid.ui.glass.LiquidGlassIconButton
import org.fdroid.ui.glass.LiquidGlassShapes
import org.fdroid.ui.glass.LiquidGlassTextButton
import org.fdroid.ui.glass.fastLiquidSurface
import org.fdroid.ui.glass.liquidRowSquish

fun LazyListScope.preferenceProxy(
  proxyState: MutableState<String>,
  showError: MutableState<Boolean>,
) {
  item(key = PREF_KEY_PROXY, contentType = "liquidProxyPreference") {
    var showDialog by remember { mutableStateOf(false) }
    var draftValue by remember(showDialog) { mutableStateOf(TextFieldValue(proxyState.value)) }
    val commitProxy = {
      val trimmed = draftValue.text.trim()
      if (trimmed.isBlank() || isProxyValid(trimmed)) {
        showError.value = false
        proxyState.value = trimmed
        showDialog = false
      } else {
        showError.value = true
      }
    }

    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(16.dp),
      modifier =
        Modifier.fillMaxWidth()
          .padding(horizontal = 8.dp, vertical = 2.dp)
          .liquidRowSquish()
          .clip(LiquidGlassShapes.SmallCardShape)
          .clickable(role = Role.Button) {
            showError.value = false
            draftValue = TextFieldValue(proxyState.value)
            showDialog = true
          }
          .padding(horizontal = 12.dp, vertical = 14.dp),
    ) {
      Icon(
        imageVector = Icons.Default.VpnLock,
        contentDescription = null,
        modifier = Modifier.semantics { hideFromAccessibility() },
      )
      Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.weight(1f)) {
        ProvideTextStyle(MaterialTheme.typography.titleMedium) {
          Text(stringResource(R.string.pref_proxy_title))
        }
        ProvideTextStyle(
          MaterialTheme.typography.bodyMedium.copy(
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        ) {
          val value = proxyState.value
          val s =
            if (value.isBlank()) {
              stringResource(R.string.pref_proxy_disabled)
            } else {
              stringResource(R.string.pref_proxy_enabled, value)
            }
          Text(s)
        }
      }
    }

    if (showDialog) {
      LiquidGlassDialog(
        onDismissRequest = {
          showError.value = false
          showDialog = false
        },
        title = { Text(stringResource(R.string.pref_proxy_title)) },
        text = {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier =
                Modifier.fillMaxWidth()
                  .fastLiquidSurface(
                    shape = LiquidGlassShapes.SmallCardShape,
                    borderColor =
                      if (showError.value) MaterialTheme.colorScheme.error.copy(alpha = 0.7f) else null,
                  )
                  .padding(start = 14.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
            ) {
              Box(modifier = Modifier.weight(1f)) {
                if (draftValue.text.isEmpty()) {
                  Text(
                    text = "127.0.0.1:8118",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
                BasicTextField(
                  value = draftValue,
                  onValueChange = {
                    showError.value = false
                    draftValue = it
                  },
                  singleLine = true,
                  textStyle =
                    MaterialTheme.typography.bodyLarge.copy(
                      color = MaterialTheme.colorScheme.onSurface
                    ),
                  cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                  keyboardActions = KeyboardActions { commitProxy() },
                  modifier = Modifier.fillMaxWidth(),
                )
              }
              if (draftValue.text.isNotBlank()) {
                LiquidGlassIconButton(
                  onClick = {
                    showError.value = false
                    draftValue = TextFieldValue("")
                  },
                  modifier = Modifier.size(36.dp),
                ) {
                  Icon(Icons.Default.Clear, stringResource(R.string.clear))
                }
              }
            }
            val s =
              if (showError.value) {
                stringResource(R.string.pref_proxy_error)
              } else {
                stringResource(R.string.pref_proxy_hint)
              }
            Text(
              text = s,
              style = MaterialTheme.typography.bodySmall,
              color =
                if (showError.value) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(horizontal = 4.dp),
            )
          }
        },
        dismissButton = {
          LiquidGlassTextButton(
            onClick = {
              showError.value = false
              showDialog = false
            }
          ) {
            Text(stringResource(R.string.cancel))
          }
        },
        confirmButton = {
          LiquidGlassTextButton(onClick = commitProxy) {
            Text(stringResource(R.string.ok))
          }
        },
      )
    }
  }
}

private fun isProxyValid(proxyStr: String): Boolean =
  try {
    val parts = proxyStr.split(':')
    if (parts.size != 2 || parts[0].isBlank()) return false
    val port = parts[1].toInt()
    if (port !in 1..65535) return false
    InetSocketAddress.createUnresolved(parts[0], port)
    true
  } catch (_: Exception) {
    false
  }

@Preview
@Composable
private fun PreviewDefault() {
  FDroidContent {
    ProvidePreferenceLocals {
      val showProxyError = remember { mutableStateOf(false) }
      val proxyState = remember { mutableStateOf(PREF_DEFAULT_PROXY) }
      LazyColumn { preferenceProxy(proxyState, showProxyError) }
    }
  }
}

@Preview
@Composable
private fun PreviewProxySet() {
  FDroidContent {
    ProvidePreferenceLocals {
      val showProxyError = remember { mutableStateOf(false) }
      val proxyState = remember { mutableStateOf("127.0.0.1:8000") }
      LazyColumn { preferenceProxy(proxyState, showProxyError) }
    }
  }
}

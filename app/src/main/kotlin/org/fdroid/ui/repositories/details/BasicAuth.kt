package org.fdroid.ui.repositories.details

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.fdroid.R
import org.fdroid.ui.FDroidContent
import org.fdroid.ui.glass.LiquidGlassShapes
import org.fdroid.ui.glass.fastLiquidSurface
import org.fdroid.ui.utils.FDroidOutlineButton

@Composable
fun BasicAuth(
  username: String,
  modifier: Modifier = Modifier,
  onEditCredentials: (String, String) -> Unit,
) {
  val usernameState = rememberTextFieldState(initialText = username)
  val passwordState = rememberTextFieldState()
  var showSaveButton by remember { mutableStateOf(true) }
  Column(verticalArrangement = spacedBy(8.dp), modifier = modifier.fillMaxWidth().imePadding()) {
    Text(
      text = stringResource(R.string.repo_basic_auth_title),
      style = MaterialTheme.typography.titleMedium,
    )
    Text(
      text = stringResource(R.string.repo_basicauth_username),
      style = MaterialTheme.typography.labelMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Box(
      modifier =
        Modifier.fillMaxWidth()
          .defaultMinSize(minHeight = 46.dp)
          .fastLiquidSurface(shape = LiquidGlassShapes.SmallCardShape, withShadow = false)
          .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
      BasicTextField(
        state = usernameState,
        textStyle =
          MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        modifier = Modifier.fillMaxWidth(),
      )
    }
    Text(
      text = stringResource(R.string.repo_basicauth_password),
      style = MaterialTheme.typography.labelMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Box(
      modifier =
        Modifier.fillMaxWidth()
          .defaultMinSize(minHeight = 46.dp)
          .fastLiquidSurface(shape = LiquidGlassShapes.SmallCardShape, withShadow = false)
          .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
      BasicTextField(
        state = passwordState,
        textStyle =
          MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        modifier = Modifier.fillMaxWidth(),
      )
    }
    AnimatedVisibility(
      visible = showSaveButton,
      modifier = Modifier.align(Alignment.End).semantics { liveRegion = LiveRegionMode.Polite },
    ) {
      FDroidOutlineButton(
        text = stringResource(R.string.repo_basicauth_edit),
        onClick = {
          val username = usernameState.text.toString()
          val password = passwordState.text.toString()
          onEditCredentials(username, password)
          showSaveButton = false
        },
        imageVector = Icons.Default.Save,
      )
    }
  }
}

@Composable
@Preview
fun BasicAuthCardPreview() {
  FDroidContent { BasicAuth("username", Modifier.padding(16.dp)) { _, _ -> } }
}

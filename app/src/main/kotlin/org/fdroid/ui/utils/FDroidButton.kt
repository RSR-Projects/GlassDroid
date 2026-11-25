package org.fdroid.ui.utils

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import org.fdroid.ui.glass.LiquidGlassButton
import org.fdroid.ui.glass.LiquidGlassOutlineButton

@Composable
fun FDroidButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  imageVector: ImageVector? = null,
) {
  LiquidGlassButton(onClick = onClick, modifier = modifier) {
    if (imageVector != null) {
      Icon(
        imageVector = imageVector,
        contentDescription = text,
        modifier = Modifier.size(18.dp),
      )
      Spacer(modifier = Modifier.size(8.dp))
    }
    Text(text = text)
  }
}

@Composable
fun FDroidOutlineButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  imageVector: ImageVector? = null,
  color: Color = MaterialTheme.colorScheme.primary,
) {
  LiquidGlassOutlineButton(onClick = onClick, tintColor = color, modifier = modifier) {
    if (imageVector != null) {
      Icon(
        imageVector = imageVector,
        contentDescription = text,
        modifier = Modifier.size(18.dp),
      )
      Spacer(modifier = Modifier.size(8.dp))
    }
    Text(text = text, maxLines = 1)
  }
}

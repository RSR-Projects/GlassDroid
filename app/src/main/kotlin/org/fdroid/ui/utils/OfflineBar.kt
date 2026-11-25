package org.fdroid.ui.utils

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.fdroid.R
import org.fdroid.ui.FDroidContent
import org.fdroid.ui.glass.LiquidGlassShapes
import org.fdroid.ui.glass.liquidGlass

@Composable
fun OfflineBar(modifier: Modifier = Modifier) {
  Box(
    contentAlignment = Alignment.Center,
    modifier =
      modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 4.dp)
        .liquidGlass(
          shape = LiquidGlassShapes.CapsuleShape,
          surfaceTint = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.78f),
          blurRadius = 5.dp,
          refractionHeight = 12.dp,
          refractionAmount = 22.dp,
          withShadow = false,
        ),
  ) {
    Text(
      text = stringResource(R.string.banner_no_internet),
      color = MaterialTheme.colorScheme.onErrorContainer,
      style = MaterialTheme.typography.labelMedium,
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
    )
  }
}

@Preview
@Composable
private fun Preview() {
  FDroidContent { OfflineBar() }
}

package org.fdroid.ui.utils

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.fdroid.ui.FDroidContent
import org.fdroid.ui.glass.LiquidGlassCircularProgress

@Composable
fun BigLoadingIndicator(modifier: Modifier = Modifier) {
  Box(contentAlignment = Alignment.Center, modifier = modifier.fillMaxSize()) {
    LiquidGlassCircularProgress(modifier = Modifier.size(64.dp), strokeWidth = 4.dp)
  }
}

@Preview
@Composable
private fun Preview() {
  FDroidContent { BigLoadingIndicator() }
}

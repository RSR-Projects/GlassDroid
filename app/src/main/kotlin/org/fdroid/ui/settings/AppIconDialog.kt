package org.fdroid.ui.settings

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import org.fdroid.R
import org.fdroid.ui.FDroidContent
import org.fdroid.ui.glass.LiquidGlassDialog
import org.fdroid.ui.glass.LiquidGlassTextButton
import org.fdroid.ui.glass.liquidButtonSquish

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppIconDialog(
  currentAppIcon: AppIcon,
  onChangeIcon: (AppIcon) -> Unit,
  onDismissRequest: () -> Unit,
) {
  val context = LocalContext.current
  val iconSizePx = with(LocalDensity.current) { 64.dp.roundToPx().coerceAtLeast(128) }
  LiquidGlassDialog(
    onDismissRequest = onDismissRequest,
    title = { Text(stringResource(R.string.icon_title)) },
    text = {
      Column(
        horizontalAlignment = CenterHorizontally,
        verticalArrangement = spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text(stringResource(R.string.icon_intro))
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(16.dp, CenterHorizontally),
          verticalArrangement = spacedBy(12.dp),
          modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        ) {
          appIcons.forEach { icon ->
            val isSelected = icon == currentAppIcon
            val label = stringResource(icon.labelRes)
            val iconBitmap = rememberAppIconBitmap(context, icon.iconRes, iconSizePx)
            Column(
              horizontalAlignment = CenterHorizontally,
              verticalArrangement = spacedBy(8.dp),
              modifier =
                Modifier.width(104.dp)
                  .liquidButtonSquish()
                  .clip(MaterialTheme.shapes.large)
                  .clickable(role = Role.Button) { onChangeIcon(icon) }
                  .padding(horizontal = 6.dp, vertical = 8.dp)
                  .testTag("app_icon_option_${icon::class.simpleName?.lowercase()}"),
            ) {
              val boxModifier = Modifier.size(64.dp)
              Box(
                modifier =
                  if (isSelected)
                    boxModifier.border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                  else boxModifier
              ) {
                val imageModifier =
                  Modifier.align(Center)
                    .size(if (isSelected) 48.dp else 64.dp)
                    .graphicsLayer(
                      shape = CircleShape,
                      shadowElevation = if (isSelected) 4f else 8f,
                      clip = true,
                    )
                if (iconBitmap != null) {
                  Image(
                    bitmap = iconBitmap,
                    contentDescription = label,
                    modifier = imageModifier,
                  )
                } else {
                  Image(
                    painter = painterResource(R.drawable.ic_glassdroid_fdroid_icon),
                    contentDescription = label,
                    modifier = imageModifier,
                  )
                }
              }
              Text(label)
            }
          }
        }
      }
    },
    confirmButton = {
      LiquidGlassTextButton(onClick = onDismissRequest) { Text(stringResource(R.string.cancel)) }
    },
  )
}

@Composable
private fun rememberAppIconBitmap(
  context: Context,
  iconRes: Int,
  sizePx: Int,
): ImageBitmap? =
  remember(context, iconRes, sizePx) {
    runCatching {
        ContextCompat.getDrawable(context, iconRes)?.toBitmap(width = sizePx, height = sizePx)?.asImageBitmap()
      }
      .getOrNull()
  }

@Preview
@Composable
private fun Preview() {
  FDroidContent {
    Box(modifier = Modifier.fillMaxSize()) {
      AppIconDialog(
        currentAppIcon = appIcons.random(),
        onChangeIcon = {},
        onDismissRequest = {},
      )
    }
  }
}

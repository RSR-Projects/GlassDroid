package org.fdroid.ui.apps

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.fdroid.R
import org.fdroid.database.AppIssue
import org.fdroid.database.NoCompatibleSigner
import org.fdroid.database.UpdateInOtherRepo
import org.fdroid.ui.FDroidContent
import org.fdroid.ui.glass.LiquidGlassBadgedBox
import org.fdroid.ui.glass.LiquidGlassDefaults
import org.fdroid.ui.glass.LiquidGlassShapes
import org.fdroid.ui.glass.appHeroSharedBounds
import org.fdroid.ui.glass.appHeroSharedElement
import org.fdroid.ui.glass.iconPopEntrance
import org.fdroid.ui.utils.AsyncShimmerImage
import org.fdroid.ui.utils.BadgeIcon
import org.fdroid.ui.utils.Names

@Composable
fun InstalledAppRow(
  app: MyInstalledAppItem,
  isSelected: Boolean,
  modifier: Modifier = Modifier,
  appIssue: AppIssue? = null,
) {
  val selectedColor = LiquidGlassDefaults.palette().selectedPillTint
  ListItem(
    leadingContent = {
      LiquidGlassBadgedBox(
        badge = {
          if (appIssue != null)
            BadgeIcon(
              icon = Icons.Filled.Error,
              color =
                if (appIssue is UpdateInOtherRepo) {
                  MaterialTheme.colorScheme.inverseSurface
                } else {
                  MaterialTheme.colorScheme.error
                },
              contentDescription = stringResource(R.string.my_apps_header_apps_with_issue),
            )
        }
      ) {
        AsyncShimmerImage(
          model = app.iconModel,
          error = painterResource(R.drawable.ic_repo_app_default),
          contentDescription = null,
          modifier =
            Modifier.size(48.dp)
              .iconPopEntrance(key = app.packageName)
              .appHeroSharedElement("app_icon_${app.packageName}")
              .clip(MaterialTheme.shapes.medium)
              .semantics { hideFromAccessibility() },
        )
      }
    },
    headlineContent = {
      Text(
        text = app.name,
        modifier = Modifier.appHeroSharedElement("app_title_${app.packageName}"),
      )
    },
    supportingContent = { Text(app.installedVersionName) },
    colors =
      ListItemDefaults.colors(
        containerColor = if (isSelected) selectedColor else Color.Transparent
      ),
    modifier =
      modifier.appHeroSharedBounds(
        key = "app_card_${app.packageName}",
        clipShape = LiquidGlassShapes.SmallCardShape,
      ),
  )
}

@Preview
@Composable
fun InstalledAppRowPreview() {
  val app =
    InstalledAppItem(
      packageName = "",
      name = Names.randomName,
      installedVersionName = "1.0.1",
      installedVersionCode = 10001,
      lastUpdated = System.currentTimeMillis() - 5000,
    )
  FDroidContent {
    Column {
      InstalledAppRow(app, false)
      InstalledAppRow(app, true)
      InstalledAppRow(app, false, appIssue = UpdateInOtherRepo(2L))
      InstalledAppRow(app, false, appIssue = NoCompatibleSigner())
    }
  }
}

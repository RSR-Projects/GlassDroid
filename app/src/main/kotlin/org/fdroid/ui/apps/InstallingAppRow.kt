package org.fdroid.ui.apps

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Icon
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.fdroid.R
import org.fdroid.install.InstallState
import org.fdroid.ui.FDroidContent
import org.fdroid.ui.glass.LiquidCompletionCheck
import org.fdroid.ui.glass.LiquidGlassCircularProgress
import org.fdroid.ui.glass.LiquidGlassDefaults
import org.fdroid.ui.glass.LiquidGlassShapes
import org.fdroid.ui.glass.LocalReducedMotion
import org.fdroid.ui.glass.appHeroSharedBounds
import org.fdroid.ui.glass.appHeroSharedElement
import org.fdroid.ui.utils.AsyncShimmerImage
import org.fdroid.ui.utils.VersionLine

@Composable
fun InstallingAppRow(app: InstallingAppItem, isSelected: Boolean, modifier: Modifier = Modifier) {
  val selectedColor = LiquidGlassDefaults.palette().selectedPillTint
  val reducedMotion = LocalReducedMotion.current
  ListItem(
    leadingContent = {
      AsyncShimmerImage(
        model = app.iconModel,
        error = painterResource(R.drawable.ic_repo_app_default),
        contentDescription = null,
        modifier =
          Modifier.size(48.dp)
            .appHeroSharedElement("app_icon_${app.packageName}")
            .clip(MaterialTheme.shapes.medium),
      )
    },
    headlineContent = {
      Text(
        text = app.name,
        modifier = Modifier.appHeroSharedElement("app_title_${app.packageName}"),
      )
    },
    supportingContent = {
      VersionLine(app.installState.currentVersionName, app.installState.versionName)
    },
    trailingContent = {
      if (app.installState is InstallState.Installed) {
        val installedDesc = stringResource(R.string.app_installed)
        LiquidCompletionCheck(
          reducedMotion = reducedMotion,
          modifier =
            Modifier.padding(6.dp)
              .size(28.dp)
              .semantics { contentDescription = installedDesc },
        )
      } else if (app.installState is InstallState.Error) {
        val desc = stringResource(R.string.notification_title_summary_install_error)
        Icon(
          imageVector = Icons.Default.ErrorOutline,
          contentDescription = desc,
          tint = MaterialTheme.colorScheme.error,
          modifier = Modifier.padding(8.dp),
        )
      } else {
        if (app.installState is InstallState.Downloading) {
          LiquidGlassCircularProgress(progress = { app.installState.progress })
        } else {
          LiquidGlassCircularProgress()
        }
      }
    },
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
private fun Preview() {
  val installingApp1 =
    InstallingAppItem(
      packageName = "A1",
      installState =
        InstallState.Downloading(
          name = "Installing App 1",
          versionName = "1.0.4",
          currentVersionName = null,
          lastUpdated = 23,
          iconModel = null,
          downloadedBytes = 25,
          totalBytes = 100,
          startMillis = System.currentTimeMillis(),
        ),
    )
  val installingApp2 =
    InstallingAppItem(
      packageName = "A2",
      installState =
        InstallState.Installed(
          name = "Installing App 2",
          versionName = "2.0.1",
          currentVersionName = "2.0.0",
          lastUpdated = 13,
          iconModel = null,
        ),
    )
  val installingApp3 =
    InstallingAppItem(
      packageName = "A3",
      installState =
        InstallState.Error(
          msg = "error msg",
          name = "Installing App 2",
          versionName = "0.0.4",
          currentVersionName = "0.0.1",
          lastUpdated = 13,
          iconModel = null,
        ),
    )
  FDroidContent {
    Column {
      InstallingAppRow(installingApp1, false)
      InstallingAppRow(installingApp2, true)
      InstallingAppRow(installingApp3, false)
    }
  }
}

@Preview(locale = "fa")
@Composable
private fun PreviewRtl() {
  val installingApp1 =
    InstallingAppItem(
      packageName = "A1",
      installState =
        InstallState.Downloading(
          name = "Installing App 1",
          versionName = "1.0.4",
          currentVersionName = "1.0.3",
          lastUpdated = 23,
          iconModel = null,
          downloadedBytes = 25,
          totalBytes = 100,
          startMillis = System.currentTimeMillis(),
        ),
    )
  val installingApp2 =
    InstallingAppItem(
      packageName = "A2",
      installState =
        InstallState.Downloading(
          name = "Installing App 2",
          versionName = "2.1.2",
          currentVersionName = null,
          lastUpdated = 23,
          iconModel = null,
          downloadedBytes = 25,
          totalBytes = 100,
          startMillis = System.currentTimeMillis(),
        ),
    )
  FDroidContent {
    Column {
      InstallingAppRow(installingApp1, false)
      InstallingAppRow(installingApp2, false)
    }
  }
}

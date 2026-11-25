package org.fdroid.ui.details

import android.content.res.Configuration
import android.text.format.Formatter
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement.SpaceBetween
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil3.compose.AsyncImage
import kotlin.math.roundToLong
import org.fdroid.R
import org.fdroid.download.NetworkState
import org.fdroid.install.InstallState
import org.fdroid.ui.FDroidContent
import org.fdroid.ui.categories.ChipFlowRow
import org.fdroid.ui.categories.chipHeight
import org.fdroid.ui.glass.LiquidDownloadControl
import org.fdroid.ui.glass.LiquidGlassBadgedBox
import org.fdroid.ui.glass.LiquidGlassChip
import org.fdroid.ui.glass.LiquidGlassLinearProgress
import org.fdroid.ui.glass.LiquidSprings
import org.fdroid.ui.glass.appHeroSharedElement
import org.fdroid.ui.glass.liquidParallax
import org.fdroid.ui.lists.AppListType
import org.fdroid.ui.navigation.NavigationKey
import org.fdroid.ui.utils.AsyncShimmerImage
import org.fdroid.ui.utils.InstalledBadge
import org.fdroid.ui.utils.MeteredConnectionDialog
import org.fdroid.ui.utils.OfflineBar
import org.fdroid.ui.utils.asRelativeTimeString
import org.fdroid.ui.utils.getRepository
import org.fdroid.ui.utils.testApp

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
/** Timestamp [now] gets passed in for screenshot tests to have a stable download speed. */
fun AppDetailsHeader(
  item: LoadedAppDetailsItem,
  onNav: (NavigationKey) -> Unit,
  innerPadding: PaddingValues,
  now: Long = System.currentTimeMillis(),
  scrollOffsetPx: () -> Float = { 0f },
) {
  val context = LocalContext.current
  Box {
    Spacer(modifier = Modifier.padding(top = innerPadding.calculateTopPadding()))
    var showFeatureGraphic by remember { mutableStateOf(true) }
    item.featureGraphic?.let { featureGraphic ->
      AsyncImage(
        model = featureGraphic,
        contentDescription = null,
        contentScale = ContentScale.FillWidth,
        onError = { showFeatureGraphic = false },
        modifier =
          Modifier.fillMaxWidth()
            .animateContentSize()
            .height(if (showFeatureGraphic) 196.dp else 0.dp)
            .liquidParallax(scrollOffsetPx, factor = 0.24f, maxOffsetPx = 56f)
            .graphicsLayer { alpha = 0.9f }
            .drawWithContent {
              val colors = listOf(Color.Black, Color.Transparent)
              drawContent()
              drawRect(brush = Brush.verticalGradient(colors), blendMode = BlendMode.DstIn)
            }
            .semantics { hideFromAccessibility() },
      )
    }
  }
  var showMeteredDialog by remember { mutableStateOf(false) }
  AnimatedVisibility(
    visible = item.mainButtonState == MainButtonState.LOADING,
    enter = LiquidSprings.expandEnter(),
    exit = LiquidSprings.expandExit(),
  ) {
    LiquidGlassLinearProgress(modifier = Modifier.fillMaxWidth().absoluteOffset(y = (-8).dp))
  }
  // Offline bar, if no internet
  if (!item.networkState.isOnline) {
    OfflineBar(modifier = Modifier.absoluteOffset(y = (-8).dp))
  }
  // Header
  val version = item.suggestedVersion ?: item.versions?.firstOrNull()?.version
  Row(
    modifier = Modifier.padding(horizontal = 16.dp),
    horizontalArrangement = spacedBy(16.dp),
    verticalAlignment = CenterVertically,
  ) {
    LiquidGlassBadgedBox(badge = { if (item.installedVersionCode != null) InstalledBadge() }) {
      AsyncShimmerImage(
        model = item.icon,
        contentDescription = "",
        contentScale = ContentScale.Crop,
        error = painterResource(R.drawable.ic_repo_app_default),
        modifier =
          Modifier.size(64.dp)
            .appHeroSharedElement("app_icon_${item.app.packageName}")
            .clip(MaterialTheme.shapes.large)
            .semantics { hideFromAccessibility() },
      )
    }
    Column {
      SelectionContainer {
        Text(
          item.name,
          style = MaterialTheme.typography.headlineSmallEmphasized,
          fontWeight = FontWeight.Medium,
          lineHeight = 26.sp,
          modifier = Modifier.appHeroSharedElement("app_title_${item.app.packageName}"),
        )
      }
      item.app.authorName?.let { authorName ->
        SelectionContainer {
          if (item.authorHasMoreThanOneApp) {
            val title = stringResource(R.string.app_list_author, authorName)
            val authorString = stringResource(R.string.by_author_format, authorName)
            val startIndex = authorString.indexOf(authorName)
            val annotatedString = buildAnnotatedString {
              append(authorString)
              addLink(
                clickable =
                  LinkAnnotation.Clickable(
                    tag = "author",
                    linkInteractionListener = {
                      onNav(NavigationKey.AppList(AppListType.Author(title, authorName)))
                    },
                    styles =
                      TextLinkStyles(
                        style =
                          SpanStyle(
                            color = MaterialTheme.colorScheme.primary,
                            textDecoration = TextDecoration.None,
                          )
                      ),
                  ),
                start = startIndex,
                end = startIndex + authorName.length,
              )
            }
            Text(text = annotatedString, style = MaterialTheme.typography.bodyMedium)
          } else {
            Text(
              text = stringResource(R.string.by_author_format, authorName),
              style = MaterialTheme.typography.bodyMedium,
            )
          }
        }
      }
      val lastUpdated = item.app.lastUpdated.asRelativeTimeString()
      val size = version?.size?.let { size -> Formatter.formatFileSize(LocalContext.current, size) }
      SelectionContainer {
        Text(
          text =
            if (size == null) {
              stringResource(R.string.last_updated, lastUpdated)
            } else {
              stringResource(R.string.last_updated_with_size, lastUpdated, size)
            },
          style = MaterialTheme.typography.bodyMedium,
        )
      }
    }
  }
  // Summary
  item.summary?.let { summary ->
    SelectionContainer {
      Text(
        text = summary,
        style = MaterialTheme.typography.bodyLargeEmphasized,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
      )
    }
  }
  // Category chips
  if (!item.categories.isNullOrEmpty())
    ChipFlowRow(modifier = Modifier.padding(start = 8.dp)) {
      item.categories.forEach { categoryItem ->
        LiquidGlassChip(
          label = {
            Text(text = categoryItem.name, maxLines = 2, overflow = TextOverflow.Ellipsis)
          },
          onClick = {
            val categoryNav = AppListType.Category(categoryItem.name, categoryItem.id)
            onNav(NavigationKey.AppList(categoryNav))
          },
          modifier = Modifier.heightIn(min = chipHeight),
        )
      }
    }
  // Repo Chooser
  RepoChooser(
    repos = item.repositories,
    currentRepoId = item.app.repoId,
    preferredRepoId = item.preferredRepoId,
    proxy = item.proxy,
    onRepoChanged = item.actions.onRepoChanged,
    onPreferredRepoChanged = item.actions.onPreferredRepoChanged,
    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
  )
  // check user confirmation ON_RESUME to work around Android bug
  val lifecycleOwner = LocalLifecycleOwner.current
  val currentInstallState by rememberUpdatedState(item.installState)
  var numChecks by remember { mutableIntStateOf(0) }
  DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_RESUME) {
        val state = currentInstallState
        if (state is InstallState.UserConfirmationNeeded && numChecks < 3) {
          Log.i("AppDetailsHeader", "Resumed ($numChecks). Checking user confirmation... $state")
          // there's annoying installer bugs where it doesn't tell us about errors,
          // and we would run into infinite UI loops here, so there's a counter.
          numChecks += 1
          item.actions.checkUserConfirmation(state)
        } else if (state is InstallState.UserConfirmationNeeded) {
          // we tried three times, so cancel install now
          Log.i("AppDetailsHeader", "Cancel installation")
          item.actions.cancelInstall()
        }
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
  }
  // Morphing Liquid Download, Progress, Completion & Action Control
  LiquidDownloadControl(
    item = item,
    now = now,
    onShowMeteredDialog = { showMeteredDialog = true },
  )
  if (showMeteredDialog)
    MeteredConnectionDialog(
      numBytes = version?.size,
      onConfirm = { notWarnWhenMetered ->
        if (notWarnWhenMetered) item.actions.onNotWarnWhenMetered()
        require(item.suggestedVersion != null) { "suggestedVersion was null" }
        item.actions.installAction(item.app, item.suggestedVersion, item.icon)
      },
      onDismiss = { showMeteredDialog = false },
    )
}

@Preview
@Composable
fun AppDetailsHeaderPreview() {
  FDroidContent { Column { AppDetailsHeader(testApp, {}, PaddingValues(top = 8.dp)) } }
}

@Preview
@Composable
private fun InstallPreview() {
  FDroidContent {
    Column {
      AppDetailsHeader(
        testApp.copy(
          installedVersion = null,
          installedVersionCode = null,
          installedVersionName = null,
          repositories = listOf(getRepository(testApp.app.repoId), getRepository()),
          preferredRepoId = 42L,
          suggestedVersion = testApp.versions?.first()?.version,
          networkState = NetworkState(true, isMetered = true),
        ),
        {},
        PaddingValues(top = 8.dp),
      )
    }
  }
}

@Preview
@Composable
private fun UpdatePreview() {
  FDroidContent {
    Column {
      AppDetailsHeader(
        testApp.copy(
          suggestedVersion = testApp.versions?.first()?.version,
          networkState = NetworkState(true, isMetered = true),
        ),
        {},
        PaddingValues(top = 8.dp),
      )
    }
  }
}

@Preview
@Composable
private fun PreviewLoading() {
  FDroidContent {
    Column {
      val app = testApp.copy(versions = null)
      AppDetailsHeader(app, {}, PaddingValues(top = 8.dp))
    }
  }
}

@Preview
@Composable
private fun PreviewDownloading() {
  FDroidContent {
    Column {
      val app =
        testApp.copy(
          installState =
            InstallState.Downloading(
              name = "",
              versionName = "",
              currentVersionName = "",
              lastUpdated = 23L,
              iconModel = null,
              downloadedBytes = 1024 * 1024 * 3,
              totalBytes = 1024 * 1024 * 8,
              startMillis = System.currentTimeMillis() - 9000,
            ),
          networkState = NetworkState(true, isMetered = true),
        )
      AppDetailsHeader(app, {}, PaddingValues(top = 8.dp))
    }
  }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun PreviewDownloadingNight() {
  FDroidContent {
    Column {
      val app =
        testApp.copy(
          installState =
            InstallState.Downloading(
              name = "",
              versionName = "",
              currentVersionName = "",
              lastUpdated = 23L,
              iconModel = null,
              downloadedBytes = 1024 * 1024 * 3,
              totalBytes = 1024 * 1024 * 8,
              startMillis = System.currentTimeMillis() - 9000,
            ),
          networkState = NetworkState(true, isMetered = true),
        )
      AppDetailsHeader(app, {}, PaddingValues(top = 8.dp))
    }
  }
}

@Preview
@Composable
private fun PreviewProgress() {
  FDroidContent {
    Column {
      val app =
        testApp.copy(
          installState = InstallState.Starting("", "", "", 23),
          networkState = NetworkState(true, isMetered = true),
        )
      AppDetailsHeader(app, {}, PaddingValues(top = 16.dp))
    }
  }
}

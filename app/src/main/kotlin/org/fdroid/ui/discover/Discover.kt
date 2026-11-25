package org.fdroid.ui.discover

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import org.fdroid.R
import org.fdroid.download.NetworkState
import org.fdroid.repo.RepoUpdateProgress
import org.fdroid.ui.FDroidContent
import org.fdroid.ui.glass.LiquidFloatingTopBar
import org.fdroid.ui.glass.LiquidGlassBadge
import org.fdroid.ui.glass.LiquidGlassBadgedBox
import org.fdroid.ui.glass.LiquidScaffold
import org.fdroid.ui.glass.reportLiquidScroll
import org.fdroid.ui.lists.AppListType
import org.fdroid.ui.navigation.NavigationKey
import org.fdroid.ui.navigation.topBarMenuItems
import org.fdroid.ui.utils.BigLoadingIndicator
import org.fdroid.ui.utils.TopAppBarButton
import org.fdroid.ui.utils.TopAppBarOverflowButton

@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
fun Discover(
  discoverModel: DiscoverModel,
  onListTap: (AppListType) -> Unit,
  onAppTap: (AppDiscoverItem) -> Unit,
  onNav: (NavKey) -> Unit,
  modifier: Modifier = Modifier,
) {
  LiquidScaffold(
    topBar = {
      LiquidFloatingTopBar(
        title = { Text(stringResource(R.string.app_name), maxLines = 1) },
        actions = {
          topBarMenuItems.forEach { dest ->
            LiquidGlassBadgedBox(
              badge = {
                val hasRepoIssues = (discoverModel as? LoadedDiscoverModel)?.hasRepoIssues == true
                if (dest.id == NavigationKey.Repos && hasRepoIssues)
                  LiquidGlassBadge(content = null, modifier = Modifier.size(8.dp))
              }
            ) {
              TopAppBarButton(
                imageVector = dest.icon,
                contentDescription = stringResource(dest.label),
                onClick = { onNav(dest.id) },
              )
            }
          }
          TopAppBarOverflowButton { onDismissRequest ->
            DiscoverOverFlowMenu {
              onDismissRequest()
              onNav(it.id)
            }
          }
        },
      )
    },
    modifier = modifier,
  ) { paddingValues ->
    when (discoverModel) {
      is FirstStartDiscoverModel ->
        FirstStart(
          networkState = discoverModel.networkState,
          repoUpdateState = discoverModel.repoUpdateState,
          modifier = Modifier.fillMaxSize().padding(paddingValues),
        )
      is LoadingDiscoverModel -> BigLoadingIndicator(Modifier.padding(paddingValues))
      is LoadedDiscoverModel -> {
        val scrollState = rememberScrollState()
        DiscoverContent(
          discoverModel = discoverModel,
          onListTap = onListTap,
          onAppTap = onAppTap,
          onNav = onNav,
          scrollOffsetPx = { scrollState.value.toFloat() },
          modifier =
            Modifier.fillMaxSize()
              .reportLiquidScroll(scrollState)
              .verticalScroll(scrollState)
              .padding(paddingValues),
        )
      }
      NoEnabledReposDiscoverModel -> {
        Box(
          contentAlignment = Alignment.Center,
          modifier = modifier.fillMaxSize().padding(paddingValues),
        ) {
          Text(
            text = stringResource(R.string.no_repos_enabled),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(16.dp),
          )
        }
      }
    }
  }
}

@Preview
@Composable
fun FirstStartDiscoverPreview() {
  FDroidContent {
    Discover(
      discoverModel =
        FirstStartDiscoverModel(
          NetworkState(true, isMetered = false),
          RepoUpdateProgress(1, true, 0.25f),
        ),
      onListTap = {},
      onAppTap = {},
      onNav = {},
    )
  }
}

@Preview
@Composable
fun LoadingDiscoverPreview() {
  FDroidContent {
    Discover(discoverModel = LoadingDiscoverModel, onListTap = {}, onAppTap = {}, onNav = {})
  }
}

@Preview
@Composable
private fun NoEnabledReposPreview() {
  FDroidContent {
    Discover(discoverModel = NoEnabledReposDiscoverModel, onListTap = {}, onAppTap = {}, onNav = {})
  }
}

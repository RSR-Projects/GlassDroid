package org.fdroid.ui.discover

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import org.fdroid.R
import org.fdroid.ui.categories.CategoryList
import org.fdroid.ui.glass.LiquidSprings
import org.fdroid.ui.glass.liquidParallax
import org.fdroid.ui.lists.AppListType

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun DiscoverContent(
  discoverModel: LoadedDiscoverModel,
  onListTap: (AppListType) -> Unit,
  onAppTap: (AppDiscoverItem) -> Unit,
  onNav: (NavKey) -> Unit,
  modifier: Modifier = Modifier,
  scrollOffsetPx: () -> Float = { 0f },
) {
  val newPkgSet = discoverModel.newApps.mapTo(HashSet()) { it.packageName }
  val updatedPkgSet = discoverModel.recentlyUpdatedApps.mapTo(HashSet()) { it.packageName }
  Column(verticalArrangement = spacedBy(18.dp), modifier = modifier) {
    AnimatedVisibility(
      visible = discoverModel.newApps.isNotEmpty(),
      enter = LiquidSprings.expandEnter(),
      exit = LiquidSprings.expandExit(),
    ) {
      val listNew = AppListType.New(stringResource(R.string.app_list_new))
      AppCarousel(
        title = listNew.title,
        apps = discoverModel.newApps,
        onTitleTap = { onListTap(listNew) },
        onAppTap = onAppTap,
        modifier = Modifier.liquidParallax(scrollOffsetPx, factor = -0.025f, maxOffsetPx = 12f),
      )
    }
    AnimatedVisibility(
      visible = !discoverModel.recentlyUpdatedApps.isEmpty(),
      enter = LiquidSprings.expandEnter(),
      exit = LiquidSprings.expandExit(),
    ) {
      val listRecentlyUpdated =
        AppListType.RecentlyUpdated(stringResource(R.string.app_list_recently_updated))
      AppCarousel(
        title = listRecentlyUpdated.title,
        apps = discoverModel.recentlyUpdatedApps,
        heroExcludePackages = newPkgSet,
        onTitleTap = { onListTap(listRecentlyUpdated) },
        onAppTap = onAppTap,
        modifier = Modifier.liquidParallax(scrollOffsetPx, factor = -0.040f, maxOffsetPx = 16f),
      )
    }
    AnimatedVisibility(
      visible = !discoverModel.mostDownloadedApps.isNullOrEmpty(),
      enter = LiquidSprings.expandEnter(),
      exit = LiquidSprings.expandExit(),
    ) {
      val listMostDownloaded =
        AppListType.MostDownloaded(stringResource(R.string.app_list_most_downloaded))
      if (discoverModel.mostDownloadedApps != null)
        AppCarousel(
          title = listMostDownloaded.title,
          apps = discoverModel.mostDownloadedApps,
          heroExcludePackages = newPkgSet + updatedPkgSet,
          onTitleTap = { onListTap(listMostDownloaded) },
          onAppTap = onAppTap,
          modifier = Modifier.liquidParallax(scrollOffsetPx, factor = -0.055f, maxOffsetPx = 20f),
        )
    }
    CategoryList(
      categoryMap = discoverModel.categories,
      onNav = onNav,
      modifier = Modifier.fillMaxWidth(),
    )
  }
}

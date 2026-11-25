package org.fdroid.ui.lists

import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ManageSearch
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition.Companion.Below
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.viktormykhailiv.compose.hints.HintProperties
import com.viktormykhailiv.compose.hints.hintAnchor
import com.viktormykhailiv.compose.hints.rememberHint
import com.viktormykhailiv.compose.hints.rememberHintAnchorState
import com.viktormykhailiv.compose.hints.rememberHintController
import org.fdroid.R
import org.fdroid.database.AppListSortOrder
import org.fdroid.ui.FDroidContent
import org.fdroid.ui.glass.AppContextMenu
import org.fdroid.ui.glass.LiquidFloatingTopBar
import org.fdroid.ui.glass.LiquidGlassBadge
import org.fdroid.ui.glass.LiquidGlassBadgedBox
import org.fdroid.ui.glass.LiquidGlassBottomSheet
import org.fdroid.ui.glass.LiquidGlassIconButton
import org.fdroid.ui.glass.LiquidGlassShapes
import org.fdroid.ui.glass.LiquidScaffold
import org.fdroid.ui.glass.liquidRowSquish
import org.fdroid.ui.glass.rememberLiquidMeltOpener
import org.fdroid.ui.glass.reportLiquidScroll
import org.fdroid.ui.glass.springListItem
import org.fdroid.ui.glass.staggeredLiquidEntrance
import org.fdroid.ui.search.TopSearchBar
import org.fdroid.ui.utils.BackButton
import org.fdroid.ui.utils.BigLoadingIndicator
import org.fdroid.ui.utils.HintOverlayContainer
import org.fdroid.ui.utils.OnboardingPopupCard
import org.fdroid.ui.utils.TopAppBarButton
import org.fdroid.ui.utils.appListItems
import org.fdroid.ui.utils.getAppListInfo

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun AppList(
  appListInfo: AppListInfo,
  currentPackageName: String?,
  modifier: Modifier = Modifier,
  onBackClicked: () -> Unit,
  onItemClick: (String) -> Unit,
) {
  var searchActive by rememberSaveable { mutableStateOf(false) }

  val hintController = rememberHintController()
  val hint =
    rememberHint(HintProperties(dismissOnClickOutside = false)) {
      OnboardingPopupCard(
        title = stringResource(R.string.onboarding_app_list_filter_title),
        message = stringResource(R.string.onboarding_app_list_filter_message),
        modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp),
        onGotIt = {
          appListInfo.actions.onOnboardingSeen()
          hintController.dismiss()
        },
      )
    }
  val hintAnchor = rememberHintAnchorState(hint)
  LaunchedEffect(appListInfo.showOnboarding) {
    if (appListInfo.showOnboarding) {
      hintController.show(hintAnchor)
    }
  }

  LiquidScaffold(
    topBar = {
      if (searchActive) {
        val onSearchCleared = { appListInfo.actions.onSearch("") }
        TopSearchBar(
          showActions = true,
          onSearch = appListInfo.actions::onSearch,
          onSearchCleared = onSearchCleared,
          onHideSearch = {
            searchActive = false
            onSearchCleared()
          },
          actions = {
            FilterButton(
              showFilterBadge = appListInfo.model.showFilterBadge,
              toggleFilterVisibility = appListInfo.actions::toggleFilterVisibility,
            )
          },
        )
      } else {
        LiquidFloatingTopBar(
          navigationIcon = {
            BackButton(onClick = { if (searchActive) searchActive = false else onBackClicked() })
          },
          title = {
            Text(
              text = appListInfo.list.title,
              maxLines = 1,
              overflow = TextOverflow.MiddleEllipsis,
            )
          },
          showActions = true,
          actions = {
            TopAppBarButton(
              imageVector = Icons.AutoMirrored.Default.ManageSearch,
              contentDescription = stringResource(R.string.menu_search),
              onClick = { searchActive = true },
            )
            FilterButton(
              showFilterBadge = appListInfo.model.showFilterBadge,
              toggleFilterVisibility = appListInfo.actions::toggleFilterVisibility,
              modifier = Modifier.hintAnchor(state = hintAnchor, shape = RoundedCornerShape(16.dp)),
            )
          },
        )
      }
    },
    modifier = modifier,
  ) { paddingValues ->
    val listState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    Column(modifier = Modifier.fillMaxSize().imePadding()) {
      val apps = appListInfo.model.apps
      if (apps == null) BigLoadingIndicator()
      else if (apps.isEmpty()) {
        Text(
          text = stringResource(R.string.search_filter_no_results),
          textAlign = TextAlign.Center,
          modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
        )
      } else {
        val haptic = LocalHapticFeedback.current
        LazyColumn(
          state = listState,
          contentPadding = paddingValues + PaddingValues(top = 8.dp),
          verticalArrangement = spacedBy(8.dp),
          modifier =
            Modifier.reportLiquidScroll(listState)
              .then(if (currentPackageName == null) Modifier else Modifier.selectableGroup()),
        ) {
          itemsIndexed(apps, key = { _, it -> it.packageName }, contentType = { _, _ -> "A" }) {
            index,
            navItem ->
            val isSelected = currentPackageName == navItem.packageName
            var menuExpanded by remember { mutableStateOf(false) }
            val (meltModifier, openWithMelt) =
              rememberLiquidMeltOpener(navItem.packageName) { onItemClick(navItem.packageName) }
            val interactionModifier =
              if (currentPackageName == null) {
                Modifier.combinedClickable(
                  onClick = openWithMelt,
                  onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    menuExpanded = true
                  },
                )
              } else {
                Modifier.selectable(
                  selected = isSelected,
                  onClick = openWithMelt,
                )
              }
            Box(
              modifier =
                Modifier.fillMaxWidth()
                  .springListItem(this)
                  .staggeredLiquidEntrance(key = navItem.packageName, index = index),
            ) {
              AppListRow(
                item = navItem,
                isSelected = isSelected,
                modifier =
                  Modifier.fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .then(meltModifier)
                    .liquidRowSquish()
                    .clip(LiquidGlassShapes.SmallCardShape)
                    .then(interactionModifier),
              )
              AppContextMenu(
                expanded = menuExpanded,
                packageName = navItem.packageName,
                appName = navItem.name,
                isInstalled = navItem.isInstalled,
                onDismissRequest = { menuExpanded = false },
                onOpenDetails = openWithMelt,
              )
            }
          }
        }
      }
      // Bottom Sheet
      if (appListInfo.showFilters) {
        LiquidGlassBottomSheet(
          onDismissRequest = { appListInfo.actions.toggleFilterVisibility() },
        ) {
          AppsFilter(info = appListInfo)
        }
      }
    }
  }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun FilterButton(
  showFilterBadge: Boolean,
  toggleFilterVisibility: () -> Unit,
  modifier: Modifier = Modifier,
) {
  TooltipBox(
    positionProvider = TooltipDefaults.rememberTooltipPositionProvider(Below),
    tooltip = { PlainTooltip { Text(stringResource(R.string.filter)) } },
    state = rememberTooltipState(),
  ) {
    LiquidGlassIconButton(
      onClick = toggleFilterVisibility,
      modifier = modifier,
    ) {
      LiquidGlassBadgedBox(
        badge = {
          if (showFilterBadge) {
            LiquidGlassBadge(containerColor = MaterialTheme.colorScheme.secondary)
          }
        }
      ) {
        Icon(
          imageVector = Icons.Filled.FilterList,
          contentDescription = stringResource(R.string.filter),
        )
      }
    }
  }
}

@Preview
@Composable
private fun Preview() {
  HintOverlayContainer {
    FDroidContent {
      val model =
        AppListModel(
          apps = appListItems,
          showFilterBadge = true,
          sortBy = AppListSortOrder.NAME,
          filterIncompatible = true,
          categories = null,
          filteredCategoryIds = emptySet(),
          antiFeatures = null,
          filteredAntiFeatureIds = emptySet(),
          repositories = emptyList(),
          filteredRepositoryIds = emptySet(),
        )
      val info = getAppListInfo(model)
      AppList(appListInfo = info, currentPackageName = null, onBackClicked = {}, onItemClick = {})
    }
  }
}

@Preview
@Composable
private fun PreviewLoading() {
  HintOverlayContainer {
    FDroidContent {
      val model =
        AppListModel(
          apps = null,
          showFilterBadge = false,
          sortBy = AppListSortOrder.NAME,
          filterIncompatible = false,
          categories = null,
          filteredCategoryIds = emptySet(),
          antiFeatures = null,
          filteredAntiFeatureIds = emptySet(),
          repositories = emptyList(),
          filteredRepositoryIds = emptySet(),
        )
      val info = getAppListInfo(model)
      AppList(appListInfo = info, currentPackageName = null, onBackClicked = {}, onItemClick = {})
    }
  }
}

@Preview
@Composable
private fun PreviewEmpty() {
  HintOverlayContainer {
    FDroidContent {
      val model =
        AppListModel(
          apps = emptyList(),
          showFilterBadge = false,
          sortBy = AppListSortOrder.NAME,
          filterIncompatible = false,
          categories = null,
          filteredCategoryIds = emptySet(),
          antiFeatures = null,
          filteredAntiFeatureIds = emptySet(),
          repositories = emptyList(),
          filteredRepositoryIds = emptySet(),
        )
      val info = getAppListInfo(model)
      AppList(appListInfo = info, currentPackageName = null, onBackClicked = {}, onItemClick = {})
    }
  }
}

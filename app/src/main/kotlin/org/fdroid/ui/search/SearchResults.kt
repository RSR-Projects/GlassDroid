package org.fdroid.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.fdroid.R
import org.fdroid.search.SavedSearch
import org.fdroid.search.SearchHelper.isSearchable
import org.fdroid.ui.categories.CategoryChip
import org.fdroid.ui.categories.CategoryItem
import org.fdroid.ui.categories.ChipFlowRow
import org.fdroid.ui.glass.AppContextMenu
import org.fdroid.ui.glass.LiquidGlassDivider
import org.fdroid.ui.glass.LiquidGlassShapes
import org.fdroid.ui.glass.liquidRowSquish
import org.fdroid.ui.glass.rememberLiquidMeltOpener
import org.fdroid.ui.glass.reportLiquidScroll
import org.fdroid.ui.glass.springListItem
import org.fdroid.ui.glass.staggeredLiquidEntrance
import org.fdroid.ui.lists.AppListItem
import org.fdroid.ui.lists.AppListRow
import org.fdroid.ui.lists.AppListType
import org.fdroid.ui.navigation.NavigationKey
import org.fdroid.ui.utils.BigLoadingIndicator

data class SearchResults(val apps: List<AppListItem>, val categories: List<CategoryItem>)

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun SearchResults(
  paddingValues: PaddingValues,
  searchResults: SearchResults?,
  textFieldState: TextFieldState,
  savedSearches: List<SavedSearch>?,
  categories: List<CategoryItem>?,
  onClearSavedSearches: () -> Unit,
  onNav: (NavigationKey) -> Unit,
  modifier: Modifier = Modifier,
) {
  // rememberLazyListState done differently, so it refreshes for different searchResults
  val listState =
    rememberSaveable(searchResults, saver = LazyListState.Saver) { LazyListState(0, 0) }
  if (searchResults == null) {
    if (textFieldState.text.isSearchable()) {
      BigLoadingIndicator(modifier.padding(paddingValues).imePadding())
    } else {
      val keyboardController = LocalSoftwareKeyboardController.current
      LazyColumn(modifier = modifier.fillMaxSize(), contentPadding = paddingValues) {
        if (!savedSearches.isNullOrEmpty()) {
          pastSearches(
            savedSearches = savedSearches,
            onSearch = {
              textFieldState.edit { replace(0, length, it) }
              keyboardController?.hide()
            },
            onClearSavedSearches = onClearSavedSearches,
          )
        }
        if (!categories.isNullOrEmpty()) {
          item(key = "categories") {
            CategoriesFlowRow(
              categories = categories,
              onNav = onNav,
              modifier = Modifier.springListItem(this).padding(top = 16.dp),
            )
          }
        }
        item { Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.systemBars)) }
        item { Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.ime)) }
      }
    }
  } else if (searchResults.apps.isEmpty() && textFieldState.text.isSearchable()) {
    Column(modifier = modifier.padding(paddingValues)) {
      if (searchResults.categories.isNotEmpty()) {
        CategoriesFlowRow(searchResults.categories, onNav)
      }
      Text(
        text = stringResource(R.string.search_no_results),
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(16.dp).fillMaxWidth(),
      )
    }
  } else {
    val haptic = LocalHapticFeedback.current
    LazyColumn(
      state = listState,
      contentPadding = paddingValues,
      modifier = modifier.fillMaxSize().reportLiquidScroll(listState),
    ) {
      if (searchResults.categories.isNotEmpty()) {
        item(key = "categories", contentType = "category") {
          CategoriesFlowRow(
            categories = searchResults.categories,
            onNav = onNav,
            modifier = Modifier.springListItem(this),
          )
        }
      }
      if (searchResults.apps.isNotEmpty()) {
        item(key = "appsHeader", contentType = "appsHeader") {
          Column(modifier = Modifier.springListItem(this)) {
            if (searchResults.categories.isNotEmpty()) {
              LiquidGlassDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp),
              )
            }
            Text(
              text = stringResource(R.string.apps),
              style = MaterialTheme.typography.labelLarge,
              modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp),
            )
          }
        }
      }
      itemsIndexed(
        searchResults.apps,
        key = { _, it -> it.packageName },
        contentType = { _, _ -> "app" },
      ) { index, item ->
        var menuExpanded by remember { mutableStateOf(false) }
        val (meltModifier, openWithMelt) =
          rememberLiquidMeltOpener(item.packageName) {
            onNav(NavigationKey.AppDetails(item.packageName))
          }
        Box(
          modifier =
            Modifier.fillMaxWidth()
              .springListItem(this)
              .staggeredLiquidEntrance(key = item.packageName, index = index),
        ) {
          AppListRow(
            item = item,
            isSelected = false,
            showLastUpdated = true,
            modifier =
              Modifier.fillMaxWidth()
                .padding(horizontal = 8.dp)
                .then(meltModifier)
                .liquidRowSquish()
                .clip(LiquidGlassShapes.SmallCardShape)
                .combinedClickable(
                  onClick = openWithMelt,
                  onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    menuExpanded = true
                  },
                ),
          )
          AppContextMenu(
            expanded = menuExpanded,
            packageName = item.packageName,
            appName = item.name,
            isInstalled = item.isInstalled,
            onDismissRequest = { menuExpanded = false },
            onOpenDetails = openWithMelt,
          )
        }
      }
      item { Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.systemBars)) }
      item { Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.ime)) }
    }
  }
}

@Composable
private fun CategoriesFlowRow(
  categories: List<CategoryItem>,
  onNav: (NavigationKey) -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(modifier = modifier.padding(horizontal = 8.dp)) {
    Text(
      text = stringResource(R.string.main_menu__categories),
      style = MaterialTheme.typography.labelLarge,
      modifier = Modifier.padding(8.dp),
    )
    ChipFlowRow {
      categories.forEach { item ->
        CategoryChip(
          categoryItem = item,
          onClick = {
            val type = AppListType.Category(item.name, item.id)
            val navKey = NavigationKey.AppList(type)
            onNav(navKey)
          },
        )
      }
    }
  }
}

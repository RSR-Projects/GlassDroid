package org.fdroid.ui.search

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import kotlinx.coroutines.FlowPreview
import org.fdroid.ui.glass.LiquidFloatingSearchBar
import org.fdroid.ui.utils.BackButton

/**
 * Floating Liquid Glass search bar for global search and in-list filtering.
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class, FlowPreview::class)
fun TopSearchBar(
  searchFieldState: TextFieldState = rememberTextFieldState(),
  shouldRequestFocus: Boolean = true,
  showActions: Boolean = false,
  actions: @Composable (RowScope.() -> Unit) = {},
  onSearch: suspend (String) -> Unit,
  onSearchCleared: () -> Unit,
  onHideSearch: () -> Unit,
) {
  val focusRequester = remember { FocusRequester() }
  val keyboardController = LocalSoftwareKeyboardController.current
  LiquidFloatingSearchBar(
    navigationIcon = { BackButton(onClick = onHideSearch) },
    searchField = {
      AppSearchInputField(
        searchBarState = rememberSearchBarState(),
        textFieldState = searchFieldState,
        onSearch = onSearch,
        onSearchCleared = {
          searchFieldState.setTextAndPlaceCursorAtEnd("")
          onSearchCleared()
        },
        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
      )
    },
    showActions = showActions,
    actions = actions,
  )
  LaunchedEffect(shouldRequestFocus) {
    if (shouldRequestFocus) {
      focusRequester.requestFocus()
      keyboardController?.show()
    }
  }
}

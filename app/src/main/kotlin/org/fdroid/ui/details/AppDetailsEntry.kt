package org.fdroid.ui.details

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.runtime.CompositionLocalProvider
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import org.fdroid.ui.glass.LocalAppAnimatedContentScope
import org.fdroid.ui.navigation.NavigationKey
import org.fdroid.ui.navigation.Navigator

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
fun EntryProviderScope<NavKey>.appDetailsEntry(navigator: Navigator, isBigScreen: Boolean) {
  entry<NavigationKey.AppDetails>(metadata = ListDetailSceneStrategy.detailPane("appdetails")) {
    CompositionLocalProvider(LocalAppAnimatedContentScope provides LocalNavAnimatedContentScope.current) {
      val viewModel =
        hiltViewModel<AppDetailsViewModel, AppDetailsViewModel.Factory>(
          creationCallback = { factory -> factory.create(it.packageName) }
        )
      AppDetails(
        item = viewModel.appDetails.collectAsStateWithLifecycle().value,
        onNav = { navKey -> navigator.navigate(navKey) },
        onBackNav =
          if (isBigScreen) null
          else {
            { navigator.goBack() }
          },
      )
    }
  }
}

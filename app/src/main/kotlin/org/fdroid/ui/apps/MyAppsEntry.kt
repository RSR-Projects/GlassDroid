package org.fdroid.ui.apps

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
fun EntryProviderScope<NavKey>.myAppsEntry(navigator: Navigator, isBigScreen: Boolean) {
  entry<NavigationKey.MyApps>(metadata = ListDetailSceneStrategy.listPane("appdetails")) {
    CompositionLocalProvider(LocalAppAnimatedContentScope provides LocalNavAnimatedContentScope.current) {
      val myAppsViewModel = hiltViewModel<MyAppsViewModel>()
      val myAppsInfo =
        object : MyAppsInfo {
          override val model = myAppsViewModel.myAppsModel.collectAsStateWithLifecycle().value
          override val actions: MyAppsActions = myAppsViewModel
        }
      MyApps(
        myAppsInfo = myAppsInfo,
        currentPackageName =
          if (isBigScreen) {
            (navigator.last as? NavigationKey.AppDetails)?.packageName
          } else null,
        onAppItemClick = {
          val new = NavigationKey.AppDetails(it)
          if (navigator.last is NavigationKey.AppDetails) {
            navigator.replaceLast(new)
          } else {
            navigator.navigate(new)
          }
        },
        onNav = { navKey -> navigator.navigate(navKey) },
      )
    }
  }
}

package org.fdroid.ui.repositories

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.content.res.Configuration.UI_MODE_TYPE_NORMAL
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.viktormykhailiv.compose.hints.HintProperties
import com.viktormykhailiv.compose.hints.hintAnchor
import com.viktormykhailiv.compose.hints.rememberHint
import com.viktormykhailiv.compose.hints.rememberHintAnchorState
import com.viktormykhailiv.compose.hints.rememberHintController
import org.fdroid.R
import org.fdroid.download.NetworkState
import org.fdroid.ui.FDroidContent
import org.fdroid.ui.glass.LiquidFloatingActionButton
import org.fdroid.ui.glass.LiquidFloatingTopBar
import org.fdroid.ui.glass.LiquidScaffold
import org.fdroid.ui.glass.LocalLiquidGlassConfig
import org.fdroid.ui.utils.BackButton
import org.fdroid.ui.utils.BigLoadingIndicator
import org.fdroid.ui.utils.HintOverlayContainer
import org.fdroid.ui.utils.OnboardingPopupCard
import org.fdroid.ui.utils.getRepositoriesInfo
import org.fdroid.ui.utils.repoItems

@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
fun Repositories(info: RepositoryInfo, isBigScreen: Boolean, onBackClicked: () -> Unit) {
  val hintController = rememberHintController()
  val hint =
    rememberHint(HintProperties(dismissOnClickOutside = false)) {
      Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        OnboardingPopupCard(
          title = stringResource(R.string.repo_list_info_title),
          message = stringResource(R.string.repo_list_info_text),
          modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp),
          onGotIt = {
            info.onOnboardingSeen()
            hintController.dismiss()
          },
        )
      }
    }
  val hintAnchor = rememberHintAnchorState(hint)
  LaunchedEffect(info.model.showOnboarding) {
    if (!isBigScreen && info.model.showOnboarding) {
      hintController.show(hintAnchor)
    }
  }
  val listState = rememberLazyListState()
  val showFab by remember {
    derivedStateOf {
      val firstVisibleItemIndex = listState.firstVisibleItemIndex
      val firstVisibleItemScrollOffset = listState.firstVisibleItemScrollOffset
      if (firstVisibleItemIndex == 0 && firstVisibleItemScrollOffset == 0) {
        true
      } else if (listState.isScrollInProgress) {
        false
      } else {
        listState.canScrollForward
      }
    }
  }
  LiquidScaffold(
    topBar = {
      LiquidFloatingTopBar(
        navigationIcon = { BackButton(onClick = onBackClicked) },
        title = { Text(stringResource(R.string.app_details_repositories)) },
        subtitle = {
          val lastUpdated = info.model.lastCheckForUpdate
          Text(stringResource(R.string.repo_last_update_check, lastUpdated))
        },
        showActions = false,
      )
    },
    floatingActionButton = {
      val glassConfig = LocalLiquidGlassConfig.current
      AnimatedVisibility(
        visible = showFab,
        enter =
          fadeIn(glassConfig.smoothSpatialSpring()) +
            scaleIn(initialScale = 0.8f, animationSpec = glassConfig.bouncySpatialSpring()),
        exit =
          fadeOut(glassConfig.smoothSpatialSpring()) +
            scaleOut(targetScale = 0.8f, animationSpec = glassConfig.smoothSpatialSpring()),
      ) {
        LiquidFloatingActionButton(onClick = info::onAddRepo) {
          Icon(
            imageVector = Icons.Default.Add,
            contentDescription = stringResource(R.string.menu_add_repo),
          )
        }
      }
    },
    modifier = Modifier.hintAnchor(hintAnchor, fullScreen = true),
  ) { paddingValues ->
    if (info.model.repositories == null) BigLoadingIndicator()
    else
      RepositoriesList(
        info = info,
        listState = listState,
        // we split up top and bottom padding to not cause bugs with list drag and drop
        paddingValues =
          PaddingValues(
            start = paddingValues.calculateStartPadding(LocalLayoutDirection.current),
            end = paddingValues.calculateEndPadding(LocalLayoutDirection.current),
            bottom = paddingValues.calculateBottomPadding(),
          ),
        modifier = Modifier.padding(top = paddingValues.calculateTopPadding()),
      )
  }
}

@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES or UI_MODE_TYPE_NORMAL)
@Composable
private fun RepositoriesScaffoldLoadingPreview() {
  HintOverlayContainer {
    FDroidContent {
      val model =
        RepositoryModel(
          repositories = null,
          showOnboarding = false,
          lastCheckForUpdate = "never",
          networkState = NetworkState(isOnline = true, isMetered = false),
        )
      val info = getRepositoriesInfo(model)
      Repositories(info, true) {}
    }
  }
}

@Composable
@PreviewLightDark
private fun RepositoriesScaffoldPreview() {
  HintOverlayContainer {
    FDroidContent {
      val model =
        RepositoryModel(
          repositories = repoItems,
          showOnboarding = false,
          lastCheckForUpdate = "42min. ago",
          networkState = NetworkState(isOnline = true, isMetered = false),
        )
      val info = getRepositoriesInfo(model, repoItems[0].repoId)
      Repositories(info, true) {}
    }
  }
}

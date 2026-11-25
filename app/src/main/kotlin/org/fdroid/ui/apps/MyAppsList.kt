package org.fdroid.ui.apps

import androidx.annotation.RestrictTo
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.fdroid.R
import org.fdroid.database.NotAvailable
import org.fdroid.install.InstallState
import org.fdroid.ui.FDroidContent
import org.fdroid.ui.glass.AppContextMenu
import org.fdroid.ui.glass.LiquidGlassButton
import org.fdroid.ui.glass.LiquidGlassCard
import org.fdroid.ui.glass.LiquidGlassShapes
import org.fdroid.ui.glass.LiquidGlassTextButton
import org.fdroid.ui.glass.liquidRowSquish
import org.fdroid.ui.glass.rememberLiquidMeltOpener
import org.fdroid.ui.glass.reportLiquidScroll
import org.fdroid.ui.glass.springListItem
import org.fdroid.ui.utils.MeteredConnectionDialog
import org.fdroid.ui.utils.OfflineBar
import org.fdroid.ui.utils.OnboardingCard
import org.fdroid.ui.utils.getMyAppsInfo
import org.fdroid.ui.utils.myAppsModel

@Composable
fun MyAppsList(
  myAppsInfo: MyAppsInfo,
  currentPackageName: String?,
  lazyListState: LazyListState,
  onAppItemClick: (String) -> Unit,
  paddingValues: PaddingValues,
  modifier: Modifier = Modifier,
) {
  val updatableApps = myAppsInfo.model.appUpdates
  val installingApps = myAppsInfo.model.installingApps
  val appsWithIssue = myAppsInfo.model.appsWithIssue
  val installedApps = myAppsInfo.model.installedApps
  // scroll to top if new updatable apps were added (saveable, so doesn't scroll on screen rotation)
  var previousNumUpdates by rememberSaveable { mutableIntStateOf(0) }
  LaunchedEffect(updatableApps) {
    if (!updatableApps.isNullOrEmpty()) {
      if (updatableApps.size > previousNumUpdates) {
        lazyListState.animateScrollToItem(0)
      }
      previousNumUpdates = updatableApps.size
    }
  }
  // allow us to hide "update all" button to avoid user pressing it twice
  var showUpdateAllButton by remember(updatableApps) { mutableStateOf(true) }
  var showMeteredDialog by remember { mutableStateOf<(() -> Unit)?>(null) }
  var showIssueIgnoreDialog by remember { mutableStateOf<AppWithIssueItem?>(null) }

  val pullToRefreshState = rememberPullToRefreshState()
  PullToRefreshBox(
    state = pullToRefreshState,
    indicator = {
      // custom indicator to render it below top app bar
      Indicator(
        modifier =
          Modifier.align(Alignment.TopCenter).padding(top = paddingValues.calculateTopPadding()),
        isRefreshing = myAppsInfo.model.reposUpdating,
        state = pullToRefreshState,
      )
    },
    threshold = PullToRefreshDefaults.PositionalThreshold + 42.dp,
    isRefreshing = myAppsInfo.model.reposUpdating,
    onRefresh = {
      val refreshLambda = { myAppsInfo.actions.checkForUpdates() }
      if (myAppsInfo.model.networkState.showWarningDialog) {
        showMeteredDialog = refreshLambda
      } else {
        refreshLambda()
      }
    },
    modifier = Modifier.fillMaxSize(),
  ) {
    LazyColumn(
      state = lazyListState,
      contentPadding = paddingValues,
      modifier =
        modifier
          .reportLiquidScroll(lazyListState)
          .then(if (currentPackageName == null) Modifier else Modifier.selectableGroup()),
    ) {
      // Updates header with Update all button
      if (
        updatableApps != null &&
          updatableApps.isEmpty() &&
          myAppsInfo.model.showUpdatesHint &&
          !myAppsInfo.model.isSearching
      ) {
        item(key = "A-hint", contentType = "hint") {
          OnboardingCard(
            title = stringResource(R.string.apps_updates_hint_title),
            message = stringResource(R.string.apps_updates_hint_text),
            buttonText = stringResource(R.string.hide),
            onGotIt = { myAppsInfo.actions.onUpdatesHintSeen() },
            modifier = Modifier.springListItem(this).padding(horizontal = 16.dp, vertical = 8.dp),
          )
        }
      } else if (!updatableApps.isNullOrEmpty()) {
        if (!myAppsInfo.model.networkState.isOnline) {
          item(key = "OfflineBar", contentType = "offlineBar") {
            OfflineBar(modifier = Modifier.springListItem(this))
          }
        }
        item(key = "A", contentType = "header") {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.springListItem(this),
          ) {
            Text(
              text = stringResource(R.string.updates),
              style = MaterialTheme.typography.titleMedium,
              modifier = Modifier.padding(16.dp).weight(1f),
            )
            if (showUpdateAllButton)
              LiquidGlassButton(
                onClick = {
                  val installLambda = {
                    myAppsInfo.actions.updateAll()
                    showUpdateAllButton = false
                  }
                  if (myAppsInfo.model.networkState.showWarningDialog) {
                    showMeteredDialog = installLambda
                  } else {
                    installLambda()
                  }
                },
                modifier = Modifier.padding(end = 16.dp),
              ) {
                Text(stringResource(R.string.update_all))
              }
          }
        }
        // List of updatable apps
        items(items = updatableApps, key = { it.packageName }, contentType = { "A" }) { app ->
          val isSelected = app.packageName == currentPackageName
          val (meltModifier, openWithMelt) =
            rememberLiquidMeltOpener(app.packageName) { onAppItemClick(app.packageName) }
          val interactionModifier =
            if (currentPackageName == null) {
              Modifier.clickable(onClick = openWithMelt)
            } else {
              Modifier.selectable(
                selected = isSelected,
                onClick = openWithMelt,
              )
            }
          val rowModifier =
            Modifier.springListItem(this)
              .padding(horizontal = 8.dp)
              .then(meltModifier)
              .liquidRowSquish()
              .clip(LiquidGlassShapes.SmallCardShape)
              .then(interactionModifier)
          UpdatableAppRow(app, isSelected, rowModifier)
        }
      }
      // Apps currently installing header
      if (installingApps.isNotEmpty()) {
        item(key = "B", contentType = "header") {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.springListItem(this),
          ) {
            Text(
              text =
                if (myAppsInfo.model.showClearInstallingAppsButton) {
                  stringResource(R.string.my_apps_header_recently_installed_apps)
                } else {
                  stringResource(R.string.notification_title_summary_installing)
                },
              style = MaterialTheme.typography.titleMedium,
              modifier = Modifier.padding(16.dp).weight(1f),
            )
            if (myAppsInfo.model.showClearInstallingAppsButton) {
              LiquidGlassTextButton(
                onClick = myAppsInfo.actions::clearInstallingApps,
                modifier = Modifier.padding(end = 16.dp),
              ) {
                Text(stringResource(R.string.clear_all))
              }
            }
          }
        }
        // List of currently installing apps
        items(items = installingApps, key = { it.packageName }, contentType = { "B" }) { app ->
          val isSelected = app.packageName == currentPackageName
          val (meltModifier, openWithMelt) =
            rememberLiquidMeltOpener(app.packageName) { onAppItemClick(app.packageName) }
          val interactionModifier =
            if (currentPackageName == null) {
              Modifier.clickable(onClick = openWithMelt)
            } else {
              Modifier.selectable(
                selected = isSelected,
                onClick = openWithMelt,
              )
            }
          val rowModifier =
            Modifier.springListItem(this)
              .padding(horizontal = 8.dp)
              .then(meltModifier)
              .liquidRowSquish()
              .clip(LiquidGlassShapes.SmallCardShape)
              .then(interactionModifier)
          InstallingAppRow(app, isSelected, rowModifier)
        }
      }
      // Apps with issues
      if (!appsWithIssue.isNullOrEmpty()) {
        // header
        item(key = "C", contentType = "header") {
          Text(
            text = stringResource(R.string.my_apps_header_apps_with_issue),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.springListItem(this).padding(16.dp),
          )
        }
        if (myAppsInfo.model.showAppIssueHint)
          item(key = "C-hint", contentType = "hint") {
            LiquidGlassCard(
              modifier =
                Modifier.springListItem(this)
                  .fillMaxWidth()
                  .padding(bottom = 16.dp, start = 16.dp, end = 16.dp)
            ) {
              Column(modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp)) {
                Text(
                  text = stringResource(R.string.app_issue_hint),
                  style = MaterialTheme.typography.bodyMedium,
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                  LiquidGlassTextButton(
                    onClick = { myAppsInfo.actions.onAppIssueHintSeen() },
                  ) {
                    Text(stringResource(R.string.got_it))
                  }
                }
              }
            }
          }
        // list of apps with issues
        items(items = appsWithIssue, key = { it.packageName }, contentType = { "C" }) { app ->
          val isSelected = app.packageName == currentPackageName
          var showNotAvailableDialog by remember { mutableStateOf(false) }
          val (meltModifier, openWithMelt) =
            rememberLiquidMeltOpener(app.packageName) { onAppItemClick(app.packageName) }
          val onClick = {
            if (app.issue is NotAvailable) {
              showNotAvailableDialog = true
            } else {
              openWithMelt()
            }
          }
          val interactionModifier =
            if (currentPackageName == null) {
              Modifier.combinedClickable(
                onClick = onClick,
                onLongClick = { showIssueIgnoreDialog = app },
                onLongClickLabel = stringResource(R.string.my_apps_ignore_dialog_title),
              )
            } else {
              Modifier.selectable(selected = isSelected, onClick = onClick)
            }
          val rowModifier =
            Modifier.springListItem(this)
              .padding(horizontal = 8.dp)
              .then(meltModifier)
              .liquidRowSquish()
              .clip(LiquidGlassShapes.SmallCardShape)
              .then(interactionModifier)
          InstalledAppRow(app, isSelected, rowModifier, app.issue)
          // Dialogs
          val appToIgnore = showIssueIgnoreDialog
          if (appToIgnore != null)
            IgnoreIssueDialog(
              appName = appToIgnore.name,
              onIgnore = {
                myAppsInfo.actions.ignoreAppIssue(appToIgnore)
                showIssueIgnoreDialog = null
              },
              onDismiss = { showIssueIgnoreDialog = null },
            )
          else if (showNotAvailableDialog)
            NotAvailableDialog(app.packageName) { showNotAvailableDialog = false }
        }
      }
      // List of installed apps
      if (!installedApps.isNullOrEmpty()) {
        item(key = "D", contentType = "header") {
          Text(
            text = stringResource(R.string.installed_apps__activity_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.springListItem(this).padding(16.dp),
          )
        }
        items(items = installedApps, key = { it.packageName }, contentType = { "D" }) { app ->
          val haptic = LocalHapticFeedback.current
          val isSelected = app.packageName == currentPackageName
          var menuExpanded by remember { mutableStateOf(false) }
          val (meltModifier, openWithMelt) =
            rememberLiquidMeltOpener(app.packageName) { onAppItemClick(app.packageName) }
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
          val rowModifier =
            Modifier.springListItem(this)
              .padding(horizontal = 8.dp)
              .then(meltModifier)
              .liquidRowSquish()
              .clip(LiquidGlassShapes.SmallCardShape)
              .then(interactionModifier)
          Box(modifier = Modifier.fillMaxWidth()) {
            InstalledAppRow(app, isSelected, rowModifier)
            AppContextMenu(
              expanded = menuExpanded,
              packageName = app.packageName,
              appName = app.name,
              isInstalled = true,
              onDismissRequest = { menuExpanded = false },
              onOpenDetails = openWithMelt,
            )
          }
        }
      }
    }
  }
  val meteredLambda = showMeteredDialog
  if (meteredLambda != null)
    MeteredConnectionDialog(
      numBytes = myAppsInfo.model.appUpdatesBytes.takeIf { it == null || it > 0L },
      onConfirm = { notWarnWhenMetered ->
        if (notWarnWhenMetered) myAppsInfo.actions.onNotWarnWhenMetered()
        meteredLambda()
      },
      onDismiss = { showMeteredDialog = null },
    )
}

@Preview
@Composable
@RestrictTo(RestrictTo.Scope.TESTS)
private fun MyAppsListPreview() {
  FDroidContent {
    MyApps(
      myAppsInfo = getMyAppsInfo(myAppsModel),
      currentPackageName = null,
      onAppItemClick = {},
      onNav = {},
    )
  }
}

@Preview
@Composable
@RestrictTo(RestrictTo.Scope.TESTS)
private fun Preview() {
  FDroidContent {
    MyApps(
      myAppsInfo =
        getMyAppsInfo(
          myAppsModel.copy(
            appUpdates = emptyList(),
            installingApps =
              listOf(
                myAppsModel.installingApps[0].copy(
                  installState = InstallState.Installed("Installed App", "0.5.2", null, 1337L, null)
                )
              ),
            appsWithIssue = emptyList(),
          )
        ),
      currentPackageName = null,
      onAppItemClick = {},
      onNav = {},
    )
  }
}

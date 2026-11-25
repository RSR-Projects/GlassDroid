package org.fdroid.ui.details

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AppSettingsAlt
import androidx.compose.material.icons.filled.ChangeHistory
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.OndemandVideo
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign.Companion.Center
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.LocaleListCompat
import com.viktormykhailiv.compose.hints.HintProperties
import com.viktormykhailiv.compose.hints.rememberHint
import com.viktormykhailiv.compose.hints.rememberHintAnchorState
import com.viktormykhailiv.compose.hints.rememberHintController
import kotlinx.coroutines.launch
import org.fdroid.LocaleChooser.getBestLocale
import org.fdroid.R
import org.fdroid.install.InstallState
import org.fdroid.ui.FDroidContent
import org.fdroid.ui.glass.LiquidGlassButton
import org.fdroid.ui.glass.LiquidGlassCard
import org.fdroid.ui.glass.LiquidGlassDialog
import org.fdroid.ui.glass.LiquidGlassShapes
import org.fdroid.ui.glass.LiquidGlassTextButton
import org.fdroid.ui.glass.LiquidScaffold
import org.fdroid.ui.glass.LiquidSprings
import org.fdroid.ui.glass.LocalLiquidGlassConfig
import org.fdroid.ui.glass.appHeroSharedBounds
import org.fdroid.ui.glass.liquidMeltDestination
import org.fdroid.ui.glass.reportLiquidScroll
import org.fdroid.ui.icons.License
import org.fdroid.ui.lists.AppListType
import org.fdroid.ui.navigation.NavigationKey
import org.fdroid.ui.utils.BigLoadingIndicator
import org.fdroid.ui.utils.ExpandableSection
import org.fdroid.ui.utils.HintOverlayContainer
import org.fdroid.ui.utils.OnboardingPopupCard
import org.fdroid.ui.utils.testApp

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
fun AppDetails(
  item: AppDetailsItem?,
  onNav: (NavigationKey) -> Unit,
  onBackNav: (() -> Unit)?,
  modifier: Modifier = Modifier,
) {
  var showInstallError by remember { mutableStateOf(false) }
  if (item == null) BigLoadingIndicator()
  else {
    val scrollState = rememberScrollState()
    val showFloatingTitle by remember { derivedStateOf { scrollState.value > 220 } }
    LiquidScaffold(
      topBar = { AppDetailsTopAppBar(item, showFloatingTitle, onBackNav) },
    ) { innerPadding ->
      if (item is NotFoundAppDetailsItem) {
        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier.fillMaxSize().padding(innerPadding),
        ) {
          Text(stringResource(R.string.no_such_app))
        }
        return@LiquidScaffold
      }
      item as LoadedAppDetailsItem
      // react to install state changes
      LaunchedEffect(item.installState) {
        val state = item.installState
        if (state is InstallState.UserConfirmationNeeded) {
          Log.i("AppDetails", "Requesting user confirmation... $state")
          item.actions.requestUserConfirmation(state)
        } else if (state is InstallState.Error) {
          showInstallError = true
        }
      }
      // onboarding hint plumbing
      val hintController = rememberHintController()
      val hint =
        rememberHint(HintProperties(dismissOnClickOutside = false)) {
          OnboardingPopupCard(
            title = stringResource(R.string.app_details_anti_features_title),
            message = stringResource(R.string.app_details_anti_features_text),
            modifier =
              Modifier.safeDrawingPadding()
                .padding(horizontal = 32.dp, vertical = 8.dp)
                .heightIn(max = 1200.dp)
                .verticalScroll(state = rememberScrollState()),
            onGotIt = {
              item.actions.onAntiFeaturesOnboardingSeen()
              hintController.dismiss()
            },
          )
        }
      val hintAnchor = rememberHintAnchorState(hint)
      val coroutineScope = rememberCoroutineScope()
      var size by remember { mutableStateOf(IntSize.Zero) }
      Column(
        modifier =
          modifier
            .appHeroSharedBounds(
              key = "app_card_${item.app.packageName}",
              clipShape = LiquidGlassShapes.CardShape,
            )
            .liquidMeltDestination(item.app.packageName)
            .reportLiquidScroll(scrollState)
            .verticalScroll(scrollState)
            .fillMaxWidth()
            .padding(
              start = innerPadding.calculateStartPadding(LocalLayoutDirection.current),
              end = innerPadding.calculateEndPadding(LocalLayoutDirection.current),
              bottom = innerPadding.calculateBottomPadding(),
            )
            .onGloballyPositioned { coordinates -> size = coordinates.size }
      ) {
        // Header is taking care of top innerPadding
        AppDetailsHeader(
          item = item,
          onNav = onNav,
          innerPadding = innerPadding,
          scrollOffsetPx = { scrollState.value.toFloat() },
        )
        AnimatedVisibility(
          visible = item.showWarnings,
          enter = LiquidSprings.expandEnter(),
          exit = LiquidSprings.expandExit(),
        ) {
          AppDetailsWarnings(item, Modifier.padding(horizontal = 16.dp))
        }
        // What's New
        if (
          item.installedVersionCode != null && (item.whatsNew != null || item.app.changelog != null)
        ) {
          LiquidGlassCard(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
          ) {
            Text(
              text = stringResource(R.string.whats_new_title),
              style = MaterialTheme.typography.titleMediumEmphasized,
              modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            if (item.whatsNew != null)
              SelectionContainer {
                Text(
                  text = item.whatsNew,
                  modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
              }
            else if (item.app.changelog != null) {
              Text(
                text =
                  buildAnnotatedString {
                    withLink(LinkAnnotation.Url(item.app.changelog!!)) {
                      append(item.app.changelog)
                    }
                  },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
              )
            }
          }
        }
        // Screenshots
        if (item.phoneScreenshots.isNotEmpty()) {
          Screenshots(item.networkState.showWarningDialog, item.phoneScreenshots)
        }
        // Anti-features
        if (!item.antiFeatures.isNullOrEmpty()) {
          AntiFeatures(
            antiFeatures = item.antiFeatures,
            hintAnchor = hintAnchor,
            showOnboarding = item.showAntiFeaturesOnboarding,
          ) {
            coroutineScope.launch { hintController.show(hintAnchor) }
          }
        }
        // Description
        item.description?.let { description ->
          val maxLines = 3
          val textMeasurer = rememberTextMeasurer()
          val allowExpand =
            remember(size.width, description) {
              textMeasurer
                .measure(text = description, constraints = Constraints.fixedWidth(size.width))
                .lineCount > maxLines
            }
          var descriptionExpanded by
            rememberSaveable(allowExpand) {
              // not expanded (false) by default,
              // but expanded (true) when expanding not allowed
              mutableStateOf(!allowExpand)
            }
          val htmlDescription = AnnotatedString.fromHtml(description)
          val glassConfig = LocalLiquidGlassConfig.current
          Column(
            modifier =
              Modifier.fillMaxWidth()
                .animateContentSize(animationSpec = glassConfig.expandSizeSpring())
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp)
          ) {
            SelectionContainer {
              Text(
                text = htmlDescription,
                lineHeight = 22.sp,
                maxLines = if (descriptionExpanded) Int.MAX_VALUE else maxLines,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
              )
            }
            if (allowExpand) {
              LiquidGlassTextButton(
                onClick = { descriptionExpanded = !descriptionExpanded },
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
              ) {
                Text(
                  text =
                    if (descriptionExpanded) {
                      stringResource(R.string.less)
                    } else {
                      stringResource(R.string.more)
                    },
                  textAlign = Center,
                  maxLines = if (descriptionExpanded) Int.MAX_VALUE else 3,
                )
              }
            }
          }
        }
        // Donate card
        if (item.showDonate) {
          LiquidGlassCard(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
          ) {
            Text(
              text = stringResource(R.string.donate_title),
              style = MaterialTheme.typography.titleMediumEmphasized,
              modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            item.donateLinks.forEach { donate ->
              AppDonationLink(
                link = donate,
                modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp),
              )
            }
          }
        }
        // Links
        ExpandableSection(
          icon = rememberVectorPainter(Icons.Default.Link),
          title = stringResource(R.string.links),
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
          Column(modifier = Modifier.padding(start = 16.dp)) {
            item.app.webSite?.let { webSite ->
              AppDetailsLink(
                icon = Icons.Default.Home,
                title = stringResource(R.string.menu_website),
                url = webSite,
              )
            }
            item.app.issueTracker?.let { issueTracker ->
              AppDetailsLink(
                icon = Icons.Default.EditNote,
                title = stringResource(R.string.menu_issues),
                url = issueTracker,
              )
            }
            item.app.changelog?.let { changelog ->
              AppDetailsLink(
                icon = Icons.Default.ChangeHistory,
                title = stringResource(R.string.menu_changelog),
                url = changelog,
              )
            }
            item.app.license?.let { license ->
              AppDetailsLink(
                icon = License,
                title = stringResource(R.string.menu_license, license),
                url = "https://spdx.org/licenses/$license",
              )
            }
            item.app.translation?.let { translation ->
              AppDetailsLink(
                icon = Icons.Default.Translate,
                title = stringResource(R.string.menu_translation),
                url = translation,
              )
            }
            item.app.sourceCode?.let { sourceCode ->
              AppDetailsLink(
                icon = Icons.Default.Code,
                title = stringResource(R.string.menu_source),
                url = sourceCode,
              )
            }
            item.app.video?.getBestLocale(LocaleListCompat.getDefault())?.let { video ->
              AppDetailsLink(
                icon = Icons.Default.OndemandVideo,
                title = stringResource(R.string.menu_video),
                url = video,
              )
            }
          }
        }
        // Versions
        if (!item.versions.isNullOrEmpty()) {
          Versions(item) { scrollState.scrollTo(0) }
          if (item.versions.any { it.permissions.isNotEmpty() }) {
            PermissionsSection(
              versions = item.versions,
              modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
          }
        }
        // Developer contact
        if (item.showAuthorContact)
          ExpandableSection(
            icon = rememberVectorPainter(Icons.Default.Person),
            title = stringResource(R.string.developer_contact),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
          ) {
            Column(modifier = Modifier.padding(start = 16.dp)) {
              item.app.authorWebSite?.let { authorWebSite ->
                AppDetailsLink(
                  icon = Icons.Default.Home,
                  title = stringResource(R.string.menu_website),
                  url = authorWebSite,
                )
              }
              item.app.authorEmail?.let { authorEmail ->
                AppDetailsLink(
                  icon = Icons.Default.Mail,
                  title = stringResource(R.string.menu_email),
                  url = "mailto:$authorEmail",
                )
              }
            }
          }
        ExpandableSection(
          icon = rememberVectorPainter(Icons.Default.AppSettingsAlt),
          title = stringResource(R.string.technical_info),
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
          TechnicalInfo(item)
        }
        // More apps by dev
        if (item.authorHasMoreThanOneApp) {
          val authorName = item.app.authorName!!
          val title = stringResource(R.string.app_list_author, authorName)
          LiquidGlassButton(
            onClick = { onNav(NavigationKey.AppList(AppListType.Author(title, authorName))) },
            modifier = Modifier.align(CenterHorizontally).padding(bottom = 16.dp),
          ) {
            val s = stringResource(R.string.app_details_more_apps_by_author, authorName)
            Text(s)
          }
        }
      }
    }
    if (item is LoadedAppDetailsItem && showInstallError && item.installState is InstallState.Error)
      LiquidGlassDialog(
        onDismissRequest = { showInstallError = false },
        title = { Text(stringResource(R.string.install_error_notify_title, item.name)) },
        text = {
          if (item.installState.msg == null) {
            Text(stringResource(R.string.app_details_install_error_text))
          } else {
            ExpandableSection(
              icon = null,
              title = stringResource(R.string.app_details_install_error_text),
            ) {
              SelectionContainer {
                Text(
                  text = item.installState.msg,
                  fontFamily = FontFamily.Monospace,
                  modifier = Modifier.padding(top = 8.dp),
                )
              }
            }
          }
        },
        confirmButton = {
          LiquidGlassTextButton(onClick = { showInstallError = false }) {
            Text(stringResource(R.string.ok))
          }
        },
      )
  }
}

@Preview
@Composable
fun AppDetailsLoadingPreview() {
  FDroidContent { AppDetails(null, {}, {}) }
}

@Preview
@Composable
fun AppDetailsNotFoundPreview() {
  FDroidContent { AppDetails(NotFoundAppDetailsItem, {}, {}) }
}

@Preview
@Composable
fun AppDetailsPreview() {
  HintOverlayContainer { FDroidContent { AppDetails(testApp, {}, {}) } }
}

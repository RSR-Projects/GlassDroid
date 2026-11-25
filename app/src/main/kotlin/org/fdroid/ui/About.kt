package org.fdroid.ui

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.fdroid.BuildConfig.APPLICATION_ID
import org.fdroid.BuildConfig.VERSION_CODE
import org.fdroid.BuildConfig.VERSION_NAME
import org.fdroid.R
import org.fdroid.ui.glass.LiquidFloatingTopBar
import org.fdroid.ui.glass.LiquidGlassButton
import org.fdroid.ui.glass.LiquidGlassCard
import org.fdroid.ui.glass.LiquidGlassDivider
import org.fdroid.ui.glass.LiquidGlassShapes
import org.fdroid.ui.glass.LiquidGlassTextButton
import org.fdroid.ui.glass.LiquidScaffold
import org.fdroid.ui.glass.LiquidToastType
import org.fdroid.ui.glass.LocalAtmosphericBackdrop
import org.fdroid.ui.glass.LocalLiquidGlassConfig
import org.fdroid.ui.glass.LocalLiquidToastHost
import org.fdroid.ui.glass.fastLiquidSurface
import org.fdroid.ui.glass.glassShimmerSweep
import org.fdroid.ui.glass.liquidGlass
import org.fdroid.ui.glass.reportLiquidScroll
import org.fdroid.ui.glass.staggeredLiquidEntrance
import org.fdroid.ui.utils.BackButton
import org.fdroid.ui.utils.openUriSafe

private const val RSR_PROJECTS_WEBSITE_URL = "https://rsr-projects.github.io/RSR-FDroid/"
private const val GLASSDROID_REPO_URL = "https://github.com/RSR-Projects/GlassDroid/"
private const val FDROID_WEBSITE_URL = "https://f-droid.org"
private const val FDROID_SOURCE_URL = "https://gitlab.com/fdroid/fdroidclient"
private const val FDROID_FORUM_URL = "https://forum.f-droid.org"

@Composable
fun About(version: String = VERSION_NAME, onBackClicked: (() -> Unit)?) {
  LiquidScaffold(
    topBar = {
      LiquidFloatingTopBar(
        navigationIcon = if (onBackClicked != null) { { BackButton(onClick = onBackClicked) } } else null,
        title = { Text(stringResource(R.string.about_title_basic), maxLines = 1) },
        showActions = false,
      )
    },
  ) { paddingValues ->
    AboutContent(version = version, modifier = Modifier.fillMaxSize(), paddingValues = paddingValues)
  }
}

@Composable
fun AboutContent(
  version: String,
  modifier: Modifier = Modifier,
  paddingValues: PaddingValues = PaddingValues(),
) {
  val scrollableState = rememberScrollState()
  Box(
    contentAlignment = Alignment.TopCenter,
    modifier = modifier.reportLiquidScroll(scrollableState).verticalScroll(scrollableState),
  ) {
    Column(
      verticalArrangement = spacedBy(14.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier =
        Modifier.widthIn(max = 620.dp)
          .fillMaxWidth()
          .padding(paddingValues)
          .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
      AboutHeroCard(version = version)
      WhatMakesDifferentCard()
      RsrProjectsLinksCard()
      UpstreamAttributionCard()
      LicensesAndAcknowledgementsCard()
      CreditsCard()
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AboutHeroCard(version: String) {
  val backdrop = LocalAtmosphericBackdrop.current
  val config = LocalLiquidGlassConfig.current
  LiquidGlassCard(
    shape = LiquidGlassShapes.CardShape,
    modifier =
      Modifier.fillMaxWidth()
        .staggeredLiquidEntrance(key = "about_hero", index = 0)
        .glassShimmerSweep(key = "about_hero_sheen", enabled = true, delayMillis = 220L),
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = spacedBy(12.dp),
      modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 22.dp),
    ) {
      Box(
        contentAlignment = Alignment.Center,
        modifier =
          Modifier.size(92.dp)
            .liquidGlass(
              backdrop = backdrop,
              shape = LiquidGlassShapes.CardShape,
              blurRadius = 8.dp,
              refractionHeight = 16.dp,
              refractionAmount = 32.dp,
              withShadow = true,
            )
            .padding(10.dp),
      ) {
        Image(
          painter = painterResource(id = R.drawable.ic_glassdroid_fdroid_icon),
          contentDescription = stringResource(R.string.app_name_basic),
          modifier = Modifier.fillMaxSize().semantics { hideFromAccessibility() },
        )
      }

      Text(
        text = stringResource(R.string.app_name_basic),
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
      )

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier =
          Modifier.clip(LiquidGlassShapes.CapsuleShape)
            .background(
              MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
              LiquidGlassShapes.CapsuleShape,
            )
            .border(
              width = 0.9.dp,
              color = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
              shape = LiquidGlassShapes.CapsuleShape,
            )
            .padding(horizontal = 12.dp, vertical = 5.dp),
      ) {
        Icon(
          imageVector = Icons.Default.AutoAwesome,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(15.dp),
        )
        Text(
          text = stringResource(R.string.about_developer_badge),
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.primary,
        )
      }

      SelectionContainer {
        FlowRow(
          horizontalArrangement = spacedBy(8.dp, Alignment.CenterHorizontally),
          verticalArrangement = spacedBy(8.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          InfoPillChip("${stringResource(R.string.about_version)} $version")
          InfoPillChip(stringResource(R.string.about_build_code, VERSION_CODE))
          InfoPillChip(APPLICATION_ID)
        }
      }

      SelectionContainer {
        Text(
          text = stringResource(R.string.about_glassdroid_description),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(top = 4.dp),
        )
      }

      @Suppress("UNUSED_EXPRESSION")
      config
    }
  }
}

@Composable
private fun InfoPillChip(label: String) {
  Text(
    text = label,
    style = MaterialTheme.typography.labelMedium,
    fontWeight = FontWeight.Medium,
    color = MaterialTheme.colorScheme.onSurface,
    modifier =
      Modifier.fastLiquidSurface(
          shape = LiquidGlassShapes.CapsuleShape,
          withShadow = false,
        )
        .padding(horizontal = 12.dp, vertical = 6.dp),
  )
}

@Composable
private fun WhatMakesDifferentCard() {
  val differences =
    listOf(
      stringResource(R.string.about_diff_1),
      stringResource(R.string.about_diff_2),
      stringResource(R.string.about_diff_3),
      stringResource(R.string.about_diff_4),
      stringResource(R.string.about_diff_5),
    )
  LiquidGlassCard(
    shape = LiquidGlassShapes.CardShape,
    modifier =
      Modifier.fillMaxWidth()
        .staggeredLiquidEntrance(key = "about_differences", index = 1),
  ) {
    Column(
      verticalArrangement = spacedBy(10.dp),
      modifier = Modifier.fillMaxWidth().padding(18.dp),
    ) {
      Text(
        text = stringResource(R.string.about_what_makes_different_title),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
      )
      LiquidGlassDivider()
      SelectionContainer {
        Column(verticalArrangement = spacedBy(8.dp)) {
          differences.forEach { bullet ->
            Row(
              horizontalArrangement = spacedBy(10.dp),
              verticalAlignment = Alignment.Top,
            ) {
              Text(
                text = "•",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
              )
              Text(
                text = bullet,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun RsrProjectsLinksCard() {
  val uriHandler = LocalUriHandler.current
  val toastHost = LocalLiquidToastHost.current
  val starToastMessage = stringResource(R.string.about_star_toast)

  LiquidGlassCard(
    shape = LiquidGlassShapes.CardShape,
    modifier =
      Modifier.fillMaxWidth()
        .staggeredLiquidEntrance(key = "about_links", index = 2),
  ) {
    Column(
      verticalArrangement = spacedBy(10.dp),
      modifier = Modifier.fillMaxWidth().padding(18.dp),
    ) {
      Text(
        text = stringResource(R.string.about_links_rsr_title),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
      )
      LiquidGlassDivider()
      AboutLinkRow(
        text = stringResource(R.string.about_rsr_website),
        subtitle = RSR_PROJECTS_WEBSITE_URL,
        icon = Icons.Default.Public,
        onClick = { uriHandler.openUriSafe(RSR_PROJECTS_WEBSITE_URL) },
        modifier = Modifier.testTag("about_rsr_website_button"),
      )
      AboutLinkRow(
        text = stringResource(R.string.about_glassdroid_repo),
        subtitle = GLASSDROID_REPO_URL,
        icon = Icons.Default.Code,
        onClick = { uriHandler.openUriSafe(GLASSDROID_REPO_URL) },
        modifier = Modifier.testTag("about_repo_button"),
      )
      LiquidGlassButton(
        onClick = {
          toastHost?.show(starToastMessage, LiquidToastType.INFO)
          uriHandler.openUriSafe(GLASSDROID_REPO_URL)
        },
        containerColor = Color(0xFFFF9F0A),
        contentColor = Color.White,
        modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp).testTag("about_star_repo_button"),
      ) {
        Icon(
          imageVector = Icons.Default.Star,
          contentDescription = null,
          tint = Color.White,
          modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
          text = stringResource(R.string.about_star_repository),
          fontWeight = FontWeight.Bold,
          color = Color.White,
        )
      }
    }
  }
}

@Composable
private fun UpstreamAttributionCard() {
  val uriHandler = LocalUriHandler.current
  LiquidGlassCard(
    shape = LiquidGlassShapes.CardShape,
    modifier =
      Modifier.fillMaxWidth()
        .staggeredLiquidEntrance(key = "about_upstream", index = 3),
  ) {
    Column(
      verticalArrangement = spacedBy(10.dp),
      modifier = Modifier.fillMaxWidth().padding(18.dp),
    ) {
      Text(
        text = stringResource(R.string.about_upstream_title),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
      )
      LiquidGlassDivider()
      SelectionContainer {
        Text(
          text = stringResource(R.string.about_upstream_summary),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      AboutLinkRow(
        text = stringResource(R.string.about_upstream_website),
        subtitle = FDROID_WEBSITE_URL,
        icon = Icons.Default.Language,
        onClick = { uriHandler.openUriSafe(FDROID_WEBSITE_URL) },
      )
      AboutLinkRow(
        text = stringResource(R.string.about_upstream_source),
        subtitle = FDROID_SOURCE_URL,
        icon = Icons.Default.Code,
        onClick = { uriHandler.openUriSafe(FDROID_SOURCE_URL) },
      )
      AboutLinkRow(
        text = stringResource(R.string.about_forum),
        subtitle = FDROID_FORUM_URL,
        icon = Icons.Default.Forum,
        onClick = { uriHandler.openUriSafe(FDROID_FORUM_URL) },
      )
    }
  }
}

@Composable
private fun LicensesAndAcknowledgementsCard() {
  val libraries =
    listOf(
      "Kyant0 AndroidLiquidGlass (Backdrop 2.0.1 & Capsule 2.1.3) — Apache License 2.0",
      "F-Droid Client (fdroidclient upstream) — GNU GPL v3.0+",
      "AndroidX Jetpack Compose, Material 3, Navigation 3, WorkManager, Room — Apache License 2.0",
      "Kotlin, Kotlin Coroutines & kotlinx.serialization — Apache License 2.0",
      "Dagger Hilt & Molecule Runtime — Apache License 2.0",
      "Coil 3 Image Loading, OkHttp & Ktor — Apache License 2.0",
      "Compose Preference & Compose Hints — Apache License 2.0",
      "ZXing & ZXing Android Embedded (QR Scanning) — Apache License 2.0",
      "SLF4J & Logback-Android — MIT / EPL 1.0",
    )

  LiquidGlassCard(
    shape = LiquidGlassShapes.CardShape,
    modifier =
      Modifier.fillMaxWidth()
        .staggeredLiquidEntrance(key = "about_licenses", index = 4),
  ) {
    Column(
      verticalArrangement = spacedBy(10.dp),
      modifier = Modifier.fillMaxWidth().padding(18.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = spacedBy(8.dp),
      ) {
        Icon(
          imageVector = Icons.Default.VerifiedUser,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(20.dp),
        )
        Text(
          text = stringResource(R.string.about_oss_title),
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
        )
      }
      LiquidGlassDivider()
      SelectionContainer {
        Column(verticalArrangement = spacedBy(8.dp)) {
          Text(
            text = stringResource(R.string.about_license_text),
            style = MaterialTheme.typography.bodyMedium,
          )
          libraries.forEach { entry ->
            Text(
              text = "• $entry",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun CreditsCard() {
  LiquidGlassCard(
    shape = LiquidGlassShapes.CardShape,
    modifier =
      Modifier.fillMaxWidth()
        .staggeredLiquidEntrance(key = "about_credits", index = 5),
  ) {
    Column(
      verticalArrangement = spacedBy(8.dp),
      modifier = Modifier.fillMaxWidth().padding(18.dp),
    ) {
      Text(
        text = stringResource(R.string.about_credits_title),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
      )
      LiquidGlassDivider()
      SelectionContainer {
        Text(
          text = stringResource(R.string.about_credits_body),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

@Composable
private fun AboutLinkRow(
  text: String,
  subtitle: String,
  icon: ImageVector,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  LiquidGlassTextButton(
    onClick = onClick,
    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
    modifier = modifier.fillMaxWidth(),
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        modifier = Modifier.size(20.dp).semantics { hideFromAccessibility() },
      )
      Column(verticalArrangement = spacedBy(2.dp), modifier = Modifier.weight(1f)) {
        Text(
          text = text,
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.SemiBold,
        )
        Text(
          text = subtitle,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

@Preview(showBackground = true)
@Composable
private fun AboutPreview() {
  FDroidContent { About {} }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AboutPreviewDark() {
  FDroidContent { About {} }
}

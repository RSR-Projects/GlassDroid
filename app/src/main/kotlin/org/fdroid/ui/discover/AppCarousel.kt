package org.fdroid.ui.discover

import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.fdroid.ui.FDroidContent
import org.fdroid.ui.glass.AppContextMenu
import org.fdroid.ui.glass.LiquidGlassBadgedBox
import org.fdroid.ui.glass.LiquidGlassIconButton
import org.fdroid.ui.glass.LiquidGlassShapes
import org.fdroid.ui.glass.appHeroSharedBounds
import org.fdroid.ui.glass.appHeroSharedElement
import org.fdroid.ui.glass.glassShimmerSweep
import org.fdroid.ui.glass.iconPopEntrance
import org.fdroid.ui.glass.liquidRowSquish
import org.fdroid.ui.glass.liquidSquish
import org.fdroid.ui.glass.rememberLiquidMeltOpener
import org.fdroid.ui.glass.springListItem
import org.fdroid.ui.glass.staggeredLiquidEntrance
import org.fdroid.ui.utils.AsyncShimmerImage
import org.fdroid.ui.utils.InstalledBadge
import org.fdroid.ui.utils.Names

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppCarousel(
  title: String,
  apps: List<AppDiscoverItem>,
  modifier: Modifier = Modifier,
  heroExcludePackages: Set<String> = emptySet(),
  onTitleTap: () -> Unit,
  onAppTap: (AppDiscoverItem) -> Unit,
) {
  val rowState = rememberLazyListState()
  Column(verticalArrangement = spacedBy(10.dp), modifier = modifier) {
    Row(
      verticalAlignment = CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
      modifier =
        Modifier.fillMaxWidth()
          .padding(horizontal = 12.dp)
          .liquidRowSquish()
          .clip(LiquidGlassShapes.SmallCardShape)
          .clickable(onClick = onTitleTap)
          .padding(horizontal = 4.dp),
    ) {
      Text(text = title, style = MaterialTheme.typography.titleMediumEmphasized, fontSize = 20.sp)
      LiquidGlassIconButton(
        onClick = onTitleTap,
        tintContainer = true,
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.ArrowForward,
          modifier = Modifier.semantics { hideFromAccessibility() },
          contentDescription = null,
        )
      }
    }
    LazyRow(
      state = rowState,
      contentPadding = PaddingValues(horizontal = 16.dp),
      horizontalArrangement = spacedBy(16.dp),
    ) {
      itemsIndexed(apps, key = { _, app -> app.packageName }) { index, app ->
        AppBox(
          app = app,
          index = index,
          rowState = rowState,
          enableHeroShared = app.packageName !in heroExcludePackages,
          onAppTap = onAppTap,
          modifier = Modifier.springListItem(this),
        )
      }
    }
  }
}

@Composable
fun AppBox(
  app: AppDiscoverItem,
  onAppTap: (AppDiscoverItem) -> Unit,
  modifier: Modifier = Modifier,
  index: Int = 0,
  rowState: LazyListState? = null,
  enableHeroShared: Boolean = true,
) {
  val haptic = LocalHapticFeedback.current
  var menuExpanded by remember { mutableStateOf(false) }
  val (meltModifier, openWithMelt) =
    rememberLiquidMeltOpener(app.packageName) { onAppTap(app) }

  Box(modifier = modifier) {
    Column(
      verticalArrangement = spacedBy(8.dp),
      modifier =
        Modifier.width(80.dp)
          .staggeredLiquidEntrance(key = app.packageName, index = index, baseDelayMs = 24L)
          .then(
            if (enableHeroShared) {
              Modifier.appHeroSharedBounds(
                key = "app_card_${app.packageName}",
                clipShape = LiquidGlassShapes.SmallCardShape,
              )
            } else {
              Modifier
            }
          )
          .then(meltModifier)
          .liquidSquish(pressScale = 0.91f, stretchAmount = 0.055f, maxDragTranslationPx = 8f)
          .clip(LiquidGlassShapes.SmallCardShape)
          .combinedClickable(
            onClick = openWithMelt,
            onLongClick = {
              haptic.performHapticFeedback(HapticFeedbackType.LongPress)
              menuExpanded = true
            },
          )
          .padding(2.dp),
    ) {
      val iconParallaxModifier =
        if (rowState != null) {
          Modifier.graphicsLayer {
            val layoutInfo = rowState.layoutInfo
            val visibleInfo = layoutInfo.visibleItemsInfo.firstOrNull { it.key == app.packageName }
            if (visibleInfo != null && layoutInfo.viewportSize.width > 0) {
              val viewportCenter = layoutInfo.viewportSize.width * 0.5f
              val itemCenter = visibleInfo.offset + visibleInfo.size * 0.5f
              val normalized =
                ((itemCenter - viewportCenter) / viewportCenter).coerceIn(-1f, 1f)
              translationX = -normalized * 5.dp.toPx()
            }
          }
        } else {
          Modifier
        }

      LiquidGlassBadgedBox(badge = { if (app.isInstalled) InstalledBadge() }) {
        AsyncShimmerImage(
          model = app.imageModel,
          contentDescription = null,
          contentScale = ContentScale.Crop,
          modifier =
            Modifier.size(76.dp)
              .then(iconParallaxModifier)
              .iconPopEntrance(key = app.packageName, index = index)
              .then(
                if (enableHeroShared) {
                  Modifier.appHeroSharedElement("app_icon_${app.packageName}")
                } else {
                  Modifier
                }
              )
              .clip(MaterialTheme.shapes.large)
              .glassShimmerSweep(
                key = app.packageName,
                enabled = index < 3,
                delayMillis = 260L + index * 110L,
              )
              .semantics { hideFromAccessibility() },
        )
      }
      Text(
        text = app.name,
        style = MaterialTheme.typography.bodySmall,
        minLines = 2,
        maxLines = 2,
        lineHeight = 14.sp,
        modifier =
          if (enableHeroShared) {
            Modifier.appHeroSharedElement("app_title_${app.packageName}")
          } else {
            Modifier
          },
      )
    }

    AppContextMenu(
      expanded = menuExpanded,
      packageName = app.packageName,
      appName = app.name,
      isInstalled = app.isInstalled,
      onDismissRequest = { menuExpanded = false },
      onOpenDetails = openWithMelt,
    )
  }
}

@Preview
@Composable
fun AppCarouselPreview() {
  val apps =
    listOf(
      AppDiscoverItem("1", Names.randomName, false),
      AppDiscoverItem("2", Names.randomName, true),
      AppDiscoverItem("3", Names.randomName, false),
      AppDiscoverItem("4", Names.randomName, false),
      AppDiscoverItem("5", Names.randomName, false),
    )
  FDroidContent { AppCarousel("Preview Apps", apps, onTitleTap = {}) {} }
}

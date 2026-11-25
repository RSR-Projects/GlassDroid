package org.fdroid.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement.Center
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import org.fdroid.R
import org.fdroid.ui.FDroidContent
import org.fdroid.ui.MainModel
import org.fdroid.ui.glass.LiquidFloatingBottomDock
import org.fdroid.ui.glass.LiquidFloatingRailContainer
import org.fdroid.ui.glass.LiquidGlassBadge
import org.fdroid.ui.glass.LiquidGlassBadgedBox
import org.fdroid.ui.glass.LiquidGlassDefaults
import org.fdroid.ui.glass.LiquidGlassShapes
import org.fdroid.ui.glass.liquidButtonSquish

@Composable
fun BottomBar(model: MainModel, currentNavKey: NavKey, onNav: (MainNavKey) -> Unit) {
  val res = LocalResources.current
  val selectedIndex = topLevelRoutes.indexOf(currentNavKey).coerceAtLeast(0)
  LiquidFloatingBottomDock(
    items = topLevelRoutes,
    selectedIndex = selectedIndex,
    onSelect = { index -> onNav(topLevelRoutes[index]) },
    itemSemantics = { _, dest ->
      Modifier.semantics {
        if (dest == NavigationKey.MyApps) {
          if (model.numUpdates > 0) {
            stateDescription =
              res.getString(R.string.notification_channel_updates_available_title)
          } else if (model.hasAppIssues) {
            stateDescription = res.getString(R.string.my_apps_header_apps_with_issue)
          }
        }
      }
    },
  ) { _, dest, isSelected, selectionFraction ->
    val palette = LiquidGlassDefaults.palette()
    val unselectedTint =
      if (palette.isDark) Color.White.copy(alpha = 0.72f)
      else MaterialTheme.colorScheme.onSurfaceVariant
    val selectedTint = MaterialTheme.colorScheme.primary
    val tint = androidx.compose.ui.graphics.lerp(unselectedTint, selectedTint, selectionFraction)
    val contentScale = 0.96f + 0.11f * selectionFraction
    val textShadowColor =
      selectedTint.copy(alpha = (0.42f * selectionFraction).coerceIn(0f, 0.45f))
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Center,
      modifier =
        Modifier.padding(horizontal = 4.dp).graphicsLayer {
          scaleX = contentScale
          scaleY = contentScale
          translationY = -1.2.dp.toPx() * selectionFraction
        },
    ) {
      NavIcon(
        dest = dest,
        numUpdates = model.numUpdates,
        hasIssues = model.hasAppIssues,
        tint = tint,
      )
      Text(
        text = stringResource(dest.label),
        style =
          MaterialTheme.typography.labelSmall.copy(
            shadow =
              if (selectionFraction > 0.25f) {
                androidx.compose.ui.graphics.Shadow(
                  color = textShadowColor,
                  blurRadius = 10f * selectionFraction,
                )
              } else {
                null
              }
          ),
        fontWeight = if (isSelected || selectionFraction > 0.55f) FontWeight.Bold else FontWeight.Medium,
        color = tint,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
fun NavigationRail(
  numUpdates: Int,
  hasIssues: Boolean,
  currentNavKey: NavKey,
  onNav: (MainNavKey) -> Unit,
  modifier: Modifier = Modifier,
) {
  val res = LocalResources.current
  val palette = LiquidGlassDefaults.palette()
  val config = org.fdroid.ui.glass.LocalLiquidGlassConfig.current
  LiquidFloatingRailContainer(modifier = modifier) {
    topLevelRoutes.forEach { dest ->
      val isSelected = dest == currentNavKey
      val tint by
        animateColorAsState(
          targetValue =
            if (isSelected) MaterialTheme.colorScheme.primary
            else if (palette.isDark) Color.White.copy(alpha = 0.78f)
            else MaterialTheme.colorScheme.onSurfaceVariant,
          animationSpec = config.colorSpring(),
        )
      val containerColor by
        animateColorAsState(
          targetValue = if (isSelected) palette.selectedPillTint else Color.Transparent,
          animationSpec = config.colorSpring(),
        )
      val scale by
        animateFloatAsState(
          targetValue = if (isSelected) 1.05f else 1f,
          animationSpec = config.bouncySpatialSpring(),
        )
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = spacedBy(4.dp, Alignment.CenterVertically),
        modifier =
          Modifier.fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .liquidButtonSquish()
            .graphicsLayer {
              scaleX = scale
              scaleY = scale
            }
            .clip(LiquidGlassShapes.SmallCardShape)
            .background(containerColor, LiquidGlassShapes.SmallCardShape)
            .clickable { onNav(dest) }
            .padding(vertical = 8.dp, horizontal = 4.dp)
            .semantics {
              role = Role.Tab
              selected = isSelected
              if (dest == NavigationKey.MyApps) {
                if (numUpdates > 0) {
                  stateDescription =
                    res.getString(R.string.notification_channel_updates_available_title)
                } else if (hasIssues) {
                  stateDescription = res.getString(R.string.my_apps_header_apps_with_issue)
                }
              }
            },
      ) {
        NavIcon(dest = dest, numUpdates = numUpdates, hasIssues = hasIssues, tint = tint)
        Text(
          text = stringResource(dest.label),
          style = MaterialTheme.typography.labelSmall,
          fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
          color = tint,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}

@Composable
private fun NavIcon(
  dest: MainNavKey,
  numUpdates: Int,
  hasIssues: Boolean,
  tint: Color,
  modifier: Modifier = Modifier,
) {
  LiquidGlassBadgedBox(
    modifier = modifier,
    badge = {
      if (dest == NavigationKey.MyApps && numUpdates > 0) {
        LiquidGlassBadge(containerColor = MaterialTheme.colorScheme.secondary) {
          Text(text = numUpdates.toString())
        }
      } else if (dest == NavigationKey.MyApps && hasIssues) {
        Icon(
          imageVector = Icons.Default.Error,
          tint = MaterialTheme.colorScheme.error,
          contentDescription = stringResource(R.string.my_apps_header_apps_with_issue),
        )
      }
    },
  ) {
    Icon(dest.icon, contentDescription = stringResource(dest.label), tint = tint)
  }
}

@Preview
@Composable
private fun Preview() {
  FDroidContent {
    Row {
      NavigationRail(
        numUpdates = 3,
        hasIssues = false,
        currentNavKey = NavigationKey.Discover,
        onNav = {},
      )
      Column(verticalArrangement = spacedBy(16.dp)) {
        val model =
          MainModel(
            dynamicColors = false,
            numUpdates = 3,
            hasAppIssues = false,
            showOnboarding = false,
          )
        BottomBar(model = model, currentNavKey = NavigationKey.Discover, onNav = {})
      }
    }
  }
}

@Preview
@Composable
private fun PreviewIssues() {
  FDroidContent {
    Row {
      NavigationRail(
        numUpdates = 0,
        hasIssues = true,
        currentNavKey = NavigationKey.MyApps,
        onNav = {},
      )
      Column(verticalArrangement = spacedBy(16.dp)) {
        val model =
          MainModel(
            dynamicColors = false,
            numUpdates = 0,
            hasAppIssues = true,
            showOnboarding = false,
          )
        BottomBar(model = model, currentNavKey = NavigationKey.MyApps, onNav = {})
      }
    }
  }
}

package org.fdroid.ui.categories

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import org.fdroid.ui.FDroidContent
import org.fdroid.ui.glass.LiquidGlassIconButton
import org.fdroid.ui.glass.LiquidGlassShapes
import org.fdroid.ui.glass.LiquidSprings
import org.fdroid.ui.glass.liquidRowSquish
import org.fdroid.ui.lists.AppListType
import org.fdroid.ui.navigation.NavigationKey
import org.fdroid.ui.utils.ExpandIconArrow

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun CategoryGroupRow(
  group: CategoryGroup,
  categories: List<CategoryItem>,
  onNav: (NavKey) -> Unit,
) {
  val isPreview = LocalInspectionMode.current
  var expanded by rememberSaveable { mutableStateOf(isPreview) }
  val onClick = { expanded = !expanded }
  ListItem(
    leadingContent = {
      Box(
        contentAlignment = Center,
        modifier =
          Modifier.size(40.dp)
            .clip(LiquidGlassShapes.SmallCardShape)
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)),
      ) {
        Icon(
          imageVector = group.imageVector,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onPrimaryContainer,
          modifier = Modifier.semantics { hideFromAccessibility() },
        )
      }
    },
    supportingContent = {
      Text(text = stringResource(group.summary), maxLines = 2, overflow = TextOverflow.Ellipsis)
    },
    trailingContent = {
      LiquidGlassIconButton(onClick = onClick) {
        ExpandIconArrow(expanded)
      }
    },
    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    onClick = onClick,
    modifier = Modifier.liquidRowSquish(),
  ) {
    Text(text = stringResource(group.name), maxLines = 2, overflow = TextOverflow.Ellipsis)
  }
  AnimatedVisibility(
    visible = expanded,
    enter = LiquidSprings.expandEnter(),
    exit = LiquidSprings.expandExit(),
  ) {
    ChipFlowRow(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
      categories.forEach { categoryItem ->
        CategoryChip(
          categoryItem = categoryItem,
          onClick = {
            val type = AppListType.Category(categoryItem.name, categoryItem.id)
            val navKey = NavigationKey.AppList(type)
            onNav(navKey)
          },
        )
      }
    }
  }
}

@Composable
@PreviewLightDark
private fun Preview() {
  FDroidContent {
    val categories =
      listOf(
        CategoryItem("App Store & Updater", "App Store & Updater"),
        CategoryItem("Browser", "Browser"),
        CategoryItem("Calendar & Agenda", "Calendar & Agenda"),
      )
    Column { CategoryGroupRow(CategoryGroups.productivity, categories) {} }
  }
}

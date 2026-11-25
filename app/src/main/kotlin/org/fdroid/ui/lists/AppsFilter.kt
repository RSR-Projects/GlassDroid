package org.fdroid.ui.lists

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.fdroid.R
import org.fdroid.database.AppListSortOrder
import org.fdroid.ui.FDroidContent
import org.fdroid.ui.categories.CategoryChip
import org.fdroid.ui.categories.ChipFlowRow
import org.fdroid.ui.glass.LiquidGlassChip
import org.fdroid.ui.glass.LiquidGlassDivider
import org.fdroid.ui.glass.LiquidGlassSwitch
import org.fdroid.ui.glass.LiquidGlassTextButton
import org.fdroid.ui.glass.liquidRowSquish
import org.fdroid.ui.icons.PackageVariant
import org.fdroid.ui.utils.AsyncShimmerImage
import org.fdroid.ui.utils.ExpandableSection
import org.fdroid.ui.utils.getAppListInfo
import org.fdroid.ui.utils.getAppListModel

@Composable
fun AppsFilter(info: AppListInfo, modifier: Modifier = Modifier) {
  val scrollState = rememberScrollState()
  Column(modifier = modifier.verticalScroll(scrollState)) {
    // Header
    Row(
      horizontalArrangement = spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically,
      modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
      Icon(
        imageVector = Icons.AutoMirrored.Default.Sort,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.semantics { hideFromAccessibility() },
      )
      Text(
        text = stringResource(R.string.sort_title),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.weight(1f),
      )
    }
    // Sort by setting
    Text(
      text = stringResource(R.string.sort_summary),
      style = MaterialTheme.typography.bodyMedium,
      modifier = Modifier.padding(horizontal = 16.dp),
    )
    Row(
      horizontalArrangement = spacedBy(10.dp),
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth(),
    ) {
      val byNameSelected = info.model.sortBy == AppListSortOrder.NAME
      LiquidGlassChip(
        selected = byNameSelected,
        onClick = { info.actions.sortBy(AppListSortOrder.NAME) },
        leadingIcon = { Icon(Icons.Default.SortByAlpha, null, modifier = Modifier.size(18.dp)) },
        label = { Text(stringResource(R.string.sort_by_name_short)) },
        modifier = Modifier.weight(0.5f).semantics { role = Role.RadioButton },
      )
      val byLatestSelected = info.model.sortBy == AppListSortOrder.LAST_UPDATED
      LiquidGlassChip(
        selected = byLatestSelected,
        onClick = { info.actions.sortBy(AppListSortOrder.LAST_UPDATED) },
        leadingIcon = { Icon(Icons.Default.AccessTime, null, modifier = Modifier.size(18.dp)) },
        label = { Text(stringResource(R.string.sort_by_latest_short)) },
        modifier = Modifier.weight(0.5f).semantics { role = Role.RadioButton },
      )
    }
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = spacedBy(16.dp),
      modifier =
        Modifier.padding(horizontal = 16.dp)
          .fillMaxWidth()
          .liquidRowSquish()
          .clickable { info.actions.toggleFilterIncompatible() },
    ) {
      Text(
        stringResource(R.string.filter_only_compatible),
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.weight(1f),
      )
      LiquidGlassSwitch(
        checked = !info.model.filterIncompatible,
        onCheckedChange = { info.actions.toggleFilterIncompatible() },
      )
    }
    LiquidGlassTextButton(
      onClick = info.actions::saveFilters,
      modifier = modifier.align(Alignment.End).padding(horizontal = 16.dp),
    ) {
      Icon(Icons.Default.Save, null, modifier = Modifier.size(18.dp))
      Spacer(Modifier.width(8.dp))
      Text(text = stringResource(R.string.filter_button_save))
    }
    // Filter apps by section
    val categories = info.model.categories
    val repositories = info.model.repositories
    val antiFeatures = info.model.antiFeatures
    if (!categories.isNullOrEmpty() || repositories.isNotEmpty() || !antiFeatures.isNullOrEmpty()) {
      LiquidGlassDivider(modifier = Modifier.padding(vertical = 8.dp))
      Text(
        text = stringResource(R.string.filter_intro),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
      )
      // Categories
      if (!categories.isNullOrEmpty())
        ExpandableSection(
          icon = rememberVectorPainter(Icons.Default.Category),
          title = stringResource(R.string.main_menu__categories),
          initiallyExpanded = info.model.filteredCategoryIds.isNotEmpty(),
          iconTint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
          collapsedSummary = {
            val selectedCategories =
              categories.filter { it.id in info.model.filteredCategoryIds }.map { it.name }
            if (selectedCategories.isNotEmpty()) {
              Text(
                text = selectedCategories.joinToString(", "),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp),
              )
            }
          },
        ) {
          ChipFlowRow(modifier = modifier.fillMaxWidth()) {
            categories.forEach { item ->
              val isSelected = item.id in info.model.filteredCategoryIds
              CategoryChip(
                categoryItem = item,
                selected = isSelected,
                onSelected = {
                  if (isSelected) {
                    info.actions.removeCategory(item.id)
                  } else {
                    info.actions.addCategory(item.id)
                  }
                },
              )
            }
          }
        }
      // Repositories
      if (repositories.isNotEmpty())
        ExpandableSection(
          icon = rememberVectorPainter(PackageVariant),
          title = stringResource(R.string.app_details_repositories),
          initiallyExpanded = info.model.filteredRepositoryIds.isNotEmpty(),
          iconTint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
          collapsedSummary = {
            val selectedRepos =
              repositories.filter { it.repoId in info.model.filteredRepositoryIds }.map { it.name }
            if (selectedRepos.isNotEmpty()) {
              Text(
                text = selectedRepos.joinToString(", "),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp),
              )
            }
          },
        ) {
          ChipFlowRow(modifier = modifier.fillMaxWidth()) {
            repositories.forEach { repo ->
              val selected = repo.repoId in info.model.filteredRepositoryIds
              LiquidGlassChip(
                selected = selected,
                leadingIcon = {
                  if (selected) {
                    Icon(
                      imageVector = Icons.Default.Check,
                      contentDescription = stringResource(R.string.filter_selected),
                    )
                  } else
                    AsyncShimmerImage(
                      model = repo.icon,
                      contentDescription = null,
                      modifier = Modifier.size(18.dp).semantics { hideFromAccessibility() },
                    )
                },
                label = { Text(repo.name) },
                onClick = {
                  if (selected) {
                    info.actions.removeRepository(repo.repoId)
                  } else {
                    info.actions.addRepository(repo.repoId)
                  }
                },
              )
            }
          }
        }
      // Anti-Features
      if (!antiFeatures.isNullOrEmpty())
        ExpandableSection(
          icon = rememberVectorPainter(Icons.Default.WarningAmber),
          title = stringResource(R.string.filter_antifeatures),
          initiallyExpanded = info.model.filteredAntiFeatureIds.isNotEmpty(),
          iconTint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
          collapsedSummary = {
            val hiddenAntiFeatures =
              antiFeatures.filter { it.id in info.model.filteredAntiFeatureIds }.map { it.name }
            if (hiddenAntiFeatures.isNotEmpty()) {
              Text(
                text =
                  stringResource(
                    R.string.filter_antifeatures_collapsed,
                    hiddenAntiFeatures.joinToString(", "),
                  ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp),
              )
            }
          },
        ) {
          Column(modifier = Modifier.fillMaxWidth()) {
            Text(
              text = stringResource(R.string.filter_antifeatures_summary),
              style = MaterialTheme.typography.bodyMedium,
            )
            antiFeatures.forEach { antiFeature ->
              AntiFeatureRow(antiFeature, info.model.filteredAntiFeatureIds, info.actions)
            }
          }
        }
      // clear all filters
      LiquidGlassDivider(modifier = Modifier.padding(vertical = 8.dp))
      LiquidGlassTextButton(
        onClick = info.actions::clearFilters,
        modifier = modifier.align(Alignment.End).padding(horizontal = 16.dp),
      ) {
        Icon(Icons.Default.Clear, null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(stringResource(R.string.filter_button_clear_all))
      }
    }
  }
}

@Composable
@Preview(locale = "en_US")
private fun Preview() {
  FDroidContent {
    val info = getAppListInfo(getAppListModel())
    AppsFilter(info)
  }
}

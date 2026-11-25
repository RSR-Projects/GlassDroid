package org.fdroid.ui.repositories

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import org.fdroid.R
import org.fdroid.ui.glass.LiquidGlassCard
import org.fdroid.ui.glass.LiquidGlassDialog
import org.fdroid.ui.glass.LiquidGlassShapes
import org.fdroid.ui.glass.LiquidGlassTextButton
import org.fdroid.ui.glass.liquidRowSquish
import org.fdroid.ui.utils.DraggableItem
import org.fdroid.ui.utils.MeteredConnectionDialog
import org.fdroid.ui.utils.dragContainer
import org.fdroid.ui.utils.rememberDragDropState

@Composable
fun RepositoriesList(
  info: RepositoryInfo,
  listState: LazyListState,
  paddingValues: PaddingValues,
  modifier: Modifier = Modifier,
) {
  val repositories = info.model.repositories ?: return
  var showDisableRepoDialog by remember { mutableStateOf<Long?>(null) }
  var showMeteredDialog by remember { mutableStateOf<(() -> Unit)?>(null) }
  val currentRepositoryId = info.currentRepositoryId
  val dragDropState =
    rememberDragDropState(
      lazyListState = listState,
      onMove = { from, to ->
        from as? Long ?: error("from $from was not a repoId")
        to as? Long ?: error("to $to was not a repoId")
        info.onRepositoryMoved(from, to)
      },
      onEnd = { from, to ->
        from as? Long ?: error("from $from was not a repoId")
        to as? Long ?: error("to $to was not a repoId")
        info.onRepositoriesFinishedMoving(from, to)
      },
    )
  LazyColumn(
    state = listState,
    contentPadding = paddingValues + PaddingValues(top = 8.dp),
    verticalArrangement = spacedBy(8.dp),
    modifier =
      modifier
        .then(if (repositories.size > 1) Modifier.dragContainer(dragDropState) else Modifier)
        .then(if (currentRepositoryId == null) Modifier else Modifier.selectableGroup()),
  ) {
    itemsIndexed(items = repositories, key = { _, item -> item.repoId }) { index, repoItem ->
      val isSelected = currentRepositoryId == repoItem.repoId
      val interactionModifier =
        if (currentRepositoryId == null) {
          Modifier.clickable(onClick = { info.onRepositorySelected(repoItem) })
        } else {
          Modifier.selectable(
            selected = isSelected,
            onClick = { info.onRepositorySelected(repoItem) },
          )
        }
      DraggableItem(dragDropState, index) { isDragging ->
        val elevation by
          animateDpAsState(
            targetValue = if (isDragging) 4.dp else 0.dp,
            animationSpec = spring(dampingRatio = 0.72f, stiffness = 420f),
          )
        RepositoryRow(
          repoItem = repoItem,
          isSelected = isSelected,
          onRepoEnabled = { enabled ->
            if (enabled) {
              if (info.model.networkState.showWarningDialog)
                showMeteredDialog = { info.onRepositoryEnabled(repoItem.repoId, true) }
              else info.onRepositoryEnabled(repoItem.repoId, true)
            } else {
              showDisableRepoDialog = repoItem.repoId
            }
          },
          modifier =
            Modifier.fillMaxWidth()
              .padding(horizontal = 8.dp)
              .liquidRowSquish()
              .clip(LiquidGlassShapes.SmallCardShape)
              .then(interactionModifier)
              .let {
                if (isDragging)
                  it.dropShadow(
                    shape = RoundedCornerShape(16.dp),
                    shadow = Shadow(radius = 8.dp, offset = DpOffset(x = elevation, elevation)),
                  )
                else it
              },
        )
      }
    }
  }
  val repoId = showDisableRepoDialog
  if (repoId != null) {
    LiquidGlassDialog(
      title = { Text(text = stringResource(R.string.repo_disable_dialog_title)) },
      text = {
        Column(verticalArrangement = spacedBy(8.dp)) {
          LiquidGlassCard(
            shape = LiquidGlassShapes.SmallCardShape,
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.88f),
          ) {
            CompositionLocalProvider(
              LocalContentColor provides MaterialTheme.colorScheme.onTertiaryContainer
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = spacedBy(8.dp),
              ) {
                Icon(
                  Icons.Default.Warning,
                  contentDescription = null,
                  modifier = Modifier.padding(start = 8.dp),
                )
                Text(
                  text = stringResource(R.string.repo_disable_dialog_warning),
                  modifier = Modifier.padding(8.dp),
                )
              }
            }
          }
          Text(stringResource(R.string.repo_disable_dialog_text))
        }
      },
      onDismissRequest = { showDisableRepoDialog = null },
      confirmButton = {
        LiquidGlassTextButton(
          onClick = {
            info.onRepositoryEnabled(repoId, false)
            showDisableRepoDialog = null
          },
          contentColor = MaterialTheme.colorScheme.error,
        ) {
          Text(text = stringResource(R.string.repo_disable_warning_button))
        }
      },
      dismissButton = {
        LiquidGlassTextButton(onClick = { showDisableRepoDialog = null }) {
          Text(stringResource(android.R.string.cancel))
        }
      },
    )
  }
  // Metered warning dialog
  val meteredLambda = showMeteredDialog
  if (meteredLambda != null)
    MeteredConnectionDialog(
      numBytes = null,
      onConfirm = { notWarnWhenMetered ->
        if (notWarnWhenMetered) info.onNotWarnWhenMetered()
        meteredLambda()
      },
      onDismiss = { showMeteredDialog = null },
    )
}

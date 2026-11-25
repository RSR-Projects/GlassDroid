package org.fdroid.ui.glass

import android.text.format.Formatter
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement.SpaceBetween
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlin.math.roundToLong
import kotlinx.coroutines.delay
import org.fdroid.R
import org.fdroid.install.InstallState
import org.fdroid.ui.details.LoadedAppDetailsItem
import org.fdroid.ui.details.MainButtonState
import org.fdroid.ui.utils.startActivitySafe

private enum class DownloadControlPhase {
  PROGRESS,
  COMPLETION,
  ACTIONS,
  HIDDEN,
}

/**
 * Unified Liquid Glass download, installation, completion, and retry control for [AppDetailsHeader].
 * Strictly driven by actual [LoadedAppDetailsItem.mainButtonState] and [InstallState] without
 * simulating or faking progress.
 */
@Composable
fun LiquidDownloadControl(
  item: LoadedAppDetailsItem,
  now: Long,
  onShowMeteredDialog: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val haptic = LocalHapticFeedback.current
  val config = LocalLiquidGlassConfig.current
  val palette = LiquidGlassDefaults.palette()
  val reducedMotion = LocalReducedMotion.current
  val state = item.installState

  var sawProgress by remember(item.app.packageName) { mutableStateOf(state.showProgress) }
  var showCompletion by remember(item.app.packageName) { mutableStateOf(false) }

  LaunchedEffect(state) {
    if (state.showProgress) {
      sawProgress = true
      showCompletion = false
    } else if (state is InstallState.Installed && sawProgress) {
      sawProgress = false
      showCompletion = true
      haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
      delay(1350L)
      showCompletion = false
    }
  }

  val hasActionButtons =
    item.mainButtonState == MainButtonState.INSTALL ||
      item.mainButtonState == MainButtonState.UPDATE ||
      (item.showOpenButton &&
        item.mainButtonState != MainButtonState.LOADING &&
        item.mainButtonState != MainButtonState.PROGRESS)

  val phase =
    when {
      item.mainButtonState == MainButtonState.PROGRESS -> DownloadControlPhase.PROGRESS
      showCompletion -> DownloadControlPhase.COMPLETION
      hasActionButtons -> DownloadControlPhase.ACTIONS
      else -> DownloadControlPhase.HIDDEN
    }

  val triggerInstall = {
    if (item.networkState.showWarningDialog) {
      onShowMeteredDialog()
    } else {
      val version = item.suggestedVersion
      if (version != null) {
        item.actions.installAction(item.app, version, item.icon)
      }
    }
  }

  Column(
    modifier =
      modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 8.dp)
        .animateContentSize(animationSpec = config.expandSizeSpring()),
    verticalArrangement = spacedBy(8.dp),
  ) {
    // Calm error retry capsule when a real InstallState.Error occurs
    AnimatedVisibility(
      visible = state is InstallState.Error && !state.showProgress,
      enter = LiquidSprings.expandEnter(config),
      exit = LiquidSprings.expandExit(config),
    ) {
      val errorColor = MaterialTheme.colorScheme.error
      Row(
        verticalAlignment = CenterVertically,
        horizontalArrangement = spacedBy(10.dp),
        modifier =
          Modifier.fillMaxWidth()
            .fastLiquidSurface(
              shape = LiquidGlassShapes.SmallCardShape,
              surfaceTint =
                errorColor.copy(alpha = if (palette.isDark) 0.16f else 0.10f),
              borderColor = errorColor.copy(alpha = 0.45f),
              withShadow = false,
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
      ) {
        Icon(
          imageVector = Icons.Default.ErrorOutline,
          contentDescription = null,
          tint = errorColor,
          modifier = Modifier.size(20.dp),
        )
        Text(
          text = stringResource(R.string.notification_title_summary_install_error),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f),
        )
        if (item.suggestedVersion != null) {
          LiquidGlassTextButton(
            onClick = triggerInstall,
            contentColor = errorColor,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            modifier = Modifier.testTag("download_retry_button"),
          ) {
            Text(stringResource(R.string.try_again))
          }
        }
      }
    }

    AnimatedContent(
      targetState = phase,
      transitionSpec = {
        (fadeIn(config.smoothSpatialSpring()) +
          scaleIn(initialScale = 0.94f, animationSpec = config.bouncySpatialSpring())) togetherWith
          (fadeOut(config.smoothSpatialSpring()) +
            scaleOut(targetScale = 0.95f, animationSpec = config.smoothSpatialSpring()))
      },
      label = "liquid_download_morph",
    ) { currentPhase ->
      when (currentPhase) {
        DownloadControlPhase.PROGRESS -> {
          Column(
            verticalArrangement = spacedBy(6.dp),
            modifier =
              Modifier.fillMaxWidth()
                .fastLiquidSurface(
                  shape = LiquidGlassShapes.SmallCardShape,
                  surfaceTint = palette.cardSurfaceTint,
                  withShadow = true,
                  isHighlighted = true,
                )
                .padding(horizontal = 14.dp, vertical = 10.dp),
          ) {
            val strRes =
              when (state) {
                is InstallState.Waiting -> R.string.status_install_preparing
                is InstallState.Starting -> R.string.status_install_preparing
                is InstallState.PreApproved -> R.string.status_install_preparing
                is InstallState.Installing -> R.string.installing
                is InstallState.UserConfirmationNeeded -> R.string.installing
                else -> -1
              }
            if (strRes >= 0) {
              Text(
                text = stringResource(strRes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f),
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
              )
            } else if (state is InstallState.Downloading) {
              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = SpaceBetween) {
                val secondsPassed = ((now - state.startMillis) / 1000f).takeIf { it > 0 } ?: 1f
                val averageSpeed = (state.downloadedBytes / secondsPassed).roundToLong()
                val speedStr = Formatter.formatFileSize(context, averageSpeed)
                val remainingStr =
                  Formatter.formatFileSize(
                    context,
                    (state.totalBytes - state.downloadedBytes).coerceAtLeast(0L),
                  )
                Text(
                  text = stringResource(R.string.status_downloading, speedStr),
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f),
                  overflow = TextOverflow.StartEllipsis,
                  maxLines = 1,
                  modifier = Modifier.padding(end = 8.dp),
                )
                Text(
                  text = stringResource(R.string.status_downloading_remaining, remainingStr),
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f),
                  overflow = TextOverflow.Ellipsis,
                  maxLines = 1,
                )
              }
            }
            Row(verticalAlignment = CenterVertically, horizontalArrangement = spacedBy(8.dp)) {
              if (state is InstallState.Downloading) {
                val animatedProgress by
                  animateFloatAsState(
                    targetValue = state.progress.coerceIn(0f, 1f),
                    animationSpec = config.smoothSpatialSpring(),
                    label = "download_progress_fraction",
                  )
                LiquidProgressCapsuleBar(
                  progress = animatedProgress,
                  reducedMotion = reducedMotion,
                  modifier = Modifier.weight(1f),
                )
              } else {
                LiquidGlassLinearProgress(modifier = Modifier.weight(1f))
              }
              var cancelled by remember { mutableStateOf(false) }
              LiquidGlassIconButton(
                onClick = {
                  if (!cancelled) item.actions.cancelInstall()
                  cancelled = true
                },
              ) {
                AnimatedVisibility(cancelled) {
                  LiquidGlassCircularProgress(modifier = Modifier.size(22.dp))
                }
                AnimatedVisibility(!cancelled) {
                  Icon(
                    imageVector = Icons.Default.Cancel,
                    contentDescription = stringResource(R.string.cancel),
                  )
                }
              }
            }
          }
        }

        DownloadControlPhase.COMPLETION -> {
          val successColor = Color(0xFF34C759)
          Row(
            verticalAlignment = CenterVertically,
            horizontalArrangement = spacedBy(10.dp, CenterHorizontally),
            modifier =
              Modifier.fillMaxWidth()
                .fastLiquidSurface(
                  shape = LiquidGlassShapes.CapsuleShape,
                  surfaceTint = successColor.copy(alpha = if (palette.isDark) 0.22f else 0.14f),
                  borderColor = successColor.copy(alpha = 0.65f),
                  withShadow = true,
                  isHighlighted = true,
                )
                .padding(horizontal = 20.dp, vertical = 12.dp),
          ) {
            LiquidCompletionCheck(
              color = successColor,
              reducedMotion = reducedMotion,
              modifier = Modifier.size(22.dp),
            )
            Text(
              text = stringResource(R.string.app_installed),
              style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
              color = MaterialTheme.colorScheme.onSurface,
            )
          }
        }

        DownloadControlPhase.ACTIONS -> {
          Row(
            horizontalArrangement = spacedBy(8.dp, CenterHorizontally),
            modifier = Modifier.fillMaxWidth(),
          ) {
            if (item.showOpenButton) {
              LiquidGlassOutlineButton(
                onClick = { context.startActivitySafe(item.actions.launchIntent) },
                modifier = Modifier.weight(1f).testTag("app_open_button"),
              ) {
                Text(stringResource(R.string.menu_open))
              }
            }
            if (
              item.mainButtonState == MainButtonState.INSTALL ||
                item.mainButtonState == MainButtonState.UPDATE
            ) {
              LiquidGlassButton(
                onClick = triggerInstall,
                modifier = Modifier.weight(1f).testTag("app_install_button"),
              ) {
                if (item.mainButtonState == MainButtonState.INSTALL) {
                  Text(stringResource(R.string.menu_install))
                } else {
                  Text(stringResource(R.string.app__install_downloaded_update))
                }
              }
            }
          }
        }

        DownloadControlPhase.HIDDEN -> Unit
      }
    }
  }
}

/**
 * Capsule linear progress track with a subtle traveling optical specular crest inside the filled
 * liquid region while downloading.
 */
@Composable
private fun LiquidProgressCapsuleBar(
  progress: Float,
  reducedMotion: Boolean,
  modifier: Modifier = Modifier,
  color: Color = MaterialTheme.colorScheme.primary,
) {
  val palette = LiquidGlassDefaults.palette()
  val trackColor =
    if (palette.isDark) Color.White.copy(alpha = 0.14f) else Color.Black.copy(alpha = 0.10f)
  val specularPhase =
    if (reducedMotion || progress <= 0.02f || progress >= 0.99f) {
      -1f
    } else {
      val infinite = rememberInfiniteTransition(label = "liquid_dl_sheen")
      infinite.animateFloat(
        initialValue = -0.3f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(tween(1300, easing = LinearEasing), RepeatMode.Restart),
        label = "dl_sheen_phase",
      ).value
    }

  Box(
    modifier =
      modifier
        .height(8.dp)
        .clip(LiquidGlassShapes.CapsuleShape)
        .background(trackColor, LiquidGlassShapes.CapsuleShape)
  ) {
    Box(
      modifier =
        Modifier.fillMaxHeight()
          .fillMaxWidth(progress.coerceIn(0.02f, 1f))
          .clip(LiquidGlassShapes.CapsuleShape)
          .background(
            Brush.horizontalGradient(
              listOf(color, color.copy(alpha = 0.85f), color)
            ),
            LiquidGlassShapes.CapsuleShape,
          )
    ) {
      if (specularPhase > -0.25f) {
        Box(
          modifier =
            Modifier.fillMaxSize().graphicsLayer {
              translationX = (specularPhase - 0.35f) * size.width
            }
        ) {
          Box(
            modifier =
              Modifier.fillMaxHeight()
                .fillMaxWidth(0.36f)
                .background(
                  Brush.horizontalGradient(
                    listOf(Color.Transparent, Color.White.copy(alpha = 0.42f), Color.Transparent)
                  )
                )
          )
        }
      }
    }
  }
}

/**
 * Self-drawing vector checkmark inside a soft glowing completion ring using [PathMeasure],
 * giving tactile completion confirmation only on real installation transitions.
 */
@Composable
fun LiquidCompletionCheck(
  color: Color = Color(0xFF34C759),
  reducedMotion: Boolean = false,
  modifier: Modifier = Modifier,
) {
  val drawProgress = remember { Animatable(if (reducedMotion) 1f else 0f) }
  LaunchedEffect(Unit) {
    if (!reducedMotion) {
      drawProgress.snapTo(0f)
      drawProgress.animateTo(1f, tween(durationMillis = 420, easing = FastOutSlowInEasing))
    }
  }
  val ringScale by
    animateFloatAsState(
      targetValue = if (drawProgress.value > 0.05f) 1f else 0.72f,
      animationSpec = LocalLiquidGlassConfig.current.bouncySpatialSpring(),
      label = "completion_ring_scale",
    )

  Canvas(
    modifier =
      modifier.size(28.dp).graphicsLayer {
        scaleX = ringScale
        scaleY = ringScale
      }
  ) {
    val strokePx = (size.minDimension * 0.11f).coerceIn(2f, 4.5f)
    drawCircle(
      color = color.copy(alpha = 0.22f),
      radius = size.minDimension * 0.46f,
    )
    drawCircle(
      color = color.copy(alpha = 0.85f),
      radius = size.minDimension * 0.46f,
      style = Stroke(width = strokePx * 0.75f),
    )

    val checkPath =
      Path().apply {
        moveTo(size.width * 0.27f, size.height * 0.52f)
        lineTo(size.width * 0.44f, size.height * 0.68f)
        lineTo(size.width * 0.74f, size.height * 0.35f)
      }
    val partialPath = Path()
    val measure = PathMeasure()
    measure.setPath(checkPath, false)
    measure.getSegment(0f, measure.length * drawProgress.value.coerceIn(0f, 1f), partialPath, true)
    drawPath(
      path = partialPath,
      color = color,
      style = Stroke(width = strokePx, cap = StrokeCap.Round, join = StrokeJoin.Round),
    )
  }
}

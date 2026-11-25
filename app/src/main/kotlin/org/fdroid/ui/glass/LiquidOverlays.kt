package org.fdroid.ui.glass

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlin.math.roundToInt

/**
 * Pure iOS 26 Liquid Glass circular/capsule Icon Button replacing Material IconButton.
 */
@Composable
fun LiquidGlassIconButton(
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  tintContainer: Boolean = false,
  contentColor: Color = LocalContentColor.current,
  content: @Composable () -> Unit,
) {
  val palette = LiquidGlassDefaults.palette()
  val bg =
    when {
      !tintContainer -> Color.Transparent
      palette.isDark -> Color.White.copy(alpha = 0.08f)
      else -> Color.White.copy(alpha = 0.55f)
    }
  val fg = if (enabled) contentColor else contentColor.copy(alpha = 0.38f)
  Box(
    contentAlignment = Alignment.Center,
    modifier =
      modifier
        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
        .liquidButtonSquish(enabled = enabled)
        .clip(LiquidGlassShapes.CapsuleShape)
        .then(
          if (tintContainer) {
            Modifier.fastLiquidSurface(
              shape = LiquidGlassShapes.CapsuleShape,
              surfaceTint = bg,
              withShadow = false,
            )
          } else {
            Modifier
          }
        )
        .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
  ) {
    CompositionLocalProvider(LocalContentColor provides fg) { content() }
  }
}

/**
 * Pure iOS 26 Liquid Glass capsule TextButton replacing Material TextButton.
 */
@Composable
fun LiquidGlassTextButton(
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  contentColor: Color = MaterialTheme.colorScheme.primary,
  contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
  content: @Composable RowScope.() -> Unit,
) {
  val fg = if (enabled) contentColor else contentColor.copy(alpha = 0.38f)
  Row(
    horizontalArrangement = Arrangement.Center,
    verticalAlignment = Alignment.CenterVertically,
    modifier =
      modifier
        .defaultMinSize(minHeight = 42.dp)
        .liquidButtonSquish(enabled = enabled)
        .clip(LiquidGlassShapes.CapsuleShape)
        .background(fg.copy(alpha = if (enabled) 0.11f else 0.04f), LiquidGlassShapes.CapsuleShape)
        .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
        .padding(contentPadding),
  ) {
    CompositionLocalProvider(LocalContentColor provides fg) {
      ProvideTextStyle(MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)) {
        content()
      }
    }
  }
}

/**
 * Pure iOS 26 Liquid Glass Dialog replacing Material AlertDialog.
 */
@Composable
fun LiquidGlassDialog(
  onDismissRequest: () -> Unit,
  confirmButton: @Composable () -> Unit,
  modifier: Modifier = Modifier,
  dismissButton: (@Composable () -> Unit)? = null,
  icon: (@Composable () -> Unit)? = null,
  title: (@Composable () -> Unit)? = null,
  text: (@Composable () -> Unit)? = null,
) {
  val palette = LiquidGlassDefaults.palette()
  val config = LocalLiquidGlassConfig.current
  val backdrop = LocalAtmosphericBackdrop.current
  var visible by remember { mutableStateOf(false) }
  LaunchedEffect(Unit) { visible = true }

  Dialog(
    onDismissRequest = onDismissRequest,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    Box(
      contentAlignment = Alignment.Center,
      modifier =
        Modifier.fillMaxSize()
          .background(Color.Black.copy(alpha = if (palette.isDark) 0.56f else 0.36f))
          .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onDismissRequest,
          )
          .padding(horizontal = 24.dp, vertical = 24.dp),
    ) {
      AnimatedVisibility(
        visible = visible,
        enter =
          fadeIn(config.smoothSpatialSpring()) +
            scaleIn(initialScale = 0.88f, animationSpec = config.bouncySpatialSpring()),
        exit =
          fadeOut(config.smoothSpatialSpring()) +
            scaleOut(targetScale = 0.90f, animationSpec = config.smoothSpatialSpring()),
      ) {
        Column(
          modifier =
            modifier
              .widthIn(min = 280.dp, max = 420.dp)
              .fillMaxWidth()
              .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
              )
              .liquidGlass(
                backdrop = backdrop,
                shape = LiquidGlassShapes.DialogShape,
                surfaceTint =
                  if (palette.isDark) Color(0xFF191E2A).copy(alpha = 0.86f)
                  else Color.White.copy(alpha = 0.90f),
                blurRadius = 12.dp,
                refractionHeight = 18.dp,
                refractionAmount = 34.dp,
                shadowRadius = 28.dp,
                shadowOffsetY = 12.dp,
              )
              .padding(22.dp)
        ) {
          CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
            if (icon != null) {
              Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
              ) {
                icon()
              }
            }
            if (title != null) {
              ProvideTextStyle(MaterialTheme.typography.titleLarge) {
                Box(modifier = Modifier.padding(bottom = if (text != null) 12.dp else 18.dp)) {
                  title()
                }
              }
            }
            if (text != null) {
              Box(
                modifier =
                  Modifier.weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 18.dp)
              ) {
                ProvideTextStyle(
                  MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                ) {
                  text()
                }
              }
            }
            Row(
              horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.fillMaxWidth(),
            ) {
              dismissButton?.invoke()
              confirmButton()
            }
          }
        }
      }
    }
  }
}

/**
 * Pure iOS 26 Liquid Glass Bottom Sheet replacing Material ModalBottomSheet.
 */
@Composable
fun LiquidGlassBottomSheet(
  onDismissRequest: () -> Unit,
  modifier: Modifier = Modifier,
  showDragHandle: Boolean = true,
  content: @Composable ColumnScope.() -> Unit,
) {
  val palette = LiquidGlassDefaults.palette()
  val config = LocalLiquidGlassConfig.current
  val backdrop = LocalAtmosphericBackdrop.current
  var visible by remember { mutableStateOf(false) }
  var dragOffsetY by remember { mutableFloatStateOf(0f) }
  LaunchedEffect(Unit) { visible = true }

  Dialog(
    onDismissRequest = onDismissRequest,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    Box(
      contentAlignment = Alignment.BottomCenter,
      modifier =
        Modifier.fillMaxSize()
          .background(Color.Black.copy(alpha = if (palette.isDark) 0.50f else 0.32f))
          .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onDismissRequest,
          ),
    ) {
      AnimatedVisibility(
        visible = visible,
        enter =
          slideInVertically(animationSpec = config.navSlideSpring()) { it / 2 } +
            fadeIn(config.smoothSpatialSpring()),
        exit =
          slideOutVertically(animationSpec = config.navSlideSpring()) { it } +
            fadeOut(config.smoothSpatialSpring()),
      ) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier =
            modifier
              .widthIn(max = 560.dp)
              .fillMaxWidth()
              .padding(horizontal = 10.dp, vertical = 10.dp)
              .navigationBarsPadding()
              .imePadding()
              .offset { IntOffset(0, dragOffsetY.roundToInt().coerceAtLeast(0)) }
              .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
              )
              .liquidGlass(
                backdrop = backdrop,
                shape = LiquidGlassShapes.SheetShape,
                surfaceTint =
                  if (palette.isDark) Color(0xFF161B26).copy(alpha = 0.88f)
                  else Color.White.copy(alpha = 0.92f),
                blurRadius = 12.dp,
                refractionHeight = 20.dp,
                refractionAmount = 38.dp,
                shadowRadius = 28.dp,
                shadowOffsetY = (-4).dp,
              )
              .pointerInput(Unit) {
                detectVerticalDragGestures(
                  onVerticalDrag = { _, delta ->
                    dragOffsetY = (dragOffsetY + delta).coerceAtLeast(0f)
                  },
                  onDragEnd = {
                    if (dragOffsetY > 140f) {
                      onDismissRequest()
                    } else {
                      dragOffsetY = 0f
                    }
                  },
                  onDragCancel = { dragOffsetY = 0f },
                )
              }
              .padding(top = 10.dp, bottom = 14.dp),
        ) {
          if (showDragHandle) {
            Box(
              modifier =
                Modifier.padding(bottom = 10.dp)
                  .width(40.dp)
                  .height(5.dp)
                  .clip(LiquidGlassShapes.CapsuleShape)
                  .background(
                    if (palette.isDark) Color.White.copy(alpha = 0.28f)
                    else Color.Black.copy(alpha = 0.18f)
                  )
            )
          }
          CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
            content()
          }
        }
      }
    }
  }
}

/**
 * Pure iOS 26 floating Liquid Glass Popover Menu replacing Material DropdownMenu.
 */
@Composable
fun LiquidGlassDropdownMenu(
  expanded: Boolean,
  onDismissRequest: () -> Unit,
  modifier: Modifier = Modifier,
  content: @Composable ColumnScope.() -> Unit,
) {
  val palette = LiquidGlassDefaults.palette()
  val config = LocalLiquidGlassConfig.current
  val backdrop = LocalLiquidBackdrop.current ?: LocalAtmosphericBackdrop.current
  if (expanded) {
    Popup(
      alignment = Alignment.TopEnd,
      onDismissRequest = onDismissRequest,
      properties = PopupProperties(focusable = true),
    ) {
      var appear by remember { mutableStateOf(false) }
      LaunchedEffect(Unit) { appear = true }
      val animScale by
        animateFloatAsState(
          targetValue = if (appear) 1f else 0.84f,
          animationSpec = config.bouncySpatialSpring(),
        )
      val animAlpha by
        animateFloatAsState(
          targetValue = if (appear) 1f else 0f,
          animationSpec = config.smoothSpatialSpring(),
        )
      Column(
        modifier =
          modifier
            .padding(top = 6.dp, end = 4.dp)
            .width(IntrinsicSize.Max)
            .widthIn(min = 196.dp, max = 300.dp)
            .graphicsLayer {
              scaleX = animScale
              scaleY = animScale
              alpha = animAlpha
              transformOrigin = TransformOrigin(1f, 0f)
            }
            .liquidGlass(
              backdrop = backdrop,
              shape = LiquidGlassShapes.MenuShape,
              surfaceTint =
                if (palette.isDark) Color(0xFF191E2A).copy(alpha = 0.88f)
                else Color.White.copy(alpha = 0.92f),
              blurRadius = 12.dp,
              refractionHeight = 16.dp,
              refractionAmount = 30.dp,
              shadowRadius = 24.dp,
              shadowOffsetY = 8.dp,
            )
            .padding(vertical = 6.dp)
      ) {
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
          content()
        }
      }
    }
  }
}

/**
 * Pure iOS 26 squishable menu row replacing Material DropdownMenuItem.
 */
@Composable
fun LiquidGlassDropdownMenuItem(
  text: @Composable () -> Unit,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  leadingIcon: (@Composable () -> Unit)? = null,
  trailingIcon: (@Composable () -> Unit)? = null,
  enabled: Boolean = true,
) {
  val fg =
    if (enabled) MaterialTheme.colorScheme.onSurface
    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    modifier =
      modifier
        .fillMaxWidth()
        .defaultMinSize(minHeight = 46.dp)
        .padding(horizontal = 6.dp, vertical = 1.dp)
        .liquidRowSquish(enabled = enabled)
        .clip(LiquidGlassShapes.SmallCardShape)
        .clickable(enabled = enabled, onClick = onClick)
        .padding(horizontal = 12.dp, vertical = 10.dp),
  ) {
    CompositionLocalProvider(LocalContentColor provides fg) {
      if (leadingIcon != null) {
        Box(modifier = Modifier.size(20.dp), contentAlignment = Alignment.Center) {
          leadingIcon()
        }
      }
      Box(modifier = Modifier.weight(1f)) {
        ProvideTextStyle(MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)) {
          text()
        }
      }
      if (trailingIcon != null) {
        Box(modifier = Modifier.size(20.dp), contentAlignment = Alignment.Center) {
          trailingIcon()
        }
      }
    }
  }
}

/**
 * Pure iOS 26 circular Liquid Glass Checkbox with spring checkmark pop.
 */
@Composable
fun LiquidGlassCheckbox(
  checked: Boolean,
  onCheckedChange: ((Boolean) -> Unit)?,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
) {
  val config = LocalLiquidGlassConfig.current
  val palette = LiquidGlassDefaults.palette()
  val bg by
    animateColorAsState(
      targetValue =
        if (checked) MaterialTheme.colorScheme.primary
        else if (palette.isDark) Color.White.copy(alpha = 0.08f)
        else Color.Black.copy(alpha = 0.05f),
      animationSpec = config.colorSpring(),
    )
  val border by
    animateColorAsState(
      targetValue =
        if (checked) MaterialTheme.colorScheme.primary
        else if (palette.isDark) Color.White.copy(alpha = 0.34f)
        else Color.Black.copy(alpha = 0.28f),
      animationSpec = config.colorSpring(),
    )
  val checkScale by
    animateFloatAsState(
      targetValue = if (checked) 1f else 0f,
      animationSpec = config.bouncySpatialSpring(),
    )

  Box(
    contentAlignment = Alignment.Center,
    modifier =
      modifier
        .defaultMinSize(minWidth = 40.dp, minHeight = 40.dp)
        .liquidButtonSquish(enabled = enabled && onCheckedChange != null)
        .then(
          if (onCheckedChange != null) {
            Modifier.clickable(
              enabled = enabled,
              role = Role.Checkbox,
              onClick = { onCheckedChange(!checked) },
            )
          } else {
            Modifier
          }
        ),
  ) {
    Box(
      contentAlignment = Alignment.Center,
      modifier =
        Modifier.size(24.dp)
          .clip(LiquidGlassShapes.CapsuleShape)
          .background(bg, LiquidGlassShapes.CapsuleShape)
          .border(1.4.dp, border, LiquidGlassShapes.CapsuleShape),
    ) {
      if (checkScale > 0.05f) {
        Icon(
          imageVector = Icons.Default.Check,
          contentDescription = null,
          tint = Color.White,
          modifier =
            Modifier.size(15.dp).graphicsLayer {
              scaleX = checkScale
              scaleY = checkScale
            },
        )
      }
    }
  }
}

/**
 * Pure iOS 26 circular Liquid Glass Radio Button with spring inner dot.
 */
@Composable
fun LiquidGlassRadioButton(
  selected: Boolean,
  onClick: (() -> Unit)?,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
) {
  val config = LocalLiquidGlassConfig.current
  val palette = LiquidGlassDefaults.palette()
  val ringColor by
    animateColorAsState(
      targetValue =
        if (selected) MaterialTheme.colorScheme.primary
        else if (palette.isDark) Color.White.copy(alpha = 0.38f)
        else Color.Black.copy(alpha = 0.30f),
      animationSpec = config.colorSpring(),
    )
  val dotScale by
    animateFloatAsState(
      targetValue = if (selected) 1f else 0f,
      animationSpec = config.bouncySpatialSpring(),
    )

  Box(
    contentAlignment = Alignment.Center,
    modifier =
      modifier
        .defaultMinSize(minWidth = 40.dp, minHeight = 40.dp)
        .liquidButtonSquish(enabled = enabled && onClick != null)
        .then(
          if (onClick != null) {
            Modifier.clickable(enabled = enabled, role = Role.RadioButton, onClick = onClick)
          } else {
            Modifier
          }
        ),
  ) {
    Box(
      contentAlignment = Alignment.Center,
      modifier =
        Modifier.size(22.dp)
          .clip(LiquidGlassShapes.CapsuleShape)
          .border(2.dp, ringColor, LiquidGlassShapes.CapsuleShape),
    ) {
      if (dotScale > 0.05f) {
        Box(
          modifier =
            Modifier.size(11.dp)
              .graphicsLayer {
                scaleX = dotScale
                scaleY = dotScale
              }
              .clip(LiquidGlassShapes.CapsuleShape)
              .background(MaterialTheme.colorScheme.primary, LiquidGlassShapes.CapsuleShape)
        )
      }
    }
  }
}

/**
 * Sleek iOS 26 rotating gradient ring spinner replacing Material CircularProgressIndicator.
 */
@Composable
fun LiquidGlassCircularProgress(
  modifier: Modifier = Modifier,
  progress: (() -> Float)? = null,
  color: Color = MaterialTheme.colorScheme.primary,
  strokeWidth: Dp = 3.dp,
) {
  val infinite = rememberInfiniteTransition(label = "liquid_spinner")
  val rotation by
    infinite.animateFloat(
      initialValue = 0f,
      targetValue = 360f,
      animationSpec = infiniteRepeatable(tween(950, easing = LinearEasing), RepeatMode.Restart),
      label = "spinner_rotation",
    )
  val trackColor = color.copy(alpha = 0.22f)
  Canvas(modifier = modifier.size(32.dp)) {
    val strokePx = strokeWidth.toPx()
    drawArc(
      color = trackColor,
      startAngle = 0f,
      sweepAngle = 360f,
      useCenter = false,
      style = Stroke(width = strokePx, cap = StrokeCap.Round),
    )
    val sweep = if (progress != null) (progress().coerceIn(0.05f, 1f) * 360f) else 108f
    val start = if (progress != null) -90f else rotation - 90f
    drawArc(
      brush = Brush.sweepGradient(listOf(color.copy(alpha = 0.25f), color, color)),
      startAngle = start,
      sweepAngle = sweep,
      useCenter = false,
      style = Stroke(width = strokePx, cap = StrokeCap.Round),
    )
  }
}

/**
 * Sleek iOS 26 glowing capsule progress bar replacing Material LinearProgressIndicator.
 */
@Composable
fun LiquidGlassLinearProgress(
  modifier: Modifier = Modifier,
  progress: (() -> Float)? = null,
  color: Color = MaterialTheme.colorScheme.primary,
) {
  val palette = LiquidGlassDefaults.palette()
  val config = LocalLiquidGlassConfig.current
  val trackColor =
    if (palette.isDark) Color.White.copy(alpha = 0.14f) else Color.Black.copy(alpha = 0.10f)
  Box(
    modifier =
      modifier
        .height(6.dp)
        .clip(LiquidGlassShapes.CapsuleShape)
        .background(trackColor, LiquidGlassShapes.CapsuleShape)
  ) {
    if (progress != null) {
      val animated by
        animateFloatAsState(
          targetValue = progress().coerceIn(0f, 1f),
          animationSpec = config.smoothSpatialSpring(),
        )
      Box(
        modifier =
          Modifier.fillMaxHeight()
            .fillMaxWidth(animated)
            .clip(LiquidGlassShapes.CapsuleShape)
            .background(
              Brush.horizontalGradient(listOf(color, color.copy(alpha = 0.78f))),
              LiquidGlassShapes.CapsuleShape,
            )
      )
    } else {
      val infinite = rememberInfiniteTransition(label = "liquid_linear")
      val phase by
        infinite.animateFloat(
          initialValue = -0.35f,
          targetValue = 1.05f,
          animationSpec =
            infiniteRepeatable(tween(1150, easing = LinearEasing), RepeatMode.Restart),
          label = "linear_phase",
        )
      Box(
        modifier =
          Modifier.fillMaxSize().graphicsLayer {
            translationX = phase * size.width
          }
      ) {
        Box(
          modifier =
            Modifier.fillMaxHeight()
              .fillMaxWidth(0.38f)
              .clip(LiquidGlassShapes.CapsuleShape)
              .background(
                Brush.horizontalGradient(
                  listOf(color.copy(alpha = 0.2f), color, color.copy(alpha = 0.2f))
                )
              )
        )
      }
    }
  }
}

/**
 * Subtle translucent iOS hairline divider replacing Material HorizontalDivider.
 */
@Composable
fun LiquidGlassDivider(modifier: Modifier = Modifier, color: Color? = null) {
  val palette = LiquidGlassDefaults.palette()
  val resolvedColor =
    color ?: if (palette.isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.09f)
  Box(
    modifier = modifier.fillMaxWidth().height(0.75.dp).background(resolvedColor)
  )
}

/**
 * Glossy iOS-style capsule badge replacing Material Badge.
 */
@Composable
fun LiquidGlassBadge(
  modifier: Modifier = Modifier,
  containerColor: Color = Color(0xFFFF3B30),
  contentColor: Color = Color.White,
  content: (@Composable RowScope.() -> Unit)? = null,
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.Center,
    modifier =
      modifier
        .defaultMinSize(minWidth = if (content != null) 20.dp else 10.dp, minHeight = 18.dp)
        .clip(LiquidGlassShapes.CapsuleShape)
        .background(containerColor, LiquidGlassShapes.CapsuleShape)
        .border(0.65.dp, Color.White.copy(alpha = 0.45f), LiquidGlassShapes.CapsuleShape)
        .padding(horizontal = if (content != null) 6.dp else 0.dp, vertical = 1.dp),
  ) {
    if (content != null) {
      CompositionLocalProvider(LocalContentColor provides contentColor) {
        ProvideTextStyle(MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)) {
          content()
        }
      }
    }
  }
}

/**
 * Clean overlay badge container replacing Material BadgedBox.
 */
@Composable
fun LiquidGlassBadgedBox(
  badge: @Composable BoxScope.() -> Unit,
  modifier: Modifier = Modifier,
  content: @Composable BoxScope.() -> Unit,
) {
  Box(modifier = modifier) {
    content()
    Box(modifier = Modifier.align(Alignment.TopEnd).offset(x = 6.dp, y = (-4).dp)) {
      badge()
    }
  }
}

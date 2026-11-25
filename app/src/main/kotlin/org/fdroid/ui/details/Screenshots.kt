package org.fdroid.ui.details

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import org.fdroid.R
import org.fdroid.ui.FDroidContent
import org.fdroid.ui.glass.LiquidGlassBottomSheet
import org.fdroid.ui.glass.LiquidGlassButton
import org.fdroid.ui.glass.LiquidGlassDefaults
import org.fdroid.ui.glass.LiquidGlassShapes
import org.fdroid.ui.glass.fastLiquidSurface
import org.fdroid.ui.glass.liquidRowSquish
import org.fdroid.ui.glass.liquidSquish
import org.fdroid.ui.utils.AsyncShimmerImage
import org.fdroid.ui.utils.testApp

@Composable
fun Screenshots(isMetered: Boolean, phoneScreenshots: List<Any>) {
  var showEvenWhenMetered by remember { mutableStateOf(false) }
  if (isMetered && !showEvenWhenMetered)
    Box(
      contentAlignment = Center,
      modifier =
        Modifier.fillMaxWidth()
          .padding(top = 8.dp)
          .padding(horizontal = 16.dp)
          .liquidRowSquish()
          .clip(LiquidGlassShapes.SmallCardShape)
          .clickable { showEvenWhenMetered = true },
    ) {
      Image(
        painterResource(R.drawable.screenshots_placeholder),
        contentDescription = null,
        contentScale = ContentScale.FillWidth,
        modifier =
          Modifier.fillMaxWidth().clip(LiquidGlassShapes.SmallCardShape).semantics {
            hideFromAccessibility()
          },
      )
      LiquidGlassButton(
        onClick = { showEvenWhenMetered = true },
        containerColor = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.86f),
        contentColor = MaterialTheme.colorScheme.inversePrimary,
        modifier = Modifier.padding(horizontal = 16.dp),
      ) {
        Text(text = stringResource(R.string.screenshots_metered), textAlign = TextAlign.Center)
      }
    }
  else {
    Screenshots(phoneScreenshots)
  }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun Screenshots(phoneScreenshots: List<Any>) {
  val listState = rememberLazyListState()
  val snapBehavior = rememberSnapFlingBehavior(lazyListState = listState)
  var showScreenshot by rememberSaveable { mutableStateOf<Int?>(null) }
  val palette = LiquidGlassDefaults.palette()

  val screenshotIndex = showScreenshot
  if (screenshotIndex != null) {
    LiquidGlassBottomSheet(
      onDismissRequest = { showScreenshot = null },
    ) {
      val pagerState =
        rememberPagerState(initialPage = screenshotIndex, pageCount = { phoneScreenshots.size })
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        HorizontalPager(
          state = pagerState,
          overscrollEffect = null,
          modifier = Modifier.fillMaxWidth().height(520.dp).padding(horizontal = 8.dp),
        ) { page ->
          val pageOffset =
            ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction)
              .coerceIn(-1f, 1f)
          val depthScale = 1f - 0.08f * abs(pageOffset)
          val depthAlpha = 1f - 0.25f * abs(pageOffset)
          AsyncShimmerImage(
            model = phoneScreenshots[page],
            contentDescription = "",
            contentScale = ContentScale.Fit,
            placeholder = rememberVectorPainter(Icons.Default.Image),
            error = rememberVectorPainter(Icons.Default.Error),
            modifier =
              Modifier.fillMaxSize()
                .graphicsLayer {
                  scaleX = depthScale
                  scaleY = depthScale
                  alpha = depthAlpha
                }
                .clip(LiquidGlassShapes.SmallCardShape),
          )
        }
        if (phoneScreenshots.size > 1) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier =
              Modifier.fastLiquidSurface(
                shape = LiquidGlassShapes.CapsuleShape,
                surfaceTint = palette.pillSurfaceTint,
                withShadow = false,
              )
                .padding(horizontal = 14.dp, vertical = 6.dp),
          ) {
            Text(
              text =
                stringResource(
                  R.string.screenshot_counter,
                  pagerState.currentPage + 1,
                  phoneScreenshots.size,
                ),
              style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
              color = MaterialTheme.colorScheme.onSurface,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
              repeat(phoneScreenshots.size.coerceAtMost(10)) { idx ->
                val active = idx == pagerState.currentPage % 10
                Box(
                  modifier =
                    Modifier.size(width = if (active) 16.dp else 6.dp, height = 6.dp)
                      .clip(LiquidGlassShapes.CapsuleShape)
                      .background(
                        if (active) MaterialTheme.colorScheme.primary
                        else Color.White.copy(alpha = if (palette.isDark) 0.28f else 0.45f)
                      )
                )
              }
            }
          }
        }
      }
    }
  }

  LazyRow(
    state = listState,
    flingBehavior = snapBehavior,
    contentPadding = PaddingValues(horizontal = 16.dp),
    horizontalArrangement = Arrangement.spacedBy(10.dp),
    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
  ) {
    itemsIndexed(phoneScreenshots) { index, shot ->
      Box(
        modifier =
          Modifier.size(width = 132.dp, height = 240.dp)
            .graphicsLayer {
              val info = listState.layoutInfo
              val itemInfo = info.visibleItemsInfo.firstOrNull { it.index == index }
              if (itemInfo != null && info.viewportSize.width > 0) {
                val viewportCenter = info.viewportSize.width * 0.5f
                val itemCenter = itemInfo.offset + itemInfo.size * 0.5f
                val norm = (abs(itemCenter - viewportCenter) / viewportCenter).coerceIn(0f, 1f)
                val s = 1f - 0.055f * norm
                scaleX = s
                scaleY = s
                alpha = 1f - 0.16f * norm
              }
            }
            .liquidSquish(pressScale = 0.955f, stretchAmount = 0.032f, maxDragTranslationPx = 6f)
            .fastLiquidSurface(
              shape = LiquidGlassShapes.SmallCardShape,
              surfaceTint = palette.cardSurfaceTint,
              withShadow = false,
            )
            .clickable { showScreenshot = index }
            .padding(2.dp),
      ) {
        AsyncShimmerImage(
          model = shot,
          contentDescription = "",
          contentScale = ContentScale.Crop,
          placeholder = rememberVectorPainter(Icons.Default.Image),
          error = rememberVectorPainter(Icons.Default.Error),
          modifier = Modifier.fillMaxSize().clip(LiquidGlassShapes.SmallCardShape),
        )
      }
    }
  }
}

@Preview
@Composable
private fun Preview() {
  FDroidContent { Screenshots(false, testApp.phoneScreenshots) }
}

@PreviewLightDark
@Composable
private fun PreviewMetered() {
  FDroidContent {
    Box(modifier = Modifier.width(300.dp)) { Screenshots(true, testApp.phoneScreenshots) }
  }
}

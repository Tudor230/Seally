package com.example.seally.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A simple pixelated progress bar made from discrete blocks.
 *
 * @param progress in [0f, 1f]
 * @param blocks total number of blocks
 */
@Composable
fun PixelProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    blocks: Int = 16,
    blockWidth: Dp = 6.dp,
    blockHeight: Dp = 10.dp,
    gap: Dp = 2.dp,
    filledColor: Color,
    emptyColor: Color,
    borderColor: Color = emptyColor.copy(alpha = 0.6f),
    cornerRadius: Dp = 3.dp,
) {
    val clamped = progress.coerceIn(0f, 1f)
    val filledBlocks = (clamped * blocks).toInt().coerceIn(0, blocks)

    Row(
        modifier = modifier
            .height(blockHeight)
            .padding(vertical = 1.dp),
        horizontalArrangement = Arrangement.spacedBy(gap),
    ) {
        repeat(blocks) { index ->
            val isFilled = index < filledBlocks
            val color = if (isFilled) filledColor else emptyColor

            Box(
                modifier = Modifier
                    .width(blockWidth)
                    .height(blockHeight)
                    .background(color, RoundedCornerShape(cornerRadius))
                    .border(1.dp, borderColor, RoundedCornerShape(cornerRadius)),
            )
        }
    }
}

/**
 * A continuous (non-blocky) progress bar that renders as a single line.
 *
 * Includes progress semantics so UI tests and accessibility can read the value.
 *
 * @param progress in [0f, 1f]
 */
@Composable
fun LinearProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = 12.dp,
    filledColor: Color,
    trackColor: Color,
    borderColor: Color = trackColor.copy(alpha = 0.6f),
    cornerRadius: Dp = 999.dp,
    borderWidth: Dp = 1.dp,
) {
    val clamped = progress.coerceIn(0f, 1f)
    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .height(height)
            .clip(shape)
            .background(trackColor)
            .border(borderWidth, borderColor, shape)
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(
                    current = clamped,
                    range = 0f..1f,
                )
            },
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(clamped)
                .background(filledColor),
        )
    }
}

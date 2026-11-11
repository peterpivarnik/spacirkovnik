package sk.spacirkovnik.ui.component

import androidx.compose.foundation.ScrollState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Tenký posuvník na pravom okraji obsahu, ktorý sa dá rolovať pomocou [state]. */
fun Modifier.verticalScrollbar(
        state: ScrollState,
        width: Dp = 6.dp,
        color: Color = Color.White.copy(alpha = 0.6f)
): Modifier = drawWithContent {
    drawContent()
    if (state.maxValue > 0) {
        val height = size.height
        val scrollValue = state.value.toFloat()
        val maxScrollValue = state.maxValue.toFloat()

        val scrollbarHeight = (height * height) / (maxScrollValue + height)
        val scrollbarOffset = (scrollValue * (height - scrollbarHeight)) / maxScrollValue

        drawRect(
            color = color,
            topLeft = Offset(size.width - width.toPx(), scrollbarOffset),
            size = Size(width.toPx(), scrollbarHeight)
        )
    }
}

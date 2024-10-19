package com.gfms.koinoor.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.material.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.gfms.koinoor.ui.theme.KoinoorTheme
import kotlin.math.ln


@Composable
fun KoinoorUISurface(
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
    color: Color = KoinoorTheme.colors.uiBackground,
    contentColor: Color = KoinoorTheme.colors.textSecondary,
    border: BorderStroke? = null,
    elevation: Dp = 0.dp,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier.shadow(elevation = elevation, shape=shape, clip=false)
            .zIndex(elevation.value)
            .then(if (border != null) Modifier.border(border, shape) else Modifier)
            .background(color=getBGColorForElevation(color, elevation), shape=shape)
            .clip(shape)
    )
}

@Composable
private fun getBGColorForElevation(color: Color, elevation: Dp): Color {
    return if (elevation > 0.dp) {
        color.withElevation(elevation)
    } else {
        color
    }
}

private fun Color.withElevation(elevation: Dp): Color {
    val fg = calculateForeground(elevation)
    return fg.compositeOver(this)
}

/*
 * @return the alpha-modified [Color.White] to overlay on top of the surface color to produce
 * the resultant color.
 */
private fun calculateForeground(elevation: Dp): Color {
    val alpha = ((4.5f * ln(elevation.value + 1)) + 2f) / 100f
    return Color.White.copy(alpha = alpha)
}
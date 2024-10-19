package com.gfms.koinoor.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.vector.ImageVector

@Stable
class Filter(
    val name: String,
    enabled: Boolean = false,
    val icon: ImageVector? = null
) {
    val enabled = mutableStateOf(enabled)
}
val filters = listOf(
    Filter(name = "Trending"),
    Filter(name = "Most Invested"),
    Filter(name = "New"),
    Filter(name = "Most Used"),
    Filter(name = "State Backed")
)
val valueFilters = listOf(
    Filter(name = "$"),
    Filter(name = "$$"),
    Filter(name = "$$$"),
    Filter(name = "$$$$")
)
val sortFilters = listOf(
    Filter(name = "Android's favorite (default)", icon = Icons.Filled.Android),
    Filter(name = "Rating", icon = Icons.Filled.Star),
    Filter(name = "Alphabetical", icon = Icons.Filled.SortByAlpha)
)

val categoryFilters = listOf(
    Filter(name = "Bitcoin"),
    Filter(name = "ETH and ETH-Based"),
    Filter(name = "GFMS"),
    Filter(name = "Transient Blockchain")
)
val trendFilters = listOf(
    Filter(name = "Trending"),
    Filter(name = "Most Invested"),
    Filter(name = "New"),
    Filter(name = "Most Used"),
    Filter(name = "State Backed")
)

var sortDefault = sortFilters.get(0).name
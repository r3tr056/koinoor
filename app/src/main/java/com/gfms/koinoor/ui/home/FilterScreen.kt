package com.gfms.koinoor.ui.home

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.gfms.koinoor.R
import com.gfms.koinoor.model.Filter
import com.gfms.koinoor.model.cryptoCoinDataRepo
import com.gfms.koinoor.ui.components.FilterChip
import com.gfms.koinoor.ui.components.KoinoorUIScaffold
import com.gfms.koinoor.ui.theme.KoinoorTheme
import com.google.accompanist.flowlayout.FlowMainAxisAlignment
import com.google.accompanist.flowlayout.FlowRow

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun FilterScreen(onDismiss: () -> Unit) {
    var sortState by remember { mutableStateOf(cryptoCoinDataRepo.getSortDefault())}
    var maxValue by remember { mutableStateOf(0f) }
    var defaultFilter = cryptoCoinDataRepo.getSortDefault()

    Dialog(onDismissRequest = onDismiss) {

        val valueFilters = remember { cryptoCoinDataRepo.getValueFilters() }
        val categoryFilters = remember { cryptoCoinDataRepo.getCategoryFilters() }
        val trendFilters = remember { cryptoCoinDataRepo.getTrendFilters() }

        KoinoorUIScaffold(
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(id = R.string.close))
                        }
                    },
                    title = {
                        Text(
                            text = stringResource(id = R.string.label_filters),
                            modifier=Modifier.fillMaxWidth(),
                            textAlign=TextAlign.Center,
                            style=MaterialTheme.typography.h6
                        )
                    },
                    actions = {
                        var resetEnabled = sortState != defaultFilter
                        IconButton(onClick = {}, enabled = resetEnabled) {
                            val alpha = if (resetEnabled) {ContentAlpha.high}
                            else {ContentAlpha.disabled}
                            CompositionLocalProvider(LocalContentAlpha provides alpha) {
                                Text(
                                    text= stringResource(id = R.string.reset),
                                    style=MaterialTheme.typography.body2
                                )
                            }
                        }
                    },
                    backgroundColor = KoinoorTheme.colors.uiBackground
                )
            }
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                SortFilterSection(
                    sortState=sortState,
                    onFilterChange={filter -> sortState = filter.name }
                )
                FilterChipSection(
                    title= stringResource(id = R.string.price),
                    filters=valueFilters
                )
                FilterChipSection(
                    title= stringResource(id = R.string.category),
                    filters=categoryFilters
                )
                MaxValue(
                    sliderPos = maxValue,
                    onValueChanged = {newValue -> maxValue = newValue}
                )
                FilterChipSection(
                    title= stringResource(id = R.string.trend),
                    filters=trendFilters
                )
            }
        }
    }
}

@Composable
fun FilterChipSection(title: String, filters: List<Filter>) {
    FilterTitle(text=title)
    FlowRow(
        mainAxisAlignment = FlowMainAxisAlignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 12.dp)
            .padding(horizontal = 4.dp)
    ) {
        filters.forEach { filter ->
            FilterChip(
                filter=filter,
                modifier=Modifier.padding(end=4.dp,bottom=8.dp)
            )
        }
    }
}

@Composable
fun SortFilterSection(sortState: String, onFilterChange: (Filter) -> Unit) {
    FilterTitle(text= stringResource(id = R.string.sort))
    Column(Modifier.padding(bottom=24.dp)) {
        SortFilters(
            sortState=sortState,
            onChanged=onFilterChange
        )
    }
}

@Composable
fun SortFilters(
    sortFilters: List<Filter> = cryptoCoinDataRepo.getSortFilters(),
    sortState: String,
    onChanged: (Filter) -> Unit
) {
    sortFilters.forEach { filter ->
        SortOption(
            text=filter.name,
            icon=filter.icon,
            selected=sortState==filter.name,
            onClickOption = {
                onChanged(filter)
            }
        )
    }
}

@Composable
fun SortOption(text: String,
               icon: ImageVector?,
               selected: Boolean,
               onClickOption: () -> Unit
) {
    Row(
        modifier=Modifier
            .padding(top=14.dp)
            .selectable(selected) {onClickOption()}
    ) {
        if (icon != null)
            Icon(imageVector = icon, contentDescription = null)
        Text(
            text=text,
            style=MaterialTheme.typography.subtitle1,
            modifier = Modifier
                .padding(start=10.dp)
                .weight(1f)
        )
        if (selected) {
            Icon(
                imageVector = Icons.Filled.Done,
                contentDescription = null,
                tint=KoinoorTheme.colors.brand
            )
        }
    }
}

@Composable
fun FilterTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.h6,
        color = KoinoorTheme.colors.brand,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
fun MaxValue(sliderPos: Float, onValueChanged: (Float) -> Unit) {
    FlowRow {
        FilterTitle(text = stringResource(id = R.string.max_value))
        Text(
            text = stringResource(id = R.string.per_serving),
            style = MaterialTheme.typography.body2,
            color = KoinoorTheme.colors.brand,
            modifier = Modifier.padding(top = 5.dp, start = 10.dp)
        )
    }
    Slider(
        value = sliderPos,
        onValueChange = { newValue ->
            onValueChanged(newValue)
        },
        valueRange = 0f..300f,
        steps = 5,
        modifier = Modifier
            .fillMaxWidth(),
        colors = SliderDefaults.colors(
            thumbColor = KoinoorTheme.colors.brand,
            activeTrackColor = KoinoorTheme.colors.brand
        )
    )
}
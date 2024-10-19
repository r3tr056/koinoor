package com.gfms.koinoor.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gfms.koinoor.model.CryptoCoinCollection
import com.gfms.koinoor.model.Filter
import com.gfms.koinoor.model.cryptoCoinDataRepo
import com.gfms.koinoor.ui.components.FilterBar
import com.gfms.koinoor.ui.components.KoinoorUIDivider
import com.gfms.koinoor.ui.components.KoinoorUISurface
import com.google.accompanist.insets.statusBarsHeight

@Composable
fun Feed(
    onCoinClicked: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val coinCollections = remember { cryptoCoinDataRepo.getCoinCollections() }
    val filters = remember { cryptoCoinDataRepo.getFilters() }

    Feed(
        onCoinClicked=onCoinClicked,
        modifier = modifier,
        coinCollections = coinCollections,
        filters = filters
    )
}

@Composable
private fun Feed(
    coinCollections: List<CryptoCoinCollection>,
    filters: List<Filter>,
    onCoinClicked: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    KoinoorUISurface(modifier=modifier.fillMaxSize()) {
        Box {
            CoinCollectionList(coinCollections, filters, onCoinClicked)
            KoinoorUIDestinationBar()
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun CoinCollectionList(
    coinCollections: List<CryptoCoinCollection>,
    filters: List<Filter>,
    onCoinClicked: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var filtersVisible by rememberSaveable { mutableStateOf(false) }
    Box(modifier) {
        LazyColumn() {
            item {
                Spacer(modifier = Modifier.statusBarsHeight(additional = 56.dp))
                FilterBar(filters = filters, onShowFilters = {filtersVisible=true})
            }
            itemsIndexed(coinCollections) { index, coinCollection ->
                if (index > 0)
                    KoinoorUIDivider(thickness=2.dp)
                // TODO : Pass the `onCoinClicked` handler
                CryptoCoinCollection(
                    id = index.toLong(),
                    cryptoCoins = coinCollection.cryptoCoins,
                    name=coinCollection.name
                )
            }
        }
    }
    AnimatedVisibility(visible = filtersVisible,
        enter= slideInVertically() + expandVertically(
            expandFrom = Alignment.Top
        ) + fadeIn(initialAlpha = 0.3f),
        exit= slideOutVertically() + shrinkVertically() + fadeOut()
    ) {
        FilterScreen(
            onDismiss = {filtersVisible=false}
        )
    }
}

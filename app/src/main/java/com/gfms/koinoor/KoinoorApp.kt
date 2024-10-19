package com.gfms.koinoor

import androidx.compose.foundation.layout.padding
import androidx.compose.material.SnackbarHost
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument
import com.gfms.koinoor.ui.MainDestinations
import com.gfms.koinoor.ui.coindetail.CoinDetail
import com.gfms.koinoor.ui.components.KoinoorUIScaffold
import com.gfms.koinoor.ui.components.KoinoorUISnackbar
import com.gfms.koinoor.ui.home.HomeSections
import com.gfms.koinoor.ui.home.KoinoorUIBottomBar
import com.gfms.koinoor.ui.home.addHomeGraph
import com.gfms.koinoor.ui.rememberKoinoorAppState
import com.gfms.koinoor.ui.theme.KoinoorTheme
import com.google.accompanist.insets.ProvideWindowInsets
import com.google.accompanist.insets.systemBarsPadding

@Composable
fun KoinoorApp() {
    ProvideWindowInsets {
        KoinoorTheme {
            val appState = rememberKoinoorAppState()
            KoinoorUIScaffold(
                bottomBar = {
                    if (appState.shouldShowBottomBar) {
                        KoinoorUIBottomBar(
                            tabs = appState.bottomBarTabs,
                            currentRoute = appState.currentRoute!!,
                            navigateToRoute = appState::navigateToBottomBarRoute
                        )
                    }
                },
                snackbarHost = {
                    SnackbarHost(
                        hostState = it,
                        modifier = Modifier.systemBarsPadding(),
                        snackbar = { snackbarData -> KoinoorUISnackbar(snackbarData) }
                    )
                },
                scaffoldState = appState.scaffoldState
            ) { innerPaddingModifier ->
                NavHost(
                    navController = appState.navController,
                    startDestination = MainDestinations.HOME_ROUTE,
                    modifier = Modifier.padding(innerPaddingModifier)
                ) {
                    koinoorNavGraph(
                        onSnackSelected = appState::navigateToSnackDetail,
                        upPress = appState::upPress
                    )
                }
            }
        }
    }
}

private fun NavGraphBuilder.koinoorNavGraph(
    onSnackSelected: (Long, NavBackStackEntry) -> Unit,
    upPress: () -> Unit
) {
    navigation(
        route = MainDestinations.HOME_ROUTE,
        startDestination = HomeSections.FEED.route
    ) {
        addHomeGraph(onSnackSelected)
    }
    composable(
        "${MainDestinations.COIN_DETAIL_ROUTE}/{${MainDestinations.COIN_DETAIL_ROUTE}}",
        arguments = listOf(navArgument(MainDestinations.COIN_DETAIL_ROUTE) { type = NavType.LongType })
    ) { backStackEntry ->
        val arguments = requireNotNull(backStackEntry.arguments)
        val snackId = arguments.getLong(MainDestinations.COIN_ID_KEY)
        CoinDetail(snackId, upPress)
    }
}
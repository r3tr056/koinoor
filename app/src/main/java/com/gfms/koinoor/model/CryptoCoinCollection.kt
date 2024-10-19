package com.gfms.koinoor.model

import androidx.compose.runtime.Immutable

enum class CollectionType { Normal, Trending, Crashing, Fluctuating, Stable }

@Immutable
data class CryptoCoinCollection(
    val id: Long,
    val name: String,
    val cryptoCoins: List<CryptoCoin>,
    val type: CollectionType = CollectionType.Normal
)

val dashboardCoins = listOf(
    trendingCollection,
    newCollection
)

@Immutable
data class CryptoInvestment(val target: CryptoCoin, val investmentValue: Double)

val cryptoInvestmentQueue = listOf<CryptoInvestment>()

private val investment = 10
private val related: List<CryptoCoinCollection> = listOf()

object cryptoCoinDataRepo {
    fun getCoinCollections(): List<CryptoCoinCollection> = dashboardCoins
    fun getCoinCollection(coinId: Long) = dashboardCoins.find { it.id == coinId }!!
    fun getRelated(@Suppress("UNUSED_PARAMETER") coinId: Long) = related
    fun getTrendFilters() = trendFilters
    fun getFilters() = filters
    fun getValueFilters() = valueFilters
    fun getInvestment() = investment
    fun getSortFilters() = sortFilters
    fun getCategoryFilters() = categoryFilters
    fun getSortDefault() = sortDefault

    fun getCoin(coinId: Long ): CryptoCoin? {
        for (dashboardCoin in dashboardCoins) {
             return dashboardCoin.cryptoCoins.find { it.id == coinId }
        }
        return null
    }
}

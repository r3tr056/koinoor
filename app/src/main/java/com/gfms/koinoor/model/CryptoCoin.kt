package com.gfms.koinoor.model

import androidx.compose.runtime.Immutable

@Immutable
data class CryptoCoin(
    val id: Long,
    val name: String,
    val imageUrl: String,
    val value: Double,
    val desc: String="",
    val tags: Set<String> = emptySet()
)

val cryptoLs = listOf(
    CryptoCoin(
        id=1L,
        name="Bitcoin",
        imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/9/9a/BTC_Logo.svg/1200px-BTC_Logo.svg.png",
        value = 61056.50,
        desc = "Bitcoin is a decentralized digital currency, without a central bank or single administrator, that can be sent from user to user on the peer-to-peer bitcoin network without the need for intermediaries",
    ),
    CryptoCoin(
        id=2L,
        name="Ethereum",
        imageUrl = "https://s2.coinmarketcap.com/static/img/coins/200x200/1027.png",
        value = 4471.44,
        desc = "Ethereum is a decentralized, open-source blockchain with smart contract functionality. Ether is the native cryptocurrency of the platform. Amongst cryptocurrencies, Ether is second only to Bitcoin in market capitalization. Ethereum was conceived in 2013 by programmer Vitalik Buterin",
    ),
    CryptoCoin(
        id=3L,
        name="Cardano",
        imageUrl = "https://logowik.com/content/uploads/images/cardano-ada2887.jpg",
        value = 4471.44,
        desc = "Cardano is a proof-of-stake blockchain platform: the first to be founded on peer-reviewed research and developed through evidence-based methods.",
    ),
    CryptoCoin(
        id=2L,
        name="Koinoor",
        imageUrl = "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcT7QTBuy0j9sLNNulxMatxjcA8wb79QtNoDbw&usqp=CAU",
        value = 4471.44,
        desc = "Koinoor is a proof-of-stake blockchain platform: the first to be founded on peer-reviewed research and developed through evidence-based methods.",
    )
)

val cryptoNew = listOf(
    CryptoCoin(
        id=1L,
        name="Cardano",
        imageUrl = "https://logowik.com/content/uploads/images/cardano-ada2887.jpg",
        value = 4471.44,
        desc = "Cardano is a proof-of-stake blockchain platform: the first to be founded on peer-reviewed research and developed through evidence-based methods.",
    ),
    CryptoCoin(
        id=2L,
        name="Koinoor",
        imageUrl = "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcT7QTBuy0j9sLNNulxMatxjcA8wb79QtNoDbw&usqp=CAU",
        value = 4471.44,
        desc = "Koinoor is a proof-of-stake blockchain platform: the first to be founded on peer-reviewed research and developed through evidence-based methods.",
    )
)

val trendingCollection = CryptoCoinCollection(
    id=1L,
    name="Trending Crypto Coins",
    type=CollectionType.Normal,
    cryptoCoins = cryptoLs
)

val newCollection = CryptoCoinCollection(
    id = 2L,
    name="New Crypto Coins",
    type=CollectionType.Normal,
    cryptoCoins = cryptoNew
)
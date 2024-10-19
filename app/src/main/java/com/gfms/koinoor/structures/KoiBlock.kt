package com.gfms.koinoor.structures

/**
import java.nio.ByteBuffer


class KoiBlock() {

    val HEADER_SIZE = 80
    val PING="PING"
    val ALLOWED_TIME_DRIFT = 2 * 60 * 60
    val LOWEST_DIFFICULTY_ALLOWED = 0x207fFFFFL

    // [START] KoiBlock Header
    val version:Long = 0
    val prevBlockHash: ByteArray = ByteArray(32)
    val merkelRoot: ByteArray? = null
    val timestamp: Long = System.currentTimeMillis()
    val difficulty = 0x1d07fff8L
    val nonce: Long = 0
    // [END] KoiBlock Header

    val fakeClock: Any? = null
    var blockTxs: List<KoiTransaction> = listOf()
    var hash: ByteArray = ByteArray(32)

}
        **/
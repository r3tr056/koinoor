package com.gfms.koinoor.structures

import java.math.BigInteger

interface WalletAddress

interface XID

data class Transaction(
    var chain: BigInteger?,
    var creationTime: Long?,
    var from: WalletAddress?,
    var input: ByteArray,
    var nonce: BigInteger?,
    var to: WalletAddress?,
    var txHash: String?,
    var asset: BigInteger?,
    var blockHash: String?,
    var blockID: BigInteger?,
) {
    constructor(): this (
        chain=null,
        creationTime=null,
        from=null,
        input= ByteArray(0),
        nonce=null,
        to=null,
        txHash=null,
        asset= BigInteger.ZERO,
        blockHash=null,
        blockID=null
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Transaction
        if (chain != other.chain) return false
        if (creationTime != other.creationTime) return false
        if (from != other.from) return false
        if (!input.contentEquals(other.input)) return false
        if (nonce != other.nonce) return false
        if (to != other.to) return false
        if (txHash != other.txHash) return false
        if (asset != other.asset) return false
        return true
    }

    override fun hashCode(): Int {
        var result = chain?.hashCode() ?: 0
        result = 31 * result + (creationTime?.hashCode() ?: 0)
        result = 31 * result + (from?.hashCode() ?: 0)
        result = 31 * result + input.contentHashCode()
        result = 31 * result + (nonce?.hashCode() ?: 0)
        result = 31 * result + (to?.hashCode() ?: 0)
        result = 31 * result + (txHash?.hashCode() ?: 0)
        result = 31 * result + (asset?.hashCode() ?: 0)
        result = 31 * result + (blockHash?.hashCode() ?: 0)
        result = 31 * result + (blockID?.hashCode() ?: 0)
        return result
    }

    fun createTransactionWithDefaults(
        chain: XID? = null,
        creationTime: Long? = null,
        from: WalletAddress,
        value: ByteArray = ByteArray(0),
        nonce: BigInteger? = null,
        to: WalletAddress,
        txHash: String? = null,
        asset: BigInteger,
        blockHash: String?=null,
        blockID: BigInteger?=null,
    ) = Transaction(BigInteger.ZERO, creationTime, from, input, nonce, to, txHash, asset, blockHash, blockID)
}

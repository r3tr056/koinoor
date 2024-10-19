package com.gfms.koinoor.wallet


import com.gfms.koinoor.structures.KoiTransaction
import com.gfms.koinoor.wallet.data.*
import java.math.BigInteger
import java.util.*
import kotlin.collections.ArrayList

private const val CIPHER = "aes-128-ctr"

class XKeyStore

class KoiBlock

data class TXHash(val hash: String)

class Wallet {
    val pendingTx: Map<TXHash, Transaction> = mapOf()

    val unspentTx: Map<TXHash, Transaction> = mapOf()

    val spendTx: Map<TXHash, Transaction> = mapOf()

    private val eventListener :ArrayList<WalletEventsListener> = arrayListOf()

    val walletKeyStore: XKeyStore = XKeyStore()

    fun receive(txRecv: KoiTransaction, storedBlock: KoiBlock, reorg: Boolean=false) {
        val prevBalance: BigInteger = getBalance()
        val txHash = txRecv.txHash
        val assetDebit = txRecv.valueFromMe(this)
        val assetCredit = txRecv.valueToMe(this)
        val valueDiff = assetDebit.difference(assetCredit)
        txRecv.updatedAt = Date()

        if (!reorg) {
            // Log the received asset
        }

        val wtx: KoiTransaction? = null
        if ((wtx = pendingTx))
    }
}
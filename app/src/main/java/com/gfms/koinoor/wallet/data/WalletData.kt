package com.gfms.koinoor.wallet.data

/**
import com.gfms.xnet.crypto.XPrivateKey
import javax.crypto.KeyGenerator
import kotlin.random.Random
import kotlin.text.Charsets.UTF_8

data class KoiWalletConfig(val n: Int, val p: Int)
data class CipherParams(var iv:String)

data class CryptoWallet(
    var cipher: String,
    var ciphertext: String,
    var cipherparams: CipherParams,
    var kdf: String,
    var kdfparams: KdfParams,
    var mac: String
)

internal data class WalletFromImport (
    var address: String?=null,
    var crypto: CryptoWallet?=null,
    var cryptoFromMEW: CryptoWallet?=null,
    var id: String?=null,
    var version: Int = 0
)

data class KoiWallet(
    val address: String?,
    val crypto: CryptoWallet,
    val id: String,
    val version: Int
)

sealed class KdfParams {
    abstract var dklen: Int
    abstract var salt: String?
}

data class AES128CTR_KDFParams(
    var c: Int = 0,
    var prf: String? = null,
    override var dklen: Int = 0,
    override var salt: String? = null
): KdfParams()

open class CipherException: Exception {
    constructor(message: String): super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
}

 **/
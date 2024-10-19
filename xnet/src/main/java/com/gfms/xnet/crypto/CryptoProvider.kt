package com.gfms.xnet.crypto



interface CryptoProvider {
    fun generateKey(): XPrivateKey
    fun keyFromPublicBin(bin: ByteArray): XPublicKey
    fun keyFromPrivateBin(bin: ByteArray): XPrivateKey
    fun isValidPublicBin(bin: ByteArray): Boolean {
        return try {
            keyFromPublicBin(bin)
            true
        } catch (e: Exception) {
            false
        }
    }
}

var defaultCryptoProvider: CryptoProvider = SodiumCryptoProvider
package com.gfms.xnet_android

import com.gfms.xnet.crypto.*
import com.goterl.lazysodium.LazySodiumAndroid
import com.goterl.lazysodium.SodiumAndroid

private val lazySodium = LazySodiumAndroid(SodiumAndroid())

class AndroidCryptoProvider: CryptoProvider {
    override fun generateKey(): XPrivateKey {
        return LibNaClSK.generate(lazySodium)
    }

    override fun keyFromPrivateBin(bin: ByteArray): XPrivateKey {
        return LibNaClSK.fromBin(bin, lazySodium)
    }

    override fun keyFromPublicBin(bin: ByteArray): XPublicKey {
        return LibNaClPK.fromBin(bin, lazySodium)
    }
}
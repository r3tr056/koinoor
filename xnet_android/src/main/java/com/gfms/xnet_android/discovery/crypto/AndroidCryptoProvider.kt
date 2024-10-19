package com.gfms.xnet_android.discovery.crypto

import com.gfms.xnet.crypto.*
import com.goterl.lazysodium.LazySodiumAndroid
import com.goterl.lazysodium.SodiumAndroid

private val lazySod = LazySodiumAndroid(SodiumAndroid())

object AndroidCryptoProvider: CryptoProvider {
    override fun generateKey(): XPrivateKey {
        return LibNaClSK.generate(lazySod)
    }

    override fun keyFromPublicBin(bin: ByteArray): XPublicKey {
        return LibNaClPK.fromBin(bin, lazySod)
    }

    override fun keyFromPrivateBin(bin: ByteArray): XPrivateKey {
        return LibNaClSK.fromBin(bin, lazySod)
    }
}
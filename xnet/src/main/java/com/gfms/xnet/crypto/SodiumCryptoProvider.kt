package com.gfms.xnet.crypto

import com.goterl.lazysodium.LazySodiumJava
import com.goterl.lazysodium.SodiumJava

private val lazySodium = LazySodiumJava(SodiumJava())

object SodiumCryptoProvider: CryptoProvider {
    override fun generateKey(): XPrivateKey {
        return LibNaClSK.generate(lazySodium)
    }

    override fun keyFromPublicBin(bin: ByteArray): XPublicKey {
        return LibNaClPK.fromBin(bin, lazySodium)
    }

    override fun keyFromPrivateBin(bin: ByteArray): XPrivateKey {
        return LibNaClSK.fromBin(bin, lazySodium)
    }
}
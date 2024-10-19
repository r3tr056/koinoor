package com.gfms.xnet.crypto

import com.gfms.xnet.utils.toHex
import com.goterl.lazysodium.LazySodium
import com.goterl.lazysodium.interfaces.Box
import java.lang.IllegalArgumentException


class LibNaClPK(
    val publicKey: ByteArray,
    val verifiyKey: ByteArray,
    private val lazySodium: LazySodium
): XPublicKey {
    override fun verify(sign: ByteArray, msg: ByteArray): Boolean {
        return lazySodium.cryptoSignVerifyDetached(sign, msg, msg.size, verifiyKey)
    }

    override fun getSignatureLength(): Int {
        return LibNaClSK.SIGNATURE_SIZE
    }

    override fun encrypt(msg: ByteArray): ByteArray {
        val cipher = ByteArray(Box.SEALBYTES + msg.size)
        lazySodium.cryptoBoxSeal(cipher, msg, msg.size.toLong(), publicKey)
        return cipher
    }

    override fun keyToBin(): ByteArray {
        return BIN_PREFIX.toByteArray(Charsets.US_ASCII) + publicKey + verifiyKey
    }

    override fun toString(): String {
        return keyToHash().toHex()
    }

    override fun equals(other: Any?): Boolean {
        return other is LibNaClPK && other.keyToHash().toHex() == this.keyToHash().toHex()
    }

    override fun hashCode(): Int {
        return publicKey.contentHashCode()
    }

    companion object {
        private const val BIN_PREFIX = "LibNaClPK:"

        fun fromBin(bin: ByteArray, lazySodium: LazySodium): XPublicKey {
            val publicKeySize = LibNaClSK.PUBLICKEY_BYTES
            val verifiyKeySize = LibNaClSK.SIGN_PUBLICKEY_BYTES
            val binSize = BIN_PREFIX.length + publicKeySize + verifiyKeySize

            val str = bin.toString(Charsets.US_ASCII)
            val binPrefix = str.substring(0, BIN_PREFIX.length)
            if (binPrefix != BIN_PREFIX)
                throw IllegalArgumentException("Bin prefix $binPrefix does not match $BIN_PREFIX")
            if (bin.size != binSize)
                throw IllegalArgumentException("Bin is expected to have $binSize bytes, has ${bin.size} bytes")
            val publicKey = bin.copyOfRange(BIN_PREFIX.length, BIN_PREFIX.length + publicKeySize)
            val verifiyKey = bin.copyOfRange(BIN_PREFIX.length + publicKeySize, BIN_PREFIX.length + publicKeySize + verifiyKeySize)
            return LibNaClPK(publicKey, verifiyKey, lazySodium)
        }
    }
}
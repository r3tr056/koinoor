package com.gfms.xnet.crypto

import java.security.MessageDigest

interface XKey {
    fun pub(): XPublicKey
    fun keyToBin(): ByteArray
    fun keyToHash(): ByteArray {
        return MessageDigest.getInstance("SHA-1").digest(pub().keyToBin())
    }
}

interface XPrivateKey: XKey {
    fun sign(msg: ByteArray): ByteArray
    fun decrypt(msg: ByteArray): ByteArray
}

interface XPublicKey: XKey {
    override fun pub(): XPublicKey {
        return this
    }

    fun verify(sign: ByteArray, msg: ByteArray): Boolean
    fun encrypt(msg: ByteArray): ByteArray
    fun getSignatureLength(): Int
}

data class XKeyPair(val privateKey: XPrivateKey, val publicKey: XPublicKey) {

}
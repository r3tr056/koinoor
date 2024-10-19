package com.gfms.xnet.xpeer

import android.util.Base64
import com.gfms.xnet.ipv4.IPv4Address
import com.gfms.xnet.crypto.XKey
import com.gfms.xnet.crypto.XPrivateKey
import com.gfms.xnet.crypto.XPublicKey
import com.gfms.xnet.xaddress.XAddress
import com.gfms.xnet.utils.toHex
import java.math.BigInteger
import java.util.*
import kotlin.math.max
import kotlin.random.Random

class XID(val bytes: ByteArray) {

    init {
        if (bytes.size < 32 || bytes.size > 50) throw IllegalArgumentException("Invalid id length: ${bytes.size}")
    }

    fun tob64() = b64.encode(bytes)

    fun toHex() = bytes.toHex()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as XID
        return bytes.contentEquals(other.bytes)
    }

    override fun hashCode(): Int {
        return bytes.hashCode()
    }

    override fun toString(): String {
        return tob64()
    }

    companion object {
        // Creates and [XID] from common base64 string repr
        fun fromb64(str: String): XID {
            return XID(b64.decode())
        }

        fun fromHex(str: String): XID {
            return XID(str.fromHex())
        }

        fun fromPubKey(pubKey: XPublicKey): XID {
            val pubKeyBytes = marshalPubKey(pubKey)
            val desc = when {
                pubKeyBytes.size <= 42 -> hash.descriptor(hash.digest.identity)
                else -> hash.descriptor(hash.digest.sha2, 256)
            }
            val hsh = hash.digest(descriptor, pubKeyBytes.toByteBuf())
            return XID(hsh.bytes.toByteArray())
        }

        fun random(): XID {
            return XID(Random.nextBytes(32))
        }
    }
}

data class XPeer(
    // Address of the peer it used to connect to us
    var xaddrs: List<XAddress> = mutableListOf(),
    var privateKey: XPrivateKey?=null,
    var publicKey: XPublicKey?=privateKey?.pub(),
    ) {
    /**
    // NOTE : lamport timestamp has currently no use in our impl
    private var _lamportTimestamp = 0uL
    val lamportTimestamp: ULong
        get() = _lamportTimestamp
    **/

    // Member ID
    val mid: String
        get() = privateKey!!.keyToHash().toHex()

    val peerid: XID
        get() = XID(publicKey)
    var protocols = mutableListOf<String>()
    var metadata = mutableMapOf<String, Any>()
    val reputation: Int = 0

    fun addProtocols(protos: List<String>) {
        protocols.addAll(protos)
    }

    fun clearXAddrs() {
        xaddrs = mutableListOf()
    }

    fun putMetadata(key: String, value: Any) {
        metadata.put(key, value)
    }

    fun distance(other: XPeer): BigInteger {
        return peerid.xor_id xor peerid.xor_id
    }

    fun idDistance(other_id: BigInteger): BigInteger {
        return peerid.xor_id xor other_id
    }

    /**
    // Timestamp of the last share with the peer
    var lastRequest: Date? = null
    // Timestamp of the last share with the peer
    var lastResp: Date? = if (intro) null else Date()
    // The duration of last pings(list) in seconds
    val pings = mutableListOf<Double>()

    /**
     * update the lamport timestamp for this peer. The lamport clock dictates that the current
     * timestamp is the maximum of the last known and the most recent. This is useful when the
     * MessageAPI works async
     * We also keep a real time timestamp of the last recvd message for timeout purposes
     * @param timestamp : A received timestamp
     */
    fun updateClock(timestamp: ULong) {
        _lamportTimestamp = max(_lamportTimestamp, timestamp)
        lastResp = Date()
    }
    // The average of the last [MAX_PINGS] pings, in seconds
    fun getAveragePing(): Double {
        return pings.average()
    }
    // Adds a new ping duration to the ping list
    fun addPing(ping: Double) {
        pings.add(ping)
        if (pings.size > MAX_PINGS) {
            pings.removeAt(0)
        }
    }
    **/
    // Checks if the address is empty or not
    fun isConnected(): Boolean {
        return !address.isEmpty()
    }

    companion object {
        const val MAX_PINGS = 5
        const val K_PUBKEY_SIZE = 512
        // used by kademlia
        const val K_ID_SIZE = 256

        // Creates an XPeer instance for a XKey and a peer address
        fun createFromAddress(xkey: XPrivateKey, source: XAddress): XPeer {
            return when(source) {
                is IPv4Address -> XPeer(privateKey = xkey, xaddrs = mutableListOf(source))
                else -> XPeer(privateKey = xkey)
            }
        }
    }
}
package com.gfms.xnet.packet

import com.gfms.xnet.crypto.XPrivateKey
import com.gfms.xnet.xaddress.XAddress
import com.gfms.xnet.xpeer.XPeer
import com.gfms.xnet.crypto.defaultCryptoProvider
import com.gfms.xnet.exceptions.PacketExceptions.PacketDecodingException
import com.gfms.xnet.packet.payloads.XNetAuthPayload
import com.gfms.xnet.packet.payloads.XNetTimeSyncPayload
import com.gfms.xnet.serialization.Deserializable
import java.lang.IllegalArgumentException

class XPacket(
    val source: XAddress,
    val data: ByteArray
    ) {

    //Deserializes an unauthenticated packet and returns the main payload
    fun <T> getPayload(deserializer: Deserializable<T>): T {
        val remainder = getPayload()
        val (_, distSize) = XNetTimeSyncPayload.deserialize(remainder)
        val (payload, _) = deserializer.deserialize(remainder, distSize)
        return payload
    }
    /**
     * Checks the signature of an authenticated packet payload and throws [PacketDecodingExpection]
     * if invalid. Returns the peer initialized using the packet source address and the public key
     * from [BinMemoryAuthPayload], and the main deserialized payload
     * @throws PacketDecodingException If the packet is authed and the sign is invalid
     */
    @Throws(PacketDecodingException::class)
    fun <T> getAuthPayload(deserializer: Deserializable<T>): Pair<XPeer, T> {
        val (peer, remainder) = getAuthPayload()
        val (_, distSize) = XNetTimeSyncPayload.deserialize(remainder)
        val (payload, _) = deserializer.deserialize(remainder, distSize)
        return Pair(peer, payload)
    }

    @Throws(PacketDecodingException::class)
    fun <T> getAuthPayloadWithDist(deserializer: Deserializable<T>): Triple<XPeer, XNetTimeSyncPayload, T> {
        val (peer, remainder) = getAuthPayload()
        val (dist, distSize) = XNetTimeSyncPayload.deserialize(remainder)
        val (payload, _) = deserializer.deserialize(remainder, distSize)
        return Triple(peer, dist, payload)
    }

    @Throws(PacketDecodingException::class)
    fun <T> getDecryptedAuthPayload(deserializer: Deserializable<T>, privateKey: XPrivateKey): Pair<XPeer, T> {
        val (peer, remainder) = getAuthPayload()
        val (_, distSize) = XNetTimeSyncPayload.deserialize(remainder)
        val encrypted = remainder.copyOfRange(distSize, remainder.size)
        val decrypted = privateKey.decrypt(encrypted)
        val (payload, _) = deserializer.deserialize(decrypted, 0)
        return Pair(peer, payload)
    }

    private fun getPayload(): ByteArray {
        return data.copyOfRange(PREFIX_SIZE + 1, data.size)
    }

    @Throws(PacketDecodingException::class)
    private fun getAuthPayload(): Pair<XPeer, ByteArray> {
        // prefix + message type
        val authOffset = PREFIX_SIZE + 1
        val (auth, authSize) = XNetAuthPayload.deserialize(data, authOffset)
        val pubKey = try {
            defaultCryptoProvider.keyFromPublicBin(auth.publicKey)
        } catch (e: IllegalArgumentException) {
            throw PacketDecodingException("Incoming packet has an individual public key", e)
        }

        val signOffset = data.size - pubKey.getSignatureLength()
        val sign = data.copyOfRange(signOffset, data.size)
        // Verify signature
        val msg = data.copyOfRange(0, signOffset)
        val isValidSign = pubKey.verify(sign, msg)
        if (!isValidSign)
            throw PacketDecodingException("Incomming packet has an invalid signature")

        // Return the peer and remaining payloads
        val peer = XPeer.createFromAddress(xkey = pubKey, source = source)
        val remainder = data.copyOfRange(authOffset + authSize, data.size - pubKey.getSignatureLength())
        return Pair(peer, remainder)
    }

    companion object {
        // The service ID size in bytes
        const val SERVICE_ID_SIZE = 20
        // The prefix size in bytes
        private const val PREFIX_SIZE = SERVICE_ID_SIZE + 2
    }
}


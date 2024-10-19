package com.gfms.xnet.packet.payloads

import com.gfms.xnet.ipv4.IPv4Address
import com.gfms.xnet.serialization.*
import com.gfms.xnet.utils.createConnectionByte
import com.gfms.xnet.utils.deserializeConnectionByte

data class XIntroRequestPayload(
    val destAddress: IPv4Address,
    val sourceLanAddress: IPv4Address,
    val sourceWanAddress: IPv4Address,
    val advice: Boolean,
    val connectionType: ConnectionType,
    val identifier: Int,
    val extraBytes: ByteArray = byteArrayOf(),
): Serializable {

    override fun serialize(): ByteArray {
        return destAddress.serialize() +
                sourceLanAddress.serialize() +
                sourceWanAddress.serialize() +
                createConnectionByte(connectionType, advice) +
                serializeUShort(identifier) +
                extraBytes
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as XIntroRequestPayload
        if (destAddress != other.destAddress) return false
        if (sourceLanAddress != other.sourceLanAddress) return false
        if (sourceWanAddress != other.sourceWanAddress) return false
        if (advice != other.advice) return false
        if (connectionType != other.connectionType) return false
        if (identifier != other.identifier) return false
        if (!extraBytes.contentEquals(other.extraBytes)) return false
        return true
    }

    override fun hashCode(): Int {
        var res = destAddress.hashCode()
        res = 31 * res + sourceLanAddress.hashCode()
        res = 31 * res + sourceWanAddress.hashCode()
        res = 31 * res + advice.hashCode()
        res = 31 * res + connectionType.hashCode()
        res = 31 * res + identifier
        res = 31 * res + extraBytes.hashCode()
        return res
    }

    companion object Deserializer: Deserializable<XIntroRequestPayload> {
        override fun deserialize(buffer: ByteArray, offset: Int): Pair<XIntroRequestPayload, Int> {
            var localOffset = 0
            val (destAddr, _) = IPv4Address.deserialize(buffer, offset + localOffset)
            localOffset += IPv4Address.SERIALIZED_SIZE
            val (sourceLanAddr, _) = IPv4Address.deserialize(buffer, offset + localOffset)
            localOffset += IPv4Address.SERIALIZED_SIZE
            val (sourceWanAddr, _) = IPv4Address.deserialize(buffer, offset + localOffset)
            localOffset += IPv4Address.SERIALIZED_SIZE
            val (advice, connectionType) = deserializeConnectionByte(buffer[offset + localOffset])
            localOffset ++
            val identifier = deserializeUShort(buffer, offset + localOffset)
            localOffset += SERIALIZED_USHORT_SIZE
            val (extraBytes, extraBytesLen) = deserializeRaw(buffer, offset + localOffset)
            localOffset += extraBytesLen

            val payload = XIntroRequestPayload(
                destAddr, sourceLanAddr, sourceWanAddr,
                advice, connectionType, identifier, extraBytes
            )
            return Pair(payload, localOffset)
        }
    }
}
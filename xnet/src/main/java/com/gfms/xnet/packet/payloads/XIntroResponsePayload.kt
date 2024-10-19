package com.gfms.xnet.packet.payloads

import com.gfms.xnet.ipv4.IPv4Address
import com.gfms.xnet.serialization.*
import com.gfms.xnet.utils.createConnectionByte
import com.gfms.xnet.utils.deserializeConnectionByte


data class XIntroResponsePayload(

    val destinationAddress: IPv4Address,

    val sourceLanAddress: IPv4Address,

    val sourceWanAddress: IPv4Address,

    val lanIntroductionAddress: IPv4Address,

    val wanIntroductionAddress: IPv4Address,

    val connectionType: ConnectionType,

    val tunnel: Boolean,

    val identifier: Int,

    val extraBytes: ByteArray = byteArrayOf()
) : Serializable {
    override fun serialize(): ByteArray {
        return destinationAddress.serialize() +
                sourceLanAddress.serialize() +
                sourceWanAddress.serialize() +
                lanIntroductionAddress.serialize() +
                wanIntroductionAddress.serialize() +
                createConnectionByte(connectionType) +
                serializeUShort(identifier) +
                extraBytes
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as XIntroResponsePayload

        if (destinationAddress != other.destinationAddress) return false
        if (sourceLanAddress != other.sourceLanAddress) return false
        if (sourceWanAddress != other.sourceWanAddress) return false
        if (lanIntroductionAddress != other.lanIntroductionAddress) return false
        if (wanIntroductionAddress != other.wanIntroductionAddress) return false
        if (connectionType != other.connectionType) return false
        if (tunnel != other.tunnel) return false
        if (identifier != other.identifier) return false
        if (!extraBytes.contentEquals(other.extraBytes)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = destinationAddress.hashCode()
        result = 31 * result + sourceLanAddress.hashCode()
        result = 31 * result + sourceWanAddress.hashCode()
        result = 31 * result + lanIntroductionAddress.hashCode()
        result = 31 * result + wanIntroductionAddress.hashCode()
        result = 31 * result + connectionType.hashCode()
        result = 31 * result + tunnel.hashCode()
        result = 31 * result + identifier
        result = 31 * result + extraBytes.contentHashCode()
        return result
    }

    companion object Deserializer : Deserializable<XIntroResponsePayload> {
        override fun deserialize(buffer: ByteArray, offset: Int): Pair<XIntroResponsePayload, Int> {
            var localOffset = 0
            val (destinationAddress, _) = IPv4Address.deserialize(buffer, offset + localOffset)
            localOffset += IPv4Address.SERIALIZED_SIZE
            val (sourceLanAddress, _) = IPv4Address.deserialize(buffer, offset + localOffset)
            localOffset += IPv4Address.SERIALIZED_SIZE
            val (sourceWanAddress, _) = IPv4Address.deserialize(buffer, offset + localOffset)
            localOffset += IPv4Address.SERIALIZED_SIZE
            val (lanIntroductionAddress, _) = IPv4Address.deserialize(buffer, offset + localOffset)
            localOffset += IPv4Address.SERIALIZED_SIZE
            val (wanIntroductionAddress, _) = IPv4Address.deserialize(buffer, offset + localOffset)
            localOffset += IPv4Address.SERIALIZED_SIZE
            val (_, connectionType) = deserializeConnectionByte(buffer[offset + localOffset])
            localOffset++
            val identifier = deserializeUShort(buffer, offset + localOffset)
            localOffset += SERIALIZED_USHORT_SIZE
            val (extraBytes, extraBytesLen) = deserializeRaw(buffer, offset + localOffset)
            localOffset += extraBytesLen
            val payload = XIntroResponsePayload(
                destinationAddress,
                sourceLanAddress,
                sourceWanAddress,
                lanIntroductionAddress,
                wanIntroductionAddress,
                connectionType,
                false,
                identifier,
                extraBytes
            )
            return Pair(payload, localOffset)
        }
    }
}
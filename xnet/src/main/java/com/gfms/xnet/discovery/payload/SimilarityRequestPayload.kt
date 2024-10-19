package com.gfms.xnet.discovery.payload

import com.gfms.xnet.ipv4.IPv4Address
import com.gfms.xnet.packet.payloads.ConnectionType
import com.gfms.xnet.serialization.*
import com.gfms.xnet.utils.hexToBytes
import com.gfms.xnet.utils.toHex

data class SimilarityRequestPayload(
    val identifier: Int,
    val lanAddress: IPv4Address,
    val wanAddress: IPv4Address,
    val connectionType: ConnectionType,
    // List of SIDs supported by the sender
    val preferenceList: List<String>
): Serializable {
    override fun serialize(): ByteArray {
        return serializeUShort(identifier % UShort.MAX_VALUE.toInt()) +
                lanAddress.serialize() +
                wanAddress.serialize() +
                connectionType.serialize() +
                preferenceList.joinToString("").hexToBytes()
    }

    companion object Deserializer: Deserializable<SimilarityRequestPayload> {
        override fun deserialize(
            buffer: ByteArray,
            offset: Int
        ): Pair<SimilarityRequestPayload, Int> {

            var localOffset = 0
            val identifier = deserializeUShort(buffer, offset)
            localOffset += SERIALIZED_USHORT_SIZE
            val (lanAddress, _) = IPv4Address.deserialize(buffer, offset + localOffset)
            localOffset += IPv4Address.SERIALIZED_SIZE
            val (wanAddress, _) = IPv4Address.deserialize(buffer, offset + localOffset)
            localOffset += IPv4Address.SERIALIZED_SIZE
            val (connectionType, _) = ConnectionType.deserialize(buffer, offset + localOffset)
            localOffset++
            val preferenceListSerialized = buffer.copyOfRange(offset + localOffset, buffer.size)
            val preferenceList = mutableListOf<String>()
            for (i in 0 until preferenceListSerialized.size / 20) {
                preferenceList += preferenceListSerialized.copyOfRange(20 * i, 20 * i + 20).toHex()
            }
            localOffset += preferenceListSerialized.size
            return Pair(
                SimilarityRequestPayload(
                identifier, lanAddress, wanAddress, connectionType, preferenceList
            ), localOffset)
        }
    }
}
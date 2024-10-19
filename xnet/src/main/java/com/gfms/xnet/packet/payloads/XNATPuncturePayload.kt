package com.gfms.xnet.packet.payloads

import com.gfms.xnet.ipv4.IPv4Address
import com.gfms.xnet.serialization.*

data class XNATPuncturePayload(
    val sourceLanAddress: IPv4Address,
    val sourceWanAddress: IPv4Address,
    val identifier: Int
): Serializable {

    override fun serialize(): ByteArray {
        return sourceLanAddress.serialize() +
                sourceWanAddress.serialize() +
                serializeUShort(identifier)
    }

    companion object Deserializer : Deserializable<XNATPuncturePayload> {
        override fun deserialize(buffer: ByteArray, offset: Int): Pair<XNATPuncturePayload, Int> {
            var localOffset = 0
            val (sourceLanAddress, _) = IPv4Address.deserialize(buffer, offset + localOffset)
            localOffset += IPv4Address.SERIALIZED_SIZE
            val (sourceWanAddress, _) = IPv4Address.deserialize(buffer, offset + localOffset)
            localOffset += IPv4Address.SERIALIZED_SIZE
            val identifier = deserializeUShort(buffer, offset + localOffset)
            localOffset += SERIALIZED_USHORT_SIZE
            val payload = XNATPuncturePayload(sourceLanAddress, sourceWanAddress, identifier)
            return Pair(payload, localOffset)
        }
    }
}
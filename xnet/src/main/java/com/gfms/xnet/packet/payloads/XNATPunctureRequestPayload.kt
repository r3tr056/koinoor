package com.gfms.xnet.packet.payloads

import com.gfms.xnet.ipv4.IPv4Address
import com.gfms.xnet.serialization.*

data class XNATPunctureRequestPayload(
    val lanWalkerAddress: IPv4Address,
    val wanWalkerAddress: IPv4Address,
    val identifier: Int
): Serializable {

    override fun serialize(): ByteArray {
        return lanWalkerAddress.serialize() +
                wanWalkerAddress.serialize() +
                serializeUShort(identifier)
    }

    companion object Deserializer : Deserializable<XNATPunctureRequestPayload> {
        override fun deserialize(buffer: ByteArray, offset: Int): Pair<XNATPunctureRequestPayload, Int> {
            var localOffset = 0
            val (lanWalkerAddress, _) = IPv4Address.deserialize(buffer, offset + localOffset)
            localOffset += IPv4Address.SERIALIZED_SIZE
            val (wanWalkerAddress, _) = IPv4Address.deserialize(buffer, offset + localOffset)
            localOffset += IPv4Address.SERIALIZED_SIZE
            val identifier = deserializeUShort(buffer, offset + localOffset)
            localOffset += SERIALIZED_USHORT_SIZE
            val payload = XNATPunctureRequestPayload(lanWalkerAddress, wanWalkerAddress, identifier)
            return Pair(payload, localOffset)
        }
    }
}
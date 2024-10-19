package com.gfms.xnet.packet.payloads

import com.gfms.xnet.serialization.Deserializable
import com.gfms.xnet.serialization.SERIALIZED_USHORT_SIZE
import com.gfms.xnet.serialization.deserializeUShort
import com.gfms.xnet.serialization.serializeUShort
import com.gfms.xnet.serialization.Serializable


class XNetAuthPayload(val publicKey: ByteArray): Serializable {

    override fun serialize(): ByteArray {
        return serializeUShort(publicKey.size) + publicKey
    }
    
    companion object: Deserializable<XNetAuthPayload> {
        override fun deserialize(buffer: ByteArray, offset: Int): Pair<XNetAuthPayload, Int> {
            var localOffset = 0
            val payloadSize = deserializeUShort(buffer, offset)
            localOffset += SERIALIZED_USHORT_SIZE
            val publicKey = buffer.copyOfRange(offset + localOffset, offset + localOffset + payloadSize)
            localOffset += payloadSize
            return Pair(XNetAuthPayload(publicKey), localOffset)
        }
    }
}
package com.gfms.xnet.discovery.payload

import com.gfms.xnet.serialization.Deserializable
import com.gfms.xnet.serialization.Serializable
import com.gfms.xnet.serialization.SERIALIZED_USHORT_SIZE
import com.gfms.xnet.serialization.deserializeUShort
import com.gfms.xnet.serialization.serializeUShort


data class PongPayload(
    val identifier: Int
): Serializable {
    override fun serialize(): ByteArray {
        return serializeUShort(identifier % UShort.MAX_VALUE.toInt())
    }

    companion object Deserializer: Deserializable<PongPayload> {
        override fun deserialize(buffer: ByteArray, offset: Int): Pair<PongPayload, Int> {
            val identifier = deserializeUShort(buffer, offset)
            return Pair(PongPayload(identifier), offset + SERIALIZED_USHORT_SIZE)
        }
    }
}
package com.gfms.xnet.discovery.payload

import com.gfms.xnet.serialization.Deserializable
import com.gfms.xnet.serialization.Serializable
import com.gfms.xnet.serialization.SERIALIZED_USHORT_SIZE
import com.gfms.xnet.serialization.deserializeUShort
import com.gfms.xnet.serialization.serializeUShort

data class PingPayload(
    val identifier: Int
): Serializable {
    override fun serialize(): ByteArray {
        return serializeUShort(identifier % UShort.MAX_VALUE.toInt())
    }

    companion object Deserializer: Deserializable<PingPayload> {
        override fun deserialize(buffer: ByteArray, offset: Int): Pair<PingPayload, Int> {
            val identifier = deserializeUShort(buffer, offset)
            return Pair(PingPayload(identifier), offset + SERIALIZED_USHORT_SIZE)
        }
    }
}
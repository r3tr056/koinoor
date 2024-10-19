package com.gfms.xnet.packet.payloads

import com.gfms.xnet.serialization.Deserializable
import com.gfms.xnet.serialization.SERIALIZED_ULONG_SIZE
import com.gfms.xnet.serialization.deserializeULong
import com.gfms.xnet.serialization.serializeULong
import com.gfms.xnet.serialization.Serializable

@OptIn(ExperimentalUnsignedTypes::class)
data class XNetTimeSyncPayload(
    val globalTime: ULong
): Serializable {

    override fun serialize(): ByteArray {
        return serializeULong(globalTime)
    }

    companion object : Deserializable<XNetTimeSyncPayload> {

        override fun deserialize(buffer: ByteArray,
                                 offset: Int
        ): Pair<XNetTimeSyncPayload, Int> {
            var localOffset = 0
            val globalTime = deserializeULong(buffer, offset + localOffset)
            localOffset += SERIALIZED_ULONG_SIZE
            return Pair(XNetTimeSyncPayload(globalTime), localOffset)
        }
    }
}
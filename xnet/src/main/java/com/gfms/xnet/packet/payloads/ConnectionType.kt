package com.gfms.xnet.packet.payloads

import com.gfms.xnet.serialization.Deserializable
import com.gfms.xnet.utils.createConnectionByte
import com.gfms.xnet.utils.deserializeConnectionByte
import com.gfms.xnet.serialization.Serializable

enum class ConnectionType(
    val value: String,
    val encoding: Pair<Boolean, Boolean>
) : Serializable {
    UNKNOWN("unknown", false to false),
    PUBLIC("public", true to false),
    SYMMETRIC_NAT("symmetric-NAT", true to true);

    override fun serialize(): ByteArray {
        val bytes = ByteArray(1)
        bytes[0] = createConnectionByte(this)
        return bytes
    }

    companion object : Deserializable<ConnectionType> {
        fun decode(bit1: Boolean, bit2: Boolean): ConnectionType {
            for (type in values()) {
                if (type.encoding.first == bit1 && type.encoding.second == bit2) {
                    return type
                }
            }
            return UNKNOWN
        }

        override fun deserialize(buffer: ByteArray, offset: Int): Pair<ConnectionType, Int> {
            val (_, connectionType) = deserializeConnectionByte(buffer[offset])
            return Pair(connectionType, 1)
        }
    }
}
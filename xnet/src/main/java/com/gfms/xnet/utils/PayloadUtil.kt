package com.gfms.xnet.utils

fun createConnectionByte(connectionType: ConenctionType, advice: Boolean = false): Byte {
    var connectionByte: UByte = 0x00u
    if (connectionType.encoding.first) {
        connectionByte = connectionByte or 0x80.toUByte()
    }
    if (connectionType.encoding.second) {
        connectionByte = connectionByte or 0x40.toUByte()
    }
    if (advice) {
        connectionByte = connectionByte or 0x01.toUByte()
    }
    return connectionByte.toByte()
}

fun deserializeConnectionByte(byte: Byte): Pair<Boolean, ConnectionType> {
    val advice = (byte and )
}
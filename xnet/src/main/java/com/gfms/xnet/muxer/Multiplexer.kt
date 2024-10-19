package com.gfms.xnet

import java.lang.Exception
import java.math.BigInteger


class Multiplexer {
}

class FrameCipherBase() {
    val MAC_LEN: Int = 16
    val header_len: Int = 32

}

interface StreamMultiplexerProtocol {
    val version: String

    fun <TController> createMultiplexedStream(bindings: List<ProtocolBinding<TController>>): Multiplexer<TController>
}

interface StreamMultiplexerProtocolDebug: StreamMultiplexerProtocol {
    fun copyWithHandlers(preHandler: XStreamHandler<*>?=null, postHandler: XStreamHandler<*>?=null): StreamMultiplexerProtocol
}


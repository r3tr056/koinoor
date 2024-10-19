package com.gfms.xnet.xconnection

import com.gfms.xnet.XConnection
import java.util.concurrent.CompletableFuture

open class XConnectionManager(
    private val secureMultistream: Any,
    private val secureStreams: List<Any>,
    private val muxerMultistream: Any,
    private val muxers: List<StreamMuxer>
) {

    open fun establishSecureStream(xconn: XConnection): CompletableFuture<SecureStream.Session> {
        return establish(secureMultistream, xconn, secureStreams)
    }

    open fun establishMuxer(xconn: XConnection): CompletableFuture<StreamMuxer.Session> {
        return establish(muxerMultistream, xconn, muxers)
    }

    private fun <T: ProtocolBinding<R>, R> establish(multistreamProtocol: Any, xconn: XConnection, xstreams: List<T>): CompletableFuture<R> {
        val multistream = multistreamProtocol.createMultistream(xstreams)
        return multistream.initChannel(xconn)
    }
}
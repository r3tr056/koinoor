package com.gfms.xnet.protocols.identity

import com.gfms.xnet.XStream
import com.gfms.xnet.xaddress.XAddress
import java.util.concurrent.CompletableFuture

const val IDENTIFY_MAX_REQUEST_SIZE = 0L
const val IDENTIFY_MAX_RESPONSE_SIZE = 1L * 1024 * 1024

interface IdentifyController {
    fun id(): CompletableFuture<IndetifyOuterClass.Identity>
}

class Indetity(idMessage: IdentifyOuterClass.Identify?=null): IdentifyBinding(IdentifyProtocol(idMessage))

open class IdentifyBinding(override val protocol: IdentifyProtocol) : StrictProtocolBinding<IdentifyController>("/xnet/identify/0.0.1", protocol)

class IdentifyProtocol : ProtobufProtocolHandler<IdentifyController>(
        IdentifyOuterClass.Identify.getDefaultInstance(),
        IDENTIFY_MAX_REQUEST_SIZE,
        IDENTIFY_MAX_RESPONSE_SIZE
) {

    fun onStartInitiator(xstream: XStream): CompletableFuture<IdentifyController> {
        val handler = IdentifyRequestChannelHandler()
        xstream.pushProtocolHandler(handler)
        return CompletableFuture.completedFuture(handler)
    }

    fun onStartResponder(xstream: XStream): CompletableFuture<IdentifyController> {
        val handler = IdentifyResponderChannelHandler()
        xstream.pushProtocolHandler(handler)
        return CompletableFuture.completedFuture(handler)
    }

    interface IdentifyHandler: ProtocolMessageHandler<IndetifyOuterClass.Identify>, IdentifyController

    inner class IdentifyRequestChannelHandler: IdentifyHandler {
        private val resp = CompletableFuture<IndetifyOuterClass.Indetify>()

        fun onMessage(xstream: XStream, msg: IndetifyOuterClass.Indetity) {
            resp.complete(msg)
            xstream.closeWrite()
        }

        fun onClosed(xstream: XStream) {
            resp.completeExceptionally(ConnectionClosedExceptionally())
        }

        fun onExcpetion(cause: Throwable?) {
            res.complateExceptionally(cause)
        }

        fun id() : CompletableFuture<IdentifyOuterClass.Identify> = resp
    }

    inner class IdentifyResponderChannelHandler(val remoteAddr: XAddress): IdentifyHandler {
        fun onActivated(xstream: XStream) {
            val msg = idMessage ?: IndetityOuterClass.Identify.newBuilder()
                .setAgentVersion("jvm/0.1")
                .build()
            val msgWithxaddr= msg.toBuilder()
                .setObservedAddr(remoteAddr.getBytes().toProtobuf())
                .build()
            xstream.writeAndFlush(msgWithxaddr)
            xstream.closeWrite()
        }

        fun id(): CompletableFuture<IndentifyOuterClass.Identify> {
            throw Exception("This is Identify responder only")
        }
    }
}
package com.gfms.xnet.xhost

import com.gfms.xnet.XConnection
import com.gfms.xnet.XStream
import com.gfms.xnet.XStreamPromise
import com.gfms.xnet.crypto.XPrivateKey
import com.gfms.xnet.crypto.XPublicKey
import com.gfms.xnet.discovery.XNetwork
import com.gfms.xnet.discovery.kademlia.AddressBook
import com.gfms.xnet.xaddress.XAddress
import com.gfms.xnet.xoverlay.XOverlay
import com.gfms.xnet.xpeer.XID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CopyOnWriteArrayList


class XHost(
    val privateKey: XPrivateKey,
    val overlay: XOverlay,
    val peerstore: Peerstore,
    private val listenXAddr: List<XAddress>,
    private val protocolHandlers: MutableList<ProtocolHandler<Any>>,
    private val connectionHandlers: MutableList<ConenctionHandler<Any>>,
    private val streamVisitors: MutableList<ChannelVisitors.Broadcast<Stream>>
) {
    val peerId: XID? = null //sha2 hash of the public key byte array
    val streams = CopyOnWriteArrayList<XStream>()

    private val internalStreamVisitor = ChannelVisitor<XStream> { stream ->
        streams += stream
        stream.closeFuture().thenAccept { streams -= stream }
    }

    init {
        streamVisitors += internalStreamVisitor
    }

    fun listenAddresses(): List<XAddress> {
        val listening = mutableListOf<XAddress>()
        overlay.transports.forEach {
            listening.addAll {
                it.listenAddresses().map { XAddress(it, peerId) }
            }
        }

        return listening
    }

    fun start(): CompletableFuture<Void> {
        return CompletableFuture.allOf(
            *listenAddrs.map { network.listen(it) }.toTypedArray()
        )
    }

    fun stop(): CompletableFuture<Void> {
        return CompletableFuture.allOf(
            network.close()
        )
    }

    fun addStreamVisitor(streamVis: ChannelVisitor<Stream>) {
        streamVisitors += streamVis
    }

    fun removeStreamVisitor(streamVis: ChannelVistor<Stream>) {
        streamVisitors -= streamVis
    }

    fun addProtocolHandler(protocolBinding: ProtocolBinding<Any> {
        protocolHandlers += protocolBinding
    }

    fun removeProtocolHandler(protoclBinding: ProtcolBinding<Any>) {
        protocolHandlers += protoclBinding
    }

    fun addConnectionHandler(handler: ConnectionHandle) {
        connectionHandlers += handler
    }

    fun removeConnectionhandler(handler: ConnectionHandler) {
        connectionHandlers -= handler
    }

    fun <TController> newXStream(xprotocols: List<String>, peerid: XID, vararg xaddr: XAddress): XStreamPromise<TController> {
        val retF = xoverlay.connect(peerid, *xaddr).thenApply{ newXStream<TController>(xprotocols, it) }
        return XStreamPromise(retF.thenCompose { it.stream }, retF.thenCompose { it.controller })
    }

    fun <TController> newXStream(xprotocols: List<String>, xconn: XConnection): XStreamPromise<TController> {
        val binding = @Suppress("UNCHECKED_CAST")protocolHandlers.find { it.protocolDescriptor.matcherAny(xprotocols)} as? ProtocolBinding<TController> ?: throw Exception("Protocol handler not found: $xprotocols")
        return xconn.muxerSession().createStream(listOf(binding.toInitiator(xprotocols)))
    }
}

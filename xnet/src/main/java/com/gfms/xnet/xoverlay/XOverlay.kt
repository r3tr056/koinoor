package com.gfms.xnet.xoverlay

import com.gfms.xnet.XConnection
import com.gfms.xnet.XConnectionHandler
import com.gfms.xnet.transports.XTransport
import com.gfms.xnet.transports.XTransportListener
import com.gfms.xnet.transports.XTransportManager
import com.gfms.xnet.xaddress.XAddress
import com.gfms.xnet.xaddress.XProtocol
import com.gfms.xnet.xpeer.XID
import java.util.concurrent.CompletableFuture

class XOverlay(val xtransports: List<XTransport>, val xconnHandler: XConnectionHandler) {
    
    // Overlay's transport manager
    private val transportManager: XTransportManager = XTransportManager()
    private val connections: List<XConnection> = mutableListOf()

    init {
        xtransports.forEach(XTransport::initialize)
    }

    fun listen(xaddr: XAddress): CompletableFuture<Unit> = getTransport(xaddr).listen(xaddr, createHookedConnHandler(xconnHandler))
    fun unlisten(xaddr: XAddress): CompletableFuture<Unit> = getTransport(xaddr).unlisten(xaddr)

    // Dials using an XAddress of the format `/xnet/<peerid>`
    fun dialXAddr(vararg xaddrs: XAddress): CompletableFuture<XConnection> {
        // get all the peerids from the address
        val peerIdSet = xaddrs.map {
            it.getStringComponent(XProtocol.XNET) ?: throw Exception("XAddress should contain /xnet/<peerId> component")
        }.toSet()
        if (peerIdSet.size != 1) throw Exception("All XAddresses should have the same peer ID")
        return dialXPeer(XID.fromb64(peerIdSet.first()), *xaddrs)
    }

    // Dials the peer id specified
    fun dialXPeer(id: XID, vararg xaddrs: XAddress): CompletableFuture<XConnection> {
        // if we already have a connection to the peer, short circuit
        connections.find { it.secureSession().remoteId == id }
            ?.apply { return CompletableFuture.completedFuture(this) }

        // check that some transport can dial at least one addr
        // dial parallel using all transports
        // when the first dial succeeds, cancel all pending dials and return  the connenction.
        // if not dial succeeds, we fail the dial, timeout and fail the `CompletableFuture`, and
        // cancel all pending dials
        val connectionFutures = xaddrs.mapNotNull { xaddr -> xtransports.firstOrNull { trans -> trans.canHandle(xaddr) }?.let { xaddr to it } }.map { it.second.dial(it.first, createHookedConnHandler(xconnHandler)) }

        return anyComplete(connectionFutures)
    }

    // Access the closer of the `xconn`
    fun disconnect(xconn: XConnection): CompletableFuture<Unit> = xconn.close()

    fun getTransport(xaddr: XAddress) = xtransports.firstOrNull { trans -> trans.canHandle(xaddr) } ?: throw Exception("No Transport to handle xaddr: $xaddr")

    // TODO : Modify the handler creation
    fun createHookedConnHandler(handler: XConnectionHandler) = XConnectionHandler.createBroadcast(
        listOf(
            handler,
            XConnectionHandler.create { xconn ->
                connections += xconn
                xconn.close().thenAccept { connections -= xconn }
            }
        )
    )

    fun close(): CompletableFuture<Unit> {
        val xtransClosed = xtransports.map(XTransport::close)
        val connectionsClosed = connections.values.map(XConnection::close)
        val everythingThatNeedsToClose = xtransClosed.union(connectionsClosed)

        return if (everythingThatNeedsToClose.isNotEmpty()) {
            CompletableFuture.allOf(*everythingThatNeedsToClose.toTypedArray()).thenApply {  }
        } else {
            CompletableFuture.completedFuture(Unit)
        }
    }

}

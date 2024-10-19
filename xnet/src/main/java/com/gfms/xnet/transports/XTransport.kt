package com.gfms.xnet.transports

import com.gfms.xnet.XConnection
import com.gfms.xnet.XConnectionHandler
import com.gfms.xnet.XStream
import com.gfms.xnet.xaddress.XAddress
import java.net.InetAddress
import java.time.Duration
import java.util.concurrent.CompletableFuture

// Transport represents an adapter to integrate a L2, L3 or L7 protocol into the XNet
// protocol collection, allowing incomming and outgoing connections to be established

typealias RemoteHost = Pair<InetAddress, String>

interface XTransport {

    var closed: Boolean
    val activeListeners: Int
    var connectTimeout: Duration

    val listeners: Map<XAddress, XTransportListener>
    val xstreams: List<XStream>

    abstract val activeConnections: Int

    abstract fun listenAddresses(): List<XAddress>

    fun addListener(addr: XAddress, listener: XTransportListener)

    // Verifies whether the transport is capable of handling the XAddress super type
    fun canHandle(addr: XAddress): Boolean
    // Performs any hardware preparation, warm-up, allocations, locks to activate the transport
    fun initialize()

    // Stops the transport entirely, closing down all ongoing connections, outbound
    // connections, listening endpoints without notice
    fun close(): CompletableFuture<Unit>

    // Makes the transport listen on the `addr` XAddress, the Future completes once
    // the endpoint is actually listening
    fun listen(addr: XAddress, listener: XTransportListener): CompletableFuture<Unit>

    // Makes the transport stop the listener on the `addr` XAddress, connections are not
    // disconnected, but a [ConnectionPause] packet is sent, once completely paused
    // and the listener detached the Future completes
    fun unlisten(addr: XAddress): CompletableFuture<Unit>

    // Tries to connect to an XAddress using the Transport, returns a Promise
    fun dial(addr: XAddress): CompletableFuture<XConnection>

    fun dial(first: XAddress, connectionHandler: XConnectionHandler): CompletableFuture<XConnection>
}

open class XTransportManager {}

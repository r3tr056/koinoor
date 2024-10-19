package com.gfms.xnet.xconnection

import com.gfms.xnet.transports.XTransport
import com.gfms.xnet.xaddress.XAddress
import java.util.concurrent.CompletableFuture

interface XConnection {
	// Returns the [xnet.muxing.XStreamMuxer.Session] which is capable of creating
	// a new XStream[s]
	fun muxerSession(): XStreamMuxer.Session
	// Returns a secure [xnet.security.SecureChannel.Session] session, with the protocol agreed upon at the core
	fun secureSession(): SecureChannel.Session
	
	// Return transport instance behind this XConnection
	fun transport(): XTransport

	// Returns local XAddress of this connection
	fun localXAddr(): XAddress
	// Just the XAddress of the other peer
	fun remoteXAddr(): XAddress

	val isInitiator: Boolean

	fun addHandler(name: String, handler: XStreamHandler<Any>)

	fun addHandlerBefore(baseName: String, name: String, handler: XStreamHandler<Any>)

	// Closes the Connection and returns a [CompletableFuture] which completes when the
	// XConnection closes
	fun close(): CompletableFuture<Unit>

}

fun interface XConnectionHandler {

	fun handleConnection(conn: XConnection)

	companion object {
		fun create(handler: (XConnection) -> Unit): XConnectionHandler {
			return object : XConnectionHandler {
				override fun handleConnection(conn: XConnection) {
					handler(conn)
				}
			}
		}

		fun createBroadcast(handlers: List<XConnectionHandler> = listOf()): Broadcast = BroadcastConenctionHandler().also { it += handlers }
	}

	interface Broadcast: XConnectionHandler, MutableList<XConnectionHandler>
}
package com.gfms.xnet

import java.util.concurrent.CompletableFuture

interface XStream {
	val isInitiator: Boolean
	val connection: XConnection

	// Returns the PeerId of the remote peer at the other end
	// of the stream
	fun remotePeerId(): XID

	// Get the protocol id the peers agreed upon
	fun getProtocol(): CompletableFuture<String>

	fun <TMessage> pushProtocolHandler(protocolHandler: ProtocolMessageHandler<TMessage>) {
		pushProtocolHandler(ProtocolMessageHandlerAdapter(this, protocolHandler))
	}

	fun <TMessage> pushHandlerBefore(protocolHandler: ProtocolMessageHandler<TMessage>) {

	}

	fun popHandler(protocolHandler: Any) {

	}

	fun writeAndFlush(msg:Any)

	fun reset() = close()

	// Equivalent of Reset
	// Closes the stream just from the local side
	fun close(): CompletableFuture<Unit>

	/**
	 * Sends a `RemoteWireClosed` packet and closes the local side of the XStream
	 * [closeWrite] is called when the local party `you` have completed
	 * writing to the stream
	 * The remote side is notified be a `RemoteWriterClosed` packet
	**/
	fun closeWrite(): CompletableFuture<Unit>
}

/**
 * The pair of [Futures] as a result of indication to a stream as a
 * promise
 * 
 * @property steam : Is completed when a stream instance is sucessfully created
 * @property controller : Is completed when the underlying client
 * protocol is initiated
**/
data class XStreamPromise<T>(
	val stream: CompletableFuture<XStream> = CompletableFuture(),
	val controller: CompletableFuture<T> = CompletableFuture()
)

fun interface XStreamHandler<TController> {
	fun handleStream(stream: XStream): CompletableFuture<TController>
}
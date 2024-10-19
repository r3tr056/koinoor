package com.gfms.xnet.endpoint.api

import java.util.concurrent.CompletableFuture

interface XChannel {
	// Indicates whether this peer is either the `_initiator_` or 
	// `_responder_` of the underlying channel impl
	val isInitiator: Boolean

	fun pushHandler(handler: ChannelHandler)

	// Top on the handler map
	fun addHandlerBefore(baseanme: String, name: String, handler: ChannelHandler)

	fun close()

	fun closeFuture(): CompletableFuture<Unit>
}
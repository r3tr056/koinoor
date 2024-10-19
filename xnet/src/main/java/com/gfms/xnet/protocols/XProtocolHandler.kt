abstract class XProtocolHandler<TController>(
	private val initiatorTrafficLimit: Long,
	private val responderTrafficLimit: Long
): XChannelHandler<TController> {

	override fun initChannel(ch: XChannel): CompletableFuture<TController> {
		val stream = ch as XStream
		// establish traffic limiter
		val inboundTrafficLimit = if (stream.isInitiator) responderTrafficLimit else initiatorTrafficLimit
		if (inboundTrafficLimit < Long.MAX_VALUE) {
			stream.pushHandler(InBoundTrafficLimitHandler())
		}
	}

	
}
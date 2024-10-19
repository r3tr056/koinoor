import io.libp2p.core.Connection
import io.libp2p.core.InternalErrorException
import io.libp2p.core.multiformats.Multiaddr
import io.libp2p.core.multiformats.Protocol
import io.libp2p.core.mux.StreamMuxer
import io.libp2p.core.security.SecureChannel
import io.libp2p.core.transport.Transport
import io.libp2p.etc.CONNECTION
import io.netty.channel.Channel
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetSocketAddress


class ConnectionOverXNet(
	ch: XChannel,
	private val transport: XTransport,
	initiator: Boolean
): XConnection, XNetChannelOverXNetWire(ch, initiator) {
	private lateinit var muxerSession: XStreamMuxer.Session
	private lateinit var secureSession: SecureChannel.Session

	init {
		ch.attr(CONNECTION).set(this)
	}

	fun setMuxerSession(ms: XStreamMuxer.Session) { muxerSession = ms}
	fun setSecureSession(ss: SecureChannel.Session) { secureSession = ss }

	fun muxerSession() = muxerSession
	fun secureSession() = secureSession
	fun transport() = transport

	fun localAddress(): XAddress = toXAddress() // set the local address
	fun remoteAddress(): XAddress = toXAddress() // return the remote address the Channel points to

	private fun toXAddress(addr: InetSocketAddress): XAddress {
		if (transport is XNetTransport)
			return transport.toXAddress(addr)
		else
			return toXAddressConventional(addr)
	}

	private fun toXAddressConventional(addr: InetSocketAddress): XAddress {
		val proto = when (addr.address) {
			is Inet4Address -> Protocol.IPV4
			is Inet6Address -> Protocol.IPV6
			else -> throw InternalErrorException("Unknown address type $addr")
		}
		return XAddress(
			listOf(
				proto to proto.addressToBytes(addr.address.hostAddress),
				Protocol.TCP to Protocol.TCP.addressToBytes(addr, port.toString())
			)
		)
	}
}
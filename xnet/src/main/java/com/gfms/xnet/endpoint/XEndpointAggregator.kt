package com.gfms.xnet.endpoint

import com.gfms.xnet.endpoint.api.XEndpoint
import com.gfms.xnet.endpoint.api.XEndpointListener
import com.gfms.xnet.ipv4.IPv4Address
import com.gfms.xnet.packet.XPacket
import com.gfms.xnet.xaddress.XAddress
import com.gfms.xnet.xpeer.XPeer
import java.util.*

class XEndpointAggregator(
    val udpEndpoint: UDPEndpoint?
): XEndpoint<XPeer>(), XEndpointListener {
    private var isOpen: Boolean = false

    // Sends a message to the peer. Currently it send over all available endpoints.
    // In future, this method should only send over selected transports
    override fun send(peer: XPeer, data: ByteArray) {
        peer.lastRequest = Date()
        if (!peer.address.isEmpty() && udpEndpoint != null) {
            udpEndpoint.send(peer, data)
        }
    }

    fun send(address: XAddress, data: ByteArray) {
        when (address) {
            is IPv4Address -> udpEndpoint?.send(address, data)
        }
    }

    override fun isOpen(): Boolean {
        return isOpen
    }

    override fun open() {
        udpEndpoint?.addListener(this)
        udpEndpoint?.open()
        isOpen = true
    }

    override fun close() {
        udpEndpoint?.removeListener(this)
        udpEndpoint?.close()
        isOpen = false
    }

    override fun onPacket(packet: XPacket) {
        notifyListeners(packet)
    }

    override fun onEstimatedLanChanged(address: IPv4Address) {
        setEstimatedLan(address)
    }
}
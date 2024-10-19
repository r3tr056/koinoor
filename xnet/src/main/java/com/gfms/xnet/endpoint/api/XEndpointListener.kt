package com.gfms.xnet.endpoint.api

import com.gfms.xnet.ipv4.IPv4Address
import com.gfms.xnet.packet.XPacket

/**
 * Handler for traffic coming through an XEndpoint
 */
interface XEndpointListener {
    /**
     * Callback for when data is received on the endpoint
     * @param packet: XPacket : The received packet
     */
    fun onPacket(packet: XPacket)
    /**
     * Callback for when the LAN address of the active network interface changes.
     * @param address: IPv4Address The local LAN address.
     */
    fun onEstimatedLanChanged(address: IPv4Address)
}
package com.gfms.xnet.overlay

import com.gfms.xnet.xpeer.XPeer
import com.gfms.xnet.discovery.XNetwork
import com.gfms.xnet.ipv4.IPv4Address
import com.gfms.xnet.endpoint.XEndpointAggregator
import com.gfms.xnet.endpoint.api.XEndpointListener

interface Overlay: XEndpointListener {
    val serviceId: String
    var selfPeer: XPeer
    var endpoint: XEndpointAggregator
    var network: XNetwork

    val selfEstimatedWan: IPv4Address
    val selfEstimatedLan: IPv4Address
    var maxPeers: Int

    private val globalTime: ULong
        get() = selfPeer.lamportTimestamp

    // Called to initialize the overlay network
    fun load() {
        endpoint.addListener(this)
    }
    // Called when this overlay needs to be shut down.
    fun unload() {
        endpoint.removeListener(this)
    }

    fun claimGlobalTime(): ULong {
        updateGlobalTime(globalTime + 1u)
        return globalTime
    }

    private fun updateGlobalTime(globalTime: ULong) {
        if (globalTime > this.globalTime) {
            selfPeer.updateClock(globalTime)
        }
    }

    // The introduction logic of this peer to get into the network
    fun bootstrap()
    // Puncture the NAT of and address
    // @param address : The address to walk to
    fun walkTo(address: IPv4Address)

    fun getNewIntro(fromPeer: XPeer? = null)

    fun getPeers(): List<XPeer>

    fun getWalkableAddresses(): List<IPv4Address>

    fun getPeerForIntro(exclude: XPeer? = null): XPeer?

    open class Factory<T: Overlay>(
        val overlayClass: Class<T>
    ) {
        open fun create(): T {
            return overlayClass.getConstructor().newInstance()
        }
    }
}
package com.gfms.xnet.endpoint.api

import com.gfms.xnet.ipv4.IPv4Address
import com.gfms.xnet.packet.XPacket

abstract class XEndpoint<A> {
    private val listeners = mutableListOf<XEndpointListener>()
    private var estimatedLan: IPv4Address? = null

    fun addListener(listener: XEndpointListener) {
        listeners.add(listener)
        if (estimatedLan != null) {
            listener.onEstimatedLanChanged(estimatedLan!!)
        }
    }

    fun removeListener(ls: XEndpointListener) {
        listeners.remove(ls)
    }

    protected fun notifyListeners(packet: XPacket) {
        for (listener in listeners) {
            try {
                listener.onPacket(packet)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    protected fun setEstimatedLan(address: IPv4Address) {
        // logger.info("Estimated LAN address: $address")
        estimatedLan = address
        for (listener in listeners) {
            listener.onEstimatedLanChanged(estimatedLan!!)
        }
    }

    abstract fun isOpen(): Boolean
    abstract fun send(peer: A, data: ByteArray)
    abstract fun open()
    abstract fun close()
}

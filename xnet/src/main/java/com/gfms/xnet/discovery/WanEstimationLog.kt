package com.gfms.xnet.discovery

import com.gfms.xnet.ipv4.IPv4Address
import com.gfms.xnet.packet.payloads.ConnectionType
import java.util.*

/**
 * The WAN estimation log keeps track of our public address that is reported by other peers.
 * The most recent values are used to determine our WAN address used in introduction requests.
 * Further behavioral NAT analysis is performed to detect symmetric NAT behavior.
 */
class WanEstimationLog {

    private val log = mutableListOf<WanLogItem>()

    // Add a new WanLogItem to the log if this WAN is not reported by this sender yet
    fun addItem(item: WanLogItem) {
        val existingItem = log.findLast {
            it.wan == item.wan && it.sender == item.sender
        }
        if (existingItem == null)
            log.add(item)
    }

    // Estimates our current WAN address using the majority from the last few log items.
    @Synchronized
    fun estimateWan(): IPv4Address? {
        val wans = log.takeLast(MAJORITY_INPUT_SIZE).map { it.wan }
        return majority(wans)
    }

    /**
     * Estimates our NAT type based on NAT behaviour data in the fashion
     * - public : Our LAN address = WAN address, we are not behind a NAT
     * - symmetric : Our WAN address is changed frequently, symmetric NAT behaviour
     * - unknown : Most of the WAN reports are matching, NAT preforms endpoint
     * independent mapping
     */
    @Synchronized
    fun estimateConnectionType(): ConnectionType {
        val wans = log.map { it.wan }.distinct()
        val wan = estimateWan()
        val lan = log.lastOrNull()?.lan
        val symmeticNATProbab = (wans.size - 1) / log.size.toFloat()

        return when {
            wan != null && wan == lan -> ConnectionType.PUBLIC
            log.size > 1 && symmeticNATProbab > 0.1 -> ConnectionType.SYMMETRIC_NAT
            else -> ConnectionType.UNKNOWN
        }
    }

    @Synchronized
    fun clear() {
        log.clear()
    }

    @Synchronized
    fun getLog(): List<WanLogItem> {
        return log
    }

    private fun <T> majority(items: List<T>): T? {
        val counts = mutableMapOf<T, Int>()
        for (item in items)
            counts[item] = (counts[item] ?: 0) + 1
        return counts.maxByOrNull { it.value }?.key
    }

    data class WanLogItem(
        // Time stamp of when the WAN address report was received
        val timestamp: Date,
        val sender: IPv4Address,
        val lan: IPv4Address,
        val wan: IPv4Address
    )

    companion object {
        private const val MAJORITY_INPUT_SIZE = 3
    }
}
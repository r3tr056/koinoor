package com.gfms.xnet.discovery

import java.lang.IllegalStateException

class PeriodicSimilarityDiscover(
    private val overlay: DiscoveryCommunity
) : XDiscover {
    override fun takeStep() {
        // Obtain a random peer from the network instance of the community
        val peer = overlay.network.getRandomPeer()
        // Send a similarity request
        if (peer != null) {
            overlay.sendSimilarityRequest(peer)
        }
    }

    class Factory : XDiscover.Factory<PeriodicSimilarityDiscover>() {
        override fun create(): PeriodicSimilarityDiscover {
            val overlay = getOverlay() as? DiscoveryCommunity
                ?: throw IllegalStateException("PeriodicSimilarity is only compatible with " + "XDiscoverer")
            return PeriodicSimilarityDiscover(overlay)
        }
    }
}
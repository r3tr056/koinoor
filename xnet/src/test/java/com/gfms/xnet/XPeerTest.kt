package com.gfms.xnet

import com.gfms.xnet.crypto.XKey
import com.gfms.xnet.xpeer.XPeer
import org.junit.Test
import org.junit.Assert

class XPeerTest {
    @Test
    fun updateClock() {
        val key = spyk<XKey>()
        val peer = XPeer(key)

        Assert.assertEquals(0uL, peer.lamportTimestamp)
        peer.updateClock(1000uL)
        Assert.assertEquals(1000uL, peer.lamportTimestamp)
        peer.updateClock(1uL)
        Assert.assertEquals(1000uL, peer.lamportTimestamp)
    }

    @Test
    fun getAveragePing() {
        val peer = XPeer(mockk())

        peer.addPing(1.0)
        peer.addPing(3.0)
        Assert.assertEquals(2.0, peer.getAveragePing(), 0.01)
    }
}
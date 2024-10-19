package com.gfms.xnet

import com.gfms.xnet.OverlayConfig
import com.gfms.xnet.crypto.SodiumCryptoProvider
import com.gfms.xnet.endpoint.UDPEndpoint
import com.gfms.xnet.endpoint.XEndpointAggregator
import com.gfms.xnet.overlay.Overlay
import com.gfms.xnet.xpeer.XPeer
import org.junit.Assert
import org.junit.Test
import java.net.InetAddress

class XNetTest {

    @Test
    fun startAndStop() {
        val selfKey = SodiumCryptoProvider.generateKey()
        val selfPeer = XPeer(key=selfKey)
        val udpEndpoint = UDPEndpoint(7868, InetAddress.getByName("0.0.0.0"))
        val endpoint = XEndpointAggregator(udpEndpoint=udpEndpoint)
        val factory = Overlay.Factory(overlayClass=TestCommunity::class.java)
        val overlayConfig = OverlayConfig(factory = factory, walkers=listOf())
        val xconfig = XConfig(overlayConfigs = listOf(overlayConfig), walkerInterval = 5.0)
        val xnet = XNet(endpoint = endpoint, configuration = xconfig, selfPeer = selfPeer)

        // Boot XNet
        xnet.boot()
        // Assert that endpoint is open
        Assert.assertTrue(endpoint.isOpen())
        // Stop XNet
        xnet.halt()
    }
}
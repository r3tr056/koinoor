package com.gfms.xnet.discovery

import com.gfms.xnet.overlay.Overlay
import com.gfms.xnet.xpeer.XPeer

interface PingOverlay : Overlay {
    // sends a ping message to the specified peer
    fun sendPing(peer: XPeer)
}
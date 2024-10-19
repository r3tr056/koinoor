package com.gfms.xnet.overlay

import com.gfms.xnet.discovery.XDiscover


data class OverlayConfig<T: Overlay> (
    val factory: Overlay.Factory<T>,
    val walkers: List<XDiscover.Factory<*>>,
    val maxPeers: Int = 30
)
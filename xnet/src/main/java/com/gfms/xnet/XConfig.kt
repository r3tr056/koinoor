package com.gfms.xnet


import com.gfms.xnet.overlay.OverlayConfig

data class XConfig(
    val address: String = "0.0.0.0",
    val port: Int = 8998,
    val walkerInterval: Double = 5.0,
    val overlayConfigs: List<OverlayConfig<*>>
)

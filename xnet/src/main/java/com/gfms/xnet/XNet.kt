package com.gfms.xnet

import com.gfms.xnet.discovery.XDiscover
import com.gfms.xnet.discovery.XNetwork
import com.gfms.xnet.overlay.Overlay
import com.gfms.xnet.endpoint.XEndpointAggregator
import com.gfms.xnet.xpeer.XPeer
import kotlinx.coroutines.*
import java.lang.IllegalStateException
import kotlin.math.roundToLong

class XNet(
    private val endpoint: XEndpointAggregator,
    private val configuration: XConfig,
    val selfPeer: XPeer,
    val network: XNetwork = XNetwork()
) {
    private val overlayLock = Object()
    // Map of the actual overlay class to the Overlay instance
    val overlays = mutableMapOf<Class<out Overlay>, Overlay>()
    private val discoveryStrategies = mutableListOf<XDiscover>()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var loopingCallJob: Job? = null

    var isRunning = false

    inline fun <reified T: Overlay> getOverlay(): T? {
        return overlays[T::class.java] as? T
    }

    fun boot() {
        if (isRunning) throw IllegalStateException("XNet is already running on this peer")
        isRunning = true
        endpoint.open()

        // Init overlays and discovery strategies
        for (overlayConfig in configuration.overlayConfigs) {
            val overlayClass = overlayConfig.factory.overlayClass
            if (overlays[overlayClass] != null) {
                throw IllegalStateException("Overlay $overlayClass already exists")
            }
            val overlay = overlayConfig.factory.create()
            overlay.selfPeer = selfPeer
            overlay.endpoint = endpoint
            overlay.network = network
            overlay.maxPeers = overlayConfig.maxPeers
            overlay.load()

            overlays[overlayClass] = overlay

            for (strategyFactory in overlayConfig.walkers) {
                val strategy = strategyFactory.setOverlay(overlay)
                    .create()
                strategy.load()
                discoveryStrategies.add(strategy)
            }
        }
        startLoopingCall()
    }

    private fun onTick() {
        if (endpoint.isOpen()) {
            synchronized(overlayLock) {
                for (strategy in discoveryStrategies) {
                    try {
                        strategy.takeStep()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    private fun startLoopingCall() {
        val interval = (configuration.walkerInterval * 1000).roundToLong()
        loopingCallJob = scope.launch {
            while (true) {
                onTick()
                delay(interval)
            }
        }
    }

    fun halt() {
        if (isRunning) throw IllegalStateException("XNet is not running")
        synchronized(overlayLock) {
            loopingCallJob?.cancel()

            for ((_, overlay) in overlays) {
                overlay.unload()
            }
            overlays.clear()
            for (strategy in discoveryStrategies) {
                strategy.stopDiscovery()
            }
            discoveryStrategies.clear()
            endpoint.close()
        }
        isRunning = false
    }
}
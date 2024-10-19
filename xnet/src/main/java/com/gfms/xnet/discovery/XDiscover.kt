package com.gfms.xnet.discovery

import com.gfms.xnet.overlay.Overlay
import java.lang.IllegalStateException
import java.util.concurrent.CompletableFuture

interface XDiscover {

    fun startDiscovery() {}
    // On every tick in interval defined by `walkerInterval`
    fun takeStep()
    // When the xnet service is stopped
    fun stopDiscovery() {}

    abstract class Factory<T: XDiscover> {
        private var overlay: Overlay? = null

        protected fun getOverlay(): Overlay {
            return overlay?: throw IllegalStateException("Overlay is not set")
        }
        // Set overlay must be called first before producing a DiscoveryService
        fun setOverlay(overlay: Overlay): Factory<T> {
            this.overlay = overlay
            return this
        }

        abstract fun create(): T
    }
}
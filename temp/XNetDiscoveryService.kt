package com.gfms.xnet_android

import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import com.gfms.xnet.discovery.DiscoveryStrategy
import com.gfms.xnet.ipv4.IPv4Address
import com.gfms.xnet.overlay.Overlay
import com.gfms.xnet.packet.XPacket
import kotlin.random.Random

class XNetDiscoveryService(
    private val nsdManager: NsdManager,
    private val overlay: Overlay
): DiscoveryStrategy {
    private var serviceName: String? = null

    private val registrationListener = object : NsdManager.RegistrationListener {
        override fun onServiceRegistered(serviceInfo: NsdServiceInfo) {
            // Save the service name. Android may have changed it in order to
            // resolve a conflict, so update the name you initially request
            // with the name Android actually used
            // logger call
            serviceName = serviceInfo.serviceName
        }

        override fun onRegistrationFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
            // logger call
        }

        override fun onServiceUnregistered(serviceInfo: NsdServiceInfo?) {
            // logger call
        }

        override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
            // logger call
        }
    }

    private val discoveryListener = object : NsdManager.DiscoveryListener {
        override fun onDiscoveryStarted(serviceType: String?) {
            // logger call
        }

        override fun onServiceFound(serviceInfo: NsdServiceInfo) {
            // logger call
            if (serviceInfo.serviceType == SERVICE_TYPE) {
                // This is an XNet service
                if (serviceInfo.serviceName == serviceName) {
                    // logger call
                }

                val serviceId = getServiceId(serviceInfo.serviceName)
                // logger call
                if (serviceId == overlay.serviceId) {
                    nsdManager.resolveService(serviceInfo, createResolveListener())
                }
            }
        }

        override fun onServiceLost(serviceInfo: NsdServiceInfo?) {
            // logger call
        }

        override fun onDiscoveryStopped(serviceType: String?) {
            // logger call
        }

        override fun onStartDiscoveryFailed(serviceType: String?, errorCode: Int) {
            // logger call
            nsdManager.stopServiceDiscovery(this)
        }

        override fun onStopDiscoveryFailed(serviceType: String?, errorCode: Int) {
            // logger call
            nsdManager.stopServiceDiscovery(this)
        }
    }

    private fun createResolveListener(): NsdManager.ResolveListener {
        return object : NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                // logger call
            }

            override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                // logger call
                val peer = overlay.selfPeer
                val address = IPv4Address(serviceInfo.host.hostAddress, serviceInfo.port)

                if (overlay.selfEstimatedLan != address) {
                    // logger call
                    overlay.network.discoverAddress(peer, address, overlay.serviceId)
                } else {
                    // logger call
                }
            }
        }
    }

    private fun registerService(port: Int, serviceName: String) {
        val serviceInfo = NsdServiceInfo()
        // The name is subject to change based on conflicts
        // with other services advertised on the same network
        serviceInfo.serviceName = serviceName
        serviceInfo.serviceType = SERVICE_TYPE
        serviceInfo.port = port

        // logger call
        nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
    }

    private fun discoverServices() {
        nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
    }

    private fun unregisterService() {
        nsdManager.unregisterService(registrationListener)
    }

    private fun stopServiceDiscovery() {
        nsdManager.stopServiceDiscovery(discoveryListener)
    }

    override fun load() {
        // logger
        val endpoint = overlay.endpoint.udpEndpoint
        if (endpoint != null) {
            val socketPort = endpoint.getSocketPort()
            val serviceName = overlay.serviceId + "_" + Random.nextInt(10000)
            // logger call
            registerService(socketPort, serviceName)

            discoverServices()
        }
    }

    override fun takeStep() {
        val addresses = overlay.getWalkableAddresses()
        if (addresses.isNotEmpty()) {
            val address = addresses.random()
            overlay.walkTo(address)
        }
    }

    override fun unload() {
        // logger
        unregisterService()
        stopServiceDiscovery()
    }

    // The service ID are the first 40 characters of the service name. Returns null if
    // the service name is invalid
    private fun getServiceId(serviceName: String): String? {
        // Service ID string length is two times its size in bytes as its hexadecimal
        // encoded
        val serviceIdLength = XPacket.SERVICE_ID_SIZE * 2
        if (serviceName.length < serviceIdLength) return null

        val serviceId = serviceName.substring(0, serviceIdLength)
        for (char in serviceId) {
            if (!char.isDigit() && char !in 'a'..'f') {
                return null
            }
        }
        return serviceId
    }

    companion object {
        private const val SERVICE_TYPE = "_xnet._udp."
    }

    class Factory(
        private val nsdManager: NsdManager
    ): DiscoveryStrategy.Factory<XNetDiscoveryService>() {
        override fun create(): XNetDiscoveryService {
            return XNetDiscoveryService(nsdManager, getOverlay())
        }
    }
}
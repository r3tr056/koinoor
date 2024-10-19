package com.gfms.xnet_android.discovery

import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import com.gfms.xnet.discovery.XDiscover
import com.gfms.xnet.ipv4.IPv4Address
import com.gfms.xnet.overlay.Overlay
import com.gfms.xnet.packet.XPacket
import kotlin.random.Random

class NSDPeerDiscovery(
    private val nsdManager: NsdManager,
    private val overlay: Overlay
): XDiscover {
    private var serviceName: String? = null

    private val registrationListener = object : NsdManager.RegistrationListener {
        override fun onServiceRegistered(serviceInfo: NsdServiceInfo) {
            // logger call => Service Name Registered
            serviceName = serviceInfo.serviceName
        }

        override fun onRegistrationFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
            // logger call -> Service Registration failed
        }

        override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) {
            // logger call => service unregistered
        }

        override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
            // logger call
        }
    }

    private val discoveryListener = object : NsdManager.DiscoveryListener {

        override fun onDiscoveryStarted(serviceType: String) {
            // logger call
        }

        override fun onServiceFound(serviceInfo: NsdServiceInfo) {
            // logger call
            if (serviceInfo.serviceType == SERVICE_TYPE) {
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
            override fun onResolveFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
                // logger error call
            }

            override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                // logger info
                val peer = overlay.selfPeer
                val address = IPv4Address(serviceInfo.host.hostAddress!!, serviceInfo.port)
                if (overlay.selfEstimatedLan != address) {
                    // logger call
                    overlay.network.discoverAddress(peer, address, overlay.serviceId)
                } else {
                    // logger call => Resolved self IP address
                }
            }
        }
    }

    private fun registerService(port: Int, serviceName: String) {
        val serviceInfo = NsdServiceInfo()
        serviceInfo.serviceName = serviceName
        serviceInfo.serviceType = SERVICE_TYPE
        serviceInfo.port = port

        // logger - >Registering service info $serviceInfo

        nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
    }

    private fun unregisterService() {
        nsdManager.unregisterService(registrationListener)
    }

    override fun startDiscovery() {
        // logger call -> NetworkServiceDiscovery load
        val endpoint = overlay.endpoint.udpEndpoint
        if (endpoint != null) {
            val socketPort = endpoint.getSocketPort()
            val serviceName = overlay.serviceId + "" + Random.nextInt(10000)
            // logger call -> Registering service $serviceName on port $socketPort
            registerService(socketPort, serviceName)
            nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
        }
    }

    override fun takeStep() {
        val addresses = overlay.getWalkableAddresses()
        if (addresses.isNotEmpty()) {
            val address = addresses.random()
            overlay.walkTo(address)
        }
    }

    override fun stopDiscovery() {
        // logger call -> Network discovery stopping
        nsdManager.unregisterService(registrationListener)
        nsdManager.stopServiceDiscovery(discoveryListener)
    }

    // The serviceId are the first 40 chars of the service name. Retruns null if
    // the service name is invalid
    private fun getServiceId(serviceName: String): String? {
        if (serviceName.length < XPacket.SERVICE_ID_SIZE) return null
        val serviceId = serviceName.substring(0, XPacket.SERVICE_ID_SIZE)
        for (char in serviceId) {
            if (!char.isDigit() && char !in 'a'..'f') {
                return null
            }
        }
        return serviceId
    }

    companion object {
        private const val SERVICE_TYPE = "_xnet0._udp"
    }

    class Factory(private val nsdManager: NsdManager): XDiscover.Factory<NSDPeerDiscovery>() {
        override fun create(): NSDPeerDiscovery {
            return NSDPeerDiscovery(nsdManager, getOverlay())
        }
    }
}
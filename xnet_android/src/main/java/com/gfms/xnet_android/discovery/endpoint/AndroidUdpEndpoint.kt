package com.gfms.xnet_android.discovery.endpoint

import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import com.gfms.xnet.endpoint.UDPEndpoint
import com.gfms.xnet.ipv4.IPv4Address
import java.net.Inet4Address
import java.net.InetAddress

class AndroidUdpEndpoint(
    port: Int,
    ip: InetAddress,
    private val connectivityManager: ConnectivityManager
): UDPEndpoint(port, ip) {

    private val defaultNetworkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onLinkPropertiesChanged(network: Network, linkProperties: LinkProperties) {
            super.onLinkPropertiesChanged(network, linkProperties)
            // logger call -> onLinkPropsChanged + linkProps.linkAddress
            for (linkAddress in linkProperties.linkAddresses) {
                if (linkAddress.address is Inet4Address && !linkAddress.address.isLoopbackAddress) {
                    val estimatedAddress = IPv4Address(linkAddress.address.hostAddress!!, getSocketPort())
                    setEstimatedLan(estimatedAddress)
                }
            }
        }
    }

    override fun startLanEstimation() {
        connectivityManager.registerDefaultNetworkCallback(defaultNetworkCallback)
    }

    override fun stopLanEstimation() {
        connectivityManager.unregisterNetworkCallback(defaultNetworkCallback)
    }
}
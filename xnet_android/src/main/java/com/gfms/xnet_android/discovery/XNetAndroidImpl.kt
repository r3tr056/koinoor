package com.gfms.xnet_android.discovery

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import androidx.core.content.getSystemService
import com.gfms.xnet.XConfig
import com.gfms.xnet.XNet
import com.gfms.xnet.crypto.XPrivateKey
import com.gfms.xnet.crypto.defaultCryptoProvider
import com.gfms.xnet.discovery.XNetwork
import com.gfms.xnet.endpoint.XEndpointAggregator
import com.gfms.xnet.xpeer.XPeer
import com.gfms.xnet_android.discovery.crypto.AndroidCryptoProvider
import com.gfms.xnet_android.discovery.endpoint.AndroidUdpEndpoint
import com.gfms.xnet_android.discovery.service.XNetAndroidService
import java.net.InetAddress
import kotlin.IllegalStateException

object XNetAndroidImpl {
    private var xnet: XNet? = null
    internal var serviceClass: Class<out XNetAndroidService>? = null

    fun getInstance(): XNet {
        return xnet ?: throw IllegalStateException("XNET is not initialized")
    }

    class Factory(private val application: Application) {
        private var privKey: XPrivateKey? = null

        // For small attestations
        private var identityPrivKeySmall: XAttestationPrivateKey? = null

        // For Big attestations
        private var identityPrivKeyBig: XAttestationPrivateKey? = null

        // For Huge and complicated attestations
        private var identityPrivKeyHuge: XAttestationPrivateKey? = null

        private var config: XConfig? = null

        private var serviceClass: Class<out XNetAndroidService> = XNetAndroidService::class.java
            set(sc: Class<out XNetAndroidService>) { field = sc }

        fun init(): XNet {
            val xnet = create()
            if (!xnet.isRunning) {
                xnet.boot()
                startAndroidService(application)
            }
            XNetAndroidImpl.xnet = xnet
            XNetAndroidImpl.serviceClass = serviceClass

            defaultCryptoProvider = AndroidCryptoProvider

            return xnet
        }

        private fun create(): XNet {
            // Private key of self
            val privKey = privKey ?: throw IllegalStateException("Private key is not set")
            // Network config
            val config = config ?: throw java.lang.IllegalStateException("Configuration is not set")
            // Android connectivity manager
            val connectivityManager = application.getSystemService<ConnectivityManager>() ?: throw IllegalStateException("ConnectivityManager not found")
            // UDP endpoint on Android
            val udpEndpoint = AndroidUdpEndpoint(7879, InetAddress.getByName("0.0.0.0"), connectivityManager)
            // Self Peer Instance
            val selfPeer = XPeer(privKey,
                identityPrivateKeySmall = identityPrivKeySmall,
                identityPrivateKeyBig = identityPrivKeyBig,
                identityPrivateKeyHuge = identityPrivKeyHuge
            )
            // XNetwork instance
            val network = XNetwork()
            // Endpoint Aggregator
            val endpointAggregator = XEndpointAggregator(udpEndpoint = udpEndpoint)
            // XNet instance
            return XNet(endpointAggregator, config, selfPeer, network)
        }

        private fun startAndroidService(context: Context) {
            val serviceIntent = Intent(context, serviceClass)
            context.startForegroundService(serviceIntent)
        }
    }
}

class XAttestationPrivateKey {

}

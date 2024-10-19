package com.gfms.xnet_android

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
import com.gfms.xnet.transport.XEndpointAggregator
import com.gfms.xnet.utils.EncodingUtils
import com.gfms.xnet.xpeer.XPeer
import java.net.InetAddress
import kotlin.IllegalStateException


object XNetAndroid {
    private var xnet: XNet? = null
    internal var serviceClass: Class<out XNetService>? = null

    fun getInstance(): XNet {
        return xnet ?: throw IllegalStateException("XNet interface is not initialized")
    }

    class Factory(private val application: Application) {
        private var privateKey: XPrivateKey? = null
        private var identityPrivateKeySmall: BonehPrivateKey? = null
        private var identityPrivateKeyBig: BonehPrivateKey? = null
        private var identityPrivateKeyHuge: BonehPrivateKey? = null

        private var config: XConfig? = null
        private var serviceClass: Class<out XNetService> = XNetService::class.java

        fun setPrivateKey(key: XPrivateKey): Factory {
            this.privateKey = key
            return this
        }

        fun setIdentityKeySmall(key: BonehPrivateKey): Factory {
            this.identityPrivateKeySmall = key
            return this
        }

        fun setIdentityKeyBig(key: BonehPrivateKey): Factory {
            this.identityPrivateKeyBig = key
            return this
        }

        fun setIdentityKeyHuge(key: BonehPrivateKey): Factory {
            this.identityPrivateKeyHuge = key
            return this
        }

        fun setConfig(config: XConfig): Factory {
            this.config = config
            return this
        }

        fun setServiceClass(serviceClass: Class<out XNetService>): Factory {
            this.serviceClass = serviceClass
            return this
        }

        fun init(): XNet {
            val xnet: XNet = create()
            if (!xnet.isRunning) {
                xnet.boot()
                startAndroidService(application)
            }
            XNetAndroid.xnet = xnet
            XNetAndroid.serviceClass = serviceClass
            defaultCryptoProvider = AndroidCryptoProvider()
            var defaultEncodingUtils = EncodingUtils
            return xnet
        }

        private fun create(): XNet {
            val privKey = privateKey ?: throw IllegalStateException("Private key is not set")
            val config = config ?: throw java.lang.IllegalStateException("Configuration is not set")
            val connectivityManager = application.getSystemService<ConnectivityManager>() ?: throw java.lang.IllegalStateException("ConnectivityManager not found")
            val udpEndpoint = AndroidUDPEndpoint(8090, InetAddress.getByName("0.0.0.0"), connectivityManager)
            val selfPeer = XPeer(
                privateKey!!,
                identityPrivateKeySmall = this.identityPrivateKeySmall,
                identityPrivateKeyBig = this.identityPrivateKeyBig,
                identityPrivateKeyHuge = this.identityPrivateKeyHuge
            )
            val network = XNetwork()
            val endpoint = XEndpointAggregator(udpEndpoint)

            return XNet(endpoint = endpoint, configuration = config, selfPeer=selfPeer, network=network)
        }

        private fun startAndroidService(context: Context) {
            val serviceIntent = Intent(context, serviceClass)
            context.startForegroundService(serviceIntent)
        }
    }
}

class BonehPrivateKey {
}

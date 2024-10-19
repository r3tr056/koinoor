package com.gfms.xnet_android.discovery.nearservice

import android.content.Context
import android.os.Looper
import com.gfms.xnet.xhost.XHost
import com.gfms.xnet.xpeer.XPeer

interface Near {

    fun send(bytes: ByteArray, peer: XHost)
    fun startReceiving()
    fun stopReceiving(abortCurrentTransfers: Boolean)
    val xpeers: Set<XPeer>
    val isReceiving: Boolean

    class NearDiscoveryFactory {
        private lateinit var mContext: Context
        private lateinit var mListener: NearListener
        private lateinit var mListenerLoop: Looper
        private lateinit var mxPeers: Set<XPeer>
        private var mPort: Int = TcpServiceService.SERVER_PORT

        fun setContext(context: Context): NearDiscoveryFactory {
            mContext = context
            return this
        }

        fun setListener(listener: NearListener, listenerloop: Looper): NearDiscoveryFactory {
            mListener = listener
            mListenerLoop = listenerloop
            return this
        }

        fun fromDiscovery(discovery: NearDiscovert): NearDiscoveryFactory {
            mxPeers = discovery.allAvailableXPeers
            return this
        }

        fun forPeers(xpeers: Set<XPeer>): NearDiscoveryFactory {
            mxPeers = xpeers
            return this
        }

        fun setPort(port: Int): NearDiscoveryFactory {
            mPort = port
            return this
        }
    }
    interface NearListener {
        fun onReceive(bytes: ByteArray, sender: XPeer)
        fun onSendComplete(jobId: Long)
        fun onSendFailure(e: Throwable?, jobId: Long)
        fun onStartListenFailure(e: Throwable?)
    }
}
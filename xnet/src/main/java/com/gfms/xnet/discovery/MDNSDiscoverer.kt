package com.gfms.xnet.discovery

/**
import android.telephony.mbms.ServiceInfo
import com.gfms.xnet.xpeer.XPeer
import java.net.InetAddress
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.ForkJoinPool

class MDNSDiscoverer(
    private val selfPeer: XPeer,
    private val serviceTag: String = ServiceTagLocal,
    private val queryInterval: Int = QueryInterval,
    val address: InetAddress? = null
): XDiscover {
    private val localHost = InetAddress.getLocalHost()
    private val mDNS = JmDNS.create(address ?: localHost)
    private val newPeerFoundListeners: MutableCollection<PeerListner> = CopyOnWriteArrayList()
    private val executor by lazy { ForkJoinPool(1) }

    override fun startDiscovery():CompletableFuture<Void> {
        return CompletableFuture.runAsync(
            Runnable {
                mDNS.start()

                mDNS.registerService(
                    xnetDiscoveryInfo()
                )
                mDNS.addAnswerListener(
                    serviceTag,
                    queryInterval,
                    Listener(this)
                )
            },
            executor
        )
    }

    override fun stopDiscovery(): CompletableFuture<Void> {
        return CompletableFuture.runAsync(
            Runnable { mDNS.stop() }, executor
        )
    }

    internal fun peerFound(peer: XPeer) {
        newPeerFoundListeners.forEach { it(peer) }
    }

    private fun selfDiscoveryInfo(): ServiceInfo {
        return ServiceInfo.create(
            serviceTag,
            selfPeer.peerId.toBase58(),
            listenPort(),
            host.peerId.toBase58(),
            ip4Addresses(),
            ip6Addresses()
        )
    }

    private fun listenPort(): Int {
        val address = selfPeer
    }
}

 **/
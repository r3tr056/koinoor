package com.gfms.xnet

import com.gfms.xnet.crypto.XPrivateKey
import com.gfms.xnet.discovery.WanEstimationLog
import com.gfms.xnet.discovery.XNetwork
import com.gfms.xnet.ipv4.IPv4Address
import com.gfms.xnet.overlay.Overlay
import com.gfms.xnet.packet.XPacket
import com.gfms.xnet.packet.payloads.*
import com.gfms.xnet.serialization.Serializable
import com.gfms.xnet.xaddress.XAddress
import com.gfms.xnet.endpoint.XEndpointAggregator
import com.gfms.xnet.utils.addressIsLAN
import com.gfms.xnet.utils.hexToBytes
import com.gfms.xnet.xpeer.XPeer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import java.lang.Exception
import java.lang.IllegalArgumentException
import java.util.*
import kotlin.random.Random

abstract class XCommunity: Overlay {

    companion object {
        val DEFAULT_ADDRESSES: List<IPv4Address> = listOf(
            // TODO : change this
            IPv4Address("131.180.27.161", 6247)
        )
        private const val BOOTSTRAP_TIMEOUT_MS = 5000
        const val DEFAULT_MAX_PEERS = 30
        const val PREFIX_XNET: Byte = (0xf9).toByte()
        const val VERSION: Byte = 1.toByte()
    }

    private val prefix: ByteArray
        get() = ByteArray(0) + PREFIX_XNET + VERSION + serviceId.hexToBytes()

    override val selfEstimatedWan: IPv4Address
        get() {
            return network.wanLog.estimateWan() ?: IPv4Address.EMPTY
        }

    override var selfEstimatedLan: IPv4Address = IPv4Address.EMPTY
    private var lastBootstrap: Date? = null
    val messageHandlers = mutableMapOf<Int, (XPacket) -> Unit>()
    override lateinit var selfPeer: XPeer
    override lateinit var endpoint: XEndpointAggregator
    override lateinit var network: XNetwork
    override var maxPeers: Int = 20

    private lateinit var job: Job
    private lateinit var scope: CoroutineScope

    init {
        messageHandlers[MessageId.PUNCTURE_REQUEST] = ::onPunctureRequestPacket
        messageHandlers[MessageId.PUNCTURE] = ::onPuncturePacket
        messageHandlers[MessageId.INTRODUCTION_REQUEST] = ::onIntroRequestPacket
        messageHandlers[MessageId.INTRODUCTION_RESPONSE] = ::onIntroResponsePacket
    }

    override fun load() {
        super.load()
        require(serviceId.length == 2 * XPacket.SERVICE_ID_SIZE) {
            "Service ID must be 20 bytes, was $serviceId"
        }
        network.registerServiceProvider(serviceId, this)
        network.blocklistMids.add(selfPeer.mid)
        network.blocklist.addAll(DEFAULT_ADDRESSES)

        job = SupervisorJob()
        scope = CoroutineScope(Dispatchers.Main + job)
    }

    override fun unload() {
        super.unload()
        job.cancel()
    }

    override fun bootstrap() {
        if (endpoint.udpEndpoint == null) return
        if (Date().time - (lastBootstrap?.time ?: 0L) < BOOTSTRAP_TIMEOUT_MS) return
        lastBootstrap = Date()

        for (socketAddr in DEFAULT_ADDRESSES) {
            walkTo(socketAddr)
        }
    }

    override fun walkTo(address: IPv4Address) {
        val packet = createIntroRequest(address)
        send(address, packet)
    }

    override fun getNewIntro(fromPeer: XPeer?) {
        var fromAddr = fromPeer?.address
        // Resilience
        if (fromAddr == null) {
            val avaiable = getPeers()
            fromAddr = if (avaiable.isNotEmpty()) {
                // With a small chance, try to remedy any disconnected network connections
                if (Random.nextFloat() < 0.5f && endpoint.udpEndpoint != null) {
                    DEFAULT_ADDRESSES.random()
                } else {
                    avaiable.random().address
                }
            } else {
                bootstrap()
                return
            }
        }
        val packet = createIntroRequest(fromAddr)
        send(fromAddr, packet)
    }

    override fun getPeerForIntro(exclude: XPeer?): XPeer? {
        val available = getPeers() - exclude
        return if (available.isNotEmpty()) {
            available.random()
        } else {
            null
        }
    }

    override fun getWalkableAddresses(): List<IPv4Address> {
        return network.getWalkableAddresses(serviceId = serviceId)
    }

    override fun getPeers(): List<XPeer> {
        return network.getPeersForService(serviceId)
    }

    override fun onPacket(packet: XPacket) {
        val srcAddr = packet.source
        val data = packet.data
        val probablePeer = network.getVerifiedByAddress(srcAddr)
        if (probablePeer != null) {
            probablePeer.lastResp = Date()
        }
        val packetPrefix = data.copyOfRange(0, prefix.size)
        if (!packetPrefix.contentEquals(prefix))
            return
        val msgId = data[prefix.size].toUByte().toInt()
        val handler = messageHandlers[msgId]

        if (handler != null) {
            try {
                handler(packet)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            // Logger ("Received unknown message")
        }
    }

    override fun onEstimatedLanChanged(address: IPv4Address) {
        if (selfEstimatedLan != address) {
            selfEstimatedLan = address
            network.wanLog.clear()
        }
    }

    private fun createIntroRequest(
        socketAddress: IPv4Address,
        extraBytes: ByteArray = byteArrayOf()
    ): ByteArray {
        val globalTime = claimGlobalTime()
        val payload = XIntroRequestPayload(
            socketAddress,
            selfEstimatedLan,
            selfEstimatedWan,
            true,
            network.wanLog.estimateConnectionType(),
            (globalTime % UShort.MAX_VALUE).toInt(),
            extraBytes
        )
        // Logger
        return serializePacket(MessageId.INTRODUCTION_REQUEST, payload)
    }

    private fun createIntroResponse(
        requester: XPeer,
        idenitifier: Int,
        introduction: XPeer? = null,
        prefix: ByteArray = this.prefix,
        extraBytes: ByteArray = byteArrayOf()
    ): ByteArray {
        val intro = introduction ?: getPeerForIntro(exclude = requester)
        val introLan = intro?.lanAddress ?: IPv4Address.EMPTY
        val introWan = intro?.wanAddress ?: IPv4Address.EMPTY

        val payload = XIntroResponsePayload(
            destinationAddress = requester.address,
            sourceLanAddress = selfEstimatedLan,
            sourceWanAddress = selfEstimatedWan,
            lanIntroductionAddress = introLan,
            wanIntroductionAddress = introWan,
            connectionType = network.wanLog.estimateConnectionType(),
            tunnel = false,
            identifier = idenitifier,
            extraBytes = extraBytes
        )
        if (intro != null) {
            val packet = createPunctureRequest(requester.lanAddress, requester.wanAddress,
            idenitifier)
            send(intro, packet)
        }
        // logger
        return serializePacket(MessageId.INTRODUCTION_RESPONSE, payload,prefix=prefix)
    }

    fun createPuncture(lanWalker: IPv4Address, wanWalker: IPv4Address, idenitifier: Int): ByteArray {
        val payload = XNATPuncturePayload(lanWalker, wanWalker, idenitifier)
        return serializePacket(MessageId.PUNCTURE, payload)
    }

    private fun createPunctureRequest(
        lanWalker: IPv4Address,
        wanWalker: IPv4Address,
        idenitifier: Int
    ): ByteArray {
        val payload = XNATPunctureRequestPayload(lanWalker, wanWalker, idenitifier)
        return serializePacket(MessageId.PUNCTURE_REQUEST, payload)
    }

    fun serializePacket(
        messageId: Int,
        payload: Serializable,
        sign: Boolean = true,
        peer: XPeer = selfPeer,
        prefix: ByteArray = this.prefix,
        encrypt: Boolean = true,
        timestamp: ULong? = null,
        recipient: XPeer? = null
    ): ByteArray {
        val payloads = mutableListOf<Serializable>()
        if (sign) {
            payloads += XNetAuthPayload(peer.publicKey.keyToBin())
        }
        payloads += XNetTimeSyncPayload(timestamp ?: claimGlobalTime())
        payloads += payload
        return serializePacket(messageId, payloads, sign, peer, prefix, encrypt, timestamp, recipient)
    }

    private fun serializePacket(
        messageId: Int,
        payload: List<Serializable>,
        sign: Boolean = true,
        peer: XPeer = selfPeer,
        prefix: ByteArray = this.prefix,
        encrypt: Boolean = true,
        timestamp: ULong? = null,
        recipient: XPeer? = null
    ): ByteArray {
        var packet = prefix
        packet += messageId.toChar().code.toByte()

        if (encrypt && recipient == null) {
            throw IllegalArgumentException("Recipient must be provided for encryption")
        }

        for ((index, item) in payload.withIndex()) {
            val serialized = item.serialize()
            packet += if (index == payload.size - 1 && encrypt && recipient != null) {
                val encrypted = recipient.publicKey.encrypt(serialized)
                encrypted
            } else {
                serialized
            }
        }

        val selfPeerKey = peer.key
        if (sign && selfPeerKey is XPrivateKey) {
            packet += selfPeerKey.sign(packet)
        }
        return packet
    }

    // Request and response deserialization

    private fun onIntroRequestPacket(packet: XPacket) {
        val (peer, payload) = packet.getAuthPayload(XIntroRequestPayload.Deserializer)
        onIntroRequest(peer, payload)
    }

    private fun onIntroResponsePacket(packet: XPacket) {
        val (peer, payload) = packet.getAuthPayload(XIntroResponsePayload.Deserializer)
        onIntroResponse(peer, payload)
    }

    private fun onPuncturePacket(packet: XPacket) {
        val (peer, payload) = packet.getAuthPayload(XNATPuncturePayload.Deserializer)
        onPuncture(peer, payload)
    }

    private fun onPunctureRequestPacket(packet: XPacket) {
        val (peer, payload) = packet.getAuthPayload(XNATPunctureRequestPayload.Deserializer)
        if (packet.source is IPv4Address) {
            onPunctureRequest(packet.source, payload)
        }
    }

    // Request and response handling
    internal open fun onIntroRequest(
        peer: XPeer,
        payload: XIntroRequestPayload
    ) {
        if (maxPeers >= 0 && getPeers().size >= maxPeers) {
            // log that the network has too many peers, dropping the current intro request
            return
        }
        val nwPeer = peer.copy(
            lanAddress = payload.sourceLanAddress,
            wanAddress = payload.sourceWanAddress
        )
        addVerifiedPeer(nwPeer)
        val packet = createIntroResponse(
            nwPeer,
            payload.identifier
        )
        send(peer, packet)
    }

    open fun onIntroResponse(
        peer: XPeer,
        payload: XIntroResponsePayload
    ) {
        // logger
        addEstimatedWan(peer, payload.destinationAddress)
        // Add the sender as a verified peer
        val newPeer = peer.copy(
            lanAddress = payload.sourceLanAddress,
            wanAddress = payload.sourceWanAddress
        )
        addVerifiedPeer(newPeer)
        // Process the introduced address
        if (!payload.wanIntroductionAddress.isEmpty() && payload.wanIntroductionAddress.ip != selfEstimatedWan.ip) {
            // WAN is not empty and its the same as us
            if (!payload.lanIntroductionAddress.isEmpty())
                discoverAddress(peer, payload.lanIntroductionAddress, serviceId)
            discoverAddress(peer, payload.lanIntroductionAddress, serviceId)
        } else if (!payload.lanIntroductionAddress.isEmpty() && payload.wanIntroductionAddress.ip == selfEstimatedWan.ip) {
            // Lan is not empty and WAN is same as ours, they are on the same LAN
            discoverAddress(peer, payload.lanIntroductionAddress, serviceId)
        } else if (!payload.wanIntroductionAddress.isEmpty()) {
            // WAN is same as ours, but we dont know the LAN
            // Try connect via WAN, NAT needs to support hairpinning
            discoverAddress(peer, payload.wanIntroductionAddress, serviceId)
            // Assume LAN is same as ours, (eg multiple instance of the application on
            // same machine)
            discoverAddress(peer, IPv4Address(selfEstimatedLan.ip, payload.wanIntroductionAddress.port), serviceId)
        }
    }

    private fun addEstimatedWan(peer: XPeer, wan: IPv4Address) {
        // change the estimated wan address if the sender is not on the same LAN
        // otherwise it would send us our LAN address
        if (!addressIsLAN(peer.address) && !peer.address.isLoopback() && !peer.address.isEmpty()) {
            // if this is a new peer, add our estimated WAN to the WAN estimation log
            // which can be used to determine symmetric NAT behaviour
            network.wanLog.addItem(
                WanEstimationLog.WanLogItem(
                Date(), peer.address, selfEstimatedLan, wan
            ))
        }
    }

    protected fun addVerifiedPeer(peer: XPeer) {
        network.addVerifiedPeer(peer)
        network.discoverServices(peer, listOf(serviceId))
    }

    protected fun discoverAddress(peer: XPeer, address: IPv4Address, serviceId: String) {
        if (address != selfEstimatedLan && address != selfEstimatedWan && !address.isEmpty()) {
            network.discoverAddress(peer, address, serviceId)
        }
    }

    private fun onPuncture(
        peer: XPeer,
        payload: XNATPuncturePayload
    ) {
        // logger call
    }

    private fun onPunctureRequest(
        address: IPv4Address,
        payload: XNATPunctureRequestPayload
    ) {
        // logger call
        // If target is on same WAN
        val target = if (payload.wanWalkerAddress.ip == selfEstimatedWan.ip) {
            // Set the target address to the LAN address
            payload.lanWalkerAddress
        } else {
            payload.wanWalkerAddress
        }

        val packet = createPuncture(selfEstimatedLan, selfEstimatedWan, payload.identifier)
        send(target, packet)
    }

    fun send(peer: XPeer, data: ByteArray) {
        val verifiedPeer = network.getVerifiedByPublicKeyBin(peer.publicKey.keyToBin())
        endpoint.send(verifiedPeer ?: peer, data)
    }

    fun send(address: XAddress, data: ByteArray) {
        val probablePeer = network.getVerifiedByAddress(address)
        if (probablePeer != null) {
            probablePeer.lastResp = Date()
        }
        // Transport SEND method is async
        endpoint.send(address, data)
    }

    object MessageId {
        const val PUNCTURE_REQUEST = 250
        const val PUNCTURE = 249
        const val INTRODUCTION_REQUEST = 246
        const val INTRODUCTION_RESPONSE = 245
    }
}
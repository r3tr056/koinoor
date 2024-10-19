package com.gfms.xnet.discovery

import com.gfms.xnet.XCommunity
import com.gfms.xnet.discovery.payload.PingPayload
import com.gfms.xnet.discovery.payload.PongPayload
import com.gfms.xnet.discovery.payload.SimilarityRequestPayload
import com.gfms.xnet.discovery.payload.SimilarityResponsePayload
import com.gfms.xnet.overlay.Overlay
import com.gfms.xnet.packet.XPacket
import com.gfms.xnet.packet.payloads.ConnectionType
import com.gfms.xnet.packet.payloads.XIntroResponsePayload
import com.gfms.xnet.xaddress.XAddress
import com.gfms.xnet.xpeer.XPeer
import java.util.*

class DiscoveryCommunity: XCommunity(),PingOverlay {
    override val serviceId: String = "7e313685c1912a141279f8248fc8db5899c5df5a"
    data class PingRequest(val identifier: Int, val peer: XPeer, val startTime: Date)
    private val pingRequestCache: MutableMap<Int, PingRequest> = mutableMapOf()

    init {
        messageHandlers[MessageId.SIMILARITY_REQUEST] = ::onSimilarityRequestPacket
        messageHandlers[MessageId.SIMILARITY_RESPONSE] = ::onSimilarityResponsePacket
        messageHandlers[MessageId.PING] = ::onPingPacket
        messageHandlers[MessageId.PONG] = ::onPongPacket
    }

    // Creating a request
    private fun createSimilarityRequest(peer: XPeer): ByteArray {
        val globalTime = claimGlobalTime()
        val payload = SimilarityRequestPayload(
            (globalTime % 65536u).toInt(),
            selfEstimatedLan,
            selfEstimatedWan,
            ConnectionType.UNKNOWN,
            getMyOverlays(peer)
        )
        // logger call
        return serializePacket(MessageId.SIMILARITY_REQUEST, payload, peer=peer)
    }

    fun sendSimilarityRequest(peer: XPeer) {
        val selfPeerSet = network.serviceOverlays.values.map { it.selfPeer }.toSet()
        for (selfPeer in selfPeerSet) {
            val packet = createSimilarityRequest(selfPeer)
            send(peer, packet)
        }
    }

    private fun createSimilarityResponse(identifier: Int, peer: XPeer): ByteArray {
        val payload = SimilarityResponsePayload(identifier, getMyOverlays(peer))
        // logger call
        return serializePacket(messageId=MessageId.SIMILARITY_RESPONSE,payload=payload, peer=peer)
    }

    private fun createPing(): Pair<Int, ByteArray> {
        val globalTime = claimGlobalTime()
        val payload = PingPayload((globalTime % UShort.MAX_VALUE).toInt())
        // logger call
        // Ping packets are un signed
        return Pair(payload.identifier, serializePacket(messageId=MessageId.PING, payload=payload, sign=false))
    }

    override fun sendPing(peer: XPeer) {
        val (identifier, packet) = createPing()
        val pingRequest = PingRequest(identifier = identifier, peer = peer, startTime = Date())
        pingRequestCache[identifier] = pingRequest
        // TODO : Implement PING cache timeout
        send(peer, packet)
    }

    internal fun createPong(identifier: Int): ByteArray {
        val payload = PongPayload(identifier)
        // logger call
        return serializePacket(messageId=MessageId.PONG,payload=payload, sign=false)
    }

    // Request and response deserialization
    internal fun onSimilarityRequestPacket(packet: XPacket) {
        val (peer, payload) = packet.getAuthPayload(SimilarityRequestPayload.Deserializer)
        onSimilarityRequest(peer, payload)
    }

    internal fun onSimilarityResponsePacket(packet: XPacket) {
        val (peer, payload) = packet.getAuthPayload(SimilarityResponsePayload.Deserializer)
        onSimilarityResponse(peer, payload)
    }

    internal fun onPingPacket(packet: XPacket) {
        val payload = packet.getPayload(PingPayload.Deserializer)
        onPing(packet.source, payload)
    }

    internal fun onPongPacket(packet: XPacket) {
        val payload = packet.getPayload(PongPayload.Deserializer)
        onPong(payload)
    }

    override fun onIntroResponse(peer: XPeer, payload: XIntroResponsePayload) {
        super.onIntroResponse(peer, payload)
        sendSimilarityRequest(peer)
    }

    internal fun onSimilarityRequest(peer: XPeer, payload: SimilarityRequestPayload) {
        // logger call
        network.addVerifiedPeer(peer)
        network.discoverServices(peer, payload.preferenceList)

        val selfPeerSet = network.serviceOverlays.values.map { it.selfPeer }.toSet()
        for (selfPeer in selfPeerSet) {
            val packet = createSimilarityResponse(payload.identifier, selfPeer)
            send(peer, packet)
        }
    }

    internal fun onSimilarityResponse(peer: XPeer, payload: SimilarityResponsePayload) {
        // logger call
        if (maxPeers >= 0 && getPeers().size >= maxPeers && !network.verifiedPeers.contains(peer)) {
            // logger call - Drop similarity response from the peer, too many peers
            return
        }
        network.addVerifiedPeer(peer)
        network.discoverServices(peer, payload.preferenceList)
    }

    internal fun onPing(address: XAddress, payload: PingPayload) {
        // logger call
        val packet = createPong(payload.identifier)
        send(address, packet)
    }

    internal fun onPong(payload: PongPayload) {
        // logger
        // If the ping is in the cache
        val pingReq = pingRequestCache[payload.identifier]
        if (pingReq != null) {
            pingReq.peer.addPing((Date().time - pingReq.startTime.time) / 1000.0)
            pingRequestCache.remove(payload.identifier)
        }
    }

    private fun getMyOverlays(peer: XPeer): List<String> {
        return network.serviceOverlays.filter { it.value.selfPeer == peer }
            .map { it.key }
    }

    object MessageId {
        const val SIMILARITY_REQUEST = 1
        const val SIMILARITY_RESPONSE = 1
        const val PING = 1
        const val PONG = 1
    }

    class Factory : Overlay.Factory<DiscoveryCommunity>(DiscoveryCommunity::class.java)
}
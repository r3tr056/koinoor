package com.gfms.xnet.discovery.kademlia

import com.gfms.xnet.xaddress.XAddress
import java.util.concurrent.TimeoutException

const val k_topic = "_KAD"

object KadMsgTypes {
    const val PING="PING"
    const val FIND_NODE="FIND_NODE"
    const val GREET="GREET"
    const val BYE="BYE"
}

data class XMessage(val topic: String, val msg: String, val id: KadId)

data class FindNodeReq(val senderId: KadId, val findId: KadId)

data class FindNodeRes(val peerExact: KAddress?=null, val peersToAsk:List<KAddress>?=null)

class XNetService {
    fun <T> sendAndRecv(peer: XAddress, msg: XMessage): T? {
        return null
    }

    fun send(peer: XAddress, msg: XMessage) {

    }
}

class KadController {
    private lateinit var addressBook: AddressBook

    fun onPing(senderId: KadId, sender: XAddress): KadId {
        addressBook.addRecord(KAddress(senderId, sender))
        return addressBook.getMine().id
    }

    fun onGreen(senderId: KadId, sender: XAddress) {
        addressBook.addRecord(KAddress(senderId, sender))
    }

    fun onBye(senderId: KadId) {
        addressBook.removeRecord(senderId)
    }

    fun onFindNode(payload: FindNodeReq, sender: XAddress): FindNodeRes {
        val peerExact = if (addressBook.getMine().id == payload.findId)
            addressBook.getMine()
        else
            addressBook.getRecordsById(payload.findId)
        val peersToAsk = addressBook.getCluster(payload.findId).wrpeers.sortedBy { it.id.xor(payload.findId) }
        val res = if (peerExact != null)
            FindNodeRes(peerExact, null)
        else
            FindNodeRes(null, peersToAsk)
        addressBook.addRecord(KAddress(payload.senderId, sender))
        return res
    }

}

class KadService {

    lateinit var addressBook: AddressBook
    private lateinit var netX: XNetService
    private val asked = hashMapOf<KadId, MutableSet<KAddress>>()

    suspend fun bootstrap(bootstrapPeer: XAddress): Boolean {
        if (!ping(bootstrapPeer)) return false
        findNode(addressBook.getMine().id)
        val myCluster = addressBook.getCluster(addressBook.getMine().id)
        myCluster.peers.forEach { greet(it.address) }
        return true
    }

    suspend fun byteMyCluster() {
        addressBook.getMyClusterExceptMe().peers.forEach { bye(it.address) }
    }

    suspend fun ping(peer: XAddress): Boolean {
        val msg = XMessage(k_topic, KadMsgTypes.PING, addressBook.getMine().id)
        return try {
            val peerId = netX.sendAndRecv<KadId>(peer, msg)
            addressBook.addRecord(KAddress(peerId!!, peer))
            true
        } catch (e: TimeoutException) {
            false
        }
    }

    suspend fun greet(peer: XAddress) {
        val msg = XMessage(k_topic, KadMsgTypes.GREET, addressBook.getMine().id)
        netX.send(peer, msg)
    }

    suspend fun bye(peer: XAddress) {
        val msg = XMessage(k_topic, KadMsgTypes.BYE, addressBook.getMine().id)
        netX.send(peer, msg)
        val peerAddr = addressBook.getRecords().find { it.address == peer }?:return
        addressBook.removeRecord(peerAddr.id)
    }

    suspend fun findNode(findId: KadId): KAddress? {
        val peerFromAddressBook = addressBook.getRecordsById(findId)
        if (peerFromAddressBook != null) return peerFromAddressBook
        if (!asked.containsKey(findId)) asked[findId] = mutableSetOf()

        while (true) {
            val cluster = addressBook.getCluster(findId)
            val closestPeer = cluster.peers.sortedBy { it.id.xor(findId) }.firstOrNull { !asked[findId]!!.contains(it) } ?: break
            val result = sendFindNodeAndProcessResult(closestPeer, findId)
            if (result != null) return result
        }

        while (true) {
            val peers = addressBook.getRecords()
            val closestPeer = peers.sortedBy { it.id.xor(findId) }.firstOrNull { !asked[findId]!!.contains((it)) }
            if (closestPeer==null) {
                asked.remove(findId)
                return null
            }
            val res = sendFindNodeAndProcessResult(closestPeer, findId)
            if (res != null) return res
        }
    }

    private suspend fun sendFindNodeAndProcessResult(peer: KAddress, findId: KadId): KAddress? {
        val res = sendFindNode(peer.address, findId)!!
        if (res.peerExact != null) {
            addressBook.addRecord(res.peerExact)
            asked.remove(findId)
            return res.peerExact
        }
        asked[findId]!!.add(peer)
        res.peersToAsk!!.forEach { addressBook.addRecord(it) }
        return null
    }

    private suspend fun sendFindNode(peer: XAddress, findId: KadId): FindNodeRes? {
        val payload = FindNodeReq(addressBook.getMine().id, findId)
        val msg = XMessage(k_topic, KadMsgTypes.FIND_NODE, payload.senderId)
        return netX.sendAndRecv(peer, msg)
    }
}
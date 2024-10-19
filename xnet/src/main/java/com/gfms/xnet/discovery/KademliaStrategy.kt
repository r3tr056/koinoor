package com.gfms.xnet.discovery
/**
//TODO : To be replaced by XNET native classes
import com.gfms.xnet.discovery.kademlia.AddressBook
import com.gfms.xnet.discovery.kademlia.KAddress
import com.gfms.xnet.discovery.kademlia.KadId

class KademliaStrategy (
    private val overlay: Overlay,
    private val timeout: Double
): DiscoveryStrategy {
    lateinit var addressBook: AddressBook
    private val asked = hashMapOf<KadId, MutableSet<KAddress>>()

    override fun load() {
        super.load()
        if (!ping(bootstrapPeer)) return false
        findNode(addressBook.getMine().id)
        val myCluster = addressBook.getCluster(addressBook.getMine().id)
        myCluster.peers.forEach { greet(it.address) }
    }

    fun ping
}
        **/
package com.gfms.xnet.discovery

import com.gfms.xnet.xaddress.XAddress
import com.gfms.xnet.xpeer.XPeer
import com.gfms.xnet.ipv4.IPv4Address
import com.gfms.xnet.overlay.Overlay
import com.gfms.xnet.tftp.TFTPCommunity
import kotlin.math.min

class XNetwork {
    // All known addresses, mapped to (introduction peer member ID, service ID)
    // MutableMap<Address, Pair<MID, SID?>>
    val allAddresses: MutableMap<IPv4Address, Pair<String, String?>> = mutableMapOf()
    // All the verified peer objects, xpeer.address of all peers in this set must be in [allAddresses
    val verifiedPeers: MutableSet<XPeer> = mutableSetOf()
    // Peer blocklist, not to be added to the network
    val blocklist = mutableListOf<IPv4Address>()
    // Peer MID blocklist
    val blocklistMids = mutableListOf<String>()
    // Map of services advertised by peers
    private val servicesPerPeer = mutableMapOf<String ,MutableSet<String>>()
    // Map of SIDs to local overlays
    val serviceOverlays = mutableMapOf<String, Overlay>()
    val wanLog = WanEstimationLog()
    val graphLock = Object()

    /** A peer has introduced us to another IP address
     *  @param peer : The peer that did the intro
     *  @param address : The introduced peer address
     *  @param serviceId : The service through which the peer was discovered
     */
    fun discoverAddress(peer: XPeer, address: IPv4Address, serviceId: String?=null) {
        if (address in blocklist) {
            addVerifiedPeer(peer)
            return
        }
        synchronized(graphLock) {
            if (address !in allAddresses || allAddresses[address]!!.first !in verifiedPeers.map { it.mid }) {
                // This is new address, prev parent has been removed
                allAddresses[address] = Pair(peer.mid, serviceId)
            }
            addVerifiedPeer(peer)
        }
    }

    /**
     * A Peer advertises some service it supports
     * @param peer : The Peer to update the services for
     * @param serviceIds : The list of service IDs to register
     */
    fun discoverServices(peer: XPeer, serviceIds: List<String>) {
        synchronized(graphLock) {
            val peerServices = servicesPerPeer[peer.mid] ?: mutableSetOf()
            peerServices.addAll(serviceIds)
            servicesPerPeer[peer.mid] = peerServices
            getVerifiedByPublicKeyBin(peer.publicKey.keyToBin())?.supportsTFTP = peerServices.contains(
                TFTPCommunity.SERVICE_ID)
        }
    }

    /**
     * Once a peer is verified, the job is to add it
     * to the list of verified peers
     * @param peer : The new peer
     */
    fun addVerifiedPeer(peer: XPeer) {
        if (peer.mid in blocklistMids) return
        synchronized(graphLock) {
            // Just an address update
            for (known in verifiedPeers) {
                if (known.mid == peer.mid) {
                    if (!peer.address.isEmpty()) {
                        // Simply an address update
                        known.address = peer.address
                    }
                    if (!peer.lanAddress.isEmpty()) {
                        known.lanAddress = peer.lanAddress
                    }
                    return
                }
            }
            if (peer.address in allAddresses) {
                if (peer !in verifiedPeers) {
                    verifiedPeers.add(peer)
                }
            } else if (peer.address !in blocklist) {
                if (peer.address !in allAddresses) {
                    allAddresses[peer.address] = Pair("", null)
                }
                if (peer !in verifiedPeers) {
                    verifiedPeers.add(peer)
                }
            }
        }
    }

    /**
     * Register an overlay to provide a certain service ID
     * @param serviceId : The ID of the service
     * @param overlay : The overlay instance of the service
     */
    fun registerServiceProvider(serviceId: String, overlay: Overlay) {
        synchronized(graphLock) {
            serviceOverlays[serviceId] = overlay
        }
    }

    /**
     * Get peers which support a certain service
     * @param serviceId: The service ID to fetch peers for
     */
    fun getPeersForService(serviceId: String): List<XPeer> {
        val out = mutableListOf<XPeer>()
        synchronized(graphLock) {
            for (peer in verifiedPeers) {
                val peerServices = servicesPerPeer[peer.mid]
                if (peerServices != null) {
                    if (serviceId in peerServices) {
                        out += peer
                    }
                }
            }
        }
        return out
    }

    /**
     * Get a list of known SIDs for a peer
     * @param peer : The peer to check
     */
    fun getServicesForPeer(peer: XPeer) : Set<String> {
        synchronized(graphLock) {
            return servicesPerPeer[peer.mid] ?: setOf()
        }
    }

    /**
     * All address that are ready to be walked to based on a SID
     * @param serviceId: The SID to filter on
     */
    fun getWalkableAddresses(serviceId: String?=null) : List<IPv4Address> {
        synchronized(graphLock) {
            val known =
                if (serviceId != null) getPeersForService(serviceId = serviceId) else verifiedPeers
            val knownAddresses = known.map { it.address }
            var out = (allAddresses.keys.toSet() - knownAddresses).toList()
            if (serviceId != null) {
                val newOut = mutableListOf<IPv4Address>()
                for (address in out) {
                    val (introPeer, service) = allAddresses[address] ?: return listOf()
                    val services = servicesPerPeer[introPeer] ?: mutableSetOf()
                    if (service != null) {
                        services += service
                    }
                }
                out = newOut
            }
            return out as List<IPv4Address>
        }
    }

    /**
     * Get a verified peer by its IP address, if multiple peers use the same IP address
     * this returns only single Instance of the Peer
     * @param address: The address to search for
     * @return : The XPeer
     */
    fun getVerifiedByAddress(address: XAddress): XPeer? {
        synchronized(graphLock) {
            return when(address) {
                is IPv4Address -> verifiedPeers.find { it.address == address }
                else -> null
            }
        }
    }

    /**
     * Get a verified peer by its public key bin
     * @param publicKeyBin : The string repr of the public key
     * @return The [XPeer] object for this public key or null
     */
    fun getVerifiedByPublicKeyBin(publicKeyBin: ByteArray): XPeer? {
        synchronized(graphLock) {
            return verifiedPeers.find { it.publicKey.keyToBin().contentEquals(publicKeyBin)}
        }
    }

    /**
     * get the addresses introduced to us by a certain peer
     * @param peer : The peer to get the introductions for
     * @return A list of the introduced addresses
     */
    fun getIntroductionFrom(peer: XPeer): List<IPv4Address> {
        synchronized(graphLock) {
            return allAddresses.filter { it.value.first == peer.mid }.map { it.key }
        }
    }

    fun removeByAddress(address: IPv4Address) {
        synchronized(graphLock) {
            allAddresses.remove(address)
            // Remove peers that have only IPv4Address
            val peer = verifiedPeers.find { it.address == address}
            if (peer != null) {
                peer.address = IPv4Address.EMPTY
                if (!peer.isConnected()) {
                    verifiedPeers.remove(peer)
                }
            }
            servicesPerPeer.remove(peer!!.mid)
        }
    }

    /**
     * Remove a verified Peer
     * @param peer : The peer to remove
     */
    fun removePeer(peer: XPeer) {
        synchronized(graphLock) {
            allAddresses.remove(peer.address)
            verifiedPeers.remove(peer)
            servicesPerPeer.remove(peer.mid)
        }
    }

    // Returns a random
    fun getRandomPeer(): XPeer? {
        synchronized(graphLock) {
            return if (verifiedPeers.isNotEmpty()) verifiedPeers.random() else null
        }
    }

    /**
     * Returns a random XPeers(list) out of the verified peers of size [maxSampleSize]
     */
    fun getRandomPeers(maxSampleSize: Int): List<XPeer> {
        synchronized(graphLock) {
            val sampleSize = min(verifiedPeers.size, maxSampleSize)
            val shuffled = verifiedPeers.shuffled()
            return shuffled.subList(0, sampleSize)
        }
    }
}
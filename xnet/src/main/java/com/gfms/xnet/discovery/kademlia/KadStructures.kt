package com.gfms.xnet.discovery.kademlia

import com.gfms.xnet.xaddress.XAddress
import java.math.BigInteger

const val k_bucket_size = 16
typealias KadId = BigInteger

// KAD Node Address
data class KAddress(val id: KadId, val address: XAddress)

// KAD Network Tree Node, its a Binary tree, so the leaves are one, zero
data class TreeNode(val bitIndex: Int=0,
                    val id: String="",
                    val parent: TreeNode? = null,
                    var zero: TreeNode? = null,
                    var one: TreeNode?=null,
                    val kBucket: MutableList<BigInteger> = mutableListOf()) {

    override fun equals(other: Any?): Boolean {
        other as TreeNode
        return id == other.id
    }
    override fun toString(): String {
        return kBucket.toString()
    }

    override fun hashCode(): Int {
        var result = bitIndex
        result = 31 * result + id.hashCode()
        result = 31 * result + (parent?.hashCode() ?: 0)
        result = 31 * result + (zero?.hashCode() ?: 0)
        result = 31 * result + (one?.hashCode() ?: 0)
        result = 31 * result + kBucket.hashCode()
        return result
    }
}

data class BinaryTree(private val root: TreeNode, private val k: Int, private val bitSize: Int, private val bitShift: Int) {
    private val leaves : MutableList<TreeNode> = mutableListOf(root)
    fun clear() {
        leaves.clear()
        root.kBucket.clear()
        root.one = null
        root.zero = null
        leaves.add(root)
    }
    private fun applyBitShift(current: Int) = (bitSize - bitShift) - (current + 1)
    override fun toString(): String {
        val elems = getNeighboursByName().map{ it -> "\tprefix=${it.key}, size=${it.value.size}, elements=${it.value.map {it.toString(2)}}"}.joinToString("\n")
        return "BinaryTree {\n$elems\n}"
    }

    fun addData(data: BigInteger): Boolean {
        val currentNode = findAppropriateNode(data)
        if (currentNode.kBucket.contains(data)) return false
        if (currentNode.kBucket.size + 1 <= 2 * k) {
            currentNode.kBucket.add(data)
            return true
        }

        val newBitIndex = currentNode.bitIndex + 1
        val zero = TreeNode(bitIndex=newBitIndex, id=currentNode.id + "0", parent=currentNode)
        val one = TreeNode(bitIndex = newBitIndex, id=currentNode.id + "1", parent = currentNode)
        currentNode.kBucket.forEach { d ->
            val bit = d.testBit(applyBitShift(currentNode.bitIndex))
            if (bit) one.kBucket.add(d)
            else zero.kBucket.add(d)
        }

        val bit = data.testBit(applyBitShift(currentNode.bitIndex))
        if (bit) one.kBucket.add(data)
        else zero.kBucket.add(data)

        if (zero.kBucket.size >= k && one.kBucket.size >= k) {
            currentNode.zero = zero
            currentNode.one = one
            currentNode.kBucket.clear()
            leaves.remove(currentNode)
            leaves.add(zero)
            leaves.add(one)
        } else {
            currentNode.kBucket.add(data)
        }
        return true
    }

    fun removeData(data: BigInteger): Boolean {
        val currentNode = findAppropriateNode(data)
        val record = currentNode.kBucket.remove(data)
        if (!record) return false
        if (currentNode.kBucket.size >= k_bucket_size) return false
        if (currentNode == root) return true

        val one = currentNode.parent!!.one!!
        val zero = currentNode.parent.zero!!
        currentNode.parent.one = null
        currentNode.parent.zero = null
        currentNode.parent.kBucket.addAll(one.kBucket + zero.kBucket)

        leaves.remove(one)
        leaves.remove(zero)
        leaves.add(currentNode.parent)

        return true
    }
    // Flatten the tree
    fun flatten() = leaves.map {it.kBucket}.flatten()

    fun getNeighbours() = leaves.map{it.kBucket}.toList()

    fun getNeighboursByName() = leaves.associate { it.id to it.kBucket }

    fun getNeighbours(of: BigInteger): List<BigInteger> {
        val node = findAppropriateNode(of)
        if (node == root) return node.kBucket.toList()
        return node.parent!!.zero!!.kBucket + node.parent.one!!.kBucket
    }

    fun getLeaf(of: BigInteger) = findAppropriateNode(of).kBucket

    private fun findAppropriateNode(data: BigInteger): TreeNode {
        var currentNode = root
        while (true) {
            val bit = data.testBit(applyBitShift(currentNode.bitIndex))
            if (bit && currentNode.one != null) {
                currentNode = currentNode.one!!
                continue
            }
            if (!bit && currentNode.zero != null) {
                currentNode = currentNode.zero!!
                continue
            }
            break
        }
        return currentNode
    }
}

data class Cluster(val name: KadId, val peers: List<KAddress>)

interface AddressBook {
    fun addRecord(address: KAddress)
    fun removeRecord(id: KadId)
    fun getRecords(): List<KAddress>
    fun getRecordsById(id: KadId): KAddress?
    fun getMine(): KAddress
    fun getCluster(of: KadId): Cluster
    fun getMyCluster() = getCluster(getMine().id)
    fun getMyClusterExceptMe(): Cluster {
        val cluster = getMyCluster()
        return Cluster(cluster.name, cluster.peers.filter {it!=getMine()})
    }
    fun clear()
}

class InMemoryAddressBook(
    private val myAddress: KAddress,
    k: Int = 20,
    bitSize: Int = 256,
    bitShift: Int = 20
) : AddressBook {

    private val addresses = BinaryTree(root= TreeNode(), k=k, bitSize=bitSize, bitShift=bitShift)
    private val addressesByIds = hashMapOf<KadId, KAddress>()

    init {
        addressesByIds[myAddress.id] = myAddress
        addresses.addData(myAddress.id)
    }

    override fun addRecord(address: KAddress) {
        if (addresses.addData(address.id))
            addressesByIds[address.id] = address
    }

    override fun removeRecord(id: KadId) {
        if (addresses.removeData(id))
            addressesByIds.remove(id)
    }

    override fun getRecords(): List<KAddress> {
        return addresses.flatten().map { addressesByIds[it]!! }
    }

    override fun getRecordsById(id: KadId): KAddress? {
        return addressesByIds[id]
    }

    override fun getMine(): KAddress {
        return myAddress
    }

    override fun getCluster(of: KadId): Cluster {
        val peerIds = addresses.getNeighbours(of).sorted()
        val name = peerIds.fold(BigInteger.ZERO) { acc, id -> acc + id } / BigInteger.valueOf(peerIds.size.toLong())
        return Cluster(name, peerIds.map{addressesByIds[it]!!})
    }

    override fun clear() {
        addressesByIds.clear()
        addresses.clear()

        addressesByIds[myAddress.id] = myAddress
        addresses.addData(myAddress.id)
    }
}

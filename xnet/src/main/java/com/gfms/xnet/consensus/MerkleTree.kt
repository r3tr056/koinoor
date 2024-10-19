package com.gfms.xnet.consensus

import com.gfms.boltz.hashing.SHA256Digest
import com.gfms.xnet.utils.toHex
import java.lang.IllegalArgumentException
import kotlin.math.floor

class MerkleTree(vararg input: ByteArray, val hash: (ByteArray) -> ByteArray = { data -> SHA256Digest.digest(data) })  {

    private var mutated = false
    private val elems: List<ByteArray> = input.map { hash(it) }.toSortedSet(byteArrayComparator).toList()
    private val layers = layers(elems.toList())

    private fun layers(elems: List<ByteArray>): List<List<ByteArray>> {
        if (elems.isEmpty())
            return emptyList()
        val layers = mutableListOf<List<ByteArray>>()
        layers.add(elems)

        while(layers[layers.size - 1].size > 1) {
            layers.add(nextLayer(layers[layers.size - 1]))
        }
        return layers
    }

    fun root(): ByteArray {
        return layers.last().first()
    }

    fun hexRoot(): ByteArray = root().toHex().toByteArray()

    fun containsLeaf(el: ByteArray): Boolean = elems.binarySearch(element = el,comparator=byteArrayComparator) >= 0
    fun conatinsElement(el: ByteArray): Boolean = containsLeaf(hash(el))

    fun elementIndex(el: ByteArray): Int {
        val idx = elems.binarySearch(el, byteArrayComparator)
        if (idx < 0) {
            throw IllegalArgumentException("Element ${el.toHex()} does not exist in Merkel tree")
        }
        return idx
    }

    fun proofForLeaf(el: ByteArray): List<ByteArray> {
        var idx = elems.binarySearch(el, byteArrayComparator)
        if (idx < 0) {
            throw IllegalArgumentException("Element ${el.toHex()} does not exist in Merkel tree")
        }
        val res = mutableListOf<ByteArray>()
        layers.forEach { next ->
            val pairElem = pairElem(idx, next)
            if (pairElem != null) {
                res.add(pairElem)
            }
            idx = floor(idx/2.0).toInt()
        }
        return res
    }

    fun hexProofForLeaf(el: ByteArray): List<ByteArray> {
        val proof = proofForLeaf(el)
        return proof.map { it.toHex().toByteArray() }
    }

    fun proofForElement(el: ByteArray): List<ByteArray> {
        return proofForLeaf(hash(el))
    }

    fun hexProofForElement(el: ByteArray): List<ByteArray> {
        return proofForLeaf(hash(el)).map { it.toHex().toByteArray() }
    }

    private fun pairElem(idx: Int, layer: List<ByteArray>): ByteArray? {
        val pairIdx = if (idx % 2 == 0) idx + 1 else idx - 1
        return if (pairIdx < layer.size)
            layer[pairIdx]
        else
            null
    }

    private fun nextLayer(elems: List<ByteArray>): List<ByteArray> {
        val res = mutableListOf<ByteArray>()
        elems.forEachIndexed { idx, next ->
            if (idx % 2 == 0) {
                res.add(combinedHash(next, elems.elementAtOrNull(idx + 1), hash))
            }
        }
        return res
    }

    companion object {
        fun verifyProof(proof: List<ByteArray>, root: ByteArray, leaf: ByteArray, hash: (ByteArray) -> ByteArray): Boolean {
            var computedHash = leaf
            for (proofElem in proof) {
                computedHash = if (byteArrayComparator.compare(computedHash, proofElem) < 0) {
                    combinedHash(computedHash, proofElem, hash)
                } else {
                    combinedHash(proofElem, computedHash, hash)
                }
            }
            return byteArrayComparator.compare(computedHash, root) == 0
        }

        private val byteArrayComparator = Comparator<ByteArray> { a, b ->
            var res = 0
            for (i in a.indices) {
                val cmp = a[i].toInt().compareTo(b[i].toInt())
                if (cmp != 0) {
                    res = cmp
                    break
                }
            }
            res
        }

        fun verifyProof(proof: List<ByteArray>, root: ByteArray, leaf: ByteArray, idx: Int, hash: (ByteArray) -> ByteArray): Boolean {
            var computedHash = leaf
            var i = idx
            for (proofElement in proof) {
                computedHash = if (i % 2 == 0) {
                    combinedHash(computedHash, proofElement, hash)
                } else {
                    combinedHash(proofElement, computedHash, hash)
                }
                i /= 2
            }

            return byteArrayComparator.compare(computedHash, root) == 0
        }

        private fun combinedHash(d1: ByteArray, d2: ByteArray?, hash: (ByteArray) -> ByteArray): ByteArray {
            if (d2 == null) {
                return d1
            }

            val list = listOf(d1, d2).sortedWith(byteArrayComparator)
            return hash(list.first().plus(list.last()))
        }
    }
}

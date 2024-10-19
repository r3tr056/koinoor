package com.gfms.xnet.xaddress

import com.gfms.xnet.xpeer.XID
import java.net.Inet6Address
import java.net.InetAddress
import java.net.UnknownHostException

/**
import java.lang.IllegalArgumentException
import java.nio.ByteBuffer

open class XAddress(val components: List<Pair<Protocol, ByteArray>>) {
    constructor(bytes: ByteArray): this(parseBytes(bytes))

    constructor(parentAddr: XAddress, childAddr: XAddress): this(concatProtocols(parentAddr, childAddr))

    constructor(parentAddr: XAddress, peerId: XPeerId): this(concatPeerId(parentAddr, peerId))

    fun writeBytes(buf: ByteBuffer): ByteBuffer {
        for (component in components) {
            buf.put(component.first.encoded)
            component.first.writeAddressBytes(buf, component.second)
        }
        return buf
    }

    fun getBytes(): ByteArray = writeBytes(Unpooled.buffer()).toByteArray()

    fun toPeerIdAndAddr(): Pair<PeerId, XAddress> {
        if (!has(Protcol.XNET))
            throw IllegalArgumentException("XAddress has no peer id")

        return Pair(PeerId.fromBase58(getStringComponent(Protocol)))
    }
}
**/

open class XAddress(
    val components: List<Pair<XProtocol, ByteArray>>
) {

    fun filterAddressComponents(vararg proto: XProtocol): List<Pair<XProtocol, ByteArray>> = components.filter { proto.contains(it.first) }

    fun getComponent(proto: XProtocol): ByteArray? = filterAddressComponents(proto).firstOrNull()?.second

    fun getStringComponent(xproto: XProtocol): String? = filterStringComponents(xproto).firstOrNull()?.second

    fun has(proto: XProtocol): Boolean = getComponent(proto) != null
    fun hasAny(vararg xprotos: XProtocol) = protos.any { has(it) }

    fun filterStringComponents(vararg proto: XProtocol): List<Pair<XProtocol, String?>> =
        components.map { p-> p.first to if (p.first.size == 0) null else p.first.bytesToAddress(p.second) }

    internal fun split(pred: (XProtocol) -> Boolean): List<XAddress> {
        val addresses = mutableListOf<XAddress>()
        split(addresses, components, pred)
        return addresses
    }

    private fun split(
        accumulated: MutableList<XAddress>,
        remainingComponents: List<Pair<XProtocol, ByteArray>>,
        pred: (XProtocol) -> Boolean
    ) {
        val splitIndex = remainingComponents.indexOfLast{ pred(it.first) }
        if (splitIndex > 0) {
            accumulated.add(0, XAddress(remainingComponents.subList(splitIndex,
                remainingComponents.size)))
            split(accumulated,
                remainingComponents.subList(0, splitIndex),
                pred)
        } else {
            accumulated.add(0, XAddress(remainingComponents))
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as XAddress
        return toString() == other.toString()
    }

    override fun toString(): String = filterStringComponents().joinToString("") { p ->
        "/" + p.first.typeName + if (p.second != null) "/" + p.second else ""
    }

    override fun hashCode(): Int {
        return toString().hashCode()
    }

    fun split(): List<String> {

    }

    constructor(xaddr: String): this(parseString(xaddr))

    companion object {
        @JvmStatic
        fun fromString(addr: String): XAddress { // helper method for Java access
            return XAddress(addr)
        }

        private fun parseString(xaddr: String):List<Pair<XProtocol, ByteArray>> {
            val ret: MutableList<Pair<XProtocol, ByteArray>> = mutableListOf()

            try {
                var addr = xaddr
                while (addr.endsWith("/"))
                    addr = addr.substring(0, addr.length - 1)
                val parts = addr.split("/")
                if (parts[0].isNotEmpty()) throw IllegalArgumentException("XAddress must start with a /")

                var i = 1
                while (i < parts.size) {
                    val part = parts[i++]
                    val p = XProtocol.getOrThrow(part)

                    val bytes = if (p.size == 0) ByteArray(0) else {
                        val component = if (p.isPath())
                            "/" + parts.subList(i, parts.size).reduce { a, b -> "$a/$b" }
                        else parts[i++]

                        if (component.isEmpty())
                            throw IllegalArgumentException("Protocol requires address, but not provided!")
                        p.addressToBytes(component)
                    }
                    ret.add(p to bytes)
                    if (p.isPath())
                        break
                }
            } catch (e: Exception) {
                throw IllegalArgumentException("Malformed XAddress: $xaddr", e)
            }
            return ret
        }

        private fun parseBytes(xaddr: ByteArray): List<Pair<XProtocol, ByteArray>> {
            val ret: MutableList<Pair<XProtocol, ByteArray>> = mutableListOf()
            while (xaddr.isNotEmpty()) {
                val protocol = XProtocol.getOrThrow(xaddr)
                ret.add(protocol to protocol.readAddressBytes(xaddr))
            }
            return ret
        }

        private fun concatProtocols(parentAddr: XAddress, childAddr: XAddress): List<Pair<XProtocol, ByteArray>> {
            return parentAddr.components + childAddr.components
        }

        private fun concatPeerId(addr: XAddress, peerId: XID): List<Pair<XProtocol, ByteArray>> {
            if (addr.has(XProtocol.XNET))
                throw IllegalArgumentException("XAddress already has peer id")
            val protocols = addr.components.toMutableList()
            protocols.add(Pair(XProtocol.XNET, peerId.toByteArray()))
        }
    }
}

class XAddrDNS {
    interface Resolver {
        fun resolveDNS4(hostname: String): List<XAddress>
        fun resolveDNS6(hostname: String): List<XAddress>
    }

    companion object {
        private val dnsProtos = arrayOf(XProtocol.DNS4, XProtocol.DNS6, XProtocol.DNSADDR)

        fun resolve(xaddr: XAddress, resolver: Resolver=DefaultResolver): List<XAddress> {
            if (!xaddr.hasAny(*dnsProtos))
                return listOf(xaddr)

            val addrToResolve = xaddr.split { isDnsProtocol(it) }
            val resolvedAddr = mutableListOf<List<XAddress>>()
            for (xaddr in addrToResolve) {
                val toResolve = xaddr.filterStringComponents(*dnsProtos).firstOrNull()
                val resolved = if (toResolve != null)
                    resolve(toResolve.first, toResolve.second!!, xaddr, resolver)
                else
                    listOf(xaddr)
                resolvedAddr.add(resolved)
            }

            return crossProduct(resolvedAddr)
        }

        private fun resolve(proto: XProtocol, hostname: String, xaddr: XAddress, resolver: Resolver): List<XAddress> {
            try {
                return when (proto) {
                    XProtocol.DNS4 -> resolver.resolveDNS4(hostname)
                    XProtocol.DNS6 -> resolver.resolveDNS6(hostname)
                    else -> {
                        TODO("$proto not done yet")
                    }
                }
            } catch (e: UnknownHostException) {
                return emptyList()
            }
        }

        private fun crossProduct(addrMatrix: List<List<XAddress>>): List<XAddress> {
            return if (addrMatrix.size == 1)
                addrMatrix[0]
            else
                addrMatrix[0].flatMap {
                        parent -> crossProduct(addrMatrix.subList(1, addrMatrix.size))
                    .map { child -> XAddress(parent, child)
                    }
                }
        }

        private fun isDnsProtocol(proto: XProtocol): Boolean {
            return dnsProtos.contains(proto)
        }

        val DefaultResolver = object :Resolver {
            override fun resolveDNS4(hostname: String): List<XAddress> {
                return resolveDNS(hostname, XProtocol.IPv4, InetAddress::class.java)
            }

            override fun resolveDNS6(hostname: String): List<XAddress> {
                return resolveDNS(hostname, XProtocol.IPv6, Inet6Address::class.java)
            }

            private fun <T: InetAddress> resolveDNS(hostname: String, resultantProto: XProtocol, desiredAddrType: Class<T>): List<XAddress> {
                val ipAddr = InetAddress.getAllByName(hostname)
                return ipAddr.filter { desiredAddrType.isInstance(it) }
                    .map { XAddress(listOf(Pair(resultantProto, it.address))) }
            }
        }
    }
}
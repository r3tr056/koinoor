package com.gfms.xnet.xaddress

/**
import java.nio.ByteBuffer

enum class Protocols(
    val code: Int,
    val size: Int,
    val typeName: String
) {


    IPV4(4, 32, "ipv4"),
    TCP(6, 16, "tcp"),
    UDP(8, 16, "udp"),
    IPV6(10, 128, "ipv6"),
    IPV6ZONE(12, LEN_PREFIX_VAR_SIZE, "ipv6zone"),
    DNS4(14, LEN_PREFIX_VAR_SIZE, "dns4"),
    DNS6(16, LEN_PREFIX_VAR_SIZE, "dns6"),
    DNSADDR(18, LEN_PREFIX_VAR_SIZE, "dnsaddr"),
    UTP(20, 0, "utp"),
    UNIX(22, LEN_PREFIX_VAR_SIZE, "unix") {
        override fun isPath() = true
    },
    XNET(26, LEN_PREFIX_VAR_SIZE, "xnet"),
    P2P(28, LEN_PREFIX_VAR_SIZE, "p2p"),
    QUIC(30, 0, "quic"),
    WS(32, 0, "ws"),
    P2PCIRCUIT(34, 0, "p2pcircuit");

    val encoded: ByteArray = encode(code)

    private fun encode(type: Int): ByteArray {
        val array: ByteArray
        ByteBuffer.allocate(4).putInt(type).
    }
}

private val LEN_PREFIX_VAR_SIZE: Int = -1

 **/

import java.net.Inet4Address
import java.net.Inet6Address
import java.nio.ByteBuffer
import io.netty.buffer.ByteBuf
import java.nio.charset.StandardCharsets

private const val LENGTH_PREFIXED_VAR_SIZE = -1

enum class XProtocol(
    val code: Int,
    val size: Int,
    val typename: String
) {

    IPv4(code=0, 32, "ipv4"),
    IPv6(code=2, 32, "ipv6"),
    IPv6Zone(code=4, LENGTH_PREFIXED_VAR_SIZE, "ipv6zone"),
    TCP(code=8, 32, "tcp"),
    UDP(code=16, 32, "udp"),
    DNS4(code=32, LENGTH_PREFIXED_VAR_SIZE, "dns4"),
    DNS6(code=64, LENGTH_PREFIXED_VAR_SIZE, "dns6"),
    UTP(code=128, 32, "utp"),
    DNSADDR(code=256, 32, "dnsaddr"),
    UNIX(code=512, LENGTH_PREFIXED_VAR_SIZE, "unix") {
        override fun isPath() = true
    },
    XNET(code=513, LENGTH_PREFIXED_VAR_SIZE, "xnet"),
    P2P(code=514, LENGTH_PREFIXED_VAR_SIZE, "p2p"),
    QUIC(code=516, 0, "quic");

    val encoded: Byte = code.toByte()

    open fun isPath() = false

    fun addressToBytes(address: String): ByteArray = when(this) {
        IPv4 -> {
            val inetAddr = Inet4Address.getByName(address)
            if (inetAddr !is Inet4Address) {
                throw IllegalArgumentException("The address is not IPv4 address: $address")
            }
            inetAddr.address
        }

        IPv6 -> Inet6Address.getByName(address).address

        TCP, UDP -> {
            val x = Integer.parseInt(address)
            if (x > 65535) throw IllegalArgumentException("Failed to parse $this address $x > 65535")
            byteArrayOf(x.toByte())
        }

        XNET, P2P -> {
            val hashBytes = PeerId.frombase58(addr).toByteArray()
            byteArrayOf(hashBytes)
        }

        UNIX -> {
            val address1 = if (address.startsWith("/")) address.substring(1) else address
            val path = address1.toByteArray(StandardCharsets.UTF_8)
            path
        }
        else -> throw IllegalArgumentException("Unknown XAddress type: $this")
    }

    fun bytesToAddress(addressBytes: ByteArray): String {
        return when(this) {
            IPv4 -> {
                Inet4Address.getByAddress(addressBytes)
                    .toString().substring(1)
            }
            IPv6 -> {
                Inet6Address.getByAddress(addressBytes)
                    .toString().substring(1)
            }
            TCP, UDP -> addressBytes.toString()
            XNET, P2P -> {
                PeerId(addressBytes).toBase58()
            }
            IPv6Zone -> TODO()
            DNS4 -> TODO()
            DNS6 -> TODO()
            UTP -> TODO()
            DNSADDR -> TODO()
            UNIX -> TODO()
            QUIC -> TODO()
        }
    }

    fun readAddressBytes(buf: ByteBuf): ByteArray {
        val size = if (size != LENGTH_PREFIXED_VAR_SIZE) size / 8 else buf.readUvarint().toInt()
    }
}
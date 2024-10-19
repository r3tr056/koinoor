package com.gfms.xnet.utils

import com.gfms.xnet.ipv4.IPv4Address
import java.lang.IllegalArgumentException
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.nio.ByteBuffer
import kotlin.math.min

private const val HEX_CHARS = "0123456789abcdef"

fun <E> Collection<E>.random(maxSampleSize: Int): Collection<E> {
    val sampleSize = min(size, maxSampleSize)
    return shuffled().subList(0, sampleSize)
}

// https://www.baeldung.com/kotlin/byte-arrays-to-hex-strings
fun ByteArray.toHex(): String = joinToString(separator="") { eachByte -> "%02x".format(eachByte) }

/**
 * Converts a hex string to ByteArray.
 * https://gist.github.com/fabiomsr/845664a9c7e92bafb6fb0ca70d4e44fd
 */
fun String.hexToBytes(): ByteArray {
    if (length % 2 != 0) throw IllegalArgumentException("String length must be even")
    val result = ByteArray(length/2)
    for (i in 0 until length step 2) {
        val firstIndex = HEX_CHARS.indexOf(this[i].lowercaseChar())
        val secondIndex = HEX_CHARS.indexOf(this[i + 1].lowercaseChar())
        val octet = firstIndex.shl(4).or(secondIndex)
        result[i.shr(1)] = octet.toByte()
    }
    return result
}

fun getReversedHex(data: ByteArray): String {
    return data.reversedArray().toHex()
}

fun hexToByte(ch: Char): Byte {
    if (ch in '0'..'9') return (ch - '0').toByte()
    if (ch in 'A'..'F') return (ch - 'A' + 10).toByte()
    return if (ch in 'a'..'f') (ch - 'a' + 10).toByte() else -1
}

// Checks if the supplied address belongs to one of the local network interfaces
// on the machine
fun addressIsLAN(address: IPv4Address): Boolean {
    val iFaces = NetworkInterface.getNetworkInterfaces()
    for (iFace in iFaces) {
        for (iFaceAddr in iFace.interfaceAddresses) {
            if (iFaceAddr.address is Inet4Address && !iFaceAddr.address.isLoopbackAddress) {
                val iNetAddr = InetAddress.getByName(address.ip)
                return addressInSubnet(iNetAddr, iFaceAddr.address, iFaceAddr.networkPrefixLength)
            }
        }
    }
    return false
}
// Returns true of the address is within the subnet address
// Stuff the address and subnet address in a 32-bit unsigned int
// Mask both the integers with `0xffffff shl (32-20)` and then compare
fun addressInSubnet(
    address: InetAddress,
    subnetAddress: InetAddress,
    networkPrefixLength: Short
): Boolean {
    val mask = 0xFFFFFF shl (32 - networkPrefixLength)
    val iNetAddrInt = ByteBuffer.wrap(subnetAddress.address).int
    val targetAddrInt = ByteBuffer.wrap(address.address).int
    return iNetAddrInt and mask == targetAddrInt and mask
}

class ByteArrayKey(val bytes: ByteArray) {

    override fun equals(other: Any?): Boolean {
        // Note: this is the same as contentEquals.
        return this.contentEquals(other)
    }

    fun contentEquals(other: Any?): Boolean {
        return this === other || (other is ByteArrayKey && this.bytes contentEquals other.bytes)
                || (other is ByteArray && this.bytes contentEquals other)
    }

    override fun hashCode(): Int {
        return bytes.contentHashCode()
    }

    override fun toString(): String {
        return bytes.contentToString()
    }
}

package com.gfms.xnet.endpoint

import com.gfms.xnet.XCommunity
import com.gfms.xnet.endpoint.api.XEndpoint
import com.gfms.xnet.endpoint.api.XEndpointListener
import com.gfms.xnet.ipv4.IPv4Address
import com.gfms.xnet.packet.XPacket
import com.gfms.xnet.tftp.TFTPEndpoint
import com.gfms.xnet.xpeer.XPeer
import kotlinx.coroutines.*
import java.io.IOException
import java.lang.Exception
import java.lang.IllegalStateException
import java.net.*


open class UDPEndpoint(
    private val port: Int,
    private val ip: InetAddress,
    private val tftpEndpoint: TFTPEndpoint = TFTPEndpoint()
): XEndpoint<XPeer>() {

    private var socket: DatagramSocket? = null
    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    private var lanEstimationJob: Job? = null
    private var bindJob: Job? = null

    init {
        tftpEndpoint.addListener(object: XEndpointListener {
            override fun onPacket(packet: XPacket) {
                //logger.debug("Received TFTP Packet (${packet.data.size} B) from ${packet.source}")
                notifyListeners(packet)
            }

            override fun onEstimatedLanChanged(address: IPv4Address) {
                TODO("Not yet implemented")
            }
        })
    }

    override fun isOpen(): Boolean {
        return socket?.isBound == true
    }

    override fun send(peer: XPeer, data: ByteArray) {
        if (!isOpen()) throw IllegalStateException("UDP Socket is closed")
        val address = peer.address
        scope.launch {
            //logger.debug("Send packet (${data.size} B) to $address ($peer)")
            try {
                if (data.size > UDP_PAYLOAD_LIMIT) {
                    if (peer.supportsTFTP) {
                        tftpEndpoint.send(address, data)
                    } else {
                        //logger.warn{"The packet is larger than UDP_PAYLOAD_LIMIT and the peer dosent support TFTP"}
                    }
                } else {
                    send(address, data)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun send(address: IPv4Address, data: ByteArray) = scope.launch(Dispatchers.IO) {
        try {
            val datagramPack = DatagramPacket(data, data.size, address.toSocketAddress())
            socket?.send(datagramPack)
        } catch (e: Exception) {
            //logger.error("Sending DatagramPacket failed", e)
        }
    }

    override fun open() {
        val sock = getDatagramSocket()
        this.socket = sock
        tftpEndpoint.socket = sock
        tftpEndpoint.open()

        //logger.info {"Opened UDP socket on port ${sock.localPort}"}

        startLanEstimation()
        bindJob = bindSocket(sock)
    }

    private fun getDatagramSocket(): DatagramSocket {
        for (i in 0 until 100) {
            try {
                return DatagramSocket(port + i, ip)
            } catch (e: Exception) {
                // Try another port
            }
        }
        // Use any available port
        return DatagramSocket()
    }

    override fun close() {
        if (!isOpen()) throw IllegalStateException("UDP socket is already closed")

        stopLanEstimation()

        bindJob?.cancel()
        bindJob = null

        // tftpEndpoint.close()

        socket?.close()
        socket = null
    }

    open fun startLanEstimation() {
        lanEstimationJob = scope.launch {
            while (isActive) {
                estimateLan()
                delay(60_000)
            }
        }
    }

    private fun estimateLan() {
        val interfaces = NetworkInterface.getNetworkInterfaces()
        for (intf in interfaces) {
            for (intfAddr in intf.interfaceAddresses) {
                if (intfAddr.address is Inet4Address && !intfAddr.address.isLoopbackAddress) {
                    val estimatedAddress =
                        IPv4Address(intfAddr.address.hostAddress!!, getSocketPort())
                    setEstimatedLan(estimatedAddress)
                }
            }
        }
    }

    open fun stopLanEstimation() {
        lanEstimationJob?.cancel()
        lanEstimationJob = null
    }

    fun getSocketPort(): Int {
        return socket?.localPort ?: port
    }

    private fun bindSocket(socket: DatagramSocket) = scope.launch {
        try {
            val receiveData = ByteArray(UDP_PAYLOAD_LIMIT)
            while (isActive) {
                val receivePacket = DatagramPacket(receiveData, receiveData.size)
                withContext(Dispatchers.IO) {
                    socket.receive(receivePacket)
                }
                handleReceivedPacket(receivePacket)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    internal fun handleReceivedPacket(receivePacket: DatagramPacket) {
        /**logger.debug(
            "Received packet (${receivePacket.length} B) from " +
                    "${receivePacket.address.hostAddress}:${receivePacket.port}"
        )
        **/

        // Check whether prefix is IPv8 or TFTP
        when (receivePacket.data[0]) {
            XCommunity.PREFIX_XNET -> {
                val sourceAddress =
                    IPv4Address(receivePacket.address.hostAddress!!, receivePacket.port)
                val packet =
                    XPacket(sourceAddress, receivePacket.data.copyOf(receivePacket.length))
                // logger.debug("Received UDP packet (${receivePacket.length} B) from $sourceAddress")

                notifyListeners(packet)
            }
            // TFTPEndpoint.PREFIX_TFTP -> { tftpEndpoint.onPacket(receivePacket) }
            else -> {
                // logger.warn { "Invalid packet prefix" }
            }
        }
    }

    companion object {
        // 1500 - 20 (IPv4 header) - 8 (UDP header)
        const val UDP_PAYLOAD_LIMIT = 1472
    }
}
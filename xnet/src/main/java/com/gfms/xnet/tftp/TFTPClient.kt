package com.gfms.xnet.tftp

import mu.KotlinLogging
import org.apache.commons.net.tftp.*
import org.apache.commons.net.tftp.TFTPClient.DEFAULT_MAX_TIMEOUTS
import java.io.IOException
import java.io.InputStream
import java.io.InterruptedIOException
import java.net.InetAddress
import java.net.SocketException

private val logger = KotlinLogging.logger {}

class TFTPClient: TFTP() {
    companion object {
        private const val PACKET_SIZE = TFTPPacket.SEGMENT_SIZE + 4
    }
    private var _totalBytesSent = 0L
    private val _sendBuffer: ByteArray = ByteArray(PACKET_SIZE)

    fun sendFile(fileName: String, mode: Int, input: InputStream, host: InetAddress, port: Int) = synchronized(this) {
        var block = 0
        var lastAckWait = false

        _totalBytesSent = 0L
        var sent: TFTPPacket = TFTPWriteRequestPacket(host, port, fileName, mode)
        val data = TFTPDataPacket(host, port, 0, _sendBuffer, 4, 0)

        do {
            send(sent)
            var wantReply = true
            var timeouts = 0
            do {
                try {
                    logger.debug { "Waiting for receive..." }
                    val recv = receive()
                    logger.debug { "Received TFTP packet of type ${recv.type}" }
                    val recvAddress = recv.address
                    val recvPort = recv.port

                    // Comply with RFC 783 indication that an error acknowledgment
                    // should be sent to originator if unexpected TID or host.
                    if (host == recvAddress && port == recvPort) {
                        when (recv.type) {
                            TFTPPacket.ERROR -> {
                                val error = recv as TFTPErrorPacket
                                throw IOException("Error code " + error.error + " received: " + error.message)
                            }
                            TFTPPacket.ACKNOWLEDGEMENT -> {
                                val lstBlock = (recv as TFTPAckPacket).blockNumber
                                logger.warn { "ACK block : $lstBlock, expected: $block"}
                                if (lstBlock == block) {
                                    ++block
                                    if (block > 65535) {
                                        block = 0
                                    }
                                    wantReply = false
                                } else {
                                    logger.debug { "discardPackets" }
                                    discardPackets()
                                }
                            }
                            else -> throw IOException("Received unexpected packet type.")
                        }
                    } else {
                        val error = TFTPErrorPacket(recvAddress, recvPort, TFTPErrorPacket.UNKNOWN_TID, "Unexpected host or port")
                        send(error)
                    }
                } catch (e: SocketException) {
                    if (++timeouts >= DEFAULT_MAX_TIMEOUTS) {
                        throw IOException("Connection timed out")

                    }
                } catch (e: InterruptedIOException) {
                    if (++timeouts >= DEFAULT_MAX_TIMEOUTS) {
                        throw IOException("Connection timed out")
                    }
                } catch (e: TFTPPacketException) {
                    throw IOException("Bad packet: " + e.message)
                }
            } while (wantReply)
            if (lastAckWait) {
                break
            }
            var dataLen = TFTPPacket.SEGMENT_SIZE
            var offset = 4
            var totalThisPacket = 0
            var bytesRead = 0
            while (dataLen > 0 && input.read(_sendBuffer, offset, dataLen).also{bytesRead=it} > 0) {
                offset += bytesRead
                dataLen -= bytesRead
                totalThisPacket += bytesRead
            }
            if (totalThisPacket < TFTPPacket.SEGMENT_SIZE) {
                // This is out last packet -- send, wait for ack, stop
                lastAckWait = true
            }
            data.blockNumber = block
            data.setData(_sendBuffer, 4, totalThisPacket)
            sent = data
            _totalBytesSent += totalThisPacket.toLong()
        } while (true) // loops until after lastAckWait is set
        logger.debug { "sendFile finished" }
    }
}
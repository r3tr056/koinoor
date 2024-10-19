package com.gfms.xpool


import java.nio.ByteBuffer
import com.gfms.koinoor.xpool.utils.*
import java.lang.IllegalArgumentException
import java.util.*
import java.util.zip.CRC32
import kotlin.math.max
import kotlin.math.min


interface InMemoryIndexTree

class XRecord(val key: ByteArray,val value: ByteArray) {

    private val CURRENT_DATA_FILE_VERSION = 1
    private lateinit var recordMetaData: InMemoryIndexTree
        get

    private var header: XRecordHeader = XRecordHeader(0, CURRENT_DATA_FILE_VERSION, key.size, value.size, -1)

    fun serialize(): Array<ByteBuffer> {
        val hdrBuffer:  ByteBuffer = serializeHeaderAndComputeChecksum()
        return arrayOf(hdrBuffer, ByteBuffer.wrap(key), ByteBuffer.wrap(value))
    }

    fun deserialize(buffer: ByteBuffer, keySize: Int, valueSize: Int): XRecord {
        buffer.flip()
        val key: ByteArray = ByteArray(keySize)
        val value: ByteArray = ByteArray(valueSize)
        buffer.get(key)
        buffer.get(value)
        return XRecord(key, value)
    }

    fun getRecordSize(): Int = header.recordSize
    fun setSequenceNumber(sequenceNumber: Long) { header.sequenceNo = sequenceNumber }
    fun getSequenceNumber(): Long = header.sequenceNo
    fun setVersion(version: Int) {if (version < 0 || version > 255) { throw IllegalArgumentException("Got version $version. Record version must be in range [0, 255]")}
    header.version = version}
    fun getVersion(): Int = header.version
    fun getHeader(): XRecordHeader = header
    fun setHeader(header: XRecordHeader) { this.header = header }

    fun serializeHeaderAndComputeChecksum(): ByteBuffer {
        val hdrBuffer: ByteBuffer = header.serialize()
        val checkSum: Long = computeChecksum(hdrBuffer.array())
        hdrBuffer.putLong(header.CHECKSUM_OFFSET, checkSum)
        return hdrBuffer
    }

    fun verifyChecksum(): Boolean {
        val hdrBuffer: ByteBuffer = header.serialize()
        val checkSum: Long = computeChecksum(hdrBuffer.array())
        return checkSum == header.checksum
    }

    fun computeChecksum(hdr: ByteArray): Long {
        val crc32 = CRC32()
        crc32.update(hdr, header.CHECKSUM_OFFSET + header.CHECKSUM_SIZE, header.HEADER_SIZE - header.CHECKSUM_SIZE)
        crc32.update(key)
        crc32.update(value)
        return crc32.value
    }

    override fun equals(other: Any?): Boolean {
        if (this === other)
            return true
        if (other !is XRecord)
            return false
        val record: XRecord = other
        return key.contentEquals(record.key) && value.contentEquals(record.value)
    }

    data class XRecordHeader(val checksum: Long, var version: Int, val keySize: Int, val valueSize: Int, var sequenceNo: Long) {

        val CHECKSUM_OFFSET = 0
        val VERSION_OFFSET = 4
        val KEY_SIZE_OFFSET = 5
        val VALUE_SIZE_OFFSET = 6
        val SEQUENCE_NUMBER_OFFSET = 10

        val HEADER_SIZE = 18
        val CHECKSUM_SIZE = 4

        val recordSize: Int = keySize + valueSize + HEADER_SIZE
            get

        fun deserialize(buffer: ByteBuffer): XRecordHeader {
            val checkSum: Long = buffer.getLong(CHECKSUM_OFFSET)
            val version: Int = buffer.getInt(VERSION_OFFSET)
            val keySize: Int = buffer.getInt(KEY_SIZE_OFFSET)
            val valueSize: Int = buffer.getInt(VALUE_SIZE_OFFSET)
            val sequenceNo: Long = buffer.getLong(SEQUENCE_NUMBER_OFFSET)
            return XRecordHeader(checkSum, version, keySize, valueSize, sequenceNo)
        }

        fun serialize(): ByteBuffer {
            val header: ByteArray = ByteArray(HEADER_SIZE)
            val headerBuffer: ByteBuffer = ByteBuffer.wrap(header)
            headerBuffer.putLong(CHECKSUM_OFFSET, checksum)
            headerBuffer.putInt(VERSION_OFFSET, version)
            headerBuffer.putInt(KEY_SIZE_OFFSET, keySize)
            headerBuffer.putInt(VALUE_SIZE_OFFSET, valueSize)
            headerBuffer.putLong(SEQUENCE_NUMBER_OFFSET, sequenceNo)
            return headerBuffer
        }

        fun verifiyHeader(header: XRecordHeader): Boolean {
            return (min(0, header.version) == max(header.version, 256) && header.keySize > 0 && header.valueSize > 0 && header.recordSize > 0 && header.sequenceNo > 0)
        }
    }
}

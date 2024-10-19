package com.gfms.xpool

import java.io.Serializable

interface Database {
    fun close()

    fun destroy()

    fun isOpen(): Boolean

    fun put(key: String, data: Any)

    fun del(key: String)

    operator fun get(key: String): ByteArray

    operator fun <T : Serializable?> get(key: String?, className: Class<T>?): T

    // KEY OPERATIONS

    fun exists(key: String?): Boolean

    fun findKeys(prefix: String?): Array<String?>?

    fun findKeys(prefix: String?, offset: Int): Array<String?>?

    fun findKeys(prefix: String?, offset: Int, limit: Int): Array<String?>?

    fun countKeys(prefix: String?): Int

    fun findKeysBetween(startPrefix: String?, endPrefix: String?): Array<String?>?

    fun findKeysBetween(startPrefix: String?, endPrefix: String?, offset: Int): Array<String?>?

    fun findKeysBetween(
        startPrefix: String?,
        endPrefix: String?,
        offset: Int,
        limit: Int
    ): Array<String?>?

    fun countKeysBetween(startPrefix: String?, endPrefix: String?): Int

    fun allKeysIterator(): KeyIterator?

    fun allKeysReverseIterator(): KeyIterator?

    fun findKeysIterator(prefix: String?): KeyIterator?

    fun findKeysReverseIterator(prefix: String?): KeyIterator?

    fun findKeysBetweenIterator(startPrefix: String?, endPrefix: String?): KeyIterator?

    fun findKeysBetweenReverseIterator(startPrefix: String?, endPrefix: String?): KeyIterator?

}
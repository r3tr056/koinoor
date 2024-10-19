package com.gfms.xnet.exceptions

import java.lang.Exception

class MultiplexerExceptions {
    open class MultiplexerException(msg: String, cause: Exception? = null) : Exception(msg, cause)

    open class DeserializationError(msg: String, cause: Exception?=null): MultiplexerException(msg, cause)

    open class FormatError(msg: String, cause: Exception?=null): MultiplexerException(msg, cause)
}
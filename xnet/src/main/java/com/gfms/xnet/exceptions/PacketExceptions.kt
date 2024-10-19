package com.gfms.xnet.exceptions

import java.lang.Exception

class PacketExceptions {
    class PacketDecodingException(msg: String, cause: Exception? = null) : Exception(msg, cause)
}
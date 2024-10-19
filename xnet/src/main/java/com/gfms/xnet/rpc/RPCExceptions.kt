package com.gfms.xnet.rpc

import java.io.IOException

class RPCException(override val message: String, val code: Int ): IOException(message)
package com.gfms.xnet.rpc

import java.math.BigInteger

class Moshi
class XSerialize

class XNetRPCTunnel {
    fun call(function:String, params: String): String {
        return ""
    }
}
class KoiTransaction
class KoiAddress
class KoiChainID

class XNetRPC(private val tunnel: XNetRPCTunnel) {

    private val moshi = Moshi()

    private val stringResultSerializer: XSerialize = XSerialize()
    private val blockInfoSerializer: XSerialize = XSerialize()
    private val transactionSerializer: XSerialize = XSerialize()
    private val assetHistorySerializer: XSerialize = XSerialize()

    private fun stringCall(function: String, params: String=""): String? {
        return tunnel.call(function, params)
    }

    private fun getBlockByIndex(number: BigInteger) = tunnel.call("koi_getBlockByNumber",  "\"${number.toHexString()}\", true")
}
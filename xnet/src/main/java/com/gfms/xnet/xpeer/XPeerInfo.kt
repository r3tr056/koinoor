package com.gfms.xnet.xpeer

import com.gfms.xnet.xaddress.XAddress
import com.gfms.xnet.xaddress.XProtocol

data class XPeerInfo(
    var peerid: XID,
    var xaddrs: List<XAddress> = mutableListOf()
) {
    fun fromPeerXAddr(xaddr: XAddress) {
        val xacomp = xaddr.split()
        val xnet_comp = xacomp[-1]
        val last_proto_code = xnet_comp.protocols()[0].code()
        if (last_proto_code != XProtocol.XNET.code) {
            throw Exception("The last protocol should be a a XNET segment")
        }
        val peerid_str = xnet_comp.value(XProtocol.XNET.code)
        peerid = XID.fromb64(peerid_str)
        if (xnet_comp.length > 1) {
            xaddrs = mutableListOf<XAddress>(XAddress.join(xacomp[:-1]))
        }
        xaddrs = mutableListOf<XAddress>(xaddr)
    }
}
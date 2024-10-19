package com.gfms.xnet

import com.gfms.xnet.XCommunity
import com.gfms.xnet.crypto.LibNaClPK
import com.gfms.xnet.utils.hexToBytes
import com.gfms.xnet.xpeer.XPeer
import com.goterl.lazysodium.LazySodium
import com.goterl.lazysodium.LazySodiumJava
import com.goterl.lazysodium.SodiumJava

private val lazySod = LazySodiumJava(SodiumJava())

class TestCommunity: XCommunity() {
    override val serviceId: String
        get() = XPeer(LibNaClPK.fromBin(MASTER_PEER_KEY.hexToBytes(), lazySodium = lazySod)).mid

    companion object {
        private const val MASTER_PEER_KEY = "4c69624e61434c504b3ae30bbf2554d0d389964bfb3630eed1f8a216791afa48b335f04a499d6e87e14bdf53b02c329b6198312f252eddfb3119f038d71f3381092de82a83de0a0443df"
    }
}
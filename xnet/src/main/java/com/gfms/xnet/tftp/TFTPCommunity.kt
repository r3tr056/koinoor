package com.gfms.xnet.tftp

import com.gfms.xnet.Community

class TFTPCommunity: Community() {
    override val serviceId = SERVICE_ID

    companion object {
        const val SERVICE_ID = "33688436558bab6d1794fe980a2c1441d1f1df88"
    }
}
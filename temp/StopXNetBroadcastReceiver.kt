package com.gfms.xnet_android
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class StopXNetBroadcastReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val serviceIntent = Intent(context, XNetAndroid.serviceClass)
        context.stopService(serviceIntent)
    }
}
package com.gfms.xnet_android.discovery.service

// NOTICE : If background service is to be implemented, this service architecture
// needs to be changed

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import com.gfms.xnet.XNet
import com.gfms.xnet_android.R
import com.gfms.xnet_android.discovery.XNetAndroidImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlin.system.exitProcess

open class XNetAndroidService: Service(), LifecycleObserver {
    private val scope = CoroutineScope(Dispatchers.Default)
    private var isForeground = false

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        showForegroundNotification()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        getXNet().halt()
        ProcessLifecycleOwner.get()
            .lifecycle
            .removeObserver(this)
        super.onDestroy()
        // Kill the process
        exitProcess(0)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onBackground() {
        isForeground = false
        showForegroundNotification()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onForeground() {
        isForeground = true
        showForegroundNotification()
    }

    // Before delivering notification on API 26 or grater we need to register the
    // app's notification channel by using NotificationChannel and
    // NotificationManager
    private fun createNotificationChannel() {
        // Our min API is 26 so no need for check
        val importance = NotificationManager.IMPORTANCE_LOW
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_CONNECTION,
            getString(R.string.notification_channel_name),
            importance
        )
        channel.description = getString(R.string.notification_channel_connection_description)
        val notificationManager = getSystemService<NotificationManager>()
        notificationManager?.createNotificationChannel(channel)
    }

    private fun showForegroundNotification() {
        // Create a cancel intent
        val cancelBroadcastIntent = Intent(this, StopXNetReceiver::class.java)
        val cancelPendingIntent = PendingIntent.getBroadcast(
            applicationContext,
            0, cancelBroadcastIntent, 0
        )

        val builder = createNotification()
        // Allow cancellation when the app is running in background
        if (!isForeground) {
            builder.addAction(NotificationCompat.Action(0, "Stop", cancelPendingIntent))
        }
        startForeground(ONGOING_NOTIFICATION_ID, builder.build())
    }

    // Creates notification that will be shown when the XNet service is running
    protected open fun createNotification(): NotificationCompat.Builder {
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_CONNECTION)
            .setContentTitle("Xnet")
            .setContentText("Running")
    }

    private fun getXNet(): XNet {
        return XNetAndroidImpl.getInstance()
    }

    companion object {
        const val NOTIFICATION_CHANNEL_CONNECTION = "service_notifications"
        private const val ONGOING_NOTIFICATION_ID = 1
    }
}

class StopXNetReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val serviceIntent = Intent(context, XNetAndroidImpl.serviceClass)
        context.stopService(serviceIntent)
    }
}

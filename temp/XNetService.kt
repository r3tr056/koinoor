package com.gfms.xnet_android

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import com.gfms.xnet.XNet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlin.system.exitProcess

open class XNetService: Service(), LifecycleObserver {
    private val scope = CoroutineScope(Dispatchers.Default)
    private var isForeground = false

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        showForegroundNotification()

        ProcessLifecycleOwner.get()
            .lifecycle
            .addObserver(this)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        getXnet().stop()
        scope.cancel()

        ProcessLifecycleOwner.get()
            .lifecycle
            .removeObserver(this)

        super.onDestroy()
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

    private fun createNotificationChannel() {
        // Create the notificatioChannel but only on API 26+ bacause
        // the NotificationChannel class is new and not in the support library
        val importance = NotificationManager.IMPORTANCE_LOW
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_CONNECTION,
            getString(R.string.notification_channel_connection_title),
            importance
        )
        channel.description = getString(R.string.notification_channel_connection_description)
        val notificationManager = getSystemService<NotificationManager>()
        notificationManager?.createNotificationChannel(channel)
    }

    private fun showForegroundNotification() {
        val cancelBroadcastIntent = Intent(this, StopXNetBroadcastReceiver::class.java)
        val cancelPendingIntent = PendingIntent.getBroadcast(
            applicationContext,
            0, cancelBroadcastIntent, 0
        )
        val builder = createNotification()

        if (!isForeground) {
            builder.addAction(NotificationCompat.Action(0, "Stop", cancelPendingIntent))

            startForeground(
                ONGOING_NOTIFICATION_ID,
                builder.build()
            )
        }
    }

    protected open fun createNotification(): NotificationCompat.Builder {
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_CONNECTION)
            .setContentTitle("XNet")
            .setContentText("Running")
    }

    private fun getXnet(): XNet {
        return XNetAndroid.getInstance()
    }

    companion object {
        const val NOTIFICATION_CHANNEL_CONNECTION = "service_notifications"
        private const val ONGOING_NOTIFICATION_ID = 1
    }
}
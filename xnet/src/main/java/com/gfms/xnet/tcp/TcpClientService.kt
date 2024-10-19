package com.gfms.xnet.tcp

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.*
import android.os.PowerManager.WakeLock
import com.gfms.xnet.xpeer.XPeer
import java.io.DataOutputStream
import java.io.IOException
import java.net.InetAddress
import java.net.Socket

class TcpClientService: Service() {
    private lateinit var mWakeLock: WakeLock

    override fun onBind(intent: Intent?): IBinder? {
        TODO("Not yet implemented")
    }

    @SuppressLint("InvalidWakeLockTag")
    override fun onCreate() {
        super.onCreate()
        val powermanager = getSystemService(Context.POWER_SERVICE) as PowerManager
        mWakeLock = powermanager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "TcpClientService")
    }

    private fun send(port: Int,
                     data: ByteArray,
                     dest: XPeer,
                     listener: TcpClientListener,
                     listenerLoop: Looper,
                     jobId: Long) {
        val destAddress: InetAddress
        var socket: Socket? = null
        mWakeLock.acquire(30 * 60 * 1000L)

        try {
            destAddress = InetAddress.getByName(dest.hostAddress)
            socket = Socket(destAddress, port)
            val dOut = DataOutputStream(socket.getOutputStream())
            dOut.writeInt(data.size)
            dOut.write(data)
            Handler(listenerLoop).post { listener.onSendSuccess(jobId) }
        } catch (e: IOException) {
            e.printStackTrace()
            Handler(listenerLoop).post { listener.onSendFailure(jobId, e) }
        } finally {
            mWakeLock.release()
            if (socket != null) {
                try {
                    socket.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }

    inner class TcpClientListener: Binder() {
        private var mListener: TcpClientListener? = null
        private var mListenerLooper: Looper? = null
        private var mPort = TcpServerService.SERVER_PORT

        fun send(data: ByteArray, dest: XPeer, jobId: Long) {
            object: HandlerThread("TcpClientThread") {
                override fun onLooperPrepared() {
                    Handler(looper).post {
                        this@TcpClientService.send(mPort, data, dest, mListener!!, mListenerLooper!!, jobId)
                    }
                }
            }.start()
        }

        fun setListener(listener: TcpClientListener, looper: Looper) {
            mListener = listener
            mListenerLooper = looper
        }

        fun setPort(port: Int) {
            mPort = port
        }
    }
}
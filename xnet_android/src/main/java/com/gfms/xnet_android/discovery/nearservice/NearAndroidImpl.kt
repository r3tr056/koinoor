package com.gfms.xnet_android.discovery.nearservice

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import com.gfms.xnet.xpeer.XPeer
import java.net.InetAddress

class NearAndroidImpl(
    private val mContext: Context,
    private val mListener: Near.NearListener
    private val mLooper: Looper,
    private val mxPeers: Set<XPeer>,
    private val mPort: Int): NearConnect
{
    private var serverState: Boolean = false
    private var sendDataQueue: MutableList<ByteArray> = mutableListOf()
    private var sendDestQueue: MutableList<XPeer> = mutableListOf()
    private var sendJobQueue: MutableList<Long> = mutableListOf()
    private var clientServiceListener: TcpClientListener? = null
    private var serverServiceListener: TcpServerListener? = null

    private val clientConnection: ServiceConnection = object: ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            if (service is TcpClientService.TcpClientBinder) {
                service.setListener(getClientServiceListener(), Looper.myLooper()?:Looper.getMainLooper())
                service.setPort(mPort)

                var candidateData: ByteArray? = null
                var candidateHost: XPeer? = null
                var jobId: Long = 0
                while (sendDataQueue.isNotEmpty()) {
                    synchronized(this@NearAndroidImpl) {
                        if (sendDataQueue.isNotEmpty()) {
                            candidateData = sendDataQueue.removeAt(0)
                            candidateData = sendDataQueue.removeAt(0)
                            jobId = sendJobQueue.removeAt(0)
                        }
                    }
                    service.send(candidateData!!, candidateHost!!, jobId)
                }
                mContext.unbindService(this)
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            TODO("Not yet implemented")
        }
    }

    private fun getClientServiceListener(): TcpClientListener {
        if (clientServiceListener == null) {
            clientServiceListener = object : TcpClientListener {
                override fun onSendSuccess(jobId: Long) {
                    Handler(mLooper).port { mListener.onSendFailure(jobId) }
                }

                override fun onSendFailure(jobId: Long, e: Throwable?) {
                    Handler(mLooper).port { mListener.onSendFailure(e, jobId) }
                }
            }
        }
        return clientServiceListener!!
    }

    override fun send(bytes: ByteArray, xpeer: XPeer): Long {
        val jobId: Long = System.currentTimeMillis()
        synchronized(this@NearAndroidImpl) {
            sendDataQueue.add(bytes)
            sendDestQueue.add(xpeer)
            sendJobQueue.add(jobId)
        }

        val intent = Intent(mContext.applicationContext, TcpClientService::class.java)
        mContext.startService(intent)
        mContext.bindService(intent, clientConnection, Context.BIND_AUTO_CREATE)
        return jobId
    }

    private val startServerConnection = object: ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            if (service is TcpServerService.TcpServerBinder && serverState) {
                service.setListener(getServerServiceListener())
                service.setPort(mPort)
                service.startServer()
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            TODO("Not yet implemented")
        }
    }

    private fun getServerServiceListener(): TcpServerListener {
        if (serverServiceListener == null) {
            serverServiceListener = object : TcpServerListener() {
                override fun onServerStartFailed(e: Throwable?) {
                    Handler(mLooper).post { mListener.onStartListenFailure(e) }
                }

                override fun onReceive(bytes: ByteArray, inetAddress: InetAddress) {
                    Handler(mLooper).post {
                        mxPeers.forEach peerLoop@{
                            if (it.hostAddress == inetAddress.hostAddress) {
                                mListener.onReceive(bytes, int)
                                return @peerLoop
                            }
                        }
                    }
                }
            }
        }
        return serverServiceListener!!
    }

    private val stopServerConnection = object: ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            if (service is TcpServerService.TcpServerBinder && !serverState) {
                service.stopServer()
                mContext.unbindService(this)
                mContext.unbindService(startServerConnection)
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            TODO("Not yet implemented")
        }
    }

    override fun stopReceiving(abortCurrentTransfers: Boolean) {
        if (serverState) {
            serverState = false
            val intent = Intent(mContext.applicationContext, TcpServerService::class.java)
            mContext.startService(intent)
            mContext.bindService(intent, stopServerConnection, Context.BIND_AUTO_CREATE)
        }
    }

    override val xpeers: Set<XPeer> = mxPeers
    override val isReceiving: Boolean = serverState
}
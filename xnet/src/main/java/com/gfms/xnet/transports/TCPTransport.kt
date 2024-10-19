package com.gfms.xnet.transports

import io.netty.bootstrap.Bootstrap
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelOption

import java.net.InetSocketAddress
import java.time.Duration
import java.util.concurrent.CompletableFuture

import com.gfms.xnet.xaddress.XAddress
import io.netty.channel.nio.NioEventLoopGroup
import java.util.concurrent.CompletableFuture

// XTCP Transport is based on the XTransportIntrface
class XTCPTransport(): XTransport {

    private var closed = false
    var connectTimeout = Duration.ofSeconds(15)

    private val listeners = mutableMapOf<XAddress, Channel>()
    private val channels = mutableListOf<XChannel>()

    private var workerGroup by lazyVar { NioEventLoopGroup() }
    private var bossGroup by lazyVar { NioEventLoopGroup(1) }

    private var client by lazyVar {
        Bootstrap().apply {
            group(workerGroup)
            channel(SocketChannel::class.java)
            option(ChannelOption.CONNECT_TIMEOUT_MILLS, connectTimeout.toMills().toInt())
        }
    }
    
    private var server by lazyVar {
        ServerBootstrap().apply {
            group(bossGroup, workerGroup)
            channel(SocketChannel::class.java)
        }
    }

    override val activeListeners: Int
        get() = listeners.size

    override val activeConnections: Int
        get() = channels.size

    override fun listenAddresses(): List<XAddress> {
        return listeners.values.map {
            toXAddress(it.localAddrss() as InetSocketAddress)
        }
    }

    override fun canHandle(addr: XAddress): Boolean {
        TODO("Not yet implemented")
    }

    override fun initialize() {
        TODO("Not yet implemented")
    }

    override fun close(): CompletableFuture<Unit> {
        closed = true

        val unbindsCompleted = listeners
            .map{ (_, ch) -> ch }
            .map{ it.close().toVoidCompletableFuture() }

        val channelsClosed = channels
            .toMutableList()
            .map { it.close().toVoidCompletableFuture() }

        val needsToClose = unbindsCompleted.union(channelsClosed)
        val allClosed = CompletableFuture.allOf(*needsToClose.tpTypedArray())

        return allClosed.thenApply {
            workerGroup.shutdownGracefully()
            bossGroup.shutdownGracefully()
            Unit
        }
    } // close

    override fun listen(addr: XAddress, listener: XTransportListener): CompletableFuture<Unit> {
        TODO("Not yet implemented")
    }

    override fun unlisten(addr: XAddress): CompletableFuture<Unit> {
        TODO("Not yet implemented")
    }

    override fun connect(addr: XAddress, listener: XTransportListener): CompletableFuture<Unit> {
        TODO("Not yet implemented")
    }
}


package reactor

/** 
 * @author slukjanov aka Frostman
 */

import java.nio.channels.Selector
import java.util.concurrent.Executors
import java.nio.channels.ServerSocketChannel
import java.net.InetSocketAddress
import java.nio.channels.SelectableChannel
import java.nio.channels.SelectionKey
import java.nio.channels.SocketChannel
import java.util.concurrent.atomic.AtomicBoolean

class ExtSelector(val workersCount : Int, val connHandlers : ConnectionHandlersStrategy) : Thread() {
    private val selector = Selector.open().sure()
    private var work = true
    private val executor = Executors.newFixedThreadPool(workersCount).sure()

    override fun run() {
        connHandlers.init(selector)

        while (work) {
            val selectedKeys = selector.select()
            if (selectedKeys == 0) continue

            val it = selector.selectedKeys().sure().iterator().sure()
            while (it.hasNext()) {
                val sk = it.next().sure()
                it.remove()

                val obj = sk.attachment().sure()
                val attachment : ConnectionHandler = if (obj is ConnectionHandler) obj else throw IllegalStateException()

                if (sk.isAcceptable()) {
                    attachment.doAccept()
                } else if (sk.isConnectable()) {
                    attachment.doConnect()
                } else {
                    if (sk.isValid() && sk.isReadable() && attachment.remReadInterest()) {
                        executor.execute(attachment.readEvent)
                    }

                    if (sk.isValid() && sk.isWritable() && attachment.remWriteInterest()) {
                        executor.execute(attachment.writeEvent)
                    }
                }
            }
        }
    }
}

abstract class ConnectionHandlersStrategy(val host : String, val port : Int) {
    abstract fun init(val selector : Selector) : Unit

    abstract fun create(val selector : Selector) : ConnectionHandler
}

abstract class ConnectionHandler(var selectionKey : SelectionKey? = null, val selector : Selector) {
    val readInterest = AtomicBoolean(false)
    val writeInterest = AtomicBoolean(false)

    abstract val readEvent : Runnable
    abstract val writeEvent : Runnable

    abstract fun doAccept()
    abstract fun doConnect()

    fun addReadInterest() = readInterest.compareAndSet(false, true)
    fun addWriteInterest() = writeInterest.compareAndSet(false, true)
    fun remReadInterest() = readInterest.compareAndSet(true, false)
    fun remWriteInterest() = writeInterest.compareAndSet(true, false)
}

class ServerConnectionHandlersStrategy(host : String, port : Int) : ConnectionHandlersStrategy(host, port) {
    val serverChannel = ServerSocketChannel.open().sure()
    var selector : Selector? = null

    override fun init(val selector : Selector) {
        this.selector = selector
        serverChannel.socket().sure().bind(InetSocketAddress(host, port))
        serverChannel.registerChannel(selector, SelectionKey.OP_ACCEPT, create(selector))
    }

    override fun create(val selector : Selector) = ServerConnectionHandler(selector.sure(), serverChannel)
}

class ServerConnectionHandler(selector : Selector, val serverChannel : ServerSocketChannel) : ConnectionHandler(null, selector){
    private var socketChannel : SocketChannel? = null

    override fun doAccept() {
        socketChannel = serverChannel.accept().sure()
        socketChannel?.registerChannel(selector, SelectionKey.OP_READ, this)
    }

    override fun doConnect() {
        throw UnsupportedOperationException()
    }

    override val readEvent = object : Runnable {
        override fun run() {
        }
    }

    override val writeEvent = object : Runnable {
        override fun run() {
        }
    }
}

fun SelectableChannel.registerChannel(selector : Selector, selectionKeys : Int, handler : ConnectionHandler) {
    var sk : SelectionKey
    if (this.isRegistered()) {
        sk = this.keyFor(selector).sure()
        sk.interestOps(selectionKeys)
        sk.attach(handler)
    } else {
        this.configureBlocking(false)
        sk = this.register(selector, selectionKeys, handler).sure()
    }
    handler.selectionKey = sk
}

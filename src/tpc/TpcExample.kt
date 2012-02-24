package tpc

/**
 * @author slukjanov aka Frostman
 */

import java.io.PrintWriter
import java.util.concurrent.atomic.AtomicInteger
import java.net.ServerSocket
import java.net.Socket
import java.io.DataInputStream
import java.io.DataOutputStream
import java.util.concurrent.CountDownLatch
import java.net.SocketException

fun main(args : Array<String>) = TpcExample(100).demo()

public class TpcExample(val clients : Int) {
    private val nextWorkerId = AtomicInteger(1)
    private val output = PrintWriter(System.out)
    private val notProcessed = CountDownLatch(clients)

    fun demo() {
        val start = System.currentTimeMillis()
        log("Starting tpc demo with $clients clients")

        val server = TpcServer(8000)
        server.start()
        Thread.sleep(100)
        for (i in 1..clients) TpcClient(8000, i).start()

        notProcessed.await()
        server.server.close()
        log("All clients processed, demo takes about ${System.currentTimeMillis() - start}ms.")

        output.close()
    }

    public class TpcServer(val port : Int) : Thread("tpc demo server") {
        val server = ServerSocket(port, 100000)

        override fun run() {
            while (notProcessed.count > 0) {
                try {
                    val socket = server.accept().sure()
                    Worker(socket).start()
                } catch (e : SocketException) {
                    // no operations
                }
            }
        }
    }

    class Worker(val socket : Socket) : Thread("worker #${nextWorkerId.getAndIncrement()}") {
        override fun run() {
            Thread.sleep(100)
            log("new connection accepted by $name")
            val input = DataInputStream(socket.getInputStream())
            val output = DataOutputStream(socket.getOutputStream())
            output.writeInt(input.readInt() + 1) // increment received Int
            log("connection successfully processed by $name")
        }
    }

    public class TpcClient(val serverPort : Int, val cid : Int) : Thread("client #$cid") {
        override fun run() {
            log("$name connecting to localhost:$serverPort")
            val socket = Socket("127.0.0.1", serverPort)
            val input = DataInputStream(socket.getInputStream())
            val output = DataOutputStream(socket.getOutputStream())
            output.writeInt(100)
            if (input.readInt() != 101) {
                throw IllegalStateException()
            }
            socket.close()
            notProcessed.countDown()
            log("$name successfully proceed")
        }
    }

    fun log(str : String?) = output.println("$str")
}

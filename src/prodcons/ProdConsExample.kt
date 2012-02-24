package prodcons

import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger

fun main(args : Array<String>) = ProdConsExample(100, 1, 10).demo()

public class ProdConsExample(val resourcesCount : Int, val producers : Int, val consumers : Int) {
    private val resources = LinkedBlockingQueue<Resource?>()
    private val notProduced = CountDownLatch(resourcesCount)
    private val notConsumed = CountDownLatch(resourcesCount)

    public fun demo() {
        println("Starting prodcons demo with $resourcesCount resources")
        for (i in 1..consumers) Consumer(i).start()
        for (j in 1..producers) Producer(j).start()

        println("$consumers consumers and $producers producers started, working...")

        notProduced.await()
        println("All resources produced")

        notConsumed.await()
        println("All resources consumed, end of demo.")
    }

    class object {
        private val NEXT_RES_ID = AtomicInteger(1)
    }

    public class Producer(val id : Int) : Runnable {
        private val thread = Thread(this)

        public fun start() : Unit = thread.start()

        override public fun run() {
            while (notProduced.getCount() > 0) {
                val resource = Resource()
                resources.put(resource)
                resource.produced(id)
                notProduced.countDown()
            }
        }
    }

    public class Consumer(val id : Int) : Runnable {
        private val thread = Thread(this)

        public fun start() : Unit = thread.start()

        override public fun run() {
            while (notConsumed.getCount() > 0) {
                var resource = resources.poll((100).toLong(), TimeUnit.MILLISECONDS)
                if (resource == null) continue
                resource?.consumed(id)
                notConsumed.countDown()
            }
        }
    }

    public class Resource() {
        private val id : Int = NEXT_RES_ID.getAndIncrement().sure()

        public fun produced(producerId : Int) : Unit = println("Produced: #$id by #$producerId")
        public fun consumed(consumerId : Int) : Unit = println("Consumed: #$id by #$consumerId")
    }
}

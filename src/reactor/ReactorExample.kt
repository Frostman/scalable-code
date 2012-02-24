package reactor

/** 
 * @author slukjanov aka Frostman
 */

import java.nio.channels.Selector

fun main(args : Array<String>) {
    val selector = ExtSelector(4, ServerConnectionHandlersStrategy("127.0.0.1", 8000))

}

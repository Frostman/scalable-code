package reactor

/** 
 * @author slukjanov aka Frostman
 */

import java.nio.channels.Selector

fun main(args : Array<String>) {
    val selector = Selector.open().sure()
    println(selector)
}

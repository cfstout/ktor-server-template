package io.github.cfstout.ktor

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking

object CoroutineDemo {
    @JvmStatic
    fun main(args: Array<String>) {
        val threadNames = runBlocking {
            val deferreds = (0 until 100).map {
                async(Dispatchers.IO) {
                    Thread.sleep(1000)
                    Thread.currentThread().name
                }
            }

            deferreds.map {
                it.await()
            }.toSet().sorted()
        }

        println(threadNames)
    }
}

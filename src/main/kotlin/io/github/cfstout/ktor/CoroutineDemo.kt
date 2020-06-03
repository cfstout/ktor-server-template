package io.github.cfstout.ktor

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

object CoroutineDemo {

    @JvmStatic
    fun main(args: Array<String>) = runBlocking {
        println("before bad method call in main")
        val seemsAsync = notReallyAsync()
        println("after bad method call in main")
        println("our fake async result: ${seemsAsync.await()}")

        println()

        println("before good method call in main")
        val isAsync = actuallyAsync()
        println("after good method call in main")
        println("our real async result: ${isAsync.await()}")
    }

    suspend fun notReallyAsync(): Deferred<Int> = coroutineScope {
        withContext(Dispatchers.IO) {
            async {
                println("before delay in fun")
                delay(100)
                println("after delay in fun")
                42
            }
        }
    }

    fun actuallyAsync(): Deferred<Int> = GlobalScope.async(Dispatchers.IO) {
        println("before delay in fun")
        delay(100)
        println("after delay in fun")
        42
    }
}

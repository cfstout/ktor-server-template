package io.github.cfstout.ktor

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import org.slf4j.LoggerFactory
import java.lang.IllegalStateException
import java.time.Instant
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

object Main {
    private val logger = LoggerFactory.getLogger(Main::class.java)

    @JvmStatic
    fun main(args: Array<String>) {
        logger.info("STARTING DEMO")
        val start = Instant.now()
//        val ifWeDontJoinNothingHappens: CompletableFuture<Int> = ifWeDontJoinNothingHappens()
//        logger.info(ifWeDontJoinNothingHappens.toString())
//        butIfWeJoinWeSeeResults()
//        val theFuture = weCanMapTheReturnValue()
//        logger.info("We have a reference to the future in the main thread")
//        logger.info("Output: {}", theFuture.join())
        logger.info("joined values = {}", whatIfYouMapToAnotherMethod().join())
        logger.info("Process took {}ms", Instant.now().toEpochMilli() - start.toEpochMilli())

    }

    fun ifWeDontJoinNothingHappens(): CompletableFuture<Int> {
        return computeFirst()
    }

    fun butIfWeJoinWeSeeResults(): Int {
        return computeFirst().join()
    }

    fun weCanMapTheReturnValue(): CompletableFuture<Int> {
        return computeFirst()
            .thenApply {
                logger.info("Now let's add 4 to our return value $it")
                it + 4
            }
    }

    fun whatIfYouMapToAnotherMethod(): CompletableFuture<Int> {
        val computeThirdFromFirst: CompletableFuture<Int> = computeFirst()
            .handle { result, error ->
                if (error != null) {
                    logger.error("ERROR", error)
                    throw error
                } else {
                    result
                }
            }
            .thenCompose { computeThird(it) }


        val computeSecond = computeSecond()

        return CompletableFuture.supplyAsync {
            computeThirdFromFirst.join() + computeSecond.join()
        }
    }

    private fun computeFirst(): CompletableFuture<Int> =
        CompletableFuture.supplyAsync {
            logger.info("START: Computing first value")
            Thread.sleep(200)
            logger.info("DONE: Computed first value")
//            throw IllegalStateException("Boom")
            1
        }

    private fun computeSecond(): CompletableFuture<Int> =
        CompletableFuture.supplyAsync {
            logger.info("START: Computing second value")
            Thread.sleep(200)
            logger.info("DONE: Computed second value")
            2
        }

    private fun computeThird(value: Int): CompletableFuture<Int> =
        CompletableFuture.supplyAsync {
            logger.info("START: Computing third value")
            Thread.sleep(250)
            logger.info("DONE: Computed third value")
            value + 5
        }

    private suspend fun coroutineComputeFirstAsync(): Deferred<Int> = coroutineScope {
        async {
            logger.info("START: Coroutine computing first value")
            delay(200)
            logger.info("DONE: Coroutine computed first value")
            3
        }
    }

    private suspend fun coroutineComputeSecondAsync(): Deferred<Int> = coroutineScope {
        async {
            logger.info("START: Coroutine computing second value")
            delay(200)
            logger.info("DONE: Coroutine computed second value")
            3
        }
    }
}

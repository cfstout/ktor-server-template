package io.github.cfstout.ktor

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.natpryce.konfig.Configuration
import com.natpryce.konfig.ConfigurationProperties
import com.natpryce.konfig.EnvironmentVariables
import com.natpryce.konfig.Key
import com.natpryce.konfig.intType
import com.natpryce.konfig.overriding
import com.zaxxer.hikari.HikariDataSource
import io.github.cfstout.ktor.config.fromDirectory
import io.github.cfstout.ktor.endpoints.Greeting
import io.github.cfstout.ktor.endpoints.GreetingEndpoints
import io.github.cfstout.ktor.hikari.buildHikariConfig
import io.ktor.application.call
import io.ktor.application.feature
import io.ktor.application.install
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.features.StatusPages
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.jackson.JacksonConverter
import io.ktor.response.respond
import io.ktor.routing.HttpMethodRouteSelector
import io.ktor.routing.Route
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import org.slf4j.LoggerFactory
import org.slf4j.event.Level
import java.nio.file.Path
import java.time.Duration
import java.time.Instant
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory

object Server {
    private val logger = LoggerFactory.getLogger(Server::class.java)
    private val configuredObjectMapper by lazy {
        ObjectMapper().apply {
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false)
            configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            registerModule(KotlinModule())
        }
    }

    @JvmStatic
    fun main(args: Array<String>) {
        val start = Instant.now()
        val warmupPool = Executors.newCachedThreadPool(DaemonThreadFactory)
        val jacksonFuture = warmupPool.submit(Callable { configuredObjectMapper })
        val configDir = Path.of(args.getOrNull(0)
            ?: throw IllegalArgumentException("First argument must be config dir"))

        val config = EnvironmentVariables() overriding ConfigurationProperties.fromDirectory(configDir)
        val hikariFuture = warmupPool.submit(Callable { HikariDataSource(buildHikariConfig(config, "DB_")) })
        logger.info("Starting up server")
        val server = embeddedServer(Netty, port = HttpServerConfig(config).port) {
            install(CallLogging) {
                level = Level.INFO
            }
            install(StatusPages) {
                exception<Throwable> {
                    logger.error("Unhandled exception", it)
                    call.respond(HttpStatusCode.InternalServerError)
                }
                exception<JsonProcessingException> { t->
                    logger.warn("Bad request json", t)
                    call.respond(HttpStatusCode.BadRequest, "Invalid JSON")
                }
            }
            install(ContentNegotiation) {
                register(ContentType.Application.Json, JacksonConverter(jacksonFuture.get()))
            }

            GreetingEndpoints(this, hikariFuture.get())

            val root = feature(Routing)
            val allRoutes = allRoutes(root)
            val allRoutesWithMethod = allRoutes.filter { it.selector is HttpMethodRouteSelector }
            allRoutesWithMethod.forEach {
                logger.info("route: $it")
            }
            logger.info("Startup time: ${Duration.between(start, Instant.now()).toMillis()}ms")
        }
        warmupPool.shutdown()
        server.start(wait = true)

    }

    private fun allRoutes(root: Route): List<Route> {
        return listOf(root) + root.children.flatMap { allRoutes(it) }
    }
}

class HttpServerConfig(config: Configuration) {
    val port: Int = config[Key("HTTP_LISTEN_PORT", intType)]
}

object DaemonThreadFactory: ThreadFactory {
    private val delegate = Executors.defaultThreadFactory()

    override fun newThread(r: Runnable): Thread =
        delegate.newThread(r).apply {
            isDaemon = true
        }
}

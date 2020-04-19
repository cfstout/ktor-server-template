package io.github.cfstout.ktor

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
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

object Server {
    private val logger = LoggerFactory.getLogger(Server::class.java)
    private val configuredObjectMapper = ObjectMapper().apply {
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false)
        configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        registerModule(KotlinModule())
    }

    @JvmStatic
    fun main(args: Array<String>) {
        logger.info("Starting up server")
        val server = embeddedServer(Netty, port = 8080) {
            install(CallLogging) {
                level = Level.INFO
            }
            install(ContentNegotiation) {
                register(ContentType.Application.Json, JacksonConverter(configuredObjectMapper))
            }
            install(StatusPages) {
                exception<Throwable> {
                    logger.error("Internal server error", it)
                    call.respond(HttpStatusCode.InternalServerError)
                }
            }
            routing {
                get("/hello") {
                    call.respond(Greeting("Hello World"))
                }
            }
            val root = feature(Routing)
            val allRoutes = allRoutes(root)
            val allRoutesWithMethod = allRoutes.filter { it.selector is HttpMethodRouteSelector }
            allRoutesWithMethod.forEach {
                logger.info("route: $it")
            }

        }
        server.start(wait = true)
    }

    private fun allRoutes(root: Route): List<Route> {
        return listOf(root) + root.children.flatMap { allRoutes(it) }
    }
}

data class Greeting(@JsonProperty("greeting") val greeting: String)
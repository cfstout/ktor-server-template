package io.github.cfstout.ktor.endpoints

import com.fasterxml.jackson.annotation.JsonProperty
import io.github.cfstout.ktor.dao.Color
import io.github.cfstout.ktor.dao.DaoFactory
import io.github.cfstout.ktor.dao.UserId
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.routing
import java.lang.IllegalArgumentException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jooq.Configuration
import org.jooq.DSLContext

class GreetingEndpoints(app: Application, jooq: DSLContext, private val daoFactory: DaoFactory) {
    init {
        app.routing {
            get("/hello") {
                call.respond(Greeting("Hello", "World"))
            }
            post("/greet") {
                val req: Greeting = call.receive()
                call.respond("${req.message} ${req.subject}")
            }

//            get("/select") {
//                val int = withContext(Dispatchers.IO) {
//                    datasource.connection.use { conn ->
//                        conn.prepareStatement("SELECT 1").use { stmt ->
//                            stmt.executeQuery().use { rs ->
//                                rs.next()
//                                rs.getInt(1)
//                            }
//                        }
//                    }
//                }
//                call.respond("result: $int")
//            }

            get("/favorite-color/user/id/{userId}") {
                val userId = UserId(
                    call.parameters["userId"]?.toInt()
                        ?: throw IllegalArgumentException("No userId found ${call.parameters}")
                )
                val color = jooq.txnWithDao(daoFactory::demoDao) { it.get(userId) }
                call.respond(GetFavoriteColorResponse(color?.name))
            }
            post("/favorite-color/user/id/{userId}") {
                val userId = UserId(
                    call.parameters["userId"]?.toInt()
                        ?: throw IllegalArgumentException("No userId found ${call.parameters}")
                )
                val req: SetFavoriteColorRequest = call.receive()
                val color = jooq.txnWithDao(daoFactory::demoDao) { it.set(userId, Color(req.colorName)) }

                call.respond(HttpStatusCode.OK)
            }
            get("/favorite-color/counts") {
                val colorCounts = jooq.txnWithDao(daoFactory::demoDao) { it.getColorsByCount() }
                val resp = colorCounts.entries
                    .sortedByDescending { it.value }
                    .map { ColorCount(it.key.name, it.value) }
                    .let(::FavoriteColorCountsResponse)

                call.respond(resp)
            }
        }
    }

    private fun dao(c: Configuration) = daoFactory.demoDao(c.dsl())
}

suspend fun <T, D> DSLContext.txnWithDao(daoMaker: (DSLContext) -> D, block: (D) -> T): T {
    return withContext(Dispatchers.IO) {
        transactionResult { c ->
            val dao = daoMaker(c.dsl())
            block(dao)
        }
    }
}

data class Greeting(
    @JsonProperty("message") val message: String,
    @JsonProperty("subject") val subject: String
)

data class GetFavoriteColorResponse(@JsonProperty("colorName") val colorName: String?)

data class SetFavoriteColorRequest(@JsonProperty("colorName") val colorName: String)

data class FavoriteColorCountsResponse(@JsonProperty("colorCounts") val counts: List<ColorCount>)

data class ColorCount(
    @JsonProperty("colorName") val colorName: String,
    @JsonProperty("count") val count: Int
)

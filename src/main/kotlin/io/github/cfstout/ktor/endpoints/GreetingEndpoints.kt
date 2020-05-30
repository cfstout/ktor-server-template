package io.github.cfstout.ktor.endpoints

import com.fasterxml.jackson.annotation.JsonProperty
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.routing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.sql.DataSource

class GreetingEndpoints(app: Application, datasource: DataSource) {
    init {
        app.routing {
            get("/hello") {
                call.respond(Greeting("Hello", "World"))
            }
            post("/greet") {
                val req: Greeting = call.receive()
                call.respond("${req.message} ${req.subject}")
            }

            get("/select") {
                val int = withContext(Dispatchers.IO) {
                    datasource.connection.use { conn ->
                        conn.prepareStatement("SELECT 1").use { stmt ->
                            stmt.executeQuery().use { rs ->
                                rs.next()
                                rs.getInt(1)
                            }
                        }
                    }
                }
                call.respond("result: $int")
            }
        }
    }
}

data class Greeting(
    @JsonProperty("message") val message: String,
    @JsonProperty("subject") val subject: String
)

package io.github.cfstout.ktor.endpoints

import io.ktor.server.testing.withTestApplication
import org.junit.jupiter.api.Test

internal class GreetingEndpointsTest {
    @Test
    internal fun getWithNoDataHasNullColor() {
        withTestApplication {
        }
    }
}

package com.mylosoftworks.kotllms.shared

import com.mylosoftworks.kotllms.jsonSettings
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.sse.*
import io.ktor.serialization.kotlinx.json.*

expect fun createKtorClient(): HttpClient

fun HttpClientConfig<*>.defaultConfigure() {
//    expectSuccess = true

    HttpResponseValidator {
        validateResponse {response ->
            if (response.status.value >= 300) {
                throw ClientRequestException(response, "Failed")
            }
        }
    }

    install(ContentNegotiation) {
        json(jsonSettings)
    }
    install(SSE)
}
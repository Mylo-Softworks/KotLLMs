package com.mylosoftworks.kotllms.shared

import com.mylosoftworks.kotllms.jsonSettings
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.sse.*
import io.ktor.serialization.kotlinx.json.*

actual fun createKtorClient(): HttpClient = HttpClient(CIO) {
    install(ContentNegotiation) {
        json(jsonSettings)
    }
    install(SSE)

    engine {
        maxConnectionsCount = 4
        requestTimeout = 30000
    }
}
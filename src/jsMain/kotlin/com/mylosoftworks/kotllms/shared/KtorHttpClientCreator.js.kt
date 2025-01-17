package com.mylosoftworks.kotllms.shared

import com.mylosoftworks.kotllms.jsonSettings
import io.ktor.client.*
import io.ktor.client.engine.js.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.sse.*
import io.ktor.serialization.kotlinx.json.*

actual fun createKtorClient(): HttpClient = HttpClient(Js) {
    install(ContentNegotiation) {
        json(jsonSettings)
    }
    install(SSE)

    engine {

    }
}
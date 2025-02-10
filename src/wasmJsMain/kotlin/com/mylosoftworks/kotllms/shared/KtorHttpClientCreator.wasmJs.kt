package com.mylosoftworks.kotllms.shared

import io.ktor.client.*
import io.ktor.client.engine.js.*

actual fun createKtorClient(): HttpClient = HttpClient(Js) {
    defaultConfigure()

    engine {

    }
}
package com.mylosoftworks.kotllms.shared

import io.ktor.client.*
import io.ktor.client.engine.android.*

actual fun createKtorClient(): HttpClient = HttpClient(Android) {
    defaultConfigure()

    engine {


        // CIO
//        maxConnectionsCount = 4
//        requestTimeout = 30000
    }
}
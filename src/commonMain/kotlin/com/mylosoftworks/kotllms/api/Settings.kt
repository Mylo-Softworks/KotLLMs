package com.mylosoftworks.kotllms.api

import io.ktor.client.request.*

/**
 * Base class used for settings per API
 */
open class Settings {
    open fun applyToRequest(builder: HttpRequestBuilder) {}
}
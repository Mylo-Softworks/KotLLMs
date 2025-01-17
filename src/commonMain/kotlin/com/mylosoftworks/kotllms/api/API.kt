package com.mylosoftworks.kotllms.api

/**
 * API base implementation
 * @param S The settings type to use, settings are things like an API key, stored in the API user.
 * @param F The flags type to use, flags are things like generation parameters such as temperature, sent with the generation request.
 */
abstract class API<S: Settings, F : Flags<*>>(var settings: S) {
    /**
     * Check if the API is available
     * @return Whether the API is functional
     */
    abstract suspend fun check(): Boolean

    abstract fun createFlags(): F
}
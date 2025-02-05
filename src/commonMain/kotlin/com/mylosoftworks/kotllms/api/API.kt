package com.mylosoftworks.kotllms.api

import com.mylosoftworks.kotllms.Union
import com.mylosoftworks.kotllms.features.Flags
import com.mylosoftworks.kotllms.features.Wrapper
import com.mylosoftworks.kotllms.toUnion2
import kotlin.reflect.KClass

/**
 * API base implementation
 * @param S The settings type to use, settings are things like an API key, stored in the API user.
 * @param F The flags type to use, flags are things like generation parameters such as temperature, sent with the generation request.
 */
abstract class API<S: Settings, F : Flags<*>>(var settings: S): Wrapper<API<S, F>> {
    /**
     * Check if the API is available
     * @return Whether the API is functional
     */
    abstract suspend fun check(): Boolean

    abstract fun createFlags(): F

    // Wrapper implementations
    override fun getLinked(): Union<API<S, F>, Wrapper<API<S, F>>> = this.toUnion2()
    override fun targetClass(): KClass<*> = this::class
}
package com.mylosoftworks.kotllms.features.impl

import com.mylosoftworks.kotllms.api.GenerationResult
import com.mylosoftworks.kotllms.features.Flags

/**
 * Adds a function [internalGen] to the class, useful for when an api returns in one format, but has multiple ways to invoke it.
 *
 * @sample [com.mylosoftworks.kotllms.api.impl.OpenAI.rawGen]
 * @sample [com.mylosoftworks.kotllms.api.impl.OpenAI.chatGen]
 */
interface FlagGenRequest<F: Flags> {
    suspend fun internalGen(url: String, flags: F): Result<GenerationResult>
}
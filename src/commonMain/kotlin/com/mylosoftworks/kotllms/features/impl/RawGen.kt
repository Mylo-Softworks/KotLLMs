package com.mylosoftworks.kotllms.features.impl

import com.mylosoftworks.kotllms.api.GenerationResult
import com.mylosoftworks.kotllms.features.Flags

/**
 * Allows a raw generation to be made (text -> text)
 */
interface RawGen<F: Flags> {
    suspend fun rawGen(prompt: String, flags: F? = null): Result<GenerationResult>
}
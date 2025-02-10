package com.mylosoftworks.kotllms.features.impl

interface ContextLength {
    suspend fun contextLength(model: String? = null): Result<Int>
}
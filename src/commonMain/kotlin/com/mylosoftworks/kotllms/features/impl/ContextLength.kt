package com.mylosoftworks.kotllms.features.impl

interface ContextLength {
    suspend fun contextLength(): Result<Int>
}
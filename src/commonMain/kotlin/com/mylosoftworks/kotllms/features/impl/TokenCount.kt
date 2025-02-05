package com.mylosoftworks.kotllms.features.impl

import com.mylosoftworks.kotllms.features.Flags

interface TokenCount<F : Flags<*>> {
    suspend fun tokenCount(string: String, flags: F? = null): Int
}
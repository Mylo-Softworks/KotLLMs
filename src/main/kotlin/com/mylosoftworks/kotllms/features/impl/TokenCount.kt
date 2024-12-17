package com.mylosoftworks.kotllms.features.impl

import com.mylosoftworks.kotllms.features.Feature
import com.mylosoftworks.kotllms.api.Flags

interface TokenCount<F : Flags<*>> : Feature {
    suspend fun tokenCount(string: String, flags: F? = null): Int
}
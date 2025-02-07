package com.mylosoftworks.kotllms.features.impl

import com.mylosoftworks.kotllms.features.Flags

interface TokenCount<F : Flags<*>, T: TokenCountDef> {
    suspend fun tokenCount(string: String, flags: F? = null): Result<T>
}

open class TokenCountDef(val count: Int)
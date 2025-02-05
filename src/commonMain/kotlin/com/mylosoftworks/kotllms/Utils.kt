package com.mylosoftworks.kotllms

import kotlinx.serialization.json.Json

fun stripTrailingSlash(string: String) = string.removeSuffix("/")

val jsonSettings by lazy { Json {
    ignoreUnknownKeys = true
} }

inline fun <reified T> Any.runIfImpl(block: (T).() -> Unit) {
    if (this is T) block(this)
}

inline fun <reified T> Any.tryCast() = this as? T

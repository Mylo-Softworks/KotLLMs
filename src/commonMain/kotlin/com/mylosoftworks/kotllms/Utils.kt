package com.mylosoftworks.kotllms

import kotlinx.serialization.json.Json

fun stripTrailingSlash(string: String) = string.removeSuffix("/")
fun <T : Any> T?.safeStructurize(alternative: T): T = this ?: alternative

val jsonSettings by lazy { Json {
    ignoreUnknownKeys = true
} }
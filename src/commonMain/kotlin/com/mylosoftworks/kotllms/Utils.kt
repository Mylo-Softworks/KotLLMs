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

// Union type
typealias Union<A, B> = Pair<A?, B?>
fun Union<*, *>.nonNull() = first ?: second ?: error("Both fields were null")
fun <T, O> T.toUnion1() = Union<T, O>(this, null)
fun <T, O> O.toUnion2() = Union<T, O>(null, this)

inline fun <T> wrapTryCatchToResult(block: () -> T): Result<T> {
    try {
        return Result.success(block())
    }
    catch (e: Exception) {
        return Result.failure(e)
    }
}

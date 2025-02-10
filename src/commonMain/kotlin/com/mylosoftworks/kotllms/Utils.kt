package com.mylosoftworks.kotllms

import kotlinx.serialization.json.Json

fun stripTrailingSlash(string: String) = string.removeSuffix("/")

val jsonSettings by lazy { Json {
    ignoreUnknownKeys = true
} }

/**
 * Run the block if `this` implements [T], and return the return value, or `null` if it doesn't implement [T]
 */
inline fun <reified T, R> Any.runIfImpl(block: T.() -> R): R? {
    if (this is T) return block(this)
    return null
}

/**
 * Run the block if `this` implements [T], no return value
 */
inline fun <reified T> Any.runIfImpl(block: T.() -> Unit) = runIfImpl<T, Unit>(block)

inline fun <reified T> Any.tryCast() = this as? T

// Union type
typealias Union<A, B> = Pair<A?, B?>
fun Union<*, *>.nonNull() = first ?: second ?: error("Both fields were null")
fun <T, O> T.toUnion1() = Union<T, O>(this, null)
fun <T, O> O.toUnion2() = Union<T, O>(null, this)

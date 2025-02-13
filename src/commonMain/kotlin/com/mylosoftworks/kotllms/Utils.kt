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

inline fun <reified T> Any.runIfImplR(block: T.() -> RunResult): Pair<RunResult, Any> {
    if (this is T) return block(this) to this
    return RunResult.Failed to this
}

enum class RunResult {
    Success, Failed
}

/**
 * Run the block if `this` implements [T], no return value
 */
inline fun <reified T> Any.runIfImpl(block: T.() -> Unit): Pair<RunResult, Any> {
    if (this is T) {
        block(this)
        RunResult.Success to this
    }
    return RunResult.Failed to this
}

inline fun <reified T> Pair<RunResult, Any>.elseIfImplRun(block: T.() -> Unit): Pair<RunResult, Any> {
    if (this.first == RunResult.Success) return RunResult.Success to this
    return this.second.runIfImpl<T>(block)
}

inline fun Pair<RunResult, Any>.elseRun(block: () -> Unit) {
    if (this.first == RunResult.Failed) block()
}


inline fun <reified T> Any.tryCast() = this as? T

// Union type
typealias Union<A, B> = Pair<A?, B?>
fun Union<*, *>.nonNull() = first ?: second ?: error("Both fields were null")
fun <T, O> T.toUnion1() = Union<T, O>(this, null)
fun <T, O> O.toUnion2() = Union<T, O>(null, this)

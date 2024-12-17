package com.mylosoftworks.kotllms.api

/**
 * The base type for generation results. Allows for things like streaming.
 * @param streaming Whether the result is being streamed or not.
 * @param canCancel Whether the result can be cancelled or not.
 */
abstract class GenerationResult(val streaming: Boolean) {
    abstract fun getText(): String
}

interface Cancellable {
    suspend fun cancel()
}

abstract class StreamedGenerationResult<C> : GenerationResult(true) {
    abstract fun registerStreamer(block: (C) -> Unit)
    abstract fun isComplete(): Boolean
}
package com.mylosoftworks.kotllms.api

/**
 * The base type for generation results. Allows for things like streaming.
 * @param streaming Whether the result is being streamed or not.
 */
abstract class GenerationResult(val streaming: Boolean) {
    abstract fun getText(): String
}

interface Cancellable {
    suspend fun cancel()
}

interface SeparateToolCalls {
    fun getToolCallsString(): String?
}

abstract class StreamChunk {
    abstract fun getTokenF(): String
    abstract fun isLastToken(): Boolean
}

abstract class StreamedGenerationResult<C: StreamChunk> : GenerationResult(true) {
    var currentContent = ""
    abstract val currentContentAsChunk: C

    val streamers = arrayListOf<(Result<C>) -> Unit>()

    var error: Throwable? = null

    open fun registerStreamer(block: (Result<C>) -> Unit) {
        block(error?.let { Result.failure(error!!) } ?: Result.success(currentContentAsChunk))
        streamers.add(block)
    }
    abstract fun isComplete(): Boolean
    open fun update(chunk: C) {
        if (error != null) return
        currentContent += chunk.getTokenF()
        streamers.forEach { it.invoke(Result.success(chunk)) }
    }

    open fun criticalError(error: Throwable) {
        this.error = error
        streamers.forEach { it.invoke(Result.failure(error)) }
    }

    override fun getText(): String {
        return currentContent
    }
}
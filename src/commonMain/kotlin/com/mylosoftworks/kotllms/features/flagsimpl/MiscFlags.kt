package com.mylosoftworks.kotllms.features.flagsimpl

// Flags which currently don't belong in another category

interface FlagQuiet {
    var quiet: Boolean?
}

/**
 * Marks that this request should be streamed.
 *
 * Make sure to check the result from the function call, if streamed, it'll be a StreamedGenerationResult<C: StreamChunk>
 */
interface FlagStream {
    var stream: Boolean?
}
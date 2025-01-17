package com.mylosoftworks.kotllms.shared

import kotlinx.serialization.Serializable

@Serializable
data class AttachedImage(var base64: String) {
    companion object // For expecting extensions
}

expect suspend fun AttachedImage.Companion.fromUrl(url: String): AttachedImage
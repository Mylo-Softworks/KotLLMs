package com.mylosoftworks.kotllms.shared

import com.mylosoftworks.kotllms.features.ToJson
import com.mylosoftworks.kotllms.features.toJson
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class AttachedImage(var base64: String): ToJson {
    companion object; // For expecting extensions

    override fun toJson(): JsonElement = base64.toJson()
}

expect suspend fun AttachedImage.Companion.fromUrl(url: String): AttachedImage
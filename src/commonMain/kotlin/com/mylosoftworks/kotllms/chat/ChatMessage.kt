package com.mylosoftworks.kotllms.chat

import com.mylosoftworks.kotllms.chat.features.ChatFeatureContent
import com.mylosoftworks.kotllms.chat.features.ChatFeatureImages
import com.mylosoftworks.kotllms.chat.features.ChatFeatureRole
import com.mylosoftworks.kotllms.features.*
import com.mylosoftworks.kotllms.features.impl.ChatGen
import com.mylosoftworks.kotllms.shared.AttachedImage
import kotlinx.serialization.json.JsonElement

/**
 * Similar to generation flags, each message can have it's own flags. The flags can be accessed for formatting and templating.
 */
open class ChatMessage: Flaggable<Any>, ChatFeatureContent, ChatFeatureRole, ToJson {
    override val setFlags = hashMapOf<String, Any>()

    override var role: ChatGen.ChatRole? by mappedFlag<ChatGen.ChatRole, Any>(mapTo = { this.genericName }, mapFrom = { ChatGen.ChatRole.valueOf(toString()) }) // Store as string
    override var content by flag<String>()

    operator fun get(name: String) = setFlags[name]
    operator fun set(name: String, value: Any) = setFlags.set(name, value)

    override fun toJson(): JsonElement {
        return setFlags.toJson()
    }
}

/**
 * A chat message meant to be used with ChatTemplate, to be turned into a prompt string.
 */
open class ChatMessageTemplated : ChatMessage(), ChatFeatureImages { // Open class, since you can add extra message variables
    override var images = listOf<AttachedImage>()

    override var role by flag<ChatGen.ChatRole>() // Store as actual ChatRole
}
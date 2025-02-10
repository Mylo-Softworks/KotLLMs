package com.mylosoftworks.kotllms.chat

import com.mylosoftworks.kotllms.chat.features.ChatFeatureContent
import com.mylosoftworks.kotllms.chat.features.ChatFeatureImages
import com.mylosoftworks.kotllms.chat.features.ChatFeatureRole
import com.mylosoftworks.kotllms.features.Flaggable
import com.mylosoftworks.kotllms.features.flag
import com.mylosoftworks.kotllms.shared.AttachedImage

/**
 * Similar to generation flags, each message can have it's own flags. The flags can be accessed for formatting and templating.
 */
open class ChatMessage: Flaggable<Any>, ChatFeatureContent, ChatFeatureRole {
    override val setFlags = hashMapOf<String, Any>()

    override var role by flag<String>()
    override var content by flag<String>()

    operator fun get(name: String) = setFlags[name]
    operator fun set(name: String, value: Any) = setFlags.set(name, value)
}

/**
 * A chat message meant to be used with ChatTemplate, to be turned into a prompt string.
 */
open class ChatMessageTemplated : ChatMessage(), ChatFeatureImages { // Open class, since you can add extra message variables
    override var images = listOf<AttachedImage>()
}
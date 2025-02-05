package com.mylosoftworks.kotllms.chat

import com.mylosoftworks.kotllms.shared.AttachedImage
import kotlin.reflect.KProperty

/**
 * Similar to generation flags, each message can have it's own flags. The flags can be accessed for formatting and templating.
 */
abstract class ChatMessage {
    val setFlags = hashMapOf<String, Any>()

    abstract var role: String?
    abstract var content: String?

    operator fun get(name: String) = setFlags[name]

    open fun getAttachedImages(): List<AttachedImage> {
        return listOf()
    }
}

/**
 * A chat message meant to be used with ChatTemplate, to be turned into a prompt string.
 */
open class ChatMessageWithImages : ChatMessage() { // Open class, since you can add extra message variables
    override var role by MessageFlag<String>()
    override var content by MessageFlag<String>()

    var images = listOf<AttachedImage>()

    override fun getAttachedImages(): List<AttachedImage> = images
}

class MessageFlag<T> {
    operator fun <U : ChatMessage> getValue(thisRef: U, property: KProperty<*>): T? {
        @Suppress("UNCHECKED_CAST")
        return thisRef.setFlags.getOrElse(property.name) { null } as T?
    }

    operator fun <U : ChatMessage> setValue(thisRef: U, property: KProperty<*>, value: T?) {
        if (value == null) {
            thisRef.setFlags.remove(property.name)
            return
        }
        thisRef.setFlags[property.name] = value
    }
}

class MessageOneWayConvertedFlag<T, V>(val convert: (T) -> V) {
    operator fun <U : ChatMessage> getValue(thisRef: U, property: KProperty<*>): Any? {
        @Suppress("UNCHECKED_CAST")
        return thisRef.setFlags.getOrElse(property.name) { null } as V?
    }

    operator fun <U : ChatMessage> setValue(thisRef: U, property: KProperty<*>, value: Any?) {
        if (value == null) {
            thisRef.setFlags.remove(property.name)
            return
        }
        thisRef.setFlags[property.name] = convert(value as T)!!
    }
}

class MessageTwoWayConvertedFlag<T, V>(val convertTo: (T) -> V, val convertFrom: (V) -> T) {
    operator fun <U : ChatMessage> getValue(thisRef: U, property: KProperty<*>): Any? {
        @Suppress("UNCHECKED_CAST")
        return (thisRef.setFlags.getOrElse(property.name) { null } as V?)?.let { convertFrom(it) }
    }

    operator fun <U : ChatMessage> setValue(thisRef: U, property: KProperty<*>, value: Any?) {
        if (value == null) {
            thisRef.setFlags.remove(property.name)
            return
        }
        thisRef.setFlags[property.name] = convertTo(value as T)!!
    }
}
package com.mylosoftworks.kotllms.chat

import java.awt.image.BufferedImage
import kotlin.reflect.KProperty

/**
 * Similar to generation flags, each message can have it's own flags. The flags can be accessed for formatting and templating.
 */
abstract class ChatMessage<T : ChatMessage<T>> {
    val setFlags = hashMapOf<String, Any>()

    operator fun get(name: String) = setFlags[name]

    abstract fun setTextContent(content: String) // Text content implementation, usually involves setting a flag

    open fun getAttachedImages(): List<BufferedImage> {
        return listOf()
    }

    inline fun init(init: T.() -> Unit): T {
        init(this as T)
        return this
    }
}

/**
 * A chat message meant to be used with ChatTemplate, to be turned into a prompt string.
 */
open class BasicChatMessage : ChatMessage<BasicChatMessage>() { // Open class, since you can add extra message variables
    var role by MessageFlag<String>()
    var content by MessageFlag<String>()

    var images = listOf<BufferedImage>()

    override fun getAttachedImages(): List<BufferedImage> = images

    override fun setTextContent(content: String) {
        this.content = content
    }
}

class MessageFlag<T> {
    operator fun <U : ChatMessage<U>> getValue(thisRef: U, property: KProperty<*>): T? {
        @Suppress("UNCHECKED_CAST")
        return thisRef.setFlags.getOrDefault(property.name, null) as T?
    }

    operator fun <U : ChatMessage<U>> setValue(thisRef: U, property: KProperty<*>, value: T?) {
        if (value == null) {
            thisRef.setFlags.remove(property.name)
            return
        }
        thisRef.setFlags[property.name] = value
    }
}

class MessageOneWayConvertedFlag<T, V>(val convert: (T) -> V) {
    operator fun <U : ChatMessage<U>> getValue(thisRef: U, property: KProperty<*>): Any? {
        @Suppress("UNCHECKED_CAST")
        return thisRef.setFlags.getOrDefault(property.name, null) as V?
    }

    operator fun <U : ChatMessage<U>> setValue(thisRef: U, property: KProperty<*>, value: Any?) {
        if (value == null) {
            thisRef.setFlags.remove(property.name)
            return
        }
        thisRef.setFlags[property.name] = convert(value as T)!!
    }
}

class MessageTwoWayConvertedFlag<T, V>(val convertTo: (T) -> V, val convertFrom: (V) -> T) {
    operator fun <U : ChatMessage<U>> getValue(thisRef: U, property: KProperty<*>): Any? {
        @Suppress("UNCHECKED_CAST")
        return (thisRef.setFlags.getOrDefault(property.name, null) as V?)?.let { convertFrom(it) }
    }

    operator fun <U : ChatMessage<U>> setValue(thisRef: U, property: KProperty<*>, value: Any?) {
        if (value == null) {
            thisRef.setFlags.remove(property.name)
            return
        }
        thisRef.setFlags[property.name] = convertTo(value as T)!!
    }
}
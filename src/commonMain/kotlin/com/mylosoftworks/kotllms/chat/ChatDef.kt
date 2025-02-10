package com.mylosoftworks.kotllms.chat

import com.mylosoftworks.kotllms.chat.features.ChatFeatureImages
import com.mylosoftworks.kotllms.runIfImpl
import com.mylosoftworks.kotllms.shared.AttachedImage

class ChatDef<M : ChatMessage> (val createEmpty: () -> M) {
    var messages = mutableListOf<M>()

    fun lastMessageImages() = messages.last().runIfImpl<ChatFeatureImages, List<AttachedImage>> { images }

    fun addMessage(message: M) {
        messages.add(message)
    }

    fun createMessage(config: M.() -> Unit) {
        addMessage(storeMessage(config))
    }

    fun storeMessage(config: M.() -> Unit): M = createEmpty().apply(config)

    fun createNew(): ChatDef<M> {
        return ChatDef(createEmpty)
    }

    fun subChat(count: Int, prefix: MutableList<M> = mutableListOf(), suffix: MutableList<M> = mutableListOf()): ChatDef<M> {
        val clone = this.createNew()
        clone.messages = prefix
        clone.messages.addAll(messages.takeLast(count).toMutableList())
        clone.messages.addAll(suffix)
        return clone
    }

    fun cutChat(start: Int, end: Int, prefix: MutableList<M> = mutableListOf(), suffix: MutableList<M> = mutableListOf()): ChatDef<M> {
        val clone = this.createNew()
        clone.messages = prefix
        clone.messages.addAll(messages.subList(start, end))
        clone.messages.addAll(suffix)
        return clone
    }

    fun clone(): ChatDef<M> {
        val clone = this.createNew()
        clone.messages = messages.toMutableList() // Copies the list
        return clone
    }

    // TODO: Filter functions based on token counts, store token counts in chat messages
}
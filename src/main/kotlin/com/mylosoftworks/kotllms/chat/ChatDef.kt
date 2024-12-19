package com.mylosoftworks.kotllms.chat

class ChatDef<M : ChatMessage<M>> : Cloneable {
    var messages = mutableListOf<M>()

    fun lastMessageImages() = messages.last().getAttachedImages()

    fun addMessage(message: M) {
        messages.add(message)
    }

    fun <T : ChatDef<M>> subChat(count: Int, prefix: MutableList<M> = mutableListOf(), suffix: MutableList<M> = mutableListOf()): T {
        val clone = this.clone() as T // Makes sure it's the same class
        clone.messages = prefix
        clone.messages.addAll(messages.takeLast(count).toMutableList())
        clone.messages.addAll(suffix)
        return clone
    }
}
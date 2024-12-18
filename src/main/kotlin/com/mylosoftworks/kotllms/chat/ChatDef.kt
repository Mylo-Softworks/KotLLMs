package com.mylosoftworks.kotllms.chat

class ChatDef<M : ChatMessage<M>> : Cloneable {
    var messages = mutableListOf<M>()

    fun lastMessageImages() = messages.last().getAttachedImages()

    fun addMessage(message: M) {
        messages.add(message)
    }

    fun <T : ChatDef<M>> subChat(count: Int): T {
        val clone = this.clone() as T // Makes sure it's the same class
        clone.messages = messages.takeLast(count).toMutableList()
        return clone
    }
}
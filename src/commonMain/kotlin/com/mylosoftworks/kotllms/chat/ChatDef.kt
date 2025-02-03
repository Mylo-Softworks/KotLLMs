package com.mylosoftworks.kotllms.chat

class ChatDef<M : ChatMessage<M>> {
    var messages = mutableListOf<M>()

    fun lastMessageImages() = messages.last().getAttachedImages()

    fun addMessage(message: M) {
        messages.add(message)
    }

    fun createNew(): ChatDef<M> {
        return ChatDef()
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
}
package com.mylosoftworks.kotllms.chat

class ChatDef<M : ChatMessage<M>> {
    val messages = arrayListOf<M>()

    fun addMessage(message: M) {
        messages.add(message)
    }
}
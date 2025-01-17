package com.mylosoftworks.kotllms.chat.templated

import com.mylosoftworks.kotllms.chat.ChatDef
import kotlin.js.JsName

/**
 * ChatTemplates are used to format a chat to a raw message. Allowing chats with apis that normally only support raw generation.
 */
abstract class ChatTemplate {
    abstract fun formatChat(def: ChatDef<*>): String
    @JsName("stopStringsFun")
    abstract fun stopStrings(): List<String>
}
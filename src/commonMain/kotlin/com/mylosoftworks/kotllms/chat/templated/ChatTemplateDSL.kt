package com.mylosoftworks.kotllms.chat.templated

import com.mylosoftworks.kotllms.chat.ChatDef

/**
 * DSL for creating chat templates
 */
class ChatTemplateDSL(val stopStrings: List<String> = listOf(), val block: ChatDef<*>.() -> String) : ChatTemplate() {
    override fun formatChat(def: ChatDef<*>): String {
        return block(def)
    }

    override fun stopStrings(): List<String> = stopStrings
}
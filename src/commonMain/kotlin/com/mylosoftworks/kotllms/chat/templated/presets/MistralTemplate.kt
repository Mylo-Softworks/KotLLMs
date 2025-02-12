package com.mylosoftworks.kotllms.chat.templated.presets

import com.mylosoftworks.kotllms.chat.ChatDef
import com.mylosoftworks.kotllms.chat.templated.ChatTemplate
import com.mylosoftworks.kotllms.features.impl.ChatGen
import com.mylosoftworks.kotllms.features.impl.ChatRole

/**
 * A template preset for mistral models, system prompts are inserted as a user message in [[INST]] content [[/INST]]
 *
 * @param startToken Whether or not to include <s> at the start of the prompt
 * @param userRoles All role names which should use [[INST]]
 */
class MistralTemplate(val startToken: Boolean = true, val prependStartToken: Boolean = true, val userRoles: List<ChatRole> = listOf(ChatRole.User, ChatRole.System)): ChatTemplate() {
    override fun formatChat(def: ChatDef<*>): String {
        return def.messages.joinToString(" ", if (startToken) "<s> " else "") {
            if (it["role"] in userRoles) {
                "[INST] ${it["content"]} [/INST]"
            }
            else {
                "${it["content"]} </s>"
            }
        }
    }

    override fun stopStrings(): List<String> = listOf("<s>", "</s>", "[INST]", "[/INST]")
}
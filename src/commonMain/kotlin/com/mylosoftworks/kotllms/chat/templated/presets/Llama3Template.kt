package com.mylosoftworks.kotllms.chat.templated.presets

import com.mylosoftworks.kotllms.chat.ChatDef
import com.mylosoftworks.kotllms.chat.templated.ChatTemplate
import com.mylosoftworks.kotllms.features.impl.ChatGen

/**
 * A template preset for Llama 3 models. Intended roles: `system`, `user`, `assistant`.
 *
 * @param roleMap A map containing lookup keys to replace in role names, for correction.
 */
class Llama3Template(val startToken: Boolean = true, val roleMap: (ChatGen.ChatRole) -> String = { it.genericName }): ChatTemplate() {
    override fun formatChat(def: ChatDef<*>): String {
        return def.messages.joinToString(" ", if (startToken) "<|begin_of_text|>" else "") {
            createMessage((it["role"] as ChatGen.ChatRole).let { roleMap(it) }, it["content"].toString())
        } + createHeader("assistant")
    }

    private fun createHeader(role: String) = "<|start_header_id|>$role<|end_header_id|>\n\n"
    private fun createMessage(role: String, content: String) = createHeader(role) + content + "<|eot_id|>"

    override fun stopStrings(): List<String> = listOf("<|im_end|>", "<|im_start|>", "<|end|>")
}
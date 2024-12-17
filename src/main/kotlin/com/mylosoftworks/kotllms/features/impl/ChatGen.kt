package com.mylosoftworks.kotllms.features.impl

import com.mylosoftworks.kotllms.api.GenerationResult
import com.mylosoftworks.kotllms.chat.ChatDef
import com.mylosoftworks.kotllms.api.Flags

/**
 * Chat generation (for apis supporting real chats)
 * Apis which do not support real chats can still work in a chat context by using a prompt template
 */
interface ChatGen<F: Flags<*>, C : ChatDef<*>> {
    suspend fun chatGen(chatDef: C, flags: F? = null): GenerationResult
}
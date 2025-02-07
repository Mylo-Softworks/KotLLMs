package com.mylosoftworks.kotllms.api.impl.extenders

import com.mylosoftworks.kotllms.Union
import com.mylosoftworks.kotllms.api.API
import com.mylosoftworks.kotllms.api.GenerationResult
import com.mylosoftworks.kotllms.api.Settings
import com.mylosoftworks.kotllms.chat.ChatDef
import com.mylosoftworks.kotllms.chat.ChatMessage
import com.mylosoftworks.kotllms.chat.templated.ChatTemplate
import com.mylosoftworks.kotllms.features.Flags
import com.mylosoftworks.kotllms.features.Wrapper
import com.mylosoftworks.kotllms.features.flagsimpl.*
import com.mylosoftworks.kotllms.features.impl.*
import com.mylosoftworks.kotllms.runIfImpl
import com.mylosoftworks.kotllms.toUnion2
import kotlin.reflect.KClass

@Suppress("unchecked_cast")
class ChatFromRawGen<F: Flags<*>, S: Settings, A: API<S, F>> internal constructor(val api: Wrapper<A>, var template: ChatTemplate):
    RawGen<F> by api.getWrapped() as RawGen<F>, ChatGen<F, ChatMessage>, Wrapper<A> {
    override suspend fun <M2 : ChatMessage> chatGen(chatDef: ChatDef<M2>, flags: F?): Result<GenerationResult> {
        val validFlags = flags ?: api.getWrapped().createFlags()

        validFlags.runIfImpl<FlagTrimStop> {
            trimStop = trimStop ?: true
        }
        validFlags.runIfImpl<FlagPrompt> {
            prompt = template.formatChat(chatDef)
        }
        validFlags.runIfImpl<FlagStopSequences> {
            stopSequences = template.stopStrings()
        }
        validFlags.runIfImpl<FlagAttachedImages> {
            if (images == null) images = chatDef.lastMessageImages()
        }

        return rawGen(validFlags)
    }

    override fun getLinked(): Union<A, Wrapper<A>> = api.toUnion2()
    override fun targetClass(): KClass<*> = API::class // We want to find the API
}

/**
 * Wrapper which lets you wrap an API to add chat functionality using the rawGen function. This has 2 main uses.
 *
 * 1. Wrapping an api which doesn't support chats
 * 2. Manually implementing chat on an API which does support chats, but through rawGen
 *
 * Once wrapped, the original api is available through `ChatFromRawGen.api`.
 */
@Suppress("unchecked_cast")
fun <F: Flags<F>> RawGen<F>.toChat(template: ChatTemplate) = ChatFromRawGen(this as API<*, F>, template)

/**
 * Extension function for generating a chat response with a model without knowing whether it's a chat API or a rawGen API beforehand.
 *
 * @param template The chat template to use in case this is a RawGen API
 */
@Suppress("unchecked_cast")
suspend fun <F: Flags<F>, M: ChatMessage> API<*, F>.chatGenWithTemplateFallback(template: ChatTemplate, chatDef: ChatDef<M>, flags: F? = null) =
    when (this) {
        is ChatGen<*, *> -> (this as ChatGen<F, M>).chatGen(chatDef, flags)
        is RawGen<*> -> (this as RawGen<F>).toChat(template).chatGen(chatDef as ChatDef<ChatMessage>, flags)
        else -> null
    }
//    (this as? ChatGen<F, ChatDef<M>>)?.chatGen(chatDef, flags)
//    (this as? RawGen<F>)?.toChat(template)?.chatGen(chatDef as ChatDef<ChatMessage>, flags)

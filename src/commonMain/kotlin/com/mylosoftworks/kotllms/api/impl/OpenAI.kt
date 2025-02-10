package com.mylosoftworks.kotllms.api.impl

import com.mylosoftworks.kotllms.api.*
import com.mylosoftworks.kotllms.api.settingfeatures.SettingFeatureAuth
import com.mylosoftworks.kotllms.api.settingfeatures.SettingFeatureUrl
import com.mylosoftworks.kotllms.chat.ChatDef
import com.mylosoftworks.kotllms.chat.ChatMessage
import com.mylosoftworks.kotllms.features.*
import com.mylosoftworks.kotllms.features.flagsimpl.*
import com.mylosoftworks.kotllms.features.impl.*
import com.mylosoftworks.kotllms.jsonSettings
import com.mylosoftworks.kotllms.runIfImpl
import com.mylosoftworks.kotllms.stripTrailingSlash
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import kotlin.coroutines.coroutineContext

class OpenAI(settings: OpenAISettings = OpenAISettings(null)): HTTPAPI<OpenAISettings, OpenAIGenFlags>(settings),
    ListModels<OpenAIModelDef>, RawGen<OpenAIGenFlags>, ChatGen<OpenAIGenFlags, OpenAIChatMessage>, FlagGenRequest<OpenAIGenFlags>
{
    override fun getApiUrl(path: String): String = settings.url + path

    override suspend fun check(): Boolean = listModels().isSuccess

    override fun createFlags(): OpenAIGenFlags = OpenAIGenFlags()
    override suspend fun listModels(): Result<List<OpenAIModelDef>> {
        return runCatching {
            val modelListJson = jsonSettings.parseToJsonElement(makeHttpGet("/models").getOrThrow().bodyAsText()).jsonObject["data"] ?: throw RuntimeException("Invalid API response")
            jsonSettings.decodeFromJsonElement<List<OpenAIModelDef>>(modelListJson)
        }
    }

    override suspend fun internalGen(url: String, flags: OpenAIGenFlags): Result<GenerationResult> {
        return openAIInternalGen(url, flags)
    }

    override suspend fun rawGen(prompt: String, flags: OpenAIGenFlags?): Result<GenerationResult> {
        val appliedFlags = (flags ?: createFlags()).apply { setFlags["prompt"] = prompt.toJson() }
        return internalGen("/completions", appliedFlags)
    }

    override suspend fun <M2 : OpenAIChatMessage> chatGen(
        chatDef: ChatDef<M2>,
        flags: OpenAIGenFlags?
    ): Result<GenerationResult> {
        val appliedFlags = (flags ?: createFlags()).apply { setFlags["messages"] = chatDef.toJson() }
        return internalGen("/chat/completions", appliedFlags)
    }

    override fun createChat(block: ChatDef<OpenAIChatMessage>.() -> Unit): ChatDef<OpenAIChatMessage> {
        return ChatDef { OpenAIChatMessage() }.apply(block)
    }
}

class OpenAISettings(override var apiKey: String?, url: String = "https://api.openai.com/v1"): Settings(), SettingFeatureUrl, SettingFeatureAuth {
    override var url: String = stripTrailingSlash(url)
        set(value) { field = stripTrailingSlash(value) }

    override fun applyToRequest(builder: HttpRequestBuilder) {
        apiKey?.let { builder.bearerAuth(it) }
    }
}

class OpenAIGenFlags: Flags(),
        FlagMaxLength, FlagStream, FlagRepetitionPenalty, FlagPresencePenalty,
        FlagStopSequences, FlagTemperature, FlagTopP, FlagModel
{
    override var model: String? by flag<String>().jsonBacked()

    override var maxLength: Int? by flag<Int>("max_tokens").jsonBacked()
    override var temperature: Float? by flag<Float>().jsonBacked()
    override var topP: Float? by flag<Float>("top_p").jsonBacked()
    override var repetitionPenalty: Float? by flag<Float>("frequency_penalty").jsonBacked()

    override var presencePenalty: Float? by flag<Float>("presence_penalty").jsonBacked()
    override var stopSequences by stringListFlag("stop")
    override var stream: Boolean? by flag<Boolean>().jsonBacked()
}

@Serializable
class OpenAIModelDef(val id: String): ListedModelDef {
    override val modelName: String get() = id
}

class OpenAIGenerationResults(json: String): GenerationResult(false) {
    var content: String
    init {
        jsonSettings.parseToJsonElement(json).jsonObject.let {root ->
            root["choices"]!!.jsonArray.let {results ->
                results[0].jsonObject.let {result ->
                    content = result["text"]?.jsonPrimitive?.content
                        ?: result["message"]?.jsonObject?.get("content")?.jsonPrimitive?.content
                                ?: error("Could not parse response.")
                }
            }
        }
    }

    override fun getText() = content
}

@Serializable
data class OpenAIStreamChunk(val text: String, val finish_reason: String?): StreamChunk() {
    override fun getTokenF(): String = text
    override fun isLastToken() = (finish_reason ?: "null") != "null"
}

@Serializable
data class OpenAIChatStreamChunk(val delta: OpenAIChatStreamChunkDelta, val finish_reason: String?) {
    fun toRegular() = OpenAIStreamChunk(delta.content, finish_reason)
}

@Serializable
data class OpenAIChatStreamChunkDelta(val role: String? = null, val content: String)

class OpenAIGenerationResultsStreamed: StreamedGenerationResult<OpenAIStreamChunk>() {
    var finish_reason = "null"
    override val currentContentAsChunk
        get() = OpenAIStreamChunk(currentContent, finish_reason)

    override fun update(chunk: OpenAIStreamChunk) {
        if (error != null) return
        super.update(chunk)

        finish_reason = chunk.finish_reason.toString()
    }

    override fun registerStreamer(block: (Result<OpenAIStreamChunk>) -> Unit) {
        block(error?.let { Result.failure(error!!) } ?: Result.success(currentContentAsChunk))
        streamers.add(block)
    }

    override fun isComplete(): Boolean {
        return finish_reason != "null"
    }

    override fun criticalError(error: Throwable) {
        finish_reason = "error"
        streamers.forEach { it(Result.failure(error)) }
    }
}

class OpenAIChatMessage: ChatMessage() {
    // TODO: Allow media like images etc
}

/**
 * Internal gen function compatible with OpenAI compatible endpoints. Useful for supporting proxies with more input features which still output in a compatible format.
 */
suspend fun <F: Flags> HTTPAPI<*, F>.openAIInternalGen(url: String, flags: F): Result<GenerationResult> {
    if (flags.runIfImpl<FlagStream, Boolean?> { stream } == true) {
        // Streamed

        val result = OpenAIGenerationResultsStreamed()
        CoroutineScope(coroutineContext).launch {
            makeHttpSSEPost(url, flags) {
                var lastChunk: OpenAIStreamChunk? = null
                while ((lastChunk?.finish_reason ?: "null") == "null" && this.isActive) {
                    incoming.collect { event ->
                        val data = event.data
                        if (data != null && data != "[DONE]") {
                            val obj = jsonSettings.parseToJsonElement(data).jsonObject
                            val chunk = obj["choices"]!!.jsonArray[0]

                            if (obj["object"]?.jsonPrimitive?.content == "chat.completion.chunk") {
                                val newChunk = jsonSettings.decodeFromJsonElement<OpenAIChatStreamChunk>(chunk).toRegular()
                                result.update(newChunk)
                                lastChunk = newChunk
                            }
                            else {
                                val newChunk = jsonSettings.decodeFromJsonElement<OpenAIStreamChunk>(chunk)
                                result.update(newChunk)
                                lastChunk = newChunk
                            }
                        }
                    }
                }
            }.getOrElse { result.criticalError(it) } // Crash the current result.
        }

        return Result.success(result) // Note: Streaming always returns success, errors are provided through the callback.
    }
    else {
        return Result.success(OpenAIGenerationResults(makeHttpPost(url, flags).getOrElse { return Result.failure(it) }.bodyAsText()))
    }
}
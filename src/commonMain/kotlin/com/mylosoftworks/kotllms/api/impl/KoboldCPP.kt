package com.mylosoftworks.kotllms.api.impl

import com.mylosoftworks.kotllms.api.*
import com.mylosoftworks.kotllms.api.settingfeatures.SettingFeatureAuth
import com.mylosoftworks.kotllms.api.settingfeatures.SettingFeatureUrl
import com.mylosoftworks.kotllms.features.*
import com.mylosoftworks.kotllms.features.flagsimpl.*
import com.mylosoftworks.kotllms.features.impl.*
import com.mylosoftworks.kotllms.jsonSettings
import com.mylosoftworks.kotllms.shared.AttachedImage
import com.mylosoftworks.kotllms.stripTrailingSlash
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import kotlin.coroutines.coroutineContext

class KoboldCPP(settings: KoboldCPPSettings = KoboldCPPSettings()) : HTTPAPI<KoboldCPPSettings, KoboldCPPGenFlags>(settings),
    Version<KoboldCPPVersion>, GetCurrentModel<KoboldCPPModel>, ContextLength, TokenCount<KoboldCPPGenFlags, TokenCountDef>,
    RawGen<KoboldCPPGenFlags>, FlagGenRequest<KoboldCPPGenFlags> {

    override fun getApiUrl(path: String) = settings.url + path

    override suspend fun check(): Boolean {
        return runCatching {
            makeHttpGet("/api/v1/model").getOrElse { return false }.status == HttpStatusCode.OK
        }.isSuccess
    }

    override fun createFlags(): KoboldCPPGenFlags = KoboldCPPGenFlags()

    override suspend fun version(): Result<KoboldCPPVersion> {
        return Result.success(makeHttpGet("/api/extra/version").getOrElse { return Result.failure(it) }.bodyAsText().let { jsonSettings.decodeFromString(it) })
    }

    override suspend fun getCurrentModel(): Result<KoboldCPPModel> {
        return Result.success(makeHttpGet("/api/v1/model").getOrElse { return Result.failure(it) }.bodyAsText().let {
            jsonSettings.parseToJsonElement(it).jsonObject["result"]?.jsonPrimitive?.content?.let { it2 ->
                KoboldCPPModel(it2)
            } ?: return Result.failure(RuntimeException("Invalid response"))
        })
    }

    override suspend fun contextLength(model: String?): Result<Int> {
        return Result.success(makeHttpGet("/api/extra/true_max_context_length").getOrElse { return Result.failure(it) }.bodyAsText()
            .let { jsonSettings.decodeFromString<RawResponses.ContextLength>(it).value })
    }

    override suspend fun tokenCount(string: String, flags: KoboldCPPGenFlags?): Result<TokenCountDef> {
        return Result.success(makeHttpPost("/api/extra/tokencount", flags ?: KoboldCPPGenFlags()) {
            set("prompt", string.toJson())
        }.getOrElse { return Result.failure(it) }.bodyAsText().let {
            TokenCountDef(jsonSettings.parseToJsonElement(it).jsonObject["value"]?.jsonPrimitive?.int ?: return Result.failure(RuntimeException("Invalid response")))
        })
    }

    override suspend fun internalGen(
        url: String,
        flags: KoboldCPPGenFlags,
        streamLikeChat: Boolean // Chat isn't currently implemented for KoboldCPP in favor of templates.
    ): Result<GenerationResult> {
        if (flags.stream==true) {
            // Streamed

            val result = KoboldCPPGenerationResultsStreamed(this)
            CoroutineScope(coroutineContext).launch {
                makeHttpSSEPost(url, flags) {
                    var lastChunk: KoboldCPPStreamChunk? = null
                    while ((lastChunk?.finish_reason ?: "null") == "null" && this.isActive) {
                        incoming.collect { event ->
                            if (event.event == "message") {
                                val data = event.data
                                if (data != null) {
                                    val newChunk = jsonSettings.decodeFromString<KoboldCPPStreamChunk>(data)
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
        // Non-streamed
        return Result.success(makeHttpPost(url, flags).getOrElse { return Result.failure(it) }.bodyAsText().let {
            KoboldCPPGenerationResults(it, this)
        })
    }

    override suspend fun rawGen(prompt: String, flags: KoboldCPPGenFlags?): Result<GenerationResult> {
        val appliedFlags = (flags ?: createFlags()).apply { setFlags["prompt"] = prompt.toJson() }
        val stream = appliedFlags.stream == true
        return internalGen(if (stream) "/api/extra/generate/stream" else "/api/v1/generate", appliedFlags)
    }

    suspend fun sendAbort() {
        makeHttpPost("/api/extra/abort", KoboldCPPGenFlags())
    }
}

class KoboldCPPSettings(url: String = "http://localhost:5001", override var apiKey: String? = null) : Settings(), SettingFeatureUrl, SettingFeatureAuth {
    override var url: String = stripTrailingSlash(url)
        set(value) { field = stripTrailingSlash(value) }

    override fun applyToRequest(builder: HttpRequestBuilder) {
        apiKey?.let { builder.bearerAuth(it) }
    }
}

class KoboldCPPGenFlags : Flags(),
    FlagsAllBasic, FlagsCommonSampling, FlagTopA, FlagTfs, FlagTypical, FlagRepetitionPenaltyWithRangeSlope,
    FlagStopSequences, FlagTrimStop, FlagEarlyStopping, FlagAttachedImages, FlagGrammarGBNF, FlagQuiet, FlagStream
{
    override var contextSize by flag<Int>("max_context_length").jsonBacked()
    override var maxLength by flag<Int>("max_length").jsonBacked()
//    override var prompt by flag<String>().jsonBacked()
    override var quiet by flag<Boolean>().jsonBacked()
    override var repetitionPenalty by flag<Float>("rep_pen").jsonBacked()
    override var repetitionPenaltyRange by flag<Int>("rep_pen_range").jsonBacked()
    override var repetitionPenaltySlope by flag<Float>("rep_pen_slope").jsonBacked()
    override var temperature by flag<Float>().jsonBacked()
    override var tfs by flag<Float>().jsonBacked()
    override var topA by flag<Float>("top_a").jsonBacked()
    override var topK by flag<Int>("top_k").jsonBacked()
    override var topP by flag<Float>("top_p").jsonBacked()
    override var typical by flag<Float>().jsonBacked()
    override var stopSequences by stringListFlag("stop_sequence")
    override var trimStop by flag<Boolean>("trim_stop").jsonBacked()
    override var earlyStopping by flag<Boolean>("bypass_eos").jsonBacked() // Set to false to prevent early stopping

    override var images by flag<List<AttachedImage>>().jsonBacked() // AttachedImage is serializable

    override var stream: Boolean? = false // Not an actual flag, but changes behavior

    override var grammar by gbnfFlag()
    override fun setGbnfGrammarRaw(gbnf: String?) {
        if (gbnf == null) {
            setFlags.remove("grammar")
            return
        }
        setFlags["grammar"] = gbnf.toJson()
    }
}

@Serializable
data class KoboldCPPVersion(val result: String, val version: String, val protected: Boolean,
    val txt2img: Boolean, val vision: Boolean, val transcribe: Boolean, val multiplayer: Boolean) : VersionInfo() {
    override val versionNumber: String
        get() = version
}

class KoboldCPPModel(override val modelName: String) : ListedModelDef

object RawResponses {
    @Serializable
    data class ContextLength(val value: Int)
}

class KoboldCPPGenerationResults(json: String, val api: KoboldCPP) : GenerationResult(false), Cancellable {
    var content: String
    init {
        jsonSettings.parseToJsonElement(json).jsonObject.let {root ->
            root["results"]!!.jsonArray.let {results ->
                results[0].jsonObject.let {result ->
                    content = result["text"]!!.jsonPrimitive.content
                }
            }
        }
    }

    override fun getText() = content

    override suspend fun cancel() {
        api.sendAbort()
    }
}

@Serializable
data class KoboldCPPStreamChunk(val token: String, val finish_reason: String): StreamChunk() {
    override fun getTokenF(): String = token
    override fun isLastToken() = finish_reason != "null"
}

class KoboldCPPGenerationResultsStreamed(val api: KoboldCPP) : StreamedGenerationResult<KoboldCPPStreamChunk>(), Cancellable {
    var finish_reason = "null"
    override val currentContentAsChunk
        get() = KoboldCPPStreamChunk(currentContent, finish_reason)

    override fun update(chunk: KoboldCPPStreamChunk) {
        if (error != null) return
        super.update(chunk)

        finish_reason = chunk.finish_reason
    }

    override fun registerStreamer(block: (Result<KoboldCPPStreamChunk>) -> Unit) {
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

    override suspend fun cancel() {
        api.sendAbort()
    }
}
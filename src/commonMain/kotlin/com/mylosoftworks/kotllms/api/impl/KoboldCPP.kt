package com.mylosoftworks.kotllms.api.impl

import com.mylosoftworks.kotllms.api.*
import com.mylosoftworks.kotllms.features.*
import com.mylosoftworks.kotllms.features.flagsimpl.*
import com.mylosoftworks.kotllms.features.impl.*
import com.mylosoftworks.kotllms.jsonSettings
import com.mylosoftworks.kotllms.shared.AttachedImage
import com.mylosoftworks.kotllms.shared.createKtorClient
import com.mylosoftworks.kotllms.stripTrailingSlash
import com.mylosoftworks.kotllms.wrapTryCatchToResult
import io.ktor.client.plugins.sse.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import kotlin.coroutines.coroutineContext

class KoboldCPP(settings: KoboldCPPSettings = KoboldCPPSettings()) : API<KoboldCPPSettings, KoboldCPPGenFlags>(settings),
    Version<KoboldCPPVersion>, GetCurrentModel<KoboldCPPModel>, ContextLength, TokenCount<KoboldCPPGenFlags, TokenCountDef>,
    RawGen<KoboldCPPGenFlags> {

    val client = createKtorClient()

    private fun getApiUrl(path: String) = settings.url + path
    private suspend fun makeHttpGet(path: String): Result<HttpResponse> {
        return wrapTryCatchToResult {
            client.get(getApiUrl(path)) {
                settings.applyToRequest(this)
            }
        }
    }

    private suspend fun makeHttpPost(path: String, flags: KoboldCPPGenFlags, extraSettings: (HttpRequestBuilder) -> Unit = {}, block: HashMap<String, JsonElement>.() -> Unit = {}): Result<HttpResponse> {
        return wrapTryCatchToResult {
            client.post(getApiUrl(path)) {
                settings.applyToRequest(this)

                contentType(ContentType.Application.Json)
                extraSettings(this)
                setBody(
                    hashMapOf<String, JsonElement>().apply(block).apply {flags.applyToRequestJson(this)}
                )
            }
        }
    }

    private suspend fun makeHttpSSEPost(path: String, flags: KoboldCPPGenFlags, extraSettings: (HttpRequestBuilder) -> Unit = {}, block: suspend ClientSSESession.() -> Unit): Result<Unit> {
        return wrapTryCatchToResult {
            client.sse(getApiUrl(path), {
                method = HttpMethod.Post

                settings.applyToRequest(this)

                contentType(ContentType.Application.Json)
                accept(ContentType.Text.EventStream)

                extraSettings(this)
                setBody(
                    hashMapOf<String, JsonElement>().apply {flags.applyToRequestJson(this)}
                )


            }, block = block)
        }
    }

    override suspend fun check(): Boolean {
        return makeHttpGet("/api/v1/model").getOrElse { return false }.status == HttpStatusCode.OK
    }

    override fun createFlags(): KoboldCPPGenFlags {
        return KoboldCPPGenFlags()
    }

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

    override suspend fun contextLength(): Result<Int> {
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

    override suspend fun rawGen(flags: KoboldCPPGenFlags?): Result<GenerationResult> {
        if (flags != null && flags.stream==true) {
            // Streamed

            val result = KoboldCPPGenerationResultsStreamed(this)
            CoroutineScope(coroutineContext).launch {
                makeHttpSSEPost("/api/extra/generate/stream", flags) {
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
        return Result.success(makeHttpPost("/api/v1/generate", flags ?: KoboldCPPGenFlags()).getOrElse { return Result.failure(it) }.bodyAsText().let {
            KoboldCPPGenerationResults(it, this)
        })
    }

    suspend fun sendAbort() {
        makeHttpPost("/api/extra/abort", KoboldCPPGenFlags())
    }
}

class KoboldCPPSettings(url: String = "http://localhost:5001") : Settings() {
    var url: String = url
        set(value) { field = stripTrailingSlash(value) }

    fun applyToRequest(builder: HttpRequestBuilder) {

    }
}

class KoboldCPPGenFlags : Flags<KoboldCPPGenFlags>(),
    FlagsAllBasic, FlagsCommonSampling, FlagTopA, FlagTfs, FlagTypical, FlagRepetitionPenaltyWithRangeSlope,
    FlagStopSequences, FlagTrimStop, FlagEarlyStopping, FlagAttachedImages, FlagGrammarGBNF, FlagQuiet, FlagStream
{
    override var contextSize by flag<Int>("max_context_length").jsonBacked()
    override var maxLength by flag<Int>("max_length").jsonBacked()
    override var prompt by flag<String>().jsonBacked()
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
    override var stopSequences by flag<List<String>>("stop_sequence").jsonBacked({ jsonSettings.encodeToJsonElement(this) },
        { this.jsonArray.map { it.jsonPrimitive.content }.toList() })
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

class KoboldCPPModel(modelName: String) : ListedModelDef(modelName)

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

    override fun getText(): String {
        return currentContent
    }

    override suspend fun cancel() {
        api.sendAbort()
    }

    override fun criticalError(error: Throwable) {
        finish_reason = "error"
        streamers.forEach { it(Result.failure(error)) }
    }
}
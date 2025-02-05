package com.mylosoftworks.kotllms.api.impl

import com.mylosoftworks.kotllms.api.*
import com.mylosoftworks.kotllms.features.*
import com.mylosoftworks.kotllms.features.flagsimpl.*
import com.mylosoftworks.kotllms.features.impl.*
import com.mylosoftworks.kotllms.jsonSettings
import com.mylosoftworks.kotllms.shared.AttachedImage
import com.mylosoftworks.kotllms.shared.createKtorClient
import com.mylosoftworks.kotllms.stripTrailingSlash
import io.ktor.client.plugins.sse.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import kotlin.coroutines.coroutineContext

class KoboldCPP(settings: KoboldCPPSettings = KoboldCPPSettings()) : API<KoboldCPPSettings, KoboldCPPGenFlags>(settings),
    Version<KoboldCPPVersion>, GetCurrentModel<KoboldCPPModel>, ContextLength, TokenCount<KoboldCPPGenFlags>,
    RawGen<KoboldCPPGenFlags> {

    val client = createKtorClient()

    private fun getApiUrl(path: String) = settings.url + path
    private suspend fun makeHttpGet(path: String): HttpResponse {
        return client.get(getApiUrl(path)) {
            settings.applyToRequest(this)
        }
    }

    private suspend fun makeHttpPost(path: String, flags: KoboldCPPGenFlags, extraSettings: (HttpRequestBuilder) -> Unit = {}, block: HashMap<String, JsonElement>.() -> Unit = {}): HttpResponse {
        return client.post(getApiUrl(path)) {
            settings.applyToRequest(this)

            contentType(ContentType.Application.Json)
            extraSettings(this)
            setBody(
                hashMapOf<String, JsonElement>().apply(block).apply {flags.applyToRequestJson(this)}
            )
        }
    }

    private suspend fun makeHttpSSEPost(path: String, flags: KoboldCPPGenFlags, extraSettings: (HttpRequestBuilder) -> Unit = {}, block: suspend ClientSSESession.() -> Unit) {
        client.sse(getApiUrl(path), {
            method = HttpMethod.Post

            settings.applyToRequest(this)

            contentType(ContentType.Application.Json)

            extraSettings(this)
            setBody(
                hashMapOf<String, JsonElement>().apply {flags.applyToRequestJson(this)}
            )
        }, block = block)
    }

    override suspend fun check(): Boolean {
        return makeHttpGet("/api/v1/model").status == HttpStatusCode.OK
    }

    override fun createFlags(): KoboldCPPGenFlags {
        return KoboldCPPGenFlags()
    }

    override suspend fun version(): KoboldCPPVersion {
        return makeHttpGet("/api/extra/version").bodyAsText().let { jsonSettings.decodeFromString(it) }
    }

    override suspend fun getCurrentModel(): KoboldCPPModel {
        return makeHttpGet("/api/v1/model").bodyAsText().let {
            jsonSettings.parseToJsonElement(it).jsonObject["result"]?.jsonPrimitive?.content?.let { it2 ->
                KoboldCPPModel(it2)
            } ?: error("Invalid response")
        }
    }

    override suspend fun contextLength(): Int {
        return makeHttpGet("/api/extra/true_max_context_length").bodyAsText()
            .let { jsonSettings.decodeFromString<RawResponses.ContextLength>(it).value }
    }

    override suspend fun tokenCount(string: String, flags: KoboldCPPGenFlags?): Int {
        return makeHttpPost("/api/extra/tokencount", flags ?: KoboldCPPGenFlags()) {
            set("prompt", string.toJson())
        }.bodyAsText().let {
            jsonSettings.parseToJsonElement(it).jsonObject["value"]?.jsonPrimitive?.int ?: error("Invalid response")
        }
    }

    override suspend fun rawGen(flags: KoboldCPPGenFlags?): GenerationResult {
        if (flags != null && flags.stream==true) {
            return KoboldCPPGenerationResultsStreamed(this).also {
                CoroutineScope(coroutineContext).launch { // Needed so KoboldCPPGenerationResultsStreamed can be returned before the SSE is already complete
                    makeHttpSSEPost("/api/extra/generate/stream", flags) {
                        var lastChunk: KoboldCPPStreamChunk? = null
                        while ((lastChunk?.finish_reason ?: "null") == "null" && this.isActive) {
                            incoming.collect { event ->
                                if (event.event == "message") {
                                    val data = event.data
                                    if (data != null) {
                                        val newChunk = jsonSettings.decodeFromString<KoboldCPPStreamChunk>(data)
                                        it.update(newChunk)
                                        lastChunk = newChunk
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return makeHttpPost("/api/v1/generate", flags ?: KoboldCPPGenFlags()).bodyAsText().let {
            KoboldCPPGenerationResults(it, this)
        }
    }

    suspend fun sendAbort() {
        makeHttpPost("/api/extra/abort", KoboldCPPGenFlags())
    }

//    override suspend fun chatGen(
//        chatDef: ChatDef<BasicChatMessage>,
//        flags: KoboldCPPGenFlags?
//    ): GenerationResult {
//        if (!supportsChat()) error("Doesn't support chat, did you forget to add a template to the settings?")
//        val validFlags = flags ?: KoboldCPPGenFlags()
//
//        validFlags.trimStop = validFlags.trimStop ?: true
//        validFlags.prompt = settings.template!!.formatChat(chatDef)
//        validFlags.stopSequences = settings.template!!.stopStrings()
//        if (validFlags.images == null) validFlags.images = chatDef.lastMessageImages()
//
//        return rawGen(validFlags)
//    }
//
//    override suspend fun supportsChat() = settings.chatTemplateSetup()

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
    override var contextSize by Flag<Int>("max_context_length")
    override var maxLength by Flag<Int>("max_length")
    override var prompt by Flag<String>()
    override var quiet by Flag<Boolean>()
    override var repetitionPenalty by Flag<Float>("rep_pen")
    override var repetitionPenaltyRange by Flag<Int>("rep_pen_range")
    override var repetitionPenaltySlope by Flag<Float>("rep_pen_slope")
    override var temperature by Flag<Float>()
    override var tfs by Flag<Float>()
    override var topA by Flag<Float>("top_a")
    override var topK by Flag<Int>("top_k")
    override var topP by Flag<Float>("top_p")
    override var typical by Flag<Float>()
    override var stopSequences by BiConvertedJsonFlag<List<String>>("stop_sequence", { jsonSettings.encodeToJsonElement(it) },
        { it.jsonArray.map { it.jsonPrimitive.content }.toList() })
    override var trimStop by Flag<Boolean>("trim_stop")
    override var earlyStopping by Flag<Boolean>("bypass_eos") // Set to false to prevent early stopping

//    var images by BiConvertedJsonFlag<List<AttachedImage>>({ Json.encodeToJsonElement(it.map { it.toBase64() }) },
//        { it.jsonArray.map { AttachedImage(it.jsonPrimitive.content) }.toList() })
    override var images by Flag<List<AttachedImage>>() // AttachedImage is serializable

    override var stream: Boolean? = false // Not an actual flag, but changes behavior

    override var grammar by GBNFFlag()
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
    lateinit var content: String
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
data class KoboldCPPStreamChunk(val tokenVal: String, val finish_reason: String): StreamChunk() {
    override fun getToken(): String = tokenVal
    override fun isLastToken() = finish_reason != "null"
}

class KoboldCPPGenerationResultsStreamed(val api: KoboldCPP) : StreamedGenerationResult<KoboldCPPStreamChunk>(), Cancellable {
    val streamers = arrayListOf<(KoboldCPPStreamChunk) -> Unit>()
    var currentContent = ""
    var finish_reason = "null"
    val currentContentAsChunk
        get() = KoboldCPPStreamChunk(currentContent, finish_reason)

    override fun update(chunk: KoboldCPPStreamChunk) {
        streamers.forEach {
            it(chunk)
        }
        currentContent += chunk.tokenVal
        finish_reason = chunk.finish_reason
    }

    override fun registerStreamer(block: (KoboldCPPStreamChunk) -> Unit) {
        block(currentContentAsChunk)
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
}
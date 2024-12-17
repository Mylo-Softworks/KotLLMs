package com.mylosoftworks.kotllms.api.impl

import com.mylosoftworks.kotllms.api.*
import com.mylosoftworks.kotllms.features.impl.*
import com.mylosoftworks.kotllms.stripTrailingSlash
import io.ktor.client.*
import io.ktor.client.engine.java.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.sse.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.*
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import kotlinx.serialization.serializer
import kotlin.coroutines.coroutineContext

class KoboldCPP(settings: KoboldCPPSettings = KoboldCPPSettings()) : API<KoboldCPPSettings, KoboldCPPGenFlags>(settings),
    Version<KoboldCPPVersion>, GetCurrentModel<KoboldCPPModel>, ContextLength, TokenCount<KoboldCPPGenFlags>,
    RawGen<KoboldCPPGenFlags> {
    val client = HttpClient(Java) {
        install(ContentNegotiation) {
            json()
        }
        install(SSE)
    }

    private fun getApiUrl(path: String) = settings.url + path
    private suspend fun makeHttpGet(path: String): HttpResponse {
        return client.get(getApiUrl(path)) {
            settings.applyToRequest(this)
        }
    }
    @OptIn(InternalSerializationApi::class)
    private suspend fun makeHttpPost(path: String, flags: KoboldCPPGenFlags, extraSettings: (HttpRequestBuilder) -> Unit = {}, block: HashMap<String, Any>.() -> Unit = {}): HttpResponse {
        return client.post(getApiUrl(path)) {
            settings.applyToRequest(this)

            contentType(ContentType.Application.Json)
            extraSettings(this)
            setBody(
                hashMapOf<String, Any>().apply(block).apply {flags.applyToRequestJson(this)}
                    .mapValues { Json.encodeToJsonElement(it.value.javaClass.kotlin.serializer(), it.value) }
            )
        }
    }

    @OptIn(InternalSerializationApi::class)
    private suspend fun makeHttpSSEPost(path: String, flags: KoboldCPPGenFlags, extraSettings: (HttpRequestBuilder) -> Unit = {}, block: suspend ClientSSESession.() -> Unit) {
        client.sse(getApiUrl(path), {
            method = HttpMethod.Post

            settings.applyToRequest(this)

            contentType(ContentType.Application.Json)
            extraSettings(this)
            setBody(
                hashMapOf<String, Any>().apply {flags.applyToRequestJson(this)}
                    .mapValues { Json.encodeToJsonElement(it.value.javaClass.kotlin.serializer(), it.value) }
            )
        }, block = block)
    }

    override suspend fun check(): Boolean {
        return makeHttpGet("/api/v1/model").status == HttpStatusCode.OK
    }

    override suspend fun version(): KoboldCPPVersion {
        return makeHttpGet("/api/extra/version").bodyAsText().let { Json.decodeFromString(it) }
    }

    override suspend fun getCurrentModel(): KoboldCPPModel {
        return makeHttpGet("/api/v1/model").bodyAsText().let {
            Json.parseToJsonElement(it).jsonObject["result"]?.jsonPrimitive?.content?.let { it2 ->
                KoboldCPPModel(it2)
            } ?: error("Invalid response")
        }
    }

    override suspend fun contextLength(): Int {
        return makeHttpGet("/api/extra/true_max_context_length").bodyAsText()
            .let { Json.decodeFromString<RawResponses.ContextLength>(it).value }
    }

    override suspend fun tokenCount(string: String, flags: KoboldCPPGenFlags?): Int {
        return makeHttpPost("/api/extra/tokencount", flags ?: KoboldCPPGenFlags()) {
            set("prompt", string)
        }.bodyAsText().let {
            Json.parseToJsonElement(it).jsonObject["value"]?.jsonPrimitive?.int ?: error("Invalid response")
        }
    }

    override suspend fun rawGen(flags: KoboldCPPGenFlags?): GenerationResult {
        if (flags != null && flags.stream) {
            return KoboldCPPGenerationResultsStreamed(this).also {
                CoroutineScope(coroutineContext).launch { // Needed so KoboldCPPGenerationResultsStreamed can be returned before the SSE is already complete
                    makeHttpSSEPost("/api/extra/generate/stream", flags) {
                        var lastChunk: KoboldCPPStreamChunk? = null
                        while ((lastChunk?.finish_reason ?: "null") == "null" && this.isActive) {
                            incoming.collect { event ->
                                if (event.event == "message") {
                                    val data = event.data
                                    if (data != null) {
                                        val newChunk = Json.decodeFromString<KoboldCPPStreamChunk>(data)
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
}

class KoboldCPPSettings(url: String = "http://localhost:5001") : Settings() {
    var url: String = url
        set(value) { field = stripTrailingSlash(value) }

    fun applyToRequest(builder: HttpRequestBuilder) {

    }
}

class KoboldCPPGenFlags : Flags<KoboldCPPGenFlags>() {
    var max_context_length by Flag<Int>()
    var max_length by Flag<Int>()
    var prompt by Flag<String>()
    var quiet by Flag<Boolean>()
    var rep_pen by Flag<Float>()
    var rep_pen_range by Flag<Int>()
    var rep_pen_slope by Flag<Float>()
    var temperature by Flag<Float>()
    var tfs by Flag<Float>()
    var top_a by Flag<Float>()
    var top_k by Flag<Int>()
    var top_p by Flag<Float>()
    var typical by Flag<Float>()

    var stream: Boolean = false // Not an actual flag, but changes behavior

    var grammar by GBNFFlag()
    fun rawGrammar(gbnf: String?) {
        if (gbnf == null) {
            setFlags.remove("grammar")
            return
        }
        setFlags["grammar"] = gbnf
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
        Json.parseToJsonElement(json).jsonObject.let {root ->
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
data class KoboldCPPStreamChunk(val token: String, val finish_reason: String) {
    fun isLastToken() = finish_reason != "null"
}

class KoboldCPPGenerationResultsStreamed(val api: KoboldCPP) : StreamedGenerationResult<KoboldCPPStreamChunk>(), Cancellable {
    val streamers = arrayListOf<(KoboldCPPStreamChunk) -> Unit>()
    var currentContent = ""
    var finish_reason = "null"
    val currentContentAsChunk
        get() = KoboldCPPStreamChunk(currentContent, finish_reason)

    fun update(chunk: KoboldCPPStreamChunk) {
        streamers.forEach {
            it(chunk)
        }
        currentContent += chunk.token
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
package com.mylosoftworks.kotllms.api

import com.mylosoftworks.kotllms.Union
import com.mylosoftworks.kotllms.api.impl.KoboldCPPGenFlags
import com.mylosoftworks.kotllms.features.Flags
import com.mylosoftworks.kotllms.features.Wrapper
import com.mylosoftworks.kotllms.shared.createKtorClient
import com.mylosoftworks.kotllms.toUnion2
import com.mylosoftworks.kotllms.wrapTryCatchToResult
import io.ktor.client.plugins.sse.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.JsonElement
import kotlin.reflect.KClass

/**
 * API base implementation
 * @param S The settings type to use, settings are things like an API key, stored in the API user.
 * @param F The flags type to use, flags are things like generation parameters such as temperature, sent with the generation request.
 */
abstract class API<S: Settings, F: Flags<*>>(var settings: S): Wrapper<API<S, F>> {
    /**
     * Check if the API is available
     * @return Whether the API is functional
     */
    abstract suspend fun check(): Boolean

    abstract fun createFlags(): F
    inline fun buildFlags(builder: F.() -> Unit): F = createFlags().apply(builder)

    // Wrapper implementations
    override fun getLinked(): Union<API<S, F>, Wrapper<API<S, F>>> = this.toUnion2()
    override fun targetClass(): KClass<*> = this::class
}

/**
 * Configure the settings for this API.
 */
inline fun <Self: API<S, F>, S: Settings, F: Flags<*>> Self.configure(block: S.() -> Unit): Self = apply { block(settings) }

abstract class HTTPAPI<S: Settings, F: Flags<*>>(settings: S): API<S, F>(settings) {
    val client = createKtorClient()

    protected abstract fun getApiUrl(path: String): String

    protected suspend fun makeHttpGet(path: String): Result<HttpResponse> {
        return wrapTryCatchToResult {
            client.get(getApiUrl(path)) {
                settings.applyToRequest(this)
            }
        }
    }

    protected suspend fun makeHttpPost(path: String, flags: F, extraSettings: (HttpRequestBuilder) -> Unit = {}, block: HashMap<String, JsonElement>.() -> Unit = {}): Result<HttpResponse> {
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

    protected suspend fun makeHttpSSEPost(path: String, flags: F, extraSettings: (HttpRequestBuilder) -> Unit = {}, block: suspend ClientSSESession.() -> Unit): Result<Unit> {
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
}
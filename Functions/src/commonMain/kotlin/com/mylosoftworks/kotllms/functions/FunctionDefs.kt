package com.mylosoftworks.kotllms.functions

import com.mylosoftworks.gbnfkotlin.GBNF
import com.mylosoftworks.gbnfkotlin.entries.GBNFEntity
import com.mylosoftworks.kotllms.*
import com.mylosoftworks.kotllms.api.API
import com.mylosoftworks.kotllms.api.GenerationResult
import com.mylosoftworks.kotllms.api.SeparateToolCalls
import com.mylosoftworks.kotllms.features.Flags
import com.mylosoftworks.kotllms.chat.ChatDef
import com.mylosoftworks.kotllms.chat.ChatMessage
import com.mylosoftworks.kotllms.features.flagsimpl.FlagGrammarGBNF
import com.mylosoftworks.kotllms.features.flagsimpl.FlagStructuredResponse
import com.mylosoftworks.kotllms.features.flagsimpl.FlagTools
import com.mylosoftworks.kotllms.features.impl.ChatGen
import com.mylosoftworks.kotllms.features.stringOrToString
import com.mylosoftworks.kotllms.jsonschema.JsonSchema
import com.mylosoftworks.kotllms.jsonschema.JsonSchemaRule
import com.mylosoftworks.kotllms.jsonschema.rules.JsonSchemaArray
import com.mylosoftworks.kotllms.jsonschema.rules.JsonSchemaObject
import com.mylosoftworks.kotllms.jsonschema.rules.JsonType
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Base class for function definitions.
 *
 * For function calls, the LLM is told a few things:
 */
class FunctionDefs(
    val grammarDef: FunctionGrammarDef? = null,
    val functionInfoDef: FunctionInfoDef = DefaultFunctionInfoDef,
    initFunctions: FunctionDefs.() -> Unit = {}
) {
    val functions: HashMap<String, FunctionDefinition<*>> = hashMapOf()

    init {
        initFunctions()
    }

    fun <T> function(name: String, comment: String? = null, initParams: FunctionDefinition<T>.() -> Unit = {}): FunctionDefinition<T> {
        val func = FunctionDefinition(name, comment, initParams)
        functions[name] = func
        return func
    }


    fun getDescriptionForAllCalls(): String {
        return functionInfoDef.getInfoForAllFunctions(this)
    }

    /**
     * Returns a GBNF object containing the grammar defined in the associated grammarDef.
     */
    fun getFunctionsGrammar(): GBNF {
        return grammarDef?.getFunctionsGrammar(this) ?: JsonSchema("", schema = getFunctionsJson()).buildGBNF()
    }

    /**
     * ```
     * // Example from OpenAI api docs
     * [{
     *     "type": "function",
     *     "function": {
     *         "name": "get_weather",
     *         "arguments": {"location":"Paris, France"}
     *     }
     * }]
     *
     * // In OpenAI api docs, arguments is a string containing a json object. We can use .stringOrToString() to get the same result for both.
     * ```
     */
    fun getFunctionsJson(): JsonSchemaRule {
        return JsonSchemaArray(JsonSchemaObject {
            addRule("type", JsonType.String("function"))
            addAnyOf("function") { // since anyOf is not supported as root
                functions.forEach {(key, value) ->
                    addObject {
                        addRule("name", JsonType.String(key))
//                        addObject("arguments") {
//                            // Can be parsed using .stringOrToString(), to get an object containing the parameters, compatible with both tool json (string arguments) and grammar json (json arguments)
//                            value.params.forEach {(pKey, pValue) ->
//                                add(pKey, pValue.addJsonRule(), !pValue.optional)
//                            }
//                        }
                        addRule("arguments", value.getFunctionParamsJson())
                    }
                }
            }
        })
    }

    /**
     * Request a generation from the LLM in the format specified by the grammarDef. And returns the result object,
     * doing the generation in 2 steps allows streaming a response, and then getting the function calls from that response.
     *
     * If you only need to get the function calls from a request,
     *
     * @param api The [ChatGen] API which is used to generate the response.
     * @param flags The flags to give to the generation, or null for default.
     *
     * @param M The message type that the chat type requires.
     * @param GM The given message type, which extends the message type that ChatGen requires [M].
     *
     * @return The response given by the API.
     *
     * @sample requestFunctionCallsAndParse
     */
    @Suppress("unchecked_cast")
    suspend fun <F: Flags, M: ChatMessage, GM: M, T: ChatGen<F, M>> requestFunctionCalls(api: T, flags: F?, chatDef: ChatDef<GM>): Result<GenerationResult> {
        val validFlags = flags ?: ((api as API<*, *>).createFlags() as F)


        validFlags.runIfImplR<FlagTools> {
            if (toolsSupported == false) {
                return@runIfImplR RunResult.Failed
            }

            // Official tool calling supported
            tools = functions.map {(k, v) -> JsonSchema(k, v.comment, v.getFunctionParamsJson()) }

            return@runIfImplR RunResult.Success
        }.elseIfImplRun<FlagGrammarGBNF> {
            // fallback to grammar

            grammar = getFunctionsGrammar()
        }

        return api.chatGen(chatDef, validFlags)
    }

    /**
     * Parses a response and returns the function calls.
     *
     * @return A result with a pair containing a list of the found function calls, and a string with the reasoning (if grammar supports it).
     *
     * @sample requestFunctionCallsAndParse
     */
    fun parseFunctionCall(response: String, apiSupportsGrammar: Boolean = true) =
        (if (apiSupportsGrammar) grammarDef?.parseFunctionCall(this, response) else null) ?: run {
            // json
            return@run runCatching {
                val json = jsonSettings.parseToJsonElement(response)
                val funcs = mutableListOf<suspend () -> Any?>()

                /*
                [{
                    "type": "function",
                    "function": {
                        "name": "get_weather",
                        "arguments": {"location":"Paris, France"}
                    }
                }]
                 */

                json.jsonArray.forEach {
                    val funcObj = it.jsonObject
                    if (funcObj["type"]?.jsonPrimitive?.content == "function") {
                        val func = funcObj["function"]?.jsonObject
                        val name = func?.get("name")?.jsonPrimitive?.content ?: return@forEach
                        val functionToCall = functions[name] ?: return@forEach
                        val arguments = (func["arguments"] ?: return@forEach).stringOrToString()
                        val argsReParsed = jsonSettings.parseToJsonElement(arguments).jsonObject // Since OpenAI returns as string with json object

                        val args = hashMapOf<String, Pair<FunctionParameter<*>, Any?>>()
                        argsReParsed.forEach {(k, v) ->
                            val parameter = functionToCall.params[k] ?: return@forEach
                            args[k] = parameter to parameter.parseProvidedParams(v.toString())
                        }

                        funcs.add({functionToCall.runCallBackWithParams(args)})
                    }
                }

                return@runCatching funcs
            }
        }

    /**
     * Request function calls based on a chat input, and parse those function calls.
     *
     * @param api The [ChatGen] API which is used to generate the response.
     * @param flags The flags to give to the generation, or null for default.
     *
     * @param M The message type that the chat type requires.
     * @param GM The given message type, which extends the message type that ChatGen requires [M].
     */
    suspend fun <F: Flags, M: ChatMessage, GM: M, T: ChatGen<F, M>> requestFunctionCallsAndParse(api: T, flags: F?, chatDef: ChatDef<GM>): Result<Pair<List<(suspend () -> Any?)>, String>> {
        // Get a response from the llm following the grammar
        val response = requestFunctionCalls(api, flags, chatDef).getOrElse { return Result.failure(it) }

        response.runIfImpl<SeparateToolCalls> {
            // In case the message has content for tool calls
//            responseRelevant = getToolCallsString() ?: return Result.success(listOf<(suspend () -> Any?)>() to response.getText())
            return Result.success(parseFunctionCall(
                getToolCallsString() ?: return Result.success(listOf<(suspend () -> Any?)>() to response.getText())
            ).getOrElse { return Result.failure(it) } to response.getText())
        }.elseRun {
            // In case the message does not have separate content for tool calls
//            responseRelevant = response.getText()
            return Result.success(parseFunctionCall(response.getText()).getOrElse { return Result.failure(it) } to response.getText())
        }

        error("Impossible to reach")
        // Parse the function call(s) from the response
//        return Result.success(parseFunctionCall(responseRelevant).getOrElse { return Result.failure(it) } to response.getText())
    }
}

class FunctionDefinition<T>(val name: String, val comment: String? = null, initParams: FunctionDefinition<T>.() -> Unit = {}) {
    val params: HashMap<String, FunctionParameter<*>> = hashMapOf()
    var callback: (suspend (HashMap<String, Pair<FunctionParameter<*>, Any?>>) -> T)? = null

    init {
        initParams()
    }

    fun <T: FunctionParameter<*>> addParam(param: T): T {
        params[param.name] = (param)
        return param
    }

    suspend fun runCallBackWithParams(params: HashMap<String, Pair<FunctionParameter<*>, Any?>>): T? {
        val nonNullCB = callback ?: return null
        return nonNullCB(params)
    }

    fun getFunctionParamsJson(): JsonSchemaRule {
        return JsonSchemaObject {
            params.forEach {(pKey, pValue) ->
                add(pKey, pValue.addJsonRule(), !pValue.optional)
            }
        }
    }
}

abstract class FunctionParameter<T>(var name: String, var optional: Boolean = false, var comment: String? = null, val typeName: String) {
    abstract fun addGBNFRule(gbnf: GBNFEntity)
    abstract fun addJsonRule(): JsonSchemaRule
    abstract fun parseProvidedParams(string: String): T

    operator fun invoke(map: HashMap<String, Pair<FunctionParameter<*>, Any?>>): T? {
        return map[name]?.second as T?
    }
}

class FunctionParameterString(name: String, optional: Boolean = false, comment: String? = null, val maxLength: Int = 999999, val multiline: Boolean = false) : FunctionParameter<String>(name, optional, comment, "String") {
    override fun addGBNFRule(gbnf: GBNFEntity) {
        gbnf.run {
            literal("\"")
            repeat(max = maxLength) {
                oneOf {
                    literal("\\\\")
                    literal("\\\"") // Should allow escaping without triggering end of string
                    if (multiline) range("\"", true) // [^"]
                    else range("\"\n", true) // [^"\n]
                }
            }
            literal("\"")
        }
    }

    override fun addJsonRule(): JsonSchemaRule = JsonType.String()

    override fun parseProvidedParams(string: String): String {
        return string.substring(1, string.length - 1)
    }
}

class FunctionParameterInt(name: String, optional: Boolean = false, comment: String? = null) : FunctionParameter<Int>(name, optional, comment, "int") {
    override fun addGBNFRule(gbnf: GBNFEntity) {
        gbnf.run {
            optional { literal("-") } // Can be negative
            repeat(0, 10) { range("[0-9]") } // Max 10 digits
        }
    }

    override fun parseProvidedParams(string: String): Int {
        return string.toInt()
    }

    override fun addJsonRule(): JsonSchemaRule = JsonType.Integer()
}

class FunctionParameterUInt(name: String, optional: Boolean = false, comment: String? = null) : FunctionParameter<UInt>(name, optional, comment, "uint") {
    override fun addGBNFRule(gbnf: GBNFEntity) {
        gbnf.run {
            repeat(0, 10) { range("[0-9]") } // Max 10 digits
        }
    }

    override fun parseProvidedParams(string: String): UInt {
        return string.toUInt()
    }

    override fun addJsonRule(): JsonSchemaRule = JsonType.Integer()
}

class FunctionParameterFloat(name: String, optional: Boolean = false, comment: String? = null) : FunctionParameter<Float>(name, optional, comment, "float") {
    override fun addGBNFRule(gbnf: GBNFEntity) {
        gbnf.run {
            optional { literal("-") }
            optional { repeat(0, 10) { range("[0-9]") } }
            optional { literal(".") }
            optional { repeat(0, 10) { range("[0-9]") } }
        }
    }

    override fun parseProvidedParams(string: String): Float {
        return string.toFloat()
    }

    override fun addJsonRule(): JsonSchemaRule = JsonType.Number()
}

class FunctionParameterBool(name: String, optional: Boolean = false, comment: String? = null) : FunctionParameter<Boolean>(name, optional, comment, "boolean") {
    override fun addGBNFRule(gbnf: GBNFEntity) {
        gbnf.apply {
            oneOf {
                +"true"
                +"false"
            }
        }
    }

    override fun addJsonRule(): JsonSchemaRule = JsonType.Boolean()

    override fun parseProvidedParams(string: String): Boolean = string == "true"
}
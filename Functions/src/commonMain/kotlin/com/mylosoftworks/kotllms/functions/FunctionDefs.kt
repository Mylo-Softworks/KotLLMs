package com.mylosoftworks.kotllms.functions

import com.mylosoftworks.gbnfkotlin.GBNF
import com.mylosoftworks.gbnfkotlin.entries.GBNFEntity
import com.mylosoftworks.kotllms.api.API
import com.mylosoftworks.kotllms.api.GenerationResult
import com.mylosoftworks.kotllms.features.Flags
import com.mylosoftworks.kotllms.chat.ChatDef
import com.mylosoftworks.kotllms.chat.ChatMessage
import com.mylosoftworks.kotllms.features.flagsimpl.FlagGrammarGBNF
import com.mylosoftworks.kotllms.features.impl.ChatGen
import com.mylosoftworks.kotllms.runIfImpl

/**
 * Base class for function definitions.
 *
 * For function calls, the LLM is told a few things:
 */
class FunctionDefs(
    val grammarDef: FunctionGrammarDef = DefaultFunctionGrammar(100),
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
        return grammarDef.getFunctionsGrammar(this)
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

        validFlags.runIfImpl<FlagGrammarGBNF> {
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
    fun parseFunctionCall(response: String) = grammarDef.parseFunctionCall(this, response)

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
        val response = requestFunctionCalls(api, flags, chatDef).getOrElse { return Result.failure(it) }.getText()

        // Parse the function call(s) from the response
        return parseFunctionCall(response)
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
}

abstract class FunctionParameter<T>(var name: String, var optional: Boolean = false, var comment: String? = null, val typeName: String) {
    abstract fun addGBNFRule(gbnf: GBNFEntity)
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

    override fun parseProvidedParams(string: String): String {
        return string.substring(1, string.length - 1)
    }
}

class FunctionParameterInt(name: String, optional: Boolean = false, comment: String? = null) : FunctionParameter<Int>(name, optional, comment, "Int") {
    override fun addGBNFRule(gbnf: GBNFEntity) {
        gbnf.run {
            optional { literal("-") } // Can be negative
            repeat(0, 10) { range("[0-9]") } // Max 10 digits
        }
    }

    override fun parseProvidedParams(string: String): Int {
        return string.toInt()
    }
}

class FunctionParameterUInt(name: String, optional: Boolean = false, comment: String? = null) : FunctionParameter<UInt>(name, optional, comment, "UInt") {
    override fun addGBNFRule(gbnf: GBNFEntity) {
        gbnf.run {
            repeat(0, 10) { range("[0-9]") } // Max 10 digits
        }
    }

    override fun parseProvidedParams(string: String): UInt {
        return string.toUInt()
    }
}

class FunctionParameterFloat(name: String, optional: Boolean = false, comment: String? = null) : FunctionParameter<Float>(name, optional, comment, "Float") {
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
}
package com.mylosoftworks.kotllms.functions

import com.mylosoftworks.gbnfkotlin.GBNF
import com.mylosoftworks.gbnfkotlin.entries.GBNFEntity
import com.mylosoftworks.kotllms.api.Flags
import com.mylosoftworks.kotllms.chat.ChatDef
import com.mylosoftworks.kotllms.features.impl.ChatGen

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
     * Returns a GBNF object containing the grammar needed to pick a function to call and parameters in one go.
     */
    fun getGrammarForAllCallsSingleRequest(): GBNF {
        return grammarDef.getFunctionsGrammar(this)
    }

    /**
     * Create the function call in a single request, uses a different grammar, makes longer responses
     */
    suspend fun <F: Flags<F>, D: ChatDef<*>, T : ChatGen<F, D>> requestFunctionCallSingleRequest(api: T, flags: F, chatDef: D): Result<Triple<String, (suspend () -> Any?)?, String>> {
        flags.applyGrammar(getGrammarForAllCallsSingleRequest())
        flags.enableEarlyStopping(false)
        val response = api.chatGen(chatDef, flags).getText()

        return grammarDef.parseFunctionCall(this, response)
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

class FunctionParameterString(name: String, optional: Boolean = false, comment: String? = null, val maxLength: Int = 999999) : FunctionParameter<String>(name, optional, comment, "String") {
    override fun addGBNFRule(gbnf: GBNFEntity) {
        gbnf.run {
            literal("\"")
            repeat(max = maxLength) {
                oneOf {
                    literal("\\\"") // Should allow escaping without triggering end of string
                    range("\"\\n", true) // [^"\n]
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
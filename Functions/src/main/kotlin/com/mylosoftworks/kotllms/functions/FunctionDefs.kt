package com.mylosoftworks.kotllms.functions

import com.mylosoftworks.com.mylosoftworks.gbnfkotlin.GBNF
import com.mylosoftworks.com.mylosoftworks.gbnfkotlin.entries.GBNFEntity
import com.mylosoftworks.kotllms.api.Flags
import com.mylosoftworks.kotllms.chat.ChatDef
import com.mylosoftworks.kotllms.features.impl.ChatGen

/**
 * Base class for function definitions.
 *
 * For function calls, the LLM is told a few things:
 */
class FunctionDefs(val grammarDef: FunctionGrammarDef = DefaultFunctionGrammar(100), initFunctions: FunctionDefs.() -> Unit = {}) {
    val functions: HashMap<String, FunctionDefinition> = hashMapOf()

    init {
        initFunctions()
    }

    fun function(name: String, comment: String? = null, initParams: FunctionDefinition.() -> Unit = {}) {
        functions[name] = FunctionDefinition(name, comment, initParams)
    }


    fun getDescriptionForAllCalls(): String {
        return functions.values.joinToString(",", "{", "}") {
            it.getShortDescriptionForFunction()
        }
    }

    fun getDetailedDescriptionForAllCalls(): String {
        return functions.values.joinToString(",", "{", "}") {
            it.getExtendedDescriptionForFunction()
        }
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
    suspend fun <F: Flags<F>, D: ChatDef<*>, T : ChatGen<F, D>> requestFunctionCallSingleRequest(api: T, flags: F, chatDef: D): Triple<String, (suspend () -> Unit)?, String> {
        flags.applyGrammar(getGrammarForAllCallsSingleRequest())
        flags.enableEarlyStopping(false)
        val response = api.chatGen(chatDef, flags).getText()

        return grammarDef.parseFunctionCall(this, response)
    }
}

class FunctionDefinition(val name: String, val comment: String? = null, initParams: FunctionDefinition.() -> Unit = {}) {
    val params: HashMap<String, FunctionParameter<*>> = hashMapOf()
    var callback: suspend (HashMap<String, Pair<FunctionParameter<*>, Any?>>) -> Unit = {}

    init {
        initParams()
    }

    fun addParam(param: FunctionParameter<*>) {
        params[param.name] = (param)
    }

    fun getShortDescriptionForFunction(): String {
        return "$name: {${if (comment != null) "comment: \"$comment\", " else ""}parameters: ${params.values.joinToString(", ", "[", "]") { it.getBasicDescription() }}}"
    }

    fun getExtendedDescriptionForFunction(): String {
        return "$name: {${if (comment != null) "comment: \"$comment\", " else ""}parameters: ${params.values.joinToString(", ", "[", "]") { it.getDetailedDescription() }}}"
    }
}

abstract class FunctionParameter<T>(var name: String, var optional: Boolean = false, var comment: String? = null, val typeName: String) {

    abstract fun addGBNFRule(gbnf: GBNFEntity)
    abstract fun parseProvidedParams(string: String): T

    fun getBasicDescription(): String {
        return "{$name: $typeName}"
    }

    fun getDetailedDescription(): String {
        return "{$name: {${if (comment != null) "comment: \"$comment\"," else ""}type: $typeName}}"
    }
}

class FunctionParameterString(name: String, optional: Boolean = false, comment: String? = null, val maxLength: Int = 999999) : FunctionParameter<String>(name, optional, comment, "String") {
    override fun addGBNFRule(gbnf: GBNFEntity) {
        gbnf.run {
            literal("\"")
            repeat(max = maxLength) {
                oneOf {
                    range("\"\\n", true) // [^"\n]
                    literal("\\\"") // Should allow escaping without triggering end of string
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
package com.mylosoftworks.kotllms.functions

import com.mylosoftworks.com.mylosoftworks.gbnfkotlin.GBNF
import com.mylosoftworks.com.mylosoftworks.gbnfkotlin.entries.GBNFEntity
import com.mylosoftworks.kotllms.api.Flags
import com.mylosoftworks.kotllms.chat.ChatDef
import com.mylosoftworks.kotllms.features.impl.ChatGen

val callRegex = Regex("Thoughts: \"(.*?)\"\\nCall: (.+)") // Single match, picked before function is selected to select a function
val paramsRegex = Regex("(.*?): (.*)") // Repeated match, picked when function is selected to specify parameters

/**
 * Base class for function definitions.
 *
 * For function calls, the LLM is told a few things:
 *
 * First, the LLM is provided with basic function signatures
 * ```
 * {
 * exampleFunction: {
 *   comment: "an example function, no purpose",
 *   parameters: [{param1: Int}, {param2: String}]
 *  }
 * }
 * ```
 *
 * The LLM is then able to select one function to call by name using GBNF `root ::= exampleFunction | exampleFunction2`
 * The result will be the function that the LLM wants to call, now the LLM can be provided with a detailed description of the function call.
 * exampleFunction: {
 *  comment: "an example function, no purpose",
 *  parameters: [{}]
 * }
 */
class FunctionDefs(initFunctions: FunctionDefs.() -> Unit = {}) {
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

    /**
     * Returns a GBNF object containing the grammar needed to pick a function to call.
     */
    fun getGrammarForAllCalls(): GBNF {
        return GBNF {
            literal("Thoughts: \"")
            anyCount { range("\\\"", true) }
            literal("\"\nCall: ")
            oneOf {
                this@FunctionDefs.functions.forEach {
                    literal(it.key)
                }
            }
        }
    }

    fun getTargettedFunctionFromResponse(fullResponse: String): Pair<FunctionDefinition?, String> {
        val regexResult = callRegex.find(fullResponse)
        val thoughts = regexResult?.groups?.get(1)?.value ?: "" // 1 is thoughts
        val call = regexResult?.groups?.get(2)?.value // 2 is function name
        return functions[call] to thoughts
    }

    /**
     * Make sure you have a high token count for generation.
     * Include info about which functions are available through getDescriptionForAllCalls()
     * Grammar is attempted to be set through the applyGrammar function on the flags
     */
    suspend fun <F: Flags<F>, D: ChatDef<*>, T : ChatGen<F, D>> requestFunctionCall(api: T, flags: F, chatDef: D): Triple<String, FunctionDefinition?, String> {
        flags.applyGrammar(getGrammarForAllCalls()) // Force the LLM to pick a function from the available ones
        flags.enableEarlyStopping(false) // Force the LLM to continue generating until the message is complete (or token max is reached)
        val response = api.chatGen(chatDef, flags).getText()
        val target = getTargettedFunctionFromResponse(response)
        return Triple(response, target.first, target.second)
    }
}

class FunctionDefinition(val name: String, val comment: String? = null, initParams: FunctionDefinition.() -> Unit = {}) {
    val params: HashMap<String, FunctionParameter<*>> = hashMapOf()
    var callback: (HashMap<String, Pair<FunctionParameter<*>, Any?>>) -> Unit = {}

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

    /**
     * Assuming the bot has already given a target function, obtain the parameters it wants to use.
     * Parameters are always in order and named, thoughts are already written.
     *
     * ```
     * paramname: value
     * param2name: value
     * ```
     */
    fun getGrammarForFunction(): GBNF {
        return GBNF {
            for (param in params.values) {
                param.addGBNFRuleLine(this)
            }
        }
    }

    fun getParametersFromResponse(fullResponse: String): HashMap<String, Pair<FunctionParameter<*>, Any?>> {
        val regexMatches = paramsRegex.findAll(fullResponse)
        val outputMap = HashMap<String, Pair<FunctionParameter<*>, Any?>>()

        for (match in regexMatches) {
            val name = match.groups[1]!!.value
            val value = match.groups[2]!!.value

            val parameter = params[name]!!
            outputMap[name] = parameter to parameter.parseProvidedParams(value)
        }

        return outputMap
    }

    fun callFunctionWithResponse(fullResponse: String) {
        val params = getParametersFromResponse(fullResponse)
        callback(params)
    }

    /**
     * Don't forget to add getExtendedDescriptionForFunction() to the chat context, so the LLM knows what the parameters are.
     */
    suspend fun <F: Flags<F>, D: ChatDef<*>, T : ChatGen<F, D>> requestCallFunction(api: T, flags: F, chatDef: D): Pair<String, () -> Unit> {
        flags.applyGrammar(getGrammarForFunction()) // Force the LLM to generate valid grammar
        flags.enableEarlyStopping(false) // Force the LLM to continue generating until the message is complete (or token max is reached)
        val response = api.chatGen(chatDef, flags).getText()
        return response to { callFunctionWithResponse(response) }
    }
}

abstract class FunctionParameter<T>(var name: String, var optional: Boolean = false, var comment: String? = null, val typeName: String) {

    abstract fun addGBNFRule(gbnf: GBNFEntity)
    abstract fun parseProvidedParams(string: String): T

    fun addGBNFRuleLine(gbnf: GBNFEntity) {
        gbnf.run {
            if (optional) {
                optional {
                    literal("$name: ")
                    addGBNFRule(this)
                    literal("\n")
                }
            }
            else {
                literal("$name: ")
                addGBNFRule(this)
                literal("\n")
            }
        }
    }

    fun getBasicDescription(): String {
        return "{$name: $typeName}"
    }

    fun getDetailedDescription(): String {
        return "{$name: {${if (comment != null) "comment: \"$comment\"," else ""}type: $typeName}}"
    }
}

class FunctionParameterString(name: String, optional: Boolean = false, comment: String? = null) : FunctionParameter<String>(name, optional, comment, "String") {
    override fun addGBNFRule(gbnf: GBNFEntity) {
        gbnf.run {
            literal("\"")
            anyCount {
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
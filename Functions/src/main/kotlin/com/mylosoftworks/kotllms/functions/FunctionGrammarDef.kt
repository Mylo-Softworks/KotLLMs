package com.mylosoftworks.kotllms.functions

import com.mylosoftworks.com.mylosoftworks.gbnfkotlin.GBNF
import com.mylosoftworks.com.mylosoftworks.gbnfkotlin.entries.GBNFEntity

/**
 * A grammar definition used for function calling, used for parsing function calls etc
 */
abstract class FunctionGrammarDef {
    abstract fun parseFunctionCall(defs: FunctionDefs, response: String): Triple<String, (suspend () -> Unit)?, String>
    abstract fun getFunctionsGrammar(defs: FunctionDefs): GBNF
}

class DefaultFunctionGrammar(val maxThoughtLength: Int = 100) : FunctionGrammarDef() {
    val callRegex = Regex("Thoughts: \"(.*?)\"\\nCall: (.+)") // Single match, picked before function is selected to select a function
    val paramsRegex = Regex("(.*?): (.*)") // Repeated match, picked when function is selected to specify parameters
    val paramsSplit = "--params--" // String used to split parameters from function selection, uses a single line, only used for single request function calling

    override fun parseFunctionCall(
        defs: FunctionDefs,
        response: String
    ): Triple<String, (suspend () -> Unit)?, String> {
        val (funcAndComment, params) = response.split("\n$paramsSplit\n")
        val (function, thoughts) = getTargettedFunctionFromResponse(defs, funcAndComment)
        return Triple(response, function?.let { { it.callback(getParametersFromResponse(it, params)) } }, thoughts)
    }

    fun getTargettedFunctionFromResponse(defs: FunctionDefs, fullResponse: String): Pair<FunctionDefinition?, String> {
        val regexResult = callRegex.find(fullResponse)
        val thoughts = regexResult?.groups?.get(1)?.value ?: "" // 1 is thoughts
        val call = regexResult?.groups?.get(2)?.value // 2 is function name
        return defs.functions[call] to thoughts
    }

    override fun getFunctionsGrammar(defs: FunctionDefs): GBNF {
        return GBNF {
            literal("Thoughts: \"")
            repeat(max = maxThoughtLength) { range("\\\"\\n", true) }
            literal("\"\nCall: ")
            val entities = mutableListOf<GBNFEntity>()
            defs.functions.values.forEach {
                entity(it.name) { // Create a named entity
                    literal(it.name + "\n$paramsSplit\n") // Call: name\n$paramsSplit\n
                    applyGrammarForFunction(it, this)
                }.let { entities.add(it) }
            }
            oneOf { // Pick from function grammars
                entities.forEach {
                    it()
                }
            }
        }
    }

    fun applyGrammarForFunction(funcDef: FunctionDefinition, entity: GBNFEntity) {
        entity.apply {
            for (param in funcDef.params.values) {
                if (param.optional) {
                    optional {
                        literal("${param.name}: ")
                        param.addGBNFRule(this)
                        literal("\n")
                    }
                }
                else {
                    literal("${param.name}: ")
                    param.addGBNFRule(this)
                    literal("\n")
                }
            }
        }
    }

    fun getParametersFromResponse(funcDef: FunctionDefinition, fullResponse: String): HashMap<String, Pair<FunctionParameter<*>, Any?>> {
        val regexMatches = paramsRegex.findAll(fullResponse)
        val outputMap = HashMap<String, Pair<FunctionParameter<*>, Any?>>()

        for (match in regexMatches) {
            val name = match.groups[1]!!.value
            val value = match.groups[2]!!.value

            val parameter = funcDef.params[name]!!
            outputMap[name] = parameter to parameter.parseProvidedParams(value)
        }

        return outputMap
    }
}
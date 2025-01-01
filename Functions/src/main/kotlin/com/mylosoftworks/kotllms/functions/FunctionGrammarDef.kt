package com.mylosoftworks.kotllms.functions

import com.mylosoftworks.gbnfkotlin.GBNF
import com.mylosoftworks.gbnfkotlin.entries.GBNFEntity

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

/**
 * Rules for correct parsing:
 *
 * 1. Thoughts should be stored in an entity named "thoughts" (fallback to empty string, in which case you won't be able to read it back)
 * 2. Functions should be stored in an entity named "function_{name}" where {name} is the name of the function.
 * 3. (The parsable value part of) function parameters should be stored in an entity named "param_{function}_{name}", inside of the function entity where {function} is the function name (to prevent name collisions) and {name} is the name of the parameter.
 */
class AutoParsedGrammarDef(val create: GBNF.(FunctionDefs) -> Unit) : FunctionGrammarDef() {
    override fun parseFunctionCall(
        defs: FunctionDefs,
        response: String
    ): Triple<String, (suspend () -> Unit)?, String> {
        val grammar = getFunctionsGrammar(defs)

        val (parsedTree, _) = grammar.parse(response)

        val thoughtsOrEmpty = parsedTree.find { it.isNamedEntity("thoughts") }?.strValue ?: ""

        // Find which function was called
        val functionCall = parsedTree.find(includeSelf = false) {parsed ->
            val entry = parsed.getAsEntityIfPossible() ?: return@find false
            return@find entry.identifier?.startsWith("function_") ?: false
        } ?: return Triple(response, null, thoughtsOrEmpty)

        val functionName = (functionCall.associatedEntry as GBNFEntity).identifier!!.substring("function_".length)
        val function = defs.functions[functionName] ?: return Triple(response, null, thoughtsOrEmpty)
        val paramsMap = hashMapOf<String, Pair<FunctionParameter<*>, Any?>>()
        // Find all defined parameters
        val paramPrefix = "param_${function.name}_"
        val definedParams = functionCall.findAll {parsed ->
            val entry = parsed.getAsEntityIfPossible() ?: return@findAll false
            return@findAll entry.identifier?.startsWith(paramPrefix) ?: false
        }

        definedParams.map {
            val paramName = (it.associatedEntry as GBNFEntity).identifier!!.substring(paramPrefix.length)
            val param = function.params[paramName] ?: return Triple(response, null, thoughtsOrEmpty)
            paramsMap[paramName] = param to param.parseProvidedParams(it.strValue)
        }

        return Triple(response, { function.callback(paramsMap) }, thoughtsOrEmpty)
    }

    override fun getFunctionsGrammar(defs: FunctionDefs): GBNF {
        val base = GBNF {
            create(this, defs)
        }
        return base
    }
}
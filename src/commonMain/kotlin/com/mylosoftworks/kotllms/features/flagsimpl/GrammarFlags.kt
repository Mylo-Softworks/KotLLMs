package com.mylosoftworks.kotllms.features.flagsimpl

import com.mylosoftworks.gbnfkotlin.GBNF
import com.mylosoftworks.kotllms.jsonschema.JsonSchema

// Flags for grammar (Currently only GBNF grammars are supported)

interface FlagGrammarGBNF {
    var grammar: GBNF?
    fun setGbnfGrammarRaw(gbnf: String?)
}

interface FlagStructuredResponse {
    var responseFormat: JsonSchema?
}

interface FlagTools {
    var tools: List<JsonSchema>? // Tools can be defined as json schemas (which include name, description and rules)
    var toolsSupported: Boolean? // Indicates if tools are supported, useful for overriding
}

interface FlagToolsParallel {
    var parallelToolCalls: Boolean?
}

enum class ToolChoice(val strVal: String) {
    Auto("auto"), Required("required"), None("none")
}

interface FlagToolChoice {
    var toolChoice: ToolChoice?
}
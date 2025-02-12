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
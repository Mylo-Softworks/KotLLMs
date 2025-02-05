package com.mylosoftworks.kotllms.features.flagsimpl

import com.mylosoftworks.gbnfkotlin.GBNF

// Flags for grammar (Currently only GBNF grammars are supported)

interface FlagGrammarGBNF {
    var grammar: GBNF?
    fun setGbnfGrammarRaw(gbnf: String?)
}

// TODO: Support response format (Like in OpenAI api). Integrate with serialization to allow simple kotlin parsing.
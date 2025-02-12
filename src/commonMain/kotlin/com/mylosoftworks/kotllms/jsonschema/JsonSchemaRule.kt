package com.mylosoftworks.kotllms.jsonschema

import com.mylosoftworks.gbnfkotlin.GBNF
import com.mylosoftworks.kotllms.features.ToJson
import kotlinx.serialization.json.JsonElement

/**
 * Represents a single rule from a json schema, which can be built to a Json Element.
 */
abstract class JsonSchemaRule: ToJson {
    abstract fun build(): JsonElement

    override fun toJson(): JsonElement = build()

    fun buildGBNF(gbnf: GBNF) = gbnf.buildGBNF() // For outside access
    abstract fun GBNF.buildGBNF() // For easy implementation (automatic this receiver)
}
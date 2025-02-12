package com.mylosoftworks.kotllms.jsonschema.rules

import com.mylosoftworks.gbnfkotlin.GBNF
import com.mylosoftworks.kotllms.jsonschema.JsonSchemaRule
import kotlinx.serialization.json.JsonElement

/**
 * A json schema entry representing a Json element, this is helpful for adding primitives.
 */
class JsonSchemaJsonElement(val element: JsonElement): JsonSchemaRule() {
    override fun build(): JsonElement {
        return element
    }

    override fun GBNF.buildGBNF() {
        TODO("Not yet implemented")
    }
}
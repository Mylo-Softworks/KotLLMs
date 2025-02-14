package com.mylosoftworks.kotllms.jsonschema.rules

import com.mylosoftworks.gbnfkotlin.entries.GBNFEntity
import com.mylosoftworks.kotllms.jsonschema.CommonDefs
import com.mylosoftworks.kotllms.jsonschema.JsonSchemaRule
import kotlinx.serialization.json.JsonElement

/**
 * A json schema entry representing a Json element, this is helpful for adding primitives.
 */
class JsonSchemaJsonElement(val element: JsonElement, val gbnf: GBNFEntity.() -> Unit): JsonSchemaRule() {
    override fun build(): JsonElement {
        return element
    }

    override fun GBNFEntity.buildGBNF(commonDefs: CommonDefs) {
        gbnf()
    }

    override fun fillIfMissing(jsonElement: JsonElement?): Pair<JsonElement?, Int> {
        return jsonElement to 0
    }
}
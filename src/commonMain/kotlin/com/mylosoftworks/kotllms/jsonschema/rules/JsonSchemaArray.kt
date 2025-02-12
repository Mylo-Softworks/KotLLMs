package com.mylosoftworks.kotllms.jsonschema.rules

import com.mylosoftworks.gbnfkotlin.GBNF
import com.mylosoftworks.kotllms.features.toJson
import com.mylosoftworks.kotllms.jsonschema.JsonSchemaRule
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

open class JsonSchemaArray(val items: JsonSchemaRule): JsonSchemaRule() {

    constructor(schema: JsonType): this(schema.type)

    override fun build(): JsonElement {
        return JsonObject(mapOf(
            "type" to "array".toJson(),
            "items" to items.build()
        ))
    }

    override fun GBNF.buildGBNF() {
        TODO("Not yet implemented")
    }
}
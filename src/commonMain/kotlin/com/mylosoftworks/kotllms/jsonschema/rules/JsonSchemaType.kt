package com.mylosoftworks.kotllms.jsonschema.rules

import com.mylosoftworks.gbnfkotlin.GBNF
import com.mylosoftworks.kotllms.features.toJson
import com.mylosoftworks.kotllms.jsonschema.JsonSchemaRule
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

class JsonSchemaType(val type: String, val gbnf: GBNF.() -> Unit): JsonSchemaRule() {
    override fun build(): JsonElement {
        return JsonObject(mapOf(
            "type" to type.toJson()
        ))
    }

    override fun GBNF.buildGBNF() {
        gbnf()
    }
}

enum class JsonType(val type: JsonSchemaType) {
    String(JsonSchemaType("string") {TODO()}),
    Number(JsonSchemaType("number") {TODO()}),
    Integer(JsonSchemaType("integer") {TODO()}),
    Boolean(JsonSchemaType("boolean") {TODO()}),
    Null(JsonSchemaType("null") {TODO()})
    ; // Don't include array or object, those should be used with JsonSchemaArray and JsonSchemaObject
}
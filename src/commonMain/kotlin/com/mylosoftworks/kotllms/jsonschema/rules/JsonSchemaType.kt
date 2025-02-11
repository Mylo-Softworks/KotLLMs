package com.mylosoftworks.kotllms.jsonschema.rules

import com.mylosoftworks.kotllms.features.toJson
import com.mylosoftworks.kotllms.jsonschema.JsonSchemaRule
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

class JsonSchemaType(val type: String): JsonSchemaRule() {
    override fun build(): JsonElement {
        return JsonObject(mapOf(
            "type" to type.toJson()
        ))
    }
}

enum class JsonType(val type: JsonSchemaType) {
    String(JsonSchemaType("string")),
    Number(JsonSchemaType("number")),
    Integer(JsonSchemaType("integer")),
    Boolean(JsonSchemaType("boolean")),
    Null(JsonSchemaType("null")),
    ; // Don't include array or object, those should be used with JsonSchemaArray and JsonSchemaObject
}
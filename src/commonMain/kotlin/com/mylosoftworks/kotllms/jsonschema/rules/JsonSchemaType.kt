package com.mylosoftworks.kotllms.jsonschema.rules

import com.mylosoftworks.gbnfkotlin.entries.GBNFEntity
import com.mylosoftworks.kotllms.features.toJson
import com.mylosoftworks.kotllms.jsonschema.CommonDefs
import com.mylosoftworks.kotllms.jsonschema.JsonSchemaRule
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

class JsonSchemaType(val type: String, val gbnf: GBNFEntity.(CommonDefs) -> Unit): JsonSchemaRule() {
    override fun build(): JsonElement {
        return JsonObject(mapOf(
            "type" to type.toJson()
        ))
    }

    override fun GBNFEntity.buildGBNF(commonDefs: CommonDefs) {
        gbnf(commonDefs)
    }
}

enum class JsonType(val type: JsonSchemaType) {
    String(JsonSchemaType("string") {it.string()}),
    Number(JsonSchemaType("number") {it.number()}),
    Integer(JsonSchemaType("integer") {it.integer()}),
    Boolean(JsonSchemaType("boolean") {it.boolean()}),
    Null(JsonSchemaType("null") {it.nullVal()})
    ; // Don't include array or object, those should be used with JsonSchemaArray and JsonSchemaObject
}
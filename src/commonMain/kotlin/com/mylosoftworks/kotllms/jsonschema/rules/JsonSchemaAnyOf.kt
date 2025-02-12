package com.mylosoftworks.kotllms.jsonschema.rules

import com.mylosoftworks.gbnfkotlin.GBNF
import com.mylosoftworks.kotllms.features.toJson
import com.mylosoftworks.kotllms.jsonschema.JsonSchemaRule
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

class JsonSchemaAnyOf(): JsonSchemaRule() {

    constructor(block: JsonSchemaAnyOf.() -> Unit) : this() {
        block()
    }

    val items = mutableListOf<JsonSchemaRule>()

    override fun build(): JsonElement {
        return JsonObject(hashMapOf(
            "anyOf" to items.map { it.build() }.toJson()
        ))
    }

    override fun GBNF.buildGBNF() {
        TODO("Not yet implemented")
    }

    // Functions for adding entries (NOTE: also has similar functions inside of JsonSchemaObject)
    fun addType(jsonType: JsonType) {
        items.add(jsonType.type)
    }

    fun addAnyOf(block: JsonSchemaAnyOf.() -> Unit) {
        items.add(JsonSchemaAnyOf().apply(block))
    }

    fun addObject(block: JsonSchemaObject.() -> Unit) {
        items.add(JsonSchemaObject().apply(block))
    }


    fun addRuleArray(content: JsonSchemaRule) {
        items.add(JsonSchemaArray(content))
    }

    fun addTypeArray(content: JsonType) {
        items.add(JsonSchemaArray(content.type))
    }

    fun addObjectArray(block: JsonSchemaObject.() -> Unit) {
        items.add(JsonSchemaArray(JsonSchemaObject(block)))
    }

    fun addAnyOfArray(block: JsonSchemaAnyOf.() -> Unit) {
        items.add(JsonSchemaAnyOf(block))
    }
}
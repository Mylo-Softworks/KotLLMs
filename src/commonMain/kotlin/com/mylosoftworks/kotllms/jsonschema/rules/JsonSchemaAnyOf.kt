package com.mylosoftworks.kotllms.jsonschema.rules

import com.mylosoftworks.gbnfkotlin.entries.GBNFEntity
import com.mylosoftworks.kotllms.features.toJson
import com.mylosoftworks.kotllms.jsonschema.CommonDefs
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

    override fun GBNFEntity.buildGBNF(commonDefs: CommonDefs) {
        oneOf {
            group { // Don't forget to group, otherwise all shallow children will be a union
                items.forEach { it.buildGBNF(this, commonDefs) }
            }
        }
    }

    // Functions for adding entries (NOTE: also has similar functions inside of JsonSchemaObject)
    fun addRule(jsonType: JsonSchemaRule) {
        items.add(jsonType)
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

    fun addObjectArray(block: JsonSchemaObject.() -> Unit) {
        items.add(JsonSchemaArray(JsonSchemaObject(block)))
    }

    fun addAnyOfArray(block: JsonSchemaAnyOf.() -> Unit) {
        items.add(JsonSchemaAnyOf(block))
    }
}
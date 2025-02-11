package com.mylosoftworks.kotllms.jsonschema.rules

import com.mylosoftworks.kotllms.Union
import com.mylosoftworks.kotllms.features.toJson
import com.mylosoftworks.kotllms.jsonschema.JsonSchemaRule
import com.mylosoftworks.kotllms.nonNull
import com.mylosoftworks.kotllms.toUnion1
import com.mylosoftworks.kotllms.toUnion2
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlin.js.JsName

class JsonSchemaObject(): JsonSchemaRule() {

    constructor(block: JsonSchemaObject.() -> Unit) : this() {
        block()
    }

    val properties = hashMapOf<String, JsonSchemaRule>()
    val required = mutableListOf<String>()
    @JsName("additionalPropertiesProp")
    var additionalProperties: Union<JsonSchemaRule, Boolean> = false.toUnion2()

    override fun build(): JsonElement {
        return JsonObject(mapOf(
            "type" to "object".toJson(),
            "properties" to properties.mapValues { it.value.build() }.toJson(),
            "required" to required.toJson(),
            "additionalProperties" to additionalProperties.nonNull().toJson()
        ))
    }

    fun add(name: String, value: JsonSchemaRule, required: Boolean = true) {
        properties[name] = value
        if (required) this.required.add(name)
    }

    /**
     * Enable/disable additional properties
     */
    fun additionalProperties(enable: Boolean) {
        additionalProperties = enable.toUnion2()
    }

    /**
     * Enable additional properties of type
     */
    fun additionalProperties(jsonSchemaRule: JsonSchemaRule) {
        additionalProperties = jsonSchemaRule.toUnion1()
    }

    // Functions for adding entries (NOTE: also has similar functions inside of JsonSchemaArray)
    fun addType(name: String, jsonType: JsonType, required: Boolean = true) {
        add(name, jsonType.type, required)
    }

    fun addAnyOf(name: String, required: Boolean = true, block: JsonSchemaAnyOf.() -> Unit) {
        add(name, JsonSchemaAnyOf().apply(block), required)
    }

    fun addObject(name: String, required: Boolean = true, block: JsonSchemaObject.() -> Unit) {
        add(name, JsonSchemaObject().apply(block), required)
    }

    fun addRuleArray(name: String, content: JsonSchemaRule, required: Boolean = true) {
        add(name, JsonSchemaArray(content), required)
    }

    fun addTypeArray(name: String, content: JsonType, required: Boolean = true) {
        add(name, JsonSchemaArray(content.type), required)
    }

    fun addObjectArray(name: String, required: Boolean = true, block: JsonSchemaObject.() -> Unit) {
        add(name, JsonSchemaArray(JsonSchemaObject(block)), required)
    }

    fun addAnyOfArray(name: String, required: Boolean = true, block: JsonSchemaAnyOf.() -> Unit) {
        add(name, JsonSchemaAnyOf(block), required)
    }
}
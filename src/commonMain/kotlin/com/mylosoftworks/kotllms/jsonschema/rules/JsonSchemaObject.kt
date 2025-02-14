package com.mylosoftworks.kotllms.jsonschema.rules

import com.mylosoftworks.gbnfkotlin.entries.GBNFEntity
import com.mylosoftworks.kotllms.Union
import com.mylosoftworks.kotllms.features.toJson
import com.mylosoftworks.kotllms.jsonschema.CommonDefs
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

    val properties = linkedMapOf<String, JsonSchemaRule>()
    val required = mutableListOf<String>()
    @JsName("additionalPropertiesProp")
    var additionalProperties: Union<JsonSchemaRule, Boolean> = false.toUnion2()

    override fun build(): JsonElement {
        return JsonObject(linkedMapOf(
            "type" to "object".toJson(),
            "properties" to properties.mapValues { it.value.build() }.toJson(),
            "required" to required.toJson(),
            "additionalProperties" to additionalProperties.nonNull().toJson()
        ))
    }

    override fun GBNFEntity.buildGBNF(commonDefs: CommonDefs) {
//        +"{"
//        val valsMapped = properties.map {(key, value) ->
//            (key in required) to (this.host ?: this as GBNF).entity {
//                commonDefs.whitespace()
//                +"\""
//                +key
//                +"\":"
//                commonDefs.whitespace()
//                value.buildGBNF(this, commonDefs)
//                commonDefs.whitespace()
//            }
//        }
//        var first = true
//        valsMapped.forEach {
//            if (!first) +","
//            first = false
//
//            if (it.first) {
//                it.second()
//            }
//            else {
//                optional {
//                    it.second()
//                }
//            }
//        }
//        commonDefs.whitespace()
//        +"}"
        +"{"
        var first = true
        properties.forEach {(key, value) ->
            if (!first) +","
            first = false

            if (key in required) {
                commonDefs.whitespace()
                +"\""
                +key
                +"\":"
                commonDefs.whitespace()
                value.buildGBNF(this, commonDefs)
//                commonDefs.whitespace()
            }
            else {
                optional {
                    commonDefs.whitespace()
                    +"\""
                    +key
                    +"\":"
                    commonDefs.whitespace()
                    value.buildGBNF(this, commonDefs)
//                    commonDefs.whitespace()
                }
            }
        }
        commonDefs.whitespace()
        +"}"
    }

    override fun fillIfMissing(jsonElement: JsonElement?): Pair<JsonElement?, Int> {
        val jsonObject = (jsonElement ?: JsonObject(mapOf()))
        if (jsonObject is JsonObject) {
            val invalidKeyCount = if (additionalProperties.second == false) jsonObject.keys.count { it !in properties.keys } else 0
            val missingKeys = required.filter { it !in jsonObject.keys }

            var itemsWrongNess = 0
            val currentEntries = jsonObject.entries.map {(k, v) -> k to (properties[k]?.fillIfMissing(v)?.also { itemsWrongNess += it.second }?.first ?: v)}.toMutableList()
            missingKeys.forEach {key ->
                properties[key]?.fillIfMissing(null)?.also { itemsWrongNess += it.second }?.first?.let { currentEntries.add(key to it) }
            }

            return JsonObject(mapOf(*currentEntries.toTypedArray())) to (invalidKeyCount + itemsWrongNess)
        }

        return null to 99999 // Unsuccessful attempt, with no value
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
    fun addRule(name: String, jsonType: JsonSchemaRule, required: Boolean = true) {
        add(name, jsonType, required)
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

    fun addObjectArray(name: String, required: Boolean = true, block: JsonSchemaObject.() -> Unit) {
        add(name, JsonSchemaArray(JsonSchemaObject(block)), required)
    }

    fun addAnyOfArray(name: String, required: Boolean = true, block: JsonSchemaAnyOf.() -> Unit) {
        add(name, JsonSchemaAnyOf(block), required)
    }
}
package com.mylosoftworks.kotllms.jsonschema.rules

import com.mylosoftworks.gbnfkotlin.GBNF
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

    override fun GBNFEntity.buildGBNF(commonDefs: CommonDefs) {
        +"{"
        val valsMapped = properties.map {(key, value) ->
            (key in required) to (this.host ?: this as GBNF).entity {
                commonDefs.whitespace()
                +"\""
                +key
                +"\":"
                commonDefs.whitespace()
                value.buildGBNF(this, commonDefs)
                commonDefs.whitespace()
            }
        }
        var first = true
        valsMapped.forEach {
            if (!first) +","
            first = false

            if (it.first) {
                it.second()
            }
            else {
                optional {
                    it.second()
                }
            }
        }
        commonDefs.whitespace()
        +"}"
//        val keys = properties.keys
//        keys.forEachIndexed {idx, key ->
//            val value = properties[key]!!
//            val couldBeLast = keys.drop(idx+1).find { it in required } == null
//            val isLast = idx == keys.size - 1
//
//            commonDefs.whitespace()
//            if (key in required) {
//                +"\""
//                +key
//                +"\":"
//                commonDefs.whitespace()
//                value.buildGBNF(this, commonDefs)
//                commonDefs.whitespace()
//                if (couldBeLast) {
//                    optional { +",O" }
//                }
//                else if (!isLast) {
//                    +",R"
//                }
//            }
//            else {
//                optional {
//                    +"\""
//                    +key
//                    +"\":"
//                    commonDefs.whitespace()
//                    value.buildGBNF(this, commonDefs)
//                    commonDefs.whitespace()
//                    if (couldBeLast) {
//                        optional { +"," }
//                    }
//                    else if (!isLast) {
//                        +","
//                    }
//                }
//            }
//        }
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
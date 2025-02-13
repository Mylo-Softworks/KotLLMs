package com.mylosoftworks.kotllms.jsonschema.rules

import com.mylosoftworks.gbnfkotlin.entries.GBNFEntity
import com.mylosoftworks.kotllms.features.stringOrToString
import com.mylosoftworks.kotllms.features.toJson
import com.mylosoftworks.kotllms.jsonschema.CommonDefs
import com.mylosoftworks.kotllms.jsonschema.JsonSchemaRule
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

sealed class JsonSchemaType(val type: String?, val default: JsonElement, val allowedValues: List<Any?>? = null, val gbnf: GBNFEntity.(CommonDefs) -> Unit): JsonSchemaRule() {
    override fun build(): JsonElement {
        return JsonObject(linkedMapOf<String, JsonElement>(
//            "type" to type.toJson()
        ).apply {
            if (type != null) this["type"] = type.toJson()
            if (!allowedValues.isNullOrEmpty()) this["enum"] = allowedValues.toJson()
        })
    }

    override fun GBNFEntity.buildGBNF(commonDefs: CommonDefs) {
        if (!allowedValues.isNullOrEmpty()) {
            oneOf {
                allowedValues.forEach { +it.toJson().toString() }
            }
        }
        else {
            gbnf(commonDefs)
        }
    }

    override fun fillIfMissing(jsonElement: JsonElement?): Pair<JsonElement?, Boolean> {
        if (allowedValues.isNullOrEmpty()) {
            if (jsonElement != null) return jsonElement to true
            return default to true
        }
        else {
            if (jsonElement == null) {
                return allowedValues[0].toJson() to true
            }
            else {
                // Try to fill
                if (jsonElement is JsonPrimitive) {
                    val strVal = jsonElement.stringOrToString()
                    val match = allowedValues.find { it.toString().startsWith(strVal) }
                    if (match == null) {
                        return allowedValues[0].toJson() to (strVal in allowedValues)
                    }
                    return match.toJson() to true
                }
            }
        }

        return null to false
    }
}

object JsonType {
    class String(vararg allowedValues: kotlin.Any?): JsonSchemaType("string", "".toJson(), allowedValues.toList(), {it.string()})
    class Number(vararg allowedValues: kotlin.Any?): JsonSchemaType("number", 0.toJson(), allowedValues.toList(), {it.number()})
    class Integer(vararg allowedValues: kotlin.Any?): JsonSchemaType("integer", 0.toJson(), allowedValues.toList(), {it.integer()})
    class Boolean(vararg allowedValues: kotlin.Any?): JsonSchemaType("boolean", false.toJson(), allowedValues.toList(), {it.boolean()})
    class Null(vararg allowedValues: kotlin.Any?): JsonSchemaType("null", null.toJson(), allowedValues.toList(), {it.nullVal()})

    class Any(vararg allowedValues: kotlin.Any?): JsonSchemaType(null, null.toJson(), allowedValues.toList(), {
        oneOf { it.string();it.number();it.boolean();it.nullVal() }
    })
}
package com.mylosoftworks.kotllms.jsonschema.rules

import com.mylosoftworks.gbnfkotlin.entries.GBNFEntity
import com.mylosoftworks.kotllms.features.toJson
import com.mylosoftworks.kotllms.jsonschema.CommonDefs
import com.mylosoftworks.kotllms.jsonschema.JsonSchemaRule
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

sealed class JsonSchemaType(val type: String?, val allowedValues: List<Any?>? = null, val gbnf: GBNFEntity.(CommonDefs) -> Unit): JsonSchemaRule() {
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
}

object JsonType {
    class String(vararg allowedValues: kotlin.Any?): JsonSchemaType("string", allowedValues.toList(), {it.string()})
    class Number(vararg allowedValues: kotlin.Any?): JsonSchemaType("number", allowedValues.toList(), {it.number()})
    class Integer(vararg allowedValues: kotlin.Any?): JsonSchemaType("integer", allowedValues.toList(), {it.integer()})
    class Boolean(vararg allowedValues: kotlin.Any?): JsonSchemaType("boolean", allowedValues.toList(), {it.boolean()})
    class Null(vararg allowedValues: kotlin.Any?): JsonSchemaType("null", allowedValues.toList(), {it.nullVal()})

    class Any(vararg allowedValues: kotlin.Any?): JsonSchemaType(null, allowedValues.toList(), {
        oneOf { it.string();it.number();it.boolean();it.nullVal() }
    })
}
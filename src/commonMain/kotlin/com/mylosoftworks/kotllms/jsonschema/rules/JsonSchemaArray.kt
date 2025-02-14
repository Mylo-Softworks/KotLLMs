package com.mylosoftworks.kotllms.jsonschema.rules

import com.mylosoftworks.gbnfkotlin.GBNF
import com.mylosoftworks.gbnfkotlin.entries.GBNFEntity
import com.mylosoftworks.kotllms.features.toJson
import com.mylosoftworks.kotllms.jsonschema.CommonDefs
import com.mylosoftworks.kotllms.jsonschema.JsonSchemaRule
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

class JsonSchemaArray(val items: JsonSchemaRule): JsonSchemaRule() {

    override fun build(): JsonElement {
        return JsonObject(mapOf(
            "type" to "array".toJson(),
            "items" to items.build()
        ))
    }

    override fun GBNFEntity.buildGBNF(commonDefs: CommonDefs) {
        val items = (host ?: this as GBNF).entity {
            commonDefs.whitespace()
            items.buildGBNF(this, commonDefs)
            optional {
                commonDefs.whitespace()
                +","
                commonDefs.whitespace()
                this@entity()
            }
        }
        +"["
        optional {
            items()
        }
        commonDefs.whitespace()
        +"]"
    }

    override fun fillIfMissing(jsonElement: JsonElement?): Pair<JsonElement?, Int> {
        val jsonArray = (jsonElement ?: JsonArray(listOf()))
        if (jsonArray is JsonArray) {
            if (jsonArray.size == 0) return jsonArray to 0

            val last = jsonArray.last() // Only check last
            val entries = jsonArray.dropLast(1).toMutableList()
            val newLast = items.fillIfMissing(last)
            val first = newLast.first

            if (first != null) {
                entries.add(first)
            }
            return JsonArray(entries) to newLast.second
        }

        return null to 99999
    }
}
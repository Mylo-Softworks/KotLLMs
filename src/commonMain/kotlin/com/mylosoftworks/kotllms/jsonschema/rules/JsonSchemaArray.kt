package com.mylosoftworks.kotllms.jsonschema.rules

import com.mylosoftworks.gbnfkotlin.GBNF
import com.mylosoftworks.gbnfkotlin.entries.GBNFEntity
import com.mylosoftworks.kotllms.features.toJson
import com.mylosoftworks.kotllms.jsonschema.CommonDefs
import com.mylosoftworks.kotllms.jsonschema.JsonSchemaRule
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

open class JsonSchemaArray(val items: JsonSchemaRule): JsonSchemaRule() {

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
}
package com.mylosoftworks.kotllms.jsonschema.rules

import com.mylosoftworks.kotllms.features.toJson
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
}
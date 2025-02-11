package com.mylosoftworks.kotllms.jsonschema

import com.mylosoftworks.kotllms.features.ToJson
import kotlinx.serialization.json.JsonElement

/**
 * Represents a single rule from a json schema, which can be built to a Json Element, TODO: or converted to a GBNF ruleset object
 */
abstract class JsonSchemaRule: ToJson {
    abstract fun build(): JsonElement

    override fun toJson(): JsonElement = build()
}
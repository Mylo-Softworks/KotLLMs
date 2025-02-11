package com.mylosoftworks.kotllms.jsonschema

import com.mylosoftworks.kotllms.features.toJson
import kotlinx.serialization.json.JsonObject

/**
 * A class used to build json schemas, and parsing them.
 */
class JsonSchema(val name: String = "unnamed", val rule: JsonSchemaRule) {
    /**
     * (Examples are in pseudocode)
     * Build the schema, when using response format:
     * ```json
     * "response_format": buildResponseFormat()
     * ```
     */
    fun buildResponseFormat(): JsonObject {
        return JsonObject(mapOf("type" to "json_schema".toJson(), "json_schema" to rule.build()))
    }

    /**
     * (Examples are in pseudocode)
     * Build the schema, when using tools:
     * ```json
     * "tools": [
     *   buildToolFunction(name, description)
     * ]
     * ```
     */
    fun buildToolFunction(name: String, description: String?): JsonObject {
        return JsonObject(mapOf("type" to "function".toJson(), "function" to hashMapOf(
            "name" to name,
            "description" to (description ?: ""),
            "strict" to true,
            "parameters" to rule.build()
        ).toJson()))
    }

    /**
     * Builds the schema itself.
     */
    fun build() = rule.build()
}
package com.mylosoftworks.kotllms.jsonschema

import com.mylosoftworks.gbnfkotlin.GBNF
import com.mylosoftworks.kotllms.features.toJson
import com.mylosoftworks.kotllms.jsonschema.rules.JsonType
import kotlinx.serialization.json.JsonObject

/**
 * A class used to build json schemas, and parsing them.
 */
class JsonSchema(val name: String, val description: String? = null, val schema: JsonSchemaRule) {

    constructor(name: String, description: String? = null, schema: JsonType): this(name, description, schema.type)

    /**
     * (Examples are in pseudocode)
     * Build the schema, when using response format:
     * ```json
     * "response_format": buildResponseFormat()
     * ```
     */
    fun buildResponseFormat(): JsonObject {
        return JsonObject(mapOf("type" to "json_schema".toJson(), "json_schema" to hashMapOf(
            "name" to name,
            "description" to description,
            "strict" to true,
            "schema" to build()
        ).toJson()))
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
    fun buildToolFunction(): JsonObject {
        return JsonObject(mapOf("type" to "function".toJson(), "function" to hashMapOf(
            "name" to name,
            "description" to (description ?: ""),
            "strict" to true,
            "parameters" to build()
        ).toJson()))
    }

    /**
     * Builds the schema itself.
     */
    fun build() = schema.build()

    /**
     * Builds a GBNF for parsing the Json Schema TODO: Not implemented yet
     */
    fun buildGBNF(): Nothing = TODO()// GBNF{ schema.buildGBNF(this) }
}
package com.mylosoftworks.kotllms.jsonschema

import com.mylosoftworks.gbnfkotlin.GBNF
import com.mylosoftworks.kotllms.features.toJson
import kotlinx.serialization.json.JsonObject

/**
 * A class used to build json schemas, and parsing them.
 */
class JsonSchema(val name: String, val description: String? = null, val schema: JsonSchemaRule) {

    /**
     * (Examples are in pseudocode)
     * Build the schema, when using response format:
     * ```json
     * "response_format": buildResponseFormat()
     * ```
     */
    fun buildResponseFormat(): JsonObject {
        return JsonObject(linkedMapOf("type" to "json_schema".toJson(), "json_schema" to hashMapOf(
            "name" to name,
            "description" to description,
            "strict" to true,
            "schema" to build()
        ).toJson())
        )
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
        return JsonObject(linkedMapOf("type" to "function".toJson(), "function" to hashMapOf(
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
     * Builds a GBNF for parsing the Json Schema
     */
    fun buildGBNF() = GBNF{
        val whiteSpace = "whitespace" {
            anyCount {
                +" "
            }
            optional {
                +"\n"
                anyCount {
                    +" "
                }
            }
        }
        val string = "string" {
            +"\"" // Json only supports double quotes
            anyCount {
                oneOf {
                    +"\\" // Allow escaping escapes
                    +"\\\"" // Capture escaped quotes
                    -!"\"" // Capture everything else except unescaped quotes
                }
            }
            +"\""
        }
        val boolean = "boolean" {
            oneOf {
                +"true"
                +"false"
            }
        }
        val nullVal = "null" {
            +"null"
        }
        val integer = "int" {
            optional { +"-" } // Sign
            oneOrMore { -"0-9" } // Value
        }
        val number = "number" {
            optional { +"-" } // Sign
            oneOf {
                group {
                    oneOrMore { -"0-9" }
                    optional { +"." }
                    anyCount { -"0-9" }
                }
                group {
                    anyCount { -"0-9" }
                    optional { +"." }
                    anyCount { -"0-9" }
                }
            }
            optional { // Exponent
                -"eE"
                oneOrMore { -"0-9" }
            }
        }
        whiteSpace()
        schema.buildGBNF(this, CommonDefs(whiteSpace, string, boolean, nullVal, integer, number))
        whiteSpace()
    }
}
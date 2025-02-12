package com.mylosoftworks.kotllms.jsonschema

import com.mylosoftworks.gbnfkotlin.entries.GBNFEntity
import com.mylosoftworks.kotllms.features.ToJson
import kotlinx.serialization.json.JsonElement
import kotlin.jvm.JvmName

/**
 * Represents a single rule from a json schema, which can be built to a Json Element.
 */
abstract class JsonSchemaRule: ToJson {
    abstract fun build(): JsonElement

    override fun toJson(): JsonElement = build()

    @JvmName("buildGBNFAlt")
    fun buildGBNF(gbnf: GBNFEntity, commonDefs: CommonDefs) = gbnf.buildGBNF(commonDefs) // For outside access

    /**
     * Build the GBNF for generating with this schema on an api which only supports GBNF.
     */
    abstract fun GBNFEntity.buildGBNF(commonDefs: CommonDefs) // For easy implementation (automatic this receiver)
}

/**
 * A data class holding references to GBNF entities which are commonly used in JSON.
 */
data class CommonDefs(val whitespace: GBNFEntity, val string: GBNFEntity, val boolean: GBNFEntity, val nullVal: GBNFEntity, val integer: GBNFEntity, val number: GBNFEntity)
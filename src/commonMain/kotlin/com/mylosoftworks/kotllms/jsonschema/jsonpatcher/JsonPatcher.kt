package com.mylosoftworks.kotllms.jsonschema.jsonpatcher

import com.mylosoftworks.klex.AnyCount
import com.mylosoftworks.klex.Klex
import com.mylosoftworks.klex.Optional
import com.mylosoftworks.kotllms.jsonschema.JsonSchema
import com.mylosoftworks.kotllms.jsonschema.JsonSchemaRule
import kotlinx.serialization.json.*

object JsonPatcher {
    /**
     * Json parser which parses to [kotlinx.serialization.json.JsonElement], but allows malformed json.
     *
     * Klex is used in a way so the output is already a tree of the json.
     */
    val jsonParser = Klex.Companion.create<JsonElement> {
        var element by placeholder()

        val ws = define { // Whitespace
            AnyCount {
                -" \t\n" // Accept space, tab and newline as whitespace characters
            }
        }

        // Primitive types
        val string = define { // Parses a json string
            +"\"" // Starts with a quote
            val strcontents = group {
                AnyCount {
                    oneOf({
                        +"\\\\" // Escaped backslash (\\) should be captured
                    }, {
                        +"\\\"" // Escaped quotes (\") should be captured
                    }, {
                        -!"\"" // Anything except for quotes can now be captured, escapes are handled on the previous options
                    })
                }
            }.getOrElse { return@define }.content // ensures we don't continue if string failed to parse
            treeValue = JsonPrimitive(strcontents)

            Optional {
                +"\"" // Ends with a quote
            }
        }

        val number = define {
            val numString = group {
                AnyCount {
                    -"0-9"
                }
                Optional {
                    +"."
                }
                AnyCount {
                    -"0-9"
                }
                Optional {
                    -"eE" // Case-insensitive
                    AnyCount {
                        -"0-9"
                    }
                }
            }.getOrElse { return@define }.content

            val parsed = numString.toIntOrNull() ?: numString.toDoubleOrNull()

            // Fail makes the parent not accept this answer, I use it here because I am lazy and don't feel like making the number parser more accurate
            // You can simply do `return@define fail()` to not run the code below, this is redundant in this example though.
            if (parsed == null) return@define fail()
            else treeValue = JsonPrimitive(parsed)
        }

        // Allows for capturing a bool from just the first character
        val restOfBoolOrNull = define {
            AnyCount {
                -"rueals" // Rest of bool or null
            }
        }

        val bool = define {
            val boolString = oneOf({+"true";restOfBoolOrNull()}, {+"false";restOfBoolOrNull()}).getOrElse { return@define }.content
            treeValue = JsonPrimitive(boolString == "true")
        }

        val nullJson = define {
            +"n"
            restOfBoolOrNull()
            treeValue = JsonNull
        }

        // Group values
        val array = define {
            +"["
            val children = group {
                Optional {
                    element()
                    AnyCount {
                        +","
                        element()
                    }
                }
            }.getOrElse { return@define }.flattenNullValues().map { it.value!! }
            Optional {
                +"]"
            }
            treeValue = JsonArray(children)
        }

        val objEntry = defineR<Pair<String, JsonElement>?> {
            // key: value
            ws()
            val key = string().getOrElse { return@defineR null }.first.value?.jsonPrimitive?.content ?: return@defineR null
            ws()
            var returnVal: Pair<String, JsonElement>? = null
            Optional {
                +":"
                ws()
                val value = element().getOrNull()?.first?.firstFlatValue()
                ws()
                //            treeValue = (mapOf(key to value))
                if (value != null) returnVal = key to value
            }
            return@defineR returnVal
        }
        val obj = define {
            +"{"
            val objects = mutableListOf<Pair<String, JsonElement>>()
            group {
                Optional {
                    objEntry()?.let { objects.add(it) }
                    AnyCount {
                        +","
                        objEntry()?.let { objects.add(it) }
                    }
                }
            }.getOrElse { return@define }
            Optional {
                +"}"
            }
            treeValue = JsonObject(mapOf(*objects.toTypedArray()))
        }

        // Define element
        element = define {
            ws()
            Optional { // Might be completely empty, in which case, the end result should just be null
                oneOf(string, number, bool, nullJson, array, obj)
            }
            ws()
        }

        element()

        if (remainder != "") remainder = "" // Strip invalid remainder
    }

    /**
     * Parses an incomplete json object, this allows for parsing partial json values as JsonObject.
     * Additionally, missing required keys can be added to match a json schema.
     */
    fun parseIncompleteJson(json: String): JsonElement? {
        return jsonParser.parse(json).getOrThrow().firstFlatValue()
    }

    fun patchToMatchSchema(json: String, schema: JsonSchema) = patchToMatchSchema(json, schema.schema)

    fun patchToMatchSchema(json: String, schema: JsonSchemaRule): JsonElement? {
        return schema.fillIfMissing(parseIncompleteJson(json)).first
    }
}
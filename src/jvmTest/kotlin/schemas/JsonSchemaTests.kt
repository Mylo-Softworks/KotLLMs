package schemas

import com.mylosoftworks.kotllms.features.stringOrToString
import com.mylosoftworks.kotllms.jsonSettings
import com.mylosoftworks.kotllms.jsonschema.JsonSchema
import com.mylosoftworks.kotllms.jsonschema.rules.JsonSchemaObject
import com.mylosoftworks.kotllms.jsonschema.rules.JsonType
import org.junit.Test
import kotlin.test.assertEquals

class JsonSchemaTests {
    @Test
    fun testBasicJsonSchema() {
        val schema = JsonSchema("name", schema = JsonSchemaObject {
            addRule("string", JsonType.String())
            addObject("object") {
                addRuleArray("array", JsonType.Number())
            }
            addObjectArray("array") {
                addRule("name", JsonType.String())
            }
            addRule("val 2 (string, bool, or number)", JsonType.Any(true, false, null, "string", 0, 1, 2, 3))
        })

        println(schema.build())
    }

    @Test
    fun testSchemaGrammar() {
        val schema = JsonSchema("name", schema = JsonSchemaObject {
            addRule("string", JsonType.String())
            addObject("object") {
                addRuleArray("array", JsonType.Number())
            }
            addObjectArray("array") {
                addRule("name", JsonType.String())
            }
            addRule("val 2 (string, bool, or number)", JsonType.Any(true, false, null, "string", 0, 1, 2, 3))
        })

        println(schema.buildGBNF().compile())
    }

    @Test
    fun testJsonStringOrObjectToString() {
        val el = jsonSettings.parseToJsonElement("{\"test\":0}")
        val el2 = jsonSettings.parseToJsonElement("\"{\\\"test\\\":0}\"")
        assertEquals(el.stringOrToString(), el2.stringOrToString())
    }
}
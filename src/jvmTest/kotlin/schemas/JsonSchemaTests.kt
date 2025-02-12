package schemas

import com.mylosoftworks.kotllms.jsonschema.JsonSchema
import com.mylosoftworks.kotllms.jsonschema.rules.JsonSchemaObject
import com.mylosoftworks.kotllms.jsonschema.rules.JsonType
import org.junit.Test

class JsonSchemaTests {
    @Test
    fun testBasicJsonSchema() {
        val schema = JsonSchema("name", schema = JsonSchemaObject {
            addType("string", JsonType.String)
            addObject("object") {
                addTypeArray("array", JsonType.Number)
            }
            addObjectArray("array") {
                addType("name", JsonType.String)
            }
        })

        println(schema.build())
    }
}
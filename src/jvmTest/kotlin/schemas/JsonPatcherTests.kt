package schemas

import com.mylosoftworks.kotllms.jsonschema.JsonSchemaRule
import com.mylosoftworks.kotllms.jsonschema.jsonpatcher.JsonPatcher
import com.mylosoftworks.kotllms.jsonschema.rules.*
import kotlin.test.Test
import kotlin.test.assertEquals

class JsonPatcherTests {
    // Raw patching tests
    @Test
    fun testBasicPatch() {
        val incompleteJson = """
            {"key":10,
            """.trimIndent()
        val compareJson = """
            {"key":10}
            """.trimIndent()

        val json = JsonPatcher.parseIncompleteJson(incompleteJson)
        assertEquals(compareJson, json.toString())
    }

    @Test
    fun testPatchOpen() {
        val incompleteJson = """
            {"incomplete":"value
            """.trimIndent()
        val compareJson = """
            {"incomplete":"value"}
            """.trimIndent()

        val json = JsonPatcher.parseIncompleteJson(incompleteJson)
        assertEquals(compareJson, json.toString())
    }

    @Test
    fun testPatchConst() {
        val incompleteJson = """
            {"incomplete":n
            """.trimIndent()
        val compareJson = """
            {"incomplete":null}
            """.trimIndent()

        val json = JsonPatcher.parseIncompleteJson(incompleteJson)
        assertEquals(compareJson, json.toString())
    }

    @Test
    fun testPatchList() {
        val incompleteJson = """
            [{"name":"item1"},{"name":"item2
        """.trimIndent()
        val compareJson = """
            [{"name":"item1"},{"name":"item2"}]
        """.trimIndent()

        val json = JsonPatcher.parseIncompleteJson(incompleteJson)
        assertEquals(compareJson, json.toString())
    }

    // Json schema patching tests

    val testSchema1 = JsonSchemaObject {
        addRule("key", JsonType.String())
    }

    @Test
    fun testSchema1Test1() {
        val incompleteJson = """
            {
        """.trimIndent() // Completely empty, however, since the object is opened, the required keys need to be filled in
        val compareJson = """
            {"key":""}
        """.trimIndent()

        val json = JsonPatcher.patchToMatchSchema(incompleteJson, testSchema1)
        assertEquals(compareJson, json.toString())
    }

    val testSchema2 = JsonSchemaArray(JsonSchemaAnyOf {
        addRule(JsonType.String())
        addRule(JsonType.Null())
        addRule(JsonType.Boolean())
    })

    @Test
    fun testSchema2Test1() {
        val incompleteJson = """
            ["String","String2
        """.trimIndent()
        val compareJson = """
            ["String","String2"]
        """.trimIndent()

        val json = JsonPatcher.patchToMatchSchema(incompleteJson, testSchema2)
        assertEquals(compareJson, json.toString())
    }

    val testSchema3 = JsonType.String("one", "two", "three", "four", "five")

    @Test
    fun testSchema3Test1() {
        val incompleteJson = """
            "o
        """.trimIndent() // "o should be converted to "one" since it's the only (and therefore first) option
        val compareJson = """
            "one"
        """.trimIndent()

        val json = JsonPatcher.patchToMatchSchema(incompleteJson, testSchema3)
        assertEquals(compareJson, json.toString())
    }

    @Test
    fun testSchema3Test2() {
        val incompleteJson = """
            "t
        """.trimIndent() // "o should be converted to "one" since it's the only (and therefore first) option
        val compareJson = """
            "two"
        """.trimIndent()

        val json = JsonPatcher.patchToMatchSchema(incompleteJson, testSchema3)
        assertEquals(compareJson, json.toString())
    }

    val testSchema4 = JsonSchemaArray(JsonSchemaAnyOf {
        addObject {
            addRule("type", JsonType.String("string"))
            addRule("string", JsonType.String())
        }
        addObject {
            addRule("type", JsonType.String("number"))
            addRule("number", JsonType.Number())
        }
    })

    @Test
    fun testSchema4Test1() {
        val incompleteJson = """
            [{"type":"number"
        """.trimIndent() // Should infer that since type is string, only the first rule can be properly matched
        val compareJson = """
            [{"type":"number","number":0}]
        """.trimIndent()

        val json = JsonPatcher.patchToMatchSchema(incompleteJson, testSchema4)
        assertEquals(compareJson, json.toString())
    }
}
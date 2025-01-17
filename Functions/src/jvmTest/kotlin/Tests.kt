import com.mylosoftworks.kotllms.functions.DefaultFunctionGrammar
import com.mylosoftworks.kotllms.functions.FunctionDefs
import com.mylosoftworks.kotllms.functions.FunctionParameterInt
import com.mylosoftworks.kotllms.functions.FunctionParameterString
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class Tests {
    @Test
    fun testFunctionDescriptions() {
        val defs = FunctionDefs {
            function<Unit>("example", "An example function") {
                addParam(FunctionParameterString("example", false, "Example description"))
            }
        }

        println(defs.getDescriptionForAllCalls())
    }

    @Test
    fun testParameterParsing() {
        val grammarDef = DefaultFunctionGrammar()
        val defs = FunctionDefs(grammarDef) {
            function<Unit>("example", "An example function") {
                addParam(FunctionParameterString("example", false, "Example description"))
            }
        }

        println(grammarDef.getParametersFromResponse(defs.functions["example"]!!, "example: \"This is a value!\""))
    }

    @Test
    fun testReturnValue() {
        var a: FunctionParameterInt? = null
        var b: FunctionParameterInt? = null

        val defs = FunctionDefs {
            function<Int>("add", "Adds 2 integers") {
                a = addParam(FunctionParameterInt("a"))
                b = addParam(FunctionParameterInt("b"))

                callback = func@{
                    return@func a!!(it)!! + b!!(it)!!
                }
            }
        }

        runBlocking {
            val result = defs.functions["add"]!!.runCallBackWithParams(hashMapOf("a" to (a!! to 2), "b" to (b!! to 5)))
            assertEquals(7, result)
        }
    }
}
import com.mylosoftworks.kotllms.functions.FunctionDefs
import com.mylosoftworks.kotllms.functions.FunctionParameterString
import kotlin.test.Test

class Tests {
    @Test
    fun testFunctionDescriptions() {
        val defs = FunctionDefs {
            function("example", "An example function") {
                addParam(FunctionParameterString("example", false, "Example description"))
            }
        }

        println(defs.getDescriptionForAllCalls())
    }

    @Test
    fun testParameterParsing() {
        val defs = FunctionDefs {
            function("example", "An example function") {
                addParam(FunctionParameterString("example", false, "Example description"))
            }
        }

        println(defs.functions["example"]?.getParametersFromResponse("example: \"This is a value!\""))
    }
}
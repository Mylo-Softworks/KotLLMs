import com.mylosoftworks.kotllms.functions.*
import kotlinx.coroutines.runBlocking
import kotlin.test.Test

class AutoParseTests {
    val autoParsedExample = AutoParsedGrammarDef {
        val thoughts = entity("thoughts") { repeat(max = 100) { range("\\\"\\n", true) } }

        val allFunctions = it.functions.values.map {func ->
            entity("function_${func.name}") {
                literal(func.name + "\n--params--\n")
                for (param in func.params.values) {
                    if (param.optional) {
                        optional {
                            literal("${param.name}: ")
                            entity("param_${func.name}_${param.name}") currentParam@{
                                param.addGBNFRule(this@currentParam) // Value part
                            }() // Immediately insert it after declaring
                            literal("\n")
                        }
                    }
                    else {
                        literal("${param.name}: ")
                        entity("param_${func.name}_${param.name}") currentParam@{
                            param.addGBNFRule(this@currentParam) // Value part
                        }() // Immediately insert it after declaring
                        literal("\n")
                    }
                }
            }
        }

        literal("Thoughts: \"")
        thoughts()
        literal("\"\nCall: ")
        oneOf {
            allFunctions.forEach {
                it()
            }
        }
    }

    val exampleFunctions = FunctionDefs(autoParsedExample) {
        function("print") {
            addParam(FunctionParameterString("value", false, "The value to print to the user"))

            callback = {
                println("Print: ${it["value"]?.second}")
            }
        }
    }

    @Test
    fun autoParsePrintGrammar() {
        println(exampleFunctions.getGrammarForAllCallsSingleRequest().compile())
    }

    @Test
    fun autoParseParseFunctionCall() {
        val testInput = """
            Thoughts: "I am a helpful AI assistant based on Llama 3. I'm a function calling model, which means I'm a tool, "
            Call: print
            --params--
            value: "I am a helpful AI assistant based on Llama 3. I'm a function calling model, which means I'm a tool that allows you to execute actions by calling functions. I'm here to help you with any questions or tasks you may have, so feel free to ask me anything!"
        """.trimIndent() + "\n" // The newline is included in the grammar, but gets trimmed with trimIndent, so I've added it back manually

        val (raw, call, thoughts) = exampleFunctions.grammarDef.parseFunctionCall(exampleFunctions, testInput).getOrThrow()
        assert(thoughts == "I am a helpful AI assistant based on Llama 3. I'm a function calling model, which means I'm a tool, ") { "Thoughts don't match parsed" }

        // Invoke the print function
        runBlocking {
            call?.invoke()
        }
    }
}
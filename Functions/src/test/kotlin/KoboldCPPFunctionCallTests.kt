import com.mylosoftworks.kotllms.api.impl.KoboldCPP
import com.mylosoftworks.kotllms.api.impl.KoboldCPPGenFlags
import com.mylosoftworks.kotllms.chat.BasicChatMessage
import com.mylosoftworks.kotllms.chat.ChatDef
import com.mylosoftworks.kotllms.chat.templated.presets.Llama3Template
import com.mylosoftworks.kotllms.functions.*
import kotlinx.coroutines.runBlocking
import kotlin.test.Test

class KoboldCPPFunctionCallTests {
    val api: KoboldCPP = KoboldCPP()

    @Test
    fun testSimpleFunction() {
        val functions = FunctionDefs {
            function("print", "Prints a piece of text to the user.") {
                addParam(FunctionParameterString("value", false, "The value to print to the user"))

                callback = {
                    println("Print: ${it["value"]?.second}")
                }
            }
        }

        val systemMessage = BasicChatMessage().init {
            content = """
                You are a helpful AI assistant based on Llama 3.
                You are a function calling model based on Llama 3, all your actions are executed through functions, including responding to the user.
            """.trimIndent()
            role = "system"
        }

        val functionList = BasicChatMessage().init {
            content = """
                First, you write your thoughts down, allowing you to decide on which function to call, and what values to give as the parameters, then, you write down the name of the function to call.
                The following is a JSON object containing all functions available to you, with a name, and description for each one of them:
                ${functions.getDescriptionForAllCalls()}
                """.trimIndent()
            role = "system"
        }

        val exampleChat = ChatDef<BasicChatMessage>()
        exampleChat.addMessage(BasicChatMessage().init {
            content = "Who are you?"
            role = "user"
        })

        api.settings.template = Llama3Template()
        runBlocking {
            val flags = KoboldCPPGenFlags().init {
                max_length = 1024
            } // We will use the same flags for both calls

            val givenChat = exampleChat.subChat<ChatDef<BasicChatMessage>>(9, mutableListOf(systemMessage, functionList))

            val targetCall = functions.requestFunctionCall(api, flags, givenChat)
            val targetFunction = targetCall.second
            if (targetFunction != null) {
                exampleChat.addMessage(BasicChatMessage().init {
                    content = targetCall.first // Ensure the bot knows which function it picked and what thoughts it had
                    role = "assistant"
                })

                println("Selected function: ${targetFunction.name}")

                val functionDef = BasicChatMessage().init {
                    content = """
                        The following is a JSON object containing information about the selected function (${targetFunction.name}):
                        ${targetFunction.getExtendedDescriptionForFunction()}
                        """.trimIndent()
                    role = "system"
                }

                val givenChatFunctionDef = exampleChat.subChat<ChatDef<BasicChatMessage>>(9, mutableListOf(systemMessage), mutableListOf(functionDef))
                val callRequest = targetFunction.requestCallFunction(api, flags, givenChatFunctionDef)
                println("Calling ${targetFunction.name} with given parameters")
                callRequest.second() // Invoke the function the LLM was trying to invoke
            }
            else {
                println("Failed to select a function!")
            }
        }
    }

    @Test
    fun testSimpleFunctionOneRequest() {
        val functions = FunctionDefs {
            function("print", "Prints a piece of text to the user.") {
                addParam(FunctionParameterString("value", false, "The value to print to the user"))

                callback = {
                    println("Print: ${it["value"]?.second}")
                }
            }
        }

        val systemMessage = BasicChatMessage().init {
            content = """
                You are a helpful AI assistant based on Llama 3.
                You are a function calling model based on Llama 3, all your actions are executed through functions, including responding to the user.
            """.trimIndent()
            role = "system"
        }

        val functionList = BasicChatMessage().init {
            content = """
                First, you write your thoughts down, allowing you to decide on which function to call, and what values to give as the parameters, then, you write down the name of the function to call, after that, provide values for the parameters, some parameters can be optional.
                The following is a JSON object containing all functions available to you, with a name, and description for each one of them:
                ${functions.getDetailedDescriptionForAllCalls()}
                """.trimIndent()
            role = "system"
        }

        val exampleChat = ChatDef<BasicChatMessage>()
        exampleChat.addMessage(BasicChatMessage().init {
            content = "Who are you?"
            role = "user"
        })

        api.settings.template = Llama3Template()

        runBlocking {
            val flags = KoboldCPPGenFlags().init {
                max_length = 1024
            } // We will use the same flags for both calls

            val givenChat =
                exampleChat.subChat<ChatDef<BasicChatMessage>>(9, mutableListOf(systemMessage, functionList))

            val (_, func, comment) = functions.requestFunctionCallSingleRequest(api, flags, givenChat)
            println("Comment: $comment")
            func?.let { it() }
        }
    }
}
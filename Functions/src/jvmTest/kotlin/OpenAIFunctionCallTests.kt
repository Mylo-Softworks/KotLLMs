import com.mylosoftworks.kotllms.api.impl.OpenAI
import com.mylosoftworks.kotllms.api.impl.OpenAISettings
import com.mylosoftworks.kotllms.features.flagsimpl.ToolChoice
import com.mylosoftworks.kotllms.features.impl.ChatRole
import com.mylosoftworks.kotllms.functions.FunctionDefs
import com.mylosoftworks.kotllms.functions.FunctionParameterString
import kotlinx.coroutines.runBlocking
import kotlin.test.Test

class OpenAIFunctionCallTests {
    val api: OpenAI = OpenAI(OpenAISettings(System.getenv("openrouter_key"), url = "https://openrouter.ai/api/v1"))

    @Test
    fun testSimpleFunctionOneRequest() {
        val functions = FunctionDefs {
            function<Unit>("print", "Prints a piece of text to the user.") {
                val value = addParam(FunctionParameterString("value", false, "The value to print to the user"))

                callback = {
                    println("Print: ${value(it)}")
                }
            }
        }

        val exampleChat = api.createChat {
            createMessage {
                content = "Who are you?"
                role = ChatRole.User
            }
        }

        // Storing messages for later (alternative to defining in chat in advance). An empty chat can be created as well.
        val systemMessage = exampleChat.storeMessage {
            content = """
                You are a helpful AI assistant.
                You are a function calling model, all your actions are executed through functions, including responding to the user.
            """.trimIndent()
            role = ChatRole.System
        }

        val functionList = exampleChat.storeMessage {
            content = """
                The following is a JSON object containing all functions available to you, with a name, and description for each one of them:
                ${functions.getDescriptionForAllCalls()}
                """.trimIndent()
            role = ChatRole.System
        }


        runBlocking {
            val flags = api.buildFlags {
                model = "google/gemini-2.0-flash-lite-preview-02-05:free"
                toolChoice = ToolChoice.Required // Force tool calls, otherwise the model would just give a regular response
            } // Since it supports tool calls

            val givenChat =
                exampleChat.subChat(9, mutableListOf(systemMessage, functionList))

            val (funcs, response) = functions.requestFunctionCallsAndParse(api, flags, givenChat).getOrThrow()
            println(response)
            funcs[0]()
        }
    }
}
import com.mylosoftworks.kotllms.api.impl.KoboldCPP
import com.mylosoftworks.kotllms.api.impl.KoboldCPPGenFlags
import com.mylosoftworks.kotllms.api.impl.extenders.toChat
import com.mylosoftworks.kotllms.chat.templated.presets.Llama3Template
import com.mylosoftworks.kotllms.functions.*
import kotlinx.coroutines.runBlocking
import kotlin.test.Test

class KoboldCPPFunctionCallTests {
    val api: KoboldCPP = KoboldCPP()

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

        val chatApi = api.toChat(Llama3Template())

        val exampleChat = chatApi.createChat {
            createMessage {
                content = "Who are you?"
                role = "user"
            }
        }

        // Storing messages for later (alternative to defining in chat in advance). An empty chat can be created as well.
        val systemMessage = exampleChat.storeMessage {
            content = """
                You are a helpful AI assistant based on Llama 3.
                You are a function calling model based on Llama 3, all your actions are executed through functions, including responding to the user.
            """.trimIndent()
            role = "system"
        }

        val functionList = exampleChat.storeMessage {
            content = """
                First, you write your thoughts down, allowing you to decide on which function to call, and what values to give as the parameters, then, you write down the name of the function to call, after that, provide values for the parameters, some parameters can be optional.
                The following is a JSON object containing all functions available to you, with a name, and description for each one of them:
                ${functions.getDescriptionForAllCalls()}
                """.trimIndent()
            role = "system"
        }


        runBlocking {
            val flags = KoboldCPPGenFlags().apply {
                maxLength = 1024
            } // We will use the same flags for both calls

            val givenChat =
                exampleChat.subChat(9, mutableListOf(systemMessage, functionList))

            val (funcs, comment) = functions.requestFunctionCallsAndParse(chatApi, flags, givenChat).getOrThrow()
            println("Comment: $comment")
            funcs[0]()
        }
    }
}
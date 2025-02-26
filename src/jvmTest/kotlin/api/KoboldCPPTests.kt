package api

import com.mylosoftworks.gbnfkotlin.GBNF
import com.mylosoftworks.kotllms.api.StreamedGenerationResult
import com.mylosoftworks.kotllms.api.impl.KoboldCPP
import com.mylosoftworks.kotllms.api.impl.KoboldCPPGenFlags
import com.mylosoftworks.kotllms.api.impl.extenders.toChat
import com.mylosoftworks.kotllms.features.toJson
import com.mylosoftworks.kotllms.chat.features.ChatFeatureImages
import com.mylosoftworks.kotllms.chat.templated.ChatTemplateDSL
import com.mylosoftworks.kotllms.chat.templated.presets.Llama3Template
import com.mylosoftworks.kotllms.features.impl.ChatGen
import com.mylosoftworks.kotllms.features.impl.ChatRole
import com.mylosoftworks.kotllms.runIfImpl
import com.mylosoftworks.kotllms.shared.toAttached
import kotlinx.coroutines.runBlocking
import javax.imageio.ImageIO
import kotlin.io.path.Path
import kotlin.test.Test

class KoboldCPPTests {
    val api: KoboldCPP = KoboldCPP()

    @Test
    fun checkCurrentModel() {
        val modelName = runBlocking {
            api.getCurrentModel().getOrThrow().modelName
        }
        print("Current model: $modelName\n")
    }

    @Test
    fun checkVersion() {
        val versionInfo = runBlocking {
            api.version()
        }.getOrThrow()
        println("Current version: ${versionInfo.versionNumber}")
    }

    @Test
    fun testTokenCounter() {
        val tokenCount = runBlocking {
            api.tokenCount("This is a test string")
        }
        println("Token count: $tokenCount")
    }

    @Test
    fun testFlags() {
        val flags = api.buildFlags {
            temperature = 0.7f
        }
        val test = hashMapOf(
            "temperature" to 0.7f.toJson()
        )
        assert(flags.setFlags == test) {"Flags didn't match"}
    }

    @Test
    fun testGen() {
        val start = "This is a short story about a"
        val flags = api.buildFlags {
            maxLength = 50

            grammar = GBNF {
                optional {
                    literal("n")
                }
                literal(" ")
                oneOrMore {
                    range("a-zA-Z")
                }
                literal(" who- Scratch that. My favorite word in the english language is \"")
                oneOrMore {
                    range("a-zA-Z")
                }
                literal("\"!")
            }
        }
        val result = runBlocking {
            api.rawGen(start, flags)
        }.getOrThrow()
        println(start + result.getText())
    }

    @Test
    fun testSuccessiveGen() {
        runBlocking {
            repeat(2) {
                api.rawGen("Test", api.buildFlags {
                    maxLength = 10
                }).getOrThrow() // Use getOrThrow, otherwise errors would be missed
            }
        }
    }

    @Test
    fun testStream() {
        val start = "This is a short story about a"
        val flags = api.buildFlags {
            maxLength = 50

            stream = true
        }
        runBlocking {
            val result = api.rawGen(start, flags).getOrThrow() as StreamedGenerationResult<*>

            print(start) // Start
            result.registerStreamer {
                val chunk = it.getOrElse {ex ->
                    throw ex // To fail the test properly
                }
                print(chunk.getTokenF()) // Stream
                System.out.flush() // Show the new tokens even before newline (since print doesn't flush)
                if (chunk.isLastToken()) println() // End
            }
        }
    }

    @Test
    fun testSuccessiveStream() {
        repeat(2) {
            testStream()
        }
    }

    @Test
    fun testChat() {
        val chatApi = api.toChat(ChatTemplateDSL {
            """
This is an example chat template

This is the beginning of the chat:
${messages.joinToString("\n") {message -> message["role"]?.let { "$it: " } + message["content"]?.let { it } }}
bot:
""".trimIndent()
        })

        val exampleChat = chatApi.createChat {
            createMessage {
                content = "Hi!"
                role = ChatRole.Assistant
            }
            createMessage {
                content = "What's up?"
                role = ChatRole.User
            }
        }

        val result = runBlocking {
            chatApi.chatGen(exampleChat, api.buildFlags {
                maxLength = 200
            })
        }.getOrThrow()

        println(result.getText())
    }

    // Assuming Llama 3 is loaded with CLIP
    @Test
    fun testImage() {
        val image = ImageIO.read(Path("testres/ReadTest.png").toFile())

        val chatApi = api.toChat(Llama3Template())

        val exampleChat = chatApi.createChat {
            createMessage {
                content = "What do you see in this image?"
                role = ChatRole.User
                runIfImpl<ChatFeatureImages> {
                    images = listOf(image.toAttached())
                }
            }
        }

        val result = runBlocking {
            chatApi.chatGen(exampleChat)
        }.getOrThrow()

        println(result.getText())
    }
}
package api

import com.mylosoftworks.gbnfkotlin.GBNF
import com.mylosoftworks.kotllms.api.StreamedGenerationResult
import com.mylosoftworks.kotllms.api.impl.KoboldCPP
import com.mylosoftworks.kotllms.api.impl.KoboldCPPGenFlags
import com.mylosoftworks.kotllms.api.impl.extenders.toChat
import com.mylosoftworks.kotllms.features.toJson
import com.mylosoftworks.kotllms.chat.ChatMessageWithImages
import com.mylosoftworks.kotllms.chat.ChatDef
import com.mylosoftworks.kotllms.chat.templated.ChatTemplateDSL
import com.mylosoftworks.kotllms.chat.templated.presets.Llama3Template
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
            api.getCurrentModel().modelName
        }
        print("Current model: $modelName\n")
    }

    @Test
    fun checkVersion() {
        val versionInfo = runBlocking {
            api.version()
        }
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
        val flags = KoboldCPPGenFlags().apply {
            prompt = "This is a prompt"
            temperature = 0.7f
        }
        val test = hashMapOf(
            "prompt" to "This is a prompt".toJson(),
            "temperature" to 0.7f.toJson()
        )
        assert(flags.setFlags == test) {"Flags didn't match"}
    }

    @Test
    fun testGen() {
        val start = "This is a short story about a"
        val flags = KoboldCPPGenFlags().apply {
            prompt = start
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
            api.rawGen(flags)
        }
        println(start + result.getText())
    }

    @Test
    fun testSuccessiveGen() {
        runBlocking {
            repeat(2) {
                api.rawGen(KoboldCPPGenFlags().apply {
                    prompt = "Test"
                    maxLength = 10
                })
            }
        }
    }

    @Test
    fun testStream() {
        val start = "This is a short story about a"
        val flags = KoboldCPPGenFlags().apply {
            prompt = start
            maxLength = 50

            stream = true
        }
        runBlocking {
            val result = api.rawGen(flags) as StreamedGenerationResult<*>

            print(start) // Start
            result.registerStreamer {
                print(it.getToken()) // Stream
                System.out.flush() // Show the new tokens even before newline (since print doesn't flush)
                if (it.isLastToken()) println() // End
            }
        }
    }

    @Test
    fun testChat() {
        val exampleChat = ChatDef<ChatMessageWithImages>()
        exampleChat.addMessage(ChatMessageWithImages().apply {
            content = "Hi!"
            role = "bot"
        })
        exampleChat.addMessage(ChatMessageWithImages().apply {
            content = "What's up?"
            role = "user"
        })

        val chatApi = api.toChat(ChatTemplateDSL {
            """
This is an example chat template

This is the beginning of the chat:
${messages.joinToString("\n") {message -> message["role"]?.let { "$it: " } + message["content"]?.let { it } }}
bot:
""".trimIndent()
        })

        val result = runBlocking {
            chatApi.chatGen(exampleChat, KoboldCPPGenFlags().apply {
                maxLength = 200
            })
        }

        println(result.getText())
    }

    // Assuming Llama 3 is loaded with CLIP
    @Test
    fun testImage() {
        val image = ImageIO.read(Path("testres/ReadTest.png").toFile())

        val exampleChat = ChatDef<ChatMessageWithImages>()
        exampleChat.addMessage(ChatMessageWithImages().apply {
            content = "What do you see in this image?"
            role = "user"
            images = listOf(image.toAttached())
        })
        val chatApi = api.toChat(Llama3Template())

        val result = runBlocking {
            chatApi.chatGen(exampleChat)
        }

        println(result.getText())
    }
}
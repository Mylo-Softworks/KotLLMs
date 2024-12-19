package api

import com.mylosoftworks.com.mylosoftworks.gbnfkotlin.GBNF
import com.mylosoftworks.kotllms.api.StreamedGenerationResult
import com.mylosoftworks.kotllms.api.impl.KoboldCPP
import com.mylosoftworks.kotllms.api.impl.KoboldCPPGenFlags
import com.mylosoftworks.kotllms.api.impl.KoboldCPPGenerationResultsStreamed
import com.mylosoftworks.kotllms.api.impl.KoboldCPPStreamChunk
import com.mylosoftworks.kotllms.chat.BasicTemplatedChatMessage
import com.mylosoftworks.kotllms.chat.ChatDef
import com.mylosoftworks.kotllms.chat.ChatMessage
import com.mylosoftworks.kotllms.chat.templated.ChatTemplateDSL
import com.mylosoftworks.kotllms.chat.templated.presets.Llama3Template
import com.mylosoftworks.kotllms.chat.templated.presets.MistralTemplate
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
        val flags = KoboldCPPGenFlags().init {
            prompt = "This is a prompt"
            temperature = 0.7f
        }
        val test = hashMapOf(
            "prompt" to "This is a prompt",
            "temperature" to 0.7f
        )
        assert(flags.setFlags == test) {"Flags didn't match"}
    }

    @Test
    fun testGen() {
        val start = "This is a short story about a"
        val flags = KoboldCPPGenFlags().init {
            prompt = start
            max_length = 50

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
                api.rawGen(KoboldCPPGenFlags().init {
                    prompt = "Test"
                    max_length = 10
                })
            }
        }
    }

    @Test
    fun testStream() {
        val start = "This is a short story about a"
        val flags = KoboldCPPGenFlags().init {
            prompt = start
            max_length = 50

            stream = true
        }
        runBlocking {
            val result = api.rawGen(flags) as KoboldCPPGenerationResultsStreamed

            print(start) // Start
            result.registerStreamer {
                print(it.token) // Stream
                System.out.flush() // Show the new tokens even before newline (since print doesn't flush)
                if (it.isLastToken()) println() // End
            }
        }
    }

    @Test
    fun testChat() {
        val exampleChat = ChatDef<BasicTemplatedChatMessage>()
        exampleChat.addMessage(BasicTemplatedChatMessage().init {
            content = "Hi!"
            role = "bot"
        })
        exampleChat.addMessage(BasicTemplatedChatMessage().init {
            content = "What's up?"
            role = "user"
        })
        api.settings.template = ChatTemplateDSL {
"""
This is an example chat template

This is the beginning of the chat:
${messages.joinToString("\n") {message -> message["role"]?.let { "$it: " } + message["content"]?.let { it } }}
bot:
""".trimIndent()
        }

        val result = runBlocking {
            api.chatGen(exampleChat, KoboldCPPGenFlags().init {
                max_length = 200
            })
        }

        println(result.getText())
    }

    // Assuming Llama 3 is loaded with CLIP
    @Test
    fun testImage() {
        val image = ImageIO.read(Path("testres/ReadTest.png").toFile())

        val exampleChat = ChatDef<BasicTemplatedChatMessage>()
        exampleChat.addMessage(BasicTemplatedChatMessage().init {
            content = "What do you see in this image?"
            role = "user"
            images = listOf(image)
        })
        api.settings.template = Llama3Template()

        val result = runBlocking {
            api.chatGen(exampleChat)
        }

        println(result.getText())
    }
}
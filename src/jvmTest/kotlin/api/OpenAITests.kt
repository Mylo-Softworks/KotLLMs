package api

import com.mylosoftworks.kotllms.api.StreamedGenerationResult
import com.mylosoftworks.kotllms.api.impl.OpenAI
import com.mylosoftworks.kotllms.api.impl.OpenAISettings
import com.mylosoftworks.kotllms.api.impl.extenders.toChat
import com.mylosoftworks.kotllms.chat.features.ChatFeatureImages
import com.mylosoftworks.kotllms.chat.templated.presets.Llama3Template
import com.mylosoftworks.kotllms.features.impl.ChatGen
import com.mylosoftworks.kotllms.runIfImpl
import com.mylosoftworks.kotllms.shared.toAttached
import kotlinx.coroutines.runBlocking
import javax.imageio.ImageIO
import kotlin.io.path.Path
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class OpenAITests {
//    val api: OpenAI = OpenAI(OpenAISettings(System.getenv("openai_key"))) // Can't test on official OpenAI api currently since I don't have credits
//    val api: OpenAI = OpenAI(OpenAISettings(null, url = "http://localhost:5001/v1/"))
    val api: OpenAI = OpenAI(OpenAISettings(System.getenv("openrouter_key"), url = "https://openrouter.ai/api/v1"))

    @Test
    fun check() {
        runBlocking {
            assertTrue(api.check())
        }
    }

    @Test
    fun listModels() {
        runBlocking {
            println(api.listModels().getOrNull()?.map { it.modelName })
            assertNotNull(api.listModels().getOrNull())
        }
    }

    @Test
    fun testGeneration() {
        runBlocking {
            val result = api.rawGen("This is a short story about a", api.buildFlags { maxLength = 200 }).getOrThrow()
            println(result.getText())
        }
    }

    @Test
    fun testStreaming() {
        runBlocking {
            val result = api.rawGen("This is a short story about a", api.buildFlags { maxLength = 200;stream = true }).getOrThrow() as StreamedGenerationResult<*>
            result.registerStreamer {
                val chunk = it.getOrThrow()
                print(chunk.getTokenF())
                System.out.flush()
                if (chunk.isLastToken()) println()
            }
        }
    }

    @Test
    fun testChat() {
        runBlocking {
            val chat = api.createChat {
                createMessage {
                    content = "You are a helpful AI assistant."
                    role = ChatGen.ChatRole.System
                }
                createMessage {
                    content = "What can you do?"
                    role = ChatGen.ChatRole.User
                }
            }

            val result = api.chatGen(chat, api.buildFlags { maxLength = 200;model = "google/gemini-2.0-flash-lite-preview-02-05:free" }).getOrThrow()

            println(result.getText())
        }
    }

    @Test
    fun testChatStreaming() {
        runBlocking {
            val chat = api.createChat {
                createMessage {
                    content = "You are a helpful AI assistant."
                    role = ChatGen.ChatRole.System
                }
                createMessage {
                    content = "What can you do?"
                    role = ChatGen.ChatRole.User
                }
            }

            val stream = api.chatGen(chat, api.buildFlags { maxLength = 200;model = "google/gemini-2.0-flash-lite-preview-02-05:free";stream = true }).getOrThrow() as StreamedGenerationResult<*>

            stream.registerStreamer {
                val chunk = it.getOrThrow()
                print(chunk.getTokenF())
                System.out.flush()
                if (chunk.isLastToken()) println()
            }
        }
    }

    // Assuming the current api supports images in chats
    @Test
    fun testImage() {
        val image = ImageIO.read(Path("testres/ReadTest.png").toFile())

        val exampleChat = api.createChat {
            createMessage {
                content = "What is the text in the image? Reply with the text only."
                role = ChatGen.ChatRole.User
                runIfImpl<ChatFeatureImages> {
                    images = listOf(image.toAttached())
                }
            }
        }

        val result = runBlocking {
            api.chatGen(exampleChat, api.buildFlags { maxLength = 200;model = "google/gemini-2.0-flash-lite-preview-02-05:free" })
        }.getOrThrow()

        println(result.getText())
    }
}
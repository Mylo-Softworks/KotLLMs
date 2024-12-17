package api

import com.mylosoftworks.com.mylosoftworks.gbnfkotlin.GBNF
import com.mylosoftworks.kotllms.api.StreamedGenerationResult
import com.mylosoftworks.kotllms.api.impl.KoboldCPP
import com.mylosoftworks.kotllms.api.impl.KoboldCPPGenFlags
import com.mylosoftworks.kotllms.api.impl.KoboldCPPGenerationResultsStreamed
import com.mylosoftworks.kotllms.api.impl.KoboldCPPStreamChunk
import kotlinx.coroutines.runBlocking
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
}
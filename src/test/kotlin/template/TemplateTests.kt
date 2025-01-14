package template

import com.mylosoftworks.kotllms.chat.BasicChatMessage
import com.mylosoftworks.kotllms.chat.ChatDef
import com.mylosoftworks.kotllms.chat.templated.ChatTemplateDSL
import com.mylosoftworks.kotllms.chat.templated.presets.Llama3Template
import kotlin.test.Test

class TemplateTests {
    @Test
    fun testTemplate() {
        val exampleChat = ChatDef<BasicChatMessage>()
        exampleChat.addMessage(BasicChatMessage().init {
            content = "Hi!"
            role = "bot"
        })
        exampleChat.addMessage(BasicChatMessage().init {
            content = "What's up?"
            role = "user"
        })
        val template = ChatTemplateDSL {
            """
This is an example chat template

This is the beginning of the chat:
${messages.joinToString("\n") {message -> message["role"]?.let { "$it: " } + message["content"]?.let { it } }}
bot:
""".trimIndent()
        }

        assert(template.formatChat(exampleChat) == """
            This is an example chat template

            This is the beginning of the chat:
            bot: Hi!
            user: What's up?
            bot:
        """.trimIndent()) {"Template didn't match test case"}
    }

    @Test
    fun testTemplatePreset() {
        val exampleChat = ChatDef<BasicChatMessage>()
        exampleChat.addMessage(BasicChatMessage().init {
            content = "Hi!"
            role = "assistant"
        })
        exampleChat.addMessage(BasicChatMessage().init {
            content = "What's up?"
            role = "user"
        })

        val template = Llama3Template()

        println(template.formatChat(exampleChat))
    }
}
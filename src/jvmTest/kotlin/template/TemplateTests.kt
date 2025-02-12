package template

import com.mylosoftworks.kotllms.chat.ChatMessageTemplated
import com.mylosoftworks.kotllms.chat.ChatDef
import com.mylosoftworks.kotllms.chat.templated.ChatTemplateDSL
import com.mylosoftworks.kotllms.chat.templated.presets.Llama3Template
import com.mylosoftworks.kotllms.features.impl.ChatRole
import kotlin.test.Test

class TemplateTests {
    @Test
    fun testTemplate() {
        val exampleChat = ChatDef{ChatMessageTemplated()}.apply {
            createMessage {
                content = "Hi!"
                role = ChatRole.Assistant
            }
            createMessage {
                content = "What's up?"
                role = ChatRole.User
            }
        }
        val template = ChatTemplateDSL {
            """
This is an example chat template

This is the beginning of the chat:
${messages.joinToString("\n") {message -> message["role"]?.let { "$it: " } + message["content"]?.let { it } }}
assistant:
""".trimIndent()
        }

        assert(template.formatChat(exampleChat) == """
            This is an example chat template

            This is the beginning of the chat:
            assistant: Hi!
            user: What's up?
            assistant:
        """.trimIndent()) { "Template didn't match test case" }
    }

    @Test
    fun testTemplatePreset() {
        val exampleChat = ChatDef{ChatMessageTemplated()}.apply {
            createMessage {
                content = "Hi!"
                role = ChatRole.Assistant
            }
            createMessage {
                content = "What's up?"
                role = ChatRole.User
            }
        }

        val template = Llama3Template()

        println(template.formatChat(exampleChat))
    }
}
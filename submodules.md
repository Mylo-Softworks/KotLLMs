# Submodules
## Kotllms.functions
Function calling submodule, allows you to define functions, let the LLM pick one, and request parameters for your function call. Using GBNF grammars, so even LLMs not made for function calling can still perform them!

### Usage
> The chat should contain information about the available functions, otherwise the llm is less likely to pick the correct option,
> while it will always pick an existing function name through the grammar, providing information is crucial for proper function selection.

> The way the context is provided to the LLM is up to your implementation. Context is provided through chat messages, your api needs to support those,
> some apis, like KoboldCPP, support chat messages through templates. See: `Llama3Template`, `MistralTemplate` and `ChatTemplateDSL` for quick manual implementations.
>
> Also, see `ChatDef.subChat(count, prefix, suffix)` for simple chat modifications, like taking the last 10 messages, and adding a system message to the start without editing the original chat object.

Example of a function definition (To define one function named "print" which prints "Print: " with the message):
```kotlin
import com.mylosoftworks.kotllms.functions.*

val functions = FunctionDefs {
    function("print", "Prints a piece of text to the user.") {
        addParam(FunctionParameterString("value", false, "The value to print to the user"))

        callback = {
            println("Print: ${it["value"]?.second}")
        }
    }
}
```

In order to request the LLM to select and call a function with parameters, assuming your chat is in a variable named "chat" and you are already in a suspend context:
```kotlin
val (raw, func, comment) = functions.requestFunctionCallSingleRequest(api, flags, chat)
println("Comment: $comment")
func?.let { it() } // Calling the chosen function
```

TODO: Write new javadoc  
TODO: Add better DSL for function calling which would allow parameters to be defined through a delegate, and accessed as if it were a regular variable.  
TODO: Support writing a custom builder for giving function definitions to the llm, make the current pseudo-json builder default. Similar to how FunctionGrammarDef can now be replaced.
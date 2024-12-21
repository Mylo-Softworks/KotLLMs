# Submodules
## Kotllms.functions
Function calling submodule, allows you to define functions, let the LLM pick one, and request parameters for your function call. Using GBNF grammars, so even LLMs not made for function calling can still perform them!

### Usage
> The chat should contain information about the available functions, otherwise the llm is less likely to pick the correct option,
> while it will always pick an existing function name through the grammar, providing information is crucial for proper function selection.

> You can use `functions.getDescriptionForAllCalls()` to get a json-like structure which can provide context to the LLM about which functions are available.
> When the LLM has selected a function, you can use `function.getExtendedDescriptionForFunction()` to give the long description for the single selected function.

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

In order to request the LLM to select a function, assuming your chat is in a variable named "chat" and you are already in a suspend context:
```kotlin
val targetCall = functions.requestFunctionCall(api, flags, chat) // Triple(raw response string, function object, throughts string)
val targetFunction = targetCall.second
if (targetFunction != null) {
    val description = targetFunction.getExtendedDescriptionForFunction() // The description with full info about the parameters
    // TODO: use the description, like with chat.subChat()
    // Assuming the new subchat is still named "chat"
    val readyCall = targetFunction.requestCallFunction(api, flags, chat) // Pair(raw response string, callable to finalize the call)
    // To execute the function call with the parameters given by the LLM
    readyCall.second()
}
else {
    // Something went wrong during the generation
}
```

### Usage (Single request)
TODO: Write documentation for `FunctionDefs.requestFunctionCallSingleRequest()`
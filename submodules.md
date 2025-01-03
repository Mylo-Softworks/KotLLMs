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
    function<Unit>("print", "Prints a piece of text to the user.") {
        val value = addParam(FunctionParameterString("value", false, "The value to print to the user"))

        callback = {
            println("Print: ${value(it)}") // Invoking "value" with "it" is a quick and safe way to obtain the value in this context
        }
    }
}
```

In order to request the LLM to select and call a function with parameters, assuming your chat is in a variable named "chat" and you are already in a suspend context:
```kotlin
val (raw, func, comment) = functions.requestFunctionCallSingleRequest(api, flags, chat).getOrThrow() // Result, getOrThrow should only be used if you're certain it's going to work and you're already in a safe context
println("Comment: $comment")
func?.let { it() } // Calling the chosen function
```

### Custom function call formats
Custom function call formats can be defined through a GBNF grammar, using `AutoParsedGrammarDef`, or manually, by implementing `FunctionGrammarDef`.  
It works by parsing the output with a custom (experimental) GBNF parser.

Any GBNF grammar is allowed as long as it's parsable and **follows these rules**:
1. Thoughts should be stored in an entity named "`thoughts`"  (fallback to empty string, in which case you won't be able to read it back)
2. Functions should be stored in an entity named "`function-{name}`" where `{name}` is the name of the function.
3. (The parsable value part of) function parameters should be stored in an entity named "`param-{function}-{name}`", inside of the function entity where `{function}` is the function name (to prevent name collisions) and `{name}` is the name of the parameter.

Below is the default grammar (`DefaultFunctionGrammar`), defined through the `AutoParsedGrammarDef` format:
```kotlin
val autoParsedExample = AutoParsedGrammarDef {
    val thoughts = entity("thoughts") { repeat(max = 100) { range("\\\"\\n", true) } }

    val allFunctions = it.functions.values.map {func ->
        entity("function-${func.name}") {
            literal(func.name + "\n--params--\n")
            for (param in func.params.values) {
                if (param.optional) {
                    optional {
                        literal("${param.name}: ")
                        entity("param-${func.name}-${param.name}") currentParam@{
                            param.addGBNFRule(this@currentParam) // Value part
                        }() // Immediately insert it after declaring
                        literal("\n")
                    }
                }
                else {
                    literal("${param.name}: ")
                    entity("param-${func.name}-${param.name}") currentParam@{
                        param.addGBNFRule(this@currentParam) // Value part
                    }() // Immediately insert it after declaring
                    literal("\n")
                }
            }
        }
    }

    literal("Thoughts: \"")
    thoughts()
    literal("\"\nCall: ")
    oneOf {
        allFunctions.forEach {
            it()
        }
    }
}
```

TODO: Support for multiple functions in one call, using the same original function calling syntax
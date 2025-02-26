[![](https://www.jitpack.io/v/Mylo-Softworks/KotLLMs.svg)](https://www.jitpack.io/#Mylo-Softworks/KotLLMs)

# KotLLMs (Kotlin LLMs)
An experimental library for calling LLM apis from Kotlin.

Everything is bound to change

| Feature                                                 | Status                        |
|---------------------------------------------------------|-------------------------------|
| Raw prompting                                           | ✅                             |
| Chat prompting                                          | ✅                             |
| Chat templates (with DSL)                               | ✅                             |
| Result streaming                                        | ✅                             |
| Generation flags                                        | ✅                             | <!--Flags for generations, like grammars, model selection, etc.-->
| Message flags                                           | ✅                             | <!--Flags for individual messages, like role, attached images, etc.-->
| Images in prompts                                       | ✅ (only JVM + JS currently)   |
| Grammars (with DSL)                                     | ✅                             |
| Function/tool calling                                   | ✅ (Using Functions submodule) |
| Json schemas                                            | ✅ (For non-recursive schemas) | <!--Use $defs if you want to implement recursion. This could be a separate code block opened like defs {}-->
| Json schema to Grammar converter                        | ✅                             |
| Json schema fixer (ensures json schema can be streamed) | ✅                             |

# Current implemented APIs
* [KoboldCPP](https://github.com/LostRuins/koboldcpp)
* [OpenAI](https://platform.openai.com/docs/api-reference/) + Proxies

... More coming soon!

# Implementing your own APIs
To implement your own API, simply make a class which extends `API<S: Settings, F : Flags<*>>`. Settings is an object provided to the API that can be used to store an endpoint, an api key, or similar. In raw-only apis the settings object can be used to store a template, to allow for formatting chats.

# Submodules
* Function calling (+ tools)
  1. Grammar-based ✅
  2. Tools (api built-in) ✅ (TODO: Support proper tool returns using call ids)

See [submodules](submodules.md).

# Usage

## Initializing an API
```kotlin
// Init KoboldCPP with default localhost endpoint
val api = KoboldCPP()

// Init KoboldCPP with a custom endpoint (localhost with port 5002 instead of default 5001)
val api = KoboldCPP(KoboldCPPSettings("http://localhost:5002"))

// Init KoboldCPP with a Llama 3 chat template
val api = KoboldCPP(KoboldCPPSettings()).toChat(Llama3Template())
// The original KoboldCPP object is available through api.getWrapped().
```

## Performing a raw generation call
```kotlin
val start = "This is a short story about a"
// KoboldCPP flags since it's the api we're using, createFlags() creates a flags object for whichever api you're using, some apis might have flags that others don't.
val flags = api.buildFlags {
    max_length = 50
}
// Assuming we're already in a suspend context
val result = api.rawGen(start, flags).getOrThrow()

print(result.getText())
```

## Streaming responses
```kotlin
val start = "This is a short story about a"
// Some apis might have flags which others don't, if you need a method to support any API, use runIfImpl<Type> {  }
val flags = api.buildFlags {
    max_length = 50
    
    stream = true
}
// Assuming we're already in a suspend context
// Either
val result = api.rawGen(start, flags).getOrThrow() as KoboldCPPGenerationResultsStreamed
// Or the more generic
val result = api.rawGen(start, flags).getOrThrow() as StreamedGenerationResult<*>

// Now we can listen on the stream
result.registerStreamer {
    val chunk = it.getOrThrow() // Using Result
    print(chunk.getToken()) // Stream
    System.out.flush() // Show the new tokens even before newline (since print doesn't flush)
    if (chunk.isLastToken()) println() // End
}
```

## Performing a chat generation call
> Chat generations require apis which implement the ChatGen interface.  
> Chat support can be added to any `RawGen` API using a template with `api.toChat(template)`.

> Custom chat templates can be created by extending `ChatTemplate`, using `ChatTemplateDSL`.
```kotlin
// Universal syntax for creating chat messages from any chat API
val exampleChat = api.createChat {
    createMessage {
        content = "You are a helpful AI assistant."
        role = ChatGen.ChatRole.System
    }
    createMessage {
        content = "Who are you?"
        role = ChatGen.ChatRole.User
    }
}
val flags = api.buildFlags {
    max_length = 50
}
val result = api.chatGen(exampleChat, flags).getOrThrow()
```

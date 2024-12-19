# KotLLMs (Kotlin LLMs)
A work in progress library for calling LLM apis from Kotlin.

| Feature                   | Status |
|---------------------------|--------|
| Raw prompting             | ✅      |
| Chat prompting            | ✅      |
| Chat templates (with DSL) | ✅      |
| Result streaming          | ✅      |
| Generation flags          | ✅      | <!--Flags for generations, like grammars, model selection, etc.-->
| Message flags             | ✅      | <!--Flags for individual messages, like role, attached images, etc.-->
| Images in prompts         | ✅      |
| Grammars (with DSL)       | ✅      |

# Current implemented APIs
* [KoboldCPP](https://github.com/LostRuins/koboldcpp)

... More coming soon!

# Implementing your own APIs
To implement your own API, simply make a class which extends `API<S: Settings, F : Flags<*>>`. Settings is an object provided to the API that can be used to store an endpoint, an api key, or similar. In raw-only apis the settings object can be used to store a template, to allow for formatting chats.

# Submodules
See [submodules](submodules.md).
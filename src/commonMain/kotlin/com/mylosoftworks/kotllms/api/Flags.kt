package com.mylosoftworks.kotllms.api

import com.mylosoftworks.gbnfkotlin.GBNF
import com.mylosoftworks.kotllms.jsonSettings
import kotlinx.serialization.json.*
import kotlin.reflect.KProperty

interface ToJson {
    fun toJson(): JsonElement
}
// Polymorphic json-serializable classes for some types
fun Any?.toJson(): JsonElement = when (this) {
    is JsonElement -> this
    is ToJson -> this.toJson()

    is String -> jsonSettings.encodeToJsonElement(this)
    is Int -> jsonSettings.encodeToJsonElement(this)
    is Float -> jsonSettings.encodeToJsonElement(this)
    is Boolean -> jsonSettings.encodeToJsonElement(this)
    is List<*> -> jsonSettings.encodeToJsonElement(this.map { it.toJson() })
    is Array<*> -> jsonSettings.encodeToJsonElement(this.map { it.toJson() })
    is HashMap<*, *> -> jsonSettings.encodeToJsonElement(this.mapKeys { it.toString() }.mapValues { it.toJson() })
    null -> JsonNull
    else -> TODO(this.toString())
}

fun JsonElement.getPrimitiveValue(): Any? {
    val primitive = this.jsonPrimitive
    return primitive.booleanOrNull ?: primitive.intOrNull ?: primitive.floatOrNull ?: primitive.contentOrNull
}


/**
 * Base class for flags, which contain extra information for a generation, such as the sampling temperature.
 */
abstract class Flags<T : Flags<T>> {
//    val setFlags = hashMapOf<String, Any>()
    val setFlags = hashMapOf<String, JsonElement>()

    fun applyToRequestJson(map: HashMap<String, JsonElement>) {
        map.putAll(setFlags)
    }

    inline fun init(init: T.() -> Unit): T {
        init(this as T)
        return this
    }

    // Optionally implemented features for better cross-compatibility
    open fun applyGrammar(grammar: GBNF): Unit = error("This api doesn't support GBNF grammar through applyGrammar()")

    open fun enableEarlyStopping(enable: Boolean): Unit = error("This api doesn't support changing early stopping through enableEarlyStopping()")
}

class Flag<T> {
    operator fun <U : Flags<U>> getValue(thisRef: U, property: KProperty<*>): T? {
        @Suppress("UNCHECKED_CAST")
        return thisRef.setFlags.getOrElse(property.name) { null }?.getPrimitiveValue() as T?
    }

    operator fun <U : Flags<U>> setValue(thisRef: U, property: KProperty<*>, value: T?) {
        if (value == null) {
            thisRef.setFlags.remove(property.name)
            return
        }
        thisRef.setFlags[property.name] = value.toJson()
    }
}

class ConvertedFlag<T, V>(val convert: (T) -> V) {
    operator fun <U : Flags<U>> getValue(thisRef: U, property: KProperty<*>): Any? {
        @Suppress("UNCHECKED_CAST")
        return thisRef.setFlags.getOrElse(property.name) { null }?.getPrimitiveValue() as V?
    }

    operator fun <U : Flags<U>> setValue(thisRef: U, property: KProperty<*>, value: Any?) {
        if (value == null) {
            thisRef.setFlags.remove(property.name)
            return
        }
        thisRef.setFlags[property.name] = convert(value as T)!!.toJson()
    }
}

class BiConvertedStringFlag<T>(val convert: (T) -> String, val unConvert: (String) -> T) {
    operator fun <U : Flags<U>> getValue(thisRef: U, property: KProperty<*>): T? {
        return unConvert(thisRef.setFlags.getOrElse(property.name) { null }?.getPrimitiveValue().toString())
    }

    operator fun <U : Flags<U>> setValue(thisRef: U, property: KProperty<*>, value: T?) {
        if (value == null) {
            thisRef.setFlags.remove(property.name)
            return
        }
        thisRef.setFlags[property.name] = convert(value).toJson()
    }
}

class BiConvertedJsonFlag<T>(val convert: (T) -> JsonElement, val unConvert: (JsonElement) -> T) {
    operator fun <U : Flags<U>> getValue(thisRef: U, property: KProperty<*>): T? {
        return (thisRef.setFlags.getOrElse(property.name) { null } as JsonElement?)?.let { unConvert(it) }
    }

    operator fun <U : Flags<U>> setValue(thisRef: U, property: KProperty<*>, value: T?) {
        if (value == null) {
            thisRef.setFlags.remove(property.name)
            return
        }
        thisRef.setFlags[property.name] = convert(value).toJson()
    }
}

fun GBNFFlag() = ConvertedFlag<GBNF, String> {
    it.compile()
}

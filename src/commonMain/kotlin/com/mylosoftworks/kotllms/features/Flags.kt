package com.mylosoftworks.kotllms.features

import com.mylosoftworks.gbnfkotlin.GBNF
import com.mylosoftworks.gbnfkotlin.interpreting.GBNFInterpreter
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
    is HashMap<*, *> -> jsonSettings.encodeToJsonElement(this.mapKeys { it.key.toString() }.mapValues { it.value.toJson() })
    null -> JsonNull
    else -> throw NotImplementedError("Serialization for $this is not implemented.")
}

fun JsonElement.getPrimitiveValue(): Any? {
    val primitive = this.jsonPrimitive
    return primitive.booleanOrNull ?: primitive.intOrNull ?: primitive.floatOrNull ?: primitive.contentOrNull
}

fun JsonElement.stringOrToString(): String {
    if (this is JsonPrimitive) return this.content
    return this.toString()
}

interface Flaggable<T: Any> {
    val setFlags: HashMap<String, T>
}

/**
 * Base class for flags, which contain extra information for a generation, such as the sampling temperature.
 */
abstract class Flags: Flaggable<JsonElement> {
    override val setFlags = hashMapOf<String, JsonElement>()

    fun applyToRequestJson(map: HashMap<String, JsonElement>) {
        preApply()
        map.putAll(setFlags)
    }

    /**
     * Override this if the implemented API requires a parameter and there's a valid default which could be set.
     */
    open fun preApply() {}
}

/**
 * Create a basic flag `var flagName by flag<String>()`
 */
fun <T: Any> flag(altName: String? = null) = Flag<T, Any>(altName)

/**
 * Create a flag which maps its values to V and back
 */
fun <T: Any, V: Any> mappedFlag(altName: String? = null, mapTo: T.() -> V = { error("Not implemented") }, mapFrom: V.() -> T = { error("Not implemented") }) = MappedFlag(altName, mapTo, mapFrom)

/**
 * Change the backing field of a class to use Json. Allows supplying custom json convert functions.
 */
@Suppress("UNCHECKED_CAST")
fun <T: Any> Flag<T, Any>.jsonBacked(
    convertTo: T.() -> JsonElement = { this.toJson() },
    convertFrom: JsonElement.() -> T? = { this.getPrimitiveValue() as? T? }
): Flag<T, JsonElement> = JsonBacked(this, convertTo, convertFrom)

fun <T: Any, V: Any> Flag<T, V>.watch(onChange: (T?) -> Unit) = WatchedFlag(this, onChange)

/**
 * A special flag for GBNF grammar entities.
 */
fun gbnfFlag(altName: String? = null) = flag<GBNF>(altName)
    .jsonBacked(
        { this.compile().toJson() },
        { GBNFInterpreter.interpretGBNF(this.getPrimitiveValue() as? String ?: "").getOrNull()}
    )

fun stringListFlag(altName: String? = null) = flag<List<String>>(altName).jsonBacked({ jsonSettings.encodeToJsonElement(this) },
    { this.jsonArray.map { it.jsonPrimitive.content }.toList() })

/**
 * @param T The type we can interact with
 * @param V The backing type
 */
open class Flag<T: Any, V: Any>(val altName: String? = null) {
    fun effectiveName(prop: KProperty<*>) = altName ?: prop.name

    @Suppress("UNCHECKED_CAST")
    open operator fun <U: Flaggable<V>> getValue(thisRef: U, property: KProperty<*>): T? {
        return thisRef.setFlags.getOrElse(effectiveName(property)) { null } as? T?
    }

    @Suppress("UNCHECKED_CAST")
    open operator fun <U: Flaggable<V>> setValue(thisRef: U, property: KProperty<*>, value: T?) {
        if (value == null) {
            thisRef.setFlags.remove(effectiveName(property))
            return
        }
        thisRef.setFlags[effectiveName(property)] = value as V
    }
}

open class MappedFlag<T: Any, V: Any>(altName: String? = null, val mapTo: T.() -> V, val mapFrom: V.() -> T): Flag<T, V>(altName) {
    override fun <U : Flaggable<V>> getValue(thisRef: U, property: KProperty<*>): T? {
        return thisRef.setFlags.getOrElse(effectiveName(property)) { null }?.mapFrom()
    }

    override fun <U : Flaggable<V>> setValue(thisRef: U, property: KProperty<*>, value: T?) {
        if (value == null) {
            thisRef.setFlags.remove(effectiveName(property))
            return
        }
        thisRef.setFlags[effectiveName(property)] = value.mapTo()
    }
}

@Suppress("UNCHECKED_CAST")
open class JsonBacked<T: Any>(flag: Flag<T, Any>,
                              val convertTo: T.() -> JsonElement = { this.toJson() },
                              val convertFrom: JsonElement.() -> T? = { this.getPrimitiveValue() as? T? }
): Flag<T, JsonElement>(flag.altName) {
    override fun <U : Flaggable<JsonElement>> getValue(thisRef: U, property: KProperty<*>): T? {
        return thisRef.setFlags.getOrElse(effectiveName(property)) { null }?.convertFrom()
    }

    override fun <U : Flaggable<JsonElement>> setValue(thisRef: U, property: KProperty<*>, value: T?) {
        if (value == null) {
            thisRef.setFlags.remove(effectiveName(property))
            return
        }
        thisRef.setFlags[effectiveName(property)] = value.convertTo()
    }
}

@Suppress("UNCHECKED_CAST")
open class WatchedFlag<T: Any, V: Any>(val flag: Flag<T, V>, val onChange: (T?) -> Unit): Flag<T, V>() {
    override fun <U : Flaggable<V>> getValue(thisRef: U, property: KProperty<*>): T? {
        return flag.getValue(thisRef, property)
    }

    override fun <U : Flaggable<V>> setValue(thisRef: U, property: KProperty<*>, value: T?) {
        onChange(value)
        flag.setValue(thisRef, property, value)
    }
}
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
    is HashMap<*, *> -> jsonSettings.encodeToJsonElement(this.mapKeys { it.toString() }.mapValues { it.toJson() })
    null -> JsonNull
    else -> TODO(this.toString())
}

fun JsonElement.getPrimitiveValue(): Any? {
    val primitive = this.jsonPrimitive
    return primitive.booleanOrNull ?: primitive.intOrNull ?: primitive.floatOrNull ?: primitive.contentOrNull
}

interface Flaggable<T: Any> {
    val setFlags: HashMap<String, T>
}

/**
 * Base class for flags, which contain extra information for a generation, such as the sampling temperature.
 */
abstract class Flags<T : Flags<T>>: Flaggable<JsonElement> {
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

//open class Flag<T>(val altName: String? = null) {
//    open operator fun <U: Flaggable<T>> getValue(thisRef: U, property: KProperty<*>): T? {
//        @Suppress("UNCHECKED_CAST")
//        return thisRef.setFlags.getOrElse(altName ?: property.name) { null }
//    }
//
//    open operator fun <U: Flaggable<T>> setValue(thisRef: U, property: KProperty<*>, value: T?) {
//        if (value == null) {
//            thisRef.setFlags.remove(altName ?: property.name)
//            return
//        }
//        thisRef.setFlags[altName ?: property.name] = value
//    }
//}
//
///**
// * Maps true to false, and false to true, and null to null
// */
//class InvertedBoolFlag(altName: String? = null): Flag<Boolean>(altName) {
//    override fun <U : Flags<U>> getValue(thisRef: U, property: KProperty<*>): Boolean? {
//        return super.getValue(thisRef, property)?.let { !it }
//    }
//
//    override fun <U : Flags<U>> setValue(thisRef: U, property: KProperty<*>, value: Boolean?) {
//        super.setValue(thisRef, property, value?.let { !it })
//    }
//}
//
//class ConvertedFlag<T, V>(val altName: String? = null, val convert: (T) -> V) {
//    operator fun <U : Flags<U>> getValue(thisRef: U, property: KProperty<*>): Any? {
//        @Suppress("UNCHECKED_CAST")
//        return thisRef.setFlags.getOrElse(altName ?: property.name) { null }?.getPrimitiveValue() as V?
//    }
//
//    operator fun <U : Flags<U>> setValue(thisRef: U, property: KProperty<*>, value: Any?) {
//        if (value == null) {
//            thisRef.setFlags.remove(altName ?: property.name)
//            return
//        }
//        thisRef.setFlags[altName ?: property.name] = convert(value as T)!!.toJson()
//    }
//}
//
//class BiConvertedStringFlag<T>(val altName: String? = null, val convert: (T) -> String, val unConvert: (String) -> T) {
//    operator fun <U : Flags<U>> getValue(thisRef: U, property: KProperty<*>): T? {
//        return unConvert(thisRef.setFlags.getOrElse(altName ?: property.name) { null }?.getPrimitiveValue().toString())
//    }
//
//    operator fun <U : Flags<U>> setValue(thisRef: U, property: KProperty<*>, value: T?) {
//        if (value == null) {
//            thisRef.setFlags.remove(altName ?: property.name)
//            return
//        }
//        thisRef.setFlags[altName ?: property.name] = convert(value).toJson()
//    }
//}
//
//class BiConvertedJsonFlag<T>(val altName: String? = null, val convert: (T) -> JsonElement, val unConvert: (JsonElement) -> T) {
//    operator fun <U : Flags<U>> getValue(thisRef: U, property: KProperty<*>): T? {
//        return (thisRef.setFlags.getOrElse(altName ?: property.name) { null } as JsonElement?)?.let { unConvert(it) }
//    }
//
//    operator fun <U : Flags<U>> setValue(thisRef: U, property: KProperty<*>, value: T?) {
//        if (value == null) {
//            thisRef.setFlags.remove(altName ?: property.name)
//            return
//        }
//        thisRef.setFlags[altName ?: property.name] = convert(value).toJson()
//    }
//}
//
//fun GBNFFlag(altName: String? = null) = BiConvertedStringFlag(altName, {it?.compile() ?: ""}, {GBNFInterpreter.interpretGBNF(it).getOrNull()})
package com.mylosoftworks.kotllms.api

import com.mylosoftworks.com.mylosoftworks.gbnfkotlin.GBNF
import kotlin.reflect.KProperty

/**
 * Base class for flags, which contain extra information for a generation, such as the sampling temperature.
 */
abstract class Flags<T : Flags<T>> {
    val setFlags = hashMapOf<String, Any>()

    fun applyToRequestJson(map: HashMap<String, Any>) {
        map.putAll(setFlags)
    }

    fun init(init: T.() -> Unit): T {
        init(this as T)
        return this
    }
}

class Flag<T> {
    operator fun <U : Flags<U>> getValue(thisRef: U, property: KProperty<*>): T? {
        @Suppress("UNCHECKED_CAST")
        return thisRef.setFlags.getOrDefault(property.name, null) as T?
    }

    operator fun <U : Flags<U>> setValue(thisRef: U, property: KProperty<*>, value: T?) {
        if (value == null) {
            thisRef.setFlags.remove(property.name)
            return
        }
        thisRef.setFlags[property.name] = value
    }
}

class ConvertedFlag<T, V>(val convert: (T) -> V) {
    operator fun <U : Flags<U>> getValue(thisRef: U, property: KProperty<*>): Any? {
        @Suppress("UNCHECKED_CAST")
        return thisRef.setFlags.getOrDefault(property.name, null) as V?
    }

    operator fun <U : Flags<U>> setValue(thisRef: U, property: KProperty<*>, value: Any?) {
        if (value == null) {
            thisRef.setFlags.remove(property.name)
            return
        }
        thisRef.setFlags[property.name] = convert(value as T)!!
    }
}

fun GBNFFlag() = ConvertedFlag<GBNF, String> {
    it.compile()
}

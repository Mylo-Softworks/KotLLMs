package com.mylosoftworks.kotllms.features

import com.mylosoftworks.kotllms.Union
import com.mylosoftworks.kotllms.nonNull
import kotlin.reflect.KClass

/**
 * A wrapper type, should be implemented
 */
interface Wrapper<T: Any> {
    /**
     * Access the upper wrapped type, could be self.
     */
    fun getWrapped(): T {
        var linked = getLinked()
        while (true) {
            linked.nonNull().let {
                if (targetClass().isInstance(it)) {
                    @Suppress("unchecked_cast")
                    return it as T
                }
                else {
                    linked = getLinked() // Go higher and try again
                }
            }
        }
    }

    fun getLinked(): Union<T, Wrapper<T>>
    fun targetClass(): KClass<*>
}

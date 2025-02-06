package com.mylosoftworks.kotllms.features

import com.mylosoftworks.kotllms.Union
import com.mylosoftworks.kotllms.nonNull
import com.mylosoftworks.kotllms.toUnion2
import kotlin.reflect.KClass

/**
 * A wrapper type, should be implemented
 */
interface Wrapper<T: Any> {
    /**
     * Access the upper wrapped type, could be self.
     */
    fun getWrapped(): T {
        return findWrapped()
    }

    /**
     * Similar to getWrapped, but allows for specifying a custom target type.
     */
    @Suppress("unchecked_cast")
    fun <Target: Any> findWrapped(targetClass: KClass<Target> = targetClass() as KClass<Target>): Target {
        if (targetClass.isInstance(this)) return this as Target
        var linked = this.toUnion2<T, Wrapper<T>>()
        while (true) {
            linked.nonNull().let {
                if (targetClass.isInstance(it)) {
                    @Suppress("unchecked_cast")
                    return it as Target
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

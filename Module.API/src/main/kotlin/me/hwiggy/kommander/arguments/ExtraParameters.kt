package me.hwiggy.kommander.arguments

import kotlin.reflect.KProperty

class ExtraParameters private constructor(private val map: Map<String, Any?>) {
    operator fun <T> get(name: String) = map[name] as T
    operator fun <T> getValue(thisRef: Any?, prop: KProperty<*>) = get<T>(prop.name)

    companion object {
        @JvmStatic val EMPTY = ExtraParameters(emptyMap())
        @JvmStatic fun of(vararg elements: Pair<String, Any?>) = ExtraParameters(mutableMapOf(*elements))
    }

    operator fun plus(other: ExtraParameters) = ExtraParameters(map + other.map)
}
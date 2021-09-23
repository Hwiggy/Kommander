package me.hwiggy.kommander.arguments

import kotlin.reflect.KProperty

class ExtraParameters private constructor(private val map: Map<String, Any>) {
    operator fun <T> get(name: String) = map[name] as T
    operator fun <T> getValue(thisRef: Any?, prop: KProperty<*>) = get<T>(prop.name)

    companion object {
        @JvmStatic val EMPTY = ExtraParameters(emptyMap())
        @JvmStatic fun fromMap(map: Map<String, Any>) = ExtraParameters(map)
    }

    fun plus(other: ExtraParameters) = ExtraParameters(other.map + map)
}
package me.hwiggy.kommander.arguments

import me.hwiggy.kommander.InvalidSyntaxException

open class Adapter<T>(base: (Arguments) -> T?) : (Arguments) -> T? by base {
    companion object {
        @JvmStatic fun single() = Adapter(Arguments::next)
        @JvmStatic fun <T> single(transform: (String) -> T?) = single().map(transform)
        
        @JvmStatic fun byte() = single(String::toByte)
        @JvmStatic fun byte(
            min: Byte? = Byte.MIN_VALUE, max: Byte? = Byte.MAX_VALUE, error: String? = null
        ) = byte().bound(min, max, error)

        @JvmStatic fun short() = single(String::toShort)
        @JvmStatic fun short(
            min: Short? = Short.MIN_VALUE, max: Short? = Short.MAX_VALUE, error: String? = null
        ) = short().bound(min, max, error)

        @JvmStatic fun int() = single(String::toInt)
        @JvmStatic fun int(
            min: Int? = Int.MIN_VALUE, max: Int? = Int.MAX_VALUE, error: String? = null
        ) = int().bound(min, max, error)

        @JvmStatic fun long() = single(String::toLong)
        @JvmStatic fun long(
            min: Long? = Long.MIN_VALUE, max: Long? = Long.MAX_VALUE, error: String? = null
        ) = long().bound(min, max, error)

        @JvmStatic fun float() = single(String::toFloat)
        @JvmStatic fun float(
            min: Float? = Float.MIN_VALUE, max: Float? = Float.MAX_VALUE, error: String? = null
        ) = float().bound(min, max, error)

        @JvmStatic fun double() = single(String::toDouble)
        @JvmStatic fun double(
            min: Double? = Double.MIN_VALUE, max: Double? = Double.MAX_VALUE, error: String? = null
        ) = double().bound(min, max, error)
    }

    infix fun <V, T : V, U : V> Adapter<T>.or(other: Adapter<U>) = Adapter { this(it) ?: other(it) }
}

fun <N> Adapter<N>.bound(
    min: N? = null,
    max: N? = null,
    error: String? = null
): BoundAdapter<N> where N : Comparable<N>, N : Number = when {
    min == null && max == null -> throw IllegalArgumentException(
        "Parameters min and max may not both be null!"
    )
    min != null && max == null -> BoundAdapter(min, null) {
        val read = it.runCatching(this).getOrNull() ?: return@BoundAdapter null
        if (read < min) throw InvalidSyntaxException(
            error ?: "Expected value >= `$min`, actual: `$read`!"
        )
        else return@BoundAdapter read
    }
    min == null && max != null -> BoundAdapter(null, max) {
        val read = it.runCatching(this).getOrNull() ?: return@BoundAdapter null
        if (read > max) throw InvalidSyntaxException(
            error ?: "Expected value <= `$max`, actual: `$read`!"
        )
        else return@BoundAdapter read
    }
    else -> BoundAdapter(min, max) {
        val read = it.runCatching(this).getOrNull() ?: return@BoundAdapter null
        if (read < min!! || read > max!!) throw InvalidSyntaxException(
            error ?: "Expected `$min` <= value <= `$max`, actual: `$read`!"
        )
        return@BoundAdapter read
    }
}

fun <T, U> Adapter<T>.map(transform: (T) -> U?) : Adapter<U> = Adapter {
    it.runCatching(this).getOrNull()?.let(transform)
}

class BoundAdapter<T>(
    val min: T? = null,
    val max: T? = null,
    block: (Arguments) -> T?
) : Adapter<T>(block) where T : Comparable<T>, T : Number
package me.hwiggy.kommander.arguments

import me.hwiggy.kommander.InvalidSyntaxException

open class Adapter<T>(base: (Arguments, Map<String, Any>) -> T?) : (Arguments, Map<String, Any>) -> T? by base {
    companion object {
        @JvmStatic
        fun single() = Adapter { args, _ -> args.next() }
        @JvmStatic
        fun <T> single(transform: (String) -> T?) = single().map(transform)
        @JvmStatic
        fun slurp(separator: String = "") = Adapter { it, _ ->
            val taken = mutableListOf<String>()
            it.forEachRemaining { taken.add(it!!) }
            return@Adapter taken.joinToString(separator)
        }

        @JvmStatic
        fun boolean() = single(String::toBoolean)
        @JvmStatic
        fun byte() = single(String::toByte)
        @JvmStatic
        fun byte(
            min: Byte? = Byte.MIN_VALUE, max: Byte? = Byte.MAX_VALUE, error: String? = null
        ) = byte().bound(min, max, error) { it }

        @JvmStatic
        fun short() = single(String::toShort)
        @JvmStatic
        fun short(
            min: Short? = Short.MIN_VALUE, max: Short? = Short.MAX_VALUE, error: String? = null
        ) = short().bound(min, max, error) { it }

        @JvmStatic
        fun int() = single(String::toInt)
        @JvmStatic
        fun int(
            min: Int? = Int.MIN_VALUE, max: Int? = Int.MAX_VALUE, error: String? = null
        ) = int().bound(min, max, error) { it }

        @JvmStatic
        fun long() = single(String::toLong)
        @JvmStatic
        fun long(
            min: Long? = Long.MIN_VALUE, max: Long? = Long.MAX_VALUE, error: String? = null
        ) = long().bound(min, max, error) { it }

        @JvmStatic
        fun float() = single(String::toFloat)
        @JvmStatic
        fun float(
            min: Float? = Float.MIN_VALUE, max: Float? = Float.MAX_VALUE, error: String? = null
        ) = float().bound(min, max, error) { it }

        @JvmStatic
        fun double() = single(String::toDouble)
        @JvmStatic
        fun double(
            min: Double? = Double.MIN_VALUE, max: Double? = Double.MAX_VALUE, error: String? = null
        ) = double().bound(min, max, error) { it }
    }

    infix fun <V, T : V, U : V> Adapter<T>.or(other: Adapter<U>) = Adapter { it, extra ->
        this(it, extra) ?: other(it, extra)
    }

    fun <N> bound(
        min: T? = null,
        max: T? = null,
        error: String? = null,
        mapper: (T) -> N
    ): BoundAdapter<T> where N : Comparable<N>, N : Number {
        val minVal = min?.let(mapper)
        val maxVal = max?.let(mapper)
        return when {
            min == null && max == null -> throw IllegalArgumentException(
                "Parameters min and max may not both be null!"
            )
            min != null && max == null -> BoundAdapter(min, null) { it, extra ->
                val read = it.runCatching {
                    this.next(this@Adapter, extra)
                }.getOrNull() ?: return@BoundAdapter null
                if (mapper(read) < minVal!!) throw InvalidSyntaxException(
                    error ?: "Expected value >= `$min`, actual: `$read`!"
                )
                else return@BoundAdapter read
            }
            min == null && max != null -> BoundAdapter(null, max) { it, extra ->
                val read = it.runCatching {
                    this.next(this@Adapter, extra)
                }.getOrNull() ?: return@BoundAdapter null
                if (mapper(read) > maxVal!!) throw InvalidSyntaxException(
                    error ?: "Expected value <= `$max`, actual: `$read`!"
                )
                else return@BoundAdapter read
            }
            else -> BoundAdapter(min, max) { it, extra ->
                val read = it.runCatching {
                    this.next(this@Adapter, extra)
                }.getOrNull() ?: return@BoundAdapter null
                val check = mapper(read)
                if (check < minVal!! || check > maxVal!!) throw InvalidSyntaxException(
                    error ?: "Expected `$min` <= value <= `$max`, actual: `$read`!"
                )
                return@BoundAdapter read
            }
        }
    }

    fun <U> map(transform: (T) -> U?): Adapter<U> = Adapter { it, extra ->
        it.runCatching { this.next(this@Adapter, extra) }.getOrNull()?.let(transform)
    }
}

class BoundAdapter<T>(
    val min: T? = null,
    val max: T? = null,
    block: (Arguments, Map<String, Any>) -> T?
) : Adapter<T>(block)
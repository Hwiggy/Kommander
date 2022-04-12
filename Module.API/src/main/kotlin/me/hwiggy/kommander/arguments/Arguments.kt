package me.hwiggy.kommander.arguments

import me.hwiggy.kommander.InvalidSyntaxException
import java.util.regex.Pattern

/**
 * This class represents an iterable array of Strings to be used during parameter parsing.
 * @param[raw] The raw array of Strings to iterate and parse with.
 */
class Arguments(
    val raw: Array<String>,
    val extra: ExtraParameters = ExtraParameters.EMPTY
) : Iterator<String?> {
    private var cursor = 0

    override fun hasNext() = cursor + 1 <= raw.size

    /**
     * Used to optionally (nullable) return an element from the array
     * @return The next element in the array if one is present, else null
     */
    override fun next() = if (hasNext()) raw[cursor++] else null
    /**
     * Used to require an element (non-null) be returned from the array
     * @param[error] Message for the exception thrown if the next element is absent.
     * @throws[InvalidSyntaxException] If the next element is absent.
     * @return The next element in the array.
     */
    fun next(error: String): String = next() ?: throw InvalidSyntaxException(error)

    fun <T> next(adapter: Adapter<T>): T? {
        // Snapshot the index prior to using an adapter
        val pre = cursor
        return adapter(this, extra).also {
            // If this adapter did not return a value, reset the index to the snapshot
            if (it == null) cursor = pre
        }
    }

    fun <T> next(
        adapter: Adapter<T>, error: String
    ) = next(adapter) ?: throw InvalidSyntaxException(error)

    /**
     * Reduces [amount] from the current [cursor] to 'undo' an argument reading.
     */
    fun backtrack(amount: Int = 1) { cursor -= amount }

    companion object {
        // This pattern splits on quoted pairs, or spaces as a fallback.
        private val ARGS_PATTERN = Pattern.compile("\"([^\"]*)\"|([^ ]+)")

        @JvmStatic fun parse(input: String, extra: ExtraParameters = ExtraParameters.EMPTY): Arguments {
            return Arguments(split(input).toTypedArray(), extra)
        }

        @JvmStatic fun split(input: String): MutableList<String> {
            val raw = mutableListOf<String>()
            val matcher = ARGS_PATTERN.matcher(input)
            while (matcher.find()) raw += when {
                matcher.group(1) != null -> matcher.group(1)
                else -> matcher.group(2)
            }
            return raw
        }
    }

    operator fun plus(other: Arguments) = Arguments(raw + other.raw, extra + other.extra)
    fun slice() = Arguments(raw.copyOfRange(cursor, raw.size), extra)

    /**
     * Represents [Arguments] that have been processed through a Command's [Synopsis]
     */
    @Suppress("unused")
    inner class Processed(private val map: Map<String, Any?>) {
        val parent: Arguments = this@Arguments
        val size = map.values.size

        @Suppress("UNCHECKED_CAST")
        fun <Output> optional(name: String): Output? = map[name] as? Output?
        fun <Output> optional(name: String, default: Output) = optional(name) ?: default
        fun <Direct, Output> optional(name: String, converter: (Direct) -> Output?) = optional<Direct>(name)?.let(converter)
        fun <Direct, Output> optional(name: String, default: Output, converter: (Direct) -> Output?) = optional(name, converter) ?: default

        fun <Output> required(name: String, error: String): Output {
            return optional(name) ?: throw InvalidSyntaxException(error)
        }

        fun <Direct, Output> required(name: String, error: String, converter: (Direct) -> Output?): Output {
            return optional(name, converter) ?: throw InvalidSyntaxException(error)
        }
    }
}
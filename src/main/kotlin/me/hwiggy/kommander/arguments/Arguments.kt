package me.hwiggy.kommander.arguments

import me.hwiggy.kommander.InvalidSyntaxException
import java.util.regex.Pattern

/**
 * This class represents an iterable array of Strings to be used during parameter parsing.
 * @param[raw] The raw array of Strings to iterate and parse with.
 */
class Arguments(private val raw: Array<String>) : Iterator<String?> {
    private var cursor = 0

    override fun hasNext() = cursor < raw.size

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

    fun <T> next(adapter: (Arguments) -> T?): T? {
        // Snapshot the index prior to using an adapter
        val pre = cursor
        return adapter(this).also {
            // If this adapter did not return a value, reset the index to the snapshot
            if (it == null) cursor = pre
        }
    }

    fun <T> next(
        adapter: (Arguments) -> T?, error: String
    ) = next(adapter) ?: throw InvalidSyntaxException(error)

    /**
     * Reduces [amount] from the current [cursor] to 'undo' an argument reading.
     */
    fun backtrack(amount: Int = 1) { cursor -= amount }

    companion object {
        // This pattern splits on quoted pairs, or spaces as a fallback.
        private val ARGS_PATTERN = Pattern.compile("\"([^\"]*)\"|([^ ]+)")

        @JvmStatic fun parse(input: String): Arguments {
            val raw = mutableListOf<String>()
            val matcher = ARGS_PATTERN.matcher(input)
            while (matcher.find()) raw += when {
                matcher.group(1) != null -> matcher.group(1)
                else -> matcher.group(2)
            }
            return Arguments(raw.toTypedArray())
        }
    }

    operator fun plus(other: Arguments) = Arguments(raw + other.raw)

    fun slice() = Arguments(raw.copyOfRange(cursor, raw.size - 1))

    /**
     * Represents [Arguments] that have been processed through a Command's [Synopsis]
     */
    @Suppress("unused")
    inner class Processed(private val map: Map<String, Any?>) {
        val parent: Arguments = this@Arguments
        val size = map.values.size

        @Suppress("UNCHECKED_CAST")
        private fun <Output> optional(name: String): Output? = map[name] as? Output?
        private fun <Output> optional(name: String, default: Output) = optional(name) ?: default
        private fun <Direct, Output> optional(name: String, converter: (Direct) -> Output?) = optional<Direct>(name)?.let(converter)
        private fun <Direct, Output> optional(name: String, default: Output, converter: (Direct) -> Output?) = optional(name, converter) ?: default

        fun <Output> required(name: String, error: String): Output {
            return optional(name) ?: throw InvalidSyntaxException(error)
        }

        fun <Direct, Output> required(name: String, error: String, converter: (Direct) -> Output?): Output {
            return optional(name, converter) ?: throw InvalidSyntaxException(error)
        }
    }
}
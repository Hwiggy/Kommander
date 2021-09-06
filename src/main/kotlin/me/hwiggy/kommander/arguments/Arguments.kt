package me.hwiggy.kommander.arguments

import me.hwiggy.kommander.InvalidSyntaxException
import java.util.regex.Pattern
import kotlin.reflect.KProperty

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
     * Used to require an element (non null) be returned from the array
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
}

/**
 * Represents [Arguments] that have been processed through a Command's [Synopsis]
 */
class ProcessedArguments(private val map: Map<String, Any?>) {
    val size = map.values.size

    fun <T> optional(name: String, default: T) = optional(name) ?: default
    fun <T> optional(name: String) = map[name] as T?

    fun <T> required(name: String, error: String) =
        optional<T>(name) ?: throw InvalidSyntaxException(error)
}
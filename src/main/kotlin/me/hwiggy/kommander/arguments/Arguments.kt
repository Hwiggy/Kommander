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
@Suppress("unused")
class ProcessedArguments(private val map: Map<String, Any?>) {
    val size = map.values.size

    @Suppress("UNCHECKED_CAST")
    private fun <Output> get(name: String): Output? = map[name] as? Output?
    private fun <Output> get(name: String, default: Output) = get(name) ?: default
    private fun <Output> get(name: String, converter: (String) -> Output?) = get<String>(name)?.let(converter)
    private fun <Output> get(name: String, default: Output, converter: (String) -> Output?) = get(name, converter) ?: default

    private fun <Output> getReq(name: String, error: String): Output {
        return get(name) ?: throw InvalidSyntaxException(error)
    }

    private fun <Output> getReq(name: String, error: String, converter: (String) -> Output?): Output {
        return get(name, converter) ?: throw InvalidSyntaxException(error)
    }

    fun <Output> optional() = Delegate<Output?>(getter = this::get)
    fun <Output> optional(name: String) = Delegate<Output?>(name, this::get)

    fun <Output> optional(default: Output) = Delegate { get(it, default) }
    fun <Output> optional(name: String, default: Output) = Delegate(name) { get(it, default) }

    fun <Output> optional(converter: (String) -> Output?) = Delegate { get(it, converter)}
    fun <Output> optional(name: String, converter: (String) -> Output?) = Delegate(name) { get(it, converter) }

    fun <Output> optional(default: Output, converter: (String) -> Output?) = Delegate { get(it, default, converter) }
    fun <Output> optional(name: String, default: Output, converter: (String) -> Output?) = Delegate(name) { get(it, default, converter) }

    fun <Output> required(error: String) = Delegate<Output> { getReq(it, error) }
    fun <Output> required(name: String, error: String) = Delegate<Output>(name) { getReq(it, error) }

    fun <Output> required(error: String, converter: (String) -> Output?) = Delegate { getReq(it, error, converter) }
    fun <Output> required(name: String, error: String, converter: (String) -> Output?) = Delegate(name) { getReq(name, error, converter) }

    inner class Delegate<Output>(
        private val name: String? = null,
        private val getter: (String) -> Output
    ) {
        operator fun getValue(thisRef: Nothing?, prop: KProperty<*>) = getter(name ?: prop.name)
    }
}
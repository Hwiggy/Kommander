package me.hwiggy.kommander.arguments

import me.hwiggy.kommander.InvalidParameterException

/**
 * Represents a synopsis of named parameters and parameter groups
 * Used to build help and syntax information for commands.
 *
 */
class Synopsis(init: Configurator.() -> Unit) {
    private val parameters = ArrayList<Parameter<*>>()

    /**
     * Adds a [Parameter] to the parameters list
     * Responsible for adding parameter-specific elements to the element list
     */
    private fun addParameter(parameter: Parameter<*>) = parameter.run(parameters::add)

    /**
     * Adds a [Group] to the parameters list
     * Responsible for adding group-specific elements to the element list
     */
    private fun addGroup(group: Group<*>) = group.also(parameters::add)

    /**
     * Processes arguments using the defined parameters list
     */
    fun process(input: Arguments) = parameters.associateTo(HashMap()) {
        val out = input.next(it.adapter) ?: it.default
        if (it is Group<*> && !it.isValid(out)) throw InvalidParameterException(out, it.name)
        it.name to out
    }.let(input::Processed)

    /**
     * Joins the parameter list to a string, for syntax
     */
    fun concatParameters() = if (parameters.isEmpty()) null else parameters.joinToString(" ") {
        val identifier = if (it is Group<*>) it.concatOptions() else it.name
        return@joinToString if (it.isOptional()) "($identifier)" else "<$identifier>"
    }

    /**
     * Builds the parameter detail.
     * @param[transformer] Function for transforming a parameter.
     * @param[collector] Function for joining the parameter transformations to a single String.
     */
    fun buildParameterDetail(
        transformer: (Parameter<*>) -> String,
        collector: (List<String>) -> String = { it.joinToString() }
    ) = parameters.map(transformer).let(collector).ifEmpty { null }

    @Suppress("unused")
    inner class Configurator {
        /**
         * Configures an required parameter
         * @param[name] The name of this parameter
         * @param[description] A description of this parameter
         * @param[adapter] The Adapter used to parse this parameter
         * @param[default] The default value of this parameter
         */
        fun <T> reqParam(
            name: String,
            description: String,
            adapter: Adapter<T>,
            default: T? = null
        ) = object : Parameter<T> {
            override val name = name
            override val description = description
            override val default = default
            override val adapter = adapter
        }.run(::addParameter)

        /**
         * Configures an optional parameter
         * @param[name] The name of this parameter
         * @param[description] A description of this parameter
         * @param[adapter] The Adapter used to parse this parameter
         * @param[default] The default value of this parameter
         */
        fun <T> optParam(
            name: String,
            description: String,
            adapter: Adapter<T>,
            default: T? = null
        ) = object : Parameter<T> {
            override val name = name
            override val description = description
            override val default = default
            override val adapter = adapter
            override fun isOptional() = true
        }.run(::addParameter)

        /**
         * Configures a required group
         * @param[name] The name of this group
         * @param[description] A description of this group
         * @param[transform] The Adapter used to parse options
         * @param[default] The default option of this group
         */
        fun <T> reqGroup(
            name: String,
            description: String,
            transform: (String) -> T?,
            default: T? = null,
            init: Group.Configurator<T>.() -> Unit
        ) where T : Group.Option, T : Enum<T> = object : Group<T>(init) {
            override val name = name
            override val description = description
            override val adapter = Adapter.single(transform)
            override val default = default
        }.run(::addGroup)

        /**
         * Configures a required group
         * @param[name] The name of this group
         * @param[description] A description of this group
         * @param[transform] The Adapter used to parse options
         * @param[default] The default option of this group
         */
        fun <T> optGroup(
            name: String,
            description: String,
            transform: (String) -> T?,
            default: T? = null,
            init: Group.Configurator<T>.() -> Unit
        ) where T : Group.Option, T : Enum<T> = object : Group<T>(init) {
            override val name = name
            override val description = description
            override val adapter = Adapter.single(transform)
            override val default = default
            override fun isOptional() = true
        }.run(::addGroup)
    }
    init { Configurator().also(init) }
}

/**
 * Marker interface for a Synopsis parameter
 */
interface Parameter<T> {
    /**
     * The name of this parameter
     */
    val name: String

    /**
     * The description of this parameter
     */
    val description: String

    /**
     * The adapter used to parse this parameter
     */
    val adapter: Adapter<out T>

    /**
     * The default value of this parameter, if any
     */
    val default: T?

    /**
     * Whether this Parameter is Optional; default false
     */
    fun isOptional() = false
}

/**
 * Represents a group of enumerated options as a Synopsis parameter
 */
abstract class Group<T>(init: Configurator<T>.() -> Unit) : Parameter<T> where T : Group.Option, T : Enum<T> {
    val choices: List<Pair<T, String>>

    @Suppress("unused")
    class Configurator<T> {
        internal val choices = ArrayList<Pair<T, String>>()
        fun choice(option: T, description: String) {
            choices += option to description
        }
    }

    init {
        Configurator<T>().also(init).also {
            this.choices = it.choices.toList()
        }
    }

    /**
     * Concatenates the group options into a pipe delimited list
     */
    fun concatOptions() = choices.map { it.first }.joinToString("|", transform = Option::synopsisName)

    /**
     * Tests if an object is a valid option for this group
     */
    fun isValid(option: Any?) = choices.any { it.first == option }

    /**
     * Marker interface for group options
     */
    interface Option {
        val synopsisName: String
        companion object {
            @JvmStatic inline fun <reified T : Enum<T>> byName(): (String) -> T? {
                val method = T::class.java.getMethod("values").also {
                    it.isAccessible = true
                }
                val values : Array<T> = method.invoke(null) as Array<T>
                return { arg -> values.find { it.name.equals(arg, true) }}
            }
        }
    }
}


package me.hwiggy.kommander.arguments

import me.hwiggy.kommander.InvalidParameterException

/**
 * Represents a synopsis of named parameters and parameter groups
 * Used to build help and syntax information for commands.
 *
 */
class Synopsis(init: Configurator.() -> Unit) {
    private val elements = ArrayList<Pair<ElementType, String?>>()
    private val parameters = ArrayList<Parameter<*>>()

    init {
        Configurator().also(init)
    }

    /**
     * Adds a Synopsis Element to the element list
     */
    private fun addElement(type: ElementType, element: Any?) {
        elements += type to element?.toString()
    }

    /**
     * Adds a [Parameter] to the parameters list
     * Responsible for adding parameter-specific elements to the element list
     */
    private fun addParameter(parameter: Parameter<*>) = parameter.apply {
        addElement(ElementType.PARAMETER_NAME, name)
        addElement(ElementType.PARAMETER_DESCRIPTION, description)
        if (this.default != null) addElement(ElementType.PARAMETER_DEFAULT, default)
        (adapter as? BoundAdapter<*>)?.apply {
            if (min != null) addElement(ElementType.VALUE_MIN, min)
            if (max != null) addElement(ElementType.VALUE_MAX, max)
        }
    }.also(parameters::add)

    /**
     * Adds a [Group] to the parameters list
     * Responsible for adding group-specific elements to the element list
     */
    private fun addGroup(group: Group<*>) = group.apply {
        addElement(ElementType.GROUP_NAME, name)
        addElement(ElementType.GROUP_DESCRIPTION, description)
        choices.forEach { (option, description) ->
            addElement(ElementType.GROUP_OPTION, option.synopsisName)
            addElement(ElementType.GROUP_OPTION_DESCRIPTION, description)
        }
        if (this.default != null) elements += ElementType.GROUP_DEFAULT to default!!.synopsisName
    }.also(parameters::add)

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
            init: Group<T>.Configurator.() -> Unit
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
            init: Group<T>.Configurator.() -> Unit
        ) where T : Group.Option, T : Enum<T> = object : Group<T>(init) {
            override val name = name
            override val description = description
            override val adapter = Adapter.single(transform)
            override val default = default
            override fun isOptional() = true
        }.run(::addGroup)
    }

    /**
     * Processes arguments using the defined parameters list
     */
    fun process(input: Arguments) = parameters.associateTo(HashMap()) {
        val out = input.next(it.adapter) ?: it.default
        if (it is Group<*> && !it.isValid(out)) throw InvalidParameterException(out, it.name)
        it.name to out
    }.let(::ProcessedArguments)

    /**
     * Joins the parameter list to a string, for syntax
     */
    fun concatParameters() = if (parameters.isEmpty()) null else parameters.joinToString(" ") {
        val identifier = if (it is Group<*>) it.concatOptions() else it.name
        return@joinToString if (it.isOptional()) "($identifier)" else "<$identifier>"
    }

    /**
     * Marker enum for Synopsis Elements
     */
    enum class ElementType {
        /**
         * The name of a parameter
         */
        PARAMETER_NAME,

        /**
         * The description for a parameter
         */
        PARAMETER_DESCRIPTION,

        /**
         * The default option for a parameter
         */
        PARAMETER_DEFAULT,

        /**
         * The name of a group
         */
        GROUP_NAME,

        /**
         * The description for a group
         */
        GROUP_DESCRIPTION,

        /**
         * An option for a group
         */
        GROUP_OPTION,

        /**
         * The description for an option for a group
         */
        GROUP_OPTION_DESCRIPTION,

        /**
         * The default option for a group
         */
        GROUP_DEFAULT,

        /**
         * The minimum value for a parameter
         */
        VALUE_MIN,

        /**
         * The maximum value for a parameter
         */
        VALUE_MAX
    }
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
abstract class Group<T>(init: Group<T>.Configurator.() -> Unit) : Parameter<T> where T : Group.Option, T : Enum<T> {
    internal val choices = ArrayList<Pair<T, String>>()

    inner class Configurator {
        fun choice(option: T, description: String) {
            choices += option to description
        }
    }

    init {
        Configurator().init()
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
    interface Option { val synopsisName: String }
}


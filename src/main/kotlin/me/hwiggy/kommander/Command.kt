package me.hwiggy.kommander

import me.hwiggy.kommander.arguments.Arguments
import me.hwiggy.kommander.arguments.Synopsis
import java.lang.Exception

abstract class Command<out Sender, Output, out Self : Command<Sender, Output, Self>> : CommandExecutor<@UnsafeVariance Sender, Output> {
    /**
     * The parent of this command, if it is registered as a child.
     */
    var parent: @UnsafeVariance Self? = null
    val children = Children()

    /**
     * The name of this command, also the primary identifier
     */
    abstract val name: String

    /**
     * The description for this command.
     */
    abstract val description: String

    /**
     * The usage for this command
     * Composed of primary identifier, plus either a parameter listing or subcommand listing, if available
     */
    val usage: String
        get() = "$name ${getParameterList() ?: ""}".trim()

    /**
     * The [Synopsis] for this command
     */
    open val synopsis = Synopsis { /* Default implementation has no parameters */ }

    /**
     * Secondary identifiers for this command
     */
    open val aliases = emptyList<String>()

    /**
     * Registers another command as a child to this command
     */
    fun addChild(command: @UnsafeVariance Self)  {
        command.also { it.parent = this as Self }.also(children::register)
    }

    /**
     * Attempts to cascade into command children using the provided arguments
     * @param[args] The arguments to cascade with
     * @param[next] A BiFunction for the next identifier and child
     *
     */
    fun cascade(
        args: Array<out String>,
        next: (String, Self) -> Output,
        last: () -> Output,
        onError: (Exception) -> Output
    ): Output? {
        return try {
            if (args.isNotEmpty()) {
                val identifier = args.first().lowercase()
                val found = children.find(identifier)
                if (found != null) return next(identifier, found)
            }
            last()
        } catch (ex: Exception) {
            onError(ex)
        }
    }

    /**
     * Derives the parameter list from [Synopsis] or available children.
     * @return
     *      The first non-null element available from the following:
     *      - A concatenated string of parameters
     *      - A concatenated string of child identifiers
     *      OR `null`
     */
    fun getParameterList(): String? =
        synopsis.concatParameters() ?: children.concatIdentifiers()?.let { "<$it>" }

    /**
     * Represents a registered map of child commands
     */
    inner class Children {
        /**
         * Map for registering children by name
         */
        private val byLabel = HashMap<String, Self>()

        /**
         * Map for registering children by alias
         */
        private val byAlias = HashMap<String, Self>()

        /**
         * Registers a command to each of the maps
         */
        fun register(command: @UnsafeVariance Self) {
            byLabel[command.name.lowercase()] = command
            command.aliases.forEach {
                byAlias[it.lowercase()] = command
            }
        }

        /**
         * Attempts to locate a command using a primary or secondary identifier
         */
        fun find(identifier: String) = byLabel[identifier] ?: byAlias[identifier]

        /**
         * Returns the primary labels for every registered child
         */
        fun getIdentifiers() = byLabel.keys.toSet()
        /**
         * Joins the primary labels for each registered child, separated by `|`
         */
        fun concatIdentifiers() = getIdentifiers().joinToString("|").ifEmpty { null }

        fun values() = byLabel.values.toList()
    }
}

/**
 * Represents the executable part of a [Command]
 */
fun interface CommandExecutor<in S, out Output> {
    /**
     * Execution callback for commands
     * Recommended default behavior is to send an invalid sub-command message.
     */
    fun execute(sender: S, arguments: Arguments.Processed): Output
}
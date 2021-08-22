package me.hwiggy.kommander

import me.hwiggy.kommander.arguments.Arguments
import me.hwiggy.kommander.arguments.ProcessedArguments
import me.hwiggy.kommander.arguments.Synopsis
import java.lang.Exception

abstract class Command<S, C : Command<S, C>> : CommandExecutor<S> {
    private val children = Children()

    /**
     * The name of this command. Also the primary identifier
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
    fun addChild(command: C) = children.register(command)

    /**
     * Attempts to cascade into command children using the provided arguments
     * @param[args] The arguments to cascade with
     * @param[next] A BiFunction for the next identifier and child
     *
     */
    fun <O> cascade(
        args: Array<out String>,
        next: (String, C) -> O,
        last: () -> O,
        onError: (Exception) -> O
    ): O? {
        return try {
            if (args.isNotEmpty()) {
                val identifier = args.first().lowercase()
                val found = children.find(identifier)
                if (found != null) return next(identifier, found)
            }
            last()
        } catch (ex: Exception) { onError(ex) }
    }

    /**
     * Overload parameter for [cascade] with a [Unit] return type
     */
    fun cascade(
        args: Array<out String>,
        next: (String, C) -> Unit,
        last: () -> Unit,
        onError: (Exception) -> Unit
    ) = cascade<Unit>(args, next, last, onError)

    /**
     * Concatenates the received arguments, then processes them against the command synopsis.
     */
    fun processArguments(args: Array<out String>) =
        args.joinToString(" ").let(Arguments::parse).let(synopsis::process)

    /**
     * Derives the parameter list from [Synopsis] or available children.
     * @return
     *      The first non-null element available from the following:
     *      - A concatenated string of parameters
     *      - A concatenated string of child identifiers
     *      OR `null`
     */
    fun getParameterList(): String? =
        synopsis.concatParameters() ?:
        children.concatIdentifiers()?.let { "<$it>" }

    /**
     * Represents a registered map of child commands
     */
    inner class Children {
        /**
         * Map for registering children by name
         */
        private val byLabel = HashMap<String, C>()

        /**
         * Map for registering children by alias
         */
        private val byAlias = HashMap<String, C>()

        /**
         * Registers a command to each of the maps
         */
        fun register(command: C) {
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
        fun getIdentifiers() = byLabel.keys.ifEmpty { null }

        /**
         * Joins the primary labels for each registered child, separated by `|`
         */
        fun concatIdentifiers() = getIdentifiers()?.joinToString("|")
    }
}

/**
 * Represents the executable part of a [Command]
 */
fun interface CommandExecutor<S> {
    /**
     * Execution callback for commands
     * Recommended default behavior is to send an invalid sub-command message.
     */
    fun execute(sender: S, arguments: ProcessedArguments)
}
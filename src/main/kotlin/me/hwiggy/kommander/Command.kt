package me.hwiggy.kommander

import me.hwiggy.kommander.arguments.Arguments
import me.hwiggy.kommander.arguments.ProcessedArguments
import me.hwiggy.kommander.arguments.Synopsis

abstract class Command<S, C : Command<S, C>> {
    private val children = Children()

    abstract val name: String
    abstract val synopsis: Synopsis

    open val aliases = emptyList<String>()

    fun addChild(command: C) = children.register(command)

    /**
     * Attempts to cascade into command children using the provided arguments
     * @param[args] The arguments to cascade with
     * @param[next] A BiFunction for the next identifier and child
     *
     */
    fun <O> cascade(args: Array<out String>, next: (String, C) -> O): O? {
        if (args.isNotEmpty()) {
            val identifier = args.first().lowercase()
            val found = children.find(identifier)
            if (found != null) return next(identifier, found)
        }
        return null
    }

    /**
     * Overload parameter for [cascade] with a [Unit] return type
     */
    fun cascade(args: Array<out String>, next: (String, C) -> Unit) = cascade<Unit>(args, next)

    /**
     * Execution callback for this command
     * Recommended default behavior is to send an invalid sub-command message.
     */
    abstract fun execute(sender: S, arguments: ProcessedArguments)

    /**
     * Concatenates the received arguments, then processes them against the command synopsis.
     */
    fun processArguments(args: Array<out String>) =
        args.joinToString(" ").let(Arguments::parse).let(synopsis::process)

    fun getParameterList(): String? =
        synopsis.concatParameters() ?:
        children.concatIdentifiers()?.let { "<$it>" }

    inner class Children {
        private val byLabel = HashMap<String, C>()
        private val byAlias = HashMap<String, C>()

        fun register(command: C) = register(command.name, command, *command.aliases.toTypedArray())
        fun register(name: String, command: C, vararg aliases: String) {
            byLabel[name.lowercase()] = command
            aliases.forEach {
                byAlias[it.lowercase()] = command
            }
        }

        fun find(identifier: String) = byLabel[identifier] ?: byAlias[identifier]

        fun getIdentifiers() = byLabel.keys.ifEmpty { null }
        fun concatIdentifiers() = getIdentifiers()?.joinToString("|")
    }
}

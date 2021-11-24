package me.hwiggy.kommander

import me.hwiggy.kommander.arguments.Arguments
import me.hwiggy.kommander.arguments.ExtraParameters
import me.hwiggy.kommander.arguments.Synopsis

abstract class Command<
    out Sender, Output, Self : Command<Sender, Output, Self>
> : ICommandParent<Sender, Output, Self> by CommandParent() {
    /**
     * The parent of this command, if it is registered as a child.
     */
    var parent: @UnsafeVariance Self? = null

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
    fun getUsage(): String = getUsage(this.name)
    fun getUsage(label: String): String {
        var parent = this.parent
        var parentStr = label
        while (parent != null) {
            parentStr = "${parent.name} $parentStr"
            parent = parent.parent
        }
        return "$parentStr ${getParameterList() ?: ""}".trim()
    }


    /**
     * The [Synopsis] for this command
     */
    open val synopsis = Synopsis { /* Default implementation has no parameters */ }

    /**
     * Secondary identifiers for this command
     */
    open val aliases = emptyList<String>()

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
     * Execution callback for commands
     * Recommended default behavior is to send an invalid sub-command message.
     */
    abstract fun execute(sender: @UnsafeVariance Sender, label: String, arguments: Arguments.Processed): Output

    /**
     * Hook to read extra parameters for Argument processing from a given [Sender]
     * @return a [Map] containing extra parameters to be referenced in argument adapters.
     */
    fun getExtraParameters(sender: @UnsafeVariance Sender) = ExtraParameters.EMPTY
}
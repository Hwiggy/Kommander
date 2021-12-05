package me.hwiggy.kommander

import me.hwiggy.kommander.arguments.Arguments
import me.hwiggy.kommander.arguments.ExtraParameters
import me.hwiggy.kommander.arguments.Synopsis

abstract class Command<
    BaseSender : Any,
    out Sender : BaseSender,
    Output : Any?, Super : Command<BaseSender, Sender, Output, Super>
> : CommandParent<BaseSender, Sender, Output, Super>() {
    /**
     * The parent of this command, if it is registered as a child.
     * Null, if this Command is a top level command.
     */
    protected var parent: Command<BaseSender, @UnsafeVariance Sender, Output, Super>? = null

    /**
     * [name] - The primary identifier of the command
     * [aliases] - The secondary identifiers of this command. Empty by default.
     * [description] - A short text describing what the command does
     * [synopsis] - The [Synopsis] for this command
     */
    abstract val name: String
    open val aliases = emptyList<String>()
    abstract val description: String
    open val synopsis = Synopsis()

    /**
     * The usage for this command
     * Composed of primary identifier, plus either a parameter listing or subcommand listing, if available
     */
    fun getUsage(): String {
        var parent = this.parent
        var parentStr = this.name
        while (parent != null) {
            parentStr = "${parent.name} $parentStr"
            parent = parent.parent
        }
        return "$parentStr ${getParameterList() ?: ""}".trim()
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
        synopsis.concatParameters() ?: children().ifEmpty { null }?.joinToString(
            prefix = "<",
            postfix = ">",
            separator = "|",
            transform = Command<BaseSender, Sender, Output, Super>::name
        )

    /**
     * Processes provided arguments & extra parameters, evaluates the synopsis, and executes the command
     */
    fun process(sender: @UnsafeVariance Sender, args: Array<String>, extra: ExtraParameters): Output {
        val arguments = Arguments(args, extra)
        val processed = synopsis.process(arguments)
        return execute(sender, processed)
    }

    /**
     * Execution callback for commands
     * Recommended default behavior is to send an invalid sub-command message.
     */
    abstract fun execute(sender: @UnsafeVariance Sender, arguments: Arguments.Processed): Output

    /**
     * Tests if this command can be run as the given sender
     * All of this command's parents will have their conditions applied first.
     * If any parent's conditions are unsuccessful, the child will not be reached.
     */
    open fun applyConditions(sender: BaseSender) = true
}
package me.hwiggy.kommander

import me.hwiggy.kommander.arguments.ExtraParameters

abstract class CommandHandler<
    BaseSender : Any,
    out Sender : BaseSender,
    R2, R1 : Any?,
    Super : Command<BaseSender, Sender, R1, Super>
    > : CommandParent<BaseSender, Sender, R1, Super>() {

    /**
     * The default [R2] value returned when a command execution has failed
     */
    abstract fun defaultResult(): R2

    /**
     * Converts a command result [R1] to a handler result [R2]
     * Default return value is the default result of this handler
     */
    open fun convertResult(cmdOut: R1) = defaultResult()

    /**
     * Handles an exception thrown when processing a command
     * Receives each of the following:
     *   [sender] - The sender who initiated the exception
     *   [command] - The command that called the exception
     *   [error] - The exception that was caught
     */
    abstract fun handleThrown(sender: @UnsafeVariance Sender, command: Super, error: Exception): R2

    /**
     * Using provided arguments, acquires a relevant command and extra parameters
     * The command and extra parameters are used to determine an output value
     * If any command's conditions fail, the return value is null
     *
     * This method can be used for abstracting additional functionality for child commands
     */
    fun <Out> acquireContext(
        sender: @UnsafeVariance Sender,
        label: String,
        args: MutableList<String>,
        consumer: (Super, ExtraParameters) -> Out,
        handler: (Sender, Super, Exception) -> Out
    ): Out? {
        // Find the root command
        val first = findSingle(label)?.let {
            // Attempt root conditions for the sender
            if (!it.applyConditions(sender)) null else it
        } ?: return null
        // If the root command was valid, we can continue
        var extras = getExtra(sender) + first.getExtra(sender)
        // Start trying to find subcommands, falling back on the root command
        val command = first.find(first, args, { it.applyConditions(sender) }) {
            extras += it.getExtra(sender)
        }
        return try {
            consumer(command, extras)
        } catch (ex: Exception) {
            handler(sender, command, ex)
        }
    }

    /**
     * Processes a command using [Command.process]
     * Returns the default result if a failure was encountered
     * Otherwise, returns the return result of the command, converted by [convertResult]
     */
    fun process(
        sender: @UnsafeVariance Sender,
        label: String,
        args: MutableList<String>
    ) = acquireContext(
        sender, label, args,
        { command, extras ->
            command.process(sender, args.toTypedArray(), extras).let(this::convertResult)
        },
        this::handleThrown
    ) ?: defaultResult()
}
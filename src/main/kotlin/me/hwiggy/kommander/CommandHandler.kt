package me.hwiggy.kommander

import me.hwiggy.kommander.arguments.ExtraParameters

abstract class CommandHandler<out Sender : Any, R2, R1 : Any?, Super : Command<Sender, R1, Super>> : CommandParent<Sender, R1, Super>() {

    /**
     * The default [R2] value returned when a command execution has failed
     */
    abstract fun defaultResult(): R2

    /**
     * Converts a command result [R1] to a handler result [R2]
     */
    abstract fun convertResult(cmdOut: R1): R2

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
        val first = findSingle(label)?.let {
            if (!it.applyConditions(sender)) null else it
        } ?: return null
        var extras = getExtra(sender) + first.getExtra(sender)
        val command = first.find(args, { it.applyConditions(sender) }) {
            extras += it.getExtra(sender)
        } ?: return null
        return try { consumer(command, extras) } catch (ex: Exception) { handler(sender, command, ex) }
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
    ) = acquireContext(sender, label, args, { command, extras ->
        command.process(sender, args.toTypedArray(), extras).let(this::convertResult)
    }, this::handleThrown) ?: defaultResult()
}
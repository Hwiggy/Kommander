package me.hwiggy.kommander

import me.hwiggy.kommander.arguments.ExtraParameters

abstract class CommandHandler<out Sender : Any, R2, R1 : Any?, Super : Command<Sender, R1, Super>> : CommandParent<Sender, R1, Super>() {

    abstract fun defaultFailResult(): R2
    abstract fun convertResult(cmdOut: R1): R2
    abstract fun handleThrown(error: Exception): R2

    fun <Out> acquireContext(
        sender: @UnsafeVariance Sender,
        label: String,
        args: MutableList<String>,
        consumer: (Super, ExtraParameters) -> Out,
        handler: (Exception) -> Out
    ): Out? {
        val first: Super = findSingle(label)?.let {
            if (!it.applyConditions(sender)) null else it
        } ?: return null
        var extras = getExtra(sender) + first.getExtra(sender)
        val command = first.find(args, { it.applyConditions(sender) }) {
            extras += it.getExtra(sender)
        } ?: first
        return try { consumer(command, extras) } catch (ex: Exception) { handler(ex) }
    }

    fun process(
        sender: @UnsafeVariance Sender,
        label: String,
        args: MutableList<String>
    ) = acquireContext(sender, label, args, { command, extras ->
        command.process(sender, args.toTypedArray(), extras).let(this::convertResult)
    }, this::handleThrown) ?: defaultFailResult()
}
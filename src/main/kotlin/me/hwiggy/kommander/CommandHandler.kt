package me.hwiggy.kommander

import me.hwiggy.kommander.arguments.Arguments

abstract class CommandHandler<
    out Sender, Output, Cmd : Command<Sender, Output, Cmd>
> : ICommandParent<Sender, Output, Cmd> by CommandParent() {

    abstract fun defaultResult(): Output
    open fun handleThrown(error: Exception) = defaultResult()
    open fun handleUnknown(sender: @UnsafeVariance Sender) = defaultResult()

    fun cascade(sender: @UnsafeVariance Sender, args: Array<String>): Output? {
        val argumentList = args.toMutableList()
        val found = find(argumentList) ?: return handleUnknown(sender)
        val extraParameters = found.getExtraParameters(sender)
        val identifier = argumentList.removeFirst()
        val arguments = Arguments(args, extraParameters)
        val processed = found.synopsis.process(arguments)
        return try {
            found.execute(sender, identifier, processed)
        } catch (ex: Exception) {
            handleThrown(ex)
        }
    }
}
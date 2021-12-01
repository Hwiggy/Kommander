package me.hwiggy.kommander

import me.hwiggy.kommander.arguments.ExtraParameters

abstract class CommandParent<out Sender : Any, Output : Any?, Super : Command<Sender, Output, Super>> {
    private val commands = HashMap<String, Super>()

    /**
     * Cascades into every child to find the command referenced by the provided arguments
     * @param[args] The arguments that may contain command identifiers
     * @param[predicate] A conditional check for continuing into the next layer of children
     * @return The found command, that has been validated by all [predicate]s
     */
    @JvmOverloads fun find(
        args: MutableList<String>,
        predicate: (Super) -> Boolean = { true },
        whenAccepted: (Super) -> Unit = { }
    ): Super? = if (args.isNotEmpty()) {
        // Peek the identifier
        val identifier = args.first().lowercase()
        // Find a matching command
        var command = findSingle(identifier)
        if (command != null) {
            // Consume the identifier
            args.removeFirst()
            // Test preconditions
            if (predicate(command)) {
                // Cascade if conditions passed
                val next = command.apply(whenAccepted).find(args, predicate)
                // Update cursor if a child was found
                if (next != null) command = next
            }
        }
        // Return the last registered command
        command
    } else null

    open fun register(child: Super) {
        commands[child.name] = child
        child.aliases.forEach { commands[it] = child }
    }

    fun findSingle(identifier: String) = commands[identifier]
    fun children(): Set<Super> = commands.values.toSet()

    open fun getExtra(sender: @UnsafeVariance Sender) = ExtraParameters.EMPTY
}
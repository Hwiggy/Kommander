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
    ): Super? {
        if (args.isEmpty()) return null
        // Peek the identifier
        val identifier = args.first().lowercase()
        // Find a matching command
        var command = findSingle(identifier)
        if (command != null) {
            // Consume the identifier
            args.removeFirst()
            // Test preconditions, null return on failed predicate
            if (!predicate(command)) return null
            // Cascade if conditions passed
            val next = command.apply(whenAccepted).find(args, predicate)
            // Update cursor if a child was found
            if (next != null) command = next
        }
        // Return the last registered command
        return command
    }

    /**
     * Registers a Command to the internal collection
     * Custom registration tasks may be handled through [postRegister]
     */
    fun register(child: Super) {
        commands[child.name] = child
        child.aliases.forEach { commands[it] = child }
        postRegister(child)
    }

    /**
     * Called so implementation can perform additional functionality post registration
     */
    open fun postRegister(child: Super) = Unit

    /**
     * Locates a single command from the internal collection
     */
    fun findSingle(identifier: String) = commands[identifier]

    /**
     * Obtains all commands (not keys) from the internal collection
     */
    fun children(): Set<Super> = commands.values.toSet()

    /**
     * Returns the [ExtraParameters] for a given [Sender]
     */
    open fun getExtra(sender: @UnsafeVariance Sender) = ExtraParameters.EMPTY
}
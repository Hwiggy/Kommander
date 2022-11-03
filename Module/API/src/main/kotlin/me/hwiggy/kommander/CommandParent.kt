package me.hwiggy.kommander

import me.hwiggy.kommander.arguments.ExtraParameters
import java.util.concurrent.atomic.AtomicBoolean

abstract class CommandParent<
    BaseSender : Any,
    out Sender : BaseSender,
    Output : Any?,
    Super : Command<BaseSender, Sender, Output, Super>
> {
    protected val commands = HashMap<String, Super>()

    /**
     * Cascades into every child to find the command referenced by the provided arguments
     * @param[args] The arguments that may contain command identifiers
     * @param[predicate] A conditional check for continuing into the next layer of children
     * @return The found command, that has been validated by all [predicate]s
     */
    @JvmOverloads fun find(
        control: AtomicBoolean,
        lastFound: Super? = null,
        args: MutableList<String>,
        predicate: (Super) -> Boolean = { true },
        whenAccepted: (Super) -> Unit = { },
    ): Super? {
        // Skip search if control is cancelled
        if (!control.get()) return lastFound
        // Skip search if there are no arguments
        if (args.isEmpty()) return lastFound
        // Peek the identifier
        val identifier = args.first().lowercase()
        // Find the next one
        val found = findSingle(identifier)
        if (found == null) {
            control.set(false)
            return lastFound
        }
        // Test preconditions, return null if it failed
        if (!predicate(found)) {
            control.set(false)
            return null
        }
        // Consume the identifier
        args.removeFirst()
        // Cascade into children
        return found.apply(whenAccepted).find(control, found, args, predicate)
    }

    /**
     * Registers a Command to the internal collection
     * Custom registration tasks may be handled through [postRegister]
     */
    fun register(vararg children: Super) {
        children.forEach { child ->
            commands[child.name] = child
            child.aliases.forEach { commands[it] = child }
            postRegister(child)
        }
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
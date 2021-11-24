package me.hwiggy.kommander

interface ICommandParent<out Sender, Output, Cmd : Command<Sender, Output, Cmd>> {
    val children: CommandChildren<Sender, Output, Cmd>

    fun find(args: MutableList<String>): Cmd?
    fun register(other: Cmd)
}

class CommandParent<out Sender, Output, Cmd : Command<Sender, Output, Cmd>> : ICommandParent<Sender, Output, Cmd> {
    override val children = CommandChildren<Sender, Output, Cmd>()

    override fun find(args: MutableList<String>) = if (args.isNotEmpty()) {
        val identifier = args.first().lowercase()
        var found = children.find(identifier)
        if (found != null) {
            val next = found.find(args.subList(1, args.size))
            if (next != null) found = next
        }
        found
    } else null

    override fun register(other: Cmd) = children.register(other)
}

/**
 * Represents a registered map of child commands
 */
class CommandChildren<out Sender, Output, Cmd : Command<Sender, Output, Cmd>> {
    /**
     * Map for registering children by name
     */
    private val byLabel = HashMap<String, Cmd>()

    /**
     * Map for registering children by alias
     */
    private val byAlias = HashMap<String, Cmd>()

    /**
     * Registers a command to each of the maps
     */
    fun register(child: @UnsafeVariance Cmd) {
        byLabel[child.name.lowercase()] = child
        child.aliases.forEach {
            byAlias[it.lowercase()] = child
        }
    }

    /**
     * Attempts to locate a command using a primary or secondary identifier
     */
    fun find(identifier: String) = byLabel[identifier] ?: byAlias[identifier]

    /**
     * Returns the primary labels for every registered child
     */
    private fun identifiers() = byLabel.keys.toSet()
    /**
     * Joins the primary labels for each registered child, separated by `|`
     */
    fun concatIdentifiers() = identifiers().joinToString("|").ifEmpty { null }

    fun values() = byLabel.values.toList()
}

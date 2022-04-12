package me.hwiggy.kommander.spigot

import me.hwiggy.kommander.CommandHandler
import me.hwiggy.kommander.InvalidSyntaxException
import me.hwiggy.reflection.declaredConstructor
import me.hwiggy.reflection.declaredInstanceField
import org.bukkit.Bukkit
import org.bukkit.command.*
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.SimplePluginManager
import org.bukkit.plugin.java.JavaPlugin

open class BukkitCommandHandler :
    CommandHandler<CommandSender, CommandSender, Boolean, Unit, BukkitCommand<CommandSender>>(), TabExecutor {
    override fun defaultResult() = true

    override fun handleThrown(
        sender: CommandSender,
        command: BukkitCommand<CommandSender>,
        error: Exception
    ) = when (error) {
        is InvalidSyntaxException -> {
            command.informBadSyntax(sender, error.message!!)
            true
        }
        else -> {
            if (command.handleCaught(sender, error)){
                error.printStackTrace()
            }
            true
        }
    }

    override fun onTabComplete(
        baseSender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): MutableList<String> {
        val arguments = args.toMutableList().also { it.add(0, alias) }
        return acquireContext(baseSender, arguments, { sender, cmd, _ ->
            cmd.tabComplete(sender, arguments.toTypedArray())
        }) { _, _, ex -> ex.printStackTrace(); null } ?: mutableListOf()
    }

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ) = process(sender, args.toMutableList().also { it.add(0, command.name) })

    protected fun transformPluginCommand(
        pluginCommand: PluginCommand,
        child: BukkitCommand<CommandSender>
    ) {
        pluginCommand.label = child.name
        pluginCommand.description = child.description
        pluginCommand.usage = "/${child.getUsage()}"
        pluginCommand.aliases = child.aliases
        pluginCommand.permission = child.permission
        pluginCommand.permissionMessage = child.permissionMessage
        pluginCommand.tabCompleter = this
        pluginCommand.executor = this
    }
}

val COMMAND_MAP: CommandMap = SimplePluginManager::class.java.declaredInstanceField(
    "commandMap", Bukkit.getPluginManager()
)

/**
 * Registers commands via the SimplePluginManager CommandMap
 */
class ReflectiveCommandHandler(private val plugin: Plugin) : BukkitCommandHandler() {
    override fun postRegister(child: BukkitCommand<CommandSender>) {
        val pluginCommand = PluginCommand::class.java.declaredConstructor(
            arrayOf(String::class.java, Plugin::class.java), child.name, plugin
        ).also { transformPluginCommand(it, child) }
        val fallback = plugin.name.lowercase()
        COMMAND_MAP.register(fallback, pluginCommand)
    }
}

/**
 * Registers commands via the plugin.yml
 */
class StandardCommandHandler(private val plugin: JavaPlugin) : BukkitCommandHandler() {
    override fun postRegister(child: BukkitCommand<CommandSender>) {
        plugin.getCommand(child.name)?.also {
            transformPluginCommand(it, child)
        }
    }
}
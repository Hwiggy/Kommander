package me.hwiggy.kommander.proxy

import me.hwiggy.kommander.CommandHandler
import me.hwiggy.kommander.InvalidSyntaxException
import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.ProxyServer
import net.md_5.bungee.api.plugin.Command
import net.md_5.bungee.api.plugin.Plugin
import net.md_5.bungee.api.plugin.TabExecutor

class ProxyCommandHandler(private val plugin: Plugin): CommandHandler<CommandSender, CommandSender, Unit, Unit, ProxyCommand<CommandSender>>(), TabExecutor {
    override fun defaultResult() = Unit

    override fun postRegister(child: ProxyCommand<CommandSender>) {
        val command = object : Command(child.name, child.permission, *child.aliases.toTypedArray()) {
            override fun execute(sender: CommandSender, args: Array<out String>) {
                process(sender, args.toMutableList().also { it.add(0, child.name) })
            }
        }
        ProxyServer.getInstance().pluginManager.registerCommand(plugin, command)
    }

    override fun handleThrown(
        sender: CommandSender,
        command: ProxyCommand<CommandSender>,
        error: Exception
    ) = when (error) {
        is InvalidSyntaxException -> command.informBadSyntax(sender, error.message!!)
        else -> {
            if (command.handleCaught(sender, error)){ error.printStackTrace() }
            Unit
        }
    }

    override fun onTabComplete(baseSender: CommandSender, args: Array<out String>): MutableIterable<String> {
        val arguments = args.toMutableList()
        return acquireContext(baseSender, arguments, { sender, cmd, _ ->
            cmd.tabComplete(sender, arguments.toTypedArray())
        }) { _, _, ex -> ex.printStackTrace(); null } ?: mutableListOf()
    }
}
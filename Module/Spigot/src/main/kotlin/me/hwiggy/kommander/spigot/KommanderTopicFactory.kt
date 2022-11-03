package me.hwiggy.kommander.spigot

import me.hwiggy.kommander.arguments.Group
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.command.CommandSender
import org.bukkit.command.ConsoleCommandSender
import org.bukkit.command.PluginCommand
import org.bukkit.help.HelpTopic
import org.bukkit.help.HelpTopicFactory

class KommanderTopicFactory : HelpTopicFactory<PluginCommand> {
    override fun createTopic(command: PluginCommand) =
        if (command.isRegistered)
            KommanderHelpTopic(command.name, command.executor as BukkitCommandHandler) else null

    companion object {
        @JvmStatic fun register() {
            Bukkit.getHelpMap().registerHelpTopicFactory(
                BukkitCommandHandler::class.java,
                KommanderTopicFactory()
            )
        }
    }
}

class KommanderHelpTopic(label: String, handler: BukkitCommandHandler) : HelpTopic() {
    private val command = handler.findSingle(label)!!
    override fun canSee(sender: CommandSender): Boolean {
        if (sender is ConsoleCommandSender) return true
        return command.permission?.let(sender::hasPermission) ?: false
    }

    override fun getName() = "/${command.name}"
    override fun getShortText() = command.description

    override fun getFullText(forWho: CommandSender): String {
        val builder = StringBuilder()
        builder.append(ChatColor.GOLD)
        builder.append("Description: ")
        builder.append(ChatColor.WHITE)
        builder.append(command.description)
        builder.append("\n")

        builder.append(ChatColor.GOLD)
        builder.append("Usage: ")
        builder.append(ChatColor.WHITE)
        builder.append("/${command.getUsage()}")
        builder.append("\n")

        val detail = command.synopsis.buildParameterDetail({ param ->
            val paramBuilder = StringBuilder(ChatColor.DARK_GRAY.toString())
            paramBuilder.append(if (param is Group<*>) "┏" else "━")
            paramBuilder.append(" ${ChatColor.GOLD}${param.name}: ")
            paramBuilder.append("${ChatColor.WHITE}${param.description}")
            if (param.default != null) {
                paramBuilder.append(" (Default: ")
                if (param is Group<*>) paramBuilder.append(param.default!!.synopsisName)
                else paramBuilder.append(param.default)
                paramBuilder.append(")")
            }
            paramBuilder.append("\n")
            if (param is Group<*>) {
                param.choices.forEachIndexed { index, (option, description) ->
                    val symbol = (if (index == param.choices.size - 1) "┗" else "┣") + "━"
                    paramBuilder.append("${ChatColor.DARK_GRAY}$symbol ")
                    paramBuilder.append("${ChatColor.GOLD}${option.synopsisName}: ")
                    paramBuilder.append("${ChatColor.WHITE}$description\n")
                }
            }
            paramBuilder.toString()
        }, { it.joinToString("\n") })
        if (detail != null) {
            builder.append(ChatColor.GOLD)
            builder.append("Synopsis:\n")
            builder.append(detail)
            builder.append("\n")
        }
        if (command.aliases.isNotEmpty()) {
            builder.append(ChatColor.GOLD)
            builder.append("Aliases: ")
            builder.append(ChatColor.WHITE)
            builder.append(command.aliases.joinToString(","))
        }
        return builder.toString()
    }

    override fun amendCanSee(amendedPermission: String?) = Unit
    override fun amendTopic(amendedShortText: String?, amendedFullText: String?) = Unit
    override fun applyAmendment(baseText: String?, amendment: String?) = baseText
}
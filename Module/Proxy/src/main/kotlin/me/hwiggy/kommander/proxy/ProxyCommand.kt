package me.hwiggy.kommander.proxy

import me.hwiggy.kommander.Command
import me.hwiggy.kommander.Extensions.test
import me.hwiggy.kommander.arguments.Arguments
import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.connection.ProxiedPlayer

abstract class ProxyCommand<out Sender : CommandSender> : Command<CommandSender, Sender, Unit, ProxyCommand<@UnsafeVariance Sender>>() {
    open val permission: String? = null

    open val permissionMessage = "§cYou do not have permission to use that command!"
    open val badSenderMessage = "§cYou are not a valid executor for this command!"
    open val badChildMessage = "§cYou did not provide a valid subcommand!"
    open val exceptionCaughtMessage = "§cAn exception occurred! Check console!"

    override fun postRegister(child: ProxyCommand<@UnsafeVariance Sender>) { child.parent = this }

    /**
     * Any exception that propagates from this command into the command handler is returned here.
     * @return: Whether the stacktrace should be printed or not
     */
    open fun handleCaught(sender: CommandSender, caught: Exception): Boolean {
        sender.sendMessage(exceptionCaughtMessage)
        return true
    }

    /**
     * Tests if the given [Sender] sender is allowed to run this command.
     * @return false if invalid sender, else true.
     */
    abstract fun isValidSender(sender: CommandSender): Boolean

    /**
     * Informs the given sender that they have used bad syntax.
     * The message is obtained via a caught [InvalidSyntaxException]
     * Recommended following up this information with proper syntax where necessary
     * Default implementation sends first the error message, then the correct usage.
     */
    open fun informBadSyntax(sender: @UnsafeVariance Sender, message: String) {
        sender.sendMessage(message)
        sender.sendMessage("Usage: /${getUsage()}")
    }

    /**
     * Gets the tab completions for this command
     * Default return value is a list of children commands
     */
    open fun tabComplete(sender: @UnsafeVariance Sender, arguments: Array<String>) =
        children().map { it.name }.toMutableList()

    /**
     * Executes the command
     * Default implementation sends an invalid subcommand message to the sender.
     */
    override fun execute(sender: @UnsafeVariance Sender, arguments: Arguments.Processed) {
        sender.sendMessage(badChildMessage)
    }


    override fun applyConditions(sender: CommandSender): Boolean {
        if (!isValidSender(sender)) {
            sender.sendMessage(badSenderMessage)
            return false
        }
        if (permission?.test(sender::hasPermission) == false) {
            sender.sendMessage(permissionMessage)
            return false
        }
        return true
    }

}

abstract class ProxiedPlayerCommand : ProxyCommand<ProxiedPlayer>() {
    final override fun isValidSender(sender: CommandSender) = sender is ProxiedPlayer
}
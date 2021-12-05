import me.hwiggy.kommander.Command
import me.hwiggy.kommander.arguments.Arguments

interface CommandSender { val name: String }

open class NumberSender : CommandSender {
    override val name = "Number"
}

class IntegerSender : NumberSender() {
    override val name = "Integer"
}

class StringSender : CommandSender {
    override val name = "String"
}

abstract class CommandBootstrap<out Sender : CommandSender> :
    Command<CommandSender, Sender, String, CommandBootstrap<@UnsafeVariance Sender>>() {
    abstract fun isValid(sender: CommandSender): Boolean
    override fun applyConditions(sender: CommandSender) = isValid(sender)
}

abstract class GenericBootstrap : CommandBootstrap<CommandSender>() {
    override fun isValid(sender: CommandSender) = true
    override fun execute(sender: CommandSender, arguments: Arguments.Processed): String {
        return "Root Command"
    }
}
abstract class NumberBootstrap : CommandBootstrap<NumberSender>() {
    override fun isValid(sender: CommandSender) = sender is NumberSender
    override fun execute(sender: NumberSender, arguments: Arguments.Processed): String {
        return "Number Command"
    }
}
abstract class IntegerBootstrap : CommandBootstrap<IntegerSender>() {
    override fun isValid(sender: CommandSender) = sender is IntegerSender
    override fun execute(sender: IntegerSender, arguments: Arguments.Processed): String {
        return "Integer Command"
    }
}
abstract class StringBootstrap : CommandBootstrap<StringSender>() {
    override fun isValid(sender: CommandSender) = sender is StringSender
    override fun execute(sender: StringSender, arguments: Arguments.Processed): String {
        return "String Command"
    }
}

/**
 * /root
 * /root number
 * /root number integer
 * /root integer
 * /root string
 */
class RootCommand : GenericBootstrap() {
    override val name = "root"
    override val description = "Root Description"

    init {
        register(NumberCommand())
        register(IntegerCommand())
        register(StringCommand())
    }
}

class NumberCommand : NumberBootstrap() {
    override val name = "number"
    override val description = "Number Description"

    init {
        register(IntegerCommand())
    }
}

class IntegerCommand : IntegerBootstrap() {
    override val name = "integer"
    override val description = "Integer Description"
}

class StringCommand : StringBootstrap() {
    override val name = "string"
    override val description = "String Description"
}
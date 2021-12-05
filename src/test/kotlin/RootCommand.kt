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
    final override fun execute(
        sender: @UnsafeVariance Sender,
        arguments: Arguments.Processed
    ) = sender.name

    override fun applyConditions(sender: CommandSender) = isValid(sender)
}

abstract class GenericBootstrap : CommandBootstrap<CommandSender>() {
    override fun isValid(sender: CommandSender) = true
}
abstract class NumberBootstrap : CommandBootstrap<NumberSender>() {
    override fun isValid(sender: CommandSender) = sender is NumberSender
}
abstract class IntegerBootstrap : CommandBootstrap<IntegerSender>() {
    override fun isValid(sender: CommandSender) = sender is IntegerSender
}
abstract class StringBootstrap : CommandBootstrap<StringSender>() {
    override fun isValid(sender: CommandSender) = sender is StringSender
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
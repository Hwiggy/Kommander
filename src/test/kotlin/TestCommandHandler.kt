import me.hwiggy.kommander.CommandHandler
import me.hwiggy.kommander.arguments.Arguments

class TestCommandHandler :
    CommandHandler<CommandSender, CommandSender, Unit, String, CommandBootstrap<CommandSender>>()
{
    override fun defaultResult() = Unit

    override fun convertResult(cmdOut: String) {
        println(cmdOut)
    }

    override fun handleThrown(
        sender: CommandSender,
        command: CommandBootstrap<CommandSender>,
        error: Exception
    ) {
        error.printStackTrace()
    }

    fun perform(sender: CommandSender, input: String) {
        val args = Arguments.split(input)
        val identifier = args.removeFirst()
        process(sender, identifier, args)
    }
}
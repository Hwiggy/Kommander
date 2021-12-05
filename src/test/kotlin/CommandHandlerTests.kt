import org.junit.jupiter.api.Test

class Tests {
    private val handler = TestCommandHandler().also {
        it.register(RootCommand())
    }

    private fun testCommand(sender: CommandSender, input: String) {
        handler.perform(sender, input)
    }

    private fun testRootCommand(sender: CommandSender) {
        testCommand(sender, "root")
    }

    private fun testRootNumberCommand(sender: CommandSender) {
        testCommand(sender, "root number")
    }

    private fun testRootNumberIntegerCommand(sender: CommandSender) {
        testCommand(sender, "root number integer")
    }

    private fun testRootIntegerCommand(sender: CommandSender) {
        testCommand(sender, "root integer")
    }

    private fun testRootStringCommand(sender: CommandSender) {
        testCommand(sender, "root string")
    }

    @Test
    fun testRootCommandNumberSender() {
        println("/root - Number Sender")
        testRootCommand(NumberSender())
    }

    @Test
    fun testRootCommandIntegerSender() {
        println("/root - Integer Sender")
        testRootCommand(IntegerSender())
    }

    @Test
    fun testRootCommandStringSender() {
        println("/root - String Sender")
        testRootCommand(StringSender())
    }

    @Test
    fun testRootNumberNumberSender() {
        println("/root number - Number Sender")
        testRootNumberCommand(NumberSender())
    }

    @Test
    fun testRootNumberIntegerSender() {
        println("/root number - Integer Sender")
        testRootNumberCommand(IntegerSender())
    }

    @Test
    fun testRootNumberIntegerIntegerSender() {
        println("/root number integer - Integer Sender")
        testRootNumberIntegerCommand(IntegerSender())
    }

    @Test
    fun testRootIntegerIntegerSender() {
        println("/root integer - Integer Sender")
        testRootIntegerCommand(IntegerSender())
    }

    @Test
    fun testRootStringStringSender() {
        println("/root string - String Sender")
        testRootStringCommand(StringSender())
    }
}
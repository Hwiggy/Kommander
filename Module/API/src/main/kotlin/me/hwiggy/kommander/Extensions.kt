package me.hwiggy.kommander

object Extensions {
    fun <T : Any?> T.test(predicate: (T) -> Boolean?) = predicate(this)
}
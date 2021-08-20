package me.hwiggy.kommander

/**
 * Thrown when a syntax error occurs in command processing.
 */
class InvalidSyntaxException(error: String) : RuntimeException(error)
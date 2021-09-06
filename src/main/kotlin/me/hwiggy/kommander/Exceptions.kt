package me.hwiggy.kommander

/**
 * Thrown when a syntax error occurs in command processing.
 */
class InvalidSyntaxException(error: String) : RuntimeException(error)

class InvalidParameterException(option: Any?, group: String) : RuntimeException(
    "Option '$option' not valid for parameter '$group'!"
)
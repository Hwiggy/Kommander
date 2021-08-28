package me.hwiggy.kommander

/**
 * Thrown when a syntax error occurs in command processing.
 */
class InvalidSyntaxException(error: String) : RuntimeException(error)

class MissingParameterException(name: String) : RuntimeException(
    "Required parameter '$name' is not present!"
)

class InvalidParameterException(option: Any?, group: String) : RuntimeException(
    "Option '$option' not valid for parameter '$group'!"
)
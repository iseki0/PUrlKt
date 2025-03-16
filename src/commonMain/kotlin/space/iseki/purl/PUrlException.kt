package space.iseki.purl

/**
 * Base exception class for all PUrl related exceptions.
 *
 * @property message Optional error message describing the exception
 */
open class PUrlException(message: String? = null) : RuntimeException(message)

/**
 * Exception thrown when there is an error during PUrl building process.
 *
 * @property message Error message describing the build failure
 */
class PUrlBuildException(message: String) : PUrlException(message)

/**
 * Exception thrown when there is an error during PUrl parsing process.
 *
 * @property input The input string that failed to parse
 * @property reason The reason why parsing failed
 * @property message Formatted error message including the reason and escaped input string
 */
class PUrlParsingException(val input: String, val reason: String) : PUrlException() {
    override val message: String
        get() = "Parsing error: $reason, in PURL ${escapeString(input)}"
}


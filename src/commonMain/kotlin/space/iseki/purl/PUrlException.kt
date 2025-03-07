package space.iseki.purl

// exception
open class PUrlException(message: String? = null) : RuntimeException(message)
class PUrlBuildException(message: String) : PUrlException(message)
class PUrlParsingException(val input: String, val reason: String) : PUrlException() {
    override val message: String
        get() = "Parsing error: $reason in ${escapeString(input)}"
}


package space.iseki.purl

import kotlin.experimental.and

/**
 * Represents a Uniform Resource Identifier (URI) as defined in RFC 3986.
 *
 * This class provides a way to parse, manipulate, and build URIs. It handles all components
 * of a URI including schema, authority, path, query, and fragment.
 *
 * The URI is immutable once created. To build a URI, use the [UriBuilder] class.
 *
 * @see UriBuilder
 * @see <a href="https://datatracker.ietf.org/doc/html/rfc3986">RFC 3986</a>
 */
internal class Uri(input: String) {
    private val string: String = input

    /** The range of indices in the original string that represent the schema component */
    val schemaIndices: IntRange?

    /** The range of indices in the original string that represent the authority component */
    val authorityIndices: IntRange?

    /** The range of indices in the original string that represent the path component */
    val pathIndices: IntRange?

    /** The range of indices in the original string that represent the query component */
    val queryIndices: IntRange?

    /** The range of indices in the original string that represent the fragment component */
    val fragmentIndices: IntRange?

    /** The range of indices in the original string that represent the userinfo component */
    val userinfoIndices: IntRange?

    /** The range of indices in the original string that represent the username component */
    val usernameIndices: IntRange?

    /** The range of indices in the original string that represent the port component */
    val portIndices: IntRange?

    /** The range of indices in the original string that represent the host component */
    val hostIndices: IntRange?

    /** The decoded authority component of the URI */
    val decodedAuthority: String?

    /** The decoded path component of the URI */
    val decodedPath: String?

    /** The decoded query component of the URI */
    val decodedQuery: String?

    /** The decoded fragment component of the URI */
    val decodedFragment: String?

    /** The decoded userinfo component of the URI */
    val decodedUserinfo: String?

    /** The decoded username component of the URI */
    val decodedUsername: String?

    /** The decoded host component of the URI */
    val decodedHost: String?

    /** The port number specified in the URI, or -1 if not specified */
    val port: Int

    /** The schema component of the URI, converted to lowercase */
    val schema: String? get() = schemaIndices?.let { string.substring(it).lowercase() }

    /** The raw (not decoded) authority component of the URI */
    val rawAuthority: String? get() = authorityIndices?.let { string.substring(it) }

    /** The raw (not decoded) path component of the URI */
    val rawPath: String? get() = pathIndices?.let { string.substring(it) }

    /** The raw (not decoded) query component of the URI */
    val rawQuery: String? get() = queryIndices?.let { string.substring(it) }

    /** The raw (not decoded) fragment component of the URI */
    val rawFragment: String? get() = fragmentIndices?.let { string.substring(it) }

    /** The raw (not decoded) userinfo component of the URI */
    val rawUserinfo: String? get() = userinfoIndices?.let { string.substring(it) }

    /** The raw (not decoded) username component of the URI */
    val rawUsername: String? get() = usernameIndices?.let { string.substring(it) }

    /** The raw (not decoded) port component of the URI */
    val rawPort: String? get() = portIndices?.let { string.substring(it) }

    /** The raw (not decoded) host component of the URI */
    val rawHost: String? get() = hostIndices?.let { string.substring(it) }

    init {
        val parser = Parser(input)
        parser.parseUri()
        schemaIndices = parser.schemaIndices
        authorityIndices = parser.authorityIndices
        pathIndices = parser.pathIndices
        queryIndices = parser.queryIndices
        fragmentIndices = parser.fragmentIndices
        userinfoIndices = parser.userinfoIndices
        usernameIndices = parser.usernameIndices
        portIndices = parser.portIndices
        hostIndices = parser.hostIndices
        decodedAuthority = authorityIndices?.let { string.unescape(it) }
        decodedPath = pathIndices?.let { string.unescape(it) }
        decodedQuery = queryIndices?.let { string.unescape(it) }
        decodedFragment = fragmentIndices?.let { string.unescape(it) }
        decodedUserinfo = userinfoIndices?.let { string.unescape(it) }
        decodedUsername = usernameIndices?.let { string.unescape(it) }
        decodedHost = hostIndices?.let { string.unescape(it) }
        port = portIndices?.takeUnless { it.isEmpty() }?.let { string.substring(it).toInt() } ?: -1
    }


    val queryArguments by lazy(LazyThreadSafetyMode.PUBLICATION) {
        if (queryIndices == null) return@lazy null
        string.substring(queryIndices)
            .split('&')
            .map { it.split('=', limit = 2) }
            .map { it[0].unescape() to it.getOrNull(1)?.unescape() }
    }


    /**
     * Returns the string representation of this URI.
     *
     * @return The original string that was used to create this URI
     */
    override fun toString(): String = string

}

internal fun String.unescape() = unescape(indices)

/**
 * Unescapes percent-encoded characters in the specified range of the string.
 *
 * @param range The range of indices to unescape
 * @return The unescaped string
 * @throws UriSyntaxException If the percent-encoding is invalid
 */
internal fun String.unescape(range: IntRange): String {
    val input = this
    if ('%' !in input) return input.slice(range)
    val inBytes = input.encodeToByteArray(range.first, range.last + 1)
    val outBytes = ByteArray(inBytes.size)
    var pos = 0
    var i = 0
    var chCount = 0
    while (i < inBytes.size) {
        val ch = inBytes[i]
        if (ch > 0 || ch and 0x11000000.toByte() == 0x11000000.toByte()) {
            chCount++
        }
        try {
            // Since UTF-8 is ASCII compatible, 37 is '%'
            if (ch == 37.toByte()) {
                val c1 = inBytes[i + 1].toInt().toChar()
                val c2 = inBytes[i + 2].toInt().toChar()
                outBytes[pos] = (hexToInt(c1) shl 4 or hexToInt(c2)).toByte()
                pos += 1
                i += 3
                continue
            }
        } catch (e: Exception) {
            if (e is IndexOutOfBoundsException || e is IllegalArgumentException) {
                throw UriSyntaxException(input, "Invalid percent encoding", chCount)
            }
            throw e
        }
        outBytes[pos] = ch
        pos += 1
        i += 1
    }
    return outBytes.decodeToString(0, pos)
}

private val UNRESERVED_L = 0b0000001111111111011000000000000000000000000000000000000000000000uL
private val UNRESERVED_H = 0b0100011111111111111111111111111010000111111111111111111111111110uL
private val GEN_DELIMS_L = 0b1000010000000000100000000000100000000000000000000000000000000000uL
private val GEN_DELIMS_H = 0b0000000000000000000000000000000000101000000000000000000000000001uL
private val SUB_DELIMS_L = 0b0010100000000000000111111101001000000000000000000000000000000000uL
private val SUB_DELIMS_H = 0b0000000000000000000000000000000000000000000000000000000000000000uL
private val RESERVED_L = 0b1010110000000000100111111101101000000000000000000000000000000000uL
private val RESERVED_H = 0b0000000000000000000000000000000000101000000000000000000000000001uL
private val SCHEMA_SUFFIX_L = 0b0000001111111111011010000000000000000000000000000000000000000000uL
private val SCHEMA_SUFFIX_H = 0b0000011111111111111111111111111000000111111111111111111111111110uL
private val AFTER_AUTHORITY_L = 0b1000000000000000100000000000100000000000000000000000000000000000uL
private val AFTER_AUTHORITY_H = 0b0000000000000000000000000000000000000000000000000000000000000000uL
private val AFTER_PATH_L = 0b1000000000000000000000000000100000000000000000000000000000000000uL
private val AFTER_PATH_H = 0b0000000000000000000000000000000000000000000000000000000000000000uL
private val HOST_ALLOWED_CHARS_L = 0b0010101111111111011111111101001000000000000000000000000000000000uL
private val HOST_ALLOWED_CHARS_H = 0b0100011111111111111111111111111010000111111111111111111111111110uL
private val USERINFO_ALLOWED_CHARS_L = 0b0010101111111111011111111101001000000000000000000000000000000000uL
private val USERINFO_ALLOWED_CHARS_H = 0b0100011111111111111111111111111010000111111111111111111111111110uL
private val USERNAME_ALLOWED_CHARS_L = 0b0010111111111111011111111101001000000000000000000000000000000000uL
private val USERNAME_ALLOWED_CHARS_H = 0b0100011111111111111111111111111010000111111111111111111111111110uL
private val QUERY_ALLOWED_CHARS_L = 0b1010111111111111111111111101001000000000000000000000000000000000uL
private val QUERY_ALLOWED_CHARS_H = 0b0100011111111111111111111111111010000111111111111111111111111111uL
internal val QUERY_ARG_ALLOWED_CHARS_L = 0b0000111111111111111101111001001000000000000000000000000000000000uL
internal val QUERY_ARG_ALLOWED_CHARS_H = 0b0100011111111111111111111111111010000111111111111111111111111110uL
internal val FRAGMENT_ALLOWED_CHARS_L = 0b0010111111111111111111111101001000000000000000000000000000000000uL
internal val FRAGMENT_ALLOWED_CHARS_H = 0b0100011111111111111111111111111010000111111111111111111111111110uL
private val IPVFUTURE_ALLOWED_CHARS_L = 0b0010111111111111011111111101001000000000000000000000000000000000uL
private val IPVFUTURE_ALLOWED_CHARS_H = 0b0100011111111111111111111111111010000111111111111111111111111110uL
private val SEGMENT_ALLOWED_CHARS_L = 0b0010111111111111011111111101001000000000000000000000000000000000uL
private val SEGMENT_ALLOWED_CHARS_H = 0b0100011111111111111111111111111010000111111111111111111111111111uL
internal val PATH_ALLOWED_CHARS_L = 0b0010111111111111111101111101001000000000000000000000000000000000uL
internal val PATH_ALLOWED_CHARS_H = 0b0100011111111111111111111111111010000111111111111111111111111110uL

/**
 * Checks if a character matches the given bit masks.
 *
 * This is an efficient way to check if a character belongs to a specific set
 * defined by the high and low bit masks.
 *
 * @param highMask The high bit mask (for characters with code points 64-127)
 * @param lowMask The low bit mask (for characters with code points 0-63)
 * @return true if the character matches the masks, false otherwise
 */
internal fun Char.match(highMask: ULong, lowMask: ULong) =
    code <= 127 && (code >= 64 && (1L shl (code - 64)) and highMask.toLong() != 0L || code < 64 && (1L shl code) and lowMask.toLong() != 0L)

/**
 * Escapes characters in the specified range of the string that don't match the given masks.
 *
 * Characters that don't match the masks will be percent-encoded according to RFC 3986.
 *
 * @param range The range of indices to escape
 * @param nHighMask The high bit mask for characters that should not be escaped
 * @param nLowMask The low bit mask for characters that should not be escaped
 * @return The escaped string
 */
internal fun String.escape(range: IntRange, nHighMask: ULong, nLowMask: ULong): String {
    val input = this
    if (input.all { it.match(nHighMask, nLowMask) }) return input.slice(range)
    val inBytes = input.encodeToByteArray(range.first, range.last + 1)
    return buildString {
        for (b in inBytes) {
            val ch = b.toInt().toChar()
            if (ch.match(nHighMask, nLowMask)) {
                append(ch)
                continue
            }
            append('%')
            append(intToHex(b.toInt() shr 4 and 0xf))
            append(intToHex((b and 0xf).toInt()))
        }
    }
}

/**
 * Escapes characters in the entire string that don't match the given masks.
 *
 * @param nHighMask The high bit mask for characters that should not be escaped
 * @param nLowMask The low bit mask for characters that should not be escaped
 * @return The escaped string
 */
internal fun String.escape(nHighMask: ULong, nLowMask: ULong) = escape(indices, nHighMask, nLowMask)

/**
 * Conditionally escapes the string based on the given condition.
 *
 * @param cond The condition to check
 * @param nHighMask The high bit mask for characters that should not be escaped
 * @param nLowMask The low bit mask for characters that should not be escaped
 * @return The escaped string if the condition is true, otherwise the original string
 */
internal fun String.escapeIf(cond: Boolean, nHighMask: ULong, nLowMask: ULong) =
    if (cond) escape(nHighMask, nLowMask) else this

/**
 * Convert a hexadecimal character to its integer value
 *
 * @param ch The hexadecimal character to convert
 * @return The integer value of the hexadecimal character
 * @throws IllegalArgumentException if the character is not a valid hexadecimal character
 */
private fun hexToInt(ch: Char): Int = when (ch) {
    in '0'..'9' -> ch - '0'
    in 'A'..'F' -> ch - 'A' + 10
    in 'a'..'f' -> ch - 'a' + 10
    else -> throw IllegalArgumentException("Invalid hex character: $ch")
}

/**
 * Convert an integer value to its hexadecimal character (uppercase)
 *
 * @param n The integer value to convert (must be in the range 0-15)
 * @return The hexadecimal character representation
 */
private fun intToHex(n: Int): Char = when {
    n < 10 -> '0' + n
    else -> 'A' + (n - 10)
}


private class Parser(val input: String) {
    private var pos = 0
    var schemaIndices: IntRange? = null
    var authorityIndices: IntRange? = null
    var pathIndices: IntRange? = null
    var queryIndices: IntRange? = null
    var fragmentIndices: IntRange? = null
    var userinfoIndices: IntRange? = null
    var usernameIndices: IntRange? = null
    var portIndices: IntRange? = null
    var hostIndices: IntRange? = null

    /**
     * Parses the entire URI string into its component parts.
     *
     * This method calls the individual parsing methods in the correct order
     * to parse the URI according to the grammar defined in RFC 3986.
     */
    fun parseUri() {
        parseSchema()
        if (parseAuthoritySlash()) {
            parseAuthorityWhole()
        }
        parsePath()
        parseQuery()
        parseFragment()
    }

    /**
     * Parses the schema component of the URI.
     *
     * According to RFC 3986, the schema must start with a letter and can only contain
     * letters, digits, '+', '-', and '.'. It is terminated by a colon.
     *
     * @return true if a schema was found, false otherwise
     */
    fun parseSchema(): Boolean {
        var i = pos
        if (i >= input.length) return false
        if (!input[i].isLetter()) return false
        while (i < input.length) {
            val ch = input[i]
            if (ch == ':') {
                schemaIndices = 0 until i
                i += 1
                pos = i
                return true
            }
            if (!ch.match(SCHEMA_SUFFIX_H, SCHEMA_SUFFIX_L)) return false
            i += 1
        }
        return false
    }

    /**
     * Checks if the authority component is introduced by a double slash.
     *
     * According to RFC 3986, the authority component is preceded by "//".
     *
     * @return true if the authority component is introduced, false otherwise
     */
    fun parseAuthoritySlash(): Boolean {
        if (pos + 1 < input.length && input[pos] == '/' && input[pos + 1] == '/') {
            pos += 2
            return true
        }
        return false
    }

    /**
     * Parses the authority component of the URI.
     *
     * The authority component consists of an optional userinfo subcomponent,
     * a host subcomponent, and an optional port subcomponent.
     */
    fun parseAuthorityWhole() {
        val authorizedEnd = input.indexOfAnyOrEnd(AFTER_AUTHORITY_H, AFTER_AUTHORITY_L, pos)
        authorityIndices = pos until authorizedEnd

        val atPos = input.lastIndexOfRange('@', pos until authorizedEnd)
        if (atPos != -1) {
            // userinfo found
            userinfoIndices = pos until atPos
            val colonPos = input.indexOfInRange(':', pos until atPos)
            usernameIndices = if (colonPos == -1) userinfoIndices else pos until colonPos
            pos = atPos + 1
        }

        var hostEnd = authorizedEnd
        // finding port, like ":8080"
        for (i in (authorizedEnd - 1) downTo pos) {
            if (input[i] == ':') {
                portIndices = i + 1 until authorizedEnd
                hostEnd = i
                break
            }
            if (!input[i].isDigit()) break
        }
        hostIndices = pos until hostEnd
        pos = authorizedEnd
    }

    /**
     * Parses the path component of the URI.
     *
     * The path component is terminated by a '?' or '#' character, or the end of the URI.
     */
    fun parsePath() {
        val pathEnd = input.indexOfAnyOrEnd(AFTER_PATH_H, AFTER_PATH_L, pos)
        pathIndices = (pos until pathEnd).takeUnless { it.isEmpty() }
        pos = pathEnd
    }

    /**
     * Parses the query component of the URI.
     *
     * The query component is introduced by a '?' character and terminated by a '#' character
     * or the end of the URI.
     */
    fun parseQuery() {
        if (pos >= input.length || input[pos] != '?') return
        pos += 1
        val queryEnd = input.indexOfOrEnd('#', pos)
        queryIndices = pos until queryEnd
        pos = queryEnd
    }

    /**
     * Parses the fragment component of the URI.
     *
     * The fragment component is introduced by a '#' character and extends to the end of the URI.
     */
    fun parseFragment() {
        if (pos >= input.length || input[pos] != '#') return
        pos += 1
        val fragmentEnd = input.length
        fragmentIndices = pos until fragmentEnd
        pos = fragmentEnd
    }

    /**
     * Finds the last index of a character within a specified range.
     *
     * @param char The character to find
     * @param range The range to search within
     * @return The last index of the character, or -1 if not found
     */
    private fun String.lastIndexOfRange(char: Char, range: IntRange): Int {
        for (i in range.last downTo range.first) {
            if (this[i] == char) return i
        }
        return -1
    }

    /**
     * Finds the first index of a character within a specified range.
     *
     * @param char The character to find
     * @param range The range to search within
     * @return The first index of the character, or -1 if not found
     */
    private fun String.indexOfInRange(char: Char, range: IntRange): Int {
        for (i in range) {
            if (this[i] == char) return i
        }
        return -1
    }

    /**
     * Finds the index of a character, or returns the length of the string if not found.
     *
     * @param char The character to find
     * @param startIndex The index to start searching from
     * @return The index of the character, or the length of the string if not found
     */
    private fun String.indexOfOrEnd(char: Char, startIndex: Int = 0) =
        indexOf(char, startIndex).let { if (it >= 0) it else length }

    /**
     * Finds the index of any character that matches the given masks, or returns the length of the string if not found.
     *
     * @param charsHigh The high bit mask for characters to match
     * @param charsLow The low bit mask for characters to match
     * @param startIndex The index to start searching from
     * @return The index of the first matching character, or the length of the string if not found
     */
    private fun String.indexOfAnyOrEnd(charsHigh: ULong, charsLow: ULong, startIndex: Int = 0): Int =
        indexOfAny(charsHigh, charsLow, startIndex).let { if (it >= 0) it else length }

    /**
     * Finds the index of any character that matches the given masks.
     *
     * @param charsHigh The high bit mask for characters to match
     * @param charsLow The low bit mask for characters to match
     * @param startIndex The index to start searching from
     * @return The index of the first matching character, or -1 if not found
     */
    private fun String.indexOfAny(charsHigh: ULong, charsLow: ULong, startIndex: Int = 0): Int {
        for (i in startIndex..lastIndex) {
            if (this[i].match(charsHigh, charsLow)) return i
        }
        return -1
    }

}

internal class UriBuilder {
    private var schema: String = ""
    private var host: String? = null
    private var port = -1
    private var userinfo: String? = null
    private val query = mutableListOf<String>()
    private var rawQuery: String? = null
    private var fragment: String? = null
    private val pathSegments = mutableListOf<String>()
    private var rawPath: String? = null

    /**
     * Specify schema part of the URI
     *
     * The schema part is case-insensitive, and should only contain letters, digits, '+', '-', and '.'.
     * And it must start with a letter.
     *
     * @throws IllegalArgumentException if the schema is invalid
     */
    fun withSchema(schema: String) = apply {
        require(schema.isNotEmpty()) { "Schema must not be empty" }
        require(schema.first().isLetter()) { "Schema must start with a letter" }
        require(schema.all { it.match(SCHEMA_SUFFIX_H, SCHEMA_SUFFIX_L) }) {
            "Schema can only contains letters, digits, '+', '-', and '.'"
        }
        this.schema = schema.lowercase()
    }

    /**
     * Specify userinfo part of the URI
     *
     * The userinfo part is in the format of `username:password`, both fields will be percent-encoded.
     *
     * @param username the username part of the URI
     * @param password the password part of the URI
     */
    fun withUserinfo(username: String, password: String) = apply {
        val encodedUsername = username.escape(USERNAME_ALLOWED_CHARS_H, USERNAME_ALLOWED_CHARS_L)
        val encodedPassword = password.escape(USERNAME_ALLOWED_CHARS_H, USERNAME_ALLOWED_CHARS_L)
        userinfo = "$encodedUsername:$encodedPassword"
    }

    /**
     * Specify userinfo part of the URI
     *
     * The userinfo part is in the format of `username`, which will be percent-encoded.
     *
     * @param userinfo the userinfo part of the URI
     * @param escape whether to percent-encode the userinfo part
     */
    fun withUserinfo(userinfo: String, escape: Boolean = true) = apply {
        this.userinfo = userinfo.escapeIf(escape, USERINFO_ALLOWED_CHARS_H, USERINFO_ALLOWED_CHARS_L)
    }

    /**
     * Specify host part of the URI
     *
     *
     * Following [RFC 3986](https://datatracker.ietf.org/doc/html/rfc3986#appendix-A),
     * the host part can be an IPv4 address, a registered name, or an IPv6/Future address, in the following format:
     * - IPv4 address: `192.168.1.1`
     * - IPv6 address: `[2001:db8::7]`
     * - IPvFuture address: `[v1.anything:you:want]`
     * - Registered name: `example.com`(in that case, the host part will be percent-encoded, see parameter `encode`)
     *
     * The port number can't be included in the host part.
     *
     * @param host the host part of the URI
     * @param encode whether to percent-encode the host part, if needed
     * @throws IllegalArgumentException if the host is invalid
     */
    fun withHost(host: String, encode: Boolean = true) = apply {
        require(host.isNotEmpty()) { "Host must not be empty" }
        // Here's some discussion about how to decide if the input string is an IPv6 or not,
        // since our builder allowing auto percentage-encoding.
        // I think we should keep the rules like java.net.URI,
        // which throws an exception if the input is a malformed IPv6.
        val isIPv6 = host.startsWith('[') && host.endsWith(']')
        if (isIPv6) {
            val range = 1..<host.lastIndex
            require(isValidVFuture(host, range) || isValidIPv6(host, range)) {
                "Invalid IPv6 or IPvFuture address"
            }
        } else if (!encode) {
            require(isValidIPv4(host, host.indices) || isValidRegName(host, host.indices)) {
                "Invalid IPv4 or registered name"
            }
        }
        // Even the RFC suggest to encode the host name in INDA format,
        // but since we can't ensure here's a domain name, we'd better use the safe way.
        // Maybe we can do some detection in the build stage?(Such as to detect the schema)
        // Normalize host to lowercase as per RFC 3986
        val normalizedHost = if (!isIPv6) host.lowercase() else host
        this.host = normalizedHost.escapeIf(encode && !isIPv6, HOST_ALLOWED_CHARS_H, HOST_ALLOWED_CHARS_L)
    }


    /**
     * Add a query argument to the URI
     *
     * The key and value will be percent-encoded.
     *
     * The rawQuery added by [withRawQuery] will be cleared.
     *
     * @param key the key of the query argument
     * @param value the value of the query argument
     */
    fun addQuery(key: String, value: String) = apply {
        val ek = key.escape(QUERY_ARG_ALLOWED_CHARS_H, QUERY_ARG_ALLOWED_CHARS_L)
        val ev = value.escape(QUERY_ARG_ALLOWED_CHARS_H, QUERY_ARG_ALLOWED_CHARS_L)
        query.add("$ek=$ev")
        rawQuery = null
    }

    /**
     * Specify the query part of the URI
     *
     * Query arguments added by [addQuery] will be cleared.
     *
     * @param query the query part of the URI
     * @param escape whether to percent-encode the query part
     */
    fun withRawQuery(query: String, escape: Boolean = true) = apply {
        // I don't know should we check the query string if contains '#' or any other unexpected characters, when the escape = false
        this.rawQuery = query.escapeIf(escape, QUERY_ALLOWED_CHARS_H, QUERY_ALLOWED_CHARS_L)
        this.query.clear()
    }

    /**
     * Specify fragment part of the URI
     *
     * @param fragment the fragment part of the URI
     * @param escape whether to percent-encode the fragment part
     */
    fun withFragment(fragment: String, escape: Boolean = true) = apply {
        this.fragment = fragment.escapeIf(escape, FRAGMENT_ALLOWED_CHARS_H, FRAGMENT_ALLOWED_CHARS_L)
    }

    /**
     * Specify port part of the URI
     *
     * The port number must be an integer in the range of 0 to 65535.
     *
     * @throws IllegalArgumentException if the port is invalid
     */
    fun withPort(port: Int) = apply {
        require(port in 0..65535) { "Port must be in the range of 0 to 65535" }
        this.port = port
    }

    /**
     * Add a path segment to the URI
     *
     * The path segment will be percent-encoded.
     *
     * @param segment the path segment to add
     */
    fun addPathSegment(segment: String) = apply {
        pathSegments.add(segment)
        rawPath = null
    }

    /**
     * Specify the path part of the URI
     *
     * Path segments added by [addPathSegment] will be cleared.
     *
     * @param path the path part of the URI
     */
    fun withRawPath(path: String, escape: Boolean = true) = apply {
        this.rawPath = path.escapeIf(escape, PATH_ALLOWED_CHARS_H, PATH_ALLOWED_CHARS_L)
        this.pathSegments.clear()
    }

    /**
     * Build the URI using the configured components.
     *
     * @return The built URI
     */
    fun build(): Uri {
        return buildString {
            if (schema.isNotEmpty()) {
                append(schema)
                append(':')
            }
            val hasAuthorize = userinfo != null || host != null
            if (hasAuthorize) {
                append("//")
                if (!userinfo.isNullOrEmpty()) {
                    append(userinfo)
                    append('@')
                }
                if (host != null) {
                    append(host)
                }
                if (port != -1) {
                    append(':')
                    append(port)
                }
            }
            if (rawPath != null) {
                if (hasAuthorize) {
                    append('/')
                }
                append(rawPath)
            } else {
                for ((i, s) in pathSegments.withIndex()) {
                    if (i != 0 || hasAuthorize) {
                        append('/')
                    }
                    val encoded = if (i == 1 && schema.isEmpty()) {
                        s.escape(SEGMENT_ALLOWED_CHARS_H, SEGMENT_ALLOWED_CHARS_L).replace(":", "%3A")
                    } else {
                        s.escape(SEGMENT_ALLOWED_CHARS_H, SEGMENT_ALLOWED_CHARS_L)
                    }
                    append(encoded)
                }
            }
            if (rawQuery != null) {
                append('?')
                append(rawQuery)
            } else {
                if (query.isNotEmpty()) {
                    append('?')
                    for ((i, s) in query.withIndex()) {
                        if (i != 0) append('&')
                        append(s)
                    }
                }
            }
            if (fragment != null) {
                append('#')
                append(fragment)
            }
        }.let { Uri(it) }
    }


}

/**
 * Convenience function to build a URI using the builder pattern.
 *
 * @param block The block to configure the URI builder
 * @return The built URI
 */
internal inline fun buildUri(block: UriBuilder.() -> Unit) = UriBuilder().apply(block).build()

/**
 * Exception thrown when a URI syntax error is encountered.
 *
 * @property input The input string that caused the error
 * @property reason The reason for the error
 * @property index The index in the input string where the error occurred, or -1 if not applicable
 */
internal class UriSyntaxException(val input: String, val reason: String, val index: Int) : RuntimeException() {
    override val message: String = buildString {
        append(reason)
        if (index > -1) {
            append(" at index ")
            append(index)
        }
        append(": ")
        append(input)
    }
}


/**
 * Checks if a character is a hexadecimal digit (0-9, a-f, A-F).
 *
 * @return true if the character is a hexadecimal digit, false otherwise
 */
internal fun Char.isHexDigit() = isDigit() || lowercaseChar() in 'a'..'f'

/**
 * Checks if a string contains a valid percent-encoded sequence at the given index.
 *
 * A valid percent-encoded sequence consists of a '%' character followed by two hexadecimal digits.
 *
 * @param s The string to check
 * @param i The index to check at
 * @return true if the string contains a valid percent-encoded sequence at the given index, false otherwise
 */
internal fun isValidPctEncoded(s: String, i: Int): Boolean =
    s.length > i + 2 && s[i] == '%' && s[i + 1].isHexDigit() && s[i + 2].isHexDigit()

/**
 * Checks if a string is a valid registered name according to RFC 3986.
 *
 * A registered name consists of unreserved characters, percent-encoded characters, and sub-delimiters.
 *
 * @param s The string to check
 * @param range The range of indices to check
 * @return true if the string is a valid registered name, false otherwise
 */
internal fun isValidRegName(s: String, range: IntRange): Boolean {
    var i = 0
    while (i <= range.last) {
        when {
            s[i].match(UNRESERVED_H, UNRESERVED_L) || s[i].match(SUB_DELIMS_H, SUB_DELIMS_L) -> i++
            isValidPctEncoded(s, i) -> i += 3
            else -> return false
        }
    }
    return true
}

/**
 * Checks if a string is a valid IPvFuture address according to RFC 3986.
 *
 * An IPvFuture address has the format "v" followed by one or more hexadecimal digits,
 * followed by "." and one or more characters from the set of unreserved characters,
 * sub-delimiters, and ":".
 *
 * @param s The string to check
 * @param range The range of indices to check
 * @return true if the string is a valid IPvFuture address, false otherwise
 */
internal fun isValidVFuture(s: String, range: IntRange): Boolean {
    if (range.isEmpty()) return false
    if (range.last - range.first + 1 < 4 || s[range.first] != 'v') return false
    var i = 1 + range.first
    while (true) {
        if (i >= range.last) return false
        if (s[i] == '.' && i != 1) break
        if (!s[i].isHexDigit()) return false
        i++
    }
    for (i in (i + 1)..range.last) {
        if (!s[i].match(IPVFUTURE_ALLOWED_CHARS_H, IPVFUTURE_ALLOWED_CHARS_L)) return false
    }
    return true
}

/**
 * Checks if a string is a valid IPv4 address according to RFC 3986.
 *
 * An IPv4 address consists of four decimal numbers, each in the range 0-255,
 * separated by periods.
 *
 * @param s The string to check
 * @param range The range of indices to check
 * @return true if the string is a valid IPv4 address, false otherwise
 */
internal fun isValidIPv4(s: String, range: IntRange): Boolean {
    var i = range.first
    var c = -1
    var seg = 0
    while (i <= range.last) {
        val ch = s[i]
        when {
            ch == '.' -> {
                if (c == -1) return false
                c = -1
                seg++
                if (seg > 4) return false
            }

            ch.isDigit() -> {
                val n = ch - '0'
                if (n !in 0..9) return false
                if (c == -1) {
                    if (n == 0 && i + 1 <= range.last && s[i + 1] != '.') return false
                    c = n
                } else {
                    c = c * 10 + n
                    if (c > 255) return false
                }
            }

            else -> return false
        }
        i++
    }
    return seg == 3
}

/**
 * Checks if a string is a valid IPv6 address according to RFC 3986.
 *
 * An IPv6 address consists of up to eight groups of four hexadecimal digits,
 * separated by colons. A double colon (::) can be used once to indicate one or more
 * groups of zeros.
 *
 * @param s The string to check
 * @param range The range of indices to check
 * @return true if the string is a valid IPv6 address, false otherwise
 */
internal fun isValidIPv6(s: String, range: IntRange): Boolean {
    var i = range.first
    var alreadyDoubleColon = false
    val lastIndex = range.last
    var numberCount = 0
    var chunkCount = 0
    while (i <= lastIndex) {
        val ch = s[i]
        when {
            ch == ':' -> {
                numberCount = 0
                if (i + 1 <= lastIndex && s[i + 1] == ':') {
                    if (alreadyDoubleColon) return false
                    alreadyDoubleColon = true
                    if (i + 2 <= lastIndex && s[i + 2] == ':') return false
                }
            }

            ch.isHexDigit() -> {
                if (numberCount == 0) chunkCount++
                numberCount++
                if (numberCount > 4) return false
            }

            else -> return false
        }
        i++
    }
    return alreadyDoubleColon || chunkCount == 8
}

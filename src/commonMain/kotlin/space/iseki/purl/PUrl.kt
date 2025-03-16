package space.iseki.purl

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.serialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Package URL (purl) is a string that is used to identify and locate a software package.
 * It follows the specification defined at https://github.com/package-url/purl-spec.
 *
 * A purl is a URL string with seven components:
 * - scheme: always 'pkg'
 * - type: the package type such as maven, npm, nuget, etc.
 * - namespace: the name prefix such as a Maven groupId or a GitHub user or organization
 * - name: the name of the package
 * - version: the version of the package
 * - qualifiers: extra qualifying data for a package such as OS, architecture, etc.
 * - subpath: extra subpath within a package, relative to the package root
 */
@ConsistentCopyVisibility
@Serializable(with = PUrlSerializer::class)
data class PUrl internal constructor(
    val type: String,
    val namespace: List<String>,
    val name: String,
    val version: String,
    val qualifiers: List<Pair<String, String>>,
    val subpath: String,
) {
    /**
     * Builder class for creating PUrl instances.
     * Use this class to construct a PUrl object with the desired components.
     */
    class Builder {
        private var type: String = ""
        private val namespace = mutableListOf<String>()
        private var name: String = ""
        private var version: String = ""
        private val qualifiers = mutableListOf<Pair<String, String>>()
        private var subpath: String = ""

        /**
         * Sets the type component of the purl.
         * The type will be converted to lowercase.
         *
         * @param type The package type (e.g., maven, npm, nuget)
         * @return This builder instance for method chaining
         */
        fun type(type: String): Builder {
            this.type = type.lowercase()
            return this
        }

        /**
         * Sets the namespace component of the purl.
         *
         * @param namespace The list of namespace segments
         * @return This builder instance for method chaining
         */
        fun namespace(namespace: List<String>): Builder {
            this.namespace.clear()
            this.namespace.addAll(namespace)
            return this
        }

        /**
         * Sets the name component of the purl.
         *
         * @param name The package name
         * @return This builder instance for method chaining
         */
        fun name(name: String): Builder {
            this.name = name
            return this
        }

        /**
         * Sets the version component of the purl.
         *
         * @param version The package version
         * @return This builder instance for method chaining
         */
        fun version(version: String): Builder {
            this.version = version
            return this
        }

        /**
         * Sets the qualifiers component of the purl.
         *
         * @param qualifiers The list of qualifier key-value pairs
         * @return This builder instance for method chaining
         */
        fun qualifiers(qualifiers: List<Pair<String, String>>): Builder {
            this.qualifiers.clear()
            this.qualifiers.addAll(qualifiers)
            return this
        }

        /**
         * Sets the subpath component of the purl.
         *
         * @param subpath The subpath within the package
         * @return This builder instance for method chaining
         */
        fun subpath(subpath: String): Builder {
            this.subpath = subpath
            return this
        }

        /**
         * Builds a PUrl instance with the configured components.
         * This method validates and corrects the values according to the package type specifications.
         *
         * @return A new PUrl instance
         * @throws PUrlBuildException If validation fails
         */
        fun build(): PUrl {
            return try {
                build0()
            } catch (e: PUrlException) {
                throw PUrlBuildException(e.message.orEmpty())
            }
        }

        internal fun build0(): PUrl {
            // Check if type is valid
            if (type.isNotEmpty()) {
                // Type cannot start with a number
                if (type[0].isDigit()) {
                    fail("type cannot start with a number")
                }
                // Type cannot contain invalid characters
                if (!type.matches(Regex("^[a-zA-Z0-9.+-]+$"))) {
                    fail("type contains invalid characters, only [a-zA-Z0-9.+-] are allowed")
                }
            }

            // Check if qualifier keys are valid
            for ((key, _) in qualifiers) {
                if (key.contains(" ")) {
                    fail("qualifier key cannot contain spaces: $key")
                }
            }

            // Validate and correct values based on package type
            when (type.lowercase()) {
                "alpm" -> {
                    if (namespace.isEmpty()) fail("alpm: namespace is required (vendor such as arch, arch32, archarm, manjaro or msys)")
                    // Namespace is case-insensitive and must be lowercase
                    for (i in namespace.indices) {
                        namespace[i] = namespace[i].lowercase()
                    }
                    // Name is case-insensitive and must be lowercase
                    name = name.lowercase()
                }

                "apk" -> {
                    if (namespace.isEmpty()) fail("apk: namespace is required (vendor such as alpine or openwrt)")
                    // Namespace is case-insensitive and must be lowercase
                    for (i in namespace.indices) {
                        namespace[i] = namespace[i].lowercase()
                    }
                    // Name is case-insensitive and must be lowercase
                    name = name.lowercase()
                }

                "bitbucket" -> {
                    if (namespace.isEmpty()) fail("bitbucket: namespace is required (user or organization)")
                    // Namespace is case-insensitive and must be lowercase
                    for (i in namespace.indices) {
                        namespace[i] = namespace[i].lowercase()
                    }
                    // Name is case-insensitive and must be lowercase
                    name = name.lowercase()
                }

                "bitnami" -> {
                    // Name must be lowercase
                    name = name.lowercase()
                }

                "cargo" -> {
                    // No special validation
                }

                "cocoapods" -> {
                    if (name.contains(" ") || name.contains("+") || name.startsWith(".")) {
                        fail("cocoapods: name cannot contain whitespace, a plus (+) character, or begin with a period (.)")
                    }
                }

                "composer" -> {
                    if (namespace.isEmpty()) fail("composer: namespace is required (vendor)")
                    // Namespace is case-insensitive and must be lowercase
                    for (i in namespace.indices) {
                        namespace[i] = namespace[i].lowercase()
                    }
                    // Name is case-insensitive and must be lowercase
                    name = name.lowercase()
                }

                "conan" -> {
                    // Conan packages need both namespace and channel qualifier, or neither
                    val hasChannel = qualifiers.any { it.first == "channel" }
                    if (namespace.isNotEmpty() && !hasChannel) {
                        fail("conan: when namespace is present, channel qualifier is required")
                    }
                    if (namespace.isEmpty() && hasChannel) {
                        fail("conan: when channel qualifier is present, namespace is required")
                    }
                }

                "conda" -> {
                    // No special validation
                }

                "cpan" -> {
                    if (namespace.isNotEmpty()) {
                        // If namespace is present, it must be uppercase CPAN author ID
                        for (i in namespace.indices) {
                            namespace[i] = namespace[i].uppercase()
                        }
                        // Distribution name cannot contain "::"
                        if (name.contains("::")) {
                            fail("cpan: distribution name must not contain '::'")
                        }
                    } else {
                        // If no namespace, it's a module name that can contain "::" but not "-"
                        if (name.contains("-")) {
                            fail("cpan: module name must not contain '-'")
                        }
                    }
                }

                "cran" -> {
                    // Name is case-sensitive
                    // CRAN packages must have a version
                    if (name.isEmpty()) {
                        fail("cran: name is required")
                    }
                    if (version.isEmpty()) {
                        fail("cran: version is required")
                    }
                }

                "deb" -> {
                    if (namespace.isEmpty()) fail("deb: namespace is required (vendor such as debian or ubuntu)")
                    // Namespace is case-insensitive and must be lowercase
                    for (i in namespace.indices) {
                        namespace[i] = namespace[i].lowercase()
                    }
                    // Name is case-insensitive and must be lowercase
                    name = name.lowercase()
                }

                "docker" -> {
                    // No special validation
                }

                "gem" -> {
                    // No special validation
                }

                "generic" -> {
                    if (name.isEmpty()) fail("generic: name is required")
                }

                "github" -> {
                    if (namespace.isEmpty()) fail("github: namespace is required (user or organization)")
                    // Namespace is case-insensitive and must be lowercase
                    for (i in namespace.indices) {
                        namespace[i] = namespace[i].lowercase()
                    }
                    // Name is case-insensitive and must be lowercase
                    name = name.lowercase()
                }

                "golang" -> {
                    // Namespace and name must be lowercase
                    for (i in namespace.indices) {
                        namespace[i] = namespace[i].lowercase()
                    }
                    name = name.lowercase()
                }

                "hackage" -> {
                    // Name is case-sensitive and uses kebab-case
                }

                "hex" -> {
                    // Namespace is optional, case-insensitive, and must be lowercase
                    for (i in namespace.indices) {
                        namespace[i] = namespace[i].lowercase()
                    }
                    // Name is case-insensitive and must be lowercase
                    name = name.lowercase()
                }

                "huggingface" -> {
                    // Huggingface packages can have no namespace
                    // Name is case-sensitive
                    // Version is case-insensitive and must be lowercase
                    if (version.isNotEmpty()) {
                        version = version.lowercase()
                    }
                }

                "luarocks" -> {
                    // Namespace is case-insensitive, lowercase recommended
                    for (i in namespace.indices) {
                        namespace[i] = namespace[i].lowercase()
                    }
                    // Name is case-insensitive, lowercase recommended
                    name = name.lowercase()
                    // Version is case-sensitive, must be lowercase
                    version = version.lowercase()
                }

                "maven" -> {
                    if (namespace.isEmpty()) fail("maven: namespace is required (group id)")
                }

                "mlflow" -> {
                    // Check if repository_url contains azuredatabricks.net
                    val repositoryUrl = qualifiers.find { it.first == "repository_url" }?.second ?: ""
                    if (repositoryUrl.contains("azuredatabricks.net")) {
                        // Azure Databricks: name is case-insensitive and must be lowercase
                        name = name.lowercase()
                    }
                    // Otherwise name is case-sensitive (like Azure ML)
                }

                "npm" -> {
                    // Name cannot have uppercase letters
                    name = name.lowercase()
                }

                "nuget" -> {
                    // No special validation
                }

                "oci" -> {
                    // Name is case-insensitive and must be lowercase
                    name = name.lowercase()
                    // Version is required, format is sha256:hex_encoded_lowercase_digest
                    if (version.isEmpty()) {
                        fail("oci: version is required (sha256:hex_encoded_lowercase_digest)")
                    }
                }

                "pub" -> {
                    // Name must be lowercase and only allow [a-z0-9_] characters
                    name = name.lowercase()
                    if (!name.matches(Regex("^[a-z0-9_]+$"))) {
                        fail("pub: name must only contain [a-z0-9_] characters")
                    }
                }

                "pypi" -> {
                    // PyPI treats "-" and "_" as the same character and is case-insensitive
                    // Name must be lowercase, underscores replaced with dashes
                    name = name.lowercase().replace("_", "-")
                }

                "rpm" -> {
                    if (namespace.isEmpty()) fail("rpm: namespace is required (vendor such as fedora or opensuse)")
                    // Namespace is case-insensitive and must be lowercase
                    for (i in namespace.indices) {
                        namespace[i] = namespace[i].lowercase()
                    }
                    // Name is case-sensitive
                }

                "swid" -> {
                    // Check if tag_id qualifier exists
                    val hasTagId = qualifiers.any { it.first == "tag_id" && it.second.isNotEmpty() }
                    if (!hasTagId) {
                        fail("swid: tag_id qualifier must not be empty")
                    }
                }

                "swift" -> {
                    if (namespace.isEmpty()) fail("swift: namespace is required (source host and user/organization)")
                    if (version.isEmpty()) fail("swift: version is required")
                    if (name.isEmpty()) {
                        fail("swift: name is required")
                    }
                }

                else -> {
                    // For unknown types, no special validation
                }
            }

            return PUrl(
                type = type,
                namespace = asUnmodifiableList(namespace),
                name = name,
                version = version,
                qualifiers = asUnmodifiableList(qualifiers),
                subpath = subpath,
            )
        }
    }

    /**
     * Returns the string representation of this PUrl.
     *
     * @return The canonical string representation of this PUrl
     */
    override fun toString(): String = _toString

    fun toUriString() = _toString

    private val _toString by lazy(LazyThreadSafetyMode.PUBLICATION) {
        buildUri {
            withSchema("pkg")
            addPathSegment(type)
            buildList {
                add(type.escape(PATH_ALLOWED_CHARS_H, PATH_ALLOWED_CHARS_L))
                // especially rules:
                // the '@' version separator must be encoded as %40 elsewhere
                namespace.forEach { add(it.escape(PATH_ALLOWED_CHARS_H, PATH_ALLOWED_CHARS_L).replace("@", "%40")) }
                if (name.isNotEmpty()) {
                    add(
                        if (version.isNotEmpty()) {
                            "$name@$version".escape(PATH_ALLOWED_CHARS_H, PATH_ALLOWED_CHARS_L)
                        } else {
                            name.escape(PATH_ALLOWED_CHARS_H, PATH_ALLOWED_CHARS_L)
                        }
                    )
                }
            }.let {
                val joinedPath = it.joinToString("/")
                withRawPath(joinedPath, escape = false)
            }
            qualifiers.sortedBy { it.first }.forEach { (k, v) -> addQuery(k, v) }
            if (subpath.isNotEmpty()) {
                val p = subpath.split('/').filterNot { it == ".." || it == "." }.joinToString(separator = "/")
                withFragment(p)
            }
        }.toString()
    }

    companion object {
        /**
         * Parses a string into a PUrl object.
         *
         * @param input The string to parse
         * @return A PUrl object representing the parsed string
         * @throws PUrlParsingException If the string cannot be parsed
         */
        @JvmStatic
        fun parse(input: String): PUrl {
            try {
                val uri = try {
                    Uri(input)
                } catch (e: UriSyntaxException) {
                    throw PUrlParsingException(input, "invalid URI: " + e.message)
                }
                if (uri.schema != "pkg") throw PUrlParsingException(input, "invalid schema: ${uri.schema}")
                var pathSegments = uri.rawPath.orEmpty().removePrefix("/").split('/').map { it.unescape(it.indices) }
                if (!uri.decodedHost.isNullOrEmpty()) {
                    pathSegments = listOf(uri.decodedHost) + pathSegments
                }
                if (pathSegments.size < 2) {
                    throw PUrlParsingException(input, "type and name are required")
                }
                val (nameText, versionText) = pathSegments.last().let {
                    val i = it.indexOf('@')
                    if (i == -1) {
                        it to ""
                    } else {
                        it.substring(0, i) to if (i > it.length) "" else it.substring(i + 1)
                    }
                }
                return Builder().type(pathSegments.first())
                    .name(nameText)
                    .namespace(pathSegments.slice(1 until pathSegments.lastIndex))
                    .version(versionText)
                    .qualifiers(uri.queryArguments.orEmpty().map { (k, v) -> k.lowercase() to v.orEmpty() })
                    .subpath(uri.decodedFragment.orEmpty().removeSurrounding("/"))
                    .build0()
            } catch (e: PUrlException) {
                if (e is PUrlParsingException) throw e
                throw PUrlParsingException(input, e.message.orEmpty())
            }
        }
    }
}

object PUrlSerializer : KSerializer<PUrl> {
    override val descriptor: SerialDescriptor
        get() = serialDescriptor<String>()

    override fun deserialize(decoder: Decoder): PUrl {
        try {
            return PUrl.parse(decoder.decodeString())
        } catch (e: PUrlParsingException) {
            throw SerializationException("failed to decode PURL: " + e.message, e)
        }
    }

    override fun serialize(encoder: Encoder, value: PUrl) {
        encoder.encodeString(value.toString())
    }

}

private fun fail(message: String): Nothing {
    throw PUrlException(message)
}

internal fun <T> asUnmodifiableList(list: List<T>): List<T> =
    if (list.isEmpty()) emptyList() else object : AbstractList<T>() {
        override val size: Int
            get() = list.size

        override fun get(index: Int): T = list[index]

        override fun toString(): String = list.toString()
    }

internal fun String.escapeStringLiteral(): String {
    if (isEmpty()) return ""
    val chars = this.toCharArray()
    val length = chars.size
    var from = 0
    var to = 0
    while (from < length) {
        var ch = chars[from++]
        if (ch == '\\') {
            ch = if (from < length) chars[from++] else '\u0000'
            when (ch) {
                'b' -> ch = '\b'
                'f' -> ch = '\u000c'
                'n' -> ch = '\n'
                'r' -> ch = '\r'
                's' -> ch = ' '
                't' -> ch = '\t'
                '\'', '\"', '\\' -> { /* 保持原样 */
                }

                in '0'..'7' -> {
                    val limit = minOf(from + if (ch <= '3') 2 else 1, length)
                    var code = ch - '0'
                    while (from < limit) {
                        val c = chars[from]
                        if (c < '0' || c > '7') break
                        from++
                        code = (code shl 3) or (c - '0')
                    }
                    ch = code.toChar()
                }

                '\n' -> continue
                '\r' -> {
                    if (from < length && chars[from] == '\n') {
                        from++
                    }
                    continue
                }

                else -> {
                    val msg = String.format(
                        "Invalid escape sequence: \\%c \\\\u%04X", ch, ch.code
                    )
                    throw IllegalArgumentException(msg)
                }
            }
        }
        chars[to++] = ch
    }
    return String(chars, 0, to)
}

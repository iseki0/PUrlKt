package space.iseki.purl

import kotlinx.serialization.Serializable

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
@Serializable
data class PUrl internal constructor(
    val schema: String,
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
        private var schema: String = "pkg"
        private var type: String = ""
        private val namespace = mutableListOf<String>()
        private var name: String = ""
        private var version: String = ""
        private val qualifiers = mutableListOf<Pair<String, String>>()
        private var subpath: String = ""

        /**
         * Sets the schema component of the purl.
         * The schema will be converted to lowercase.
         *
         * @param schema The schema to set, typically "pkg"
         * @return This builder instance for method chaining
         */
        fun schema(schema: String): Builder {
            this.schema = schema.lowercase()
            return this
        }

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
                schema = schema,
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

    private val _toString by lazy(LazyThreadSafetyMode.PUBLICATION) {
        buildString {
            append(schema).append(':').append(type).append('/')
            namespace.forEachIndexed { index, s ->
                if (index != 0) append('/')
                append(encodeURIComponent(s))
            }
            if (namespace.isNotEmpty()) append('/')
            append(encodeURIComponent(name).replace("%3A", ":"))
            if (version.isNotEmpty()) append('@').append(encodeURIComponent(version))
            if (qualifiers.isNotEmpty()) {
                append('?')
                qualifiers.sortedBy { it.first }.forEachIndexed { index, (k, v) ->
                    if (index != 0) append('&')
                    append(encodeURIComponent(k))
                    if (v.isNotEmpty()) append('=').append(encodeURIComponent(v).replace("%2F", "/").replace("%3A", ":"))
                }
            }
            if (subpath.isNotEmpty()) append('#').append(
                subpath.split('/')
                .filterNot { it == ".." || it == "." }
                .joinToString(separator = "/") { encodeURIComponent(it) })
        }
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
                val schema = findingSchemaPart(input)
                val type = findingTypePart(input, schema.last + 2)
                val nsName = findingNsNamePart(input, type.last + 2)
                val name = findingNameInNsName(input, nsName)
                val namespace = run {
                    for (i in name.first - 1 downTo nsName.first) {
                        // skip the trailing '/'
                        if (input[i] == '/') continue
                        return@run nsName.first..i
                    }
                    return@run IntRange.EMPTY
                }
                var pos = nsName.last + 1
                val version = if (pos < input.length && input[pos] == '@') {
                    findingVersionPart(input, pos).also { pos = it.last + 1 }
                } else {
                    IntRange.EMPTY
                }
                val qualifiers = if (pos < input.length && input[pos] == '?') {
                    findingQualifiersPart(input, pos).also { pos = it.last + 1 }
                } else {
                    IntRange.EMPTY
                }
                val subpath = if (pos < input.length && input[pos] == '#') {
                    findingSubpathPart(input, pos).also { pos = it.last + 1 }
                } else {
                    IntRange.EMPTY
                }
                // parse namespace
                val namespaceSegments = parseNS(input, namespace)
                val nameText = input.substring(name).let(::decodeURIComponent)
                val typeText = input.substring(type).lowercase()
                val versionText = input.substring(version).let(::decodeURIComponent)
                val qualifierList = parseQuery(input, qualifiers)
                val subpathText = input.substring(subpath).removeSurrounding("/").let(::decodeURIComponent)
                val schemaText = input.substring(schema).lowercase()

                return Builder().schema(schemaText)
                    .type(typeText)
                    .name(nameText)
                    .namespace(namespaceSegments)
                    .version(versionText)
                    .qualifiers(qualifierList)
                    .subpath(subpathText)
                    .build0()
            } catch (e: PUrlException) {
                throw PUrlParsingException(input, e.message.orEmpty())
            }
        }
    }
}

private fun parseNS(input: String, range: IntRange): List<String> {
    if (range.isEmpty()) return emptyList()
    var c = 0
    for (i in range) {
        if (input[i] == '/') c++
    }
    if (c == 0) {
        return listOf(input.substring(range).let(::decodeURIComponent))
    }
    val r = arrayOfNulls<String>(c + 1)
    var pos = range.first
    for (i in 0..c) {
        val next = input.indexOf('/', pos).let { if (it == -1) range.last + 1 else it }
        r[i] = decodeURIComponent(input.substring(pos until next))
        pos = next + 1
    }
    @Suppress("UNCHECKED_CAST") return asUnmodifiableList(r.asList()) as List<String>
}

private fun parseQuery(input: String, range: IntRange): List<Pair<String, String>> {
    if (range.isEmpty()) return emptyList()
    var c = 0
    for (i in range) {
        if (input[i] == '&') c++
    }
    val r = arrayOfNulls<Pair<String, String>>(c + 1)
    var pos = range.first
    for (i in 0..c) {
        val next = input.indexOf('&', pos).let { if (it == -1) range.last + 1 else it }
        val eq = input.indexOf('=', pos)
        if (eq == -1 || eq > next) {
            r[i] = decodeURIComponent(input.substring(pos until next)).lowercase() to ""
        } else {
            r[i] =
                decodeURIComponent(input.substring(pos until eq)).lowercase() to decodeURIComponent(input.substring(eq + 1 until next))
        }
        pos = next + 1
    }
    @Suppress("UNCHECKED_CAST") return asUnmodifiableList(r.asList()) as List<Pair<String, String>>
}

private fun findingSchemaPart(input: String): IntRange {
    val pos = input.indexOf(':')
    return (0 until pos).also { if (it.isEmpty()) fail("parsing schema failed") }
}

private fun findingTypePart(input: String, startAt: Int): IntRange {
    val begin = startAt + measureLeadingSlash(input, startAt)
    return (begin..<input.indexOf('/', begin)).also {
        if (it.isEmpty()) fail("parsing type failed")
    }
}

private fun findingNsNamePart(input: String, startAt: Int): IntRange {
    val begin = startAt + measureLeadingSlash(input, startAt)
    var pos = begin
    while (pos < input.length) {
        when (input[pos]) {
            '?', '#', '@' -> break
            else -> pos += 1
        }
    }
    return (begin until pos).also { if (it.isEmpty()) fail("parsing namespace and name failed") }
}

private fun findingNameInNsName(input: String, nsName: IntRange): IntRange {
    for (i in nsName.reversed()) {
        (input.lastIndexOf('/', i) + 1..i).let { return it }
    }
    fail("parsing name failed")
}

private fun findingVersionPart(input: String, startAt: Int): IntRange {
    var pos = input.indexOf('@', startAt)
    if (pos == -1) return IntRange.EMPTY
    val begin = pos + 1
    while (pos < input.length) {
        when (input[pos]) {
            '?', '#' -> break
            else -> pos += 1
        }
    }
    return begin..<pos
}

private fun findingQualifiersPart(input: String, startAt: Int): IntRange {
    var pos = input.indexOf('?', startAt)
    if (pos == -1) return IntRange.EMPTY
    val begin = pos + 1
    while (pos < input.length) {
        when (input[pos]) {
            '#' -> break
            else -> pos += 1
        }
    }
    return begin..<pos
}

private fun findingSubpathPart(input: String, startAt: Int): IntRange {
    val pos = input.indexOf('#', startAt)
    if (pos == -1) return IntRange.EMPTY
    val begin = pos + 1
    return begin..<input.length
}

private fun measureLeadingSlash(input: String, startAt: Int): Int {
    var pos = startAt
    while (pos < input.length && input[pos] == '/') {
        pos += 1
    }
    return pos - startAt
}

private fun fail(message: String): Nothing {
    throw PUrlException(message)
}

private fun decodeURIComponent(input: String): String {
    try {
        return decodeURIComponent0(input)
    } catch (e: Exception) {
        val msg = e.message.orEmpty()
        fail("decodeURIComponent failed" + if (msg.isNotEmpty()) ": $msg" else "")
    }
}

private fun encodeURIComponent(input: String): String {
    try {
        return encodeURIComponent0(input)
    } catch (e: Exception) {
        val msg = e.message.orEmpty()
        fail("encodeURIComponent failed" + if (msg.isNotEmpty()) ": $msg" else "")
    }
}

internal expect fun <T> asUnmodifiableList(list: List<T>): List<T>
internal expect fun decodeURIComponent0(input: String): String
internal expect fun encodeURIComponent0(input: String): String
internal expect fun escapeString(input: String): String

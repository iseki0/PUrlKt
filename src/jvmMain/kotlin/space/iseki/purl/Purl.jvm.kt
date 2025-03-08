package space.iseki.purl

import java.nio.charset.StandardCharsets
import java.util.*

@JvmSynthetic
internal actual fun decodeURIComponent0(input: String): String {
    return java.net.URLDecoder.decode(input, "UTF-8")
}

internal actual fun <T> asUnmodifiableList(list: List<T>): List<T> =
    if (list.isEmpty()) emptyList() else Collections.unmodifiableList(list)

internal actual fun encodeURIComponent0(input: String): String {
    return java.net.URLEncoder.encode(input, StandardCharsets.UTF_8).replace("+", "%20")
}

internal actual fun escapeString(input: String): String {
    @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN") return (input as java.lang.String).translateEscapes()
}
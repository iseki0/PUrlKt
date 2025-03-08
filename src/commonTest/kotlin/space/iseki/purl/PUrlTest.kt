package space.iseki.purl

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PUrlTest {

    @Test
    fun testParse() {
        checkParse("schema:type/name/space/name")
        checkParse("schema:type/name/space/name?query")
        checkParse("schema:type/name/space/name#fragment")
        checkParse("schema:type/name/space/name@version")
        checkParse("schema:type/namespace/name?query#fragment@version")
        checkParse("schema://type/namespace/name")
        checkParse("schema://type/namespace/name?query")
        checkParse("schema://type/namespace/name#fragment")
        checkParse("schema://type/namespace/name@version")
        checkParse("schema://type/namespace/name?query#fragment@version")
    }

    private fun checkParse(s: String) {
        val p = PUrl.parse(s)
        val s2 = p.toString()
        val p2 = PUrl.parse(s2)
        assertEquals(p, p2)
        assertEquals(p.toString(), p2.toString())
        println("$s -> $p2")
    }

    @TestFactory
    fun bulkTest(): List<DynamicTest> {
        @Serializable
        data class A(
            val description: String,
            val purl: String,
            val canonical_purl: String?,
            val type: String?,
            val namespace: String?,
            val name: String?,
            val qualifiers: Map<String, String>?,
            val subpath: String?,
            val version: String?,
            val is_invalid: Boolean,
        )

        val json = Json { ignoreUnknownKeys = true }
        val list = json.decodeFromString<List<A>>(testData)
        return list.map {
            DynamicTest.dynamicTest(it.description) {
                if (it.is_invalid) {
                    assertFailsWith<PUrlParsingException>(message = it.toString()) { println(PUrl.parse(it.purl)) }
                } else {
                    val purl = PUrl.parse(it.purl)
                    assertEquals(it.type, purl.type, it.toString())
                    assertEquals(it.namespace.orEmpty(), purl.namespace.joinToString("/"), it.toString())
                    assertEquals(it.name, purl.name, it.toString())
                    assertEquals(it.version.orEmpty(), purl.version, it.toString())
                    assertEquals(it.qualifiers.orEmpty(), purl.qualifiers.toMap(), it.toString())
                    assertEquals(it.subpath.orEmpty(), purl.subpath, it.toString())
                    assertEquals(it.canonical_purl, purl.toString(), it.toString())
                }
            }
        }
    }
}
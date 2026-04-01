package space.iseki.purl

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class PUrlTest {
    @Test
    fun testParse() {
        checkParse("pkg:type/name/space/name")
        checkParse("pkg:type/name/space/name?query")
        checkParse("pkg:type/name/space/name#fragment")
        checkParse("pkg:type/name/space/name@version")
        checkParse("pkg:type/namespace/name?query#fragment@version")
        checkParse("pkg://type/namespace/name")
        checkParse("pkg://type/namespace/name?query")
        checkParse("pkg://type/namespace/name#fragment")
        checkParse("pkg://type/namespace/name@version")
        checkParse("pkg://type/namespace/name?query#fragment@version")
    }

    private fun checkParse(s: String) {
        val p = PUrl.parse(s)
        val s2 = p.toUriString()
        val p2 = PUrl.parse(s2)
        assertEquals(p, p2)
        assertEquals(p.toUriString(), p2.toUriString())
        println("$s -> $p2")
    }

    @Test
    fun buildCollectsMultipleValidationErrors() {
        val exception = assertFailsWith<PUrlBuildException> {
            PUrl.Builder()
                .type("1 bad")
                .name("")
                .qualifiers(listOf("bad key" to "value"))
                .build()
        }

        assertEquals(
            listOf(
                "name is required",
                "type cannot start with a number",
                "type contains invalid characters, only [a-zA-Z0-9.+-] are allowed",
                "qualifier key cannot contain spaces: bad key",
            ),
            exception.errors,
        )
        assertTrue(exception.message!!.endsWith("Found 4 errors."))
    }

    @Test
    fun buildKeepsSingleErrorMessageShape() {
        val exception = assertFailsWith<PUrlBuildException> {
            PUrl.Builder().name("demo").build()
        }

        assertEquals(listOf("type is required"), exception.errors)
        assertEquals("type is required", exception.message)
    }

}

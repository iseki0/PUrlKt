package space.iseki.purl

import kotlin.test.Test
import kotlin.test.assertEquals

class LegacyPUrlTest {
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

}
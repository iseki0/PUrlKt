package space.iseki.purl

import kotlinx.serialization.json.Json
import org.junit.jupiter.api.DynamicContainer
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import java.nio.file.Path
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.readText
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull

class TestDirectoryTest {

    val testEntries = Path.of("purl-spec/tests/types").listDirectoryEntries("*-test.json")

    @TestFactory
    fun testReadTestCases(): List<DynamicTest> {
        return testEntries.map {
            DynamicTest.dynamicTest(it.name) {
                val content = it.readText()
                val suite = Json.decodeFromString<PUrlTestSuite>(content)
                assertNotNull(suite.schema)
                assertNotEquals(0, suite.tests.size)
            }
        }
    }

    @TestFactory
    fun testAllTypes(): List<DynamicContainer> {
        val path = Path.of("purl-spec/tests/types")
        println(path.toAbsolutePath())
        return path.listDirectoryEntries("*-test.json").map {
            val content = it.readText()
            val suite = Json.decodeFromString<PUrlTestSuite>(content)
            val r = suite.tests.mapIndexed { index, test ->
                DynamicTest.dynamicTest("$index ${test.testType} ${test.testGroup}: ${test.description}") {
                    when (test.testType) {
                        TestType.PARSE -> {
                            val inputString = (test.input as StringInput).value
                            if (test.expectedFailure) {
                                assertFailsWith<PUrlParsingException> {
                                    PUrl.parse(inputString)
                                }
                            } else {
                                val purl = PUrl.parse(inputString)
                                val expected = (test.expectedOutput as ComponentsOutput).value
                                assertEquals(expected.type, purl.type)
                                assertEquals(expected.namespace.orEmpty(), purl.namespace.joinToString("/"))
                                assertEquals(expected.name, purl.name)
                                assertEquals(expected.version.orEmpty(), purl.version)
                                assertEquals(expected.qualifiers.orEmpty(), purl.qualifiers.toMap())
                                assertEquals(expected.subpath.orEmpty(), purl.subpath)
                            }
                        }

                        TestType.BUILD -> {
                            val inputComponents = (test.input as ComponentsInput).value
                            if (test.expectedFailure) {
                                assertFailsWith<PUrlBuildException> {
                                    PUrl.Builder().apply {
                                        inputComponents.type?.let { type(it) }
                                        inputComponents.namespace?.let {
                                            namespace(if (it.isEmpty()) emptyList() else it.split("/"))
                                        }
                                        inputComponents.name?.let { name(it) }
                                        inputComponents.version?.let { version(it) }
                                        inputComponents.qualifiers?.let {
                                            qualifiers(it.toList())
                                        }
                                        inputComponents.subpath?.let { subpath(it) }
                                    }.build()
                                }
                            } else {
                                val purl = PUrl.Builder().apply {
                                    inputComponents.type?.let { type(it) }
                                    inputComponents.namespace?.let {
                                        namespace(if (it.isEmpty()) emptyList() else it.split("/"))
                                    }
                                    inputComponents.name?.let { name(it) }
                                    inputComponents.version?.let { version(it) }
                                    inputComponents.qualifiers?.let {
                                        qualifiers(it.toList())
                                    }
                                    inputComponents.subpath?.let { subpath(it) }
                                }.build()
                                val expected = (test.expectedOutput as StringOutput).value
                                assertEquals(expected, purl.toString())
                            }
                        }

                        TestType.ROUNDTRIP -> {
                            val inputString = (test.input as StringInput).value
                            if (test.expectedFailure) {
                                assertFailsWith<Exception> {
                                    PUrl.parse(inputString).toString()
                                }
                            } else {
                                val result = PUrl.parse(inputString).toString()
                                val expected = (test.expectedOutput as StringOutput).value
                                assertEquals(expected, result)
                            }
                        }
                    }

                }
            }
            DynamicContainer.dynamicContainer(it.name, r)
        }
    }
}
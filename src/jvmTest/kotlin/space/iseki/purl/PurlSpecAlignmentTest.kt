package space.iseki.purl

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import kotlin.io.path.readText
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PurlSpecAlignmentTest {

    @Serializable
    data class PurlComponents(
        val type: String? = null,
        val namespace: String? = null,
        val name: String? = null,
        val version: String? = null,
        val qualifiers: Map<String, String>? = null,
        val subpath: String? = null,
    )

    @Serializable
    data class PurlSpecTestCase(
        val description: String,
        val test_group: String,
        val test_type: String,
        val input: JsonElement,
        val expected_output: JsonElement? = null,
        val expected_failure: Boolean = false,
        val expected_failure_reason: String? = null,
    )

    @Serializable
    data class PurlSpecTestFile(
        @SerialName("\$schema")
        val schema: String? = null,
        val tests: List<PurlSpecTestCase>,
    )

    private val json = Json { ignoreUnknownKeys = true }

    @TestFactory
    fun runPurlSpecTests(): List<DynamicTest> {
        return loadCases("purl-spec/tests/spec")
            .plus(loadCases("purl-spec/tests/types"))
            .map { (id, case) ->
                DynamicTest.dynamicTest(id) {
                    runCase(case)
                }
            }
    }

    private fun loadCases(resourceDir: String): List<Pair<String, PurlSpecTestCase>> {
        val rootUrl = requireNotNull(javaClass.classLoader.getResource(resourceDir)) {
            "Resource directory not found: $resourceDir"
        }
        val root = Paths.get(rootUrl.toURI())
        return Files.list(root).use { stream ->
            stream
                .filter { it.isRegularFile() && it.name.endsWith(".json") }
                .sorted()
                .flatMap { path ->
                    val file = json.decodeFromString<PurlSpecTestFile>(path.readText())
                    file.tests.mapIndexed { index, case ->
                        val id = "${path.name}#${index + 1} [${case.test_group}/${case.test_type}] ${case.description}"
                        id to case
                    }.stream()
                }
                .toList()
        }
    }

    private fun runCase(case: PurlSpecTestCase) {
        when (case.test_type) {
            "parse" -> runParseCase(case)
            "build" -> runBuildCase(case)
            "roundtrip" -> runRoundTripCase(case)
            else -> error("Unsupported test_type: ${case.test_type}")
        }
    }

    private fun runParseCase(case: PurlSpecTestCase) {
        val input = json.decodeFromJsonElement(String.serializer(), case.input)
        if (case.expected_failure) {
            assertFailsWith<PUrlParsingException>(case.description) {
                PUrl.parse(input)
            }
            return
        }

        val expected = json.decodeFromJsonElement(PurlComponents.serializer(), requireNotNull(case.expected_output))
        val actual = PUrl.parse(input)
        assertComponents(expected, actual, case.description)
    }

    private fun runBuildCase(case: PurlSpecTestCase) {
        val input = json.decodeFromJsonElement(PurlComponents.serializer(), case.input)
        if (case.expected_failure) {
            assertFailsWith<PUrlBuildException>(case.description) {
                buildPurl(input)
            }
            return
        }

        val expected = json.decodeFromJsonElement(String.serializer(), requireNotNull(case.expected_output))
        val actual = buildPurl(input).toString()
        assertEquals(expected, actual, case.description)
    }

    private fun runRoundTripCase(case: PurlSpecTestCase) {
        val input = json.decodeFromJsonElement(String.serializer(), case.input)
        if (case.expected_failure) {
            assertFailsWith<PUrlParsingException>(case.description) {
                PUrl.parse(input)
            }
            return
        }

        val expected = json.decodeFromJsonElement(String.serializer(), requireNotNull(case.expected_output))
        val actual = PUrl.parse(input).toString()
        assertEquals(expected, actual, case.description)
    }

    private fun buildPurl(input: PurlComponents): PUrl {
        val builder = PUrl.Builder()
        input.type?.let { builder.type(it) }
        input.namespace?.let {
            val segments = it.split('/').filter { segment -> segment.isNotEmpty() }
            builder.namespace(segments)
        }
        input.name?.let { builder.name(it) }
        input.version?.let { builder.version(it) }
        input.qualifiers?.let { qualifiers ->
            builder.qualifiers(qualifiers.entries.map { (k, v) -> k to v })
        }
        input.subpath?.let { builder.subpath(it) }
        return builder.build()
    }

    private fun assertComponents(expected: PurlComponents, actual: PUrl, message: String) {
        assertEquals(expected.type ?: "", actual.type, message)
        assertEquals(expected.namespace ?: "", actual.namespace.joinToString("/"), message)
        assertEquals(expected.name ?: "", actual.name, message)
        assertEquals(expected.version ?: "", actual.version, message)
        assertEquals(expected.qualifiers.orEmpty(), actual.qualifiers.toMap(), message)
        assertEquals(expected.subpath ?: "", actual.subpath, message)
    }
}

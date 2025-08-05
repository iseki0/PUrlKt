package space.iseki.purl

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Test group for PURL tests.
 */
@Serializable
enum class TestGroup {
    /**
     * Test group for base conformance tests for PURL building and parsing.
     */
    @SerialName("base")
    BASE,

    /**
     * Test group for advanced tests to support flexible PURL building and parsing.
     */
    @SerialName("advanced")
    ADVANCED
}

/**
 * Test type for PURL tests.
 */
@Serializable
enum class TestType {
    /**
     * A PURL building test from decoded components to a canonical PURL string.
     */
    @SerialName("build")
    BUILD,

    /**
     * A PURL parsing test from a PURL string to decoded components.
     */
    @SerialName("parse")
    PARSE,

    /**
     * A PURL roundtrip test, parsing then building back a PURL from a canonical string input.
     */
    @SerialName("roundtrip")
    ROUNDTRIP
}

/**
 * Individual decoded PURL components to use as a test input or expected output.
 *
 * @property type Package-URL type component
 * @property namespace Package-URL namespace decoded component
 * @property name Package-URL name decoded component
 * @property version Package-URL version decoded component
 * @property qualifiers Package-URL qualifiers decoded component as a map
 * @property subpath Package-URL subpath decoded component
 */
@Serializable
data class PUrlComponents(
    val type: String? = null,
    val namespace: String? = null,
    val name: String? = null,
    val version: String? = null,
    val qualifiers: Map<String, String>? = null,
    val subpath: String? = null,
)

/**
 * A PURL test with input and expected output.
 *
 * @property description A description for this test
 * @property testGroup The group of this test like 'base' or 'advanced'
 * @property testType The type of this test like 'build' or 'parse'
 * @property expectedFailure true if this test input is expected to fail to be processed
 * @property expectedFailureReason The reason why this test is expected to fail if expectedFailure is true
 * @property input The test input - can be either a PURL string or PUrlComponents object
 * @property expectedOutput The expected test output - can be either PUrlComponents or a PURL string
 */
@Serializable
data class PUrlTest(
    val description: String,
    @SerialName("test_group") val testGroup: TestGroup,
    @SerialName("test_type") val testType: TestType,
    @SerialName("expected_failure") val expectedFailure: Boolean = false,
    @SerialName("expected_failure_reason") val expectedFailureReason: String? = null,
    val input: PUrlTestInput,
    @SerialName("expected_output") val expectedOutput: PUrlTestOutput? = null,
)

/**
 * Test input that can be either a PURL string or decoded components.
 */
@Serializable(PUrlTestInput.Serializer::class)
sealed interface PUrlTestInput {
    data object Serializer : KSerializer<PUrlTestInput> {
        override val descriptor: SerialDescriptor
            get() = JsonElement.serializer().descriptor

        override fun serialize(encoder: Encoder, value: PUrlTestInput) {
            TODO("Not yet implemented")
        }

        override fun deserialize(decoder: Decoder): PUrlTestInput {
            return when (val value = decoder.decodeSerializableValue(JsonElement.serializer())) {
                is JsonObject -> Json.decodeFromJsonElement(ComponentsInput.serializer(), value)
                is JsonPrimitive -> StringInput(value.content)
                else -> throw SerializationException("Unsupported JSON element type for PUrlTestInput: ${value::class.java.simpleName}")
            }
        }

    }
}

/**
 * String input for parse and roundtrip tests.
 */
@Serializable
@JvmInline
value class StringInput(val value: String) : PUrlTestInput

/**
 * Components input for build tests.
 */
@Serializable
@JvmInline
value class ComponentsInput(val value: PUrlComponents) : PUrlTestInput

/**
 * Test output that can be either decoded PURL components or a canonical PURL string.
 */
@Serializable(PUrlTestOutput.Serializer::class)
sealed interface PUrlTestOutput {
    data object Serializer : KSerializer<PUrlTestOutput> {
        override val descriptor: SerialDescriptor
            get() = JsonElement.serializer().descriptor

        override fun serialize(
            encoder: Encoder,
            value: PUrlTestOutput,
        ) {
            TODO("Not yet implemented")
        }

        override fun deserialize(decoder: Decoder): PUrlTestOutput {
            return when (val value = decoder.decodeSerializableValue(JsonElement.serializer())) {
                is JsonObject -> Json.decodeFromJsonElement(ComponentsOutput.serializer(), value)
                is JsonPrimitive -> StringOutput(value.content)
                else -> throw SerializationException("Unsupported JSON element type for PUrlTestOutput: ${value::class.java.simpleName}")
            }
        }

    }
}

/**
 * Components output for parse tests.
 */
@Serializable
@JvmInline
value class ComponentsOutput(val value: PUrlComponents) : PUrlTestOutput

/**
 * String output for build and roundtrip tests.
 */
@Serializable
@JvmInline
value class StringOutput(val value: String) : PUrlTestOutput

/**
 * Root test suite containing a list of PURL tests.
 *
 * @property schema The JSON schema URL for Package-URL tests
 * @property tests A list of Package-URL build and parse tests
 */
@Serializable
data class PUrlTestSuite(
    @SerialName("\$schema") val schema: String? = "https://packageurl.org/schemas/purl-test.schema-1.0.json",
    val tests: List<PUrlTest>,
)

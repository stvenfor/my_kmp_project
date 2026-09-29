package com.example.my_kmp_project.core.network

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.contentOrNull

/**
 * Standard BFF envelope `{ code, message, data }` plus OAuth-style fields for
 * [Envelope Fallback] when the gateway returns HTTP 401 without a business `code`.
 */
@Serializable
public data class ApiEnvelope(
    @Serializable(with = FlexibleIntSerializer::class)
    val code: Int? = null,
    val message: String? = null,
    val data: JsonElement? = null,
    val error: String? = null,
    @SerialName("error_description")
    val errorDescription: String? = null,
)

/**
 * Accepts JSON number or numeric string for `code` (BFF sends both).
 */
public object FlexibleIntSerializer : KSerializer<Int?> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("FlexibleInt", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Int? {
        val json = decoder as? JsonDecoder ?: return decoder.decodeInt()
        val el = json.decodeJsonElement()
        val p = el as? JsonPrimitive ?: return null
        p.intOrNull?.let { return it }
        return p.contentOrNull?.toIntOrNull()
    }

    override fun serialize(encoder: Encoder, value: Int?) {
        if (value == null) encoder.encodeNull()
        else encoder.encodeInt(value)
    }
}

public fun ApiEnvelope.dataOrNull(): JsonElement? =
    data?.takeUnless { it is JsonNull }

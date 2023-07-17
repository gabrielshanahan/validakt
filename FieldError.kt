package cz.etn.btip.service.mapping

import arrow.core.nel
import cz.etn.btip.domain.common.mapping.NestedValidationErrorResponse
import cz.etn.btip.domain.common.mapping.Valid
import cz.etn.btip.domain.common.mapping.ValidationErrorResponse
import cz.etn.btip.domain.common.mapping.ValidationErrorResponseValue

@Deprecated(
    "Deprecated in favor of returning flat responses, and parsing on the client side." +
        "See e.g. cz.etn.btip.v2.api.mappers.asErrorMessages for inspiration on how to replace."
)
fun ValidationError.toResponse(): ValidationErrorResponse {
    var result: ValidationErrorResponse = ValidationErrorResponseValue((this::class.simpleName ?: toString()).nel())
    propertyPath.reversed()
        .forEach { result = NestedValidationErrorResponse(mapOf(it to result)) }

    return result
}

@Deprecated(
    "Deprecated in favor of returning flat responses, and parsing on the client side." +
        "See e.g. cz.etn.btip.v2.api.mappers.asErrorMessages for inspiration on how to replace."
)
fun List<ValidationError>.toResponse(): ValidationErrorResponse = map { it.toResponse() }.reduce(ValidationErrorResponse::merge)

/**
 * JSON is not a format which is expressive enough to encode information about
 * "the current level" - a value is either an object or a scalar, and cannot be both.
 * Therefore, if we need to write a scalar (ErrorValue) to a place where an object is
 * already contained (NestedError) we can only choose one.
 * If there is a problem with a whole group of fields, it should supersede problems with
 * subfields.
 */
fun ValidationErrorResponse.merge(other: ValidationErrorResponse): ValidationErrorResponse = when (this) {
    is ValidationErrorResponseValue -> {
        when (other) {
            is ValidationErrorResponseValue -> ValidationErrorResponseValue(errors + other.errors)
            is NestedValidationErrorResponse, Valid -> this
        }
    }
    is NestedValidationErrorResponse -> {
        when (other) {
            is Valid -> this
            is ValidationErrorResponseValue -> other
            is NestedValidationErrorResponse -> NestedValidationErrorResponse(mergeMaps(map, other.map, ValidationErrorResponse::merge))
        }
    }
    is Valid -> other
}

/**
 * Merges two maps, using [mergeValues] to handle distinct values that appear under the same keys in both.
 */
private fun <K, V> mergeMaps(
    mapA: Map<K, V>,
    mapB: Map<K, V>,
    mergeValues: (V, V) -> V
): Map<K, V> = mutableMapOf<K, V>().apply {
    val seq = (mapA.asSequence() + mapB.asSequence()).distinct()
    for ((key, value) in seq) {
        compute(key) { _, existing -> if (existing != null) mergeValues(existing, value) else value }
    }
}

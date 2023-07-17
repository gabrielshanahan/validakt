package cz.etn.btip.service.mapping

import arrow.core.Either
import arrow.core.Nel
import arrow.core.continuations.Effect
import arrow.core.continuations.effect
import arrow.core.toNonEmptyListOrNull
import cz.etn.btip.service.mapping.stateEffect.StateEffect
import cz.etn.btip.service.mapping.stateEffect.evalState

typealias PropertyPath = List<String>
val emptyPath: PropertyPath = emptyList()

interface ValidationError {
    val propertyPath: PropertyPath
}

typealias ValidationErrors = Nel<ValidationError>

data class CannotBeEmpty(override val propertyPath: PropertyPath) : ValidationError

typealias Validation<T> = StateEffect<PropertyPath, ValidationErrors, T>
typealias ValidationResult<T> = Effect<ValidationErrors, T>

suspend fun <R> Validation<R>.evaluate(): ValidationResult<R> = evalState(emptyPath)
suspend fun <T, R> Validation<T>.fold(
    error: suspend (error: Throwable) -> R,
    recover: suspend (shifted: ValidationErrors) -> R,
    transform: suspend (value: T) -> R
): R = evalState(emptyPath).fold(error, recover, transform)

/**
 * At some point, the need will probably arise for this to be mapped to a single error containing multiple paths.
 * However, as of 1.1.2023, we are not prepared to parse such errors on the FE (and, indeed, even the data
 * representation on the BE only expects a single path attribute), and since there is currently no need for
 * this capability, we're keeping the current implementation. The only place where this would be usable is the
 * AtLeastOneChecked part of TeslaDataValidatingMapper, however since this is the only place, we can just send three
 * errors with one path instead of one error with three paths, without the risk of confusion - there are no other
 * fields where this can happen.
 *
 * There are other places where this is needed, e.g. TeslaCustomerValidatingMapper::contact, however these are never
 * displayed on the FE, so we can also get by without this functionality.
 */
fun <T> Nel<Validation<T>>.firstValid(): Validation<T> = Validation { path ->
    effect {
        val result = fold(emptyList<ValidationError>()) { acc, mapper ->
            acc + mapper.runState(path).toEither().let {
                when (it) {
                    is Either.Left<ValidationErrors> -> it.value
                    is Either.Right<Pair<PropertyPath, T>> -> return@effect it.value
                }
            }
        }

        shift(result.toNonEmptyListOrNull()!!)
    }
}

inline fun <T, R> Nel<Validation<T>>.firstValidOf(
    block: (Validation<T>) -> Validation<R>
): Validation<R> = map(block).firstValid()

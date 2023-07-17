package cz.etn.btip.service.mapping

import arrow.core.Either
import arrow.core.Nel
import arrow.core.NonEmptyList
import arrow.core.continuations.EffectScope
import arrow.core.continuations.effect
import arrow.core.identity
import arrow.core.nonEmptyListOf
import cz.etn.btip.service.mapping.stateEffect.StateEffectScope
import kotlin.reflect.KCallable

class ValidationScope(
    state: PropertyPath,
    effectScope: EffectScope<ValidationErrors>
) : StateEffectScope<PropertyPath, ValidationErrors>(state, effectScope) {
    fun <T> KCallable<T>.it(): T {
        state += name
        return call()
    }

    suspend inline fun <T, R> KCallable<List<T>>.each(
        crossinline block: suspend ValidationScope.(T) -> R
    ): List<Validation<R>> =
        it().mapIndexed { idx, it ->
            validation {
                state += idx.toString()
                block(it)
            }
        }

    suspend inline fun fail(error: (PropertyPath) -> ValidationError): Nothing = shift(nonEmptyListOf(error(state)))

    suspend inline fun <T> Validation<T>.failedWith(
        crossinline error: (PropertyPath) -> ValidationError
    ): Validation<Nothing> = validation {
        bind()
        fail(error)
    }

    suspend inline fun <T : Any> T?.nn(
        error: (PropertyPath) -> ValidationError = ::CannotBeEmpty
    ): T = this ?: fail(error)

    suspend inline fun <T> catch(
        crossinline error: suspend (PropertyPath) -> ValidationError,
        crossinline f: suspend () -> T
    ): T = Either.catch { f() }.orNull() ?: fail { error(it) }

    suspend inline fun <T> catchWithThrowable(
        crossinline error: suspend (PropertyPath, Throwable) -> ValidationError,
        crossinline f: suspend () -> T
    ): T = Either.catch { f() }.fold(
        { exc -> fail { error(it, exc) } },
        ::identity
    )

    suspend inline fun <reified R> Any?.cast(
        error: (PropertyPath) -> ValidationError
    ): R = this as? R ?: fail(error)

    suspend inline fun <T : Any> KCallable<T?>.nn(
        error: (PropertyPath) -> ValidationError = ::CannotBeEmpty
    ): T = it().nn(error)

    suspend inline fun <T> KCallable<List<T>>.notEmpty(
        error: (PropertyPath) -> ValidationError = ::CannotBeEmpty
    ): Nel<T> = it().let {
        NonEmptyList(it.firstOrNull() ?: fail(error), it.drop(1))
    }

    suspend inline fun String?.nnOrEmpty(
        error: (PropertyPath) -> ValidationError = ::CannotBeEmpty
    ): String =
        takeUnless { it.isNullOrEmpty() } ?: fail(error)

    suspend inline fun KCallable<String?>.nnOrEmpty(
        error: (PropertyPath) -> ValidationError = ::CannotBeEmpty
    ): String =
        it().nnOrEmpty(error)

    suspend inline fun KCallable<String?>.nullOrNotEmpty(
        error: (PropertyPath) -> ValidationError = ::CannotBeEmpty
    ): String? = it()?.let { it.nnOrEmpty(error) }
}

suspend inline fun <A> validation(
    crossinline block: suspend ValidationScope.() -> A
): Validation<A> = Validation { state ->
    effect {
        val scope = ValidationScope(state, this)
        val result = block(scope)
        scope.state to result
    }
}

package cz.etn.btip.service.mapping.stateEffect

import arrow.core.continuations.effect
import arrow.core.traverse
import arrow.typeclasses.Semigroup
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/*
 * Ideally, we would define the functions bellow with a context(Semigroup<EE>) receiver. However, as of 1.8, Kotlin
 * causes "java.lang.VerifyError: Bad type on operand stack" errors during runtime if functions with context
 * receivers are inlined. This can be solved by running Java with -Xverify:none, however a quick Google search
 * revealed that it is not recommended to use this setting in production.
 *
 * Careful! When using gradle bootRun, -Xverify:none is added automatically, which means such code will work locally,
 * but fail once deployed.
 */

/**
 * Use the type-signature, Luke.
 *
 * Takes a [List] of [StateEffects][StateEffect], and creates a new [StateEffect] which evaluates them in parallel,
 * without changing its own state. We don't change the state because, after we run all the
 * [StateEffects][StateEffect], we get a list of new states (as opposed to a single one), and there's no consistent
 * way to pick one over the other. This is consistent with how [apZip] works.
 *
 * Ignoring the things that don't get changed, this function basically does
 * `List<StateEffect<T>> -> StateEffect<List<T>>`
 */
suspend fun <SS, EE, T> List<StateEffect<SS, EE, T>>.apSequence(
    semigroup: Semigroup<EE>,
): StateEffect<SS, EE, List<T>> = StateEffect {
    effect {
        coroutineScope {
            val res = map { stateEffect ->
                async { stateEffect.runState(it).toValidated() }
            }.awaitAll()

            it to res.traverse(semigroup) {
                it.map { (_, it) -> it }
            }.bind()
        }
    }
}

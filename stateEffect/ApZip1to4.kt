@file:Suppress("ktlint:filename")

package cz.etn.btip.service.mapping.stateEffect

import arrow.core.Tuple4
import arrow.core.Tuple5
import arrow.typeclasses.Semigroup

/*
 * Ideally, we would define the functions bellow with a context(Semigroup<EE>) receiver. However, as of 1.8, Kotlin
 * causes "java.lang.VerifyError: Bad type on operand stack" errors during runtime if functions with context
 * receivers are inlined. This can be solved by running Java with -Xverify:none, however a quick Google search
 * revealed that it is not recommended to use this setting in production.
 *
 * Careful! When using gradle bootRun, -Xverify:none is added automatically, which means such code will work locally,
 * but fail once deployed.
 */

suspend inline fun <SS, EE, A, B, RR> StateEffect<SS, EE, A>.apZip(
    semigroup: Semigroup<EE>,
    one: StateEffect<SS, EE, B>,
    crossinline transform: suspend (A, B) -> RR
): StateEffect<SS, EE, RR> = apZip(
    semigroup,
    one,
    Unit.just(),
    Unit.just(),
    Unit.just(),
    Unit.just(),
    Unit.just(),
    Unit.just(),
    Unit.just(),
    Unit.just()
) { a, b, _, _, _, _, _, _, _, _ ->
    transform(a, b)
}

suspend fun <SS, EE, A, B> StateEffect<SS, EE, A>.apZip(
    semigroup: Semigroup<EE>,
    one: StateEffect<SS, EE, B>
): StateEffect<SS, EE, Pair<A, B>> =
    apZip(
        semigroup,
        one,
        ::Pair
    )

suspend inline fun <SS, EE, A, B, C, RR> StateEffect<SS, EE, A>.apZip(
    semigroup: Semigroup<EE>,
    one: StateEffect<SS, EE, B>,
    two: StateEffect<SS, EE, C>,
    crossinline transform: suspend (A, B, C) -> RR
): StateEffect<SS, EE, RR> = apZip(
    semigroup,
    one,
    two,
    Unit.just(),
    Unit.just(),
    Unit.just(),
    Unit.just(),
    Unit.just(),
    Unit.just(),
    Unit.just()
) { a, b, c, _, _, _, _, _, _, _ ->
    transform(a, b, c)
}

suspend fun <SS, EE, A, B, C> StateEffect<SS, EE, A>.apZip(
    semigroup: Semigroup<EE>,
    one: StateEffect<SS, EE, B>,
    two: StateEffect<SS, EE, C>,
): StateEffect<SS, EE, Triple<A, B, C>> =
    apZip(
        semigroup,
        one,
        two,
        ::Triple
    )

suspend inline fun <SS, EE, A, B, C, D, RR> StateEffect<SS, EE, A>.apZip(
    semigroup: Semigroup<EE>,
    one: StateEffect<SS, EE, B>,
    two: StateEffect<SS, EE, C>,
    three: StateEffect<SS, EE, D>,
    crossinline transform: suspend (A, B, C, D) -> RR
): StateEffect<SS, EE, RR> = apZip(
    semigroup,
    one,
    two,
    three,
    Unit.just(),
    Unit.just(),
    Unit.just(),
    Unit.just(),
    Unit.just(),
    Unit.just()
) { a, b, c, d, _, _, _, _, _, _ ->
    transform(a, b, c, d)
}

suspend fun <SS, EE, A, B, C, D> StateEffect<SS, EE, A>.apZip(
    semigroup: Semigroup<EE>,
    one: StateEffect<SS, EE, B>,
    two: StateEffect<SS, EE, C>,
    three: StateEffect<SS, EE, D>,
): StateEffect<SS, EE, Tuple4<A, B, C, D>> =
    apZip(
        semigroup,
        one,
        two,
        three,
        ::Tuple4
    )

suspend inline fun <SS, EE, A, B, C, D, E, RR> StateEffect<SS, EE, A>.apZip(
    semigroup: Semigroup<EE>,
    one: StateEffect<SS, EE, B>,
    two: StateEffect<SS, EE, C>,
    three: StateEffect<SS, EE, D>,
    four: StateEffect<SS, EE, E>,
    crossinline transform: suspend (A, B, C, D, E) -> RR
): StateEffect<SS, EE, RR> = apZip(
    semigroup,
    one,
    two,
    three,
    four,
    Unit.just(),
    Unit.just(),
    Unit.just(),
    Unit.just(),
    Unit.just()
) { a, b, c, d, e, _, _, _, _, _ ->
    transform(a, b, c, d, e)
}

suspend fun <SS, EE, A, B, C, D, E> StateEffect<SS, EE, A>.apZip(
    semigroup: Semigroup<EE>,
    one: StateEffect<SS, EE, B>,
    two: StateEffect<SS, EE, C>,
    three: StateEffect<SS, EE, D>,
    four: StateEffect<SS, EE, E>,
): StateEffect<SS, EE, Tuple5<A, B, C, D, E>> =
    apZip(
        semigroup,
        one,
        two,
        three,
        four,
        ::Tuple5
    )

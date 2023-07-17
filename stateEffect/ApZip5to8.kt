@file:Suppress("ktlint:filename")

package cz.etn.btip.service.mapping.stateEffect

import arrow.core.Tuple6
import arrow.core.Tuple7
import arrow.core.Tuple8
import arrow.core.Tuple9
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

suspend inline fun <SS, EE, A, B, C, D, E, F, RR> StateEffect<SS, EE, A>.apZip(
    semigroup: Semigroup<EE>,
    one: StateEffect<SS, EE, B>,
    two: StateEffect<SS, EE, C>,
    three: StateEffect<SS, EE, D>,
    four: StateEffect<SS, EE, E>,
    five: StateEffect<SS, EE, F>,
    crossinline transform: suspend (A, B, C, D, E, F) -> RR
): StateEffect<SS, EE, RR> = apZip(
    semigroup,
    one,
    two,
    three,
    four,
    five,
    Unit.just(),
    Unit.just(),
    Unit.just(),
    Unit.just()
) { a, b, c, d, e, f, _, _, _, _ ->
    transform(a, b, c, d, e, f)
}

suspend fun <SS, EE, A, B, C, D, E, F> StateEffect<SS, EE, A>.apZip(
    semigroup: Semigroup<EE>,
    one: StateEffect<SS, EE, B>,
    two: StateEffect<SS, EE, C>,
    three: StateEffect<SS, EE, D>,
    four: StateEffect<SS, EE, E>,
    five: StateEffect<SS, EE, F>,
): StateEffect<SS, EE, Tuple6<A, B, C, D, E, F>> =
    apZip(
        semigroup,
        one,
        two,
        three,
        four,
        five,
        ::Tuple6
    )

suspend inline fun <SS, EE, A, B, C, D, E, F, G, RR> StateEffect<SS, EE, A>.apZip(
    semigroup: Semigroup<EE>,
    one: StateEffect<SS, EE, B>,
    two: StateEffect<SS, EE, C>,
    three: StateEffect<SS, EE, D>,
    four: StateEffect<SS, EE, E>,
    five: StateEffect<SS, EE, F>,
    six: StateEffect<SS, EE, G>,
    crossinline transform: suspend (A, B, C, D, E, F, G) -> RR
): StateEffect<SS, EE, RR> = apZip(
    semigroup,
    one,
    two,
    three,
    four,
    five,
    six,
    Unit.just(),
    Unit.just(),
    Unit.just()
) { a, b, c, d, e, f, g, _, _, _ ->
    transform(a, b, c, d, e, f, g)
}

suspend fun <SS, EE, A, B, C, D, E, F, G> StateEffect<SS, EE, A>.apZip(
    semigroup: Semigroup<EE>,
    one: StateEffect<SS, EE, B>,
    two: StateEffect<SS, EE, C>,
    three: StateEffect<SS, EE, D>,
    four: StateEffect<SS, EE, E>,
    five: StateEffect<SS, EE, F>,
    six: StateEffect<SS, EE, G>,
): StateEffect<SS, EE, Tuple7<A, B, C, D, E, F, G>> =
    apZip(
        semigroup,
        one,
        two,
        three,
        four,
        five,
        six,
        ::Tuple7
    )

suspend inline fun <SS, EE, A, B, C, D, E, F, G, H, RR> StateEffect<SS, EE, A>.apZip(
    semigroup: Semigroup<EE>,
    one: StateEffect<SS, EE, B>,
    two: StateEffect<SS, EE, C>,
    three: StateEffect<SS, EE, D>,
    four: StateEffect<SS, EE, E>,
    five: StateEffect<SS, EE, F>,
    six: StateEffect<SS, EE, G>,
    seven: StateEffect<SS, EE, H>,
    crossinline transform: suspend (A, B, C, D, E, F, G, H) -> RR
): StateEffect<SS, EE, RR> = apZip(
    semigroup,
    one,
    two,
    three,
    four,
    five,
    six,
    seven,
    Unit.just(),
    Unit.just()
) { a, b, c, d, e, f, g, h, _, _ ->
    transform(a, b, c, d, e, f, g, h)
}

suspend fun <SS, EE, A, B, C, D, E, F, G, H> StateEffect<SS, EE, A>.apZip(
    semigroup: Semigroup<EE>,
    one: StateEffect<SS, EE, B>,
    two: StateEffect<SS, EE, C>,
    three: StateEffect<SS, EE, D>,
    four: StateEffect<SS, EE, E>,
    five: StateEffect<SS, EE, F>,
    six: StateEffect<SS, EE, G>,
    seven: StateEffect<SS, EE, H>,
): StateEffect<SS, EE, Tuple8<A, B, C, D, E, F, G, H>> =
    apZip(
        semigroup,
        one, two, three, four, five, six, seven, ::Tuple8
    )

suspend inline fun <SS, EE, A, B, C, D, E, F, G, H, I, RR> StateEffect<SS, EE, A>.apZip(
    semigroup: Semigroup<EE>,
    one: StateEffect<SS, EE, B>,
    two: StateEffect<SS, EE, C>,
    three: StateEffect<SS, EE, D>,
    four: StateEffect<SS, EE, E>,
    five: StateEffect<SS, EE, F>,
    six: StateEffect<SS, EE, G>,
    seven: StateEffect<SS, EE, H>,
    eight: StateEffect<SS, EE, I>,
    crossinline transform: suspend (A, B, C, D, E, F, G, H, I) -> RR
): StateEffect<SS, EE, RR> = apZip(
    semigroup,
    one,
    two,
    three,
    four,
    five,
    six,
    seven,
    eight,
    Unit.just(),
) { a, b, c, d, e, f, g, h, i, _ ->
    transform(a, b, c, d, e, f, g, h, i)
}

suspend fun <SS, EE, A, B, C, D, E, F, G, H, I> StateEffect<SS, EE, A>.apZip(
    semigroup: Semigroup<EE>,
    one: StateEffect<SS, EE, B>,
    two: StateEffect<SS, EE, C>,
    three: StateEffect<SS, EE, D>,
    four: StateEffect<SS, EE, E>,
    five: StateEffect<SS, EE, F>,
    six: StateEffect<SS, EE, G>,
    seven: StateEffect<SS, EE, H>,
    eight: StateEffect<SS, EE, I>,
): StateEffect<SS, EE, Tuple9<A, B, C, D, E, F, G, H, I>> =
    apZip(
        semigroup,
        one, two, three, four, five, six, seven, eight, ::Tuple9
    )

@file:Suppress("ktlint:filename")

package cz.etn.btip.service.mapping.stateEffect

import arrow.core.Tuple14
import arrow.core.Tuple15
import arrow.core.Tuple16
import arrow.core.Tuple17
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

suspend inline fun <SS, EE, A, B, C, D, E, F, G, H, I, J, K, L, M, N, RR> StateEffect<SS, EE, A>.apZip(
    semigroup: Semigroup<EE>,
    one: StateEffect<SS, EE, B>,
    two: StateEffect<SS, EE, C>,
    three: StateEffect<SS, EE, D>,
    four: StateEffect<SS, EE, E>,
    five: StateEffect<SS, EE, F>,
    six: StateEffect<SS, EE, G>,
    seven: StateEffect<SS, EE, H>,
    eight: StateEffect<SS, EE, I>,
    nine: StateEffect<SS, EE, J>,
    ten: StateEffect<SS, EE, K>,
    eleven: StateEffect<SS, EE, L>,
    twelve: StateEffect<SS, EE, M>,
    thirteen: StateEffect<SS, EE, N>,
    crossinline transform: suspend (A, B, C, D, E, F, G, H, I, J, K, L, M, N) -> RR
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
    nine,
    ten,
    eleven,
    twelve,
    thirteen,
    Unit.just(),
    Unit.just(),
    Unit.just(),
    Unit.just(),
    Unit.just()
) { a, b, c, d, e, f, g, h, i, j, k, l, m, n, _, _, _, _, _ ->
    transform(a, b, c, d, e, f, g, h, i, j, k, l, m, n)
}

suspend fun <SS, EE, A, B, C, D, E, F, G, H, I, J, K, L, M, N> StateEffect<SS, EE, A>.apZip(
    semigroup: Semigroup<EE>,
    one: StateEffect<SS, EE, B>,
    two: StateEffect<SS, EE, C>,
    three: StateEffect<SS, EE, D>,
    four: StateEffect<SS, EE, E>,
    five: StateEffect<SS, EE, F>,
    six: StateEffect<SS, EE, G>,
    seven: StateEffect<SS, EE, H>,
    eight: StateEffect<SS, EE, I>,
    nine: StateEffect<SS, EE, J>,
    ten: StateEffect<SS, EE, K>,
    eleven: StateEffect<SS, EE, L>,
    twelve: StateEffect<SS, EE, M>,
    thirteen: StateEffect<SS, EE, N>,
): StateEffect<SS, EE, Tuple14<A, B, C, D, E, F, G, H, I, J, K, L, M, N>> =
    apZip(
        semigroup,
        one, two, three, four, five, six, seven, eight, nine, ten, eleven, twelve, thirteen, ::Tuple14
    )

suspend inline fun <SS, EE, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, RR> StateEffect<SS, EE, A>.apZip(
    semigroup: Semigroup<EE>,
    one: StateEffect<SS, EE, B>,
    two: StateEffect<SS, EE, C>,
    three: StateEffect<SS, EE, D>,
    four: StateEffect<SS, EE, E>,
    five: StateEffect<SS, EE, F>,
    six: StateEffect<SS, EE, G>,
    seven: StateEffect<SS, EE, H>,
    eight: StateEffect<SS, EE, I>,
    nine: StateEffect<SS, EE, J>,
    ten: StateEffect<SS, EE, K>,
    eleven: StateEffect<SS, EE, L>,
    twelve: StateEffect<SS, EE, M>,
    thirteen: StateEffect<SS, EE, N>,
    fourteen: StateEffect<SS, EE, O>,
    crossinline transform: suspend (A, B, C, D, E, F, G, H, I, J, K, L, M, N, O) -> RR
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
    nine,
    ten,
    eleven,
    twelve,
    thirteen,
    fourteen,
    Unit.just(),
    Unit.just(),
    Unit.just(),
    Unit.just()
) { a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, _, _, _, _ ->
    transform(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o)
}

suspend fun <SS, EE, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O> StateEffect<SS, EE, A>.apZip(
    semigroup: Semigroup<EE>,
    one: StateEffect<SS, EE, B>,
    two: StateEffect<SS, EE, C>,
    three: StateEffect<SS, EE, D>,
    four: StateEffect<SS, EE, E>,
    five: StateEffect<SS, EE, F>,
    six: StateEffect<SS, EE, G>,
    seven: StateEffect<SS, EE, H>,
    eight: StateEffect<SS, EE, I>,
    nine: StateEffect<SS, EE, J>,
    ten: StateEffect<SS, EE, K>,
    eleven: StateEffect<SS, EE, L>,
    twelve: StateEffect<SS, EE, M>,
    thirteen: StateEffect<SS, EE, N>,
    fourteen: StateEffect<SS, EE, O>,
): StateEffect<SS, EE, Tuple15<A, B, C, D, E, F, G, H, I, J, K, L, M, N, O>> =
    apZip(
        semigroup,
        one, two, three, four, five, six, seven, eight, nine, ten, eleven, twelve, thirteen, fourteen,
        ::Tuple15
    )

suspend inline fun <SS, EE, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, RR> StateEffect<SS, EE, A>.apZip(
    semigroup: Semigroup<EE>,
    one: StateEffect<SS, EE, B>,
    two: StateEffect<SS, EE, C>,
    three: StateEffect<SS, EE, D>,
    four: StateEffect<SS, EE, E>,
    five: StateEffect<SS, EE, F>,
    six: StateEffect<SS, EE, G>,
    seven: StateEffect<SS, EE, H>,
    eight: StateEffect<SS, EE, I>,
    nine: StateEffect<SS, EE, J>,
    ten: StateEffect<SS, EE, K>,
    eleven: StateEffect<SS, EE, L>,
    twelve: StateEffect<SS, EE, M>,
    thirteen: StateEffect<SS, EE, N>,
    fourteen: StateEffect<SS, EE, O>,
    fifteen: StateEffect<SS, EE, P>,
    crossinline transform: suspend (A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P) -> RR
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
    nine,
    ten,
    eleven,
    twelve,
    thirteen,
    fourteen,
    fifteen,
    Unit.just(),
    Unit.just(),
    Unit.just()
) { a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, _, _, _ ->
    transform(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p)
}

suspend fun <SS, EE, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P> StateEffect<SS, EE, A>.apZip(
    semigroup: Semigroup<EE>,
    one: StateEffect<SS, EE, B>,
    two: StateEffect<SS, EE, C>,
    three: StateEffect<SS, EE, D>,
    four: StateEffect<SS, EE, E>,
    five: StateEffect<SS, EE, F>,
    six: StateEffect<SS, EE, G>,
    seven: StateEffect<SS, EE, H>,
    eight: StateEffect<SS, EE, I>,
    nine: StateEffect<SS, EE, J>,
    ten: StateEffect<SS, EE, K>,
    eleven: StateEffect<SS, EE, L>,
    twelve: StateEffect<SS, EE, M>,
    thirteen: StateEffect<SS, EE, N>,
    fourteen: StateEffect<SS, EE, O>,
    fifteen: StateEffect<SS, EE, P>,
): StateEffect<SS, EE, Tuple16<A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P>> =
    apZip(
        semigroup,
        one, two, three, four, five, six, seven, eight, nine, ten, eleven, twelve, thirteen, fourteen, fifteen,
        ::Tuple16
    )

suspend inline fun <SS, EE, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, RR> StateEffect<SS, EE, A>.apZip(
    semigroup: Semigroup<EE>,
    one: StateEffect<SS, EE, B>,
    two: StateEffect<SS, EE, C>,
    three: StateEffect<SS, EE, D>,
    four: StateEffect<SS, EE, E>,
    five: StateEffect<SS, EE, F>,
    six: StateEffect<SS, EE, G>,
    seven: StateEffect<SS, EE, H>,
    eight: StateEffect<SS, EE, I>,
    nine: StateEffect<SS, EE, J>,
    ten: StateEffect<SS, EE, K>,
    eleven: StateEffect<SS, EE, L>,
    twelve: StateEffect<SS, EE, M>,
    thirteen: StateEffect<SS, EE, N>,
    fourteen: StateEffect<SS, EE, O>,
    fifteen: StateEffect<SS, EE, P>,
    sixteen: StateEffect<SS, EE, Q>,
    crossinline transform: suspend (A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q) -> RR
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
    nine,
    ten,
    eleven,
    twelve,
    thirteen,
    fourteen,
    fifteen,
    sixteen,
    Unit.just(),
    Unit.just()
) { a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, _, _ ->
    transform(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q)
}

suspend fun <SS, EE, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q> StateEffect<SS, EE, A>.apZip(
    semigroup: Semigroup<EE>,
    one: StateEffect<SS, EE, B>,
    two: StateEffect<SS, EE, C>,
    three: StateEffect<SS, EE, D>,
    four: StateEffect<SS, EE, E>,
    five: StateEffect<SS, EE, F>,
    six: StateEffect<SS, EE, G>,
    seven: StateEffect<SS, EE, H>,
    eight: StateEffect<SS, EE, I>,
    nine: StateEffect<SS, EE, J>,
    ten: StateEffect<SS, EE, K>,
    eleven: StateEffect<SS, EE, L>,
    twelve: StateEffect<SS, EE, M>,
    thirteen: StateEffect<SS, EE, N>,
    fourteen: StateEffect<SS, EE, O>,
    fifteen: StateEffect<SS, EE, P>,
    sixteen: StateEffect<SS, EE, Q>,
): StateEffect<SS, EE, Tuple17<A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q>> =
    apZip(
        semigroup,
        one, two, three, four, five, six, seven, eight, nine, ten, eleven, twelve,
        thirteen, fourteen, fifteen, sixteen, ::Tuple17
    )

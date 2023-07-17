@file:Suppress("ktlint:filename")

package cz.etn.btip.service.mapping.stateEffect

import arrow.core.Tuple10
import arrow.core.Tuple11
import arrow.core.Tuple12
import arrow.core.Tuple13
import arrow.core.continuations.effect
import arrow.core.zip
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

@Suppress("UNCHECKED_CAST")
suspend inline fun <SS, EE, A, B, C, D, E, F, G, H, I, J, RR> StateEffect<SS, EE, A>.apZip(
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
    crossinline transform: suspend (A, B, C, D, E, F, G, H, I, J) -> RR
): StateEffect<SS, EE, RR> = StateEffect {
    effect {
        coroutineScope {
            val res = listOf(
                async { runState(it).toValidated() },
                async { one.runState(it).toValidated() },
                async { two.runState(it).toValidated() },
                async { three.runState(it).toValidated() },
                async { four.runState(it).toValidated() },
                async { five.runState(it).toValidated() },
                async { six.runState(it).toValidated() },
                async { seven.runState(it).toValidated() },
                async { eight.runState(it).toValidated() },
                async { nine.runState(it).toValidated() },
            ).awaitAll()

            res[0].zip(
                semigroup,
                res[1], res[2], res[3], res[4], res[5], res[6], res[7], res[8], res[9],
            ) { (_, a), (_, b), (_, c), (_, d), (_, e), (_, f), (_, g), (_, h), (_, i), (_, j) ->
                it to transform(a as A, b as B, c as C, d as D, e as E, f as F, g as G, h as H, i as I, j as J)
            }.bind()
        }
    }
}

suspend fun <SS, EE, A, B, C, D, E, F, G, H, I, J> StateEffect<SS, EE, A>.apZip(
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
): StateEffect<SS, EE, Tuple10<A, B, C, D, E, F, G, H, I, J>> =
    apZip(
        semigroup,
        one, two, three, four, five, six, seven, eight, nine, ::Tuple10
    )

suspend inline fun <SS, EE, A, B, C, D, E, F, G, H, I, J, K, RR> StateEffect<SS, EE, A>.apZip(
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
    crossinline transform: suspend (A, B, C, D, E, F, G, H, I, J, K) -> RR
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
    Unit.just(),
    Unit.just(),
    Unit.just(),
    Unit.just(),
    Unit.just(),
    Unit.just(),
    Unit.just(),
    Unit.just()
) { a, b, c, d, e, f, g, h, i, j, k, _, _, _, _, _, _, _, _ ->
    transform(a, b, c, d, e, f, g, h, i, j, k)
}

suspend fun <SS, EE, A, B, C, D, E, F, G, H, I, J, K> StateEffect<SS, EE, A>.apZip(
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
): StateEffect<SS, EE, Tuple11<A, B, C, D, E, F, G, H, I, J, K>> =
    apZip(
        semigroup,
        one, two, three, four, five, six, seven, eight, nine, ten, ::Tuple11
    )

suspend inline fun <SS, EE, A, B, C, D, E, F, G, H, I, J, K, L, RR> StateEffect<SS, EE, A>.apZip(
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
    crossinline transform: suspend (A, B, C, D, E, F, G, H, I, J, K, L) -> RR
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
    Unit.just(),
    Unit.just(),
    Unit.just(),
    Unit.just(),
    Unit.just(),
    Unit.just(),
    Unit.just()
) { a, b, c, d, e, f, g, h, i, j, k, l, _, _, _, _, _, _, _ ->
    transform(a, b, c, d, e, f, g, h, i, j, k, l)
}

suspend fun <SS, EE, A, B, C, D, E, F, G, H, I, J, K, L> StateEffect<SS, EE, A>.apZip(
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
): StateEffect<SS, EE, Tuple12<A, B, C, D, E, F, G, H, I, J, K, L>> =
    apZip(
        semigroup,
        one, two, three, four, five, six, seven, eight, nine, ten, eleven, ::Tuple12
    )

suspend inline fun <SS, EE, A, B, C, D, E, F, G, H, I, J, K, L, M, RR> StateEffect<SS, EE, A>.apZip(
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
    crossinline transform: suspend (A, B, C, D, E, F, G, H, I, J, K, L, M) -> RR
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
    Unit.just(),
    Unit.just(),
    Unit.just(),
    Unit.just(),
    Unit.just(),
    Unit.just()
) { a, b, c, d, e, f, g, h, i, j, k, l, m, _, _, _, _, _, _ ->
    transform(a, b, c, d, e, f, g, h, i, j, k, l, m)
}

suspend fun <SS, EE, A, B, C, D, E, F, G, H, I, J, K, L, M> StateEffect<SS, EE, A>.apZip(
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
): StateEffect<SS, EE, Tuple13<A, B, C, D, E, F, G, H, I, J, K, L, M>> =
    apZip(
        semigroup,
        one, two, three, four, five, six, seven, eight, nine, ten, eleven, twelve, ::Tuple13
    )

@file:Suppress("ktlint:filename")

package cz.etn.btip.service.mapping.stateEffect

import arrow.core.Tuple10
import arrow.core.Tuple18
import arrow.core.Tuple19
import arrow.core.Tuple20
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

suspend inline fun <SS, EE, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, RR> StateEffect<SS, EE, A>.apZip(
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
    seventeen: StateEffect<SS, EE, R>,
    crossinline transform: suspend (A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R) -> RR
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
    seventeen,
    Unit.just()
) { a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, _ ->
    transform(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r)
}

suspend fun <SS, EE, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R> StateEffect<SS, EE, A>.apZip(
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
    seventeen: StateEffect<SS, EE, R>,
): StateEffect<SS, EE, Tuple18<A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R>> =
    apZip(
        semigroup,
        one, two, three, four, five, six, seven, eight, nine, ten, eleven, twelve,
        thirteen, fourteen, fifteen, sixteen, seventeen, ::Tuple18
    )

@Suppress("UNCHECKED_CAST")
suspend inline fun <SS, EE, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, RR> StateEffect<SS, EE, A>.apZip(
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
    seventeen: StateEffect<SS, EE, R>,
    eighteen: StateEffect<SS, EE, S>,
    crossinline transform: suspend (A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S) -> RR
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
                async { ten.runState(it).toValidated() },
                async { eleven.runState(it).toValidated() },
                async { twelve.runState(it).toValidated() },
                async { thirteen.runState(it).toValidated() },
                async { fourteen.runState(it).toValidated() },
                async { fifteen.runState(it).toValidated() },
                async { sixteen.runState(it).toValidated() },
                async { seventeen.runState(it).toValidated() },
                async { eighteen.runState(it).toValidated() },
            ).awaitAll()

            res[0].zip(
                semigroup,
                res[1], res[2], res[3], res[4], res[5], res[6], res[7], res[8], res[9],
            ) { (_, a), (_, b), (_, c), (_, d), (_, e), (_, f), (_, g), (_, h), (_, i), (_, j) ->
                Unit to Tuple10(a, b, c, d, e, f, g, h, i, j)
            }.zip(
                semigroup,
                res[10], res[11], res[12], res[13], res[14], res[15], res[16], res[17], res[18],
            ) { (_, tuple), (_, k), (_, l), (_, m), (_, n), (_, o), (_, p), (_, q), (_, r), (_, s) ->
                val (a, b, c, d, e, f, g, h, i, j) = tuple

                it to transform(
                    a as A, b as B, c as C, d as D, e as E, f as F, g as G, h as H, i as I,
                    j as J, k as K, l as L, m as M, n as N, o as O, p as P, q as Q, r as R, s as S
                )
            }.bind()
        }
    }
}

suspend fun <SS, EE, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S> StateEffect<SS, EE, A>.apZip(
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
    seventeen: StateEffect<SS, EE, R>,
    eighteen: StateEffect<SS, EE, S>,
): StateEffect<SS, EE, Tuple19<A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S>> =
    apZip(
        semigroup,
        one, two, three, four, five, six, seven, eight, nine, ten, eleven, twelve,
        thirteen, fourteen, fifteen, sixteen, seventeen, eighteen, ::Tuple19
    )

@Suppress("UNCHECKED_CAST")
suspend inline fun <SS, EE, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, RR> StateEffect<SS, EE, A>.apZip(
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
    seventeen: StateEffect<SS, EE, R>,
    eighteen: StateEffect<SS, EE, S>,
    nineteen: StateEffect<SS, EE, T>,
    crossinline transform: suspend (A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T) -> RR
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
                async { ten.runState(it).toValidated() },
                async { eleven.runState(it).toValidated() },
                async { twelve.runState(it).toValidated() },
                async { thirteen.runState(it).toValidated() },
                async { fourteen.runState(it).toValidated() },
                async { fifteen.runState(it).toValidated() },
                async { sixteen.runState(it).toValidated() },
                async { seventeen.runState(it).toValidated() },
                async { eighteen.runState(it).toValidated() },
                async { nineteen.runState(it).toValidated() },
            ).awaitAll()

            res[0].zip(
                semigroup,
                res[1], res[2], res[3], res[4], res[5], res[6], res[7], res[8], res[9],
            ) { (_, a), (_, b), (_, c), (_, d), (_, e), (_, f), (_, g), (_, h), (_, i), (_, j) ->
                Unit to Tuple10(a, b, c, d, e, f, g, h, i, j)
            }.zip(
                semigroup,
                res[10], res[11], res[12], res[13], res[14], res[15], res[16], res[17], res[18],
            ) { (_, tuple), (_, k), (_, l), (_, m), (_, n), (_, o), (_, p), (_, q), (_, r), (_, s) ->
                Unit to Tuple10(tuple, k, l, m, n, o, p, q, r, s)
            }.zip(semigroup, res[19]) { (_, tuple), (_, t) ->
                val (tuple1, k, l, m, n, o, p, q, r, s) = tuple
                val (a, b, c, d, e, f, g, h, i, j) = tuple1

                it to transform(
                    a as A, b as B, c as C, d as D, e as E, f as F, g as G, h as H, i as I,
                    j as J, k as K, l as L, m as M, n as N, o as O, p as P, q as Q, r as R, s as S, t as T
                )
            }.bind()
        }
    }
}

suspend fun <SS, EE, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T> StateEffect<SS, EE, A>.apZip(
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
    seventeen: StateEffect<SS, EE, R>,
    eighteen: StateEffect<SS, EE, S>,
    nineteen: StateEffect<SS, EE, T>,
): StateEffect<SS, EE, Tuple20<A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T>> =
    apZip(
        semigroup,
        one, two, three, four, five, six, seven, eight, nine, ten, eleven, twelve,
        thirteen, fourteen, fifteen, sixteen, seventeen, eighteen, nineteen, ::Tuple20
    )

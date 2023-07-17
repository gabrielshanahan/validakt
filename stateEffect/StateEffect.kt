package cz.etn.btip.service.mapping.stateEffect

import arrow.core.continuations.Effect
import arrow.core.continuations.EffectScope
import arrow.core.continuations.effect

class StateEffect<S, out E, out A>(
    val runState: suspend (S) -> Effect<E, Pair<S, A>>
)

fun <E, A, B> Effect<E, A>.map(f: (A) -> B): Effect<E, B> = effect { f(bind()) }

suspend fun <S, E, A> StateEffect<S, E, A>.evalState(initState: S): Effect<E, A> =
    runState(initState).map { (_, it) -> it }

fun <T, S, E> T.just(): StateEffect<S, E, T> = StateEffect { effect { it to this@just } }

open class StateEffectScope<S, in E>(
    var state: S,
    private val effectScope: EffectScope<E>
) : EffectScope<E> by effectScope {
    suspend fun <A> StateEffect<S, E, A>.bind(): A = effectScope.run {
        val (s, a) = runState(state).bind()
        state = s
        return a
    }

    suspend fun <A> StateEffect<S, E, A>.peek(): A = effectScope.run {
        val (_, a) = runState(state).bind()
        return a
    }
}

inline fun <S, E, A> stateEffect(
    crossinline block: suspend StateEffectScope<S, E>.() -> A
): StateEffect<S, E, A> = StateEffect { state ->
    effect {
        val scope = StateEffectScope(state, this)
        val result = scope.block()
        scope.state to result
    }
}

package cz.etn.btip.service.mapping

import arrow.core.continuations.Effect
import arrow.core.identity

suspend fun <E, T> Effect<E, T>.orThrow(exc: (E) -> Exception): T = fold(
    { throw exc(it) },
    ::identity
)

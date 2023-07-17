package cz.etn.btip.service.mapping

interface ValidatingMapper<in T, R> {
    suspend fun definition(input: T): Validation<R>
    suspend fun execute(input: T): ValidationResult<R> = definition(input).evaluate()
}

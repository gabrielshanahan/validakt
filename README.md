# Mapping & Validation

This document will explain everything that is necessary to understand how the DSL for mapping and validation works, how
it was built, and why I chose this particular approach. I am aware that it is exceedingly long, but unfortunately, at the
time this was written, the internet was frustratingly sparse on resources one could use to learn about the concepts
described herein without having to know Haskell/Scala, understand FP concepts etc., which means I really do need to
explain everything from scratch. I did my best to be clear and to make no assumptions on knowledge about FP concepts,
Arrow, or anything beyond what is included in [The Kotlin Primer](https://medium.com/the-kotlin-primer/table-of-contents-c52573cfa291).

## Prerequisites
This text assumes you have read the Primer. Specifically, it assumes you understand the following:

* domain errors should be modelled explicitly using separate types, and not exceptions
* functions which produce domain errors should explicitly return these types. Under no circumstances should they throw
  exceptions.
* Different states of objects should be modeled as separate types, e.g. a `Person` vs. a `ValidatedPerson`.

If you need a refresher on the parts of the Primer we're referencing, read the following two articles:

* [The Kotlin Primer - Modeling Illegal States](https://medium.com/the-kotlin-primer/sealed-hierarchies-strongly-typed-illegal-states-3d3c50c9a77b?sk=aff18431d87ecc6d0f9def47cffbdcb6)
* [The Kotlin Primer - Modeling States and Structure](https://medium.com/the-kotlin-primer/sealed-hierarchies-strongly-typed-domain-modeling-c8e2731ea181?sk=0f0a651676b7a5b76be269aab4e02abf)

## Validation vs. Mapping
Before we begin, it is important to realize that mapping and validation should always go hand-in-hand. Except in the most
trivial cases, whenever you're mapping one data structure to another, there will always be conditions that you want to
enforce. Most often, you will want to ensure that a certain property is not null (or empty in some other way, e.g. an
empty string). This is especially relevant in the context of Java-Kotlin interop, because objects coming in from Java
are instances of [platform types](https://kotlinlang.org/docs/java-interop.html#null-safety-and-platform-types), which
means that we get no guarantees about their nullability. Therefore, we need to explicitly check every single property and
verify that they are not null, and it's obvious this won't scale well. Indeed, this fact is one of the main motivations for
creating this framework.

Going the other way, whenever you perform validations, you should always return a new type that reflects the fact that it
contains validated data. You should do this even if the returned data structure is isomorphic to the one being validated,
i.e. contains the same properties. In other words, if you write a function that validates an instance of `Person`, that
function should return a `ValidatedPerson`, even if it contains the same properties.

Given this fact, it becomes clear that validation actually _is_ mapping. The process of taking an input `T`, performing
some validations, and then either transforming it to a type `R` or returning a domain error, is represented by the mapping
`(T) -> Success<R> | Failure<DomainError>`, by which we mean a function that accepts a `T`, and returns either a data
structure representing success (containing the result of the transformation), or a data structure representing failure
(containing the domain error that occurred).

Therefore, from now on, whenever we use the word 'validation', keep in mind we also mean 'mapping' as well.

## Collecting all errors

While representing validation as `(T) -> Success<R> | Failure<DomainError>` makes sense, we could (and should) go further
than that. When performing validation, we don't want to return only the _first_ error that we find. We want to perform as
many of the validations as possible, collect _all_ the errors, and return them as a list.

This fact has some interesting consequences that are best demonstrated on an example.

```kotlin
// We explicitly mark all the properties as nullable, to model the situation where this object comes from Java.
data class Address(val street: String?, val city: String?, val country: String?)
data class Person(val name: String?, val phoneNumber: String?, val address: Address?)
```

We want to validate the following:
* `name` is not null
* `phoneNumber` is not null, has a valid format, and its country extension corresponds to `address.country`
* `address` is not null
* `address.street`, `address.city` and `address.country` are not null

In this scenario, even if the `name` validation fails, we still want to perform validation on `address` and
`phoneNumber`, to return the maximum amount of information about the validity of the input. On the other hand,
if `address` is not valid (i.e. it is `null`), we naturally cannot perform the validation on `phoneNumber`, since
`address.country` doesn't exist. However, once `address` is valid, we can again perform the validations on `street`,
`city` and `country` independently of one another.

Therefore, wanting to collect all possible errors has the following consequences:
1) validation should result in a list of domain errors, i.e. validation should in fact be represented by the mapping
   `(T) -> Success<R> | Failure<List<DomainError>>`.
2) There is a kind of "ordering" defined on validations - some validations can be performed **independently** of one
   another, while others can only be performed if/after one or more other validations have resulted in success - they need
   to be performed **one after the other**. In other words, there is a sort of "dependency tree" between validations.
   Try to notice how it manifests itself in the examples bellow.


Finally, if we take into account that the result of the mapping/validation will almost always get sent back to some
sort of client (usually whoever calls some API, which is usually the FE client) and possibly displayed to a human user,
we need the domain errors to include information on the **path** to the property that caused the error. The client can
parse this information and use it to e.g. highlight the appropriate field in red.


Putting all of the above together, validations should be represented by `(T) -> Success<R> | Failure<List<DomainError<Path>>>`,
where `Path = List<String>` and would contain e.g. `listOf('address', 'street')`.

---
_Note: In general, an error can pertain to a combination of fields. Take a look at the "country extensions corresponds to
`address.country`" validation above. When it fails, it's not that either of these fields alone is wrong, it is their
**combination** that is wrong._

_Currently, our implementation doesn't support errors with multiple paths, because it was not needed, and the FE client is
not prepared to parse such structures. Once the need arises, the implementation will be expanded to cover these situations._
---

Let's implement a naive version of the example described above, without using Arrow or any standardized types and
approaches:

```kotlin
sealed interface Person {
    val name: String?
    val phoneNumber: String?
    val address: Address?
}

sealed interface Address {
    val street: String?
    val city: String?
    val country: String?
}

// Unvalidated and Valid Person/Address hierarchy

data class UnvalidatedAddress(
    override val street: String?,
    override val city: String?,
    override val country: String?
) : Address
data class UnvalidatedPerson(
    override val name: String?,
    override val phoneNumber: String?,
    override val address: UnvalidatedAddress?
) : Person

data class ValidAddress(
    override val street: String,
    override val city: String,
    override val country: String
) : Address

data class ValidPerson(
    override val name: String,
    override val phoneNumber: String,
    override val address: ValidAddress
) : Person

// Types used to represent the process of mapping and validation

typealias PropertyPath = List<String>

interface ValidationError {
    val propertyPath: PropertyPath
}

data class CannotBeEmpty(override val propertyPath: PropertyPath) : ValidationError
data class InvalidPhoneFormat(override val propertyPath: PropertyPath) : ValidationError
data class IncorrectCountryExtension(override val propertyPath: PropertyPath) : ValidationError

sealed interface ValidationResult<out T>
data class Success<T>(val value: T) : ValidationResult<T>
data class Failure(val errors: List<ValidationError>) : ValidationResult<Nothing>

fun validatePerson(person: Person?): ValidationResult<ValidPerson> {
    // Empty path represents the person itself
    val currentPath = emptyList<String>()
    return when (person) {
        null -> Failure(
            listOf(
                CannotBeEmpty(currentPath)
            )
        )
        is ValidPerson -> Success(person)
        is UnvalidatedPerson -> {
            val errors = mutableListOf<ValidationError>()

            if (person.name == null) {
                val currentPath = currentPath + "name"
                errors.add(CannotBeEmpty(currentPath))
            }

            if (person.address == null) {
                val currentPath = currentPath + "address"
                errors.add(CannotBeEmpty(currentPath))
            } else {
                val currentPath = currentPath + "address"
                val address = person.address

                if (address.street == null) {
                    val currentPath = currentPath + "street"
                    errors.add(CannotBeEmpty(currentPath))
                }
                if (address.city == null) {
                    val currentPath = currentPath + "city"
                    errors.add(CannotBeEmpty(currentPath))
                }
                if (address.country == null) {
                    val currentPath = currentPath + "country"
                    errors.add(CannotBeEmpty(currentPath))
                }
            }

            if (person.phoneNumber == null) {
                val currentPath = currentPath + "phoneNumber"
                errors.add(CannotBeEmpty(currentPath))
            } else {
                val currentPath = currentPath + "phoneNumber"
                val phoneNumber = person.phoneNumber
                if (/*Phone number has invalid format*/) {
                    errors.add(InvalidPhoneFormat(currentPath))
                }

                if (person.address?.country != null) {
                    if (/*Country extension does not correspond to country*/) {
                        errors.add(IncorrectCountryExtension(currentPath))
                    }
                }
            }

            if (errors.isEmpty()) {
                Success(
                    ValidPerson(
                        name = person.name!!,
                        phoneNumber = person.phoneNumber!!,
                        address = ValidAddress(
                            street = person.address!!.street!!,
                            city = person.address!!.city!!,
                            country = person.address!!.country!!,
                        )
                    )
                )
            } else {
                Failure(errors)
            }
        }
    }
}
```

Well, that's pretty fuckin' horrible. Notice how much clutter is due to us having to repetativelly check for the nullability
of all the fields, and imagine how much worse this will get in situations where we work with real data structures with
tens of properties nested 3-4 levels deep. Also understand that this will be a recurring problem - whenever we interface
with Java, this is a problem we will have to deal with. Clearly, we need a less verbose way to achieve this.

Thankfully, we can do a lot better, and as a teaser, let's take a look at how we would do the same thing using the DSL
we're going to talk about:

```kotlin
suspend fun validatePerson(input: Person?): ValidationResult<ValidPerson> = validation {
    // 'nn' stands for "Not Null", i.e. must not be null
    val person = input.nn()
    when(person) {
        is ValidPerson -> person
        is UnvalidatedPerson -> {
            val name = validation { person::name.nn() }
            val address = validation {
                val address = person::address.nn()
                val street = validation { address::street.nn() }
                val city = validation { address::city.nn() }
                val country = validation { address::country.nn() }

                street.apZip(Semigroup.nonEmptyList(), city, country) { street, city, country ->
                    ValidAddress(
                        street = street,
                        city = city,
                        country = country
                    )
                }.bind()
            }

            val phoneNumber = validation {
                val phoneNumber = person::phoneNumber.nn()

                val validFormat = validation {
                    if(/*Phone number has invalid format*/) {
                        fail(::InvalidPhoneFormat)
                    }
                }

                val validCountryExtension = validation {
                    val country = address.peek().country
                    if(/*Country extension does not correspond to country*/) {
                        fail(::IncorrectCountryExtension)
                    }
                }

                validFormat.apZip(Semigroup.nonEmptyList(), validCountryExtension) { _, _ ->
                    phoneNumber
                }.bind()
            }

            name.apZip(Semigroup.nonEmptyList(), address, phoneNumber) { name, address, phoneNumber ->
                ValidPerson(
                    name = name,
                    phoneNumber = phoneNumber,
                    address = address
                )
            }.bind()

        }
    }
}.evaluate()
```

Now, naturally, you can't yet understand what's going on, but let's just look at the high level characteristics for now.
You can see that the naive version is nearly 50% longer, requires us to track the path manually, is full of
clutter that distracts from the essence of the validations we're doing, and other things like having to use the `!!`
operator at the end. And while the new version does require some initial learning (`peek()`?
`bind()`? `apZip()`? friggin' `Semigroup`??), you will see that there are only a very few fundamental principles you
need to understand, and you can build literally anything from them.

Before you go on, try to notice how the "dependency tree" we talked about earlier manifests in the code. Notice how
"independent" validations are each wrapped in their own `validation` block and then "zipped" (i.e. combined) together,
while validations that are dependent on a given validation are all grouped together inside a single parent `validation`
block, which starts with the validation we're depending on, which is **not** wrapped in a `validation` block. Also take
a look at the naive example, and notice how this tree manifests there. You're not expected to draw any conclusions or gain
any deep understanding, just notice the patterns and then keep reading. This pattern is important when you start actually
using the framework, so you know how to group validations together based on how they depend on each other.

## Validation DSL Requirements

Let's recap what we want from our DSL:

### Automatic path management
We want it to deal with tracking the paths to the properties automatically. As an immediate consequence, we can see that
we'll need to work with getters (e.g. `person::name`), which are instances of `kotlin.reflect.KCallable`, which defines
a `name` property containing the [name](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.reflect/-k-callable/name.html)
of the property.

#### Path composition/chaining
A key feature of this path-tracking mechanism is that it must be _composable_/_chainable_. To understand what this means,
go back to the example and take a look at the `Address` validator. It's entirely conceivable that we would want to extract
it to a separate function that can be run completely independently from `validatePerson()`. Maybe there's a different
part of the app where we work with addresses, or maybe a `Person` actually has two addresses - a permanent address and
a correspondence address - and we don't want to write the same code twice.
Such an address validator can only track paths relative to the `Address` object passed in (so the path to `street`
would just be e.g. `listOf('street')`), but when we call this validator from a different validator (e.g. from
`validatePerson()`), we then need to be able to prepend the "current path" to whatever the address validator returns
(so the path to `street` would become e.g. `listOf('permanentAddress', 'street')` or
`listOf('correspondenceAddress', 'street')` depending on where it's called from).

If there's a voice inside you exclaiming _'monads, baby!'_, listen to it!

### Composing/chaining validations
We want to be able to _compose/chain_ validations, i.e. use a set of validations to define a new, composite validation,
that performs them **one after another**. What this means is that if one fails, *the whole block (i.e. the composite
validation) immediately returns the failure*, and the rest of the validations are not performed. This is so we can
model validations that depend on one another, e.g. no sense in validating the country extension of `phoneNumber`
when `address`is `null`.

If there's a voice inside you exclaiming _'monads, baby!'_, listen to it!

*Note: The behaviour of "immediately exiting" from a block is often referred to as **short-circuiting** the block. We'll be
using this terminology often.*

### Combining/zipping validations
We want to be able to _combine_ validations, i.e. perform them **independently alongside one another**, and combine
their results. In other words, we want to use a set of validations to define a new, composite validation, which runs all
of them, regardless of whether they fail or not. If one or more of them result in failure, the result of the composite
validation is a failure containing all the individual failures combined, and if all the validations succeed, the result
of the composite validation is also a success. The value inside the successful validation is determined by some
transformation of the values of all the "sub-validations".

If there's a voice inside you exclaiming _'monads, baby!'_, well, it's actually _'applicatives, baby!'_, but close enough!

---
_It turns out that the process described above is actually exactly equivalent to what you already know as `zip`,
defined on `List`._

_Here's a definition that you probably know well, for 3 lists (we assume they're all the same length, for simplicity):_

```kotlin
fun <A, B, C> List<A>.zip(one: List<B>, two: List<C>): List<Triple<A, B, C>> = mutableListOf().apply {
    for (i in indices) {
        add(Triple(get(i), one[i], two[it]))
    }
}
```

_Here, the values are combined into an instance of `Triple`, but it's trivial to generalize this to use any function that's
passed in:_

```kotlin
inline fun <A, B, C, R> List<A>.zip(
    one: List<B>, 
    two: List<C>, 
    f:(A, B, C) -> R = ::Triple
): List<Triple<A, B, C>> = mutableListOf().apply {
    for (i in indices) {
        add(f(get(i), one[i], two[it]))
    }
}
```

_Now, think about what `zip` is doing - a value is extracted from **every** list by **iterating each of them them** (because that's how you extract a value from `List`), a function is applied to these extracted values, and the result is **put into** a new `List`._

_But this is actually the same thing we want to do with our validations! A value is extracted from **every** validation by **running each of them** (because that's how you extract a value from a validation). If all are a success, a function is applied, and the result is **put into** a new (successfull) validation. If any of them are failures, we combine these failures, and the result is **put into** a new (failed) validation._

_That's what those calls to `apZip` in the example above are. The 'ap' prefix stands for 'applicative', and we'll
explain it briefly later on._

_You're probably wondering what the `Semigroup` argument is. It's an object which defines how the errors should
be combined, similarly to how a `Comparator` defines how values should be compared. Again, we'll talk about that more later,
don't worry about it for now._
---

The key difference between composing/chaining and combining/zipping is which of the validations get run: in the case of
composing/chaining, the validations only get run up to the first failure, and then no further validations are run. This works nicelly for modelling **dependent** validations (we don't want to execute validations that depend on a validation that failed). In other words, **composing/chaining behaves in a short-circuiting manner**. In the case of combining/zipping, all validations are always run, regardless of whether they end in failure or not. This works nicelly for modelling **independent** validations (we want to always execute a validation that is independent of other validations, so that we can return the maximum amount of information to the client). In other words, **combining/zipping does not behave in a short-circuiting manner**.

Before we continue, let's split this discussion into to parts:
1) How to effectively implement validations in-and-of themselves, without paths,
2) How to automatically manage paths


## Implementing Validations

### Representing the result of a validation

Here are the things we need to be able to do with the datatype that represents the result of a validation:

* Able to represent a success or a failure
* Able to compose/chain **with** short-circuiting behaviour
* Able to combine/zip **without** short-circuiting behaviour

Seeing "success" and "failure", Kotlin's builtin `Result` immediately comes to mind, and even though it isn't what
we'll be using in the end, it provides a good opportunity to explain many of the things we need to understand, while also
explaining _why_ it's not a good choice for our purposes.

### Validations using `kotlin.Result`

`Result` is obviously able to represent success and failure, however it restrict the type of the failure to be an
instance of `Throwable`. That's a big limitation:

* it really doesn't feel right to model domain errors as exceptions, because we specifically **don't** want to be throwing
  them
* unless special care is taken, whenever an instance of `Exception` is created, the stacktrace is filled, which is a costly
  operation, and one which makes absolutely no sense in the context of domain errors.

Ideally, we would want a datatype that's like `Result`, but allows the "incorrect" (often called **the "left" type**, while
the correct type is usually called **"right"**, as in "correct") type to be whatever we want.

But let's ignore that for the time being, and take a look at our other requirements.

Interestingly, we get composition with short-circuiting behaviour for free, via `runCatching` and `getOrThrow()`.

```kotlin
object CannotBeEmpty : Exception()
// 'nn' stands for "notNull"
fun <T : Any> T?.nn(): T = this ?: throw CannotBeEmpty

val address: Result<ValidAddress> = runCatching {
    val nnAddress = person.address.nn()
    val nnStreet = nnAddress.street.nn() // Will only get executed if person.address is non-null
    val nnCity = nnAddress.city.nn() // Will only get executed if person.address is non-null and address.street is non-null
    val nnCountry = nnAddress.country.nn() // Will only get executed if all of the above are non-null

    // Will only get executed if all of the above are non-null
    ValidAddress(
        street = nnStreet,
        city = nnCity,
        country = nnCountry
    )
}
```

In the above, if all the fields are non-null, `address` contains a successful `Result` with the result of the mapping.
Otherwise, it contains a failed `Result` with the **first** encountered error. In other words, `Result` represents the
result of a validation.

This is actually not bad at all, but still not quite what we want. What we want is to collect the result of the `street`,
`city` and `country` validations and *combine* them together. To understand how we should achieve this, look **carefully**
at the sentence we just wrote:

_"we want to collect the **results** of the `street`, `city` and `country` **validations**..."._

The answer is right there in front of our eyes - to be able to combine them, we need `street`, `city` and `country` to
be the result of validations = instances of `Result`! This is actually a specific instance of something that leads to a
very important point of view, which we'll discuss shortly.

For now, let's make the necessary changes to the code, and introduce some DSL-like verbs:

```kotlin

typealias ValidationResult<T> = Result<T>

object CannotBeEmpty : Exception()

inline fun <T> validation(block: () -> T): ValidationResult<T> = runCatching(block)

fun <T : Any> T?.nn(): T = this ?: throw CannotBeEmpty

fun <A, B, C, R> apZip(
    a: ValidationResult<A>, 
    b: ValidationResult<B>, 
    c: ValidationResult<C>,
    onSuccess: (A, B, C) -> R
): ValidationResult<R> = TODO()

val address = validation {
    val nnAddress: Address = person.address.nn()
    val nnStreet: ValidationResult<String> = validation { nnAddress.street.nn() }
    val nnCity: ValidationResult<String> = validation { nnAddress.city.nn() }
    val nnCountry: ValidationResult<String> = validation { nnAddress.country.nn() }

    apZip(nnStreet, nnCity, nnCountry) { nnStreet, nnCity, nnCountry ->
        ValidAddress(
            street = nnStreet,
            city = nnCity,
            country = nnCountry
        )   
    }
}
```

Now, obviously, we need a reasonable definition of `apZip`, and we'll get to that in a second, but there's another problem
with the code above. Ask yourself: what is the type of the top-level `val address`? If you think its `ValidationResult<ValidAddress>`,
then you are wrong! Look closely - in fact, it's `ValidationResult<ValidationResult<ValidAddress>>`! This is because
`apZip` returns a `ValidationResult` , which makes sense - we're **combining** `ValidationResults`, so the result should
be a `ValidationResult`. When you combine strings, the result is a string, when you combine lists, the result is a list,
when you combine asynchronous operations, the result is an asynchronous operation.

To implement what we need, we need a way to "extract" the value that's "inside" the zipped `ValidationResult`. If it's
a success, we'll, that's easy - we just extract the value. But what should happen if it's a failure? Well, if it's a failure,
the whole parent (i.e. address) validation should fail with the same failures. And seeing as`ValidationResult` is just
`Result`, and `validation` is just `runCatching`, implementing this behaviour is easy - just call `getOrThrow()`!


```kotlin
typealias ValidationResult<T> = Result<T>
inline fun <T> validation(block: () -> T): ValidationResult<T> = runCatching(block)
fun <T> ValidationResult<T>.extract(): T = getOrThrow()
fun <T : Any> T?.nn(): T = this ?: throw SomeException

fun <A, B, C, R> apZip(
    a: ValidationResult<A>, 
    b: ValidationResult<B>, 
    c: ValidationResult<C>,
    onSuccess: (A, B, C) -> R
): ValidationResult<R> = TODO()

val address: ValidationResult<ValidAddress> = validation {
    val nnAddress: Address = person.address.nn()
    val nnStreet: ValidationResult<String> = validation { nnAddress.street.nn() }
    val nnCity: ValidationResult<String> = validation { nnAddress.city.nn() }
    val nnCountry: ValidationResult<String> = validation { nnAddress.country.nn() }

    apZip(nnStreet, nnCity, nnCountry) { nnStreet, nnCity, nnCountry ->
        ValidAddress(
            street = nnStreet,
            city = nnCity,
            country = nnCountry
        )   
    }.extract()
}
```

Notice how we're basically performing the `nnAddress` and the zipped composite validation **one after the other**/**in sequence**.

Let's pause for a moment and emphasize the "extracting a value" point of view. Calling `extract` on a `ValidationResult`
means that we're executing some type of effect/behavior necessary to retrieve the value. In this case, the effect/behavior
is short-circuiting - if the `ValidationResult` is a failure, we kill the parent block, otherwise we return the value.
Notice how this is reflected in the signature of `extract: (ValidationResult<T>) -> T` - this literally represents
"extracting a value from a `ValidationResult`". It's key to understand that extraction means **executing some
effect/behavior, which allows us to access the value**.

At this point, you're probably wondering why we're stressing this part so much. The reason is that viewing things from this
perspective allows you to form a mental framework that is applicable to a *huge* number of scenarios. This is important
enough that we'll devote a whole section to it, because we'll keep using it over and over again.

### Effects/behaviors, and when they are executed

The mental framework is this: when we have a `ValidationResult<T>`, we are in fact working with a value, `T`,
which is wrapped in a container type which represents some type of *effect*/*behavior*/(generally speaking) *code*,
**which has yet to be executed**. To extract the value from the container, we need to execute the effect/behaviour/code
associated with the wrapper. In the case of `ValidationResult`, the effect/behavior we're talking about is
*short-circuiting on failure* - when a validation results in failure, we immediately exit the `validation` block with
some value.

Notice how, on the lines defining `nnStreet/City/Country` above, no short-circuiting happens even when they fail. We're
simply *defining* the validation (i.e. creating an instance of `ValidationResult<T>`), but not extracting the value,
therefore the **effects don't get run**. Even if any of the values are `null`, no short-circuiting happens.

But *inside* each of the nested `validation` blocks it's a different story - we *are* short-circuiting there, but
we're only short-circuiting the inner `validation` block, not the outer one.

After we *define* the three effects, we combine them into a new, composite effect (which is what `apZip`
does). Here, again, **no effect is executed**! We're simply building the object representing the validation.
To actually run it, we need to call `extract`, which either produces a value, or kills the entire `validation` block
with the failure.

Notice how we could rewrite the code for `nnAddress`like this:

```kotlin
 val address = validation {
    val nnAddress: Address = validation { person.address.nn() }.extract()
    // Rest of the code
}
```

We define an validation, and run it immediately, and since we're nesting this validation inside a parent validation, we
can just "cancel out" the calls to `validation` and `extract` and call `nn` directly in the scope of the parent validation.

Also notice how the very first version we wrote could have been written like this:

```kotlin
val address = validation {
    val nnAddress: Address = person.address.nn()
    val nnStreet: ValidationResult<String> = validation { nnAddress.street.nn() }.extract()
    val nnCity: ValidationResult<String> = validation { nnAddress.city.nn() }.extract()
    val nnCountry: ValidationResult<String> = validation { nnAddress.country.nn() }.extract()
    
    // We have no validations here - all of them have already been run - so there's nothing to combine!
    ValidAddress(
        street = nnStreet,
        city = nnCity,
        country = nnCountry
    )
}
```

In the code above, we are immediately running the validations, which means that short-circuiting happens immediately,
and we immediately get back the values, at the cost of not executing the following lines if a validation fails. Since we're immediatelly extracting the values, there's no `ValidationResults` to combine, so no call to `apZip`.

Also notice a small subtlety. In theory, we could have chosen to not call `extract` on the result of  `apZip`, and instead called `extract` on the outer `valdation` block, like this:

```kotlin
val address: ValidationResult<ValidAddress> = validation {
    val nnAddress: Address = person.address.nn()
    val nnStreet: ValidationResult<String> = validation { nnAddress.street.nn() }
    val nnCity: ValidationResult<String> = validation { nnAddress.city.nn() }
    val nnCountry: ValidationResult<String> = validation { nnAddress.country.nn() }

    apZip(nnStreet, nnCity, nnCountry) { nnStreet, nnCity, nnCountry ->
        ValidAddress(
            street = nnStreet,
            city = nnCity,
            country = nnCountry
        )   
    }
}.extract()
```

In other words, we build a `ValidationResult<ValidationResult<ValidAddress>>`, and then call extract,
producing a `ValidationResult<ValidAddress>`. While the types match, the behaviour is different!

To demonstrate, consider the following:

```kotlin
person.address = null
val address: ValidationResult<ValidAddress> = validation {
    val nnAddress: Address = person.address.nn()
    val nnStreet: ValidationResult<String> = validation { nnAddress.street.nn() }
    val nnCity: ValidationResult<String> = validation { nnAddress.city.nn() }
    val nnCountry: ValidationResult<String> = validation { nnAddress.country.nn() }

    apZip(nnStreet, nnCity, nnCountry) { nnStreet, nnCity, nnCountry ->
        ValidAddress(
            street = nnStreet,
            city = nnCity,
            country = nnCountry
        )   
    }.extract()
}

println("Hello!")
```

Versus

```kotlin
person.address = null
val address: ValidationResult<ValidAddress> = validation {
    val nnAddress: Address = person.address.nn()
    val nnStreet: ValidationResult<String> = validation { nnAddress.street.nn() }
    val nnCity: ValidationResult<String> = validation { nnAddress.city.nn() }
    val nnCountry: ValidationResult<String> = validation { nnAddress.country.nn() }

    apZip(nnStreet, nnCity, nnCountry) { nnStreet, nnCity, nnCountry ->
        ValidAddress(
            street = nnStreet,
            city = nnCity,
            country = nnCountry
        )   
    }
}.extract()

println("Hello!")
```

In the first case, the `println` call gets executed, while in the second case, it does not. That's because we're running different validations (the one "inside" vs. the one "outside"), and therefore executing different effects/behaviours, in both cases!

The first code defines a validation which is stored in `address` and which will run two validations one after the other
(`nnAddress` and the result of `apZip`). When the validation stored in `address` is run sometime in the future, it will
evaluate to the value contained in the result of `apZip`. Crucially, **the validation stored in `address` is NOT run
at this point** - it is only defined.

The second code defines a validation which is stored in `address` and which will only run a single validation
(`nnAddress`). This validation does not evaluate to a concrete value, but instead evaluates to a second
validation (the one returned by `apZip`). Crucially, **the `address` validation is then immediately run**,
and since `person.address` is set to `null`, this causes it to short-circuit and the `println` call doesn't
get executed.

---

The reason why we keep ranting about how fundamental this view is is because this same mental framework can be
applied to *many* other data types. As an example, take `Future<T>`, which represents an async operation, and imagine
we have a `future { }` block that allows us to define the function that should be represented as a `Future`:

```kotlin
val myFuture: Future<String> = future {
    val x: Int = executeApiCall() // Call an API
    val y: Double = f(x) // Do stuff
    val z: Double = g(x) // Do other stuff
    formatValues(y, z) // Return formatted value
}
```

If we execute this code, nothing should happen! We need to *run* the future = **extract** the value = run the
*effect*/*behavior* associated with`Future<T>` (in this case asynchronous execution) - before anything happens.

```kotlin
val result: String = future {
    val x: Int = executeApiCall() // Call an API
    val y: Double = f(x) // Do stuff
    val z: Double = g(x) // Do other stuff
    formatValues(y, z) // Return formatted value
}.extract()
```

Now imagine if `f` and `g` were actually async operations themselves, i.e. returned a `Future`. We'll, here's one thing
we could do:

```kotlin
val result: String = future {
    val x: Int = executeApiCall()
    val y: Double = f(x).extract()
    val z: Double = g(x).extract()
    formatValues(y, z)
}.extract()
```

Here, we're executing `f` and `g` in sequence, or **one after the other** (sound familiar)?

Or, since `z` doesn't depend on `y`, we could run them **alongside each other** . However, both depend on `x`, so they
need to be run *after* `x` has finished:

```kotlin
val result: String = future {
    // Here, we could just write 'executeApiCall()`, for exactly the same reasons as in the validation blocks
    val x: Int = future { executeApiCall() }.extract()
    val y: Future<Double> = f(x)
    val z: Future<Double> = g(x)

    apZip(y, z) { extractedY, extractedZ ->
        formatValues(extractedY, extractedZ) // Return formatted value   
    }.extract()
}.extract()
```

Notice how this is *exactly* the same structure of code we wrote in our validation example above. Everything is the
same, including a similar sort of "dependency tree", the fact that the calls to `future` and `extract` cancel out, the
way we combine the `Futures` to produce a new, combined `Future`, which is then immediately run, everything. **This is no
coincidence.**

Let's switch around the types:

```kotlin
val result: Optional<String> = optional {
    val x: Int = optional { doSomeStuff() }.extract()
    val y: Optional<Double> = f(x)
    val z: Optional<Double> = g(x)

    apZip(y, z) { extractedY, extractedZ ->
        formatValues(extractedY, extractedZ) // Return formatted value   
    }.extract()
}
```

Here, `optional` is a builder which allows us to work with `Optional` in the same way. What effect should `extract` have?
Well, if it's called on a `Some<T>`, it should return the `T`, and if it's called on a `None`, it should exit the block
and evaluate the whole thing as `None` - in other words, the effect is short-circuiting again, but with different types
of values.

Here's a final one:

```kotlin
val result: List<String> = list {
    val x: Int = list { doSomeStuff() }.extract()
    val y: List<Double> = f(x)
    val z: List<Double> = g(x)

    zip(y, z) { extractedY, extractedZ ->
        formatValues(extractedY, extractedZ) // Return formatted value   
    }.extract()
}
```

This one is a little different - or is it? `list` is obviously a builder that builds lists. The `doSomeStuff` function
only returns a single value, so we can deduce what `x` is - it's just the result of calling `doSomeStuff()`. Calling `zip`
on `y` and `z` is again clear - it produces a list. The `extract` call cancels out with the topmost `list`, so `result`
contains the zipped list of strings.

Let's make it more interesting:

```kotlin
val result: List<String> = list {
    val x: Int = list { doSomeStuff() }.extract()
    val y: Double = f(x).extract()
    val z: Double = g(x).extract()

    formatValues(y, z)
}
```

Here, instead of zipping the result of `f` and `g`, we're immediately calling `extract`. What does this mean? Well, let's
reason from the properties of `extract` - `extract` **runs the effect/behavior** necessary to extract a value from the
given data-type. Since the data-type in question is `List`, what is the effect? Why, it's iteration of course! You need
to iterate a list to get to the values. In other words, every time `extract` is called, **all the lines bellow it are
executed for each value in the list that is being extracted**, and the result is functionally equivalent something like
this:

```kotlin
val result: List<String> = list {
    val x: Int = doSomeStuff()
    val resultingList = mutableListOf<String>()
    
    for( y in f(x)) {
        for (z in g(x)) {
            resultingList.add(formatValues(y, z))
        }
    }

    resultingList.extract()
}
```

That last line is, in essence, calling "`return <elem>`" for each element in `resultingList`. The topmost `list` then
collects all of them in a `List`, which gets assigned to `result`. In other words, we can indeed "cancel the two out"
and get the same result.

Now, this may look crazy - how on earth would we define `extract` to do this? Surprisingly enough, this
[actually is possible in Kotlin, with some caveats](https://discuss.kotlinlang.org/t/first-class-coroutine-continuations/2529),
but for the purposes of this discussion it's a technical detail. What we want to emphasize is the fact that we can use
the same point of view (extracting a value from a wrapper by executing the behavior/effect associated with the wrapper)
for an incredibly wide spectrum of data structures.

One last thing to notice, which is not really important for our purposes, but you will encounter it often on the
web: In each of the examples above, you could replace all but the last call to `extract` by a call to `flatMap`. The last
call to `extract` needs to be replaced by a call to `map`.

Here are the type signatures of `flatMap` for the types we used.

* `List<T>.flatMap(f: (T) -> List<R>): List<R>`
* `Optional<T>.flatMap(f: (T) -> Optional<R>): Optional<R>`
* `Future<T>.flatMap(f: (T) -> Future<R>): Future<R>`
* `ValidationResult<T>.flatMap(f: (T) -> ValidationResult<R>): ValidationResult<R>`

And `map`:

* `List<T>.map(f: (T) -> R): List<R>`
* `Optional<T>.map(f: (T) -> R): Optional<R>`
* `Future<T>.map(f: (T) -> R): Future<R>`
* `ValidationResult<T>.map(f: (T) -> R): ValidationResult<R>`

```kotlin
val result: List<String> = listOf(doSomeStuff()).flatMap {
    f(x).flatMap { y ->
        g(x).map { z ->
            formatValues(y, z)
        }
    }
}

val result: Optional<String> = Optional.of(doSomeStuff()).flatMap {
    f(x).flatMap { y ->
        g(x).map { z ->
            formatValues(y, z)
        }
    }
}

val result: Future<String> = future { executeApiCall() }.flatMap {
    f(x).flatMap { y ->
        g(x).map { z ->
            formatValues(y, z)
        }
    }
}
```

...and so on.

This pattern holds regardless of the data type, and this is exactly why
[for-comprehensions](https://docs.scala-lang.org/tour/for-comprehensions.html) exist in Scala.

Hopefully, you're convinced that this is an incredibly powerful way of looking at things, and we will use exactly the
same thinking and vocabulary continuously from now on.

### `Result` - wrapping up

The only remaining thing is that we still haven't implemented `apZip`. The reason why we're putting it off is that this is
the place where `Result` really stops shining. Since its left value is a `Throwable`, it starts being really inconvenient
once you start thinking about combining `Throwables`. You would need to define some sort of `CompositeThrowable`
which contains a list of `Throwables`, and that's just messy.

```kotlin
data class CompositeThrowable(val throwables: List<Throwable>): Throwable()

fun <A, B, C, R> zip(
    a: ValidationResult<A>,
    b: ValidationResult<B>,
    c: ValidationResult<C>,
    onSuccess: (A, B, C) -> R
): ValidationResult<R> {
    val throwables = mutableListOf<Throwable>().apply {
        a.exceptionOrNull()?.let(::add)
        b.exceptionOrNull()?.let(::add)
        c.exceptionOrNull()?.let(::add)
    }

    return if(throwables.isEmpty()) {
        Result.success(onSuccess(a.getOrNull()!!, b.getOrNull()!!, c.getOrNull()!!))
    } else {
        Result.failure(CompositeThrowable(throwables))
    }
}
```

There are other ways this function could be implemented, and which one you pick doesn't really matter, the important
part is the *contract* it implements: it returns a new *ValidationResult*, which evaluates the three `ValidationResults`
independently and combines their results. Crucially, calling `zip` **does not** cause short-circuiting in the block it is
called in! It simply returns a new `ValidationResult`, which must be run (i.e. `extract` called) for anything to happen.

While it may seem we actually implemented everything we needed using `Result`, it's obvious that the left type is `Throwable`
is a problem, and we'll see there are other, less obvious problems as well. Thankfully, `Arrow` provides us with better
alternatives, but before we talk about those, we need to talk about how short-circuiting and error combining is
done in Arrow.

### Short-circuiting in Arrow and the `Effect` interface

We just finished a lengthy exposition where we demonstrated how short-circuiting behavior is something that is naturally
associated with`kotlin.Result<T>`, and is implemented via the `runCatching` and `getOrThrow` functions. However, there
are many other types that lend themselves well to the concept of short-circuiting. We already gave the example of Optional,
where we want to short-circuit whenever we call `extract` on a `None`. Nullable types are another example, where we want
to short-circuit whenever we call `extract` on a `null`. And once we get to know the `Either` and `Validated` types from
Arrow, we'll see that they are examples of the same, and that there are even others still. So Arrow generalized this
behaviour, and represented it by a separate type called an `Effect`.

Before we go on, we need to be very careful about nomenclature, because we just spent a long time using the word 'effect'
in a slightly different context. Up until now, when we used the word 'effect' (or 'behaviour'), we were talking about
any type of code that needed to be executed whenever we "extracted" a value from a wrapper type. Most of the time, this
effect/behaviour was short-circuiting, but not exclusivelly. For example, we gave the example of `Future<T>`, where
the "effect" was asynchronous execution, and `List<T>`, where the "effect" was iteration.

The `Effect` *type* from Arrow is **not** a generalization of "a wrapper type having some sort of effect/behavior
associated with value extraction". It is a generalization of "wrapper types having **short-circuiting** associated with
value extraction". So `Result` is "compatible" with an `Effect`, and so are `Optional`, nullable types, the `Either`
and `Validated` types we mentioned earlier, and some others. But `Future<T>` has nothing to do with `Effect`. Neither does
`List<T>`.

So when we talk about an "effect", we're talking about some sort of behavior that happens when a value is extracted from a
wrapper  type. This behavior could be short-circuiting, and it could be something else. But when we talk about
`Effect<R, A>`,  the type, we're talking about a specific wrapper type whose "effect" = "behaviour" is short-circuiting.
Specifically, it represents an operation that either produces a value of type `A`, or short circuits with a value of type
`R`.

The reason `Effect` exists is that it provides a convenient and unified DSL which allows you to work with all
short-circuiting types in the same way, convert between them, and also allows you to extend this DSL to work for your own
types (which is what we'll end up doing later on).

Let's take a look at how this works by analyzing the equivalent "DSL" for `Result`. Here's the general way we build a
`Result`:

```kotlin
val myResult: Result<String> = runCatching {
    // Do some stuff that can throw
    val x: Int = fThatThrows()
    // Do some other stuff that returns a Result
    val y: String = gThatReturnsResult(x).getOrThrow()
    
    // Explicitly do some stuff that can throw
    if(y == "") {
        throw CannotBeEmptyException
    }
    
    y
}
```

Here are the key things we need:

* a function (`runCatching`) that delimits the scope in which the short-circuiting happens, and contains the process
  used to build the `Result`
  * When we short-circuit, this is the block we "exit"
  * At the end, we return the success value the `Result` should contain.
* a function (`getOrThrow`) that attempts to extract a value, short-circuiting if necessary
  * In reality, this function should only exist "inside" the `runCatching` block, because that's the only place where
    short-circuiting makes sense. However, `Result` uses exceptions to implement short-circuiting, and there is no
    limitation to where exceptions can be thrown, so this doesn't really apply in this particular implementation.
* a mechanism (throwing exceptions) through which we can explicitly initiate short-circuiting

Building instances of `Effect` is practically identical in Arrow:

* the function used to delimit the scope of the effect, and build its value, is `effect`
  * `effect` accepts a lambda parameter with a `EffectScope` receiver. `EffectScope` is where the functions such as `bind`
    and `shift` (introduced bellow) are defined
* the function that attempts to extract a value is `bind`. `bind` is defined for all the "short-circuiting" types we
  mentioned, i.e. we have `T?.bind()`, `Result<T>.bind()`, `Option<T>.bind()`, `Either<L, R>.bind()` and so on
  * the reason for this name is that 'bind', or 'monadic binding', is a standardized term used in the context of monads
    (which is what we're secretly talking about this whole time). See e.g.
    [the wiki article on monads](https://en.wikipedia.org/wiki/Monad_(functional_programming)#Overview)
* the mechanism through which we can explicitly initiate short-circuiting is implemented using the `shift` function. So
  e.g. calling `shift("abc")` short-circuits the block with the value `"abc"`.
  * Again, 'shift' is a standardized term used in context of continuations. See e.g. the [wiki article on delimited
    continuations](https://en.wikipedia.org/wiki/Delimited_continuation#Examples). `shift` is sometimes referred to as
    "the functional `throw`", the crucial difference being that `shift` is always limited by some scope (cannot be called
    from anywhere, and can only short-circuit up to a certain point and not beyond). In other words, **`shift` is a `throw`
    that doesn't leak.**

It's not an ideal example, but here's how the example above could be rewritten as an effect.

```kotlin
val myEffect: Effect<Throwable, String> = effect { 
    // In this block, 'this' has type EffectScope
    
    // Effects don't handle exceptions, so we have to handle it ourselves. We could either use try/catch, 
    // but since Effects know how to work with Result, we'll transform the exception into a Result using 
    // runCatching and then extract it on the next line using bind
    val resX: Result<Int> = runCatching { fThatThrows() }

    // bind() for Result accepts a function that transforms the Throwable inside the Result into a
    // different data type. Remember, in general, Effect doesn't limit the left type, so in general, 
    // we need to map the Throwable inside the Result to whatever the left type is. However, in our
    // case, we actually did specify the left type of myEffect as Throwable, so we just use ::identity
    val x: Int = resX.bind(::identity)
  
    // Do some other stuff that returns a Result
    val y: String = gThatReturnsResult(x).bind(::identity)
    
    // Explicitly fail with some Throwable
    if(y == "") {
        shift(CannotBeEmptyException)
    }
    
    y
}

// The above code is only actually run when this line is executed
val myResult: Result<String> = myEffect.toResult()
```

While this particular example looks more cumbersome when written as an `Effect`, it is actually better in general:

* It avoids using exceptions, and instead uses a call to `shift`, which doesn't leak + is only defined on `EffectScope`
  (so we're explicit about if, when and where we allow short-circuiting)
  * Since `shift` is just a function call, we can `shift` with any type, not just a `Throwable`
* Can use any combination of short-circuiting types in the same effect definition
* Can be converted into any short-circuiting type
* `Effect` definition is not run immediately - it is only run on conversion to some specific short-circuiting type

That last one is particularly important, because it allows us to really think about short-circuiting types using the
"running some effect/behavior when extracting a value" point of view. Here's why:

```kotlin

val someValidation = validation {
    // Here, someOtherValidation is only defined. extract() is not called
    val someOtherValidation = validation { 
        println("Hello")
        // Do something else
    }
    
    throw SomeException
    
    someOtherValidation.extract()
}.extract()

```

According to our point of view, in the example above, the `println` should not get executed - after all, we're simply
defining a validation, but not running it. It should only get executed when we attempt to `extract` its result, and that
never happens, because we short-circuit the parent validation by throwing `SomeException` (only possible in our `Result`
based implementation).

But when `validation` is implemented with `runCatching`/`getOrThrow`, the `println` _does_ get executed - `validation`
is just an alias for `runCatching`, which in turn is just an alias for `try/catch`, so everything between the `{` and `}`
gets executed immediately.

Let's look at a simple example to see why this is problematic. Imagine if, for instance, we were migrating between two
databases and due to various factors, the data was only partially migrated. This means that we would need to first try
querying the first database, and if the record was not found, query the second database, and then map the result.
Here's how that could be done:

```kotlin
fun <T> firstValidOf(vararg validations: ValidationResult<T>): ValidationResult<T> = TODO()

fun mapRecord1(record1: Record1): ValidationResult<MappedRecord> = TODO()
fun mapRecord2(record2: Record2): ValidationResult<MappedRecord> = TODO()

val record = firstValidOf(
    validation { 
        val record = db1.executeQuery()
        mapRecord1(record).extract()
    },
    validation {
        val record = db2.executeQuery()
        mapRecord2(record).extract()
    },
)
```

Glossing over the implementations of `firstValid` and `mapRecord`, we can see the problem - if `validation` blocks were
executed immediately, _both_ databases would be called, even if `db1` returned the record, and that's obviously not
what we want. What we want is the validations to be executed one-by-one until a valid one is found, and then the following
ones shouldn't be executed. This is a simple example, but it's easy to see how this could accumulate into a big problem.

However, if we implement `validation` using `effect`, then we don't have that problem - we can choose when the resulting
`Effect` is run, and therefore implement `firstValidOf` to try one effect at a time, and only run the next one if the one it tried failed.

To recap:

* the `Effect<R, A>` interface represents a short-circuiting operation that can either succeed with a valid of type `A`,
  or short-circuit, i.e. exist immediately, with a value of type `R`.
* To create an `Effect`, we use the `effect(f: EffectScope<R>.() -> A)` builder function
* `EffectScope` defines functions that we can use to a) extract values from other short-circuiting types, and b) manually
  initiate short-circuiting.
  * The former is `bind()`, which is defined as an extension function on all short-circuiting types. Depending on
    the specific type, it might accept a parameter that converts the "failure" value of the given type into an `R`. This
    is the case for e.g. `Result`, `Option` and nullable types
  * The latter is `shift(value: R)`
* When an `Effect` is created, it is not immediately run. It is only run once we either convert it into a specific
  short-circuiting type (such as a `Result`), or `bind` is called on it in the context of an `EffectScope`, usually in
  another `effect` block.

### Combining/Zipping in Arrow

In the previous section, we learned how to run the effects of `Effect` = extract values from them, which is how we chain
`Effects`one-after-the-other - we just call `bind` one-after-the-other. However, we have yet to talk about _combining_
effects, i.e. running them independently and combining the results.
In Arrow, this is actually extremely easy - practically every Arrow datatype defines a `zip` function. However, it's
important to understand that the way `zip` is implemented **differs between types**. Some types implement `zip` by
returning the _first_ error that is encountered (so basically what we talked about until now), while other types
_accumulate/combine_ the errors, which is the behaviour we want. It is always **technically possible to implement both
variants** for a given data-type, and which choice is made is determined by the intended _meaning_ of the type.

Just so you know, when `zip` is implemented to run effects until the first failure encountered, the implementation is often
labeled as _monadic_, while an implementation that runs all effects and accumulates the errors is often labeled as
_applicative_.

Both _monads_ and _applicatives_ are terms from the FP world, and you don't need to concern yourself with them too much,
it's just for reference if you ever run into them online. However, understand that being an "applicative implementation"
has nothing to do with the fact that there's some sort of accumulation going on. The key feature is the fact that it
**doesn't** stop when it encounters a failure, as opposed to a "monadic implementation", which **does** stop when it
encounters a failure. We could define `zip` to return the first failure, and it wouldn't have any bearing on whether the
implementation was monadic or applicative. The key question that determines this is: if a failure is encountered, are the
rest of the parameters evaluated or not? In an applicative implementation, all the effects are always evaluated, no matter
what. In a monadic implementation, the effects are only evaluated up to the first failure.

The `Effect` interface itself does not define a `zip` method, because as you can see, there are two different sensible
contracts of `zip` that correspond to short-circuiting types. Therefore, we will implement our own (`apZip`), by
translating the `Effect` to a short-circuiting type that implements the applicative `zip` semantics we want, calling its
implementation of `zip`, and then transforming the result back to an `Effect`.

### Fundamental Arrow types - `Either` and `Validated`

There are basically two types that come into play, and we'll mention both just because they're fundamental parts of the
Arrow (and generally FP) ecosystem, and you should know their names. One is `Either<L, R>`, the other is `Validated<I, V>`.

`Either` is a direct generalization of `Result` -`Result<T> = Either<Throwable, T>` (again, notice that `Throwable` is
on the _left_, while `T` is on the _right_, because it is right = correct). `Either<L, R>` is actually a supertype that
has two subtypes: `Right<R> : Either<Nothing, R>`, which denotes success, and `Left<L> : Either<L, Nothing>`, which
denotes failure.

`Validated` is...exactly the same thing. `Validated<I, V>` denotes exactly the same situation, is also a supertype, and
also has two subtypes: `Valid<V> : Validated<Nothing, V>` and `Invalid<I> : Validated<I, Nothing>`.

So what's the difference? Well, it could be said that the only (or at the least the most significant) difference between
the two is precisely the implementation of `zip`. For `Either`, `zip` will produce an `Either` which is a `Right` if all
the zipped parameters are `Right`, and otherwise is equal to the _first_ `Left` encountered. If a failure is encountered,
the **rest of the parameters are not evaluated**.

On the other hand, the implementation of `zip` for `Validated` will produce a `Validated` which is a `Valid` if all
the zipped parameters are `Valid`, and otherwise an `Invalid` containing all the encountered `Invalid` instances combined.
As a necessary consequence, all the parameters are always evaluated.

To facilitate the combining of errors, the implementation of `zip` for `Validated` also requires you to pass in an
instance of `Semigroup`. This interface defines a single method, `T.combine(other: T): T`, and is used by `zip` to
combine the errors.

In practice, in the *vast* majority of cases, what you want as the `Invalid` type in `Validated` is actually a list of
errors. That way, combination makes intuitive sense, and the whole datatype makes sense: `Validated<List<E>, V>` is a
datatype that represents either a valid value `V` (`Valid<V>`) or an invalid list of errors (`Invalid<List<E>>`).

In fact, it goes a little further - Arrow defines a `NonEmptyList`, commonly shortened to `Nel` (in Scala, it's `Nec` for
'non-empty-collection', which is what you will often find on the internet), which represents a list that always has at
least one element. And since, if the value is `Invalid`, there is always at least one error, the most commonly used form
of `Validated` is actually `Validated<Nel<E>, V>`. This is so common that there's even a typealias defined:
`ValidatedNel<E, V> = Validated<Nel<E>, V>` (or `ValidatedNec` in Scala parlance).

All of the above, i.e. `Either`, `Validated`, `Nel`, `ValidatedNel` are incredibly common in FP, and you will encounter
them often when reading about this subject.

To recap:

* `Either` is a datatype that represents success or failure
  * Failure is denoted by `Left`, and success by `Right`
  * `Either` is often referred to as a "fail-fast" or "short-circuiting" datatype, because when combined with
    other `Either` instances using `zip`, it returns the first error and doesn't evaluate the rest of the parameters
* `Validated` is also a datatype that represents success or failure
  * Failure is denoted by `Invalid`, and success by `Valid`
  * Unlike `Either`, the implementation of `zip` accumulates the errors, using a `Semigroup` instance to combine them
  * In the vast majority of cases, a `Nel` is used as the invalid datatype, leading to the abbreviation `ValidatedNel`.

### Implementing an applicative `zip` for `Effect`

From the above, it is obvious that the implementation of `zip` for `Validated` contains the semantics we're interested
in. Without further ado, this is the implementation we'll be using for `Effect` (we named it `apZip` to emphasize its
applicative nature).

```kotlin
suspend fun <R, A, B, T> Effect<R, A>.apZip(
    semigroup: Semigroup<R>,
    one: Effect<R, B>,
    f: (A, B) -> T
): Effect<R, T> = effect {
    toValidated().zip(semigroup, one.toValidated(), f).bind()
}
```

Naturally, we'll also be implementing versions for more than two parameters, but they follow exactly the same pattern -
`Validated` defines `zip` for up to 10 parameters, or something like that.

You probably noticed that we're including the `suspend` keyword, which is a mysterious thing from the realm of coroutines.
Don't worry about it. It needs to be there because Arrow defines almost all its functions as suspend functions. The
reasons for that are complex and not important for our discussion - for more information, you can read
[this](https://arrow-kt.io/docs/effects/io/) or [this](https://stackoverflow.com/questions/70922793/relation-between-arrow-suspend-functions-and-monad-comprehension),
but it probably won't make much sense, because it solves problems that you don't know you have.

### Putting it all together
We're finally ready to rewrite the example we gave at the beginning using what we now learned:

```kotlin
sealed interface ValidationError
object CannotBeEmpty : ValidationError

typealias ValidationErrors = Nel<ValidationError>

class ValidationScope(
    effectScope: EffectScope<ValidationErrors>
) : EffectScope<ValidationErrors> by effectScope {
    suspend fun <T : Any> T?.nn(): T = this ?: shift(nonEmptyListOf(CannotBeEmpty))
}

typealias ValidationResult<T> = Effect<ValidationErrors, T>

inline fun <T> validation(
    crossinline block: suspend ValidationScope.() -> T
): ValidationResult<T> = effect {
    val scope = ValidationScope(this)
    scope.block()
}

suspend fun <R, A, B, C, T> Effect<R, A>.apZip(
    semigroup: Semigroup<R>,
    one: Effect<R, B>,
    two: Effect<R, C>,
    f: (A, B, C) -> T
): Effect<R, T> = effect {
    toValidated().zip(semigroup, one.toValidated(), two.toValidated(), f).bind()
}

val address: ValidationResult<ValidAddress> = validation {
    val nnAddress: Address = person.address.nn()
    val nnStreet: ValidationResult<String> = validation { nnAddress.street.nn() }
    val nnCity: ValidationResult<String> = validation { nnAddress.city.nn() }
    val nnCountry: ValidationResult<String> = validation { nnAddress.country.nn() }

    // `nonEmptyList` is defined on the companion object of Semigroup (i.e. it's a "static function") and 
    // returns an implementation of Semigroup for Nel<T>
    nnStreet.apZip(Semigroup.nonEmptyList(), nnCity, nnCountry) { nnStreet, nnCity, nnCountry ->
        ValidAddress(
            street = nnStreet,
            city = nnCity,
            country = nnCountry
        )
    }.bind()
}
```

## Automatically managing paths

The final piece of the puzzle is to be able to automatically track the paths to the property we're validating, and, if an
error occurs, pass the property path to the error constructor. In other words, as we're traversing the properties and
running checks on them, we need to keep track of **state**. Most importantly, we want to do this in such a way that we
can compose/chain computations that track state - think back to the example we gave with the address validator at the
beginning.

Let's start with the **type** of such a _stateful computation_.

First, it's obvious that, in our situation, the type of the state will be `List<String>` - the list of property names that
lead to the property where the error happened.

Now, since we want to be able to compose/chain these computations, e.g. call the address validator once for `permanentAddress`,
and then for `correspondenceAddress`, and have the property paths prepended correctly, it's obvious that it has to somehow
**accept the current property path**, i.e. the current state. In other words, the address validator represents the path
to `street` as something like `listOf(...<current state>, 'street')`, and then for e.g. `permanentAddress`, the 'current
state' gets passed in as `listOf('permanentAddress')`. The address validator then returns the final path as
`listOf('permanentAddress', 'street')`, which leads us to our second observation - the property path, i.e. the state,
also **needs to be returned**.

And finally, this whole time we've been talking about *computations* that keep track of state - in other words, those
computations are computing something, not just keeping track of state. And the result of that computation obviously
needs to be returned as well.

So, putting it all together: What we need is a type that accepts some state `S`, and produces the updated state `S` and
some value `T`. In our case, `S` will be `List<String>`, since the state is the path to the property we're currently
working with, and `T` will represent the result of the mapping/validation. But for now, let's just work with `S` and `T`.

The key thing to realize is that this data type is a function. It's a function which we feed the current state `S`, and
it produces the output `T`, and the updated state `S`. So when we talk about "computations that track state", we're
actually talking about functions of the form `(S) -> Pair<S, T>` - functions that accept the current state, compute
a value, and produce the updated the state.

Now, here's a different and much more useful way of looking at things: we can also say that the function represents
a computation _that hasn't been executed yet_. It represents a value `T` that needs to be _extracted_ by _running_ the
computation. Sound familiar? That's because it's exactly the same thing we've been talking about up until now, and as
you'll see, the similarities don't end here.

So, without further ado, the datatype we'll be working with is this:
```kotlin
class State<S, out T>(
    val runState: (S) -> Pair<S, T>
)
```

Just so you know, this class (along with the implementations of `bind` we'll introduce shortly) is normally called the
state monad.

Now, when we talked about `Effects` and the other types, there was always a nice DSL that allowed us to
compose/chain them together. Basically we had some sort of builder function (`runCatching`, `validation`, `effect`, ...)
that we used to build a new instance of the given type (`Result`, `ValidationResult`, `Effect`, ...). In the scope of
this builder, we had one very important function available - `bind` - which allowed us to "extract" values from other
instances of the same datatype by running the corresponding effects/behaviours.  So can we define something similar for
`State`?

Remember, `bind` extracts the value by executing the associated effects/behaviors. In the case of `State`, the effect
is updating state. So from this, it's obvious that the `StateScope` in which we'll define `bind` must have a `var state: S`
property, which will contain the current state.

With that in mind, defining `bind` is easy:

```kotlin
class StateScope<S>(
    var state: S,
) {
    fun <T> State<S, T>.bind(): T {
        val (s, t) = runState(state)
        state = s
        return t
    }
}
```

Now let's define the builder function, `state`. This function returns a `State` instance (same as `runCatching` returns
an instance of `Result`, `effect` returns an instance of `Effect`, etc.). The "contents" of the `State` instance are
defined by the passed in `block`, which is run on an instance of `StateScope` (so we have `bind` available inside it).

Putting all that together, here's what we get:

```kotlin
inline fun <S, T> state(
  crossinline block: StateScope<S>.() -> T
): State<S, T> = State { s ->
    val scope = StateScope(s)
    val result = scope.block() // block contains calls to bind, which updates the state on the scope
    scope.state to result
}
```

And that's it! Let's take it for a spin to get a feeling for what it can do by writting a simple program that allows
working with strings while tracking their length:

```kotlin
val hello: State<Int, String> = State {
    val str = "hello"
    it + str.length to str
}

val space: State<Int, String> = State {
    val str = " "
    it + str.length to str
}

val world: State<Int, String> = State {
    val str = "world"
    it + str.length to str
}

val helloWord: State<Int, String> = state {
    hello.bind() + space.bind() + world.bind()
}

// Run the State instance with 0 as the initial state,
// i.e. initially, the string length is 0
helloWord.runState(0) // (11, "hello world)
```

Cool! But that's a lot of repetition. There's an obvious pattern there, and it makes sense - in all three scenarios,
what we're really doing is taking a "simple" value (in this case, a string) and creating an instance of `State` that
contains _just_ that value. Such an operation is very common, and is often called `just` (other names include `return`,
which comes from Haskell and is confusing in the context of imperative languages, and `pure`).

```kotlin
fun String.just(): State<Int, String> = State { 
    it + length to this
}

val hello = "hello".just()
val space = " ".just()
val world = "world".just()

val helloWord: State<Int, String> = state {
    hello.bind() + space.bind() + world.bind()
}

helloWord.runState(0) // (11, "hello world)
```

That was a trivial example - time to get a little more serious, and implement our path tracking mechanism.

As we said at the very beginning, the key here is to work with getters (e.g. `address::street`) which are instances of
`kotlin.reflect.KCallable`, which defines a `name` property containing the
[name](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.reflect/-k-callable/name.html) of the property. So all we have
to do is add a new function defined on `KCallable`, which updates the state with the name of the getter, and then calls
it. We'll call this function `it`:

```kotlin
typealias PropertyPath = List<String>
typealias PathTracked<T> = State<PropertyPath, T>

class PathTrackingScope(
    state: PropertyPath
): StateScope<PropertyPath>(state) {

    // We could also implement this as a State instance and 
    // immediatelly call bind on it, but there's really no
    // point
    fun <T> KCallable<T>.it(): T {
        state += name
        return call()
    }
}

inline fun <T> track(
    crossinline block: PathTrackingScope.() -> T
): PathTracked<T> = PathTracked { s ->
    val scope = PathTrackingScope(s)
    val result = scope.block()
    scope.state to result
}

// Non-nullable for demonstration purposes
data class Address(val street: String, val city: String, val country: String)
data class Person(val name: String, val phoneNumber: String, val address: Address)

val person = Person(
    "Gabriel Shanahan",
    "+420123456789",
    Address(
        "Some Street",
        "A City",
        "Random Country"
    )
)

track { person::address.it()::street.it() }
    .runState(emptyList()) // ([address, street], Some Street)
```

And that's it! All that's left is to combine it with the validation framework we designed earlier, and we're done!


## Putting it all together - StateEffect
To combine `State` with our validation framework, we need to change the type of `State` to reflect the fact that it will
be returning a `ValidationResult<T> = Effect<ValidationErrors, T>`. More generally, we need our `State` to return an
`Effect<E, T>`.

Our first instinct would probably be to do this:

```kotlin
class StateEffect<S, E, T>(
    val runState: (S) -> Pair<S, Effect<E, T>>
)
```

However, that would be wrong. In this implementation, we're saying that we **always** update the path, because we're always
returning it, regardless of whether we encountered an error in the validation or not. That's a problem, because there's
no way to consistently chose a sensible path when the `Effect` fails. If we only had a single error, then it could make
sense to set the path to the place where the error occurred. But what if `Effect` results in multiple errors at
different places?

Also (as you've probably come to expect), `Effect` isn't the only thing we might want to combine with `State`. Maybe we'd
like a different data type with a different associated effect/behaviour, like `Future`. In this situation, the updated
state might be retrieved e.g. from an API call, and in such a situation there really is nothing meaningful you could
return if the API call failed.

This is the correct type signature:

```kotlin
class StateEffect<S, E, T>(
    val runState: suspend (S) -> Effect<E, Pair<S, T>>
)
```

In other words, an operation that uses the current state to produce an `Effect`, which either fails with `E` or
succeeds with a value and the updated state. The `suspend` keyword is added so we can use other Arrow functions.

The rest of the classes (e.g. the `ValidationScope` class) are just direct combinations of what we used before, and, 
if you've made it this far, you should have no problem understanding their implementations and usage in the example at
the beginning. 

The only thing from the example at the beginning that wasn't explained was `peek`, which is very similar to `bind`. Here
are both implementations for `StateEffect`:

```kotlin
open class StateEffectScope<S, E>(
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
```

As you can see, `peek` does the same thing as `bind`, except it doesn't update the state. This is useful for situations
where we're validating a field X, and we need to access the (validated) value of field Y, but don't want to modify the
path, since Y is somewhere completely different. See the example at the very beginning to understand when it comes in
handy.

## Future work
As we've said before, one thing that the current implementation can't do is represent errors that pertain to multiple
paths. A good example is the "invalid country extension" from our example - that error pertains both to the country and
the phone number. Neither of these fields alone are wrong, it is their *combination* that is wrong.


## Further reading
* [Error handling tutorial for Arrow](https://arrow-kt.io/docs/patterns/error_handling/)
* [Arrow documentation of Either](https://arrow-kt.io/docs/apidocs/arrow-core/arrow.core/-either/)
* [Arrow documentation of Either](https://arrow-kt.io/docs/apidocs/arrow-core/arrow.core/-either/)
* [Scala documentation of Either](https://typelevel.org/cats/datatypes/either.html)
* [Scala documentation of Validated](https://typelevel.org/cats/datatypes/validated.html)
* [Scala documentation of State](https://typelevel.org/cats/datatypes/state.html)
* [Scala documentation of StateT](https://typelevel.org/cats/datatypes/statet.html)
* [Haskell documentation of State & StateT](https://hackage.haskell.org/package/mtl-2.3.1/docs/Control-Monad-State-Lazy.html)
* [Tutorial on domain modelling](https://www.47deg.com/blog/functional-domain-modeling/)
* [Tutorial on simple validation using Arrow](https://www.47deg.com/blog/functional-domain-modeling-part-2/)
* [Arrow documentation of Effect](https://arrow-kt.io/docs/apidocs/arrow-core/arrow.core.continuations/-effect/)
* [Continuation monad in Kotlin](https://nomisrev.github.io/continuation-monad-in-kotlin/)

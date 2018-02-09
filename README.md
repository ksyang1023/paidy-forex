# Paidy Forex



## Overview
The basic idea behind the implementation is to cache the exchange rates pulled from 1Forge API in memory. These rates are kept for a given amount of time and new exchange ratest are pulled after it passes. This mechanism allows for managing the total amount of requests not going over the daily quota (implemented [here](#LINK)). As a side effect, it also improves the response latency of our service.

The downside is that the exchange rates can (and probably will) diverge from the cached values while they are being kept in memory. As such, this is not ideal for use cases where high temporal precision is required. The proper way how to solve such a problem would obviously be to upgrade the 1Forge plan. Alternatively, a considerably less polite way how to circumvent the limits is to create multiple indenpendent free-tier accounts and rotate their API keys (e.g. round robin) when sending the requests to 1Forge and thus "increase" the quotas. Though, this will probably be in conflict with T&C of 1Forge so it's not recommended as it might lead to a ban, it is _an_ option, though...

The live interpreter uses [sttp](https://github.com/softwaremill/sttp) as the http client, which provides an integration with monix tasks (used by the skeleton implementation).

I've also introduced [enumeratum](https://github.com/lloydmeta/enumeratum) which provides a convenient way to extend the functionality over currencies (Enum-like functionality). This is useful in the context of `Currency`s as these are used as a whitelist of _supported_ currencies. Adding a new Currency case object will automatically translate to obtaining its exchange rates when populating the cache.

Finally, I've also extended the [`Error`](#LINK) types to account for more specific error cases and failures. These also translate into more specific HTTP error codes 

## What's missing/left to do?
First and foremost, tests. Both unit- and integration tests. For the sake of time I haven't ventured into setting up the entire testing environment but I would implement those using the scalatest + scalacheck combo. If interested, feel free to check out my other projects using it (such as here: [here](https://github.com/thenobody/clearscore-creditcards/blob/master/credit-cards-service/src/test/scala/net/thenobody/clearscore/creditcards/service/route/RootRouteTest.scala)).

Additionally, structure of the interpreter code (such as get and getAll) could use some refactoring into a bit more reusable code (would be useful for example when unit testing).

## Running
The service can be started with the default configuration [reference.conf] (with 2-minute cache refresh interval) by:

	sbt run

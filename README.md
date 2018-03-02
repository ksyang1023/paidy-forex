# Paidy Forex

Table of Contents
=================

* [Overview](#overview)
* [What's missing/left to do?](#whats-missingleft-to-do)
* [Running](#running)


## Overview
The basic idea behind the implementation is to cache the exchange rates pulled from 1Forge API in memory. These rates are kept for a given amount of time and new exchange ratest are pulled only after it passes. This mechanism allows for keeping the total amount of requests not going over the daily quota (implemented [here](https://github.com/thenobody/paidy-forex/blob/master/src/main/scala/forex/services/oneforge/Interpreters.scala#L77-L97)). At the same time, it also improves the response latency of our service.

The downside is that the exchange rates can (and probably will) diverge from the cached values while they are being kept in memory. As such, this is not ideal for use cases where high temporal precision is required. The proper way how to solve such a problem would obviously be to upgrade the 1Forge plan. 

Alternatively, a considerably less polite way how to circumvent the limits is to create multiple indenpendent free-tier accounts and rotate their API keys (e.g. round robin) when sending the requests to 1Forge and thus "increase" the quotas. Though, this will probably be in conflict with T&C of 1Forge so it's not recommended as it might lead to a ban, it is _an_ option, though...

The live interpreter uses [sttp](https://github.com/softwaremill/sttp) as the http client, which provides an integration with monix tasks (used by the skeleton implementation).

I've also introduced [enumeratum](https://github.com/lloydmeta/enumeratum) which provides a convenient way to achieve Enum-like functionality over `case object`s. This is useful in the context of `Currency`s as these are used as a whitelist of _supported_ currencies. Adding a new Currency case object will automatically translate to obtaining its exchange rates when populating the cache.

Finally, I've also extended the `Error`] types ([here](https://github.com/thenobody/paidy-forex/blob/master/src/main/scala/forex/services/oneforge/Error.scala) and [here](https://github.com/thenobody/paidy-forex/blob/master/src/main/scala/forex/processes/rates/messages.scala)) to account for more specific error cases and failures. These also translate into more specific HTTP error codes 

## Running
The service can be started with the default configuration [reference.conf](https://github.com/thenobody/paidy-forex/blob/master/src/main/resources/reference.conf#L12-L16) (with 2-minute cache refresh interval) by:

	sbt run

## Testing
The test suite can be run by a standard

	sbt test
	
This executes tests for:

* 1forge client ([OneForgeClientImplTest](https://github.com/thenobody/paidy-forex/blob/master/src/test/scala/forex/services/oneforge/client/OneForgeClientImplTest.scala))
* JSON schema validation for 1forge API responses ([QuotaTest](https://github.com/thenobody/paidy-forex/blob/master/src/test/scala/forex/domain/oneforge/QuotaTest.scala) and [QuoteTest](https://github.com/thenobody/paidy-forex/blob/master/src/test/scala/forex/domain/oneforge/QuoteTest.scala))
* 1forge service classes ([OneForgeServiceTest](https://github.com/thenobody/paidy-forex/blob/master/src/test/scala/forex/services/oneforge/OneForgeServiceTest.scala) and [CachedOneForgeServiceTest](https://github.com/thenobody/paidy-forex/blob/master/src/test/scala/forex/services/oneforge/CachedOneForgeServiceTest.scala))
* the API routes ([RoutesTest](https://github.com/thenobody/paidy-forex/blob/master/src/test/scala/forex/interfaces/api/RoutesTest.scala))

The unit tests are implemented using [Scalatest](http://www.scalatest.org/) with property-based tests using [Scalacheck](https://www.scalacheck.org/) (as defined in [ModelFactory](https://github.com/thenobody/paidy-forex/blob/master/src/test/scala/forex/ModelFactory.scala)).
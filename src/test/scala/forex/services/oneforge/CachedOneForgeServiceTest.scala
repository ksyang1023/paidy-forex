package forex.services.oneforge

import forex.ModelFactory
import forex.config.OneForgeConfig
import forex.domain
import forex.domain.oneforge
import forex.main.AppStack
import forex.services.oneforge.Error.ApiError
import forex.services.oneforge.client.OneForgeClient
import monix.cats._
import monix.eval.Task
import monix.execution.Scheduler.Implicits.global
import org.atnos.eff.EffInterpretation
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatest.time._
import org.scalatest.{ EitherValues, FlatSpec, Matchers }

import scala.concurrent.duration._

class CachedOneForgeServiceTest[F]
    extends FlatSpec
    with Matchers
    with GeneratorDrivenPropertyChecks
    with ModelFactory
    with EitherValues
    with ScalaFutures {

  implicit val defaultPatience =
    PatienceConfig(timeout = Span(5, Minutes), interval = Span(500, Millis))

  behavior of classOf[CachedOneForgeService[F]].getSimpleName

  it should "read data from cache after populating" in {
    forAll(aCurrency, aCurrency, aPrice, aTimestamp) {
      case (from, to, price, timestamp) ⇒
        val pair = domain.Rate.Pair(from, to)
        val expResult = domain.Rate(pair, price, timestamp)
        val quote =
          oneforge.Quote(pair.toApiPair.render, price.value, price.value, price.value, timestamp.value.toEpochSecond)

        val client = statefulClient(Right(List(quote)), Left(ApiError("Invalid request")))
        val instance = new CachedOneForgeService[AppStack](OneForgeConfig(1.minute, null), client)
        val result1 = instance.get(pair)

        whenReady(EffInterpretation.detach(result1).runAsync) { result ⇒
          result.right.value shouldEqual expResult
        }

        val result2 = instance.get(pair)
        whenReady(EffInterpretation.detach(result2).runAsync) { result ⇒
          result.right.value shouldEqual expResult
        }
    }
  }

  it should "repopulate cache after a specified TTL elapses" in {
    forAll(aCurrency, aCurrency, aPrice, aTimestamp) {
      case (from, to, price, timestamp) ⇒
        val pair = domain.Rate.Pair(from, to)
        val quote1 =
          oneforge.Quote(pair.toApiPair.render, price.value, price.value, price.value, timestamp.value.toEpochSecond)
        val quote2 = quote1.copy(price = price.value * 10, timestamp = timestamp.value.toEpochSecond + 2)

        val expResult1 = domain.Rate(pair, price, timestamp)
        val expResult2 = domain.Rate(pair, price.copy(price.value * 10), timestamp.copy(timestamp.value.plusSeconds(2)))

        val client = statefulClient(Right(List(quote1)), Right(List(quote2)))

        val instance = new CachedOneForgeService[AppStack](OneForgeConfig(20.millis, null), client)
        val result1 = instance.get(pair)

        whenReady(EffInterpretation.detach(result1).runAsync) { result ⇒
          result.right.value shouldEqual expResult1
        }

        Thread.sleep(1000)
        val result2 = instance.get(pair)
        whenReady(EffInterpretation.detach(result2).runAsync) { result ⇒
          result.right.value shouldEqual expResult2
        }
    }
  }

  def statefulClient(firstResult: Either[Error, List[oneforge.Quote]],
                     subsequentResult: Either[Error, List[oneforge.Quote]]): OneForgeClient[Task] =
    new OneForgeClient[Task] {
      // first call will retrieve this
      val _quotesFirst: Result[List[oneforge.Quote]] = Task.now(firstResult)
      // ... all subsequent calls return this
      val _quotesSubsequent: Result[List[oneforge.Quote]] = Task.now(subsequentResult)

      private var _quotes = _quotesFirst

      def quotes(pairs: List[oneforge.Pair]): Result[List[oneforge.Quote]] = {
        val result = _quotes
        _quotes = _quotesSubsequent
        result
      }

      def symbols: Result[List[oneforge.Pair]] = ???

      def marketStatus: Result[oneforge.MarketStatus] = ???

      def quota: Result[oneforge.Quota] = ???
    }

}

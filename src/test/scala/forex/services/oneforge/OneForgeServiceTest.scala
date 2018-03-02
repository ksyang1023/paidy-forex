package forex.services.oneforge

import forex.ModelFactory
import forex.domain
import forex.domain.oneforge
import forex.main.AppStack
import forex.services.oneforge.Error.{ ApiError, UnsupportedCurrencyPair }
import forex.services.oneforge.client.OneForgeClient
import monix.eval.Task
import org.atnos.eff.EffInterpretation
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatest._
import monix.cats._
import monix.execution.Scheduler.Implicits.global
import org.scalatest.concurrent.ScalaFutures

class OneForgeServiceTest[F]
    extends FlatSpec
    with Matchers
    with GeneratorDrivenPropertyChecks
    with OptionValues
    with EitherValues
    with ModelFactory
    with ScalaFutures {

  behavior of classOf[OneForgeService[F]].getSimpleName

  it should "retrieve a quote for a specified currency pair" in {
    forAll(aCurrency, aCurrency, aPrice, aTimestamp) {
      case (from, to, price, timestamp) ⇒
        val pair = domain.Rate.Pair(from, to)
        val quote =
          oneforge.Quote(pair.toApiPair.render, price.value, price.value, price.value, timestamp.value.toEpochSecond)
        val expResult = domain.Rate(pair, price, timestamp)

        val client = new OneForgeClient[Task] {
          def quotes(pairs: List[oneforge.Pair]): Result[List[oneforge.Quote]] =
            Task.now(Right(List(quote)))

          def symbols: Result[List[oneforge.Pair]] = ???
          def marketStatus: Result[oneforge.MarketStatus] = ???
          def quota: Result[oneforge.Quota] = ???
        }

        val instance = new OneForgeService[AppStack](null, client)
        val result = instance.get(pair)

        whenReady(EffInterpretation.detach(result).runAsync) { result ⇒
          result.right.value shouldEqual expResult
        }
    }
  }

  it should "retrieve quotas" in {
    forAll(anInt, anInt, anInt, anInt) { (num1, num2, num3, num4) ⇒
      val expResult = oneforge.Quota(num1, num2, num3, num4)

      val client = new OneForgeClient[Task] {
        def quotes(pairs: List[oneforge.Pair]): Result[List[oneforge.Quote]] = ???
        def symbols: Result[List[oneforge.Pair]] = ???
        def marketStatus: Result[oneforge.MarketStatus] = ???
        def quota: Result[oneforge.Quota] =
          Task.now(
            Right(oneforge.Quota(num1, num2, num3, num4))
          )
      }

      val instance = new OneForgeService[AppStack](null, client)
      val result = instance.quota

      whenReady(EffInterpretation.detach(result).runAsync) { result ⇒
        result.right.value shouldEqual expResult
      }
    }
  }

  it should "handle unsupported currency requests" in {
    forAll(aCurrency, aCurrency) {
      case (from, to) ⇒
        val pair = domain.Rate.Pair(from, to)

        val client = new OneForgeClient[Task] {
          def quotes(pairs: List[oneforge.Pair]): Result[List[oneforge.Quote]] =
            Task.now(Left(UnsupportedCurrencyPair(pair)))

          def symbols: Result[List[oneforge.Pair]] = ???
          def marketStatus: Result[oneforge.MarketStatus] = ???
          def quota: Result[oneforge.Quota] = ???
        }

        val instance = new OneForgeService[AppStack](null, client)
        val result = instance.get(pair)

        whenReady(EffInterpretation.detach(result).runAsync) { result ⇒
          result.left.value shouldEqual UnsupportedCurrencyPair(pair)
        }
    }
  }

  it should "handle external service errors" in {
    forAll(aCurrency, aCurrency) {
      case (from, to) ⇒
        val pair = domain.Rate.Pair(from, to)

        val client = new OneForgeClient[Task] {
          def quotes(pairs: List[oneforge.Pair]): Result[List[oneforge.Quote]] =
            Task.now(Left(ApiError("Invalid request")))

          def symbols: Result[List[oneforge.Pair]] = ???
          def marketStatus: Result[oneforge.MarketStatus] = ???
          def quota: Result[oneforge.Quota] = ???
        }

        val instance = new OneForgeService[AppStack](null, client)
        val result = instance.get(pair)

        whenReady(EffInterpretation.detach(result).runAsync) { result ⇒
          result.left.value shouldEqual ApiError("Invalid request")
        }
    }
  }
}

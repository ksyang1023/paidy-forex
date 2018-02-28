package forex.services.oneforge

import cats.syntax.either._
import forex.config.OneForgeConfig
import forex.domain._
import monix.eval.Task
import org.atnos.eff._
import org.atnos.eff.addon.monix.task._
import forex.domain.oneforge._
import forex.services.oneforge.client.OneForgeClient

import scala.collection.mutable

object Interpreters {
  def dummy[R](
      implicit
      m1: _task[R]
  ): Algebra[Eff[R, ?]] = new Dummy[R]

  def live[R](config: OneForgeConfig)(
      implicit m1: _task[R],
      client: OneForgeClient[Task]
  ): Algebra[Eff[R, ?]] = new OneForgeService[R](config, client)
//  ): Algebra[Eff[R, ?]] = new CachedOneForgeService[R](config, sttpBackend)
}

class OneForgeService[R: _task] private[oneforge] (oneForgeConfig: OneForgeConfig, client: OneForgeClient[Task])
    extends Algebra[Eff[R, ?]] {
  type Result[T] = Eff[R, Error Either T]

  def quota: Result[Quota] =
    for {
      result ← fromTask(client.quota)
    } yield result

  def get(pair: Rate.Pair): Result[Rate] =
    for {
      result <- fromTask(client.quotes(pair.toApiPair))
    } yield result.map(_.head.toRate)

//  def getAll()
//    for {
//      rates ← getAll
//    } yield
//      rates
//        .map(_.get(pair))
//        .flatMap(Either.fromOption[Error, Rate](_, Error.UnknownCurrencyPair(pair)))

//  def getAll: Result[Map[Rate.Pair, Rate]] = {
//    val pairs = for {
//      from ← Currency.upperCaseNameValuesToMap.keys
//      to ← Currency.upperCaseNameValuesToMap.keys if to != from
//    } yield from + to
//    for {
//      result ← fromTask(
//        sttp
//          .get(uri"${oneForgeConfig.baseUri}/quotes?pairs=${pairs.mkString(",")}&api_key=${oneForgeConfig.apiKey}")
//          .response(asJson[List[Quote]])
//          .send()
//      )
//    } yield
//      result.body
//        .leftMap(Error.QuotesApiError)
//        .flatMap { quotes ⇒
//          quotes
//            .map(_.map(_.toRate).map(rate ⇒ rate.pair → rate).toMap)
//            .leftMap(Error.JsonError)
//        }
//  }
}

//class CachedOneForgeService[R] private[oneforge] (oneForgeConfig: OneForgeConfig,
//                                                  _sttpBackend: SttpBackend[Task, Nothing])(
//    implicit
//    m1: _task[R]
//) extends OneForgeService[R](oneForgeConfig, _sttpBackend) {
//  val cache: mutable.Map[Rate.Pair, Rate] = mutable.Map()
//  val maxAge = oneForgeConfig.maxAge
//
//  override def get(pair: Rate.Pair): Result[Rate] = cache.get(pair) match {
//    case Some(rate) if rate.age() <= maxAge ⇒ Pure(Right(rate))
//    case _ ⇒
//      for {
//        all ← getAll
//      } yield {
//        all.flatMap { pairs ⇒
//          cache ++= pairs
//          Either.fromOption(cache.get(pair), Error.UnknownCurrencyPair(pair))
//        }
//      }
//  }
//}

final class Dummy[R] private[oneforge] (
    implicit
    m1: _task[R]
) extends Algebra[Eff[R, ?]] {
  override def get(
      pair: Rate.Pair
  ): Eff[R, Error Either Rate] =
    for {
      result ← fromTask(Task.now(Rate(pair, Price(BigDecimal(100)), Timestamp.now)))
    } yield Right(result)

  def quota: Eff[R, Either[Error, Quota]] = ???
}

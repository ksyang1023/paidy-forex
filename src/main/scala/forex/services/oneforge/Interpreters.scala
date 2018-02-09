package forex.services.oneforge

import cats.syntax.either._
import forex.config.OneForgeConfig
import forex.domain._
import monix.eval.Task
import org.atnos.eff._
import org.atnos.eff.addon.monix.task._
import com.softwaremill.sttp._
import com.softwaremill.sttp.circe._
import forex.domain.oneforge._

import scala.collection.mutable

object Interpreters {
  def dummy[R](
      implicit
      m1: _task[R]
  ): Algebra[Eff[R, ?]] = new Dummy[R]

  def live[R](config: OneForgeConfig)(
      implicit
      m1: _task[R],
      sttpBackend: SttpBackend[Task, Nothing]
  ): Algebra[Eff[R, ?]] = new CachedOneForgeService[R](config, sttpBackend)
}

class OneForgeService[R] private[oneforge] (oneForgeConfig: OneForgeConfig, _sttpBackend: SttpBackend[Task, Nothing])(
    implicit
    m1: _task[R]
) extends Algebra[Eff[R, ?]] {
  type Result[T] = Eff[R, Error Either T]

  implicit val sttpBackend: SttpBackend[Task, Nothing] = _sttpBackend

  def quota: Result[Quota] =
    for {
      result ← fromTask(
        sttp.get(uri"${oneForgeConfig.baseUri}/quota?api_key=${oneForgeConfig.apiKey}").response(asJson[Quota]).send()
      )
    } yield
      result.body
        .leftMap(Error.QuotasApiError)
        .flatMap(_.leftMap(Error.JsonError))

  def get(pair: Rate.Pair): Result[Rate] =
    for {
      rates ← getAll
    } yield
      rates
        .map(_.get(pair))
        .flatMap(Either.fromOption[Error, Rate](_, Error.UnknownCurrencyPair(pair)))

  def getAll: Result[Map[Rate.Pair, Rate]] = {
    val pairs = for {
      from ← Currency.upperCaseNameValuesToMap.keys
      to ← Currency.upperCaseNameValuesToMap.keys if to != from
    } yield from + to
    for {
      result ← fromTask(
        sttp
          .get(uri"${oneForgeConfig.baseUri}/quotes?pairs=${pairs.mkString(",")}&api_key=${oneForgeConfig.apiKey}")
          .response(asJson[List[Quote]])
          .send()
      )
    } yield
      result.body
        .leftMap(Error.QuotesApiError)
        .flatMap { quotes ⇒
          quotes
            .map(_.map(_.toRate).map(rate ⇒ rate.pair → rate).toMap)
            .leftMap(Error.JsonError)
        }
  }
}

class CachedOneForgeService[R] private[oneforge] (oneForgeConfig: OneForgeConfig,
                                                  _sttpBackend: SttpBackend[Task, Nothing])(
    implicit
    m1: _task[R]
) extends OneForgeService[R](oneForgeConfig, _sttpBackend) {
  val cache: mutable.Map[Rate.Pair, Rate] = mutable.Map()
  val maxAge = oneForgeConfig.maxAge

  override def get(pair: Rate.Pair): Result[Rate] = cache.get(pair) match {
    case Some(rate) if rate.age() <= maxAge ⇒ Pure(Right(rate))
    case _ ⇒
      for {
        all ← getAll
      } yield {
        all.flatMap { pairs ⇒
          cache ++= pairs
          Either.fromOption(cache.get(pair), Error.UnknownCurrencyPair(pair))
        }
      }
  }
}

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

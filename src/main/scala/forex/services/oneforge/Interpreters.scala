package forex.services.oneforge

import cats.syntax.either._
import forex.config.OneForgeConfig
import forex.domain._
import monix.eval.Task
import org.atnos.eff._
import org.atnos.eff.addon.monix.task._
import forex.domain.oneforge._
import forex.services.oneforge.client.OneForgeClient

object Interpreters {
  def dummy[R](
      implicit
      m1: _task[R]
  ): Algebra[Eff[R, ?]] = new Dummy[R]

  def simple[R](config: OneForgeConfig)(
    implicit m1: _task[R],
    client: OneForgeClient[Task]
  ): Algebra[Eff[R, ?]] = new OneForgeService[R](config, client)

  def live[R](config: OneForgeConfig)(
      implicit m1: _task[R],
      client: OneForgeClient[Task]
  ): Algebra[Eff[R, ?]] = new CachedOneForgeService[R](config, client)
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
      result ← fromTask(client.quotes(pair.toApiPair))
    } yield result.map(_.head.toRate)

  def getAll: Result[Map[Rate.Pair, Rate]] = {
    val pairs = for {
      from ← Currency.values
      to ← Currency.values if to != from
    } yield Rate.Pair(from, to).toApiPair
    for {
      result ← fromTask(client.quotes(pairs: _*))
    } yield
      result.map(_.map { quote ⇒
        val rate = quote.toRate
        rate.pair → rate
      }.toMap)
  }
}

class CachedOneForgeService[R] private[oneforge] (oneForgeConfig: OneForgeConfig, client: OneForgeClient[Task])(
    implicit
    m1: _task[R]
) extends OneForgeService[R](oneForgeConfig, client) {
  var cache: Map[Rate.Pair, Rate] = Map.empty
  val maxAge = oneForgeConfig.maxAge

  override def get(pair: Rate.Pair): Result[Rate] = cache.get(pair) match {
    case Some(rate) if rate.age() <= maxAge ⇒
      Pure(Right(rate))
    case _ ⇒
      getAll.map { all ⇒
        all.flatMap { pairs ⇒
          cache = pairs
          Either.fromOption(cache.get(pair), Error.UnsupportedCurrencyPair(pair))
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

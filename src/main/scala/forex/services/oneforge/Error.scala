package forex.services.oneforge

import forex.domain.Rate

import scala.util.control.NoStackTrace

sealed trait Error extends Throwable with NoStackTrace
object Error {
  final case class QuotasApiError(underlying: String) extends Error
  final case class QuotesApiError(underlying: String) extends Error
  final case class JsonError(underlying: io.circe.Error) extends Error
  final case class UnknownCurrencyPair(pair: Rate.Pair) extends Error
  final case object Generic extends Error
  final case class System(underlying: Throwable) extends Error
}

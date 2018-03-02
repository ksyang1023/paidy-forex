package forex.services.oneforge.client

import cats.Monad
import cats.syntax.either._
import cats.syntax.functor._
import com.softwaremill.sttp._
import com.softwaremill.sttp.circe._
import forex.config.OneForgeClientConfig
import forex.domain.oneforge._
import forex.services.oneforge.Error

import scala.util.{Failure, Success, Try}

trait OneForgeClient[F[_]] {
  type Result[R] = F[Error Either R]

  def quotes(pairs: List[Pair]): Result[List[Quote]]
  def quotes(pairs: Pair*): Result[List[Quote]] = quotes(pairs.toList)
  def symbols: Result[List[Pair]]
  def marketStatus: Result[MarketStatus]
  def quota: Result[Quota]
}

class OneForgeClientImpl[F[_]: Monad](
    clientConfig: OneForgeClientConfig
)(implicit _sttpBackend: SttpBackend[F, Nothing])
    extends OneForgeClient[F] {
  def quotes(pairs: List[Pair]): Result[List[Quote]] = performRequest {
    sttp
      .get(
        uri"${clientConfig.baseUri}/quotes?pairs=${pairs.map(_.render).mkString(",")}&api_key=${clientConfig.apiKey}"
      )
      .response(asJson[List[Quote]])
  }

  def symbols: Result[List[Pair]] = ???

  def marketStatus: Result[MarketStatus] = ???

  def quota: Result[Quota] = performRequest {
    sttp
      .get(uri"${clientConfig.baseUri}/quota?api_key=${clientConfig.apiKey}")
      .response(asJson[Quota])
  }

  private def performRequest[X](request: ⇒ Request[io.circe.Error Either X, Nothing]): Result[X] =
    Try(request.send()) match {
      case Success(response)  ⇒ response.map(handleResponse)
      case Failure(throwable) ⇒ Monad[F].pure(Left(Error.System(throwable)))
    }

  private def handleResponse[A](response: Response[io.circe.Error Either A]): Error Either A =
    for {
      body ← response.body.leftMap(Error.ApiError)
      result ← body.leftMap(Error.JsonError)
    } yield result
}

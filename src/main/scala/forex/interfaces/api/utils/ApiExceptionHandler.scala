package forex.interfaces.api.utils

import akka.http.scaladsl._
import forex.processes._
import forex.processes.rates.messages.Error.InvalidRequestError
import ApiMarshallers._

object ApiExceptionHandler {

  def apply(): server.ExceptionHandler =
    server.ExceptionHandler {
      case error: InvalidRequestError =>
        ctx =>
          ctx.complete(400 -> ErrorApiResponse(s"Invalid request parameters"))
      case error: RatesError ⇒
        ctx ⇒
          ctx.complete(500 -> ErrorApiResponse(error.errorMessage))
      case error: Throwable ⇒
        ctx ⇒
          ctx.complete(500 -> ErrorApiResponse(error.toString))
    }

  implicit class RatesErrorOps(self: RatesError) {
    def errorMessage: String = self match {
      case RatesError.ExternalApiError(_) => "External API error"
      case RatesError.InvalidRequestError(_) => "Invalid request error"
      case RatesError.System(_) | RatesError.Generic => "Internal server error"
    }
  }
}

package forex.interfaces.api.utils

import akka.http.scaladsl._
import forex.processes._
import forex.processes.rates.messages.Error.InvalidRequestError

object ApiExceptionHandler {

  def apply(): server.ExceptionHandler =
    server.ExceptionHandler {
      case error: InvalidRequestError =>
        ctx =>
          ctx.complete(400 -> s"Invalid request parameters")
      case error: RatesError ⇒
        ctx ⇒
          ctx.complete(500 -> error.toString)
      case error: Throwable ⇒
        ctx ⇒
          ctx.complete(500 -> error.toString)
    }

}

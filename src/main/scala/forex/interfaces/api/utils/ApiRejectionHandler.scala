package forex.interfaces.api.utils

import akka.http.scaladsl._
import akka.http.scaladsl.model.{ ContentTypes, HttpEntity, HttpResponse }
import io.circe.syntax._

object ApiRejectionHandler {

  def apply(): server.RejectionHandler =
    server.RejectionHandler.default
      .mapRejectionResponse {
        case res @ HttpResponse(_, _, HttpEntity.Strict(_, data), _) ⇒
          res.copy(
            entity = HttpEntity(ContentTypes.`application/json`, ErrorApiResponse(data.utf8String).asJson.noSpaces)
          )
      }

}

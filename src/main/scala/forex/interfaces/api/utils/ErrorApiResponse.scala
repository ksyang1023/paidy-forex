package forex.interfaces.api.utils

import io.circe._
import io.circe.generic.semiauto._

case class ErrorApiResponse(error: String)

object ErrorApiResponse {
  implicit val encoder: Encoder[ErrorApiResponse] = deriveEncoder[ErrorApiResponse]
  implicit val decoder: Decoder[ErrorApiResponse] = deriveDecoder[ErrorApiResponse]
}

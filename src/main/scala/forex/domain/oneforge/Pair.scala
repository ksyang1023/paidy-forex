package forex.domain.oneforge

import io.circe.Decoder

case class Pair(from: String, to: String) {
  def render: String = s"$from$to"
}

object Pair {
  implicit val decoder: Decoder[Pair] = Decoder.decodeString.emap(_.splitAt(3) match {
    case (from, to) ⇒ Right(Pair(from, to))
    case invalid => Left(s"'$invalid' is not a valid from-to currency pair")
  })
}

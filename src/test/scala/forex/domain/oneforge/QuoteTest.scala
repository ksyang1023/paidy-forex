package forex.domain.oneforge

import org.scalatest.{EitherValues, FlatSpec, Matchers}
import io.circe.parser._

class QuoteTest extends FlatSpec with Matchers with EitherValues {

  behavior of classOf[Quote].getSimpleName

  it should "get parsed from JSON" in {
    // copied from https://1forge.com/forex-data-api/api-documentation#quotes
    val input =
      """
        |[
        |     {
        |          "symbol": "AUDUSD",
        |          "price": 0.792495,
        |          "bid": 0.79248,
        |          "ask": 0.79251,
        |          "timestamp": 1502160793
        |     },
        |     {
        |          "symbol": "EURUSD",
        |          "price": 1.181,
        |          "bid": 1.18099,
        |          "ask": 1.18101,
        |          "timestamp": 1502160794
        |     },
        |     {
        |          "symbol": "GBPJPY",
        |          "price": 144.3715,
        |          "bid": 144.368,
        |          "ask": 144.375,
        |          "timestamp": 1502160794
        |     }
        |]
      """.stripMargin

    val expResult = Seq(
      Quote(symbol = "AUDUSD", price = BigDecimal(792495, 6), bid = BigDecimal(79248, 5), ask = BigDecimal(79251, 5), timestamp = 1502160793L),
      Quote(symbol = "EURUSD", price = BigDecimal(1181, 3), bid = BigDecimal(118099, 5), ask = BigDecimal(118101, 5), timestamp = 1502160794L),
      Quote(symbol = "GBPJPY", price = BigDecimal(1443715, 4), bid = BigDecimal(144368, 3), ask = BigDecimal(144375, 3), timestamp = 1502160794L),
    )

    parse(input).flatMap(_.as[List[Quote]]).right.value shouldEqual expResult
  }

}

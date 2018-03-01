package forex.domain.oneforge

import org.scalatest.{EitherValues, FlatSpec, Matchers}
import io.circe.parser._

class QuotaTest extends FlatSpec with Matchers with EitherValues {

  behavior of classOf[Quota].getSimpleName

  it should "get parsed from JSON" in {
    // copied from https://1forge.com/forex-data-api/api-documentation#quota
    val input =
      """
        |{
        |     "quota_used": 53232,
        |     "quota_limit": 100000,
        |     "quota_remaining": 46768,
        |     "hours_until_reset": 11
        |}
      """.stripMargin

    val expResult = Quota(quota_used = 53232, quota_limit = 100000, quota_remaining = 46768, hours_until_reset = 11)

    parse(input).flatMap(_.as[Quota]).right.value shouldEqual expResult

  }
}

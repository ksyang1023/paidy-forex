package forex.services.oneforge.client

import java.net.URI

import com.softwaremill.sttp._
import com.softwaremill.sttp.testing._
import forex.ModelFactory
import forex.config.OneForgeClientConfig
import forex.domain.oneforge
import forex.services.oneforge.Error
import org.scalacheck.Gen
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatest.{ EitherValues, FreeSpec, Matchers }

import scala.concurrent.TimeoutException

class OneForgeClientImplTest[F[_]]
    extends FreeSpec
    with Matchers
    with EitherValues
    with GeneratorDrivenPropertyChecks
    with ModelFactory {

  // fixtures
  val baseUri = "https://1forge.local/1.0.0"
  val apiKey = "apiKey1"
  val clientConfig = OneForgeClientConfig(new URI(baseUri), apiKey)

  classOf[OneForgeClientImpl[F]].getSimpleName - {
    "for /quotes" - {
      "should retrieve quotes for currency pairs" in {
        forAll(Gen.listOf(aOneForgePair), aBigDecimal, aBigDecimal, aBigDecimal, aTimestamp) {
          case (pairs, price, bid, ask, timestamp) ⇒
            val pairsString = pairs.map(_.render).mkString(",")

            val expResult = pairs.map { pair ⇒
              oneforge.Quote(pair.render, price, bid, ask, timestamp.value.toEpochSecond)
            }

            implicit val _sttpBackend = SttpBackendStub(HttpURLConnectionBackend())
              .whenRequestMatches(_.uri == uri"$baseUri/quotes?pairs=$pairsString&api_key=$apiKey")
              .thenRespond(Right(expResult))
            val instance = new OneForgeClientImpl[Id](clientConfig)

            instance.quotes(pairs: _*).right.value shouldEqual expResult
        }
      }

      "should handle JSON parsing errors" in {
        val expError = io.circe.ParsingFailure("Failed to parse JSON", null)
        val pair = oneforge.Pair("FROM", "TO")

        implicit val _sttpBackend = SttpBackendStub(HttpURLConnectionBackend())
          .whenRequestMatches(_.uri == uri"$baseUri/quotes?pairs=${pair.render}&api_key=$apiKey")
          .thenRespond(Left(expError))
        val instance = new OneForgeClientImpl[Id](clientConfig)

        instance.quotes(pair).left.value shouldEqual Error.JsonError(expError)
      }

      "should handle API errors" in {
        val expError = Error.ApiError("Service unavailable")
        val pair = oneforge.Pair("FROM", "TO")

        implicit val _sttpBackend = SttpBackendStub(HttpURLConnectionBackend())
          .whenRequestMatches(_.uri == uri"$baseUri/quotes?pairs=${pair.render}&api_key=$apiKey")
          .thenRespondWithCode(500, "Service unavailable")
        val instance = new OneForgeClientImpl[Id](clientConfig)

        instance.quotes(pair).left.value shouldEqual expError
      }

      "should handle other errors" in {
        val pair = oneforge.Pair("FROM", "TO")
        implicit val _sttpBackend = SttpBackendStub(HttpURLConnectionBackend())
          .whenRequestMatches(_.uri == uri"$baseUri/quotes?pairs=${pair.render}&api_key=$apiKey")
          .thenRespond(throw new TimeoutException("Unable to connect"))
        val instance = new OneForgeClientImpl[Id](clientConfig)

        val result = instance.quotes(pair).left.value
        result shouldBe an[Error.System]

        val throwable = result.asInstanceOf[Error.System].underlying
        throwable shouldBe a[TimeoutException]
        throwable.getMessage shouldEqual "Unable to connect"
      }
    }

    "for /quota" - {
      "should retrieve the current API quotas" in {
        forAll(aOneForgeQuota) { quota ⇒
          val expResult = quota

          implicit val _sttpBackend = SttpBackendStub(HttpURLConnectionBackend())
            .whenRequestMatches(_.uri == uri"$baseUri/quota?api_key=$apiKey")
            .thenRespond(Right(expResult))
          val instance = new OneForgeClientImpl[Id](clientConfig)

          instance.quota.right.value shouldEqual expResult
        }
      }

      "should handle JSON parsing errors" in {
        val expError = io.circe.ParsingFailure("Failed to parse JSON", null)

        implicit val _sttpBackend = SttpBackendStub(HttpURLConnectionBackend())
          .whenRequestMatches(_.uri == uri"$baseUri/quota?api_key=$apiKey")
          .thenRespond(Left(expError))
        val instance = new OneForgeClientImpl[Id](clientConfig)

        instance.quota.left.value shouldEqual Error.JsonError(expError)
      }

      "should handle API errors" in {
        val expError = Error.ApiError("Service unavailable")

        implicit val _sttpBackend = SttpBackendStub(HttpURLConnectionBackend())
          .whenRequestMatches(_.uri == uri"$baseUri/quota?api_key=$apiKey")
          .thenRespondWithCode(500, "Service unavailable")
        val instance = new OneForgeClientImpl[Id](clientConfig)

        instance.quota.left.value shouldEqual expError
      }

      "should handle other errors" in {
        implicit val _sttpBackend = SttpBackendStub(HttpURLConnectionBackend())
          .whenRequestMatches(_.uri == uri"$baseUri/quota?api_key=$apiKey")
          .thenRespond(throw new TimeoutException("Unable to connect"))
        val instance = new OneForgeClientImpl[Id](clientConfig)

        val result = instance.quota.left.value
        result shouldBe an[Error.System]

        val throwable = result.asInstanceOf[Error.System].underlying
        throwable shouldBe a[TimeoutException]
        throwable.getMessage shouldEqual "Unable to connect"
      }
    }
  }

}

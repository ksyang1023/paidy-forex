package forex.services.oneforge.client

import java.net.URI

import com.softwaremill.sttp._
import com.softwaremill.sttp.testing._
import forex.config.OneForgeClientConfig
import forex.domain.oneforge
import forex.services.oneforge.Error
import org.scalatest.{EitherValues, FreeSpec, Matchers}

import scala.concurrent.TimeoutException

class OneForgeClientImplTest[F[_]] extends FreeSpec with Matchers with EitherValues {

  // fixtures
  val baseUri = "https://1forge.local/1.0.0"
  val apiKey = "apiKey1"
  val clientConfig = OneForgeClientConfig(new URI(baseUri), apiKey)

  classOf[OneForgeClientImpl[F]].getSimpleName - {
    "for /quotes" - {
      "should retrieve quotes for currency pairs" in {
        val pairs = Seq(
          oneforge.Pair("FROM1", "TO1"),
          oneforge.Pair("FROM2", "TO2"),
          oneforge.Pair("FROM3", "TO3"),
        )

        val pairsString = pairs.map(_.render).mkString(",")

        val expResult = List(
          oneforge.Quote("FROM1TO1", 1.1, 2.1, 3.1, 124567891L),
          oneforge.Quote("FROM2TO2", 1.2, 2.2, 3.2, 124567892L),
          oneforge.Quote("FROM3TO3", 1.3, 2.3, 3.3, 124567893L),
        )

        implicit val _sttpBackend = SttpBackendStub(HttpURLConnectionBackend())
          .whenRequestMatches(_.uri == uri"$baseUri/quotes?pairs=$pairsString&api_key=$apiKey")
          .thenRespond(Right(expResult))
        val instance = new OneForgeClientImpl[Id](clientConfig)

        instance.quotes(pairs:_*).right.value shouldEqual expResult
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
        val expResult = oneforge.Quota(10, 1000, 990, 10)

        implicit val _sttpBackend = SttpBackendStub(HttpURLConnectionBackend())
          .whenRequestMatches(_.uri == uri"$baseUri/quota?api_key=$apiKey")
          .thenRespond(Right(expResult))
        val instance = new OneForgeClientImpl[Id](clientConfig)

        instance.quota.right.value shouldEqual expResult
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

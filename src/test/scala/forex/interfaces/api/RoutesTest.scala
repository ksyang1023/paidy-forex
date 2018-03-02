package forex.interfaces.api

import java.net.URI

import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.softwaremill.sttp.SttpBackend
import com.softwaremill.sttp.asynchttpclient.monix.AsyncHttpClientMonixBackend
import com.softwaremill.sttp.testing.SttpBackendStub
import forex.ModelFactory
import forex.config.{ OneForgeClientConfig, OneForgeConfig }
import forex.domain.oneforge.Quote
import forex.interfaces.api.rates.Protocol.GetApiResponse
import forex.interfaces.api.utils.ApiMarshallers._
import forex.main.{ AppEffect, Processes, Runners }
import forex.services.OneForge
import forex.services.oneforge.client.{ OneForgeClient, OneForgeClientImpl }
import monix.cats._
import monix.eval.Task
import org.scalatest.prop.PropertyChecks
import org.scalatest.{ FreeSpec, Matchers }

class RoutesTest extends FreeSpec with Matchers with ScalatestRouteTest with PropertyChecks with ModelFactory {

  "Routes" - {
    "/" - {
      "should retrieve the quote for a currency pair" in {
        forAll(aCurrency, aCurrency, aPrice, aTimestamp) {
          case (from, to, price, timestamp) ⇒
            val quote =
              Quote(from.entryName + to.entryName, price.value, price.value, price.value, timestamp.value.toEpochSecond)
            val sttpBackend =
              SttpBackendStub(AsyncHttpClientMonixBackend()).whenAnyRequest.thenRespond(Right(List(quote)))

            val ratesRoutes = rates.Routes(processes(sttpBackend), Runners())
            val route = Routes(ratesRoutes).route

            val query = Uri.Query("from" → from.entryName, "to" → to.entryName)
            val uri = Uri("/").withQuery(query)

            val expResult = GetApiResponse(from, to, price, timestamp)

            Get(uri) ~> route ~> check {
              responseAs[GetApiResponse] shouldEqual expResult
            }
        }
      }

      "should handle external API errors" in {
        forAll(aCurrency, aCurrency, aPrice, aTimestamp) {
          case (from, to, price, timestamp) ⇒
            val sttpBackend = SttpBackendStub(AsyncHttpClientMonixBackend()).whenAnyRequest.thenRespondServerError()

            val ratesRoutes = rates.Routes(processes(sttpBackend), Runners())
            val route = Routes(ratesRoutes).route

            val query = Uri.Query("from" → from.entryName, "to" → to.entryName)
            val uri = Uri("/").withQuery(query)

            val expResult = Map("error" → "External API error")

            Get(uri) ~> route ~> check {
              val x = response.status
              responseAs[Map[String, String]] shouldEqual expResult
            }
        }
      }

      // other tests to check parameters validation and similar would follow here
    }
  }

  private def processes(_sttpBackend: SttpBackend[Task, Nothing]): Processes = new Processes {
    implicit val oneForgeConfig: OneForgeConfig = null
    implicit val sttpBackend: SttpBackend[Task, Nothing] = _sttpBackend
    implicit val oneForgeClient: OneForgeClient[Task] =
      new OneForgeClientImpl[Task](OneForgeClientConfig(new URI("https://bogus.local"), "apiKey1"))
    implicit val oneForge: OneForge[AppEffect] = OneForge.simple(oneForgeConfig)
  }
}

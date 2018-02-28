package forex.main

import monix.cats._
import com.softwaremill.sttp.SttpBackend
import com.softwaremill.sttp.asynchttpclient.monix.AsyncHttpClientMonixBackend
import forex.config._
import forex.services.oneforge.client.{ OneForgeClient, OneForgeClientImpl }
import forex.{ services ⇒ s }
import forex.{ processes ⇒ p }
import monix.eval.Task
import org.zalando.grafter.macros._

@readerOf[ApplicationConfig]
case class Processes(oneForgeConfig: OneForgeConfig) {

  implicit final lazy val sttpBackend: SttpBackend[Task, Nothing] = AsyncHttpClientMonixBackend()

  implicit final lazy val oneForgeClient: OneForgeClient[Task] = new OneForgeClientImpl(oneForgeConfig.client)

  implicit final lazy val oneForge: s.OneForge[AppEffect] =
    s.OneForge.live[AppStack](oneForgeConfig)

  final val Rates = p.Rates[AppEffect]

}

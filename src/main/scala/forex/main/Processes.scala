package forex.main

import forex.config._
import forex.services.oneforge.OneForgeService
import forex.{services => s}
import forex.{processes => p}
import org.zalando.grafter.macros._

@readerOf[ApplicationConfig]
case class Processes(oneForgeConfig: OneForgeConfig) {

  implicit final lazy val oneForge: s.OneForge[AppEffect] =
    s.OneForge.live[AppStack](oneForgeConfig)

  final val Rates = p.Rates[AppEffect]

}

package forex.config

import java.net.URI

import org.zalando.grafter.macros._

import scala.concurrent.duration.{Duration, FiniteDuration}

@readers
case class ApplicationConfig(
    akka: AkkaConfig,
    api: ApiConfig,
    executors: ExecutorsConfig,
    oneForge: OneForgeConfig
)

case class AkkaConfig(
    name: String,
    exitJvmTimeout: Option[FiniteDuration]
)

case class ApiConfig(
    interface: String,
    port: Int
)

case class ExecutorsConfig(
    default: String
)

case class OneForgeConfig(
  maxAge: Duration,
  client: OneForgeClientConfig
)

case class OneForgeClientConfig(
  baseUri: URI,
  apiKey: String
)

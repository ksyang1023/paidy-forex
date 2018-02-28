package forex.services

import forex.domain.{ Rate, Timestamp }
import forex.domain.{oneforge => Api }

import scala.concurrent.duration.{ Duration, SECONDS }

package object oneforge {

  implicit class RateOps(rate: Rate) {
    def age(now: Timestamp = Timestamp.now): Duration =
      Duration(now.value.toEpochSecond - rate.timestamp.value.toEpochSecond, SECONDS)
  }

  implicit class PairOps(pair: Rate.Pair) {
    def toApiPair: Api.Pair = Api.Pair(pair.from.entryName, pair.to.entryName)
  }
}

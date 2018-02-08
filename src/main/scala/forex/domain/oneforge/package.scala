package forex.domain

import java.time.{ Instant, OffsetDateTime, ZoneId }

package object oneforge {

  implicit class QuoteOps(quote: Quote) {
    def toRate: Rate = Rate(pair, price, timestamp)
    def pair: Rate.Pair = quote.symbol.splitAt(3) match {
      case (from, to) ⇒ Rate.Pair(Currency.withName(from), Currency.withName(to))
    }
    def price: Price = Price(quote.price)
    def timestamp: Timestamp =
      Timestamp(OffsetDateTime.ofInstant(Instant.ofEpochSecond(quote.timestamp), ZoneId.of("UTC")))
  }

}

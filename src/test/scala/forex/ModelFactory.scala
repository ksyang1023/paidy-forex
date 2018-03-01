package forex

import java.time.{Instant, OffsetDateTime}

import forex.domain._
import org.scalacheck.Gen

trait ModelFactory {

  def aCurrency: Gen[Currency] = Gen.oneOf(Currency.values)
  def aCurrencyString: Gen[String] = aCurrency.map(_.entryName)

  def aPrice: Gen[Price] = Gen.posNum[Int].map(Price(_))
  def aTimestamp: Gen[Timestamp] = Gen.const(Timestamp(OffsetDateTime.now().withNano(0)))

}

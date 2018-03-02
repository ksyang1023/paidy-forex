package forex

import java.time.{Instant, OffsetDateTime}

import forex.domain._
import org.scalacheck.Gen

trait ModelFactory {

  def aCurrency: Gen[Currency] = Gen.oneOf(Currency.values)
  def aCurrencyString: Gen[String] = aCurrency.map(_.entryName)

  def anInt: Gen[Int] = Gen.posNum[Int]
  def aBigDecimal: Gen[BigDecimal] = Gen.posNum[Int].map(BigDecimal(_))
  def aPrice: Gen[Price] = Gen.posNum[Int].map(Price(_))
  def aTimestamp: Gen[Timestamp] = Gen.const(Timestamp(OffsetDateTime.now().withNano(0)))

}

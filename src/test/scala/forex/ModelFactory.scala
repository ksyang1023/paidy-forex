package forex

import java.time.{ Instant, OffsetDateTime }

import forex.domain._
import org.scalacheck.Gen

trait ModelFactory {

  def aCurrency: Gen[Currency] = Gen.oneOf(Currency.values)
  def aCurrencyString: Gen[String] = aCurrency.map(_.entryName)

  def anInt: Gen[Int] = Gen.posNum[Int]
  def aBigDecimal: Gen[BigDecimal] = Gen.choose(0.0, 100.0).map(BigDecimal(_))
  def aPrice: Gen[Price] = Gen.posNum[Int].map(Price(_))
  def aTimestamp: Gen[Timestamp] = Gen.const(Timestamp(OffsetDateTime.now().withNano(0)))

  def aOneForgePair: Gen[oneforge.Pair] =
    for {
      from ← aCurrency
      to ← aCurrency
    } yield oneforge.Pair(from.entryName, to.entryName)

  def aOneForgeQuota: Gen[oneforge.Quota] =
    for {
      limit ← Gen.choose(10000, 20000)
      used ← Gen.posNum[Int].suchThat(_ <= limit)
      remaining = limit - used
      hours ← Gen.posNum[Int]
    } yield oneforge.Quota(used, limit, remaining, hours)

}

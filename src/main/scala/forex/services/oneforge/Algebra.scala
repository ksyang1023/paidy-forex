package forex.services.oneforge

import forex.domain._
import forex.domain.oneforge._

trait Algebra[F[_]] {
  def get(pair: Rate.Pair): F[Error Either Rate]
  def quota: F[Error Either Quota]
}

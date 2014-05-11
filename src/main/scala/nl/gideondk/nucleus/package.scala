package nl.gideondk

import nl.gideondk.nucleus.protocol.ETFReader
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

package object nucleus {

  implicit class ConvertableFuture(val f: Future[ClientResult]) {
    def as[T](implicit reader: ETFReader[T]) =
      f.flatMap(x ⇒ x.as[T] match {
        case None    ⇒ Future.failed(new Exception("Failed to deserialize"))
        case Some(x) ⇒ Future(x)
      })
  }

  implicit class ConvertableStreamTask(val sf: Future[ClientStreamResult]) {
    def as[T](implicit reader: ETFReader[T]) =
      sf.map(x ⇒ x.as[T])
  }
}
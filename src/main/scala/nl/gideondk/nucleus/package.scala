package nl.gideondk

import nl.gideondk.sentinel.Task
import nl.gideondk.nucleus.protocol.ETFReader
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

package object nucleus {

  implicit class ConvertableTask(val task: Task[ClientResult]) {
    def as[T](implicit reader: ETFReader[T]) =
      Task(task.get.map(_.flatMap(x ⇒ x.as[T] match {
        case None    ⇒ Future.failed(new Exception("Failed to deserialize"))
        case Some(x) ⇒ Future(x)
      })))
  }

  implicit class ConvertableStreamTask(val task: Task[ClientStreamResult]) {
    def as[T](implicit reader: ETFReader[T]) =
      Task(task.get.map(_.map(x ⇒ x.as[T])))
  }
}
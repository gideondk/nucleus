package nl.gideondk.nucleus

import org.specs2.mutable.Specification
import scalaz._
import Scalaz._
import scala.concurrent.duration._
import nl.gideondk.nucleus.protocol._
import ETF._
import scala.concurrent.Await

class RequestResponseSpec extends Specification {

  import Workers._

  sequential
  implicit val duration = Duration(10, SECONDS)

  "A Client" should {
    "be able to be send and receive messages to and from a server" in {
      val req = ((client |?| "calc" |/| "add") ? (1, 4))
      val res = req.as[Int]
      Await.result(res, duration) == 5
    }

    "be able to receive complexer types correctly" in {
      val req = ((client |?| "spatial" |/| "cubicalString") ? (2, 5, 7))
      val res = req.as[String]
      Await.result(res, duration) == (2 * 5 * 7).toString
    }

    "be able to use argumentless functions" in {
      val req = (client |?| "generation" |/| "generate_5") ? ()
      val res = req.as[Int]
      Await.result(res, duration) == 5
    }
  }
}

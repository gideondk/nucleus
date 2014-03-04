package nl.gideondk.nucleus

import org.specs2.mutable.Specification
import scalaz._
import Scalaz._
import scala.concurrent._
import scala.concurrent.duration._
import nl.gideondk.nucleus.protocol._
import ETF._
import play.api.libs.iteratee._

class StreamSpec extends Specification with Routing {

  import Workers._

  sequential
  implicit val duration = Duration(10, SECONDS)

  "A Client" should {
    "be able to be stream from a server" in {
      val n = 500
      val reqE = (client |?| "generation" |/| "generate_stream") ?->> (n)
      val resE = reqE.as[String]
      val compE = resE.copoint |>>> Iteratee.getChunks
      val res = Await.result(compE, Duration(5, SECONDS))

      res.length == n
    }

    "be able to send a stream to a server" in {
      val enum = Enumerator(1, 2, 3, 4, 5, 6)
      val req = (client |?| "process" |/| "sum") ?<<- enum
      val res = req.as[Int]
      res.copoint == 21
    }

    "be able to stream a larger file to a server" in {
      val num = 50
      val multiplier = 5
      val chunks = List.fill(num)(LargerPayloadTestHelper.randomBAForSize((1024 * 100).toInt)) // 5MB
      val req = (client |?| "process" |/| "size") ?<<-(multiplier, Enumerator(chunks: _*))
      val res = req.as[Int]

      val localSize = chunks.foldLeft(Array[Byte]())(_ ++ _).size * multiplier
      res.copoint == localSize
    }
  }
}

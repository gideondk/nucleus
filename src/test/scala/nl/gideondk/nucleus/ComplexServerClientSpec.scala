package nl.gideondk.nucleus

import org.specs2.mutable.Specification
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import akka.actor.ActorSystem
import scalaz._
import Scalaz._
import scala.concurrent.Await
import scala.concurrent.duration.Duration

import akka.actor.Actor
import akka.actor.Props
import akka.pattern.ask

import akka.util.Timeout
import scala.concurrent.duration._

import nl.gideondk.nucleus._
import nl.gideondk.nucleus.protocol._
import ETF._

object CacheServer extends Routing {
  implicit val system = ActorSystem("cache-system")

  case class SetCache(k: String, v: String)
  case class GetCache(k: String)
  case class CacheResult(v: Option[String])

  class CacheActor extends Actor {
    var cacheMap = Map[String, String]()

    def receive = {
      case x: SetCache ⇒ cacheMap = cacheMap + (x.k -> x.v)
      case x: GetCache ⇒ sender ! CacheResult(cacheMap.get(x.k))
    }
  }

  implicit val timeout = Timeout(1 second)
  val actor = system.actorOf(Props[CacheActor])
  val modules = Module("cache") {
    cast("set")((key: String, value: String) ⇒ actor ! SetCache(key, value)) ~
      call("get")((key: String) ⇒ (actor ? GetCache(key)).mapTo[CacheResult].map(_.v.getOrElse("")))
  }
}

class ComplexClientSpec extends Specification {
  implicit val duration = Duration(10, SECONDS)

  lazy val (client, server) = {
    val serverSystem = ActorSystem("server-system")
    val server = Server("cacheService", CacheServer.modules)(serverSystem)
    server.run(8888)
    Thread.sleep(1000)
    val clientSystem = ActorSystem("client-system")
    val client = Client("localhost", 8888, 4, "Cache client")(clientSystem)
    (client, server)
  }

  "A more complex client" should {
    "be able to use both cast as call functionality in correct fashion" in {
      val res = for {
        _ ← (client |?| "cache" |/| "set") ! ("A", "Test Value") // Because the current back-end is single actor backed, we can anticipate on the fact that the cast is handled before the call
        x ← (client |?| "cache" |/| "get") ? "A"
      } yield x

      res.as[String].copoint must equalTo("Test Value")
    }
  }
}

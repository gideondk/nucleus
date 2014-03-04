package nl.gideondk.nucleus

import scala.concurrent.ExecutionContext.Implicits.global
import akka.actor.ActorSystem
import scala.concurrent._
import Module._
import nl.gideondk.nucleus.protocol._
import ETF._
import play.api.libs.iteratee._

object Workers extends Routing {
  lazy val (client, server) = {
    val serverSystem = ActorSystem("server-system")
    val server = Server("CalcService", modules)(serverSystem)
    server.run(9999)
    Thread.sleep(1000)
    val clientSystem = ActorSystem("client-system")
    val client = Client("localhost", 9999, 32, "Calc client")(clientSystem)
    (client, server)
  }

  val modules = Module("calc") {
    call("add")((a: Int, b: Int) ⇒ Future(a + b)) ~
      call("length")((s: String) ⇒ Future(s.length))
  } ~
    Module("spatial") {
      call("cubicalString")((a: Int, b: Int, c: Int) ⇒ Future((a * b * c).toString))
    } ~
    Module("identity") {
      call("list")((a: List[Int]) ⇒ Future(a))
    } ~
    Module("generation") {
      call("generate_5")(() ⇒ Future(5)) ~
        stream("generate_stream")((n: Int) ⇒ Future(Enumerator(List.fill(n)("CHUNK"): _*)))
    } ~
    Module("process") {
      process("sum")(() ⇒ (x: Enumerator[Int]) ⇒ x |>>> Iteratee.fold(0)((a, b) ⇒ b + a)) ~
        process("size")((multiplier: Int) ⇒ (x: Enumerator[Array[Byte]]) ⇒ (x |>>> Iteratee.fold(Array[Byte]()) {
          _ ++ _
        }).map(_.size * multiplier))
    } ~
    Module("exception") {
      call("generate_exception")(() ⇒ Future(List(1, 2, 3, 4)(5)))
    }

}

object LargerPayloadTestHelper {
  def randomBAForSize(size: Int) = {
    implicit val be = java.nio.ByteOrder.BIG_ENDIAN
    val stringB = new StringBuilder(size)
    val paddingString = "abcdefghijklmnopqrs"

    while (stringB.length() + paddingString.length() < size) stringB.append(paddingString)

    stringB.toString().getBytes()
  }
}

object BenchmarkHelpers {
  def timed(desc: String, n: Int)(benchmark: ⇒ Unit) = {
    println("* " + desc)
    val t = System.currentTimeMillis
    benchmark
    val d = System.currentTimeMillis - t

    println("* - number of ops/s: " + n / (d / 1000.0) + "\n")
  }

  def throughput(desc: String, size: Double, n: Int)(benchmark: ⇒ Unit) = {
    println("* " + desc)
    val t = System.currentTimeMillis
    benchmark
    val d = System.currentTimeMillis - t

    val totalSize = n * size
    println("* - number of mb/s: " + totalSize / (d / 1000.0) + "\n")
  }
}
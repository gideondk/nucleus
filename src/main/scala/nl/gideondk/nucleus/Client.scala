package nl.gideondk.nucleus

import akka.actor.ActorSystem

import akka.util.ByteString
import scalaz._
import Scalaz._

import play.api.libs.iteratee._
import play.api.libs.iteratee.Enumerator._
import scala.util.Try

import akka.io._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

import nl.gideondk.nucleus.protocol._

import nl.gideondk.sentinel.Task

import shapeless._
import nl.gideondk.sentinel.processors.Consumer.ConsumerException

trait ClientConfig {
  def host: String
  def port: Int
  def workers: Int
}

object ClientConfig {
  def apply(serverHost: String, serverPort: Int, workerCount: Int) = new ClientConfig {
    val host = serverHost
    val port = serverPort
    val workers = workerCount
  }
}

case class ClientResult(rawResult: ByteString) {
  def as[T](implicit reader: ETFReader[T]): Option[T] = Try(reader.read(rawResult)).toOption
}

case class ClientStreamResult(rawResult: Enumerator[ByteString]) {
  def as[T](implicit reader: ETFReader[T]): Enumerator[T] = rawResult &> Enumeratee.map(reader.read)
}

class CoreClientFunction(client: Client, module: String, functionName: String) {
  private def handleException[T](t: Task[T]): Task[T] = Task(t.get.map(x ⇒ x.recoverWith {
    case e: ConsumerException[NucleusMessage] ⇒ e.cause match {
      case x: Response.Error ⇒ Future.failed(NucleusException.errorToNucleusException(x))
      case _                 ⇒ Future.failed(new Exception("Received incorrect message as a error"))
    }
    case e: Throwable ⇒ Future.failed(e)
  }))

  private def handleResponse: PartialFunction[NucleusMessage, Task[ClientResult]] = {
    case x: Response.Reply ⇒ ClientResult(x.value).point[Task]
    case x: NucleusMessage ⇒ Task[ClientResult](Future.failed(new Exception("Unexpected response: expected Reply, received " + x)))
  }

  private def handleStreamResponse(bse: Enumerator[NucleusMessage]) = {
    ClientStreamResult(bse &> Enumeratee.map {
      case x: Response.ReplyChunk ⇒ x.value
      case x: NucleusMessage      ⇒ throw new Exception("Internal stream exception, didn't receive stream chunk")
    }).point[Task]
  }

  def call(): Task[ClientResult] = {
    val cmd = Request.ArgumentLessCall(Atom(module), Atom(functionName))
    ((client ask cmd) flatMap handleResponse) |> handleException
  }

  def call[T](args: T)(implicit nst: T <:!< Product, tW: ETFConverter[Tuple1[T]]): Task[ClientResult] =
    call(Tuple1(args))

  def call[T <: Product](args: T)(implicit tW: ETFConverter[T]): Task[ClientResult] = {
    val cmd = Request.Call(Atom(module), Atom(functionName), tW.write(args))
    ((client ask cmd) flatMap handleResponse) |> handleException
  }

  def cast[T](args: T)(implicit nst: T <:!< Product, tW: ETFConverter[Tuple1[T]]): Task[Unit] =
    cast(Tuple1(args))

  def cast[T <: Product](args: T)(implicit tW: ETFConverter[T]): Task[Unit] = {
    val cmd = Request.Cast(Atom(module), Atom(functionName), tW.write(args))
    ((client ask cmd).flatMap {
      case x: Response.NoReply ⇒ ().point[Task]
      case x: NucleusMessage   ⇒ Task[Unit](Future.failed(new Exception("Unexpected response: expected NoReply, received " + x)))
    }) |> handleException[Unit]
  }

  def stream[T](args: T)(implicit nst: T <:!< Product, tW: ETFConverter[Tuple1[T]]): Task[ClientStreamResult] =
    stream(Tuple1(args))

  def stream[T <: Product](args: T)(implicit tW: ETFConverter[T]): Task[ClientStreamResult] = {
    val cmd = Request.Stream(Atom(module), Atom(functionName), tW.write(args))
    ((client askStream cmd) flatMap handleStreamResponse) |> handleException
  }

  def process[C](enum: Enumerator[C])(implicit cC: ETFConverter[C]): Task[ClientResult] = {
    val cmd = Request.Process(Atom(module), Atom(functionName), ByteString())
    val stream = enum.map(x ⇒ Request.RequestChunk(cC.write(x)).asInstanceOf[NucleusMessage]) andThen Enumerator(Request.RequestChunkTerminator().asInstanceOf[NucleusMessage])
    (client.sendStream(cmd, stream) flatMap handleResponse) |> handleException
  }

  def process[T, C](args: T, enum: Enumerator[C])(implicit nst: T <:!< Product, tW: ETFConverter[Tuple1[T]], cC: ETFConverter[C]): Task[ClientResult] =
    process(Tuple1(args), enum)

  def process[T <: Product, C](args: T, enum: Enumerator[C])(implicit tW: ETFConverter[T], cC: ETFConverter[C]): Task[ClientResult] = {
    val cmd = Request.Process(Atom(module), Atom(functionName), tW.write(args))
    val stream = enum.map(x ⇒ Request.RequestChunk(cC.write(x)).asInstanceOf[NucleusMessage]) andThen Enumerator(Request.RequestChunkTerminator().asInstanceOf[NucleusMessage])
    (client.sendStream(cmd, stream) flatMap handleResponse) |> handleException
  }

  def ?(): Task[ClientResult] = call()

  def ?[T <: Product](args: T)(implicit tW: ETFConverter[T]): Task[ClientResult] = call(args)

  def ?[T](args: T)(implicit nst: T <:!< Product, tW: ETFConverter[Tuple1[T]]): Task[ClientResult] = call(args)

  def ![T <: Product](args: T)(implicit tW: ETFConverter[T]): Task[Unit] = cast(args)

  def ![T](args: T)(implicit nst: T <:!< Product, tW: ETFConverter[Tuple1[T]]): Task[Unit] = cast(args)

  def ?->>[T <: Product](args: T)(implicit tW: ETFConverter[T]): Task[ClientStreamResult] = stream(args)

  def ?->>[T](args: T)(implicit nst: T <:!< Product, tW: ETFConverter[Tuple1[T]]): Task[ClientStreamResult] = stream(args)

  def ?<<-[C](enum: Enumerator[C])(implicit cC: ETFConverter[C]): Task[ClientResult] = process(enum)

  def ?<<-[T <: Product, C](args: T, enum: Enumerator[C])(implicit tW: ETFConverter[T], cC: ETFConverter[C]): Task[ClientResult] = process(args, enum)

  def ?<<-[T, C](args: T, enum: Enumerator[C])(implicit nst: T <:!< Product, tW: ETFConverter[Tuple1[T]], cC: ETFConverter[C]): Task[ClientResult] = process(args, enum)
}

class ClientModule(client: Client, name: String) {
  def |/|(functionName: String) = new CoreClientFunction(client, name, functionName)
}

class Client(host: String, port: Int, numberOfWorkers: Int, description: String, router: Router)(implicit system: ActorSystem) {
  val stages = new NucleusMessageStage >> new LengthFieldFrame(1024 * 1024 * 50) // Max 50MB messages

  val client = nl.gideondk.sentinel.Client.randomRouting(host, port, numberOfWorkers, description, stages, resolver = Processor(router), lowBytes = 1024, highBytes = 1024 * 1024, maxBufferSize = 1024 * 1024 * 10)

  def close = {
    system stop client.actor
  }

  def ask(cmd: NucleusMessage) = client ask cmd

  def askStream(cmd: NucleusMessage) = client askStream cmd

  def sendStream(cmd: NucleusMessage, enum: Enumerator[NucleusMessage]) = client.sendStream(cmd, enum)

  def module(moduleName: String) = new ClientModule(this, moduleName)

  def |?|(moduleName: String) = module(moduleName)
}

object Client {
  def apply(host: String, port: Int, numberOfWorkers: Int, description: String, modules: NucleusModules = NucleusModules(Map[Atom, Module]()))(implicit system: ActorSystem) =
    new Client(host, port, numberOfWorkers, description, new Router(modules))
}

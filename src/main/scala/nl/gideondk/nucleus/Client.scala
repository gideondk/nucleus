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
  private def handleException[T](t: Future[T]): Future[T] = t.recoverWith {
    case e: ConsumerException[NucleusMessage] ⇒ e.cause match {
      case x: Response.Error ⇒ Future.failed(NucleusException.errorToNucleusException(x))
      case _                 ⇒ Future.failed(new Exception("Received incorrect message as a error"))
    }
    case e: Throwable ⇒ Future.failed(e)
  }

  private def handleResponse: PartialFunction[NucleusMessage, Future[ClientResult]] = {
    case x: Response.Reply ⇒ Future(ClientResult(x.value))
    case x: NucleusMessage ⇒ Future.failed(new Exception("Unexpected response: expected Reply, received " + x))
  }

  private def handleStreamResponse(bse: Enumerator[NucleusMessage]) = {
    ClientStreamResult(bse &> Enumeratee.map {
      case x: Response.ReplyChunk ⇒ x.value
      case x: NucleusMessage      ⇒ throw new Exception("Internal stream exception, didn't receive stream chunk")
    })
  }

  def call(): Future[ClientResult] = {
    val cmd = Request.ArgumentLessCall(Atom(module), Atom(functionName))
    handleException(((client ask cmd) flatMap handleResponse))
  }

  def call[T](args: T)(implicit tW: ETFConverter[Tuple1[T]]): Future[ClientResult] =
    call(Tuple1(args))

  def call[T <: Product](args: T)(implicit tW: ETFConverter[T]): Future[ClientResult] = {
    val cmd = Request.Call(Atom(module), Atom(functionName), tW.write(args))
    handleException(((client ask cmd) flatMap handleResponse))
  }

  def cast[T](args: T)(implicit tW: ETFConverter[Tuple1[T]]): Future[Unit] =
    cast(Tuple1(args))

  def cast[T <: Product](args: T)(implicit tW: ETFConverter[T]): Future[Unit] = {
    val cmd = Request.Cast(Atom(module), Atom(functionName), tW.write(args))
    handleException[Unit](((client ask cmd).flatMap {
      case x: Response.NoReply ⇒ Future(())
      case x: NucleusMessage   ⇒ Future.failed(new Exception("Unexpected response: expected NoReply, received " + x))
    }))
  }

  def stream[T](args: T)(implicit tW: ETFConverter[Tuple1[T]]): Future[ClientStreamResult] =
    stream(Tuple1(args))

  def stream[T <: Product](args: T)(implicit tW: ETFConverter[T]): Future[ClientStreamResult] = {
    val cmd = Request.Stream(Atom(module), Atom(functionName), tW.write(args))
    handleException(((client askStream cmd) map handleStreamResponse))
  }

  def process[C](enum: Enumerator[C])(implicit cC: ETFConverter[C]): Future[ClientResult] = {
    val cmd = Request.Process(Atom(module), Atom(functionName), ByteString())
    val stream = enum.map(x ⇒ Request.RequestChunk(cC.write(x)).asInstanceOf[NucleusMessage]) andThen Enumerator(Request.RequestChunkTerminator().asInstanceOf[NucleusMessage])
    handleException((client.sendStream(cmd, stream) flatMap handleResponse))
  }

  def process[T, C](args: T, enum: Enumerator[C])(implicit tW: ETFConverter[Tuple1[T]], cC: ETFConverter[C]): Future[ClientResult] =
    process(Tuple1(args), enum)

  def process[T <: Product, C](args: T, enum: Enumerator[C])(implicit tW: ETFConverter[T], cC: ETFConverter[C]): Future[ClientResult] = {
    val cmd = Request.Process(Atom(module), Atom(functionName), tW.write(args))
    val stream = enum.map(x ⇒ Request.RequestChunk(cC.write(x)).asInstanceOf[NucleusMessage]) andThen Enumerator(Request.RequestChunkTerminator().asInstanceOf[NucleusMessage])
    handleException((client.sendStream(cmd, stream) flatMap handleResponse))
  }

  def ?(): Future[ClientResult] = call()

  def ?[T <: Product](args: T)(implicit tW: ETFConverter[T]): Future[ClientResult] = call(args)

  def ?[T](args: T)(implicit tW: ETFConverter[Tuple1[T]]): Future[ClientResult] = call(args)

  def ![T <: Product](args: T)(implicit tW: ETFConverter[T]): Future[Unit] = cast(args)

  def ![T](args: T)(implicit tW: ETFConverter[Tuple1[T]]): Future[Unit] = cast(args)

  def ?->>[T <: Product](args: T)(implicit tW: ETFConverter[T]): Future[ClientStreamResult] = stream(args)

  def ?->>[T](args: T)(implicit tW: ETFConverter[Tuple1[T]]): Future[ClientStreamResult] = stream(args)

  def ?<<-[C](enum: Enumerator[C])(implicit cC: ETFConverter[C]): Future[ClientResult] = process(enum)

  def ?<<-[T <: Product, C](args: T, enum: Enumerator[C])(implicit tW: ETFConverter[T], cC: ETFConverter[C]): Future[ClientResult] = process(args, enum)

  def ?<<-[T, C](args: T, enum: Enumerator[C])(implicit tW: ETFConverter[Tuple1[T]], cC: ETFConverter[C]): Future[ClientResult] = process(args, enum)
}

class ClientModule(client: Client, name: String) {
  def |/|(functionName: String) = new CoreClientFunction(client, name, functionName)
}

class Client(host: String, port: Int, numberOfWorkers: Int, description: String, router: Router)(implicit system: ActorSystem) {
  val stages = new NucleusMessageStage >> new LengthFieldFrame(1024 * 1024 * 50, lengthIncludesHeader = false) // Max 50MB messages

  val client = nl.gideondk.sentinel.Client.randomRouting(host, port, numberOfWorkers, description, stages, resolver = Processor(router), lowBytes = 1024, highBytes = 1024 * 1024, maxBufferSize = 1024 * 1024 * 10, allowPipelining = true)

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
  def apply(host: String, port: Int, numberOfWorkers: Int, description: String, modules: NucleusModules = NucleusModules(Map[Atom, Module]()), etfProtocol: ETFProtocol = ETFProtocol())(implicit system: ActorSystem) =
    new Client(host, port, numberOfWorkers, description, new Router(modules, etfProtocol))
}

package nl.gideondk.nucleus

import nl.gideondk.sentinel._

import scalaz._
import Scalaz._

import akka.util._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

import nl.gideondk.nucleus.protocol._

import shapeless.syntax.std.function._
import shapeless._
import shapeless.ops.hlist.Filter
import shapeless.ops.hlist.Tupler
import shapeless.syntax.std.product._

import play.api.libs.iteratee._

import scala.util.Try
import shapeless.ops.function.FnToProduct
import nl.gideondk.nucleus.protocol.Atom
import scala.util.Failure

import scala.util.Success

case class Processor(router: Router) extends Resolver[NucleusMessage, NucleusMessage] {
  override def process = {
    case _: Request.Call ⇒ ProducerAction.Signal(handleCallRequest)
    case _: Request.ArgumentLessCall ⇒ ProducerAction.Signal(handleArgumentlessCallRequest)
    case _: Request.Cast ⇒ ProducerAction.Signal(handleCastRequest)
    case _: Request.Stream ⇒ ProducerAction.ProduceStream(handleStreamRequest)
    case _: Request.Process ⇒ ProducerAction.ConsumeStream(handleProcessRequest)

    case _: Response.Reply ⇒ ConsumerAction.AcceptSignal
    case _: Response.NoReply ⇒ ConsumerAction.AcceptSignal
    case _: Response.Error ⇒ ConsumerAction.AcceptError

    case _: Request.RequestChunk | _: Response.ReplyChunk ⇒ ConsumerAction.ConsumeStreamChunk
    case _: Request.RequestChunkTerminator | _: Response.ReplyChunkTerminator ⇒ ConsumerAction.EndStream

  }

  def handleCallExceptions(r: Try[Future[NucleusMessage]]) = {
    (r match {
      case Success(s) ⇒ s
      case Failure(e) ⇒ e match {
        case x: NucleusException ⇒ Future(NucleusException.nucleusExceptionToError(x))
      }
    }).recover {
      case e: Throwable ⇒
        NucleusException.nucleusExceptionToError(new NucleusServerRuntimeException(e.toString, e.getStackTrace().toList.map(x ⇒ x.toString())))
    }
  }

  def handleArgumentlessCallRequest(c: Request.ArgumentLessCall): Future[NucleusMessage] = {
    (for {
      module ← router.getModule(c.module)
      function ← router.getCallFunction(module, c.functionName)
      res ← Try(function.function(ByteString()).map(Response.Reply))
    } yield res) |> handleCallExceptions
  }

  def handleCallRequest(c: Request.Call): Future[NucleusMessage] = {
    (for {
      module ← router.getModule(c.module)
      function ← router.getCallFunction(module, c.functionName)
      arguments = c.arguments
      res ← Try(function.function(arguments).map(Response.Reply))
    } yield res) |> handleCallExceptions
  }

  def handleCastRequest(c: Request.Cast): Future[NucleusMessage] = {
    (for {
      module ← router.getModule(c.module)
      function ← router.getCastFunction(module, c.functionName)
      arguments = c.arguments
      res ← Try(function.function(arguments))
    } yield Response.NoReply()) match {
      case Success(s) ⇒ Future(s)
      case Failure(e) ⇒ e match {
        case x: NucleusException ⇒ Future(NucleusException.nucleusExceptionToError(x))
      }
    }
  }

  def handleStreamExceptions(r: Try[Future[Enumerator[NucleusMessage]]]): Future[Enumerator[NucleusMessage]] = {
    (r match {
      case Success(s) ⇒ s
      case Failure(e) ⇒ e match {
        case x: NucleusException ⇒ Future(Enumerator(NucleusException.nucleusExceptionToError(x).asInstanceOf[NucleusMessage]))
      }
    }).recover {
      case e: Throwable ⇒
        Enumerator(NucleusException.nucleusExceptionToError(new NucleusServerRuntimeException(e.toString, e.getStackTrace().toList.map(x ⇒ x.toString()))))
    }
  }

  def handleStreamRequest(c: Request.Stream): Future[Enumerator[NucleusMessage]] = {
    (for {
      module ← router.getModule(c.module)
      function ← router.getStreamFunction(module, c.functionName)
      arguments = c.arguments
      res ← Try {
        val enum = function.function(arguments).map(x ⇒ (x &> Enumeratee.map(c ⇒ Response.ReplyChunk(c).asInstanceOf[NucleusMessage])))
        enum.map(_ >>> Enumerator(Response.ReplyChunkTerminator()))
      }
    } yield res) |> handleStreamExceptions
  }

  def handleProcessRequest(c: Request.Process)(chunks: Enumerator[Request.RequestChunk]): Future[NucleusMessage] = {
    (for {
      module ← router.getModule(c.module)
      function ← router.getProcessFunction(module, c.functionName)
      arguments = c.arguments
      res ← Try { function.function(arguments)(chunks.map(x ⇒ x.value)).map(Response.Reply) }
    } yield res) |> handleCallExceptions
  }
}

class Router(modules: NucleusModules, etfProtocol: ETFProtocol = ETFProtocol()) {
  import etfProtocol._

  def checkHeader(iter: ByteIterator): Try[Unit] = {
    try {
      HeaderFunctions.checkMagic(iter.getByte)
      HeaderFunctions.checkSignature(ETFTypes.SMALL_TUPLE, iter.getByte)
      Success(())
    } catch {
      case e: Throwable ⇒ Failure(new NucleusProtocolHeaderException(e.getMessage, e.getStackTrace().toList.map(x ⇒ x.toString())))
    }
  }

  def parseRequestType(iter: ByteIterator): Try[Atom] = {
    val size = iter.getByte
    if (size < 3 || size > 5) Failure(new NucleusProtocolHeaderException("Incorrect tuple size"))
    else Try(AtomConverter.readFromIterator(iter))
  }

  def getModule(moduleName: Atom): Try[Module] =
    modules.modules.get(moduleName).map(Success(_)).getOrElse(Failure(new NucleusServerIncorrectModuleException("Module: " + moduleName + " isn't available on the server")))

  def getCallFunction(module: Module, functionName: Atom): Try[Call] =
    module.funcs.calls.get(functionName).map(Success(_)).getOrElse(Failure(new NucleusServerIncorrectFunctionException("Function: " + functionName + " isn't available in module: " + module.name)))

  def getCastFunction(module: Module, functionName: Atom): Try[Cast] =
    module.funcs.casts.get(functionName).map(Success(_)).getOrElse(Failure(new NucleusServerIncorrectFunctionException("Function: " + functionName + " isn't available in module: " + module.name)))

  def getStreamFunction(module: Module, functionName: Atom): Try[Stream] =
    module.funcs.streamFunctions.get(functionName).map(Success(_)).getOrElse(Failure(new NucleusServerIncorrectFunctionException("Function: " + functionName + " isn't available in module: " + module.name)))

  def getProcessFunction(module: Module, functionName: Atom): Try[Process] =
    module.funcs.processFunctions.get(functionName).map(Success(_)).getOrElse(Failure(new NucleusServerIncorrectFunctionException("Function: " + functionName + " isn't available in module: " + module.name)))
}

import protocol.ETF._
trait CallBuilder extends {
  def name: Atom

  def apply[R](f: ⇒ Function0[Future[R]])(implicit writer: ETFWriter[R]) =
    NucleusFunction.call(name)((bs: ByteString) ⇒ f().map(writer.write))

  def apply[F, FO, A <: HList, R](f: ⇒ F)(implicit h: FnToProduct.Aux[F, FO], ev: FO <:< (A ⇒ Future[R]), reader: ETFReader[A], writer: ETFWriter[R]) =
    NucleusFunction.call(name)((args: ByteString) ⇒ f.toProduct(reader.read(args)).map(writer.write))

}

trait CastBuilder {
  def name: Atom

  def apply[F, FO, A <: HList](f: ⇒ F)(implicit h: FnToProduct.Aux[F, FO], ev: FO <:< (A ⇒ Unit), reader: ETFReader[A]) =
    NucleusFunction.cast(name)((args: ByteString) ⇒ f.toProduct(reader.read(args)))
}

trait StreamBuilder {
  def name: Atom

  def apply[R](f: ⇒ Function0[Future[Enumerator[R]]])(implicit writer: ETFWriter[R]) =
    NucleusFunction.stream(name)((args: ByteString) ⇒ f().map(x ⇒ x &> Enumeratee.map[R](writer.write(_))))

  def apply[F, FO, A <: HList, R](f: ⇒ F)(implicit h: FnToProduct.Aux[F, FO], ev: FO <:< (A ⇒ Future[Enumerator[R]]), reader: ETFReader[A], writer: ETFWriter[R]) =
    NucleusFunction.stream(name)((args: ByteString) ⇒ f.toProduct(reader.read(args)).map(x ⇒ x &> Enumeratee.map[R](writer.write)))

}

trait ProcessBuilder {
  def name: Atom

  def apply[R, C](f: ⇒ Function0[Enumerator[C] ⇒ Future[R]])(implicit writer: ETFWriter[R], chunkReader: ETFReader[C]) =
    NucleusFunction.process(name)((args: ByteString) ⇒ (chunks: Enumerator[ByteString]) ⇒ f()(chunks &> Enumeratee.map(chunkReader.read)).map(writer.write))

  def apply[F, FO, A <: HList, C, R](f: ⇒ F)(implicit h: FnToProduct.Aux[F, FO], ev: FO <:< (A ⇒ Enumerator[C] ⇒ Future[R]), reader: ETFReader[A], chunkReader: ETFReader[C], writer: ETFWriter[R]) =
    NucleusFunction.process(name)((args: ByteString) ⇒ (chunks: Enumerator[ByteString]) ⇒ (f.toProduct(reader.read(args))(chunks &> Enumeratee.map(chunkReader.read))).map(writer.write))

}

trait Routing {
  import Module._

  def call(n: String) = new CallBuilder {
    val name = Atom(n)
  }

  def cast(n: String) = new CastBuilder {
    val name = Atom(n)
  }

  def stream(n: String) = new StreamBuilder {
    val name = Atom(n)
  }

  def process(n: String) = new ProcessBuilder {
    val name = Atom(n)
  }
}

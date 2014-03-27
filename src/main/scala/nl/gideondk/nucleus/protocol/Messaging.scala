package nl.gideondk.nucleus.protocol

import shapeless._

import akka.util.{ ByteIterator, ByteStringBuilder, ByteString }

import ETFTypes._

import akka.io.{ SymmetricPipePair, SymmetricPipelineStage, PipelineContext }
import nl.gideondk.nucleus.protocol.HeaderFunctions._

sealed trait NucleusMessage

sealed trait ArgumentLessRequest extends NucleusMessage {
  def module: Atom
  def functionName: Atom
}

sealed trait Request extends ArgumentLessRequest {
  def module: Atom
  def functionName: Atom
  def arguments: ByteString
}

sealed trait Response extends NucleusMessage

sealed trait FailedResponse extends Response

object Request {
  /* 
   * Argument less call: Send a request to a server, waiting for a immediate (blocking) response, best used in CPU bound services. 
   * 
   * (`call, `generation, `generateId))
   * 
   */
  case class ArgumentLessCall(module: Atom, functionName: Atom) extends ArgumentLessRequest

  /* 
   * Call: Send a request to a server, waiting for a immediate (blocking) response, best used in CPU bound services. 
   * 
   * (`call, `persons, `fetch, ("gideondk")))
   * 
   */
  case class Call(module: Atom, functionName: Atom, arguments: ByteString) extends Request

  /* 
   * Cast: Send a request to a server, waiting for a immediate reply but not a response (fire and forget)
   * 
   * (`cast, `persons, `remove, ("gideondk")))
   *   
   */
  case class Cast(module: Atom, functionName: Atom, arguments: ByteString) extends Request

  /*
   * Stream: Send a request to a server, waiting for a stream of responses, best used for subscriptions / larger payloads
   *
   * (`stream, `log, ("eth1")))
   *
   */
  case class Stream(module: Atom, functionName: Atom, arguments: ByteString) extends Request

  /*
   * Process: Send a request to a server, pushing a stream of chunks afterwards, used for processing of larger payloads or co-subscriptions
   *
   * (`process, `log, ("eth1")))
   *
   */
  case class Process(module: Atom, functionName: Atom, arguments: ByteString) extends Request

  /*
   * RequestChunk: Send to the server in case of a chunked request
   *
   * (`requestchunk, (<BINARY>))
   *
   * */
  case class RequestChunk(value: ByteString) extends NucleusMessage

  /*
   * RequestChunkTerminator: Send to the server in case of a chunked request
   *
   * (`requestchunkterminator)
   *
   * */
  case class RequestChunkTerminator() extends NucleusMessage
}

object Response {
  /*
   * Reply: Send back to the client with the resulting value
   *
   * (`reply, ("Gideon", "de Kok"))
   *
   * */
  case class Reply(value: ByteString) extends Response

  /*
   * NoReply: Send back to the client in case of a "cast" request
   *
   * (`noreply)
   *
   * */
  case class NoReply() extends Response

  /*
   * ReplyChunk: Send back to the client in case of a chunked reply
   *
   * (`replychunk, (<BINARY>))
   *
   * */
  case class ReplyChunk(value: ByteString) extends Response

  /*
   * ReplyChunkTerminator: Send to the server in case of a chunked request
   *
   * (`replychunkterminator)
   *
   * */
  case class ReplyChunkTerminator() extends Response

  /* 
   * Error: Send back to the client in case of an error
   * 
   * Error types: protocol, server, user, and proxy (BERT-RPC style)
   * ('error, (`server, 2, "UnknownFunction", "function 'collect' not found on module 'logs'", [""]))
   * 
   * */
  case class Error(errorType: Atom, errorCode: Int, errorClass: String, errorDetail: String, backtrace: List[String]) extends FailedResponse
}

object NucleusMessaging extends ETFConverters with ProductConverters {
  import HeaderFunctions._

  trait HasByteOrder extends PipelineContext {
    def byteOrder: java.nio.ByteOrder
  }

  implicit def callConverter = new ETFConverter[Request.Call] {
    def write(o: Request.Call) = {
      val builder = new ByteStringBuilder
      builder.putByte(SMALL_TUPLE)
      builder.putByte(4.toByte)

      builder ++= AtomConverter.write(Atom("call"))
      builder ++= AtomConverter.write(o.module)
      builder ++= AtomConverter.write(o.functionName)
      builder ++= o.arguments

      builder.result
    }

    def readFromIterator(iter: ByteIterator): Request.Call = {
      checkSignature(SMALL_TUPLE, iter.getByte)
      val size = iter.getByte
      val funType = AtomConverter.readFromIterator(iter)
      if (funType != Atom("call")) throw new Exception("Request is not of the 'call' type")

      val module = AtomConverter.readFromIterator(iter)
      val function = AtomConverter.readFromIterator(iter)

      Request.Call(module, function, iter.toByteString)
    }
  }

  implicit def argumentLessCallConverter = new ETFConverter[Request.ArgumentLessCall] {
    def write(o: Request.ArgumentLessCall) = {
      val builder = new ByteStringBuilder
      builder.putByte(SMALL_TUPLE)
      builder.putByte(3.toByte)

      builder ++= AtomConverter.write(Atom("call"))
      builder ++= AtomConverter.write(o.module)
      builder ++= AtomConverter.write(o.functionName)

      builder.result
    }

    def readFromIterator(iter: ByteIterator): Request.ArgumentLessCall = {
      checkSignature(SMALL_TUPLE, iter.getByte)
      val size = iter.getByte
      val funType = AtomConverter.readFromIterator(iter)
      if (funType != Atom("call")) throw new Exception("Request is not of the 'call' type")

      val module = AtomConverter.readFromIterator(iter)
      val function = AtomConverter.readFromIterator(iter)

      Request.ArgumentLessCall(module, function)
    }
  }

  implicit def castConverter = new ETFConverter[Request.Cast] {
    def write(o: Request.Cast) = {
      val builder = new ByteStringBuilder
      builder.putByte(SMALL_TUPLE)
      builder.putByte(4.toByte)

      builder ++= AtomConverter.write(Atom("cast"))
      builder ++= AtomConverter.write(o.module)
      builder ++= AtomConverter.write(o.functionName)
      builder ++= o.arguments

      builder.result
    }

    def readFromIterator(iter: ByteIterator): Request.Cast = {
      checkSignature(SMALL_TUPLE, iter.getByte)
      val size = iter.getByte
      val funType = AtomConverter.readFromIterator(iter)
      if (funType != Atom("cast")) throw new Exception("Request is not of the 'stream' type")

      val module = AtomConverter.readFromIterator(iter)
      val function = AtomConverter.readFromIterator(iter)

      Request.Cast(module, function, iter.toByteString)
    }
  }

  implicit def streamCallConverter = new ETFConverter[Request.Stream] {
    def write(o: Request.Stream) = {
      val builder = new ByteStringBuilder
      builder.putByte(SMALL_TUPLE)
      builder.putByte(4.toByte)

      builder ++= AtomConverter.write(Atom("stream"))
      builder ++= AtomConverter.write(o.module)
      builder ++= AtomConverter.write(o.functionName)
      builder ++= o.arguments

      builder.result
    }

    def readFromIterator(iter: ByteIterator): Request.Stream = {
      checkSignature(SMALL_TUPLE, iter.getByte)
      val size = iter.getByte
      val funType = AtomConverter.readFromIterator(iter)
      if (funType != Atom("stream")) throw new Exception("Request is not of the 'stream' type")

      val module = AtomConverter.readFromIterator(iter)
      val function = AtomConverter.readFromIterator(iter)

      Request.Stream(module, function, iter.toByteString)
    }
  }

  implicit def ProcessCallConverter = new ETFConverter[Request.Process] {
    def write(o: Request.Process) = {
      val builder = new ByteStringBuilder
      builder.putByte(SMALL_TUPLE)
      builder.putByte(4.toByte)

      builder ++= AtomConverter.write(Atom("process"))
      builder ++= AtomConverter.write(o.module)
      builder ++= AtomConverter.write(o.functionName)
      builder ++= o.arguments

      builder.result
    }

    def readFromIterator(iter: ByteIterator): Request.Process = {
      checkSignature(SMALL_TUPLE, iter.getByte)
      val size = iter.getByte
      val funType = AtomConverter.readFromIterator(iter)
      if (funType != Atom("process")) throw new Exception("Request is not of the 'process' type")

      val module = AtomConverter.readFromIterator(iter)
      val function = AtomConverter.readFromIterator(iter)

      Request.Process(module, function, iter.toByteString)
    }
  }

  implicit def requestChunkConverter = new ETFConverter[Request.RequestChunk] {
    def write(o: Request.RequestChunk) = {
      val builder = new ByteStringBuilder
      builder.putByte(SMALL_TUPLE)
      builder.putByte(2.toByte)

      builder ++= AtomConverter.write(Atom("requestchunk"))
      builder ++= o.value
      builder.result
    }

    def readFromIterator(iter: ByteIterator): Request.RequestChunk = {
      checkSignature(SMALL_TUPLE, iter.getByte)
      val size = iter.getByte
      val v1 = AtomConverter.readFromIterator(iter)
      if (v1 != Atom("requestchunk")) throw new Exception("Request is not of the 'request chunk' type")
      Request.RequestChunk(iter.toByteString)
    }
  }

  implicit def requestChunkTerminatorConverter = new ETFConverter[Request.RequestChunkTerminator] {
    def write(o: Request.RequestChunkTerminator) = {
      tuple1Converter[Atom].write(Tuple1(Atom("requestchunkterminator")))
    }

    def readFromIterator(iter: ByteIterator): Request.RequestChunkTerminator = {
      val tpl = tuple1Converter[Atom].readFromIterator(iter)
      if (tpl._1 != Atom("requestchunkterminator")) throw new Exception("Request is not of the 'requestchunkterminator' type")
      Request.RequestChunkTerminator()
    }
  }

  /* Response */

  implicit def replyConverter = new ETFConverter[Response.Reply] {
    def write(o: Response.Reply) = {
      val builder = new ByteStringBuilder
      builder.putByte(SMALL_TUPLE)
      builder.putByte(2.toByte)

      builder ++= AtomConverter.write(Atom("reply"))
      builder ++= o.value
      builder.result
    }

    def readFromIterator(iter: ByteIterator): Response.Reply = {
      checkSignature(SMALL_TUPLE, iter.getByte)
      val size = iter.getByte
      val v1 = AtomConverter.readFromIterator(iter)
      if (v1 != Atom("reply")) throw new Exception("Response is not of the 'reply' type")
      Response.Reply(iter.toByteString)
    }
  }

  implicit def replyChunkConverter = new ETFConverter[Response.ReplyChunk] {
    def write(o: Response.ReplyChunk) = {
      val builder = new ByteStringBuilder
      builder.putByte(SMALL_TUPLE)
      builder.putByte(3.toByte)

      builder ++= AtomConverter.write(Atom("replychunk"))
      builder ++= o.value
      builder.result
    }

    def readFromIterator(iter: ByteIterator): Response.ReplyChunk = {
      checkSignature(SMALL_TUPLE, iter.getByte)
      val size = iter.getByte
      val v1 = AtomConverter.readFromIterator(iter)
      if (v1 != Atom("replychunk")) throw new Exception("Response is not of the 'reply' type")
      Response.ReplyChunk(iter.toByteString)
    }
  }

  implicit def replyChunkTerminatorConverter = new ETFConverter[Response.ReplyChunkTerminator] {
    def write(o: Response.ReplyChunkTerminator) = {
      tuple1Converter[Atom].write(Tuple1(Atom("replychunkterminator")))
    }

    def readFromIterator(iter: ByteIterator): Response.ReplyChunkTerminator = {
      val tpl = tuple1Converter[Atom].readFromIterator(iter)
      if (tpl._1 != Atom("replychunkterminator")) throw new Exception("Response is not of the 'replychunkterminator' type")
      Response.ReplyChunkTerminator()
    }
  }

  implicit def noReplyConverter = new ETFConverter[Response.NoReply] {
    def write(o: Response.NoReply) = {
      tuple1Converter[Atom].write(Tuple1(Atom("noreply")))
    }

    def readFromIterator(iter: ByteIterator): Response.NoReply = {
      val tpl = tuple1Converter[Atom].readFromIterator(iter)
      if (tpl._1 != Atom("noreply")) throw new Exception("Response is not of the 'noreply' type")
      Response.NoReply()
    }
  }

  implicit def errorConverter = new ETFConverter[Response.Error] {
    def write(o: Response.Error) = {
      tuple2Converter[Atom, Tuple5[Atom, Int, String, String, List[String]]].write((Atom("error"), (o.errorType, o.errorCode, o.errorClass, o.errorDetail, o.backtrace)))
    }

    def readFromIterator(iter: ByteIterator): Response.Error = {
      val tpl = tuple2Converter[Atom, (Atom, Int, String, String, List[String])].readFromIterator(iter)
      Response.Error(tpl._2._1, tpl._2._2, tpl._2._3, tpl._2._4, tpl._2._5)
    }
  }
}

class NucleusMessageStage(etfProtocol: ETFProtocol = ETFProtocol()) extends SymmetricPipelineStage[PipelineContext, NucleusMessage, ByteString] {
  import NucleusMessaging._

  override def apply(ctx: PipelineContext) = new SymmetricPipePair[NucleusMessage, ByteString] {
    implicit val byteOrder = java.nio.ByteOrder.BIG_ENDIAN

    override val commandPipeline = {
      msg: NucleusMessage ⇒
        {
          Seq(Right(msg match { // Yikes
            case x: Request.Call                   ⇒ etfProtocol.toETF(x)
            case x: Request.ArgumentLessCall       ⇒ etfProtocol.toETF(x)
            case x: Request.Cast                   ⇒ etfProtocol.toETF(x)
            case x: Request.Stream                 ⇒ etfProtocol.toETF(x)
            case x: Request.Process                ⇒ etfProtocol.toETF(x)
            case x: Request.RequestChunk           ⇒ etfProtocol.toETF(x)
            case x: Request.RequestChunkTerminator ⇒ etfProtocol.toETF(x)
            case x: Response.Reply                 ⇒ etfProtocol.toETF(x)
            case x: Response.NoReply               ⇒ etfProtocol.toETF(x)
            case x: Response.ReplyChunk            ⇒ etfProtocol.toETF(x)
            case x: Response.ReplyChunkTerminator  ⇒ etfProtocol.toETF(x)
            case x: Response.Error                 ⇒ etfProtocol.toETF(x)
          }))
        }
    }

    override val eventPipeline = {
      bs: ByteString ⇒
        val iter = bs.iterator
        checkMagic(iter.getByte)
        checkSignature(SMALL_TUPLE, iter.getByte)
        val size = iter.getByte
        val s = AtomConverter.readFromIterator(iter) match {
          case Atom("call") ⇒ size match {
            case 3 ⇒ etfProtocol.fromETF[Request.ArgumentLessCall](bs)
            case 4 ⇒ etfProtocol.fromETF[Request.Call](bs)
          }
          case Atom("cast")                   ⇒ etfProtocol.fromETF[Request.Cast](bs)
          case Atom("stream")                 ⇒ etfProtocol.fromETF[Request.Stream](bs)
          case Atom("process")                ⇒ etfProtocol.fromETF[Request.Process](bs)
          case Atom("requestchunk")           ⇒ etfProtocol.fromETF[Request.RequestChunk](bs)
          case Atom("requestchunkterminator") ⇒ etfProtocol.fromETF[Request.RequestChunkTerminator](bs)
          case Atom("reply")                  ⇒ etfProtocol.fromETF[Response.Reply](bs)
          case Atom("noreply")                ⇒ etfProtocol.fromETF[Response.NoReply](bs)
          case Atom("replychunk")             ⇒ etfProtocol.fromETF[Response.ReplyChunk](bs)
          case Atom("replychunkterminator")   ⇒ etfProtocol.fromETF[Response.ReplyChunkTerminator](bs)
          case Atom("error")                  ⇒ etfProtocol.fromETF[Response.Error](bs)
        }
        Seq(Left(s.get))
    }
  }
}
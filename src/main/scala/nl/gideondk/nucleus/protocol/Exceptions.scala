package nl.gideondk.nucleus.protocol

class NucleusException(val errorType: Atom, val errorCode: Int, val errorClass: String, val detail: String, val stackTrace: List[String]) extends Exception(detail)

class NucleusProtocolException(errorCode: Int, errorClass: String, detail: String, stackTrace: List[String]) extends NucleusException(Atom("protocol"), errorCode, errorClass, detail, stackTrace)

class NucleusProtocolHeaderException(detail: String, stackTrace: List[String] = List[String]()) extends NucleusProtocolException(1, "NucleusProtocolHeaderException", detail, stackTrace)

class NucleusProtocolDataException(detail: String, stackTrace: List[String] = List[String]()) extends NucleusProtocolException(2, "NucleusProtocolDataException", detail, stackTrace)

class NucleusServerException(errorCode: Int, errorClass: String, detail: String, stackTrace: List[String]) extends NucleusException(Atom("server"), errorCode, errorClass, detail, stackTrace)

class NucleusServerIncorrectModuleException(detail: String) extends NucleusServerException(1, "NucleusServerIncorrectModuleException", detail, List[String]())

class NucleusServerIncorrectFunctionException(detail: String) extends NucleusServerException(2, "NucleusServerIncorrectFunctionException", detail, List[String]())

class NucleusServerRuntimeException(detail: String, stackTrace: List[String]) extends NucleusServerException(3, "NucleusServerRuntimeException", detail, stackTrace)

object NucleusException {
  implicit def nucleusExceptionToError(e: NucleusException): Response.Error = Response.Error(e.errorType, e.errorCode, e.errorClass, e.detail, e.stackTrace)

  implicit def errorToNucleusException(e: Response.Error) = upcastException(new NucleusException(e.errorType, e.errorCode, e.errorClass, e.errorDetail, e.backtrace))

  def upcastException(e: NucleusException) = {
    e.errorType match {
      case Atom("protocol") ⇒
        upcastProtocolException(new NucleusProtocolException(e.errorCode, e.errorClass, e.detail, e.stackTrace))
      case Atom("server") ⇒
        upcastServerException(new NucleusServerException(e.errorCode, e.errorClass, e.detail, e.stackTrace))
    }
  }

  def upcastProtocolException(e: NucleusProtocolException) = e.errorCode match {
    case 1 ⇒ new NucleusProtocolHeaderException(e.detail, e.stackTrace)
    case 2 ⇒ new NucleusProtocolDataException(e.detail, e.stackTrace)
  }

  def upcastServerException(e: NucleusServerException) = e.errorCode match {
    case 1 ⇒ new NucleusServerIncorrectModuleException(e.detail)
    case 2 ⇒ new NucleusServerIncorrectFunctionException(e.detail)
    case 3 ⇒ new NucleusServerRuntimeException(e.detail, e.stackTrace)
  }
}
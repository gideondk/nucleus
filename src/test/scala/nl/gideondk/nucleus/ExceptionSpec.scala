package nl.gideondk.nucleus

import org.specs2.mutable.Specification
import scalaz._
import Scalaz._
import scala.concurrent.duration._
import nl.gideondk.nucleus.protocol._
import ETF._

class ExceptionSpec extends Specification with Routing {

  import Workers._

  sequential
  implicit val duration = Duration(10, SECONDS)

  "A Client" should {
    "correctly receive a error for a unknown module" in {
      val reqA = ((client |?| "nonexisting" |/| "add") ? (1, 4))
      val resA = reqA.map(x ⇒ x.as[Int])
      resA.run.get must throwA[NucleusServerIncorrectModuleException]
    }

    "correctly receive a error for a unknown function" in {
      val reqA = ((client |?| "calc" |/| "nonexisting") ? (1, 4))
      val resA = reqA.map(x ⇒ x.as[Int])
      resA.run.get must throwA[NucleusServerIncorrectFunctionException]
    }

    "correctly receive a runtime error when one occurs" in {
      val reqA = ((client |?| "exception" |/| "generate_exception") ? ())
      val resA = reqA.map(x ⇒ x.as[Int]).run
      resA.get must throwA[NucleusServerRuntimeException]
    }
  }
}

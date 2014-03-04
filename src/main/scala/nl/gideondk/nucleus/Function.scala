package nl.gideondk.nucleus

import akka.util.ByteString
import scala.concurrent.Future

import play.api.libs.iteratee._

import nl.gideondk.nucleus.protocol._
import shapeless._
import HList._

case class NucleusFunctions[T <: HList](functions: T) {
  def ~(function: Call)(implicit prepend: Prepend[T, Call :: HNil]) =
    NucleusFunctions(functions :+ function)

  def ~(function: Cast)(implicit prepend: Prepend[T, Cast :: HNil]) =
    NucleusFunctions(functions :+ function)

  def ~(function: Stream)(implicit prepend: Prepend[T, Stream :: HNil]) =
    NucleusFunctions(functions :+ function)

  def ~(function: Process)(implicit prepend: Prepend[T, Process :: HNil]) =
    NucleusFunctions(functions :+ function)
}

private object NucleusFunction {
  implicit def nucleusCallFunctiontoETFFunctions(m: Call): NucleusFunctions[Call :: HNil] = NucleusFunctions(m :: HNil)
  implicit def nucleusCastFunctiontoETFFunctions(m: Cast): NucleusFunctions[Cast :: HNil] = NucleusFunctions(m :: HNil)
  implicit def nucleusStreamFunctiontoETFFunctions(m: Stream): NucleusFunctions[Stream :: HNil] = NucleusFunctions(m :: HNil)
  implicit def nucleusProcessFunctiontoETFFunctions(m: Process): NucleusFunctions[Process :: HNil] = NucleusFunctions(m :: HNil)

  def call(n: Atom)(f: ByteString ⇒ Future[ByteString]) = Call(n, f)

  def cast(n: Atom)(f: ByteString ⇒ Unit) = Cast(n, f)

  def stream(n: Atom)(f: ByteString ⇒ Future[Enumerator[ByteString]]) = Stream(n, f)

  def process(n: Atom)(f: ByteString ⇒ Enumerator[ByteString] ⇒ Future[ByteString]) = Process(n, f)
}

trait NucleusFunction[A, B] {
  def name: Atom

  def function: A ⇒ B
}

case class Call(name: Atom, function: ByteString ⇒ Future[ByteString]) extends NucleusFunction[ByteString, Future[ByteString]]

case class Cast(name: Atom, function: ByteString ⇒ Unit) extends NucleusFunction[ByteString, Unit]

case class Stream(name: Atom, function: ByteString ⇒ Future[Enumerator[ByteString]]) extends NucleusFunction[ByteString, Future[Enumerator[ByteString]]]

case class Process(name: Atom, function: ByteString ⇒ Enumerator[ByteString] ⇒ Future[ByteString]) extends NucleusFunction[ByteString, Enumerator[ByteString] ⇒ Future[ByteString]]

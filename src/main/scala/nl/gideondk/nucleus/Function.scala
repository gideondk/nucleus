package nl.gideondk.nucleus

import akka.util.ByteString
import scala.concurrent.Future

import play.api.libs.iteratee._

import nl.gideondk.nucleus.protocol._

case class NucleusFunctions(calls: Map[Atom, Call] = Map[Atom, Call](), casts: Map[Atom, Cast] = Map[Atom, Cast](), streamFunctions: Map[Atom, Stream] = Map[Atom, Stream](), processFunctions: Map[Atom, Process] = Map[Atom, Process]()) {
  def ~(function: Call) =
    this.copy(calls = this.calls + (function.name -> function))

  def ~(function: Cast) =
    this.copy(casts = this.casts + (function.name -> function))

  def ~(function: Stream) =
    this.copy(streamFunctions = this.streamFunctions + (function.name -> function))

  def ~(function: Process) =
    this.copy(processFunctions = this.processFunctions + (function.name -> function))
}

private object NucleusFunction {
  implicit def nucleusFunctionToNucleusFunctions[A, B](f: NucleusFunction[A, B]) = f match {
    case x: Call    ⇒ NucleusFunctions(calls = Map(f.name -> x))
    case x: Cast    ⇒ NucleusFunctions(casts = Map(f.name -> x))
    case x: Stream  ⇒ NucleusFunctions(streamFunctions = Map(f.name -> x))
    case x: Process ⇒ NucleusFunctions(processFunctions = Map(f.name -> x))
  }

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

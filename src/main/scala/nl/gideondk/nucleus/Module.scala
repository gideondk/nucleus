package nl.gideondk.nucleus

import shapeless._
import LUBConstraint._
import HList._
import ops.hlist.{ Filter, ToList }
import nl.gideondk.nucleus.protocol.Atom

trait Module {
  def name: Atom

  def funcs: NucleusFunctionHolder
}

case class NucleusModules(modules: Map[Atom, Module]) {
  def ~(module: Module) = NucleusModules(modules + (module.name -> module))
}

case class NucleusFunctionHolder(calls: Map[Atom, Call], casts: Map[Atom, Cast], streamFunctions: Map[Atom, Stream], processFunctions: Map[Atom, Process])

object Module {
  implicit def nucleusModuletoETFModules(m: Module): NucleusModules = NucleusModules(Map(m.name -> m))

  def apply[T <: HList: <<:[NucleusFunction[_, _]]#λ, A <: HList, B <: HList, C <: HList, D <: HList](n: String)(fs: NucleusFunctions[T])(implicit callFilter: Filter.Aux[T, Call, A],
                                                                                                                                          castFilter: Filter.Aux[T, Cast, B],
                                                                                                                                          streamFilter: Filter.Aux[T, Stream, C],
                                                                                                                                          processFilter: Filter.Aux[T, Process, D],
                                                                                                                                          tl: ToList[A, Call],
                                                                                                                                          tl2: ToList[B, Cast],
                                                                                                                                          tl3: ToList[C, Stream],
                                                                                                                                          tl4: ToList[D, Process]) =
    new Module {
      val name = Atom(n)
      val funcs = {
        val calls = callFilter(fs.functions)
        val casts = castFilter(fs.functions)
        val streamFunctions = streamFilter(fs.functions)
        val processFunctions = processFilter(fs.functions)
        NucleusFunctionHolder(calls.toList[Call].map(x ⇒ x.name -> x).toMap, casts.toList[Cast].map(x ⇒ x.name -> x).toMap, streamFunctions.toList[Stream].map(x ⇒ x.name -> x).toMap, processFunctions.toList[Process].map(x ⇒ x.name -> x).toMap)
      }
    }
}
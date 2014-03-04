package nl.gideondk.nucleus

import shapeless._
import LUBConstraint._
import HList._
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

  def apply[T <: HList: <<:[NucleusFunction[_, _]]#λ, A <: HList, B <: HList, C <: HList, D <: HList](n: String)(fs: NucleusFunctions[T])(implicit callFilter: FilterAux[T, Call, A],
                                                                                                                                          castFilter: FilterAux[T, Cast, B],
                                                                                                                                          streamFilter: FilterAux[T, Stream, C],
                                                                                                                                          processFilter: FilterAux[T, Process, D],
                                                                                                                                          tl: ToList[A, Call],
                                                                                                                                          tl2: ToList[B, Cast],
                                                                                                                                          tl3: ToList[C, Stream],
                                                                                                                                          tl4: ToList[D, Process]) =
    new Module {
      val name = Atom(n)
      val funcs = {
        val calls = fs.functions.filter[Call]
        val casts = fs.functions.filter[Cast]
        val streamFunctions = fs.functions.filter[Stream]
        val processFunctions = fs.functions.filter[Process]
        NucleusFunctionHolder(calls.toList.map(x ⇒ x.name -> x).toMap, casts.toList.map(x ⇒ x.name -> x).toMap, streamFunctions.toList.map(x ⇒ x.name -> x).toMap, processFunctions.toList.map(x ⇒ x.name -> x).toMap)
      }
    }
}
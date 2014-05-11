package nl.gideondk.nucleus

import nl.gideondk.nucleus.protocol.Atom

trait Module {
  def name: Atom

  def funcs: NucleusFunctions
}

case class NucleusModules(modules: Map[Atom, Module]) {
  def ~(module: Module) = NucleusModules(modules + (module.name -> module))
}

object Module {
  implicit def nucleusModuletoETFModules(m: Module): NucleusModules = NucleusModules(Map(m.name -> m))

  def apply(n: String)(fs: NucleusFunctions) =
    new Module {
      val name = Atom(n)
      val funcs = fs
    }
}
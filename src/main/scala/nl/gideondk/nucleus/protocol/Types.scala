package nl.gideondk.nucleus.protocol

import akka.util.ByteString

case class LargeTuple(elements: Seq[Any]) extends Product {
  override def productElement(n: Int) = elements(n)

  override def productArity = elements.size

  override def canEqual(other: Any): Boolean = {
    other match {
      case o: LargeTuple ⇒ o.elements == elements
      case _             ⇒ false
    }
  }

  override def equals(other: Any) = canEqual(other)
}

object ETFTypes {
  val SMALL_INT: Byte = 97
  val INT: Byte = 98
  val SMALL_BIGNUM: Byte = 110
  val LARGE_BIGNUM: Byte = 111
  val FLOAT: Byte = 99
  val ATOM: Byte = 100
  val SMALL_TUPLE: Byte = 104
  val LARGE_TUPLE: Byte = 105
  val NIL: Byte = 106
  val STRING: Byte = 107
  val LIST: Byte = 108
  val BIN: Byte = 109
  val FUN: Byte = 117
  val NEW_FUN: Byte = 112
  val MAGIC: Byte = 131.asInstanceOf[Byte]

  val MAX_INT: Byte = ((1 << 27) - 1).asInstanceOf[Byte]
  val MIN_INT: Byte = (-(1 << 27)).asInstanceOf[Byte]

  val ZERO: Byte = 0

  val FLOAT_LENGTH: Byte = 31
}
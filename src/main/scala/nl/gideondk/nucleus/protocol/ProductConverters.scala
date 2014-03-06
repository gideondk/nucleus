package nl.gideondk.nucleus.protocol

import akka.util.{ ByteStringBuilder, ByteString, ByteIterator }
import akka.actor._

import HeaderFunctions._
import ETFTypes._
import shapeless._
import ops.hlist.Length

trait ProductConverters {
  trait HListTailConverter[A] {
    def writeUnpadded(o: A): ByteString
    def readUnpaddedFromIterator(iter: ByteIterator): A
  }

  implicit def hListNilTailConverter = new HListTailConverter[HNil] {
    def writeUnpadded(o: HNil) = {
      ByteString()
    }

    def readUnpaddedFromIterator(iter: ByteIterator) =
      HNil
  }

  implicit def hListNilConverter = new ETFConverter[HNil] {
    def write(o: HNil) = {
      ByteString()
    }

    def readFromIterator(iter: ByteIterator) =
      HNil
  }

  implicit def hListConverter[H: ETFConverter, T <: HList: HListTailConverter] = new ETFConverter[H :: T] {
    def write(o: H :: T): ByteString = {
      val head :: tail = o

      val builder = new ByteStringBuilder

      builder.putByte(SMALL_TUPLE)
      builder.putByte(o.runtimeLength.toByte)

      builder ++= implicitly[ETFConverter[H]].write(head)
      builder ++= implicitly[HListTailConverter[T]].writeUnpadded(tail)
      builder.result

    }

    def readFromIterator(iter: ByteIterator): H :: T = {
      checkSignature(SMALL_TUPLE, iter.getByte)
      val size = iter.getByte
      implicitly[ETFConverter[H]].readFromIterator(iter) :: implicitly[HListTailConverter[T]].readUnpaddedFromIterator(iter)
    }
  }

  implicit def hListTailConverter[H: ETFConverter, T <: HList: HListTailConverter] = new HListTailConverter[H :: T] {
    def writeUnpadded(o: H :: T): ByteString = {
      val builder = new ByteStringBuilder
      val head :: tail = o
      builder ++= implicitly[ETFConverter[H]].write(head)
      builder ++= implicitly[HListTailConverter[T]].writeUnpadded(tail)
      builder.result
    }

    def readUnpaddedFromIterator(iter: ByteIterator): H :: T =
      implicitly[ETFConverter[H]].readFromIterator(iter) :: implicitly[HListTailConverter[T]].readUnpaddedFromIterator(iter)

  }

  implicit def tuple1Converter[T1](implicit c1: ETFConverter[T1]) = new ETFConverter[Tuple1[T1]] {
    def write(o: Tuple1[T1]) = {
      val builder = new ByteStringBuilder

      builder.putByte(SMALL_TUPLE)
      builder.putByte(o.productArity.toByte)

      builder ++= c1.write(o._1)
      builder.result
    }

    def readFromIterator(iter: ByteIterator) = {
      checkSignature(SMALL_TUPLE, iter.getByte)
      val size = iter.getByte
      val v1 = c1.readFromIterator(iter)
      Tuple1(v1)
    }
  }

  implicit def tuple2Converter[T1, T2](implicit c1: ETFConverter[T1], c2: ETFConverter[T2]) = new ETFConverter[Tuple2[T1, T2]] {
    def write(o: Tuple2[T1, T2]) = {
      val builder = new ByteStringBuilder

      builder.putByte(SMALL_TUPLE)
      builder.putByte(o.productArity.toByte)

      builder ++= c1.write(o._1)
      builder ++= c2.write(o._2)
      builder.result
    }

    def readFromIterator(iter: ByteIterator) = {
      checkSignature(SMALL_TUPLE, iter.getByte)
      val size = iter.getByte
      val v1 = c1.readFromIterator(iter)
      val v2 = c2.readFromIterator(iter)
      (v1, v2)
    }
  }

  implicit def tuple3Converter[T1, T2, T3](implicit c1: ETFConverter[T1], c2: ETFConverter[T2], c3: ETFConverter[T3]) = new ETFConverter[Tuple3[T1, T2, T3]] {
    def write(o: Tuple3[T1, T2, T3]) = {
      val builder = new ByteStringBuilder

      builder.putByte(SMALL_TUPLE)
      builder.putByte(o.productArity.toByte)

      builder ++= c1.write(o._1)
      builder ++= c2.write(o._2)
      builder ++= c3.write(o._3)
      builder.result
    }

    def readFromIterator(iter: ByteIterator) = {
      checkSignature(SMALL_TUPLE, iter.getByte)
      val size = iter.getByte
      val v1 = c1.readFromIterator(iter)
      val v2 = c2.readFromIterator(iter)
      val v3 = c3.readFromIterator(iter)
      (v1, v2, v3)
    }
  }

  implicit def tuple4Converter[T1, T2, T3, T4](implicit c1: ETFConverter[T1], c2: ETFConverter[T2], c3: ETFConverter[T3], c4: ETFConverter[T4]) = new ETFConverter[Tuple4[T1, T2, T3, T4]] {
    def write(o: Tuple4[T1, T2, T3, T4]) = {
      val builder = new ByteStringBuilder

      builder.putByte(SMALL_TUPLE)
      builder.putByte(o.productArity.toByte)

      builder ++= c1.write(o._1)
      builder ++= c2.write(o._2)
      builder ++= c3.write(o._3)
      builder ++= c4.write(o._4)
      builder.result
    }

    def readFromIterator(iter: ByteIterator) = {
      checkSignature(SMALL_TUPLE, iter.getByte)
      val size = iter.getByte
      val v1 = c1.readFromIterator(iter)
      val v2 = c2.readFromIterator(iter)
      val v3 = c3.readFromIterator(iter)
      val v4 = c4.readFromIterator(iter)
      (v1, v2, v3, v4)
    }
  }

  implicit def tuple5Converter[T1, T2, T3, T4, T5](implicit c1: ETFConverter[T1], c2: ETFConverter[T2], c3: ETFConverter[T3], c4: ETFConverter[T4], c5: ETFConverter[T5]) = new ETFConverter[Tuple5[T1, T2, T3, T4, T5]] {
    def write(o: Tuple5[T1, T2, T3, T4, T5]) = {
      val builder = new ByteStringBuilder
      builder.putByte(SMALL_TUPLE)
      builder.putByte(o.productArity.toByte)

      builder ++= c1.write(o._1)
      builder ++= c2.write(o._2)
      builder ++= c3.write(o._3)
      builder ++= c4.write(o._4)
      builder ++= c5.write(o._5)
      builder.result
    }

    def readFromIterator(iter: ByteIterator) = {
      checkSignature(SMALL_TUPLE, iter.getByte)
      val size = iter.getByte
      val v1 = c1.readFromIterator(iter)
      val v2 = c2.readFromIterator(iter)
      val v3 = c3.readFromIterator(iter)
      val v4 = c4.readFromIterator(iter)
      val v5 = c5.readFromIterator(iter)
      (v1, v2, v3, v4, v5)
    }
  }

  implicit def tuple6Converter[T1, T2, T3, T4, T5, T6](implicit c1: ETFConverter[T1], c2: ETFConverter[T2], c3: ETFConverter[T3], c4: ETFConverter[T4], c5: ETFConverter[T5], c6: ETFConverter[T6]) = new ETFConverter[Tuple6[T1, T2, T3, T4, T5, T6]] {
    def write(o: Tuple6[T1, T2, T3, T4, T5, T6]) = {
      val builder = new ByteStringBuilder
      builder.putByte(SMALL_TUPLE)
      builder.putByte(o.productArity.toByte)

      builder ++= c1.write(o._1)
      builder ++= c2.write(o._2)
      builder ++= c3.write(o._3)
      builder ++= c4.write(o._4)
      builder ++= c5.write(o._5)
      builder ++= c6.write(o._6)
      builder.result
    }

    def readFromIterator(iter: ByteIterator) = {
      checkSignature(SMALL_TUPLE, iter.getByte)
      val size = iter.getByte
      val v1 = c1.readFromIterator(iter)
      val v2 = c2.readFromIterator(iter)
      val v3 = c3.readFromIterator(iter)
      val v4 = c4.readFromIterator(iter)
      val v5 = c5.readFromIterator(iter)
      val v6 = c6.readFromIterator(iter)
      (v1, v2, v3, v4, v5, v6)
    }
  }

  implicit def tuple7Converter[T1, T2, T3, T4, T5, T6, T7](implicit c1: ETFConverter[T1], c2: ETFConverter[T2], c3: ETFConverter[T3], c4: ETFConverter[T4], c5: ETFConverter[T5], c6: ETFConverter[T6], c7: ETFConverter[T7]) = new ETFConverter[Tuple7[T1, T2, T3, T4, T5, T6, T7]] {
    def write(o: Tuple7[T1, T2, T3, T4, T5, T6, T7]) = {
      val builder = new ByteStringBuilder

      builder.putByte(SMALL_TUPLE)
      builder.putByte(o.productArity.toByte)

      builder ++= c1.write(o._1)
      builder ++= c2.write(o._2)
      builder ++= c3.write(o._3)
      builder ++= c4.write(o._4)
      builder ++= c5.write(o._5)
      builder ++= c6.write(o._6)
      builder ++= c7.write(o._7)
      builder.result
    }

    def readFromIterator(iter: ByteIterator) = {
      checkSignature(SMALL_TUPLE, iter.getByte)
      val size = iter.getByte
      val v1 = c1.readFromIterator(iter)
      val v2 = c2.readFromIterator(iter)
      val v3 = c3.readFromIterator(iter)
      val v4 = c4.readFromIterator(iter)
      val v5 = c5.readFromIterator(iter)
      val v6 = c6.readFromIterator(iter)
      val v7 = c7.readFromIterator(iter)
      (v1, v2, v3, v4, v5, v6, v7)
    }
  }

  implicit def tuple8Converter[T1, T2, T3, T4, T5, T6, T7, T8](implicit c1: ETFConverter[T1], c2: ETFConverter[T2], c3: ETFConverter[T3], c4: ETFConverter[T4], c5: ETFConverter[T5], c6: ETFConverter[T6], c7: ETFConverter[T7], c8: ETFConverter[T8]) = new ETFConverter[Tuple8[T1, T2, T3, T4, T5, T6, T7, T8]] {
    def write(o: Tuple8[T1, T2, T3, T4, T5, T6, T7, T8]) = {
      val builder = new ByteStringBuilder

      builder.putByte(SMALL_TUPLE)
      builder.putByte(o.productArity.toByte)

      builder ++= c1.write(o._1)
      builder ++= c2.write(o._2)
      builder ++= c3.write(o._3)
      builder ++= c4.write(o._4)
      builder ++= c5.write(o._5)
      builder ++= c6.write(o._6)
      builder ++= c7.write(o._7)
      builder ++= c8.write(o._8)
      builder.result
    }

    def readFromIterator(iter: ByteIterator) = {
      checkSignature(SMALL_TUPLE, iter.getByte)
      val size = iter.getByte
      val v1 = c1.readFromIterator(iter)
      val v2 = c2.readFromIterator(iter)
      val v3 = c3.readFromIterator(iter)
      val v4 = c4.readFromIterator(iter)
      val v5 = c5.readFromIterator(iter)
      val v6 = c6.readFromIterator(iter)
      val v7 = c7.readFromIterator(iter)
      val v8 = c8.readFromIterator(iter)
      (v1, v2, v3, v4, v5, v6, v7, v8)
    }
  }

  implicit def tuple9Converter[T1, T2, T3, T4, T5, T6, T7, T8, T9](implicit c1: ETFConverter[T1], c2: ETFConverter[T2], c3: ETFConverter[T3], c4: ETFConverter[T4], c5: ETFConverter[T5], c6: ETFConverter[T6], c7: ETFConverter[T7], c8: ETFConverter[T8], c9: ETFConverter[T9]) = new ETFConverter[Tuple9[T1, T2, T3, T4, T5, T6, T7, T8, T9]] {
    def write(o: Tuple9[T1, T2, T3, T4, T5, T6, T7, T8, T9]) = {
      val builder = new ByteStringBuilder

      builder.putByte(SMALL_TUPLE)
      builder.putByte(o.productArity.toByte)

      builder ++= c1.write(o._1)
      builder ++= c2.write(o._2)
      builder ++= c3.write(o._3)
      builder ++= c4.write(o._4)
      builder ++= c5.write(o._5)
      builder ++= c6.write(o._6)
      builder ++= c7.write(o._7)
      builder ++= c8.write(o._8)
      builder ++= c9.write(o._9)
      builder.result
    }

    def readFromIterator(iter: ByteIterator) = {
      checkSignature(SMALL_TUPLE, iter.getByte)
      val size = iter.getByte
      val v1 = c1.readFromIterator(iter)
      val v2 = c2.readFromIterator(iter)
      val v3 = c3.readFromIterator(iter)
      val v4 = c4.readFromIterator(iter)
      val v5 = c5.readFromIterator(iter)
      val v6 = c6.readFromIterator(iter)
      val v7 = c7.readFromIterator(iter)
      val v8 = c8.readFromIterator(iter)
      val v9 = c9.readFromIterator(iter)
      (v1, v2, v3, v4, v5, v6, v7, v8, v9)
    }
  }

  implicit def tuple10Converter[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10](implicit c1: ETFConverter[T1], c2: ETFConverter[T2], c3: ETFConverter[T3], c4: ETFConverter[T4], c5: ETFConverter[T5], c6: ETFConverter[T6], c7: ETFConverter[T7], c8: ETFConverter[T8], c9: ETFConverter[T9], c10: ETFConverter[T10]) = new ETFConverter[Tuple10[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10]] {
    def write(o: Tuple10[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10]) = {
      val builder = new ByteStringBuilder

      builder.putByte(SMALL_TUPLE)
      builder.putByte(o.productArity.toByte)

      builder ++= c1.write(o._1)
      builder ++= c2.write(o._2)
      builder ++= c3.write(o._3)
      builder ++= c4.write(o._4)
      builder ++= c5.write(o._5)
      builder ++= c6.write(o._6)
      builder ++= c7.write(o._7)
      builder ++= c8.write(o._8)
      builder ++= c9.write(o._9)
      builder ++= c10.write(o._10)
      builder.result
    }

    def readFromIterator(iter: ByteIterator) = {
      checkSignature(SMALL_TUPLE, iter.getByte)
      val size = iter.getByte
      val v1 = c1.readFromIterator(iter)
      val v2 = c2.readFromIterator(iter)
      val v3 = c3.readFromIterator(iter)
      val v4 = c4.readFromIterator(iter)
      val v5 = c5.readFromIterator(iter)
      val v6 = c6.readFromIterator(iter)
      val v7 = c7.readFromIterator(iter)
      val v8 = c8.readFromIterator(iter)
      val v9 = c9.readFromIterator(iter)
      val v10 = c10.readFromIterator(iter)
      (v1, v2, v3, v4, v5, v6, v7, v8, v9, v10)
    }
  }

  implicit def tuple11Converter[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11](implicit c1: ETFConverter[T1], c2: ETFConverter[T2], c3: ETFConverter[T3], c4: ETFConverter[T4], c5: ETFConverter[T5], c6: ETFConverter[T6], c7: ETFConverter[T7], c8: ETFConverter[T8], c9: ETFConverter[T9], c10: ETFConverter[T10], c11: ETFConverter[T11]) = new ETFConverter[Tuple11[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11]] {
    def write(o: Tuple11[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11]) = {
      val builder = new ByteStringBuilder

      builder.putByte(SMALL_TUPLE)
      builder.putByte(o.productArity.toByte)

      builder ++= c1.write(o._1)
      builder ++= c2.write(o._2)
      builder ++= c3.write(o._3)
      builder ++= c4.write(o._4)
      builder ++= c5.write(o._5)
      builder ++= c6.write(o._6)
      builder ++= c7.write(o._7)
      builder ++= c8.write(o._8)
      builder ++= c9.write(o._9)
      builder ++= c10.write(o._10)
      builder ++= c11.write(o._11)
      builder.result
    }

    def readFromIterator(iter: ByteIterator) = {
      checkSignature(SMALL_TUPLE, iter.getByte)
      val size = iter.getByte
      val v1 = c1.readFromIterator(iter)
      val v2 = c2.readFromIterator(iter)
      val v3 = c3.readFromIterator(iter)
      val v4 = c4.readFromIterator(iter)
      val v5 = c5.readFromIterator(iter)
      val v6 = c6.readFromIterator(iter)
      val v7 = c7.readFromIterator(iter)
      val v8 = c8.readFromIterator(iter)
      val v9 = c9.readFromIterator(iter)
      val v10 = c10.readFromIterator(iter)
      val v11 = c11.readFromIterator(iter)
      (v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11)
    }
  }

  implicit def tuple12Converter[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12](implicit c1: ETFConverter[T1], c2: ETFConverter[T2], c3: ETFConverter[T3], c4: ETFConverter[T4], c5: ETFConverter[T5], c6: ETFConverter[T6], c7: ETFConverter[T7], c8: ETFConverter[T8], c9: ETFConverter[T9], c10: ETFConverter[T10], c11: ETFConverter[T11], c12: ETFConverter[T12]) = new ETFConverter[Tuple12[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12]] {
    def write(o: Tuple12[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12]) = {
      val builder = new ByteStringBuilder

      builder.putByte(SMALL_TUPLE)
      builder.putByte(o.productArity.toByte)

      builder ++= c1.write(o._1)
      builder ++= c2.write(o._2)
      builder ++= c3.write(o._3)
      builder ++= c4.write(o._4)
      builder ++= c5.write(o._5)
      builder ++= c6.write(o._6)
      builder ++= c7.write(o._7)
      builder ++= c8.write(o._8)
      builder ++= c9.write(o._9)
      builder ++= c10.write(o._10)
      builder ++= c11.write(o._11)
      builder ++= c12.write(o._12)
      builder.result
    }

    def readFromIterator(iter: ByteIterator) = {
      checkSignature(SMALL_TUPLE, iter.getByte)
      val size = iter.getByte
      val v1 = c1.readFromIterator(iter)
      val v2 = c2.readFromIterator(iter)
      val v3 = c3.readFromIterator(iter)
      val v4 = c4.readFromIterator(iter)
      val v5 = c5.readFromIterator(iter)
      val v6 = c6.readFromIterator(iter)
      val v7 = c7.readFromIterator(iter)
      val v8 = c8.readFromIterator(iter)
      val v9 = c9.readFromIterator(iter)
      val v10 = c10.readFromIterator(iter)
      val v11 = c11.readFromIterator(iter)
      val v12 = c12.readFromIterator(iter)
      (v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12)
    }
  }

  implicit def tuple13Converter[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13](implicit c1: ETFConverter[T1], c2: ETFConverter[T2], c3: ETFConverter[T3], c4: ETFConverter[T4], c5: ETFConverter[T5], c6: ETFConverter[T6], c7: ETFConverter[T7], c8: ETFConverter[T8], c9: ETFConverter[T9], c10: ETFConverter[T10], c11: ETFConverter[T11], c12: ETFConverter[T12], c13: ETFConverter[T13]) = new ETFConverter[Tuple13[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13]] {
    def write(o: Tuple13[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13]) = {
      val builder = new ByteStringBuilder

      builder.putByte(SMALL_TUPLE)
      builder.putByte(o.productArity.toByte)

      builder ++= c1.write(o._1)
      builder ++= c2.write(o._2)
      builder ++= c3.write(o._3)
      builder ++= c4.write(o._4)
      builder ++= c5.write(o._5)
      builder ++= c6.write(o._6)
      builder ++= c7.write(o._7)
      builder ++= c8.write(o._8)
      builder ++= c9.write(o._9)
      builder ++= c10.write(o._10)
      builder ++= c11.write(o._11)
      builder ++= c12.write(o._12)
      builder ++= c13.write(o._13)
      builder.result
    }

    def readFromIterator(iter: ByteIterator) = {
      checkSignature(SMALL_TUPLE, iter.getByte)
      val size = iter.getByte
      val v1 = c1.readFromIterator(iter)
      val v2 = c2.readFromIterator(iter)
      val v3 = c3.readFromIterator(iter)
      val v4 = c4.readFromIterator(iter)
      val v5 = c5.readFromIterator(iter)
      val v6 = c6.readFromIterator(iter)
      val v7 = c7.readFromIterator(iter)
      val v8 = c8.readFromIterator(iter)
      val v9 = c9.readFromIterator(iter)
      val v10 = c10.readFromIterator(iter)
      val v11 = c11.readFromIterator(iter)
      val v12 = c12.readFromIterator(iter)
      val v13 = c13.readFromIterator(iter)
      (v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13)
    }
  }

  implicit def tuple14Converter[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14](implicit c1: ETFConverter[T1], c2: ETFConverter[T2], c3: ETFConverter[T3], c4: ETFConverter[T4], c5: ETFConverter[T5], c6: ETFConverter[T6], c7: ETFConverter[T7], c8: ETFConverter[T8], c9: ETFConverter[T9], c10: ETFConverter[T10], c11: ETFConverter[T11], c12: ETFConverter[T12], c13: ETFConverter[T13], c14: ETFConverter[T14]) = new ETFConverter[Tuple14[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14]] {
    def write(o: Tuple14[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14]) = {
      val builder = new ByteStringBuilder

      builder.putByte(SMALL_TUPLE)
      builder.putByte(o.productArity.toByte)

      builder ++= c1.write(o._1)
      builder ++= c2.write(o._2)
      builder ++= c3.write(o._3)
      builder ++= c4.write(o._4)
      builder ++= c5.write(o._5)
      builder ++= c6.write(o._6)
      builder ++= c7.write(o._7)
      builder ++= c8.write(o._8)
      builder ++= c9.write(o._9)
      builder ++= c10.write(o._10)
      builder ++= c11.write(o._11)
      builder ++= c12.write(o._12)
      builder ++= c13.write(o._13)
      builder ++= c14.write(o._14)
      builder.result
    }

    def readFromIterator(iter: ByteIterator) = {
      checkSignature(SMALL_TUPLE, iter.getByte)
      val size = iter.getByte
      val v1 = c1.readFromIterator(iter)
      val v2 = c2.readFromIterator(iter)
      val v3 = c3.readFromIterator(iter)
      val v4 = c4.readFromIterator(iter)
      val v5 = c5.readFromIterator(iter)
      val v6 = c6.readFromIterator(iter)
      val v7 = c7.readFromIterator(iter)
      val v8 = c8.readFromIterator(iter)
      val v9 = c9.readFromIterator(iter)
      val v10 = c10.readFromIterator(iter)
      val v11 = c11.readFromIterator(iter)
      val v12 = c12.readFromIterator(iter)
      val v13 = c13.readFromIterator(iter)
      val v14 = c14.readFromIterator(iter)
      (v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14)
    }
  }

  implicit def tuple15Converter[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15](implicit c1: ETFConverter[T1], c2: ETFConverter[T2], c3: ETFConverter[T3], c4: ETFConverter[T4], c5: ETFConverter[T5], c6: ETFConverter[T6], c7: ETFConverter[T7], c8: ETFConverter[T8], c9: ETFConverter[T9], c10: ETFConverter[T10], c11: ETFConverter[T11], c12: ETFConverter[T12], c13: ETFConverter[T13], c14: ETFConverter[T14], c15: ETFConverter[T15]) = new ETFConverter[Tuple15[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15]] {
    def write(o: Tuple15[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15]) = {
      val builder = new ByteStringBuilder

      builder.putByte(SMALL_TUPLE)
      builder.putByte(o.productArity.toByte)

      builder ++= c1.write(o._1)
      builder ++= c2.write(o._2)
      builder ++= c3.write(o._3)
      builder ++= c4.write(o._4)
      builder ++= c5.write(o._5)
      builder ++= c6.write(o._6)
      builder ++= c7.write(o._7)
      builder ++= c8.write(o._8)
      builder ++= c9.write(o._9)
      builder ++= c10.write(o._10)
      builder ++= c11.write(o._11)
      builder ++= c12.write(o._12)
      builder ++= c13.write(o._13)
      builder ++= c14.write(o._14)
      builder ++= c15.write(o._15)
      builder.result
    }

    def readFromIterator(iter: ByteIterator) = {
      checkSignature(SMALL_TUPLE, iter.getByte)
      val size = iter.getByte
      val v1 = c1.readFromIterator(iter)
      val v2 = c2.readFromIterator(iter)
      val v3 = c3.readFromIterator(iter)
      val v4 = c4.readFromIterator(iter)
      val v5 = c5.readFromIterator(iter)
      val v6 = c6.readFromIterator(iter)
      val v7 = c7.readFromIterator(iter)
      val v8 = c8.readFromIterator(iter)
      val v9 = c9.readFromIterator(iter)
      val v10 = c10.readFromIterator(iter)
      val v11 = c11.readFromIterator(iter)
      val v12 = c12.readFromIterator(iter)
      val v13 = c13.readFromIterator(iter)
      val v14 = c14.readFromIterator(iter)
      val v15 = c15.readFromIterator(iter)
      (v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14, v15)
    }
  }

  implicit def tuple16Converter[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16](implicit c1: ETFConverter[T1], c2: ETFConverter[T2], c3: ETFConverter[T3], c4: ETFConverter[T4], c5: ETFConverter[T5], c6: ETFConverter[T6], c7: ETFConverter[T7], c8: ETFConverter[T8], c9: ETFConverter[T9], c10: ETFConverter[T10], c11: ETFConverter[T11], c12: ETFConverter[T12], c13: ETFConverter[T13], c14: ETFConverter[T14], c15: ETFConverter[T15], c16: ETFConverter[T16]) = new ETFConverter[Tuple16[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16]] {
    def write(o: Tuple16[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16]) = {
      val builder = new ByteStringBuilder

      builder.putByte(SMALL_TUPLE)
      builder.putByte(o.productArity.toByte)

      builder ++= c1.write(o._1)
      builder ++= c2.write(o._2)
      builder ++= c3.write(o._3)
      builder ++= c4.write(o._4)
      builder ++= c5.write(o._5)
      builder ++= c6.write(o._6)
      builder ++= c7.write(o._7)
      builder ++= c8.write(o._8)
      builder ++= c9.write(o._9)
      builder ++= c10.write(o._10)
      builder ++= c11.write(o._11)
      builder ++= c12.write(o._12)
      builder ++= c13.write(o._13)
      builder ++= c14.write(o._14)
      builder ++= c15.write(o._15)
      builder ++= c16.write(o._16)
      builder.result
    }

    def readFromIterator(iter: ByteIterator) = {
      checkSignature(SMALL_TUPLE, iter.getByte)
      val size = iter.getByte
      val v1 = c1.readFromIterator(iter)
      val v2 = c2.readFromIterator(iter)
      val v3 = c3.readFromIterator(iter)
      val v4 = c4.readFromIterator(iter)
      val v5 = c5.readFromIterator(iter)
      val v6 = c6.readFromIterator(iter)
      val v7 = c7.readFromIterator(iter)
      val v8 = c8.readFromIterator(iter)
      val v9 = c9.readFromIterator(iter)
      val v10 = c10.readFromIterator(iter)
      val v11 = c11.readFromIterator(iter)
      val v12 = c12.readFromIterator(iter)
      val v13 = c13.readFromIterator(iter)
      val v14 = c14.readFromIterator(iter)
      val v15 = c15.readFromIterator(iter)
      val v16 = c16.readFromIterator(iter)
      (v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14, v15, v16)
    }
  }

  implicit def tuple17Converter[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17](implicit c1: ETFConverter[T1], c2: ETFConverter[T2], c3: ETFConverter[T3], c4: ETFConverter[T4], c5: ETFConverter[T5], c6: ETFConverter[T6], c7: ETFConverter[T7], c8: ETFConverter[T8], c9: ETFConverter[T9], c10: ETFConverter[T10], c11: ETFConverter[T11], c12: ETFConverter[T12], c13: ETFConverter[T13], c14: ETFConverter[T14], c15: ETFConverter[T15], c16: ETFConverter[T16], c17: ETFConverter[T17]) = new ETFConverter[Tuple17[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17]] {
    def write(o: Tuple17[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17]) = {
      val builder = new ByteStringBuilder

      builder.putByte(SMALL_TUPLE)
      builder.putByte(o.productArity.toByte)

      builder ++= c1.write(o._1)
      builder ++= c2.write(o._2)
      builder ++= c3.write(o._3)
      builder ++= c4.write(o._4)
      builder ++= c5.write(o._5)
      builder ++= c6.write(o._6)
      builder ++= c7.write(o._7)
      builder ++= c8.write(o._8)
      builder ++= c9.write(o._9)
      builder ++= c10.write(o._10)
      builder ++= c11.write(o._11)
      builder ++= c12.write(o._12)
      builder ++= c13.write(o._13)
      builder ++= c14.write(o._14)
      builder ++= c15.write(o._15)
      builder ++= c16.write(o._16)
      builder ++= c17.write(o._17)
      builder.result
    }

    def readFromIterator(iter: ByteIterator) = {
      checkSignature(SMALL_TUPLE, iter.getByte)
      val size = iter.getByte
      val v1 = c1.readFromIterator(iter)
      val v2 = c2.readFromIterator(iter)
      val v3 = c3.readFromIterator(iter)
      val v4 = c4.readFromIterator(iter)
      val v5 = c5.readFromIterator(iter)
      val v6 = c6.readFromIterator(iter)
      val v7 = c7.readFromIterator(iter)
      val v8 = c8.readFromIterator(iter)
      val v9 = c9.readFromIterator(iter)
      val v10 = c10.readFromIterator(iter)
      val v11 = c11.readFromIterator(iter)
      val v12 = c12.readFromIterator(iter)
      val v13 = c13.readFromIterator(iter)
      val v14 = c14.readFromIterator(iter)
      val v15 = c15.readFromIterator(iter)
      val v16 = c16.readFromIterator(iter)
      val v17 = c17.readFromIterator(iter)
      (v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14, v15, v16, v17)
    }
  }

  implicit def tuple18Converter[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18](implicit c1: ETFConverter[T1], c2: ETFConverter[T2], c3: ETFConverter[T3], c4: ETFConverter[T4], c5: ETFConverter[T5], c6: ETFConverter[T6], c7: ETFConverter[T7], c8: ETFConverter[T8], c9: ETFConverter[T9], c10: ETFConverter[T10], c11: ETFConverter[T11], c12: ETFConverter[T12], c13: ETFConverter[T13], c14: ETFConverter[T14], c15: ETFConverter[T15], c16: ETFConverter[T16], c17: ETFConverter[T17], c18: ETFConverter[T18]) = new ETFConverter[Tuple18[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18]] {
    def write(o: Tuple18[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18]) = {
      val builder = new ByteStringBuilder

      builder.putByte(SMALL_TUPLE)
      builder.putByte(o.productArity.toByte)

      builder ++= c1.write(o._1)
      builder ++= c2.write(o._2)
      builder ++= c3.write(o._3)
      builder ++= c4.write(o._4)
      builder ++= c5.write(o._5)
      builder ++= c6.write(o._6)
      builder ++= c7.write(o._7)
      builder ++= c8.write(o._8)
      builder ++= c9.write(o._9)
      builder ++= c10.write(o._10)
      builder ++= c11.write(o._11)
      builder ++= c12.write(o._12)
      builder ++= c13.write(o._13)
      builder ++= c14.write(o._14)
      builder ++= c15.write(o._15)
      builder ++= c16.write(o._16)
      builder ++= c17.write(o._17)
      builder ++= c18.write(o._18)
      builder.result
    }

    def readFromIterator(iter: ByteIterator) = {
      checkSignature(SMALL_TUPLE, iter.getByte)
      val size = iter.getByte
      val v1 = c1.readFromIterator(iter)
      val v2 = c2.readFromIterator(iter)
      val v3 = c3.readFromIterator(iter)
      val v4 = c4.readFromIterator(iter)
      val v5 = c5.readFromIterator(iter)
      val v6 = c6.readFromIterator(iter)
      val v7 = c7.readFromIterator(iter)
      val v8 = c8.readFromIterator(iter)
      val v9 = c9.readFromIterator(iter)
      val v10 = c10.readFromIterator(iter)
      val v11 = c11.readFromIterator(iter)
      val v12 = c12.readFromIterator(iter)
      val v13 = c13.readFromIterator(iter)
      val v14 = c14.readFromIterator(iter)
      val v15 = c15.readFromIterator(iter)
      val v16 = c16.readFromIterator(iter)
      val v17 = c17.readFromIterator(iter)
      val v18 = c18.readFromIterator(iter)
      (v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14, v15, v16, v17, v18)
    }
  }

  implicit def tuple19Converter[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19](implicit c1: ETFConverter[T1], c2: ETFConverter[T2], c3: ETFConverter[T3], c4: ETFConverter[T4], c5: ETFConverter[T5], c6: ETFConverter[T6], c7: ETFConverter[T7], c8: ETFConverter[T8], c9: ETFConverter[T9], c10: ETFConverter[T10], c11: ETFConverter[T11], c12: ETFConverter[T12], c13: ETFConverter[T13], c14: ETFConverter[T14], c15: ETFConverter[T15], c16: ETFConverter[T16], c17: ETFConverter[T17], c18: ETFConverter[T18], c19: ETFConverter[T19]) = new ETFConverter[Tuple19[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19]] {
    def write(o: Tuple19[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19]) = {
      val builder = new ByteStringBuilder

      builder.putByte(SMALL_TUPLE)
      builder.putByte(o.productArity.toByte)

      builder ++= c1.write(o._1)
      builder ++= c2.write(o._2)
      builder ++= c3.write(o._3)
      builder ++= c4.write(o._4)
      builder ++= c5.write(o._5)
      builder ++= c6.write(o._6)
      builder ++= c7.write(o._7)
      builder ++= c8.write(o._8)
      builder ++= c9.write(o._9)
      builder ++= c10.write(o._10)
      builder ++= c11.write(o._11)
      builder ++= c12.write(o._12)
      builder ++= c13.write(o._13)
      builder ++= c14.write(o._14)
      builder ++= c15.write(o._15)
      builder ++= c16.write(o._16)
      builder ++= c17.write(o._17)
      builder ++= c18.write(o._18)
      builder ++= c19.write(o._19)
      builder.result
    }

    def readFromIterator(iter: ByteIterator) = {
      checkSignature(SMALL_TUPLE, iter.getByte)
      val size = iter.getByte
      val v1 = c1.readFromIterator(iter)
      val v2 = c2.readFromIterator(iter)
      val v3 = c3.readFromIterator(iter)
      val v4 = c4.readFromIterator(iter)
      val v5 = c5.readFromIterator(iter)
      val v6 = c6.readFromIterator(iter)
      val v7 = c7.readFromIterator(iter)
      val v8 = c8.readFromIterator(iter)
      val v9 = c9.readFromIterator(iter)
      val v10 = c10.readFromIterator(iter)
      val v11 = c11.readFromIterator(iter)
      val v12 = c12.readFromIterator(iter)
      val v13 = c13.readFromIterator(iter)
      val v14 = c14.readFromIterator(iter)
      val v15 = c15.readFromIterator(iter)
      val v16 = c16.readFromIterator(iter)
      val v17 = c17.readFromIterator(iter)
      val v18 = c18.readFromIterator(iter)
      val v19 = c19.readFromIterator(iter)
      (v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14, v15, v16, v17, v18, v19)
    }
  }

  implicit def tuple20Converter[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20](implicit c1: ETFConverter[T1], c2: ETFConverter[T2], c3: ETFConverter[T3], c4: ETFConverter[T4], c5: ETFConverter[T5], c6: ETFConverter[T6], c7: ETFConverter[T7], c8: ETFConverter[T8], c9: ETFConverter[T9], c10: ETFConverter[T10], c11: ETFConverter[T11], c12: ETFConverter[T12], c13: ETFConverter[T13], c14: ETFConverter[T14], c15: ETFConverter[T15], c16: ETFConverter[T16], c17: ETFConverter[T17], c18: ETFConverter[T18], c19: ETFConverter[T19], c20: ETFConverter[T20]) = new ETFConverter[Tuple20[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20]] {
    def write(o: Tuple20[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20]) = {
      val builder = new ByteStringBuilder

      builder.putByte(SMALL_TUPLE)
      builder.putByte(o.productArity.toByte)

      builder ++= c1.write(o._1)
      builder ++= c2.write(o._2)
      builder ++= c3.write(o._3)
      builder ++= c4.write(o._4)
      builder ++= c5.write(o._5)
      builder ++= c6.write(o._6)
      builder ++= c7.write(o._7)
      builder ++= c8.write(o._8)
      builder ++= c9.write(o._9)
      builder ++= c10.write(o._10)
      builder ++= c11.write(o._11)
      builder ++= c12.write(o._12)
      builder ++= c13.write(o._13)
      builder ++= c14.write(o._14)
      builder ++= c15.write(o._15)
      builder ++= c16.write(o._16)
      builder ++= c17.write(o._17)
      builder ++= c18.write(o._18)
      builder ++= c19.write(o._19)
      builder ++= c20.write(o._20)
      builder.result
    }

    def readFromIterator(iter: ByteIterator) = {
      checkSignature(SMALL_TUPLE, iter.getByte)
      val size = iter.getByte
      val v1 = c1.readFromIterator(iter)
      val v2 = c2.readFromIterator(iter)
      val v3 = c3.readFromIterator(iter)
      val v4 = c4.readFromIterator(iter)
      val v5 = c5.readFromIterator(iter)
      val v6 = c6.readFromIterator(iter)
      val v7 = c7.readFromIterator(iter)
      val v8 = c8.readFromIterator(iter)
      val v9 = c9.readFromIterator(iter)
      val v10 = c10.readFromIterator(iter)
      val v11 = c11.readFromIterator(iter)
      val v12 = c12.readFromIterator(iter)
      val v13 = c13.readFromIterator(iter)
      val v14 = c14.readFromIterator(iter)
      val v15 = c15.readFromIterator(iter)
      val v16 = c16.readFromIterator(iter)
      val v17 = c17.readFromIterator(iter)
      val v18 = c18.readFromIterator(iter)
      val v19 = c19.readFromIterator(iter)
      val v20 = c20.readFromIterator(iter)
      (v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14, v15, v16, v17, v18, v19, v20)
    }
  }

  implicit def tuple21Converter[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21](implicit c1: ETFConverter[T1], c2: ETFConverter[T2], c3: ETFConverter[T3], c4: ETFConverter[T4], c5: ETFConverter[T5], c6: ETFConverter[T6], c7: ETFConverter[T7], c8: ETFConverter[T8], c9: ETFConverter[T9], c10: ETFConverter[T10], c11: ETFConverter[T11], c12: ETFConverter[T12], c13: ETFConverter[T13], c14: ETFConverter[T14], c15: ETFConverter[T15], c16: ETFConverter[T16], c17: ETFConverter[T17], c18: ETFConverter[T18], c19: ETFConverter[T19], c20: ETFConverter[T20], c21: ETFConverter[T21]) = new ETFConverter[Tuple21[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21]] {
    def write(o: Tuple21[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21]) = {
      val builder = new ByteStringBuilder

      builder.putByte(SMALL_TUPLE)
      builder.putByte(o.productArity.toByte)

      builder ++= c1.write(o._1)
      builder ++= c2.write(o._2)
      builder ++= c3.write(o._3)
      builder ++= c4.write(o._4)
      builder ++= c5.write(o._5)
      builder ++= c6.write(o._6)
      builder ++= c7.write(o._7)
      builder ++= c8.write(o._8)
      builder ++= c9.write(o._9)
      builder ++= c10.write(o._10)
      builder ++= c11.write(o._11)
      builder ++= c12.write(o._12)
      builder ++= c13.write(o._13)
      builder ++= c14.write(o._14)
      builder ++= c15.write(o._15)
      builder ++= c16.write(o._16)
      builder ++= c17.write(o._17)
      builder ++= c18.write(o._18)
      builder ++= c19.write(o._19)
      builder ++= c20.write(o._20)
      builder ++= c21.write(o._21)
      builder.result
    }

    def readFromIterator(iter: ByteIterator) = {
      checkSignature(SMALL_TUPLE, iter.getByte)
      val size = iter.getByte
      val v1 = c1.readFromIterator(iter)
      val v2 = c2.readFromIterator(iter)
      val v3 = c3.readFromIterator(iter)
      val v4 = c4.readFromIterator(iter)
      val v5 = c5.readFromIterator(iter)
      val v6 = c6.readFromIterator(iter)
      val v7 = c7.readFromIterator(iter)
      val v8 = c8.readFromIterator(iter)
      val v9 = c9.readFromIterator(iter)
      val v10 = c10.readFromIterator(iter)
      val v11 = c11.readFromIterator(iter)
      val v12 = c12.readFromIterator(iter)
      val v13 = c13.readFromIterator(iter)
      val v14 = c14.readFromIterator(iter)
      val v15 = c15.readFromIterator(iter)
      val v16 = c16.readFromIterator(iter)
      val v17 = c17.readFromIterator(iter)
      val v18 = c18.readFromIterator(iter)
      val v19 = c19.readFromIterator(iter)
      val v20 = c20.readFromIterator(iter)
      val v21 = c21.readFromIterator(iter)
      (v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14, v15, v16, v17, v18, v19, v20, v21)
    }
  }

  implicit def tuple22Converter[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22](implicit c1: ETFConverter[T1], c2: ETFConverter[T2], c3: ETFConverter[T3], c4: ETFConverter[T4], c5: ETFConverter[T5], c6: ETFConverter[T6], c7: ETFConverter[T7], c8: ETFConverter[T8], c9: ETFConverter[T9], c10: ETFConverter[T10], c11: ETFConverter[T11], c12: ETFConverter[T12], c13: ETFConverter[T13], c14: ETFConverter[T14], c15: ETFConverter[T15], c16: ETFConverter[T16], c17: ETFConverter[T17], c18: ETFConverter[T18], c19: ETFConverter[T19], c20: ETFConverter[T20], c21: ETFConverter[T21], c22: ETFConverter[T22]) = new ETFConverter[Tuple22[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22]] {
    def write(o: Tuple22[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22]) = {
      val builder = new ByteStringBuilder

      builder.putByte(SMALL_TUPLE)
      builder.putByte(o.productArity.toByte)

      builder ++= c1.write(o._1)
      builder ++= c2.write(o._2)
      builder ++= c3.write(o._3)
      builder ++= c4.write(o._4)
      builder ++= c5.write(o._5)
      builder ++= c6.write(o._6)
      builder ++= c7.write(o._7)
      builder ++= c8.write(o._8)
      builder ++= c9.write(o._9)
      builder ++= c10.write(o._10)
      builder ++= c11.write(o._11)
      builder ++= c12.write(o._12)
      builder ++= c13.write(o._13)
      builder ++= c14.write(o._14)
      builder ++= c15.write(o._15)
      builder ++= c16.write(o._16)
      builder ++= c17.write(o._17)
      builder ++= c18.write(o._18)
      builder ++= c19.write(o._19)
      builder ++= c20.write(o._20)
      builder ++= c21.write(o._21)
      builder ++= c22.write(o._22)
      builder.result
    }

    def readFromIterator(iter: ByteIterator) = {
      checkSignature(SMALL_TUPLE, iter.getByte)
      val size = iter.getByte
      val v1 = c1.readFromIterator(iter)
      val v2 = c2.readFromIterator(iter)
      val v3 = c3.readFromIterator(iter)
      val v4 = c4.readFromIterator(iter)
      val v5 = c5.readFromIterator(iter)
      val v6 = c6.readFromIterator(iter)
      val v7 = c7.readFromIterator(iter)
      val v8 = c8.readFromIterator(iter)
      val v9 = c9.readFromIterator(iter)
      val v10 = c10.readFromIterator(iter)
      val v11 = c11.readFromIterator(iter)
      val v12 = c12.readFromIterator(iter)
      val v13 = c13.readFromIterator(iter)
      val v14 = c14.readFromIterator(iter)
      val v15 = c15.readFromIterator(iter)
      val v16 = c16.readFromIterator(iter)
      val v17 = c17.readFromIterator(iter)
      val v18 = c18.readFromIterator(iter)
      val v19 = c19.readFromIterator(iter)
      val v20 = c20.readFromIterator(iter)
      val v21 = c21.readFromIterator(iter)
      val v22 = c22.readFromIterator(iter)
      (v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14, v15, v16, v17, v18, v19, v20, v21, v22)
    }
  }
}

package nl.gideondk.nucleus.protocol

import akka.util.{ ByteStringBuilder, ByteString, ByteIterator }

trait ETFWriter[T] {
  def write(o: T): ByteString
}

trait ETFReader[T] {
  def readFromIterator(o: ByteIterator): T
  def read(o: ByteString): T = readFromIterator(o.iterator)
}

trait ETFConverter[T] extends ETFReader[T] with ETFWriter[T]

trait NucleusConverter[T] extends ETFConverter[T]

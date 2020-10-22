package event.ext

case class Pair[L, R](left: L, right: R)

trait Decoder {
  def getName(): String
  def decode(bytes: Array[Byte]): Pair[Array[Byte], Long]
  def decode(bytes: Array[Byte], limit: Int): Pair[Array[Byte], Long]
}
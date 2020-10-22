package event.ext

case class Pair(bytes: Array[Byte], size: Long)

trait Decoder {
  def getName(): String
  def decode(bytes: Array[Byte]): Pair
  def decode(bytes: Array[Byte], limit: Int): Pair
}
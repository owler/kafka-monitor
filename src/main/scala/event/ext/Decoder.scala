package event.ext

trait Decoder {
  def getName(): String
  def decode(bytes: Array[Byte]): String
}

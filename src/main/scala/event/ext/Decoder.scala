package event.ext

trait Decoder {
  def decode(bytes: Array[Byte]): String
}

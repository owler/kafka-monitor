package event.ext

class Utf8Decoder extends Decoder {
  override def getName(): String = "UTF8"

  override def decode(bytes: Array[Byte]): Pair = {
    Pair(bytes, bytes.length)
  }

  override def decode(bytes: Array[Byte], limit: Int): Pair = {
    Pair(bytes.take(limit), bytes.length)
  }
}
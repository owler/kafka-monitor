package event.ext

class Utf8Decoder extends Decoder {
  override def getName(): String = "UTF8"

  override def decode(bytes: Array[Byte]): Pair[Array[Byte], Long] = {
    Pair(bytes, bytes.length)
  }

  override def decode(bytes: Array[Byte], limit: Int): Pair[Array[Byte], Long] = {
    Pair(bytes.take(limit), bytes.length)
  }
}
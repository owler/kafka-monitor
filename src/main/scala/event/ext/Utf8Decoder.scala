package event.ext

class Utf8Decoder extends Decoder {
  override def getName(): String = "UTF8"

  override def decode(bytes: Array[Byte]): DecodedMessage = {
    DecodedMessage(bytes, bytes.length)
  }

  override def decode(bytes: Array[Byte], limit: Int): DecodedMessage = {
    DecodedMessage(bytes.take(limit), bytes.length)
  }
}
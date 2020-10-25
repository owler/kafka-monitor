package event.ext

case class DecodedMessage(bytes: Array[Byte], size: Long)

trait Decoder {
  def getName: String
  def decode(bytes: Array[Byte]): DecodedMessage
  def decode(bytes: Array[Byte], limit: Int): DecodedMessage
}
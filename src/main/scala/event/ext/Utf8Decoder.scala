package event.ext

import java.nio.charset.StandardCharsets

class Utf8Decoder extends Decoder {
  override def getName(): String = "UTF8"

  override def decode(bytes: Array[Byte]): String = {
    new String(bytes, StandardCharsets.UTF_8)
  }
}
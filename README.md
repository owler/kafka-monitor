# kafka-monitor
[![Build](https://travis-ci.com/owler/kafka-monitor.svg?token=q8PUaXxtZ8UBGympJ7ap&branch=master)](https://travis-ci.org/owler/kafka-monitor#)

<em>Kafka monitor is a web UI for viewing Kafka topics and messages.</em> 
# Features
* **View topics** — partitions with start and end offsets
* **Browse messages** — UTF8 or custom format
* **Download** - raw bytes, UTF8 or custom format 
* **Plugins** - supports custom plugins written in Java or Scala to view/download messages

# Requirements
* Java 8 or newer
* Kafka (version 0.10.0 or newer)

# Build
sbt pack

# Plugins
Implement your custom Decoder and put jar with single class into plugins folder.
Any extra lib  place into lib/ext folder
decode method may return truncated bytes (for big message) if you specify limit,
it expected that DecodedMessage.size property is a size of message before truncate.
You may put approximate size (it will used only for information)
```scala
package event.ext

case class DecodedMessage(bytes: Array[Byte], size: Long)

trait Decoder {
  def getName: String
  def decode(bytes: Array[Byte]): DecodedMessage
  def decode(bytes: Array[Byte], limit: Int): DecodedMessage
}
```

# kafka-monitor
[![Build](https://travis-ci.com/owler/kafka-monitor.svg?token=q8PUaXxtZ8UBGympJ7ap&branch=master)](https://travis-ci.org/owler/kafka-monitor#)

<em>Kafka monitor is a web UI for viewing Kafka topics and messages.</em> 
<img src="https://user-images.githubusercontent.com/2174326/97114531-14b96580-1702-11eb-9360-01303a1eee21.PNG"></img>

# Features
* **View topics** — partitions with start and end offsets
* **Browse messages** — UTF8 or custom format
* **Download messages** - raw bytes, UTF8 or custom format 
* **Plugins** - supports custom plugins written in Java or Scala to view/download messages

# Requirements
* Java 8 or newer
* Kafka (version 0.10.0 or newer)

# Build
sbt pack

# Plugins
Implement your custom Decoder and put jar with single class into plugins folder.
Any extra lib  place into lib/ext folder.
decode(bytes: Array[Byte], limit: Int) method may return truncated bytes (for big message) if you specify limit,
It's expected that DecodedMessage.size property is a size of message before truncate.
You may put approximate size (it will be used only for information)
```scala
package event.ext

case class DecodedMessage(bytes: Array[Byte], size: Long)

trait Decoder {
  def getName(): String
  def decode(bytes: Array[Byte]): DecodedMessage
  def decode(bytes: Array[Byte], limit: Int): DecodedMessage
}
```

## Securing the Kafka monitor UI
Kafka monitor UI implements an Basic Auth authentication mechanism to restrict user access.
Use 
```scala
com.eclipse.Util.Password.obfuscate("your password")
``` 
to obfuscate password and then add it into myRealm.properties

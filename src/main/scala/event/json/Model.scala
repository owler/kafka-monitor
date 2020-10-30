package event.json

import java.util.Date

trait Model {
}

case class Topic(topic: String, enabled: Boolean) extends Model
case class MsgType(msgtype: String) extends Model
case class Partition(topic: String, partition: Int, start: Long, end: Long) extends Model
// size is original length of raw bytes and msgsize is decoded msg size before truncate
case class KMessage[T](offset: Long, timestamp: Date, message: T, size: Long, msgtype: String, msgsize: Long) extends Model


package event.json

import java.util.Date

trait Model {
}

case class Topics(topics: List[Topic]) extends Model
case class Topic(topic: String) extends Model
case class MsgTypes(msgtypes: List[MsgType]) extends Model
case class MsgType(msgtype: String) extends Model

case class Partitions(partitions: List[Partition]) extends Model
case class Partition(topic: String, partition: Int, start: Long, end: Long) extends Model
case class KMessages[T](messages: List[T]) extends Model
// size is original length of raw bytes and msgsize is decoded msg size before truncate
case class KMessage[T](offset: Long, timestamp: Date, message: T, size: Long, msgtype: String, msgsize: Long) extends Model


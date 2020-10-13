package event.json

import java.sql.Timestamp

trait Model {
}

case class Topics(topics: List[Topic]) extends Model
case class Topic(topic: String) extends Model
case class Partitions(partitions: List[Partition]) extends Model
case class Partition(topic: String, partition: Int, start: Long, end: Long) extends Model
case class KMessages[T](messages: List[T]) extends Model
case class KMessage[T](offset: Long, timestamp: Long, message: T) extends Model


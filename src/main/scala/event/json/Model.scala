package event.json

trait Model {
}

case class Topics(topics: List[Topic]) extends Model
case class Topic(topic: String) extends Model
case class Partitions(partitions: List[Partition]) extends Model
case class Partition(partition: Int, start: Long, end: Long) extends Model
case class KMessage(message: Array[Byte]) extends Model
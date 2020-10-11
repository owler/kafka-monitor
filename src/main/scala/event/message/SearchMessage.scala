package event.message


trait EventMessage {}

trait SearchMessage extends EventMessage {
  def callback: String
}

case class ListTopics(callback: String) extends SearchMessage
case class TopicDetails(topicName: String, callback: String) extends SearchMessage
case class Message(topicName: String, partition: String, offset: String, callback: String) extends SearchMessage


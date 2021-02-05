package event.utils

import com.typesafe.config.ConfigFactory
import scala.jdk.CollectionConverters._

/**
 */
object CharmConfigObject {

  val conf = ConfigFactory.load
  val cryptor = new Crypto(conf.getString("env"))

  def getConfig = conf
  def getString(key: String) = {
    cryptor.decrypt(conf.getString(key))
  }

  def parse(key: String): Map[String, AnyRef] = {
    conf.getObject(key).unwrapped().asScala.mapValuesInPlace((_, v) => v match {
      case s: String => cryptor.decrypt(s)
      case _ => v
    }).toMap
  }

}

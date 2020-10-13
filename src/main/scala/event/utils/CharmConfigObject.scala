package event.utils

import java.util

import com.typesafe.config.ConfigFactory


/**
  */
object CharmConfigObject {

  val conf = ConfigFactory.load
  val cryptor = new Crypto(conf.getString("env"))

  def getConfig = conf
  def getString(key: String) = {
    cryptor.decrypt(conf.getString(key))
  }

}

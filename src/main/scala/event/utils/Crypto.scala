package event.utils

import org.jasypt.encryption.pbe.StandardPBEStringEncryptor
import scala.io.StdIn

/**
 */
class Crypto(password: String) {

  val Reg = "ENC(.*)".r
  val encryptor = new StandardPBEStringEncryptor()
  encryptor.setAlgorithm("PBEWithMD5AndDES")
  encryptor.setPassword(password)



  def encrypt(plainText: String) = "ENC(" + encryptor.encrypt(plainText) + ")"

  def decrypt(encryptedText: String) = encryptedText match {
    case Reg(str) => encryptor.decrypt(str)
    case _ => encryptedText
  }

}

object Crypto {
}

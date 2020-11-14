package simple

import event.utils.CharmConfigObject
import org.eclipse.jetty.util.security.Password
import org.scalatest.funsuite.AnyFunSuite

/**
 */
class SimpleMainTest extends AnyFunSuite {
  test("") {
    val conf = CharmConfigObject
    println(conf.getString("env"))
    assert(conf.parse("kafka")("ssl.truststore.password") == "encrypted password")
  }
  test("com.eclipse.Util.Password") {
    println( Password.obfuscate("duck"))
  }
}

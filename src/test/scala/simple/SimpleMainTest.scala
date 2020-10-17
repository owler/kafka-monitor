package simple

import event.utils.CharmConfigObject
import org.eclipse.jetty.util.security.Password
import org.scalatest.FunSuite

/**
 */
class SimpleMainTest extends FunSuite {
  test("") {
    val conf = CharmConfigObject
    println(conf.getString("env"))
  }
  test("com.eclipse.Util.Password") {
    println( Password.obfuscate("duck"))
  }
}

package simple

import event.utils.CharmConfigObject
import org.scalatest.FunSuite

/**
 */
class SimpleMainTest extends FunSuite {
  test("") {
    val conf = CharmConfigObject
    println(conf.getString("env"))
  }
}

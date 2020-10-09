package simple

import org.scalatest.funsuite.AnyFunSuite
import simple.utils.CharmConfigObject

/**
 */
class SimpleMainTest extends AnyFunSuite {
  test("") {
    val conf = CharmConfigObject
    println(conf.getString("env"))
    println(conf.parse("path"))
  }
}

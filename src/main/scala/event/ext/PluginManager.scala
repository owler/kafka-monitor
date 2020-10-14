package event.ext

import java.io.File
import java.net.{URL, URLClassLoader}
import java.util.jar.JarFile

import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}

object PluginManager {
  def loadDecoders(path: String): Map[String, Decoder] = {
    Map()
  }

  def processFile(file: File): Map[String, Decoder] = {
    val l = getClassNames(file.getAbsolutePath)
    val loader = new URLClassLoader(Array(new URL("file:" + file.getAbsolutePath)))
    l.map { clazz =>
      Try (loader.loadClass(clazz)) match {
        case Success(value) if value.isInstance(classOf[Decoder]) => Some(clazz -> value.asInstanceOf[Decoder])
        case Failure(e) => println(e); None
      }
    }.filter(_.isDefined).map(_.get).toMap
  }


  @throws[Exception]
  private def getClassNames(jarPath: String):List[String] = {
    val jar = new JarFile(jarPath)
    val entries = jar.entries.asScala
    entries.filter(e => e.getName.endsWith(".class"))
      .map(e => e.getName.replaceAll("/", ".")
        .replaceAll(".class", "")).toList
  }
}

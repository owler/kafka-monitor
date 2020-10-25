package event.ext

import java.io.File
import java.net.{URL, URLClassLoader}
import java.util.jar.JarFile

import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory

import scala.jdk.CollectionConverters._

object PluginManager {
  private val log = Logger(LoggerFactory.getLogger(this.getClass))
  def loadDecoders(path: String): Map[String, Decoder] = {
    val dir = new File(path)
    val files = if (dir.exists && dir.isDirectory) {
      dir.listFiles.filter(f => f.isFile && f.getName.endsWith(".jar")).toList
    } else {
      List[File]()
    }
    files.flatMap(f => processFile(f)).toMap
  }

  def processFile(file: File): Map[String, Decoder] = {
    val classes = getClassNames(file.getAbsolutePath)
    log.info("Processing plugins: " + classes)
    val loader = new URLClassLoader(Array(new URL("file:" + file.getAbsolutePath)), this.getClass.getClassLoader)
    classes.map { clazz => {
      try {
        log.info("Trying instantiate class: " + clazz)
        val res = loader.loadClass(clazz)
        if (classOf[Decoder].isAssignableFrom(res)) {
          val decoder = res.getConstructor().newInstance().asInstanceOf[Decoder]
          Some(decoder.getName() -> decoder)
        } else  None
      } catch {
        case e: Throwable => log.warn("Failed to instantiate decoder: " + e); None
      }
    }}.filter(_.isDefined).map(_.get).toMap
  }

  private def getClassNames(jarPath: String):List[String] = {
    val jar = new JarFile(jarPath)
    val entries = jar.entries.asScala
    entries.filter(e => e.getName.endsWith(".class"))
      .map(e => e.getName.replaceAll("/", ".")
        .replaceAll(".class", "")).toList
  }
}

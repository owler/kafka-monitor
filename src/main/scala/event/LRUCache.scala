package event

import java.util.Collections.synchronizedMap

import scala.collection.JavaConverters._
import scala.collection.mutable

class LRUCache[K, V](maxEntries: Int)
  extends java.util.LinkedHashMap[K, V](100, .75f, true) {

  override def removeEldestEntry(eldest: java.util.Map.Entry[K, V]): Boolean
  = size > maxEntries

}

object LRUCache {
  def apply[K, V](maxEntries: Int): mutable.Map[K, V] = {
    synchronizedMap(new LRUCache[K, V] (maxEntries)).asScala
  }
}
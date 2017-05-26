package ch.epfl.lts2

import java.io.PrintWriter

import breeze.linalg.{max, min}
import org.apache.spark.graphx.{Edge, Graph}
import org.apache.spark.rdd.RDD

import scala.reflect.ClassTag

/**
  * Created by volodymyrmiz on 05.10.16.
  */
package object Utils {
  /**
    * Hide Apache Spark console logs.
    *
    * @param params List of logs to be suppressed.
    */
  def suppressLogs(params: List[String]): Unit = {
    import org.apache.log4j.{Level, Logger}
    params.foreach(Logger.getLogger(_).setLevel(Level.OFF))
  }

  def removeLowWeightEdges[VD: ClassTag, ED: ClassTag](graph: Graph[VD, ED], minWeight: Double): Graph[VD, ED] = {
    Graph(graph.vertices,
      graph.edges.filter(_.attr.toString.toDouble  > minWeight)
    )
  }

  def removeSingletons[VD: ClassTag, ED: ClassTag](graph: Graph[VD, ED]) =
    Graph(graph.triplets.map(et => (et.srcId, et.srcAttr))
      .union(graph.triplets.map(et => (et.dstId, et.dstAttr)))
      .distinct,
      graph.edges)

  private def mean(m: Map[Int, Double]): Double = {
    m.values.sum/m.size
  }

  def compareTimeSeries(m1: Map[Int, Double], m2: Map[Int, Double], isFiltered: Boolean): Double = {
    val commonKeys = m2.keySet.intersect(m1.keySet)

    var m1Freq = 1
    var m2Freq = 1

    if (isFiltered) {
      m1Freq = m1.keys.size
      m2Freq = m2.keys.size
    }

    if (commonKeys.isEmpty) 0
    else {
      var weight: Double = 0.0
      for (key <- commonKeys) {
        val value1 = m1(key)/m1Freq
        val value2 = m2(key)/m2Freq
        val threshold = min(value1, value2)/max(value1, value2)
        if (threshold > 0.5) weight += threshold
        else weight -= threshold
      }
      weight
    }
  }

  def compareTimeSeries(m1: Map[Int, Double], m2: Map[Int, Double], start: Int, stop: Int, upperBoundActivationsNumber: Int = 0, isFiltered: Boolean): Double = {

    val m1Filtered = m1.filter(pair => pair._2 > upperBoundActivationsNumber)
    val m2Filtered = m2.filter(pair => pair._2 > upperBoundActivationsNumber)

    val commonKeys = m2Filtered.keySet.intersect(m1Filtered.keySet)
      .filter(hour => hour > start & hour < stop)

    var m1Freq = 1
    var m2Freq = 1

    if (isFiltered) {
      m1Freq = m1Filtered.keys.count(key => key > start & key < stop)
      m2Freq = m2Filtered.keys.count(key => key > start & key < stop)
    }

    if (commonKeys.isEmpty) 0
    else {
      var weight: Double = 0.0
      for (key <- commonKeys) {
        val value1 = m1Filtered(key)/m1Freq
        val value2 = m2Filtered(key)/m2Freq
        val threshold = min(value1, value2)/max(value1, value2)
        if (threshold > 0.5) weight += threshold
        else weight -= threshold
      }
      weight
    }
  }

  private def toGexf[VD, ED](g: Graph[VD, ED]) =
    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
      "<gexf xmlns=\"http://www.gexf.net/1.2draft\" version=\"1.2\">\n" +
      " <graph mode=\"static\" defaultedgetype=\"directed\">\n" +
      " <nodes>\n" +
      g.vertices.map(v => "      <node id=\"" + v._1 + "\" label=\"" +
        v._2 + "\" />\n").collect.mkString +
      "      </nodes>\n" +
      "      <edges>\n" +
      g.edges.map(e => "        <edge source=\"" + e.srcId +
        "\" target=\"" + e.dstId + "\" label=\"" + e.attr +
        "\" />\n").collect.mkString +
      "        </edges>\n" +
      " </graph>\n" +
      "</gexf>"

  def saveGraph[VD, ED](graph: Graph[VD, ED], fileName: String) = {
    val pw = new PrintWriter(fileName)
    pw.write(toGexf(graph))
    pw.close
  }

  def getLargestConnectedComponent[VD: ClassTag, ED: ClassTag](g: Graph[VD, Double]): Graph[VD, Double] = {
    val cc = g.connectedComponents()
    val ids = cc.vertices.map((v: (Long, Long)) => v._2)
    val largestId = ids.map((_, 1L)).reduceByKey(_ + _).sortBy(-_._2).keys.collect(){0}
    val largestCC = cc.vertices.filter((v: (Long, Long)) => v._2 == largestId)
    val lccVertices = largestCC.map(_._1).collect()
    g.subgraph(vpred = (id, attr) => lccVertices.contains(id))
  }

  def getLargestConnectedComponent[VD: ClassTag, ED: ClassTag](g: Graph[VD, Double], order: Int): Graph[VD, Double] = {
    val cc = g.connectedComponents()
    val ids = cc.vertices.map((v: (Long, Long)) => v._2)
    val largestId = ids.map((_, 1L)).reduceByKey(_ + _).sortBy(-_._2).keys.collect(){order}
    val largestCC = cc.vertices.filter((v: (Long, Long)) => v._2 == largestId)
    val lccVertices = largestCC.map(_._1).collect()
    g.subgraph(vpred = (id, attr) => lccVertices.contains(id))
  }

  implicit def graphXExt[VD: ClassTag, ED: ClassTag](g: Graph[VD, ED]) = new GraphXExtension(g)

  def isAllDigits(x: String) = x forall Character.isDigit
}
package ch.epfl.lts2

import java.io.PrintWriter

import breeze.linalg.{max, min}
import org.apache.spark.graphx.Graph

import scala.reflect.ClassTag

/**
  * Created by volodymyrmiz on 05.10.16.
  */
package object Utils {
  /** *
    * Hide Apache Spark console logs.
    *
    * @param params List of logs to be suppressed.
    */
  def suppressLogs(params: List[String]): Unit = {
    import org.apache.log4j.{Level, Logger}
    params.foreach(Logger.getLogger(_).setLevel(Level.OFF))
  }

  def removeLowWeightEdges[VD: ClassTag, ED: ClassTag](graph: Graph[VD, ED], minWeight: Double) = {
    Graph(graph.vertices,
      graph.edges.filter(_.attr.toString.toDouble > minWeight)
    )
  }

  def removeSingletons[VD: ClassTag, ED: ClassTag](graph: Graph[VD, ED]) =
    Graph(graph.triplets.map(et => (et.srcId, et.srcAttr))
      .union(graph.triplets.map(et => (et.dstId, et.dstAttr)))
      .distinct,
      graph.edges)

  def compareTimeSeries(m1: Map[Int, Double], m2: Map[Int, Double]): Double = {
    val commonKeys = m2.keySet.intersect(m1.keySet)
      .filter(hour => hour > 2200 & hour < 3000) // take only specified hours (e.g. the first month is 0 - 744 hours)

    val m1Freq = m1.size
    val m2Freq = m2.size

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

  def toGexf[VD, ED](g: Graph[VD, ED]) =
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
}
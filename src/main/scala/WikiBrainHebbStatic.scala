import ch.epfl.lts2.Utils._
import ch.epfl.lts2.Globals._
import org.apache.spark.graphx.{Edge, Graph, VertexId}
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.SparkSession

import scala.reflect.io.Path

/**
  * Created by volodymyrmiz on 15.05.17.
  */
object WikiBrainHebbStatic {
  def main(args: Array[String]): Unit = {
    suppressLogs(List("org", "akka"))

    val spark = SparkSession.builder
      .master("local[*]")
      .appName("Wiki Brain")
      .config("spark.driver.maxResultSize", "10g")
      .config("spark.executor.memory", "50g")
      .getOrCreate()

    println("Read vertices from disk...")
    val verticesRDD: RDD[(VertexId, (String, Map[Int, Double]))] = spark.sparkContext.objectFile(PATH_RESOURCES + "RDDs/staticVerticesRDD")

    println("Vertices RDD: " +  verticesRDD.count())

    val vertexIDs = verticesRDD.map(_._1.toLong).collect().toSet

    /**
      * Filter edges
      */
//    val edgesRDD: RDD[Edge[Double]] = edgesDF.as[(String, String)].rdd
//      .coalesce(12)
//      .map(e => (e._1.toLong, e._2.toLong))
//      .filter(e => vertexIDs.contains(e._1) & vertexIDs.contains(e._2))
//      .map(e => Edge(e._1, e._2, 0.0)).cache()
//
//    println("Edges filtered: " + edgesRDD.count())
//
//    Path(PATH_RESOURCES + "edges").deleteRecursively()
//    edgesRDD.saveAsObjectFile(PATH_RESOURCES + "edges")

    /**
      * Read edges from file
      */
    val edgesRDD: RDD[Edge[Double]] = spark.sparkContext.objectFile(PATH_RESOURCES + "edges")
      .filter(e => vertexIDs.contains(e.srcId) & vertexIDs.contains(e.dstId))

//    Path(PATH_RESOURCES + "vertices").deleteRecursively()
//    verticesRDD.saveAsObjectFile(PATH_RESOURCES + "vertices")

    val graph = Graph(verticesRDD, edgesRDD)
    println("Vertices in graph: " + graph.vertices.count())
    println("Edges in graph: " + graph.edges.count())

    val trainedGraph = graph.mapTriplets(trplt => compareTimeSeries(trplt.dstAttr._2, trplt.srcAttr._2, start = OCT_START, stop = APR_END, isFiltered = true))

    val prunedGraph = removeLowWeightEdges(trainedGraph, minWeight = 100.0)

    println("Vertices in trained graph: " + prunedGraph.vertices.count())
    println("Edges in trained graph: " + prunedGraph.edges.count())

//    val LCCgraph = getLargestConnectedComponent(prunedGraph)
//    println("Vertices left in LCC: " + LCCgraph.vertices.count())
//    println("Edges left in LCC: " + LCCgraph.edges.count())
//
//    saveGraph(LCCgraph.mapVertices((vID, attr) => attr._1), PATH_RESOURCES + "graph.gexf")

    val cleanGraph = removeSingletons(prunedGraph)
    val CC = getLargestConnectedComponent(cleanGraph)

    println("Vertices in LCC graph: " + CC.vertices.count())
    println("Edges in LCC graph: " + CC.edges.count())

    saveGraph(CC.mapVertices((vID, attr) => attr._1), PATH_RESOURCES + "graph.gexf")

    spark.stop()
  }
}

import ch.epfl.lts2.Utils._
import ch.epfl.lts2.Globals._
import org.apache.spark.graphx.{Edge, Graph, VertexId}
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.SparkSession

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

    println("Read edges from disk...")
    val edgesRDD: RDD[Edge[Double]] = spark.sparkContext.objectFile(PATH_RESOURCES + "RDDs/staticEdgesRDD")
      .filter(e => vertexIDs.contains(e.srcId) & vertexIDs.contains(e.dstId))

    val graph = Graph(verticesRDD, edgesRDD)
    println("Vertices in graph: " + graph.vertices.count())
    println("Edges in graph: " + graph.edges.count())

    val trainedGraph = graph.mapTriplets(trplt => compareTimeSeries(trplt.dstAttr._2, trplt.srcAttr._2, start = OCT_START, stop = APR_END, isFiltered = true))

    val prunedGraph = removeLowWeightEdges(trainedGraph, minWeight = 100.0)

    println("Vertices in trained graph: " + prunedGraph.vertices.count())
    println("Edges in trained graph: " + prunedGraph.edges.count())

    val cleanGraph = removeSingletons(prunedGraph)
    val CC = getLargestConnectedComponent(cleanGraph)

    println("Vertices in LCC graph: " + CC.vertices.count())
    println("Edges in LCC graph: " + CC.edges.count())

    saveGraph(CC.mapVertices((vID, attr) => attr._1), PATH_RESOURCES + "graph.gexf")

    spark.stop()
  }
}

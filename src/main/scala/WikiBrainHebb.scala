import org.apache.spark.sql.SparkSession
import ch.epfl.lts2.Utils._
import ch.epfl.lts2.Globals._
import org.apache.spark.graphx.{Edge, Graph, VertexId}
import org.apache.spark.rdd.RDD

/**
  * Created by volodymyrmiz on 29.04.17.
  */
object WikiBrainHebb {
  def main(args: Array[String]): Unit = {

    suppressLogs(List("org", "akka"))

    println("WikiBrainHebb.scala")

    val spark = SparkSession.builder
      .master("local[*]")
      .appName("Wiki Brain")
      .config("spark.driver.maxResultSize", "10g")
      .config("spark.executor.memory", "50g")
      .getOrCreate()

    val path: String = "./src/main/resources/wikiTS/"
    val fileName: String = "signal_500.csv"

    val rdd: RDD[(Long, Map[Int, Double])] = spark.sparkContext.objectFile(PATH_RESOURCES + "RDDs/tabularSignalRDD")

    println("Total number of pages: " + rdd.count())

    val startTime = JAN_START
    val endTime = JAN_END

    val verticesRDD = rdd
//      .mapValues(visitsTS => visitsTS.toSparse.indices.zip(visitsTS.toSparse.values).toMap)
      .filter(v => v._2.keys.count(key => key > startTime & key < endTime) > 0)
      .mapValues(v => v.filter(m => m._1 > startTime & m._1 < endTime))
//      .filter(v => v._2.keys.size > 10 & v._2.keys.size < 730)
      .filter(v => v._2.values.max > 5000)

    println("Number of pages with a certain number visits: " + verticesRDD.count())

    println("Edges generation...")
    val edgeIndexesRDD = verticesRDD.map(_._1).repartition(12).cache()
    edgeIndexesRDD.take(1)

    val edgesRDD = edgeIndexesRDD.cartesian(edgeIndexesRDD)
      .filter { case (a, b) => a < b }
      .map(pair => Edge(pair._1, pair._2, 1.0))

    val verticesGX: RDD[(VertexId, Map[Int, Double])] = verticesRDD
    val edgesGX: RDD[Edge[Double]] = edgesRDD

    val graph = Graph(verticesGX, edgesGX)

    println("Applying Hebbian plasticity... N pages: " + verticesGX.count() + "; N edges: " + edgesGX.count())
    val trainedGraph = graph.mapTriplets(trplt => compareTimeSeries(trplt.dstAttr, trplt.srcAttr, start = startTime, stop = endTime, isFiltered = false)).mapVertices((vID, attr) => vID)

    println("Removing low weight edges...")
    val prunedGraph = removeLowWeightEdges(trainedGraph, minWeight = 500.0)
    println("Filtered graph with " + prunedGraph.edges.count() + " edges.")

    println("Removing singletone vertices...")
    val cleanGraph = removeSingletons(prunedGraph)
    println(cleanGraph.vertices.count() + " vertices left.")

    // Name vertices by IDs
    val idsfileName: String = "ids_titles_for_500_filtered.csv"

    val idsDF = spark.sqlContext.read
      .format("com.databricks.spark.csv")
      .options(Map("header"->"false", "inferSchema"->"true"))
      .load(path + idsfileName)

    val idsTitlesMap = idsDF.collect.map(pair => pair{0} -> pair{1}).toMap

    val graphWithIds = cleanGraph.mapVertices((vId, v) => idsTitlesMap(v).toString.replace('&', ' ').replace("""\n""", ""))

    println(graphWithIds.vertices.count() + " vertices left.")

    println("Saving graph...")
    saveGraph(graphWithIds, path + "graph.gexf")
  }
}

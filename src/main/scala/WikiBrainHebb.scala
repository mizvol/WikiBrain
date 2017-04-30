import org.apache.spark.sql.SparkSession
import org.apache.spark.mllib.linalg.Vectors
import ch.epfl.lts2.Utils._
import org.apache.spark.graphx.{Edge, Graph, VertexId}
import org.apache.spark.rdd.RDD

/**
  * Created by volodymyrmiz on 29.04.17.
  */
object WikiBrainHebb {
  def main(args: Array[String]): Unit = {

    suppressLogs(List("org", "akka"))

    val spark = SparkSession.builder
      .master("local")
      .appName("Wiki Brain")
      .getOrCreate()

    val path: String = "./src/main/resources/wikiTS/"
    val fileName: String = "signal_20000.csv"

    val df = spark.sqlContext.read
      .format("com.databricks.spark.csv")
      .options(Map("header"->"true", "inferSchema"->"true"))
      .load(path + fileName)
      .drop("_c0")

    println("Reading and transforming the data into GraphX vertex format...")
    // Transform a dataframe into (pageID, SparseVector) format. Only God and I know how it works. Sorry. Tomorrow only God will know.
    val rdd = df.toJavaRDD.rdd.map(_.toSeq.toList).map(page => (page.head.toString.toLong, (page{2}, page{1}))).groupBy(_._1)
      .mapValues(page => page.map(_._2).groupBy(_._1).map{case(k,v) => (k, v.map(_._2))})
      .mapValues(pair => (pair.keys.map(_.toString.toInt).toArray, pair.values.map(_.head.toString.toDouble).toArray))
      .mapValues(pair => Vectors.sparse(pair._1.length, pair._1, pair._2).toSparse)

    println("Total number of pages: " + rdd.count())

    val verticesRDD = rdd.mapValues(visitsTS => visitsTS.indices.zip(visitsTS.values).toMap)

    println("Edges generation...")
    val edgeIndexesRDD = rdd.map(_._1).repartition(12).cache()
    edgeIndexesRDD.take(1)
//    spark.sqlContext.sql("SET spark.sql.autoBroadcastJoinThreshold = 0")

    val edgesRDD = edgeIndexesRDD.cartesian(edgeIndexesRDD)
      .filter { case (a, b) => a < b }
      .map(pair => Edge(pair._1, pair._2, 0.0))

//    spark.sqlContext.sql("SET spark.sql.autoBroadcastJoinThreshold = 1")

    val verticesGX: RDD[(VertexId, Map[Int, Double])] = verticesRDD
    val edgesGX: RDD[Edge[Double]] = edgesRDD

    val graph = Graph(verticesGX, edgesGX)

    println("Applying Hebbian plasticity... N pages: " + verticesGX.count() + "; N edges: " + edgesGX.count())
    val trainedGraph = graph.mapTriplets(trplt => compareTimeSeries(trplt.dstAttr, trplt.srcAttr)).mapVertices((vID, attr) => vID)

    println("Removing low weight edges...")
    val prunedGraph = removeLowWeightEdges(trainedGraph, minWeight = 1.5)
    println("Filtered graph with " + prunedGraph.edges.count() + " edges.")

    println("Removing singletone vertices...")
    val cleanGraph = removeSingletons(prunedGraph)
    println(cleanGraph.vertices.count() + " vertices left.")

    val idsfileName: String = "ids_titles_for_20000_filtered.csv"

    val idsDF = spark.sqlContext.read
      .format("com.databricks.spark.csv")
      .options(Map("header"->"false", "inferSchema"->"true"))
      .load(path + idsfileName)

    val idsTitlesMap = idsDF.collect.map(pair => pair{0} -> pair{1}).toMap

    val graphWithIds = cleanGraph.mapVertices((vId, v) => idsTitlesMap(v))

    println("Saving graph...")
    saveGraph(graphWithIds, path + "graph.gexf")
  }
}

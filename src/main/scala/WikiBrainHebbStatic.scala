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

    import spark.implicits._

    val verticesDF = spark.sqlContext.read
      .format("com.databricks.spark.csv")
      .option("header", "false")
      .option("inferSchema", "false")
      .option("delimiter", ",")
      .load(PATH_RESOURCES + "csv/vertices.csv")

    println("Vertices initially: " + verticesDF.count())

    val edgesDF = spark.sqlContext.read
      .format("com.databricks.spark.csv")
      .option("header", "false")
      .option("inferSchema", "false")
      .option("delimiter", " ")
      .load(PATH_RESOURCES + "csv/edges.csv")

    println("Edges initially: " + edgesDF.count())

    val timeSeriesDF = spark.sqlContext.read
      .format("com.databricks.spark.csv")
      .options(Map("header"->"true", "inferSchema"->"true"))
      .load(PATH_RESOURCES + "wikiTS/signal_500.csv")
      .drop("_c0")
      .rdd
      .map(_.toSeq.toList).map(page => (page.head.toString.toLong, (page{2}, page{1}))).groupBy(_._1)
      .mapValues(page => page.map(_._2).groupBy(_._1).map{case(k,v) => (k, v.map(_._2))})
      .mapValues(pair => (pair.keys.map(_.toString.toInt).toArray, pair.values.map(_.head.toString.toDouble).toArray))
      .mapValues(pair => Vectors.sparse(pair._1.length, pair._1, pair._2).toSparse)
      .mapValues(visitsTS => visitsTS.indices.zip(visitsTS.values).toMap)
      .toDF(Seq("_c0", "_c1"): _*)

    println("Vertices with timeseries: " + timeSeriesDF.count())

    val verticesTimeSeriesDF = verticesDF.join(timeSeriesDF, Seq("_c0"), "outer").toDF(Seq("id", "title", "ts"): _*).filter("ts is not null")

    println("Vertices merged: " + verticesTimeSeriesDF.count())

    val verticesRDD: RDD[(VertexId, (String, Map[Int, Double]))] = verticesTimeSeriesDF
      .as[(String, String, Map[Int, Double])]
      .rdd
      .filter(v => isAllDigits(v._1))
      .map(v => (v._1.toLong, (v._2, v._3)))

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

//    val graph = Graph(verticesRDD.map(v => (v._1, v._2._1)), edgesRDD)
    val graph = Graph(verticesRDD, edgesRDD)
    println("Vertices in graph: " + graph.vertices.count())

    val trainedGraph = graph.mapTriplets(trplt => compareTimeSeries(trplt.dstAttr._2, trplt.srcAttr._2, start = 0, stop = 6000))
    println("Vertices in trained graph: " + trainedGraph.vertices.count())
    println("Edges in trained graph: " + trainedGraph.edges.count())

    val prunedGraph = removeLowWeightEdges(trainedGraph, minWeight = 1.0)

    val LCCgraph = getLargestConnectedComponent(prunedGraph)
    println("Vertices left in LCC: " + LCCgraph.vertices.count())
    println("Edges left in LCC: " + LCCgraph.edges.count())

//    val LCCgraph = getLargestConnectedComponent(graph)
//
    saveGraph(LCCgraph.mapVertices((vID, attr) => attr._1), PATH_RESOURCES + "graph.gexf")

    spark.stop()
  }
}

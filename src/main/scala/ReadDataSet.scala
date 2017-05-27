import ch.epfl.lts2.Utils._
import ch.epfl.lts2.Globals._
import org.apache.spark.graphx.{Edge, VertexId}
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.SparkSession

import scala.reflect.io.Path

/**
  * Created by volodymyrmiz on 25.05.17.
  */
object ReadDataSet {
  def main(args: Array[String]): Unit = {
    suppressLogs(List("org", "akka"))

    val spark = SparkSession.builder
      .master("local[*]")
      .appName("Wiki Brain")
      .config("spark.driver.maxResultSize", "10g")
      .config("spark.executor.memory", "50g")
      .getOrCreate()

    val fileNameLayered: String = "signal_500.csv"
    val fileNameTabular: String = "activations_100.csv"
    val pathToCSV: String = PATH_RESOURCES + "csv/"
    val pathToTimeSeries: String = PATH_RESOURCES + "wikiTS/"

//    readLayeredSignal(pathToTimeSeries + fileNameLayered, spark)
//
//    readTabularSignal(pathToTimeSeries + fileNameTabular, spark)

    readStaticGraph(pathToCSV, pathToTimeSeries, spark)
  }

  private def readLayeredSignal(path: String, spark: SparkSession) = {
    val df = spark.sqlContext.read
      .format("com.databricks.spark.csv")
      .options(Map("header" -> "true", "inferSchema" -> "true"))
      .load(path)
      .drop("_c0")

    val rdd = df.toJavaRDD.rdd.map(_.toSeq.toList).map(page => (page.head.toString.toLong, (page {2}, page {1}))).groupBy(_._1)
      .mapValues(page => page.map(_._2).groupBy(_._1).map { case (k, v) => (k, v.map(_._2)) })
      .mapValues(pair => (pair.keys.map(_.toString.toInt).toArray, pair.values.map(_.head.toString.toDouble).toArray))
      .mapValues(pair => Vectors.sparse(pair._1.length, pair._1, pair._2).toSparse)
      .mapValues(visitsTS => visitsTS.indices.zip(visitsTS.values).toMap)

    Path(PATH_RESOURCES + "RDDs/layeredSignalRDD").deleteRecursively()
    rdd.saveAsObjectFile(PATH_RESOURCES + "RDDs/layeredSignalRDD")
  }

  private def readTabularSignal(path: String, spark: SparkSession) = {
    val df = spark.sqlContext.read
      .format("com.databricks.spark.csv")
      .options(Map("header" -> "true", "inferSchema" -> "true"))
      .load(path)

    val rdd = df.rdd.map(v => (v {0}.toString.toLong, v.toSeq.toList.drop(1).map(_.toString.toDouble))).mapValues(v => (v.indices.map(_ + 1) zip v).toMap.filter(_._2 != 0))

    Path(PATH_RESOURCES + "RDDs/tabularSignalRDD").deleteRecursively()
    rdd.saveAsObjectFile(PATH_RESOURCES + "RDDs/tabularSignalRDD")
  }

  private def readStaticGraph(pathToCSV: String, pathToTimeSeries: String, spark: SparkSession) = {

    import spark.implicits._

    val verticesDF = spark.sqlContext.read
      .format("com.databricks.spark.csv")
      .option("header", "false")
      .option("inferSchema", "false")
      .option("delimiter", ",")
      .load(pathToCSV + "vertices.csv")

    println("Vertices initially: " + verticesDF.count())

    val edgesDF = spark.sqlContext.read
      .format("com.databricks.spark.csv")
      .option("header", "false")
      .option("inferSchema", "false")
      .option("delimiter", " ")
      .load(pathToCSV + "/edges.csv")

    println("Edges initially: " + edgesDF.count())

    val timeSeriesDF = spark.sqlContext.read
      .format("com.databricks.spark.csv")
      .options(Map("header" -> "true", "inferSchema" -> "true"))
      .load(pathToTimeSeries + "signal_500.csv")
      .drop("_c0")
      .rdd
      .map(_.toSeq.toList).map(page => (page.head.toString.toLong, (page {2}, page {1}))).groupBy(_._1)
      .mapValues(page => page.map(_._2).groupBy(_._1).map { case (k, v) => (k, v.map(_._2)) })
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
      .filter(v => v._2 != null)
      .map(v => (v._1.toLong, (v._2.replace("&", "").replace("""\"""", ""), v._3)))

    Path(PATH_RESOURCES + "RDDs/staticVerticesRDD").deleteRecursively()
    verticesRDD.saveAsObjectFile(PATH_RESOURCES + "RDDs/staticVerticesRDD")

    val vertexIDs = verticesRDD.map(_._1.toLong).collect().toSet

    val edgesRDD: RDD[Edge[Double]] = edgesDF.as[(String, String)].rdd
      .coalesce(12)
      .map(e => (e._1.toLong, e._2.toLong))
      .filter(e => vertexIDs.contains(e._1) & vertexIDs.contains(e._2))
      .map(e => Edge(e._1, e._2, 0.0)).cache()

    println("Edges filtered: " + edgesRDD.count())

    Path(PATH_RESOURCES + "RDDs/staticEdgesRDD").deleteRecursively()
    edgesRDD.saveAsObjectFile(PATH_RESOURCES + "RDDs/staticEdgesRDD")
  }
}

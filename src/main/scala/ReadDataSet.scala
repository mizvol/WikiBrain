import ch.epfl.lts2.Utils._
import ch.epfl.lts2.Globals._
import org.apache.spark.mllib.linalg.Vectors
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

    val path: String = "./src/main/resources/wikiTS/"
    val fileNameLayered: String = "signal_500.csv"
    val fileNameTabular: String = "activations_100.csv"

    readLayeredSignal(path + fileNameLayered, spark)

    readTabularSignal(path + fileNameTabular, spark)
  }

  private def readLayeredSignal(path: String, spark: SparkSession) = {
    val df = spark.sqlContext.read
      .format("com.databricks.spark.csv")
      .options(Map("header"->"true", "inferSchema"->"true"))
      .load(path)
      .drop("_c0")

    val rdd = df.toJavaRDD.rdd.map(_.toSeq.toList).map(page => (page.head.toString.toLong, (page{2}, page{1}))).groupBy(_._1)
      .mapValues(page => page.map(_._2).groupBy(_._1).map{case(k,v) => (k, v.map(_._2))})
      .mapValues(pair => (pair.keys.map(_.toString.toInt).toArray, pair.values.map(_.head.toString.toDouble).toArray))
      .mapValues(pair => Vectors.sparse(pair._1.length, pair._1, pair._2).toSparse)
      .mapValues(visitsTS => visitsTS.indices.zip(visitsTS.values).toMap)

    Path(PATH_RESOURCES + "RDDs/layeredSignalRDD").deleteRecursively()
    rdd.saveAsObjectFile(PATH_RESOURCES + "RDDs/layeredSignalRDD")
  }

  private def readTabularSignal(path: String, spark: SparkSession) = {
    val df = spark.sqlContext.read
      .format("com.databricks.spark.csv")
      .options(Map("header"->"true", "inferSchema"->"true"))
      .load(path)

    val rdd = df.rdd.map(v => (v{0}.toString.toLong, v.toSeq.toList.drop(1).map(_.toString.toDouble))).mapValues(v => (v.indices.map(_ + 1) zip v).toMap.filter(_._2 != 0))

    Path(PATH_RESOURCES + "RDDs/tabularSignalRDD").deleteRecursively()
    rdd.saveAsObjectFile(PATH_RESOURCES + "RDDs/tabularSignalRDD")
  }
}

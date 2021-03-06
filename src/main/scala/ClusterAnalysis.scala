import ch.epfl.lts2.Globals._
import ch.epfl.lts2.Utils.suppressLogs
import org.apache.spark.graphx.VertexId
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.SparkSession
import org.slf4j.{Logger, LoggerFactory}
import org.apache.spark.mllib.linalg.Vectors

/**
  * Created by volodymyrmiz on 26.07.17.
  */
object ClusterAnalysis {
  def main(args: Array[String]): Unit = {
    suppressLogs(List("org", "akka"))

    val log: Logger = LoggerFactory.getLogger(this.getClass)

    val spark = SparkSession.builder
      .master("local[*]")
      .appName("Cluster Analysis")
      .config("spark.driver.maxResultSize", "10g")
      .config("spark.executor.memory", "50g")
      .getOrCreate()

    val verticesRDD: RDD[(Long, Map[Int, Double])] = spark.sparkContext.objectFile(PATH_RESOURCES + "RDDs/layeredSignalRDD")

    val SUPER_BOWL = "clusters/trainded_graphs_1_0_cleaned_FEB_SB_cluster.csv"
    val WW2 = "clusters/trainded_graphs_1_0_cleaned_WW2.csv"
    val CHARLIE = "clusters/trainded_graphs_1_0_cleaned _Charlie.csv"
    val CHARLIE_CLUSTER = "clusters/trainded_graphs_1_0_cleaned_charlie_cluster.csv"
    val CHARLIE_WEEK = "clusters/Charlie_Hebdo_one_week_5_12_January.csv"
    val FERGUSON = "clusters/Ferguson_one_week_22_03_December.csv"
    val GERMANWINGS_WEEK = "clusters/GermanWings_week.csv"
    val GERMANWINGS_MONTH = "clusters/GermanWings_month.csv"

    val FERGUSON_STDDEV = "/clusters/ferguson_week_stddev.csv"
    val CHARLIE_STDDEV = "/clusters/charlie_week_stddev.csv"
    val GERMANWINGS_STDDEV = "/clusters/germanwings_week_stddev.csv"

    val df = spark.sqlContext.read
      .format("com.databricks.spark.csv")
      .options(Map("header" -> "false", "inferSchema" -> "false"))
      .load(PATH_RESOURCES + GERMANWINGS_STDDEV)

    val idList = df.select("_c0").collect().map(_.toSeq.toList).map(_.head.toString.toLong)

    val clusterMap = verticesRDD.filter(v => idList.contains(v._1))

    val clusterDense = clusterMap.mapValues(v => v.toList).mapValues(v => Vectors.sparse(TOTAL_HOURS, v).toDense)

    clusterDense.coalesce(1).saveAsTextFile(PATH_RESOURCES + "clusters/germanwings_stddev")
  }
}

import ch.epfl.lts2.Utils._
import org.apache.spark.graphx.{Edge, Graph, VertexId}
import org.apache.spark.sql.{Row, SparkSession}
import java.security.MessageDigest

import org.apache.spark.sql.types._


/**
  * Created by volodymyrmiz on 30.04.17.
  */
object Test {
  def main(args: Array[String]): Unit = {
    suppressLogs(List("org", "akka"))

    val spark = SparkSession.builder
      .master("local")
      .appName("Test")
      .getOrCreate()

    val sc = spark.sparkContext
    import spark.sqlContext.implicits._
    //    val path: String = "./src/main/resources/wikiTS/"
    //    val idsfileName: String = "ids_titles_for_20000_filtered.csv"
    //
    //    val idsDF = spark.sqlContext.read
    //      .format("com.databricks.spark.csv")
    //      .options(Map("header"->"false", "inferSchema"->"true"))
    //      .load(path + idsfileName)
    //
    //    val idsTitlesMap = idsDF.collect.map(pair => pair{0} -> pair{1}).toMap


//    val vertices =
//      sc.parallelize(Seq[(VertexId, String)]((1, "one"), (2, "two"), (3, "three")), 2)
//    val edges = sc.parallelize((Seq(Edge(1, 2, true), Edge(2, 3, false), Edge(3, 1, false))))
//    val g: Graph[String, Boolean] = Graph(vertices, edges)
//
//    val c = g.coarsen(_.attr, _ + _).cache()
//    val cV = c.vertices.collect.toSet
//    assert(
//      cV == Set((1L, "onetwo"), (3L, "three")) ||
//        cV == Set((1L, "twoone"), (3L, "three")))
//
//    println("End")

    val str = List ("String", "Another_string", "String,", "String", "String")

    def md5(s: String) = {
      MessageDigest.getInstance("MD5").digest(s.getBytes)
    }


    for (s <- str) {
      println(s.hashCode)

    }

    val path: String = "./src/main/resources/wikiTS/"

    val wikiData = sc.textFile(path + "pagecounts-20160101-000000.gz").map(_.split(" ")).map(row => Row(row(0), row(1), row(2), row(3)))

    val schemaString = "lang pageName pageCount size"

    val fields = schemaString.split(" ")
      .map(fieldName => StructField(fieldName, StringType, nullable = true))
    val schema = StructType(fields)

    val wikiDataDF = spark.createDataFrame(wikiData, schema)

    wikiDataDF.filter("lang = 'en'").filter("pageCount > 100").show()
  }
}

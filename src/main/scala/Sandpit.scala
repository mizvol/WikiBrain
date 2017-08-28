import ch.epfl.lts2.Globals.PATH_RESOURCES
import ch.epfl.lts2.Utils._
import org.apache.spark.graphx.{Edge, Graph, VertexId}
import org.apache.spark.mllib.linalg.Vector
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.SparkSession

/**
  * Created by volodymyrmiz on 30.04.17.
  */
object Sandpit {
  def main(args: Array[String]): Unit = {
    suppressLogs(List("org", "akka"))

    val spark = SparkSession.builder
      .master("local[*]")
      .appName("Test")
      .config("spark.driver.maxResultSize", "2g")
      .config("spark.executor.memory", "50g")
      .getOrCreate()

    import org.apache.spark.mllib.stat.Statistics

    val sc = spark.sparkContext

    val seriesX: RDD[Double] = sc.parallelize(Array(1, 2, 3, 4, 5))
    val seriesY: RDD[Double] = sc.parallelize(Array(500, 600, 700, 800, 9000))

    val correlation: Double = Statistics.corr(seriesX, seriesY, "pearson")

    println(correlation)

    val map1: Map[Int, Double] = Map(1->1, 2->2, 3->3, 4->4, 5->5)
    val map2: Map[Int, Double] = Map(1->500, 2->600, 3->700, 4->800, 5->9000)

    println(pearsonCorrelation(map1, map2, start = 0, stop = 6))

    val seq = Seq(1,2,9,7,5,11,4).sorted.foreach(println)

//    implicit val ctx:SparkContext = spark.sparkContext

    //    val path: String = "./src/main/resources/wikiTS/"
    //    val idsfileName: String = "ids_titles_for_20000_filtered.csv"
    //
    //    val idsDF = spark.sqlContext.read
    //      .format("com.databricks.spark.csv")
    //      .options(Map("header"->"false", "inferSchema"->"true"))
    //      .load(path + idsfileName)
    //
    //    val idsTitlesMap = idsDF.collect.map(pair => pair{0} -> pair{1}).toMap

    /**
      * Coarsening test @author Dave Ankur
      */
//    val verticesRDD = spark.sparkContext.parallelize(Seq[(VertexId, String)]((1, "one"), (2, "two"), (3, "three")), 2)
//    val edgesRDD = spark.sparkContext.parallelize((Seq(Edge(1, 2, true), Edge(2, 3, false), Edge(3, 1, false), Edge(10,11, false))))
//    val g: Graph[String, Boolean] = Graph(verticesRDD, edgesRDD)
//    println(g.edges.count())

//    val c = g.coarsen(_.attr, _ + _).cache()
//    val cV = c.vertices.collect.toSet
//    assert(
//      cV == Set((1L, "onetwo"), (3L, "three")) ||
//        cV == Set((1L, "twoone"), (3L, "three")))
//
//    println("End")

//    import org.apache.spark.graphx.lib.ShortestPaths
//
//    val shortestPathGraph = ShortestPaths.run(g, Seq(1))
//
//    println(shortestPathGraph.vertices.filter(_._2.nonEmpty).mapValues(_.values.toList.head).collect().maxBy(_._2))

    /**
      * Load wiki page counts into a Spark DF
      */
//    val str = List ("String", "Another_string", "String,", "String", "String")
//
//    def md5(s: String) = {
//      MessageDigest.getInstance("MD5").digest(s.getBytes)
//    }
//
//
//    for (s <- str) {
//      println(s.hashCode)
//
//    }
//
//    val path: String = "./src/main/resources/wikiTS/"
//
//    val wikiData = sc.textFile(path + "pagecounts-20160101-000000.gz").map(_.split(" ")).map(row => Row(row(0), row(1), row(2), row(3)))
//
//    val schemaString = "lang pageName pageCount size"
//
//    val fields = schemaString.split(" ")
//      .map(fieldName => StructField(fieldName, StringType, nullable = true))
//    val schema = StructType(fields)
//
//    val wikiDataDF = spark.createDataFrame(wikiData, schema)
//
//    wikiDataDF.filter("lang = 'en'").filter("pageCount > 100").show()

    /**
      * Load GraphML to GraphX. Note: Change ID type from LONG to INT before loading, since LONG is not supported
      */

//    val pathSSD = "/home/volodymyrmiz/wikiGraphML.xml"

//    implicit val ctx: SparkContext = spark.sparkContext
//
//    val graph = LoadGraph.from(GraphML(pathSSD)).load()
//
//    Path("/home/volodymyrmiz/vertices").deleteRecursively()
//    graph.vertices.saveAsObjectFile("/home/volodymyrmiz/vertices")
//    Path("/home/volodymyrmiz/edges").deleteRecursively()
//    graph.edges.saveAsObjectFile("/home/volodymyrmiz/edges")


    /**
      * Load graph from file
      */
//    val vertices: RDD[(VertexId, Map[String, Int])] = spark.sparkContext.objectFile("/home/volodymyrmiz/vertices")
//    val edges: RDD[Edge[Map[String, String]]] = spark.sparkContext.objectFile("/home/volodymyrmiz/edges")
//
//    val graph1 = Graph(vertices, edges)
//    println(graph1.vertices.take(10).mkString("\n"))
//    println(graph1.edges.take(10).mkString("\n"))


    /**
      * Load graph from csv
      */

//    val path = "/mnt/data/git/wiki/notebook/"
//
//    val vertices = spark.sqlContext.read
//      .format("com.databricks.spark.csv")
//      .option("header", "false")
//      .option("inferSchema", "false")
//      .option("delimiter", ",")
//      .load(path + "vertices.csv")
//
//    vertices.show()
//
//    val edges = spark.sqlContext.read
//      .format("com.databricks.spark.csv")
//      .option("header", "false")
//      .option("inferSchema", "false")
//      .option("delimiter", " ")
//      .load(path + "edges.csv")
//
//    edges.show()


//
//  val rdd: RDD[(Long, Map[Int, Double])] = spark.sparkContext.objectFile(PATH_RESOURCES + "RDDs/layeredSignalRDD")
//
//    println(rdd.mapValues(_.keys.size).take(20).mkString("\n"))
  }
}
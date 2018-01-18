import ch.epfl.lts2.Globals.PATH_RESOURCES
import ch.epfl.lts2.Utils.suppressLogs
import org.apache.spark.graphx.{Edge, Graph, VertexId}
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.SparkSession
import org.slf4j.{Logger, LoggerFactory}
import ch.epfl.lts2.Utils._
import ch.epfl.lts2.Globals._

/**
  * Created by volodymyrmiz on 18.01.18.
  */
object WikiPeaksGraph {
  def main(args: Array[String]): Unit = {
    suppressLogs(List("org", "akka"))

    val log: Logger = LoggerFactory.getLogger(this.getClass)

    val spark = SparkSession.builder
      .master("local[*]")
      .appName("Wiki Brain")
      .config("spark.driver.maxResultSize", "20g")
      .config("spark.executor.memory", "50g")
      .getOrCreate()

    log.info("Read vertices from disk...")
    val verticesRDD: RDD[(VertexId, (String, Map[Int, Double]))] = spark.sparkContext.objectFile(PATH_RESOURCES + "RDDs/staticVerticesRDD")

    val sc = spark.sparkContext
    //    val vertices = verticesRDD.mapValues(v => (v._1, Vectors.sparse(v._2.size, v._2.keys.toList.toArray, v._2.values.toList.toArray).toArray)).mapValues(v => (v._1, sc.parallelize(v._2)))

    log.info("Vertices RDD: " + verticesRDD.count())

    val vertexIDs = verticesRDD.map(_._1.toLong).collect().toSet

    log.info("Read edges from disk...")
    val edgesRDD: RDD[Edge[Double]] = spark.sparkContext.objectFile(PATH_RESOURCES + "RDDs/staticEdgesRDD")
      .filter(e => vertexIDs.contains(e.srcId) & vertexIDs.contains(e.dstId))

    val graph = Graph(verticesRDD, edgesRDD)
    log.info("Vertices in graph: " + graph.vertices.count())
    log.info("Edges in graph: " + graph.edges.count())

    val peaksVertices = graph.vertices.map(v => (v._1, (v._2._1, mapToList(v._2._2, TOTAL_HOURS), v._2._2)))
      .filter(v => v._2._3.values.count(l => l > 3 * stddev(v._2._2, v._2._3.values.sum / TOTAL_HOURS)) > 10)
      .map(v=> (v._1, (v._2._1, v._2._3)))

    val vIDs = peaksVertices.map(_._1).collect().toSeq

    val peaksEgdes = graph.edges.filter(e => vIDs.contains(e.dstId) & vIDs.contains(e.srcId))

    val peaksGraph = Graph(peaksVertices, peaksEgdes)

    log.info("Vertices in graph: " + peaksGraph.vertices.count())
    log.info("Edges in graph: " + peaksGraph.edges.count())

//    saveGraph(peaksGraph.mapVertices((id, v) => v._1), fileName = PATH_RESOURCES + "peaks_graph.gexf")
  }
}
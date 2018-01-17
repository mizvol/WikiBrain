import ch.epfl.lts2.Globals.{FEB_END, FEB_SRART, PATH_RESOURCES}
import ch.epfl.lts2.Utils.{compareTimeSeries, getLargestConnectedComponent, removeLowWeightEdges, removeSingletons, saveGraph, suppressLogs}
import org.apache.spark.graphx.VertexId
import org.apache.spark.graphx.util.GraphGenerators
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.SparkSession
import org.apache.spark.graphx._
import org.slf4j.{Logger, LoggerFactory}

/**
  * Created by volodymyrmiz on 17.01.18.
  */
object RandomWikiBrain {
  def main(args: Array[String]): Unit = {
    suppressLogs(List("org", "akka"))

    val log: Logger = LoggerFactory.getLogger(this.getClass)

    val spark = SparkSession.builder
      .master("local[*]")
      .appName("Wiki Brain")
      .config("spark.driver.maxResultSize", "20g")
      .config("spark.executor.memory", "50g")
      .getOrCreate()

    val sc = spark.sparkContext

    val graph: Graph[Int, Int] = GraphGenerators.rmatGraph(sc, 116016*2, 15000000)



    log.info("Read vertices from disk...")
    val verticesRDD: RDD[(VertexId, (String, Map[Int, Double]))] = spark.sparkContext.objectFile(PATH_RESOURCES + "RDDs/staticVerticesRDD")

    val vRDD =verticesRDD.zipWithIndex().map(v => (v._2, v._1._2))

    val vIDs = vRDD.map(v => v._1).collect().toSet

    val edges: RDD[Edge[Double]] = graph.edges
      .filter(e => e.dstId != e.srcId)
      .filter(e => vIDs.contains(e.dstId - 131072) & vIDs.contains(e.srcId - 131072))
      .map(e => Edge(e.srcId - 131072, e.dstId - 131072))

//    println(vIDs.min)
//    println(vIDs.max)
//
//    println(edges.map(e => e.srcId).min)
//    println(edges.map(e => e.srcId).max)
//    println(vRDD.map(v => v._1).min)
//    println(vRDD.map(v => v._1).max)

    val g = Graph(vRDD, edges).mapTriplets(t => 0.0)


    val trainedGraph = g.mapTriplets(trplt => compareTimeSeries(trplt.dstAttr._2, trplt.srcAttr._2, start = FEB_SRART, stop = FEB_END, isFiltered = true, lambda = 0.5))

    val prunedGraph = removeLowWeightEdges(trainedGraph, minWeight = 0.0)

    log.info("Edges in trained graph: " + prunedGraph.edges.count())

    val cleanGraph = removeSingletons(prunedGraph)
    val CC = getLargestConnectedComponent(cleanGraph)

    log.info("Vertices in LCC graph: " + CC.vertices.count())
    log.info("Edges in LCC graph: " + CC.edges.count())

    saveGraph(CC.mapVertices((vID, attr) => attr._1), weighted = false, fileName = PATH_RESOURCES + "graph.gexf")

    spark.stop()

//    saveGraph(g.mapVertices((vID, attr) => attr._1), weighted = false, fileName = PATH_RESOURCES + "graph.gexf")
  }
}

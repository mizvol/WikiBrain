import java.io.PrintWriter
import java.util.Calendar

import ch.epfl.lts2.Utils._
import ch.epfl.lts2.Globals._
import org.apache.spark.graphx.lib.ShortestPaths
import org.apache.spark.graphx.{Edge, Graph, VertexId}
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.SparkSession
import org.slf4j.LoggerFactory
import org.slf4j.Logger


/**
  * Created by volodymyrmiz on 15.05.17.
  */
object WikiBrainHebbStatic {
  def main(args: Array[String]): Unit = {
    suppressLogs(List("org", "akka"))

    val log: Logger = LoggerFactory.getLogger(this.getClass)

    val spark = SparkSession.builder
      .master("local[*]")
      .appName("Wiki Brain")
      .config("spark.driver.maxResultSize", "10g")
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


    /**
      * Average path length. Initial graph
      */
    //    val vID = graph.vertices.take(10)(1)._1
    //    println(vID)
    //    val shortestPathGraph = ShortestPaths.run(graph, Seq(vID))

    //    val shortestPath = shortestPathGraph.vertices.map(_._2.values).filter(_.nonEmpty).map(_.toList.head.toString.toDouble).max()
    //    println(shortestPath)
    log.info("Start time: " + Calendar.getInstance().getTime())
    val trainedGraph = graph.mapTriplets(trplt => compareTimeSeries(trplt.dstAttr._2, trplt.srcAttr._2, start = JAN_START, stop = JAN_END, isFiltered = true))
//    val trainedGraph = graph.mapTriplets(trplt => pearsonCorrelation(trplt.dstAttr._2, trplt.srcAttr._2, start = FEB_SRART, stop = FEB_END))

//    Check the number of 0-edges and write non-zero-edges to a file
//        println(trainedGraph.edges.map(_.attr).filter(_ > 0).count())

//        import java.io._
//        val writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(PATH_RESOURCES + "weights/weights")))
//        for (x <- trainedGraph.edges.map(_.attr).filter(_ > 0).collect()) {
//          writer.write(x + "\n")  // however you want to format it
//        }
//        writer.close()


    val prunedGraph = removeLowWeightEdges(trainedGraph, minWeight = 1.0)

    log.info("Edges in trained graph: " + prunedGraph.edges.count())

    val cleanGraph = removeSingletons(prunedGraph)
    val CC = getLargestConnectedComponent(cleanGraph)

    log.info("Vertices in LCC graph: " + CC.vertices.count())
    log.info("Edges in LCC graph: " + CC.edges.count())

    /**
      * Average path length. Learned graph
      */
    //    val vID = CC.vertices.take(1)(0)._1
    //    val shortestPathGraph = ShortestPaths.run(CC, Seq(vID))
    //
    //    val shortestPath = shortestPathGraph.vertices.map(_._2.values).filter(_.nonEmpty).map(_.toList.head.toString.toDouble).mean()
    //    println(shortestPath)

    saveGraph(CC.mapVertices((vID, attr) => attr._1), PATH_RESOURCES + "graph.gexf")
    saveSignal(CC, PATH_RESOURCES + "signal.txt")

    log.info("End time: " + Calendar.getInstance().getTime())
    spark.stop()
  }
}

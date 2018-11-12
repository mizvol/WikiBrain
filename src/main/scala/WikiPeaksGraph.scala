import java.io.PrintWriter
import java.util.Calendar

import ch.epfl.lts2.Globals
import ch.epfl.lts2.Globals.PATH_RESOURCES
import ch.epfl.lts2.Utils.suppressLogs
import org.apache.spark.graphx.{Edge, Graph, PartitionStrategy, VertexId}
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.SparkSession
import org.slf4j.{Logger, LoggerFactory}
import ch.epfl.lts2.Utils._
import ch.epfl.lts2.Globals._
import org.apache.spark.mllib.linalg.Vectors

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

    log.info("Start time: " + Calendar.getInstance().getTime())

    val graph = Graph(verticesRDD, edgesRDD)
    log.info("Vertices in graph: " + graph.vertices.count())
    log.info("Edges in graph: " + graph.edges.count())

    val start_time = APR_START
    val end_time = APR_END
    val BURST_RATE = 5
    val BURST_COUNT = 3

    val peaksVertices = graph.vertices.map(v => (v._1, (v._2._1, mapToList(v._2._2, TOTAL_HOURS), v._2._2)))
      .map(v => (v._1, (v._2._1, v._2._2, v._2._3, BURST_RATE * stddev(v._2._2, v._2._3.values.sum / TOTAL_HOURS) + v._2._3.values.sum / TOTAL_HOURS))) //compute threshold and save it as a 4th value in the set
      .filter(v => v._2._3.filterKeys(hour => hour > start_time & hour < end_time).values.count(l => l > v._2._4) > BURST_COUNT) // filter vertices that do not have enough bursts
      .map(v=> (v._1, (v._2._1, v._2._3.filterKeys(v._2._3(_) > v._2._4).map(identity)))) //filter time-series by keeping only bursts

    val vIDs = peaksVertices.map(_._1).collect().toSet

    val peaksEgdes = graph.edges.filter(e => vIDs.contains(e.dstId) & vIDs.contains(e.srcId))

    //Write edges to file
//    val pg = graph.mapTriplets(trplt => {if (vIDs.contains(trplt.dstId) & vIDs.contains(trplt.srcId)) 1.0 else 0.0})
//
//    import spark.implicits._
//    pg.edges.repartition(1).toDF.write.csv(PATH_RESOURCES + "edges_full.csv")

    val peaksGraph = Graph(peaksVertices, peaksEgdes)
//    val peaksGraph = graph

    log.info("Vertices in peaks graph: " + peaksGraph.vertices.count())
    log.info("Edges in peaks graph: " + peaksGraph.edges.count())

    // STDDEV + HEBB
    val trainedGraph = peaksGraph.mapTriplets(trplt => compareTimeSeries(trplt.dstAttr._2, trplt.srcAttr._2, start = start_time, stop = end_time, isFiltered = true, lambda = 0.5))

    // STDDEV only
//    val trainedGraph = peaksGraph.mapTriplets(t => 1.0)
//    val trainedGraph = peaksGraph.mapTriplets(t => pearsonCorrelation(t.dstAttr._2, t.srcAttr._2, start = start_time, stop = end_time))

    // STDDEV + HEBB
    val prunedGraph = removeLowWeightEdges(trainedGraph, minWeight = 1.0)

    //STDDEV only
//    val prunedGraph = trainedGraph

    log.info("Vertices in trained graph: " + prunedGraph.vertices.count())
    log.info("Edges in trained graph: " + prunedGraph.edges.count())

    //Non-learning case
//    val prunedGraph = peaksGraph

    val cleanGraph = removeSingletons(prunedGraph)
    val CC = getLargestConnectedComponent(cleanGraph)

    log.info("Vertices in LCC graph: " + CC.vertices.count())
    log.info("Edges in LCC graph: " + CC.edges.count())


    log.info("End time: " + Calendar.getInstance().getTime())

    //Write edges to file
//    val ccIDs = CC.vertices.map(_._1).collect().toSet
//        val adj = graph.mapTriplets(trplt => {if (ccIDs.contains(trplt.dstId) & ccIDs.contains(trplt.srcId)) 1.0 else 0.0})
//
//        import spark.implicits._
//        trainedGraph.edges.repartition(1).toDF.write.csv(PATH_RESOURCES + "edges_full.csv")

    saveGraph(CC.mapVertices((id, v) => v._1), weighted = false, fileName = PATH_RESOURCES + "peaks_graph.gexf")

    val v_ids = CC.vertices.map(_._1).collect().toSet

    val siggraph = Graph(graph.vertices.filter(v => v_ids.contains(v._1)).map(v => (v._1, (v._2._1, v._2._2))), graph.edges.filter(e => v_ids.contains(e.srcId) && v_ids.contains(e.dstId)))

//    siggraph.vertices.take(10).foreach(println)

    saveSignal(siggraph, PATH_RESOURCES + "apr_page_views.txt")

//    val pw = new PrintWriter(PATH_RESOURCES + "feb_page_views.csv")
//    siggraph.vertices.map(v => (v._1, Vectors.sparse(Globals.TOTAL_HOURS, v._2.keys.toArray, v._2.values.toArray).toDense))
//      .collect.map(_.toString())
//      .map(pw.write(_))
//    pw.close
  }
}

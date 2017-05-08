package ch.epfl.lts2.GraphXExtension

import org.apache.spark.graphx.{Edge, EdgeTriplet, Graph, GraphOps}
import scala.reflect.ClassTag

/**
  * Created by volodymyrmiz on 08.05.17.
  */

class GraphExtension[VD: ClassTag, ED: ClassTag](val graph: Graph[VD, ED]) {
  /**
    * Merge edges that satisfy an edge predicate and collapse vertices connected by merged edges.
    * @author Dave Ankur
    */
  def coarsen(pred: EdgeTriplet[VD, ED] => Boolean, reduce: (VD, VD) => VD): Graph[VD, ED] = {
    // Restrict graph to contractable edges
    val subG = graph.subgraph(epred = pred)
    // Compute connected component id for each vertex
    val cc = subG.connectedComponents.vertices.cache()
    // Merge all vertices in the same component
    val toMerge = graph.vertices.innerJoin(cc) { (id, attr, cc) => (cc, attr) }.map(_._2)
    val superV = graph.vertices.aggregateUsingIndex(toMerge, reduce)
    // Link remaining edges between components
    val invG = graph.subgraph(epred = e => !pred(e))
    val remainingE = invG.outerJoinVertices(cc) { (id, _, ccOpt) => ccOpt.get }
      .triplets.map { e => Edge(e.srcAttr, e.dstAttr, e.attr) }
    Graph(superV, remainingE)
  }
}

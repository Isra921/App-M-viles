package com.escom.escomorienta.data.map

import java.util.PriorityQueue
import kotlin.math.hypot

object AStarRouteFinder {
    fun findShortestPath(
        graph: MapNavigationGraph,
        startNodeId: String,
        destinationNodeId: String,
        accessibleOnly: Boolean = false
    ): List<String> {
        if (startNodeId == destinationNodeId) return listOf(startNodeId)

        val nodesById = graph.nodes.associateBy { it.id }
        val destination = nodesById[destinationNodeId] ?: return emptyList()
        if (startNodeId !in nodesById) return emptyList()

        val adjacency = buildMap<String, MutableList<Pair<String, Float>>> {
            graph.edges.forEach { edge ->
                if (edge.weight <= 0f || edge.from !in nodesById || edge.to !in nodesById) return@forEach
                val fromNode = nodesById[edge.from]
                val toNode = nodesById[edge.to]
                val isStairsEdge = fromNode?.type == MapNavigationNodeType.STAIRS || toNode?.type == MapNavigationNodeType.STAIRS
                if (accessibleOnly && isStairsEdge) {
                    // En modo accesible se evitan escaleras
                    return@forEach
                }
                getOrPut(edge.from) { mutableListOf() }.add(edge.to to edge.weight)
                getOrPut(edge.to) { mutableListOf() }.add(edge.from to edge.weight)
            }
        }
        val open = PriorityQueue(compareBy<RouteCandidate> { it.estimatedTotal }.thenBy { it.cost })
        val costFromStart = mutableMapOf(startNodeId to 0f)
        val previous = mutableMapOf<String, String>()
        val closed = mutableSetOf<String>()

        open.add(RouteCandidate(startNodeId, 0f, heuristic(nodesById.getValue(startNodeId), destination)))

        while (open.isNotEmpty()) {
            val current = open.poll() ?: break
            if (!closed.add(current.nodeId)) continue
            if (current.nodeId == destinationNodeId) return reconstructPath(previous, destinationNodeId)

            adjacency[current.nodeId].orEmpty().forEach { (neighborId, weight) ->
                if (neighborId in closed) return@forEach
                val nextCost = costFromStart.getValue(current.nodeId) + weight
                if (nextCost < costFromStart.getOrDefault(neighborId, Float.POSITIVE_INFINITY)) {
                    costFromStart[neighborId] = nextCost
                    previous[neighborId] = current.nodeId
                    open.add(
                        RouteCandidate(
                            nodeId = neighborId,
                            cost = nextCost,
                            estimatedTotal = nextCost + heuristic(nodesById.getValue(neighborId), destination)
                        )
                    )
                }
            }
        }

        return emptyList()
    }

    private fun heuristic(node: MapNavigationNode, destination: MapNavigationNode): Float =
        hypot(node.x - destination.x, node.y - destination.y)

    private fun reconstructPath(previous: Map<String, String>, destination: String): List<String> =
        buildList {
            var current: String? = destination
            while (current != null) {
                add(current)
                current = previous[current]
            }
        }.asReversed()

    private data class RouteCandidate(
        val nodeId: String,
        val cost: Float,
        val estimatedTotal: Float
    )
}

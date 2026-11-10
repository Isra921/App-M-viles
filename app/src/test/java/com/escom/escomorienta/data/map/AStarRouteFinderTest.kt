package com.escom.escomorienta.data.map

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AStarRouteFinderTest {
    @Test
    fun choosesTheLowestWeightRoute() {
        val graph = graph(
            nodes = listOf(node("A", 0f, 0f), node("B", 1f, 0f), node("C", 0f, 2f)),
            edges = listOf(
                MapNavigationEdge("A", "B", 1f),
                MapNavigationEdge("B", "C", 1f),
                MapNavigationEdge("A", "C", 5f)
            )
        )

        assertEquals(listOf("A", "B", "C"), AStarRouteFinder.findShortestPath(graph, "A", "C"))
    }

    @Test
    fun returnsEmptyPathForDisconnectedNodes() {
        val graph = graph(
            nodes = listOf(node("A", 0f, 0f), node("B", 1f, 0f)),
            edges = emptyList()
        )

        assertTrue(AStarRouteFinder.findShortestPath(graph, "A", "B").isEmpty())
    }

    private fun node(id: String, x: Float, y: Float) =
        MapNavigationNode(id, x, y, MapNavigationNodeType.PATH)

    private fun graph(
        nodes: List<MapNavigationNode>,
        edges: List<MapNavigationEdge>
    ) = MapNavigationGraph(
        floorId = "PB",
        coordinateWidth = 1260f,
        coordinateHeight = 1600f,
        mapWidth = 1260f,
        mapHeight = 1600f,
        verticalOffset = 0f,
        nodes = nodes,
        edges = edges
    )
}

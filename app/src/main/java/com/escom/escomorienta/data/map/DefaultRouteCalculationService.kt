package com.escom.escomorienta.data.map

import com.escom.escomorienta.domain.navigation.NormalizedMapPoint
import com.escom.escomorienta.domain.navigation.RouteCalculationService
import com.escom.escomorienta.domain.navigation.RouteEndpoint
import com.escom.escomorienta.domain.navigation.RouteResult
import com.escom.escomorienta.domain.navigation.RouteSegment
import com.escom.escomorienta.domain.navigation.RouteTransition
import kotlin.math.sqrt

/**
 * Production implementation of RouteCalculationService extracted from MapScreen.
 * Handles single-floor and multi-floor pathfinding using A* and pre-mapped stair connectors.
 * Independent of Android UI / Compose.
 */
class DefaultRouteCalculationService(
    private val mapRepository: MapRepository? = null
) : RouteCalculationService {

    companion object {
        val MapFloorAssets = listOf(
            "places_pb.json",
            "places_p1.json",
            "places_p2.json"
        )

        val NavigationAssetByFloor = mapOf(
            "places_pb.json" to "navigation_pb.json",
            "places_p1.json" to "navigation_p1.json",
            "places_p2.json" to "navigation_p2.json"
        )

        val AccessAssetByFloor = mapOf(
            "places_pb.json" to "accesos_pb.json",
            "places_p1.json" to "accesos_p1.json",
            "places_p2.json" to "accesos_p2.json"
        )

        val StairNodeIdsByConnector = mapOf(
            "PB_S01" to mapOf(
                "places_pb.json" to "S01",
                "places_p1.json" to "S01",
                "places_p2.json" to "S01"
            ),
            "PB_S02" to mapOf(
                "places_pb.json" to "S02",
                "places_p1.json" to "S02",
                "places_p2.json" to "S02"
            ),
            "PB_S03" to mapOf(
                "places_pb.json" to "S03",
                "places_p1.json" to "S04",
                "places_p2.json" to "S04"
            ),
            "PB_S04" to mapOf(
                "places_pb.json" to "S04",
                "places_p1.json" to "S05",
                "places_p2.json" to "S05"
            ),
            "PB_S05" to mapOf(
                "places_pb.json" to "S05",
                "places_p1.json" to "S06",
                "places_p2.json" to "S06"
            ),
            "PB_S06" to mapOf(
                "places_pb.json" to "S06",
                "places_p1.json" to "S07",
                "places_p2.json" to "S07"
            ),
            "PB_S07" to mapOf(
                "places_pb.json" to "S07",
                "places_p1.json" to "S08"
            ),
            "PB_S08" to mapOf(
                "places_pb.json" to "S08",
                "places_p1.json" to "S03",
                "places_p2.json" to "S03"
            )
        )
    }

    override suspend fun calculateRoute(
        origin: RouteEndpoint,
        destination: RouteEndpoint
    ): RouteResult {
        if (mapRepository == null) {
            return RouteResult.failure("MapRepository no disponible para resolver endpoints")
        }

        val destinationPlace = when (destination) {
            is RouteEndpoint.Place -> destination.mapPlace
            is RouteEndpoint.NamedPlaceId -> mapRepository.getPlaceById(destination.placeId)
            else -> null
        } ?: return RouteResult.failure("Destino inválido o no encontrado")

        val (originPlace, originPoint, originFloorAsset) = when (origin) {
            is RouteEndpoint.Place -> Triple(origin.mapPlace, origin.mapPlace.centerPoint(), MapAssetCatalog.assetForFloor(origin.mapPlace.floorId))
            is RouteEndpoint.NamedPlaceId -> {
                val p = mapRepository.getPlaceById(origin.placeId)
                Triple(p, p?.centerPoint(), p?.let { MapAssetCatalog.assetForFloor(it.floorId) })
            }
            is RouteEndpoint.ManualPoint -> Triple(null, origin.point, origin.floorAsset)
            else -> Triple(null, null, null)
        }

        if (originPoint == null || originFloorAsset == null) {
            return RouteResult.failure("Origen inválido o no encontrado")
        }

        val destinationFloorAsset = MapAssetCatalog.assetForFloor(destinationPlace.floorId)
            ?: return RouteResult.failure("Piso de destino no encontrado")

        // Load required navigation graphs and access data
        val graphsByFloor = mutableMapOf<String, MapNavigationGraph>()
        val accessDataByFloor = mutableMapOf<String, MapFloorAccessData>()

        for (floorAsset in MapFloorAssets) {
            val navAsset = NavigationAssetByFloor[floorAsset] ?: continue
            val accessAsset = AccessAssetByFloor[floorAsset] ?: continue
            mapRepository.loadNavigationGraphFromAssetsOrNull(navAsset)?.let { graphsByFloor[floorAsset] = it }
            mapRepository.loadFloorAccessDataFromAssetsOrNull(accessAsset)?.let { accessDataByFloor[floorAsset] = it }
        }

        val result = calculateRouteWithGraphs(
            graphsByFloor = graphsByFloor,
            originFloorAsset = originFloorAsset,
            originPoint = originPoint,
            originPlace = originPlace,
            destinationFloorAsset = destinationFloorAsset,
            destinationPlace = destinationPlace,
            accessDataByFloor = accessDataByFloor
        )

        return result.copy(origin = origin, destination = destination)
    }

    override suspend fun calculateRouteByIds(
        originPlaceId: String,
        destinationPlaceId: String
    ): RouteResult = calculateRoute(
        origin = RouteEndpoint.NamedPlaceId(originPlaceId),
        destination = RouteEndpoint.NamedPlaceId(destinationPlaceId)
    )

    override fun calculateRouteWithGraphs(
        graphsByFloor: Map<String, MapNavigationGraph>,
        originFloorAsset: String?,
        originPoint: NormalizedMapPoint?,
        originPlace: MapPlace?,
        destinationFloorAsset: String?,
        destinationPlace: MapPlace?,
        accessDataByFloor: Map<String, MapFloorAccessData>
    ): RouteResult {
        if (originFloorAsset == null || originPoint == null) {
            return RouteResult.failure("Origen no especificado")
        }
        if (destinationFloorAsset == null || destinationPlace == null) {
            return RouteResult.failure("Destino no especificado")
        }

        val originGraph = graphsByFloor[originFloorAsset]
            ?: return RouteResult.failure("Grafo no disponible para el piso de origen: $originFloorAsset")
        val destinationGraph = graphsByFloor[destinationFloorAsset]
            ?: return RouteResult.failure("Grafo no disponible para el piso de destino: $destinationFloorAsset")

        // CASE 1: Same floor
        if (originFloorAsset == destinationFloorAsset) {
            val floorRoute = buildSameFloorRoute(
                graph = originGraph,
                origin = originPoint,
                originPlace = originPlace,
                destination = destinationPlace,
                accessData = accessDataByFloor[originFloorAsset]
            ) ?: return RouteResult.failure("No se encontró ruta en el mismo piso")

            val floorId = originGraph.floorId
            val segment = RouteSegment(
                floorAsset = originFloorAsset,
                floorId = floorId,
                points = floorRoute.points,
                cost = floorRoute.cost
            )

            return RouteResult(
                success = true,
                segments = listOf(segment),
                transitions = emptyList(),
                totalCost = floorRoute.cost,
                pointsByFloor = mapOf(originFloorAsset to floorRoute.points),
                isMultiFloor = false
            )
        }

        // CASE 2: Multi floor
        val originFloorIndex = MapFloorAssets.indexOf(originFloorAsset)
        val destinationFloorIndex = MapFloorAssets.indexOf(destinationFloorAsset)
        if (originFloorIndex < 0 || destinationFloorIndex < 0) {
            return RouteResult.failure("Índices de piso inválidos")
        }

        val traversedFloorAssets = if (originFloorIndex < destinationFloorIndex) {
            MapFloorAssets.subList(originFloorIndex, destinationFloorIndex + 1)
        } else {
            MapFloorAssets.subList(destinationFloorIndex, originFloorIndex + 1).asReversed()
        }

        val traversedGraphs = mutableMapOf<String, MapNavigationGraph>()
        for (floorAsset in traversedFloorAssets) {
            val graph = graphsByFloor[floorAsset]
                ?: return RouteResult.failure("Grafo intermedio no disponible para $floorAsset")
            traversedGraphs[floorAsset] = graph
        }

        val originCandidates = originPlace
            ?.let { buildRouteEndpointCandidates(it, originGraph, accessDataByFloor[originFloorAsset]) }
            ?: listOfNotNull(manualRouteEndpointCandidate(originPoint, originGraph))

        val destinationCandidates = buildRouteEndpointCandidates(
            destinationPlace,
            destinationGraph,
            accessDataByFloor[destinationFloorAsset]
        )

        val originNodes = originGraph.nodes.associate { it.id to it.toNormalizedPoint(originGraph) }
        val destinationNodes = destinationGraph.nodes.associate { it.id to it.toNormalizedPoint(destinationGraph) }

        val bestConnection = StairNodeIdsByConnector.entries
            .asSequence()
            .flatMap connector@ { (connectorKey, nodeIdsByFloor) ->
                val stairsByFloor = mutableMapOf<String, MapNavigationNode>()
                for ((floorAsset, graph) in traversedGraphs) {
                    val stairNodeId = nodeIdsByFloor[floorAsset]
                        ?: return@connector emptySequence<MultiFloorConnectionCandidate>()
                    val stairs = graph.nodes.firstOrNull { node ->
                        node.id == stairNodeId && node.type == MapNavigationNodeType.STAIRS
                    } ?: return@connector emptySequence<MultiFloorConnectionCandidate>()
                    stairsByFloor[floorAsset] = stairs
                }
                val originStairs = stairsByFloor.getValue(originFloorAsset)
                val destinationStairs = stairsByFloor.getValue(destinationFloorAsset)

                originCandidates.asSequence().flatMap { originCandidate ->
                    destinationCandidates.asSequence().mapNotNull { destinationCandidate ->
                        val originGraphPath = findGraphPath(
                            originGraph,
                            originCandidate.graphNodeId,
                            originStairs.id
                        ) ?: return@mapNotNull null
                        val destinationGraphPath = findGraphPath(
                            destinationGraph,
                            destinationStairs.id,
                            destinationCandidate.graphNodeId
                        ) ?: return@mapNotNull null

                        MultiFloorConnectionCandidate(
                            connectorId = connectorKey,
                            originSegment = distinctConsecutive(
                                originCandidate.pointsToGraph(originGraph) +
                                    originGraphPath.nodeIds.map(originNodes::getValue)
                            ),
                            destinationSegment = distinctConsecutive(
                                destinationGraphPath.nodeIds.map(destinationNodes::getValue) +
                                    destinationCandidate.pointsFromGraph(destinationGraph)
                            ),
                            stairsByFloor = stairsByFloor,
                            totalWeight = originCandidate.connectorCost + originGraphPath.cost +
                                destinationGraphPath.cost + destinationCandidate.connectorCost,
                            originCost = originCandidate.connectorCost + originGraphPath.cost,
                            destinationCost = destinationCandidate.connectorCost + destinationGraphPath.cost
                        )
                    }
                }
            }
            .minByOrNull { it.totalWeight }
            ?: return RouteResult.failure("No se encontró conexión entre pisos")

        val pointsByFloor = buildMap<String, List<NormalizedMapPoint>> {
            put(originFloorAsset, bestConnection.originSegment)
            traversedFloorAssets
                .filterNot { it == originFloorAsset || it == destinationFloorAsset }
                .forEach { floorAsset ->
                    val graph = traversedGraphs.getValue(floorAsset)
                    val stairs = bestConnection.stairsByFloor.getValue(floorAsset)
                    put(floorAsset, listOf(stairs.toNormalizedPoint(graph)))
                }
            put(destinationFloorAsset, bestConnection.destinationSegment)
        }

        val segments = buildList {
            add(
                RouteSegment(
                    floorAsset = originFloorAsset,
                    floorId = originGraph.floorId,
                    points = bestConnection.originSegment,
                    cost = bestConnection.originCost
                )
            )
            traversedFloorAssets
                .filterNot { it == originFloorAsset || it == destinationFloorAsset }
                .forEach { floorAsset ->
                    val graph = traversedGraphs.getValue(floorAsset)
                    val stairs = bestConnection.stairsByFloor.getValue(floorAsset)
                    add(
                        RouteSegment(
                            floorAsset = floorAsset,
                            floorId = graph.floorId,
                            points = listOf(stairs.toNormalizedPoint(graph)),
                            cost = 0f
                        )
                    )
                }
            add(
                RouteSegment(
                    floorAsset = destinationFloorAsset,
                    floorId = destinationGraph.floorId,
                    points = bestConnection.destinationSegment,
                    cost = bestConnection.destinationCost
                )
            )
        }

        val transitions = traversedFloorAssets.zipWithNext().map { (fromFloor, toFloor) ->
            val stairNode = bestConnection.stairsByFloor.getValue(fromFloor)
            val graph = traversedGraphs.getValue(fromFloor)
            RouteTransition(
                connectorId = bestConnection.connectorId,
                fromFloorAsset = fromFloor,
                toFloorAsset = toFloor,
                stairNodeId = stairNode.id,
                point = stairNode.toNormalizedPoint(graph)
            )
        }

        return RouteResult(
            success = true,
            segments = segments,
            transitions = transitions,
            totalCost = bestConnection.totalWeight,
            pointsByFloor = pointsByFloor,
            isMultiFloor = true
        )
    }

    private fun buildSameFloorRoute(
        graph: MapNavigationGraph,
        origin: NormalizedMapPoint,
        originPlace: MapPlace?,
        destination: MapPlace,
        accessData: MapFloorAccessData?
    ): FloorRouteResult? {
        val originCandidates = originPlace
            ?.let { buildRouteEndpointCandidates(it, graph, accessData) }
            ?: listOfNotNull(manualRouteEndpointCandidate(origin, graph))
        val destinationCandidates = buildRouteEndpointCandidates(destination, graph, accessData)
        val nodes = graph.nodes.associate { it.id to it.toNormalizedPoint(graph) }

        return originCandidates.flatMap { originCandidate ->
            destinationCandidates.mapNotNull { destinationCandidate ->
                val graphPath = findGraphPath(
                    graph,
                    originCandidate.graphNodeId,
                    destinationCandidate.graphNodeId
                ) ?: return@mapNotNull null
                FloorRouteResult(
                    points = distinctConsecutive(
                        originCandidate.pointsToGraph(graph) +
                            graphPath.nodeIds.map(nodes::getValue) +
                            destinationCandidate.pointsFromGraph(graph)
                    ),
                    cost = originCandidate.connectorCost + graphPath.cost + destinationCandidate.connectorCost
                )
            }
        }.minByOrNull { it.cost }
    }

    private fun findGraphPath(
        graph: MapNavigationGraph,
        startNodeId: String,
        destinationNodeId: String
    ): GraphPathResult? {
        val nodeIds = AStarRouteFinder.findShortestPath(graph, startNodeId, destinationNodeId)
        if (nodeIds.isEmpty()) return null
        val weights = graph.edges.groupBy { setOf(it.from, it.to) }
        var cost = 0f
        for ((from, to) in nodeIds.zipWithNext()) {
            val weight = weights[setOf(from, to)]?.minOfOrNull { it.weight } ?: return null
            cost += weight
        }
        return GraphPathResult(nodeIds, cost)
    }

    fun buildRouteEndpointCandidates(
        place: MapPlace,
        graph: MapNavigationGraph,
        accessData: MapFloorAccessData?
    ): List<RouteEndpointCandidate> {
        val locationPoint = place.centerPoint() ?: return emptyList()
        val nodesById = graph.nodes.associateBy { it.id }
        val explicitAccessData = accessData?.takeIf { place.id in it.accessPointsByPlaceId }

        if (explicitAccessData != null) {
            val explicitCandidates = explicitAccessData.accessPointsByPlaceId.getValue(place.id).flatMap { accessPoint ->
                val normalizedAccessPoint = accessPoint.toNormalizedPoint(explicitAccessData)
                accessPoint.graphNodeIds.mapNotNull { graphNodeId ->
                    val node = nodesById[graphNodeId] ?: return@mapNotNull null
                    val nodePoint = node.toNormalizedPoint(graph)
                    RouteEndpointCandidate(
                        graphNodeId = graphNodeId,
                        locationPoint = locationPoint,
                        accessPoint = normalizedAccessPoint,
                        connectorCost = normalizedAccessPoint.pixelDistanceTo(nodePoint, graph),
                        usesExplicitAccess = true
                    )
                }
            }
            if (explicitCandidates.isNotEmpty()) return explicitCandidates
        }

        val explicitNode = place.navigationNodeId?.let(nodesById::get)
        val fallbackNode = explicitNode ?: graph.nodes
            .asSequence()
            .filter { it.type == MapNavigationNodeType.PATH }
            .minByOrNull { candidate ->
                val graphPoint = candidate.toNormalizedPoint(graph)
                locationPoint.pixelDistanceTo(graphPoint, graph)
            } ?: return emptyList()
        val nodePoint = fallbackNode.toNormalizedPoint(graph)
        return listOf(
            RouteEndpointCandidate(
                graphNodeId = fallbackNode.id,
                locationPoint = locationPoint,
                accessPoint = locationPoint,
                connectorCost = locationPoint.pixelDistanceTo(nodePoint, graph),
                usesExplicitAccess = false
            )
        )
    }

    private fun manualRouteEndpointCandidate(
        point: NormalizedMapPoint,
        graph: MapNavigationGraph
    ): RouteEndpointCandidate? {
        val node = graph.nodes
            .asSequence()
            .filter { it.type == MapNavigationNodeType.PATH }
            .minByOrNull { point.pixelDistanceTo(it.toNormalizedPoint(graph), graph) }
            ?: return null
        val nodePoint = node.toNormalizedPoint(graph)
        return RouteEndpointCandidate(
            graphNodeId = node.id,
            locationPoint = point,
            accessPoint = point,
            connectorCost = point.pixelDistanceTo(nodePoint, graph),
            usesExplicitAccess = false
        )
    }

    private fun RouteEndpointCandidate.pointsToGraph(graph: MapNavigationGraph): List<NormalizedMapPoint> =
        distinctConsecutive(
            if (usesExplicitAccess) {
                listOf(accessPoint, graph.nodes.first { it.id == graphNodeId }.toNormalizedPoint(graph))
            } else {
                listOf(locationPoint, graph.nodes.first { it.id == graphNodeId }.toNormalizedPoint(graph))
            }
        )

    private fun RouteEndpointCandidate.pointsFromGraph(graph: MapNavigationGraph): List<NormalizedMapPoint> =
        pointsToGraph(graph).asReversed()

    private fun distinctConsecutive(points: List<NormalizedMapPoint>): List<NormalizedMapPoint> =
        points.fold(mutableListOf()) { result, point ->
            if (result.lastOrNull() != point) result.add(point)
            result
        }

    private fun NormalizedMapPoint.pixelDistanceTo(
        other: NormalizedMapPoint,
        graph: MapNavigationGraph
    ): Float {
        val dx = (x - other.x) * graph.mapWidth
        val dy = (y - other.y) * graph.mapHeight
        return sqrt(dx * dx + dy * dy)
    }

    private fun MapNavigationNode.toNormalizedPoint(graph: MapNavigationGraph) = NormalizedMapPoint(
        x = (x / graph.coordinateWidth).coerceIn(0f, 1f),
        y = ((y + graph.verticalOffset) / graph.mapHeight).coerceIn(0f, 1f)
    )

    private fun MapAccessPoint.toNormalizedPoint(accessData: MapFloorAccessData) = NormalizedMapPoint(
        x = (x / accessData.coordinateWidth).coerceIn(0f, 1f),
        y = ((y + accessData.verticalOffset) / accessData.mapHeight).coerceIn(0f, 1f)
    )

    private data class GraphPathResult(
        val nodeIds: List<String>,
        val cost: Float
    )

    private data class FloorRouteResult(
        val points: List<NormalizedMapPoint>,
        val cost: Float
    )

    data class RouteEndpointCandidate(
        val graphNodeId: String,
        val locationPoint: NormalizedMapPoint,
        val accessPoint: NormalizedMapPoint,
        val connectorCost: Float,
        val usesExplicitAccess: Boolean = false
    )

    private data class MultiFloorConnectionCandidate(
        val connectorId: String,
        val originSegment: List<NormalizedMapPoint>,
        val destinationSegment: List<NormalizedMapPoint>,
        val stairsByFloor: Map<String, MapNavigationNode>,
        val totalWeight: Float,
        val originCost: Float,
        val destinationCost: Float
    )
}

// Extensions for MapPlace center point computation decoupled from UI
fun MapPlace.enclosingBounds(): MapBounds? {
    if (bounds.isEmpty()) return null
    return MapBounds(
        left = bounds.minOf { it.left },
        top = bounds.minOf { it.top },
        right = bounds.maxOf { it.right },
        bottom = bounds.maxOf { it.bottom }
    )
}

fun MapPlace.centerPoint(): NormalizedMapPoint? {
    val b = enclosingBounds() ?: return null
    return NormalizedMapPoint(
        x = (b.left + b.right) / 2f,
        y = (b.top + b.bottom) / 2f
    )
}

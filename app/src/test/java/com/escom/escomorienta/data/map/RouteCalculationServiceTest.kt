package com.escom.escomorienta.data.map

import com.escom.escomorienta.domain.navigation.NormalizedMapPoint
import com.escom.escomorienta.domain.navigation.RouteCalculationService
import com.escom.escomorienta.domain.navigation.RouteEndpoint
import com.escom.escomorienta.domain.navigation.RouteResult
import com.escom.escomorienta.ui.screens.buildMultiFloorRoutePoints
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class RouteCalculationServiceTest {

    private lateinit var graphs: Map<String, MapNavigationGraph>
    private lateinit var accessData: Map<String, MapFloorAccessData>
    private lateinit var places: Map<String, MapPlace>
    private lateinit var service: RouteCalculationService

    @Before
    fun setUp() {
        val assetDir = findAssetMapsDir()
        graphs = mapOf(
            "places_pb.json" to parseNavigationGraph(File(assetDir, "navigation_pb.json").readText()),
            "places_p1.json" to parseNavigationGraph(File(assetDir, "navigation_p1.json").readText()),
            "places_p2.json" to parseNavigationGraph(File(assetDir, "navigation_p2.json").readText())
        )
        accessData = mapOf(
            "places_pb.json" to parseFloorAccessData(File(assetDir, "accesos_pb.json").readText()),
            "places_p1.json" to parseFloorAccessData(File(assetDir, "accesos_p1.json").readText()),
            "places_p2.json" to parseFloorAccessData(File(assetDir, "accesos_p2.json").readText())
        )
        places = listOf("places_pb.json", "places_p1.json", "places_p2.json")
            .flatMap { parsePlaces(File(assetDir, it).readText(), it) }
            .associateBy { it.id }

        service = DefaultRouteCalculationService()
    }

    @Test
    fun testSameFloorRoute1201To1202() {
        val origin = checkNotNull(places["aula_1201"] ?: places.values.firstOrNull { it.name.contains("1201") })
        val destination = checkNotNull(places["aula_1202"] ?: places.values.firstOrNull { it.name.contains("1202") })

        val result = service.calculateRouteWithGraphs(
            graphsByFloor = graphs,
            originFloorAsset = "places_p1.json",
            originPoint = origin.centerPoint(),
            originPlace = origin,
            destinationFloorAsset = "places_p1.json",
            destinationPlace = destination,
            accessDataByFloor = accessData
        )

        assertTrue("Ruta en misma planta debe ser exitosa", result.success)
        assertFalse("Ruta en misma planta no debe ser multi-piso", result.isMultiFloor)
        assertEquals("Debe tener 1 solo segmento en P1", 1, result.segments.size)
        assertTrue("Debe contener puntos de ruta en P1", result.pointsByFloor["places_p1.json"].orEmpty().size >= 2)
        assertTrue("No debe haber transiciones de escaleras", result.transitions.isEmpty())
        assertTrue("El costo total debe ser mayor a 0", result.totalCost > 0f)
    }

    @Test
    fun testMultiFloorRouteGestionTo1201() {
        val origin = checkNotNull(places["gestion_escolar"] ?: places.values.firstOrNull { it.name.contains("Gestión") })
        val destination = checkNotNull(places["aula_1201"] ?: places.values.firstOrNull { it.name.contains("1201") })

        val result = service.calculateRouteWithGraphs(
            graphsByFloor = graphs,
            originFloorAsset = "places_pb.json",
            originPoint = origin.centerPoint(),
            originPlace = origin,
            destinationFloorAsset = "places_p1.json",
            destinationPlace = destination,
            accessDataByFloor = accessData
        )

        assertTrue("Ruta PB -> P1 debe ser exitosa", result.success)
        assertTrue("Ruta PB -> P1 debe ser marcada como multi-piso", result.isMultiFloor)
        assertTrue("Debe registrar al menos 2 segmentos", result.segments.size >= 2)
        assertTrue("Debe registrar al menos 1 transición de escalera", result.transitions.isNotEmpty())
        assertTrue("Debe tener puntos en PB", result.pointsByFloor["places_pb.json"].orEmpty().size >= 2)
        assertTrue("Debe tener puntos en P1", result.pointsByFloor["places_p1.json"].orEmpty().size >= 2)
    }

    @Test
    fun testRouteFromEntranceTo1201() {
        val origin = checkNotNull(places["entrada_principal"])
        val destination = checkNotNull(places["aula_1201"] ?: places.values.firstOrNull { it.name.contains("1201") })

        val result = service.calculateRouteWithGraphs(
            graphsByFloor = graphs,
            originFloorAsset = "places_pb.json",
            originPoint = origin.centerPoint(),
            originPlace = origin,
            destinationFloorAsset = "places_p1.json",
            destinationPlace = destination,
            accessDataByFloor = accessData
        )

        assertTrue("Ruta desde entrada a 1201 debe ser exitosa", result.success)
        assertTrue("Debe ser multi-piso", result.isMultiFloor)
        assertTrue("Costo total debe ser positivo", result.totalCost > 0f)
    }

    @Test
    fun testNonExistentRouteHandling() {
        val destination = checkNotNull(places.values.first())

        val result = service.calculateRouteWithGraphs(
            graphsByFloor = graphs,
            originFloorAsset = null,
            originPoint = null,
            originPlace = null,
            destinationFloorAsset = "places_p1.json",
            destinationPlace = destination,
            accessDataByFloor = accessData
        )

        assertFalse("Ruta con origen nulo debe fallar", result.success)
        assertNotNull("Debe proporcionar mensaje de error explicativo", result.errorMessage)
    }

@Test
    fun testCoincidentOriginAndDestination() {
        val place = checkNotNull(places["aula_1201"] ?: places.values.first())
        val result = service.calculateRouteWithGraphs(
            graphsByFloor = graphs,
            originFloorAsset = "places_p1.json",
            originPoint = place.centerPoint(),
            originPlace = place,
            destinationFloorAsset = "places_p1.json",
            destinationPlace = place,
            accessDataByFloor = accessData
        )

        assertTrue("Ruta coincidente debe ser exitosa", result.success)
        assertFalse("No debe ser multi-piso", result.isMultiFloor)
        assertTrue("Transiciones deben estar vacías", result.transitions.isEmpty())
        assertEquals("Debe tener 1 segmento", 1, result.segments.size)
    }

    @Test
    fun testMultiFloorTraversalsAndInverses() {
        val pbPlace = checkNotNull(places["entrada_principal"] ?: places.values.first { it.floorId == "PB" })
        val p1Place = checkNotNull(places["aula_1201"] ?: places.values.first { it.floorId == "1" })
        val p2Place = checkNotNull(places["aula_2201"] ?: places.values.first { it.floorId == "2" })

        // 1. PB -> P1
        val pbToP1 = service.calculateRouteWithGraphs(graphs, "places_pb.json", pbPlace.centerPoint(), pbPlace, "places_p1.json", p1Place, accessData)
        assertTrue("PB -> P1 debe tener éxito", pbToP1.success)
        assertTrue("PB -> P1 debe ser multi-piso", pbToP1.isMultiFloor)

        // 2. P1 -> PB (inverso)
        val p1ToPb = service.calculateRouteWithGraphs(graphs, "places_p1.json", p1Place.centerPoint(), p1Place, "places_pb.json", pbPlace, accessData)
        assertTrue("P1 -> PB debe tener éxito", p1ToPb.success)
        assertTrue("P1 -> PB debe ser multi-piso", p1ToPb.isMultiFloor)

        // 3. P1 -> P2
        val p1ToP2 = service.calculateRouteWithGraphs(graphs, "places_p1.json", p1Place.centerPoint(), p1Place, "places_p2.json", p2Place, accessData)
        assertTrue("P1 -> P2 debe tener éxito", p1ToP2.success)
        assertTrue("P1 -> P2 debe ser multi-piso", p1ToP2.isMultiFloor)

        // 4. P2 -> P1 (inverso)
        val p2ToP1 = service.calculateRouteWithGraphs(graphs, "places_p2.json", p2Place.centerPoint(), p2Place, "places_p1.json", p1Place, accessData)
        assertTrue("P2 -> P1 debe tener éxito", p2ToP1.success)
        assertTrue("P2 -> P1 debe ser multi-piso", p2ToP1.isMultiFloor)

        // 5. PB -> P2 (travesía completa con piso intermedio P1)
        val pbToP2 = service.calculateRouteWithGraphs(graphs, "places_pb.json", pbPlace.centerPoint(), pbPlace, "places_p2.json", p2Place, accessData)
        assertTrue("PB -> P2 debe tener éxito", pbToP2.success)
        assertTrue("PB -> P2 debe ser multi-piso", pbToP2.isMultiFloor)
        assertEquals("Debe cruzar 3 pisos", 3, pbToP2.segments.size)
        assertEquals("Debe tener 2 transiciones verticales", 2, pbToP2.transitions.size)
        assertNotNull("Debe registrar punto en piso intermedio P1", pbToP2.pointsByFloor["places_p1.json"])
        assertEquals("El piso intermedio debe tener exactamente 1 punto de tránsito", 1, pbToP2.pointsByFloor["places_p1.json"]?.size)

        // 6. P2 -> PB (inverso con piso intermedio P1)
        val p2ToPb = service.calculateRouteWithGraphs(graphs, "places_p2.json", p2Place.centerPoint(), p2Place, "places_pb.json", pbPlace, accessData)
        assertTrue("P2 -> PB debe tener éxito", p2ToPb.success)
        assertTrue("P2 -> PB debe ser multi-piso", p2ToPb.isMultiFloor)
        assertEquals("Debe cruzar 3 pisos", 3, p2ToPb.segments.size)
        assertEquals("Debe tener 2 transiciones verticales", 2, p2ToPb.transitions.size)
        assertEquals(1, p2ToPb.pointsByFloor["places_p1.json"]?.size)
    }

    @Test
    fun testDisconnectedGraphRouteFailure() {
        val disconnectedGraph = MapNavigationGraph(
            floorId = "PB",
            coordinateWidth = 1260f,
            coordinateHeight = 1600f,
            mapWidth = 1260f,
            mapHeight = 1600f,
            verticalOffset = 0f,
            nodes = listOf(
                MapNavigationNode("N1", 10f, 10f, MapNavigationNodeType.PATH),
                MapNavigationNode("N2", 500f, 500f, MapNavigationNodeType.PATH)
            ),
            edges = emptyList()
        )
        val mockGraphs = mapOf("places_pb.json" to disconnectedGraph)
        val p1 = MapPlace("p1", "Lugar 1", emptyList(), PlaceType.CLASSROOM, "Edif 1", "PB", "Planta Baja", "", emptyList(), "N1")
        val p2 = MapPlace("p2", "Lugar 2", emptyList(), PlaceType.CLASSROOM, "Edif 1", "PB", "Planta Baja", "", emptyList(), "N2")

        val result = service.calculateRouteWithGraphs(
            graphsByFloor = mockGraphs,
            originFloorAsset = "places_pb.json",
            originPoint = NormalizedMapPoint(0.01f, 0.01f),
            originPlace = p1,
            destinationFloorAsset = "places_pb.json",
            destinationPlace = p2,
            accessDataByFloor = emptyMap()
        )
        assertFalse("Ruta sin conectividad debe retornar éxito falso", result.success)
    }

    @Test
    fun testConsistencyWithLegacyMapScreenHelper() {
        val origin = checkNotNull(places["gestion_escolar"] ?: places.values.first())
        val destination = checkNotNull(places["aula_1201"] ?: places.values.first())

        val serviceResult = service.calculateRouteWithGraphs(
            graphsByFloor = graphs,
            originFloorAsset = "places_pb.json",
            originPoint = origin.centerPoint(),
            originPlace = origin,
            destinationFloorAsset = "places_p1.json",
            destinationPlace = destination,
            accessDataByFloor = accessData
        )

        val legacyPoints = buildMultiFloorRoutePoints(
            graphsByFloor = graphs,
            originFloorAsset = "places_pb.json",
            origin = origin.centerPoint(),
            originPlace = origin,
            destinationFloorAsset = "places_p1.json",
            destination = destination,
            accessDataByFloor = accessData
        )

        assertEquals("Los puntos calculados deben ser 100% idénticos entre el servicio y el helper heredado",
            serviceResult.pointsByFloor,
            legacyPoints
        )
    }

    // JSON parsing test helpers
    private fun findAssetMapsDir(): File {
        val candidates = listOf(
            File("src/main/assets/maps"),
            File("app/src/main/assets/maps")
        )
        return candidates.firstOrNull { it.exists() }
            ?: throw IllegalStateException("Directorio de mapas no encontrado")
    }

    private fun parseNavigationGraph(json: String): MapNavigationGraph {
        val root = JSONObject(json)
        val coordinates = root.getJSONObject("coordinateSystem")
        val nodesArray = root.getJSONArray("nodes")
        val edgesArray = root.getJSONArray("edges")

        val nodes = buildList {
            for (i in 0 until nodesArray.length()) {
                val item = nodesArray.getJSONObject(i)
                add(
                    MapNavigationNode(
                        id = item.getString("id"),
                        x = item.getDouble("x").toFloat(),
                        y = item.getDouble("y").toFloat(),
                        type = MapNavigationNodeType.fromJson(item.optString("tipo", item.optString("type", "CAMINO")))
                    )
                )
            }
        }
        val edges = buildList {
            for (i in 0 until edgesArray.length()) {
                val item = edgesArray.getJSONObject(i)
                add(
                    MapNavigationEdge(
                        from = item.getString("from"),
                        to = item.getString("to"),
                        weight = item.getDouble("weight").toFloat()
                    )
                )
            }
        }

        return MapNavigationGraph(
            floorId = root.getString("floorId"),
            coordinateWidth = coordinates.getDouble("width").toFloat(),
            coordinateHeight = coordinates.getDouble("height").toFloat(),
            mapWidth = coordinates.getDouble("mapWidth").toFloat(),
            mapHeight = coordinates.getDouble("mapHeight").toFloat(),
            verticalOffset = coordinates.optDouble("verticalOffset", 0.0).toFloat(),
            nodes = nodes,
            edges = edges
        )
    }

    private fun parseFloorAccessData(json: String): MapFloorAccessData {
        val root = JSONObject(json)
        val coordinates = root.getJSONObject("coordinateSystem")
        val placesArray = root.getJSONArray("places")
        val accessPointsByPlaceId = buildMap {
            for (i in 0 until placesArray.length()) {
                val place = placesArray.getJSONObject(i)
                val accessPointsArray = place.optJSONArray("accessPoints") ?: JSONArray()
                put(
                    place.getString("id"),
                    buildList {
                        for (j in 0 until accessPointsArray.length()) {
                            val access = accessPointsArray.getJSONObject(j)
                            add(
                                MapAccessPoint(
                                    id = access.getString("id"),
                                    x = access.getDouble("x").toFloat(),
                                    y = access.getDouble("y").toFloat(),
                                    type = access.optString("tipo", access.optString("type", "ENTRADA_SALIDA")),
                                    graphNodeIds = buildList {
                                        val arr = access.optJSONArray("graphNodeIds") ?: JSONArray()
                                        for (k in 0 until arr.length()) add(arr.getString(k))
                                    }
                                )
                            )
                        }
                    }
                )
            }
        }

        return MapFloorAccessData(
            floorId = root.getString("floorId"),
            coordinateWidth = coordinates.getDouble("width").toFloat(),
            mapHeight = coordinates.getDouble("mapHeight").toFloat(),
            verticalOffset = coordinates.optDouble("verticalOffset", 0.0).toFloat(),
            accessPointsByPlaceId = accessPointsByPlaceId
        )
    }

    private fun parsePlaces(json: String, assetName: String): List<MapPlace> {
        val root = JSONObject(json)
        val floorObj = root.getJSONObject("floor")
        val floorId = floorObj.getString("id")
        val floorName = floorObj.getString("name")
        val arr = root.getJSONArray("places")
        return buildList {
            for (i in 0 until arr.length()) {
                val item = arr.getJSONObject(i)
                val bounds = buildList {
                    val raw = item.opt("bounds")
                    if (raw is JSONArray) {
                        for (j in 0 until raw.length()) {
                            val b = raw.getJSONObject(j)
                            add(MapBounds(b.getDouble("left").toFloat(), b.getDouble("top").toFloat(), b.getDouble("right").toFloat(), b.getDouble("bottom").toFloat()))
                        }
                    }
                }
                add(
                    MapPlace(
                        id = item.getString("id"),
                        name = item.getString("name"),
                        aliases = emptyList(),
                        type = PlaceType.fromJson(item.optString("tipo", item.optString("type", "GENERAL"))),
                        building = item.optString("building", "ESCOM"),
                        floorId = floorId,
                        floorName = floorName,
                        description = item.optString("description", ""),
                        bounds = bounds,
                        navigationNodeId = item.optString("navigationNodeId").takeIf { it.isNotBlank() && it != "null" }
                    )
                )
            }
        }
    }
}

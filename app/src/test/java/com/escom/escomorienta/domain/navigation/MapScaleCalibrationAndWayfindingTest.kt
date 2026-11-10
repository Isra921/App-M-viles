package com.escom.escomorienta.domain.navigation

import com.escom.escomorienta.data.map.AStarRouteFinder
import com.escom.escomorienta.data.map.DefaultRouteCalculationService
import com.escom.escomorienta.data.map.MapAccessPoint
import com.escom.escomorienta.data.map.MapAssetCatalog
import com.escom.escomorienta.data.map.MapBounds
import com.escom.escomorienta.data.map.MapFloorAccessData
import com.escom.escomorienta.data.map.MapNavigationEdge
import com.escom.escomorienta.data.map.MapNavigationGraph
import com.escom.escomorienta.data.map.MapNavigationNode
import com.escom.escomorienta.data.map.MapNavigationNodeType
import com.escom.escomorienta.data.map.MapPlace
import com.escom.escomorienta.data.map.MapPlaceIndex
import com.escom.escomorienta.data.map.PlaceType
import com.escom.escomorienta.data.map.centerPoint
import com.escom.escomorienta.data.map.enclosingBounds
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import kotlin.math.abs

class MapScaleCalibrationAndWayfindingTest {

    private lateinit var graphs: Map<String, MapNavigationGraph>
    private lateinit var accessData: Map<String, MapFloorAccessData>
    private lateinit var placesByFloorAsset: Map<String, List<MapPlace>>
    private lateinit var placesById: Map<String, MapPlace>
    private lateinit var allPlaces: List<MapPlace>
    private lateinit var routeService: DefaultRouteCalculationService

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
        placesByFloorAsset = listOf("places_pb.json", "places_p1.json", "places_p2.json")
            .associateWith { parsePlaces(File(assetDir, it).readText()) }
        allPlaces = placesByFloorAsset.values.flatten()
        placesById = allPlaces.associateBy { it.id }
        routeService = DefaultRouteCalculationService()
    }

    // =========================================================================
    // 1. Conversión de píxeles originales a metros
    // =========================================================================
    @Test
    fun `1 conversion of original frame pixels to meters matches designer calibration`() {
        // 3.33 px = 1 m -> 1 px = 0.3003003 m
        assertEquals(1.0f, MapScaleCalibration.framePixelsToMeters(3.33f), 1e-4f)
        assertEquals(0.3003003f, MapScaleCalibration.PB_METERS_PER_PIXEL, 1e-6f)

        // Referencia del diseñador: Auditorio 86 x 39 px (~26 m x 12 m)
        val auditoriumWidthMeters = MapScaleCalibration.framePixelsToMeters(86f)
        val auditoriumHeightMeters = MapScaleCalibration.framePixelsToMeters(39f)
        assertEquals(25.8258f, auditoriumWidthMeters, 0.01f)
        assertEquals(11.7117f, auditoriumHeightMeters, 0.01f)
        assertEquals(26, kotlin.math.round(auditoriumWidthMeters).toInt())
        assertEquals(12, kotlin.math.round(auditoriumHeightMeters).toInt())

        // Verificación contra el recurso real auditorio en places_pb.json
        val auditorio = checkNotNull(placesById["auditorio"])
        val bounds = checkNotNull(auditorio.enclosingBounds())
        assertEquals(0.2103f, bounds.left, 1e-4f)
        assertEquals(0.1313f, bounds.top, 1e-4f)
        assertEquals(0.2786f, bounds.right, 1e-4f)
        assertEquals(0.1556f, bounds.bottom, 1e-4f)
    }

    // =========================================================================
    // 2. Aplicación de factores de ampliación distintos por piso
    // =========================================================================
    @Test
    fun `2 floor amplification factors apply distinct scales to PB vs P1 and P2`() {
        val pbScale = checkNotNull(MapScaleCalibration.resolveFloorScale("places_pb.json"))
        val p1Scale = checkNotNull(MapScaleCalibration.resolveFloorScale("places_p1.json"))
        val p2Scale = checkNotNull(MapScaleCalibration.resolveFloorScale("places_p2.json"))

        assertEquals(1.0f, pbScale.amplificationX, 1e-5f)
        assertEquals(2.0f, p1Scale.amplificationX, 1e-5f)
        assertEquals(2.0f, p2Scale.amplificationX, 1e-5f)

        // Un tramo de 100 px en PB equivale al doble de metros que 100 px en P1 o P2
        val pbMeters = MapScaleCalibration.pixelDeltaToMeters(100f, 0f, pbScale)
        val p1Meters = MapScaleCalibration.pixelDeltaToMeters(100f, 0f, p1Scale)
        val p2Meters = MapScaleCalibration.pixelDeltaToMeters(100f, 0f, p2Scale)

        assertEquals(30.03003f, pbMeters, 1e-4f)
        assertEquals(15.015015f, p1Meters, 1e-4f)
        assertEquals(15.015015f, p2Meters, 1e-4f)
        assertEquals(pbMeters / 2f, p1Meters, 1e-5f)
        assertEquals(p1Meters, p2Meters, 1e-5f)
    }

    // =========================================================================
    // 3. Conversión desde coordenadas normalizadas
    // =========================================================================
    @Test
    fun `3 conversion from normalized coordinates accounts for rectangular 1260x1600 frame`() {
        // Desplazamiento horizontal Δx = 0.1 -> 126 px en frame 1260x1600
        val pStart = NormalizedMapPoint(0.2f, 0.3f)
        val pHoriz = NormalizedMapPoint(0.3f, 0.3f)
        // Desplazamiento vertical Δy = 0.1 -> 160 px en frame 1260x1600
        val pVert = NormalizedMapPoint(0.2f, 0.4f)

        val distHorizPb = checkNotNull(
            MapScaleCalibration.normalizedDistanceToMeters(pStart, pHoriz, "places_pb.json")
        )
        val distVertPb = checkNotNull(
            MapScaleCalibration.normalizedDistanceToMeters(pStart, pVert, "places_pb.json")
        )

        // 126 px / 3.33 = 37.8378 m; 160 px / 3.33 = 48.0480 m
        assertEquals(126f / 3.33f, distHorizPb, 1e-3f)
        assertEquals(160f / 3.33f, distVertPb, 1e-3f)
        assertTrue("El eje Y (1600 px) debe representar mayor distancia por unidad normalizada que el eje X (1260 px)",
            distVertPb > distHorizPb
        )
    }

    // =========================================================================
    // 4. Invariancia de distancia ante zoom y tamaño de pantalla
    // =========================================================================
    @Test
    fun `4 route distance and time are invariant to screen zoom and viewport dimensions`() {
        val origin = checkNotNull(placesById["cafeteria"])
        val destination = checkNotNull(placesById["biblioteca"])

        val route = routeService.calculateRouteWithGraphs(
            graphsByFloor = graphs,
            originFloorAsset = "places_pb.json",
            originPoint = origin.centerPoint(),
            originPlace = origin,
            destinationFloorAsset = "places_pb.json",
            destinationPlace = destination,
            accessDataByFloor = accessData
        )
        val baseWayfinding = TurnByTurnWayfinding.generateInstructions(route, origin.name, destination.name)

        // Simulamos distintos niveles de zoom y tamaños de pantalla sobre los puntos de renderizado
        val zoomFactors = listOf(1.0f, 1.5f, 2.25f, 3.5f)
        val viewports = listOf(360f to 640f, 1080f to 2400f, 1440f to 3200f)

        for (zoom in zoomFactors) {
            for ((vw, vh) in viewports) {
                // La transformación visual a pantalla multiplica por viewport y zoom,
                // pero el dominio de rutas opera exclusivamente sobre NormalizedMapPoint del RouteResult
                val screenPoints = route.segments.first().points.map { pt ->
                    Pair(pt.x * vw * zoom, pt.y * vh * zoom)
                }
                assertTrue(screenPoints.isNotEmpty())

                // Al recalcular desde el modelo de ruta a cualquier nivel de zoom, la métrica es idéntica
                val recalculated = TurnByTurnWayfinding.generateInstructions(route, origin.name, destination.name)
                assertEquals(baseWayfinding.totalDistanceMeters, recalculated.totalDistanceMeters)
                assertEquals(baseWayfinding.totalTimeMinutes, recalculated.totalTimeMinutes)
                assertEquals(baseWayfinding.horizontalDistanceMeters, recalculated.horizontalDistanceMeters, 1e-5f)
            }
        }
    }

    // =========================================================================
    // 5. Comparación de referencias físicas equivalentes entre PB, P1 y P2
    // =========================================================================
    @Test
    fun `5 equivalent reference classrooms 1001 1101 1201 have consistent physical dimensions after calibration`() {
        val room1001 = checkNotNull(placesById["unidad_tecnologia_educativa_1001"])
        val room1101 = checkNotNull(placesById["salon_1101"])
        val room1201 = checkNotNull(placesById["salon_1201"])

        val b1001 = checkNotNull(room1001.enclosingBounds())
        val b1101 = checkNotNull(room1101.enclosingBounds())
        val b1201 = checkNotNull(room1201.enclosingBounds())

        // Ancho y alto en píxeles del frame 1260x1600
        val w0Px = (b1001.right - b1001.left) * 1260f
        val h0Px = (b1001.bottom - b1001.top) * 1600f
        val w1Px = (b1101.right - b1101.left) * 1260f
        val h1Px = (b1101.bottom - b1101.top) * 1600f
        val w2Px = (b1201.right - b1201.left) * 1260f
        val h2Px = (b1201.bottom - b1201.top) * 1600f

        // Factor de ampliación medido f_x = W1/W0 ~ 2.0, f_y = H1/H0 ~ 2.0
        assertEquals(2.0f, w1Px / w0Px, 0.02f)
        assertEquals(2.0f, h1Px / h0Px, 0.02f)
        assertEquals(2.0f, w2Px / w0Px, 0.02f)
        assertEquals(2.0f, h2Px / h0Px, 0.02f)

        // Dimensiones físicas calibradas en metros deben coincidir entre los tres niveles
        val w0Meters = MapScaleCalibration.framePixelsToMeters(w0Px, MapScaleCalibration.PB_AMPLIFICATION_FACTOR_X)
        val w1Meters = MapScaleCalibration.framePixelsToMeters(w1Px, MapScaleCalibration.P1_AMPLIFICATION_FACTOR_X)
        val w2Meters = MapScaleCalibration.framePixelsToMeters(w2Px, MapScaleCalibration.P2_AMPLIFICATION_FACTOR_X)

        val h0Meters = MapScaleCalibration.framePixelsToMeters(h0Px, MapScaleCalibration.PB_AMPLIFICATION_FACTOR_Y)
        val h1Meters = MapScaleCalibration.framePixelsToMeters(h1Px, MapScaleCalibration.P1_AMPLIFICATION_FACTOR_Y)
        val h2Meters = MapScaleCalibration.framePixelsToMeters(h2Px, MapScaleCalibration.P2_AMPLIFICATION_FACTOR_Y)

        assertEquals(w0Meters, w1Meters, 0.10f)
        assertEquals(w0Meters, w2Meters, 0.10f)
        assertEquals(h0Meters, h1Meters, 0.10f)
        assertEquals(h0Meters, h2Meters, 0.10f)
    }

    // =========================================================================
    // 6. Suma de distancias en rutas con varios segmentos
    // =========================================================================
    @Test
    fun `6 polyline distance sums real trajectory across corners rather than straight line`() {
        // Trayectoria en L sobre PB: (0.1, 0.1) -> (0.2, 0.1) -> (0.2, 0.2)
        val p1 = NormalizedMapPoint(0.1f, 0.1f)
        val p2 = NormalizedMapPoint(0.2f, 0.1f)
        val p3 = NormalizedMapPoint(0.2f, 0.2f)

        val seg1 = checkNotNull(MapScaleCalibration.normalizedDistanceToMeters(p1, p2, "PB"))
        val seg2 = checkNotNull(MapScaleCalibration.normalizedDistanceToMeters(p2, p3, "PB"))
        val direct = checkNotNull(MapScaleCalibration.normalizedDistanceToMeters(p1, p3, "PB"))
        val polyline = checkNotNull(MapScaleCalibration.polylineDistanceMeters(listOf(p1, p2, p3), "PB"))

        assertEquals(seg1 + seg2, polyline, 1e-4f)
        assertTrue("La distancia de trayectoria real por pasillos debe ser mayor que la línea recta",
            polyline > direct
        )
    }

    // =========================================================================
    // 7. Rutas con cambios de piso
    // =========================================================================
    @Test
    fun `7 multi floor routes include calibrated horizontal segments and vertical transitions`() {
        val origin = checkNotNull(placesById["unidad_tecnologia_educativa_1001"])
        val destP1 = checkNotNull(placesById["salon_1101"])
        val destP2 = checkNotNull(placesById["salon_1201"])

        val routePbToP1 = routeService.calculateRouteWithGraphs(
            graphsByFloor = graphs,
            originFloorAsset = "places_pb.json",
            originPoint = origin.centerPoint(),
            originPlace = origin,
            destinationFloorAsset = "places_p1.json",
            destinationPlace = destP1,
            accessDataByFloor = accessData
        )
        val routePbToP2 = routeService.calculateRouteWithGraphs(
            graphsByFloor = graphs,
            originFloorAsset = "places_pb.json",
            originPoint = origin.centerPoint(),
            originPlace = origin,
            destinationFloorAsset = "places_p2.json",
            destinationPlace = destP2,
            accessDataByFloor = accessData
        )

        val metricsPbToP1 = MapScaleCalibration.estimateRouteMetrics(routePbToP1)
        val metricsPbToP2 = MapScaleCalibration.estimateRouteMetrics(routePbToP2)

        assertTrue(metricsPbToP1.isValid)
        assertTrue(metricsPbToP2.isValid)
        assertEquals(7.0f, metricsPbToP1.verticalDistanceMeters, 1e-4f)
        assertEquals(14.0f, metricsPbToP2.verticalDistanceMeters, 1e-4f)
        assertTrue("Subir dos pisos (PB->P2) debe tener mayor distancia total que subir uno (PB->P1) hacia el salón equivalente",
            metricsPbToP2.totalDistanceMeters > metricsPbToP1.totalDistanceMeters
        )
    }

    // =========================================================================
    // 8. Manejo de aristas y conectores sin distancia física conocida
    // =========================================================================
    @Test
    fun `8 uncalibrated floor or unknown connector is handled safely without misleading metrics`() {
        // Segmento en piso no calibrado ("piso_desconocido.json")
        val unknownFloorRoute = RouteResult(
            success = true,
            segments = listOf(
                RouteSegment(
                    floorAsset = "piso_desconocido.json",
                    floorId = "X",
                    points = listOf(NormalizedMapPoint(0.1f, 0.1f), NormalizedMapPoint(0.5f, 0.5f)),
                    cost = 100f
                )
            )
        )
        val invalidEstimate = MapScaleCalibration.estimateRouteMetrics(unknownFloorRoute)
        assertFalse("Un piso sin escala verificada debe marcarse como inválido", invalidEstimate.isValid)
        val wayfindingInvalid = TurnByTurnWayfinding.generateInstructions(unknownFloorRoute)
        assertFalse(wayfindingInvalid.hasValidMetrics)
        assertEquals(0, wayfindingInvalid.totalDistanceMeters)

        // Transición con conector sin distancia física conocida ("CONECTOR_DESCONOCIDO")
        val routeWithUnknownConnector = RouteResult(
            success = true,
            segments = listOf(
                RouteSegment(
                    floorAsset = "places_pb.json",
                    floorId = "PB",
                    points = listOf(NormalizedMapPoint(0.1f, 0.1f), NormalizedMapPoint(0.2f, 0.1f)),
                    cost = 126f
                )
            ),
            transitions = listOf(
                RouteTransition(
                    connectorId = "CONECTOR_SIN_CALIBRAR",
                    fromFloorAsset = "places_pb.json",
                    toFloorAsset = "places_p1.json",
                    stairNodeId = "S99",
                    point = NormalizedMapPoint(0.2f, 0.1f)
                )
            ),
            isMultiFloor = true
        )
        val estimateUnknownConnector = MapScaleCalibration.estimateRouteMetrics(routeWithUnknownConnector)
        assertTrue(estimateUnknownConnector.isValid)
        assertEquals("No debe inventar distancia vertical para conectores desconocidos",
            0f, estimateUnknownConnector.verticalDistanceMeters, 1e-5f
        )
    }

    // =========================================================================
    // 9. Cálculo temporal y redondeo
    // =========================================================================
    @Test
    fun `9 travel time estimation uses walking speed model and coherent rounding`() {
        // 72 m horizontales a 1.2 m/s = 60 s = 1 min
        val (sec72, min72) = MapScaleCalibration.estimateTravelTime(72f, 0)
        assertEquals(60, sec72)
        assertEquals(1, min72)

        // 144 m horizontales a 1.2 m/s = 120 s = 2 min
        val (sec144, min144) = MapScaleCalibration.estimateTravelTime(144f, 0)
        assertEquals(120, sec144)
        assertEquals(2, min144)

        // Recorrido corto (12 m -> 10 s) no debe mostrar 0 min, sino mínimo 1 min
        val (secShort, minShort) = MapScaleCalibration.estimateTravelTime(12f, 0)
        assertEquals(10, secShort)
        assertEquals(1, minShort)

        // Con 2 cambios de piso (+36 s) y 120 m horizontales (100 s) -> 136 s -> 2 min
        val (secMulti, minMulti) = MapScaleCalibration.estimateTravelTime(120f, 2)
        assertEquals(136, secMulti)
        assertEquals(2, minMulti)
    }

    // =========================================================================
    // 10. Ausencia de valores métricos inválidos (NaN, Infinity, negativos)
    // =========================================================================
    @Test
    fun `10 invalid coordinates NaN Infinity or failed routes never produce NaN infinite or negative metrics`() {
        val nanRoute = RouteResult(
            success = true,
            segments = listOf(
                RouteSegment(
                    floorAsset = "places_pb.json",
                    floorId = "PB",
                    points = listOf(NormalizedMapPoint(Float.NaN, 0.2f), NormalizedMapPoint(0.3f, 0.4f))
                )
            )
        )
        val infRoute = RouteResult(
            success = true,
            segments = listOf(
                RouteSegment(
                    floorAsset = "places_pb.json",
                    floorId = "PB",
                    points = listOf(NormalizedMapPoint(Float.POSITIVE_INFINITY, 0.2f), NormalizedMapPoint(0.3f, 0.4f))
                )
            )
        )
        val failedRoute = RouteResult.failure("Error de prueba")

        for (r in listOf(nanRoute, infRoute, failedRoute)) {
            val summary = TurnByTurnWayfinding.generateInstructions(r)
            assertFalse(summary.hasValidMetrics)
            assertTrue(summary.totalDistanceMeters >= 0)
            assertTrue(summary.totalTimeMinutes >= 0)
            assertTrue(summary.horizontalDistanceMeters.isFinite())
            assertTrue(summary.verticalDistanceMeters.isFinite())
        }

        assertNull(
            MapScaleCalibration.normalizedDistanceToMeters(
                NormalizedMapPoint(Float.NaN, 0.1f),
                NormalizedMapPoint(0.2f, 0.2f),
                "PB"
            )
        )
        assertEquals(0f, MapScaleCalibration.framePixelsToMeters(-50f), 1e-5f)
        assertEquals(0f, MapScaleCalibration.framePixelsToMeters(Float.NaN), 1e-5f)
    }

    // =========================================================================
    // PRUEBAS DE REGRESIÓN Y VERIFICACIÓN DE LAS 6 RUTAS FÍSICAS RECOMENDADAS
    // =========================================================================
    @Test
    fun `regression and physical routes verification across ESCOM`() {
        data class RouteCase(
            val label: String,
            val originId: String,
            val destId: String,
            val expectedTransitions: Int
        )

        val cases = listOf(
            RouteCase("Cafetería -> Biblioteca", "cafeteria", "biblioteca", 0),
            RouteCase("Salón 1001 -> Salón 1101", "unidad_tecnologia_educativa_1001", "salon_1101", 1),
            RouteCase("Salón 1101 -> Salón 1201", "salon_1101", "salon_1201", 1),
            RouteCase("Salón 1201 -> Cafetería", "salon_1201", "cafeteria", 2),
            RouteCase("Cercanas mismo pasillo (1201 -> 1202)", "salon_1201", "salon_1202", 0),
            RouteCase("Lejanas varios pasillos (Entrada -> 4001)", "entrada_principal", "actividades_culturales_deportivas_4001", 0)
        )

        println("=== VERIFICACIÓN DE RUTAS FÍSICAS CALIBRADAS ===")
        for (c in cases) {
            val origin = checkNotNull(placesById[c.originId]) { "No se encontró ${c.originId}" }
            val dest = checkNotNull(placesById[c.destId]) { "No se encontró ${c.destId}" }
            val originAsset = checkNotNull(MapAssetCatalog.assetForFloor(origin.floorId))
            val destAsset = checkNotNull(MapAssetCatalog.assetForFloor(dest.floorId))

            val route = routeService.calculateRouteWithGraphs(
                graphsByFloor = graphs,
                originFloorAsset = originAsset,
                originPoint = origin.centerPoint(),
                originPlace = origin,
                destinationFloorAsset = destAsset,
                destinationPlace = dest,
                accessDataByFloor = accessData
            ).copy(
                origin = RouteEndpoint.NamedPlaceId(origin.id),
                destination = RouteEndpoint.NamedPlaceId(dest.id)
            )

            assertTrue("La ruta ${c.label} debe ser válida", route.success)
            assertEquals("Cantidad de transiciones en ${c.label}", c.expectedTransitions, route.transitions.size)
            assertEquals(RouteEndpoint.NamedPlaceId(origin.id), route.origin)
            assertEquals(RouteEndpoint.NamedPlaceId(dest.id), route.destination)

            val summary = TurnByTurnWayfinding.generateInstructions(route, origin.name, dest.name)
            assertTrue("Las métricas de ${c.label} deben ser válidas", summary.hasValidMetrics)
            assertTrue("Distancia positiva en ${c.label}", summary.totalDistanceMeters > 0)
            assertTrue("Tiempo positivo en ${c.label}", summary.totalTimeMinutes >= 1)

            val floorsInvolved = route.segments.map { it.floorId }.distinct().joinToString(" -> ")
            println(
                String.format(
                    "%-42s | Pisos: %-12s | Horiz: %6.1f m | Vert: %4.1f m | Total: %3d m | Tiempo: %3d s (%d min) | Cambios piso: %d",
                    c.label,
                    floorsInvolved,
                    summary.horizontalDistanceMeters,
                    summary.verticalDistanceMeters,
                    summary.totalDistanceMeters,
                    summary.estimatedSeconds,
                    summary.totalTimeMinutes,
                    route.transitions.size
                )
            )
        }

        // Regresión: búsqueda de salones y espacios
        val placeIndex = MapPlaceIndex(allPlaces)
        assertEquals("salon_1201", placeIndex.search("1201").first().id)
        assertEquals("cafeteria", placeIndex.search("Cafetería").first().id)
        assertEquals("biblioteca", placeIndex.search("Biblioteca").first().id)


        // Regresión: A* directo sobre los tres grafos sigue encontrando caminos óptimos
        for ((asset, graph) in graphs) {
            val firstPathNode = graph.nodes.first { it.type == MapNavigationNodeType.PATH }.id
            val lastPathNode = graph.nodes.last { it.type == MapNavigationNodeType.PATH }.id
            val path = AStarRouteFinder.findShortestPath(graph, firstPathNode, lastPathNode)
            assertTrue("A* debe encontrar ruta en $asset", path.isNotEmpty())
        }
    }

    // =========================================================================
    // Helpers de carga de assets reales
    // =========================================================================
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

    private fun parsePlaces(json: String): List<MapPlace> {
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
                            add(
                                MapBounds(
                                    b.getDouble("left").toFloat(),
                                    b.getDouble("top").toFloat(),
                                    b.getDouble("right").toFloat(),
                                    b.getDouble("bottom").toFloat()
                                )
                            )
                        }
                    } else if (raw is JSONObject) {
                        add(
                            MapBounds(
                                raw.getDouble("left").toFloat(),
                                raw.getDouble("top").toFloat(),
                                raw.getDouble("right").toFloat(),
                                raw.getDouble("bottom").toFloat()
                            )
                        )
                    }
                }
                val aliases = buildList {
                    val rawAliases = item.optJSONArray("aliases")
                    if (rawAliases != null) {
                        for (j in 0 until rawAliases.length()) {
                            add(rawAliases.getString(j))
                        }
                    }
                }
                add(
                    MapPlace(
                        id = item.getString("id"),
                        name = item.getString("name"),
                        aliases = aliases,
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

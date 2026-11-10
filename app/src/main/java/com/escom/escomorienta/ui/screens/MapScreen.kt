package com.escom.escomorienta.ui.screens

import android.content.Context
import com.escom.escomorienta.R
import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.escom.escomorienta.data.map.MapBounds
import com.escom.escomorienta.data.map.MapAccessPoint
import com.escom.escomorienta.data.map.MapFloor
import com.escom.escomorienta.data.map.MapFloorAccessData
import com.escom.escomorienta.data.map.MapNavigationGraph
import com.escom.escomorienta.data.map.MapNavigationNode
import com.escom.escomorienta.data.map.MapNavigationNodeType
import com.escom.escomorienta.data.map.MapPlace
import com.escom.escomorienta.data.map.MapRepository
import com.escom.escomorienta.data.map.PlaceType
import com.escom.escomorienta.data.map.extractRoomNumber
import com.escom.escomorienta.data.diagnostics.trace.TraceModule
import com.escom.escomorienta.data.diagnostics.trace.TraceStatus
import com.escom.escomorienta.data.diagnostics.trace.TraceEvent
import com.escom.escomorienta.data.diagnostics.trace.StructuredExecutionTracer
import com.escom.escomorienta.data.map.MapAssetCatalog
import com.escom.escomorienta.data.map.DefaultRouteCalculationService
import com.escom.escomorienta.data.map.DefaultRouteCalculationService.RouteEndpointCandidate
import com.escom.escomorienta.data.map.centerPoint
import com.escom.escomorienta.data.map.enclosingBounds
import com.escom.escomorienta.domain.navigation.NormalizedMapPoint
import com.escom.escomorienta.domain.navigation.RouteCalculationService
import com.escom.escomorienta.domain.navigation.RouteResult
import com.escom.escomorienta.domain.navigation.RouteTransition
import com.escom.escomorienta.domain.navigation.TurnByTurnWayfinding
import com.escom.escomorienta.domain.navigation.WayfindingSummary
import com.escom.escomorienta.ui.screens.map.TurnByTurnInstructionsSheet
import com.escom.escomorienta.data.map.MapSettingsPreferences
import com.escom.escomorienta.ui.theme.EscomAppTheme
import com.escom.escomorienta.ui.theme.appExtendedColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.Normalizer
import kotlin.math.sqrt
import kotlin.math.roundToInt

private const val MinMapScale = 1f
private const val MaxMapScale = 6f
private const val MapZoomSensitivity = 0.65f

private enum class MapLegendMarker {
    ZONE,
    CIRCLE,
    PIN
}

private data class MapLegendItem(
    val id: String,
    val label: String,
    val color: Color,
    val marker: MapLegendMarker = MapLegendMarker.ZONE
)

// Estas categorías reutilizan la simbología real aplicada por MapOverlayCanvas.
private val RealMapLegendItems = listOf(
    MapLegendItem("classrooms", "Salones / Aulas", Color(0x553B82C4)),
    MapLegendItem("laboratories", "Laboratorios", Color(0x665B61C9)),
    MapLegendItem("administrative", "Áreas administrativas", Color(0x662A9D8F)),
    MapLegendItem("study_recreation", "Estudio / Descanso (Palapas)", Color(0x66E59B5B)),
    MapLegendItem("services", "Servicios / Sanitarios", Color(0x6648CAE4)),
    MapLegendItem("auditorium", "Auditorio / Biblioteca", Color(0x66B06AB3)),
    MapLegendItem("origin", "Origen", Color(0xFF1684C5), MapLegendMarker.CIRCLE),
    MapLegendItem("destination", "Destino", Color(0xFF075985), MapLegendMarker.PIN),
    MapLegendItem("floor_change", "Cambio piso", Color(0xFFF59E0B), MapLegendMarker.CIRCLE)
)
private const val MaxPanSpeedMultiplier = 1.55f
private const val SelectedPlaceFocusScale = 4f
private const val MaxRouteFocusScale = 2.4f
private const val RouteFocusContentFraction = 0.64f
private const val PlaceTapTolerancePx = 14f
private val RoutePathSnapTolerance = 24.dp
private const val RouteStrokeWidth = 6.5f
private const val RouteHaloWidth = 11.5f
private const val PinSizeTransition = 18f
private const val MinRoomLabelScale = 1.25f
private const val MediumResolutionEnterScale = 1.8f
private const val MediumResolutionExitScale = 1.5f
private const val HighResolutionEnterScale = 3.6f
private const val HighResolutionExitScale = 3.2f
private const val RecentMapSearchesPrefs = "map_recent_searches"
private const val RecentMapPlaceIdsKey = "recent_place_ids"
private const val MaxRecentMapPlaces = 6
private const val RouteOriginRetentionMillis = 5 * 60 * 1_000L
private const val MapImageAspectRatio = 0.7875f

private val MapFloorAssets = listOf(
    "places_pb.json",
    "places_p1.json",
    "places_p2.json"
)

private val NavigationAssetByFloor = mapOf(
    "places_pb.json" to "navigation_pb.json",
    "places_p1.json" to "navigation_p1.json",
    "places_p2.json" to "navigation_p2.json"
)

private val AccessAssetByFloor = mapOf(
    "places_pb.json" to "accesos_pb.json",
    "places_p1.json" to "accesos_p1.json",
    "places_p2.json" to "accesos_p2.json"
)

private val StairNodeIdsByConnector = mapOf(
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

private val FeaturedMapPlaceIds = listOf(
    "letras_escom",
    "explanada",
    "estacionamiento_principal"
)

private val FloorReferenceLabels = mapOf(
    "places_pb.json" to listOf(
        "Entrada principal",
        "Explanada",
        "Cafetería",
        "Escaleras",
        "Baños"
    ),
    "places_p1.json" to listOf(
        "Laboratorios",
        "Escaleras",
        "Elevador",
        "Baños"
    ),
    "places_p2.json" to listOf(
        "Laboratorios",
        "Cubículos",
        "Escaleras",
        "Baños"
    )
)

private enum class MapResolutionLevel(
    val resourceSuffix: String
) {
    LOW("b"),
    MEDIUM("m"),
    HIGH("a")
}

private val restroomWords = setOf(
    "bano",
    "banos",
    "sanitario",
    "sanitarios",
    "wc",
    "servicio",
    "servicios"
)

private val womenRestroomWords = setOf(
    "mujer",
    "mujeres",
    "dama",
    "damas",
    "femenino",
    "femeninos",
    "femenina",
    "femeninas"
)

private val menRestroomWords = setOf(
    "hombre",
    "hombres",
    "caballero",
    "caballeros",
    "varon",
    "varones",
    "masculino",
    "masculinos",
    "masculina",
    "masculinas"
)

@Composable
fun MapScreen(
    navController: NavHostController,
    initialQuery: String? = null,
    initialRouteOrigin: String? = null,
    initialRouteDestination: String? = null
) {
    val context = LocalContext.current
    val repository = remember { MapRepository(context) }

    var selectedFloorAsset by remember { mutableStateOf("places_pb.json") }
    var currentFloor by remember { mutableStateOf<MapFloor?>(null) }
    val navigationGraphs = remember { mutableStateMapOf<String, MapNavigationGraph>() }
    val accessDataByFloor = remember { mutableStateMapOf<String, MapFloorAccessData>() }

    val places = remember { mutableStateListOf<MapPlace>() }
    val recentPlaceIds = remember { mutableStateListOf<String>() }

    var query by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var selectedPlaces by remember { mutableStateOf<List<MapPlace>>(emptyList()) }
    var routeOriginPoint by remember { mutableStateOf<NormalizedMapPoint?>(null) }
    var routeOriginPlace by remember { mutableStateOf<MapPlace?>(null) }
    var routeOriginFloorAsset by remember { mutableStateOf<String?>(null) }
    var routeDestination by remember { mutableStateOf<MapPlace?>(null) }
    var routeDestinationFloorAsset by remember { mutableStateOf<String?>(null) }
    var routeOriginExpiresAt by remember { mutableStateOf<Long?>(null) }
    var awaitingRouteOrigin by remember { mutableStateOf(false) }
    var showRoute by remember { mutableStateOf(false) }
    var initialQueryApplied by remember(initialQuery) { mutableStateOf(false) }
    var initialRouteApplied by remember(initialRouteOrigin, initialRouteDestination) {
        mutableStateOf(false)
    }
    var isMapOverview by remember { mutableStateOf(true) }
    var isLegendExpanded by remember { mutableStateOf(false) }
    val selectedPlace = selectedPlaces.firstOrNull()

    LaunchedEffect(selectedPlace) {
        if (selectedPlace != null) {
            isLegendExpanded = false
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val tracer = remember(context) { StructuredExecutionTracer(context.applicationContext) }

    fun accessDataForFloor(floorAsset: String): MapFloorAccessData? {
        accessDataByFloor[floorAsset]?.let { return it }
        val accessAsset = AccessAssetByFloor[floorAsset] ?: return null
        return repository.loadFloorAccessDataFromAssetsOrNull(accessAsset)?.also { accessData ->
            accessDataByFloor[floorAsset] = accessData
        }
    }

    fun retainRouteOriginTemporarily() {
        if (routeOriginPoint != null) {
            routeOriginExpiresAt = SystemClock.elapsedRealtime() + RouteOriginRetentionMillis
        }
    }

    fun hideRouteAndRetainOrigin() {
        showRoute = false
        awaitingRouteOrigin = false
        retainRouteOriginTemporarily()
    }

    fun showRouteFromRetainedOrigin(): Boolean {
        if (routeOriginPoint == null) return false

        routeOriginExpiresAt = null
        awaitingRouteOrigin = false
        showRoute = true
        return true
    }

    LaunchedEffect(routeOriginExpiresAt, showRoute, awaitingRouteOrigin) {
        val expiresAt = routeOriginExpiresAt ?: return@LaunchedEffect
        if (showRoute || awaitingRouteOrigin) return@LaunchedEffect

        delay((expiresAt - SystemClock.elapsedRealtime()).coerceAtLeast(0L))
        if (
            !showRoute &&
            !awaitingRouteOrigin &&
            routeOriginExpiresAt == expiresAt &&
            SystemClock.elapsedRealtime() >= expiresAt
        ) {
            routeOriginPoint = null
            routeOriginPlace = null
            routeOriginFloorAsset = null
            routeOriginExpiresAt = null
        }
    }

    LaunchedEffect(Unit) {
        recentPlaceIds.clear()
        recentPlaceIds.addAll(context.loadRecentMapPlaceIds())
        NavigationAssetByFloor.forEach { (floorAsset, navigationAsset) ->
            repository.loadNavigationGraphFromAssetsOrNull(navigationAsset)?.let { graph ->
                navigationGraphs[floorAsset] = graph
            }
        }
        AccessAssetByFloor.forEach { (floorAsset, accessAsset) ->
            repository.loadFloorAccessDataFromAssetsOrNull(accessAsset)?.let { accessData ->
                accessDataByFloor[floorAsset] = accessData
            }
        }
    }

    LaunchedEffect(
        selectedFloorAsset,
        initialQuery,
        initialRouteOrigin,
        initialRouteDestination
    ) {
        val requestedQuery = initialQuery?.trim().orEmpty()
        val requestedOrigin = initialRouteOrigin?.trim().orEmpty()
        val requestedDestination = initialRouteDestination?.trim().orEmpty()

        if (!initialRouteApplied && requestedOrigin.isNotBlank() && requestedDestination.isNotBlank()) {
            // Iniciar la vista de navegación visualmente en el piso del origen
            val origin = repository.searchAllPlaces(requestedOrigin).firstOrNull()
            val matchingFloorAsset = origin?.floorId?.let(MapAssetCatalog::assetForFloor)

            if (matchingFloorAsset != null && matchingFloorAsset != selectedFloorAsset) {
                selectedFloorAsset = matchingFloorAsset
                return@LaunchedEffect
            }
        }

        if (!initialQueryApplied && requestedQuery.isNotBlank()) {
            val matchingFloorAsset = MapFloorAssets.firstOrNull { floorAsset ->
                val floorPlaces = repository.loadPlacesFromAssets(floorAsset)
                repository.searchPlaces(requestedQuery, floorPlaces).isNotEmpty()
            }

            if (matchingFloorAsset != null && matchingFloorAsset != selectedFloorAsset) {
                selectedFloorAsset = matchingFloorAsset
                return@LaunchedEffect
            }
        }

        currentFloor = repository.loadFloorFromAssets(selectedFloorAsset)
        val loadedNavigationGraph = NavigationAssetByFloor[selectedFloorAsset]
            ?.let(repository::loadNavigationGraphFromAssetsOrNull)
        loadedNavigationGraph?.let { graph ->
            navigationGraphs[selectedFloorAsset] = graph
        }
        val loadedPlaces = repository.loadPlacesFromAssets(selectedFloorAsset)
        accessDataForFloor(selectedFloorAsset)
        places.clear()
        places.addAll(loadedPlaces)

        if (!initialRouteApplied && requestedOrigin.isNotBlank() && requestedDestination.isNotBlank()) {
            val origin = repository.searchAllPlaces(requestedOrigin).firstOrNull()
            val destination = repository.searchAllPlaces(requestedDestination).firstOrNull()
            if (origin != null && destination != null) {
                val originFloor = MapAssetCatalog.assetForFloor(origin.floorId)
                val destinationFloor = MapAssetCatalog.assetForFloor(destination.floorId)
                val originGraph = originFloor?.let { floor -> navigationGraphs[floor]
                    ?: NavigationAssetByFloor[floor]?.let(repository::loadNavigationGraphFromAssetsOrNull) }
                val destinationGraph = destinationFloor?.let { floor -> navigationGraphs[floor]
                    ?: NavigationAssetByFloor[floor]?.let(repository::loadNavigationGraphFromAssetsOrNull) }
                val originCandidates = if (originFloor != null && originGraph != null) {
                    buildRouteEndpointCandidates(origin, originGraph, accessDataForFloor(originFloor))
                } else emptyList()
                val destinationCandidates = if (destinationFloor != null && destinationGraph != null) {
                    buildRouteEndpointCandidates(destination, destinationGraph, accessDataForFloor(destinationFloor))
                } else emptyList()
                
                tracer.record(TraceEvent(
                    traceId = tracer.currentTraceId() ?: "route-init",
                    sequence = 0,
                    module = TraceModule.ROUTE,
                    stage = "GRAPH_ENDPOINT_RESOLVED",
                    eventType = "DOMAIN",
                    status = if (originCandidates.isNotEmpty() && destinationCandidates.isNotEmpty()) TraceStatus.SUCCESS else TraceStatus.FAILURE,
                    attributes = mapOf(
                        "origin" to origin.id, 
                        "destination" to destination.id,
                        "originResolved" to originCandidates.isNotEmpty().toString(),
                        "destinationResolved" to destinationCandidates.isNotEmpty().toString()
                    )
                ))

                if (originCandidates.isEmpty() || destinationCandidates.isEmpty()) {
                    initialRouteApplied = true
                    snackbarHostState.showSnackbar("No pude vincular una ubicación con la red de rutas. Selecciónala directamente en el mapa.")
                    return@LaunchedEffect
                }
                navigationGraphs[checkNotNull(originFloor)] = checkNotNull(originGraph)
                navigationGraphs[checkNotNull(destinationFloor)] = checkNotNull(destinationGraph)
                routeOriginPoint = origin.centerPoint()
                routeOriginPlace = origin
                routeOriginFloorAsset = originFloor
                routeDestination = destination
                routeDestinationFloorAsset = destinationFloor
                routeOriginExpiresAt = null
                selectedPlaces = if (destinationFloor == selectedFloorAsset) listOf(destination) else listOf(origin)
                query = destination.name
                showRoute = true
                initialRouteApplied = true
                initialQueryApplied = true
                return@LaunchedEffect
            }
        }

        val queryToApply = requestedQuery.takeIf { !initialQueryApplied }.orEmpty()
        if (queryToApply.isNotBlank()) {
            val matched = repository.searchPlaces(queryToApply, loadedPlaces).selectionForQuery(queryToApply)
            query = matched.singleOrNull()?.name ?: queryToApply
            selectedPlaces = matched
            initialQueryApplied = true
        } else {
            val preservedSelection = routeDestination
                ?.takeIf { destination ->
                    routeDestinationFloorAsset == selectedFloorAsset && (showRoute || awaitingRouteOrigin)
                }
                ?.let(::listOf)
            if (preservedSelection != null) {
                selectedPlaces = preservedSelection
            } else if (!initialQueryApplied) {
                query = ""
                selectedPlaces = emptyList()
            } else if (query.isNotBlank()) {
                selectedPlaces = repository.searchPlaces(query, loadedPlaces).selectionForQuery(query)
            } else {
                selectedPlaces = emptyList()
            }
            initialQueryApplied = true
        }
    }

    val searchResults = remember(query) {
        repository.searchAllPlaces(query)
    }

    val recentPlaces = remember(places.toList(), recentPlaceIds.toList()) {
        recentPlaceIds.mapNotNull { recentId ->
            places.firstOrNull { it.id == recentId }
        }
    }

    val frequentPlaces = remember(places.toList()) {
        FeaturedMapPlaceIds.mapNotNull { featuredId ->
            places.firstOrNull { it.id == featuredId }
        }
    }

    val routeCalculationService: RouteCalculationService = remember(repository) {
        DefaultRouteCalculationService(repository)
    }

    val routeResult = remember(
        showRoute,
        routeOriginPoint,
        routeOriginPlace,
        routeOriginFloorAsset,
        routeDestination,
        routeDestinationFloorAsset,
        navigationGraphs.toMap(),
        accessDataByFloor.toMap()
    ) {
        if (!showRoute) {
            RouteResult.failure("Ruta oculta")
        } else {
            routeCalculationService.calculateRouteWithGraphs(
                graphsByFloor = navigationGraphs,
                originFloorAsset = routeOriginFloorAsset,
                originPoint = routeOriginPoint,
                originPlace = routeOriginPlace,
                destinationFloorAsset = routeDestinationFloorAsset,
                destinationPlace = routeDestination,
                accessDataByFloor = accessDataByFloor
            )
        }
    }
    val routePointsByFloor = routeResult.pointsByFloor
    val currentRouteTransition = routeResult.transitions.firstOrNull { transition ->
        transition.fromFloorAsset == selectedFloorAsset
    }

    val wayfindingSummary = remember(
        routeResult,
        routeOriginPlace,
        routeDestination,
        showRoute
    ) {
        if (routeResult.success && showRoute) {
            TurnByTurnWayfinding.generateInstructions(
                route = routeResult,
                originName = routeOriginPlace?.name ?: "Punto de partida",
                destinationName = routeDestination?.name ?: "Destino",
                floorNameLookup = { floorAsset ->
                    when {
                        floorAsset.contains("pb", ignoreCase = true) -> "Planta Baja"
                        floorAsset.contains("p1", ignoreCase = true) -> "Piso 1"
                        floorAsset.contains("p2", ignoreCase = true) -> "Piso 2"
                        else -> floorAsset
                    }
                }
            )
        } else {
            null
        }
    }

    fun rememberRecentPlace(place: MapPlace) {
        recentPlaceIds.remove(place.id)
        recentPlaceIds.add(0, place.id)

        while (recentPlaceIds.size > MaxRecentMapPlaces) {
            recentPlaceIds.removeAt(recentPlaceIds.lastIndex)
        }

        context.saveRecentMapPlaceIds(recentPlaceIds)
    }

    fun selectPlace(place: MapPlace) {
        MapAssetCatalog.assetForFloor(place.floorId)?.let { targetAsset ->
            if (targetAsset != selectedFloorAsset) selectedFloorAsset = targetAsset
        }
        tracer.record(TraceEvent(
            traceId = tracer.currentTraceId() ?: "map-nav",
            sequence = 0,
            module = TraceModule.MAP,
            stage = "LOCATION_HIGHLIGHT_VISIBLE",
            eventType = "UI",
            status = TraceStatus.SUCCESS,
            attributes = mapOf("id" to place.id, "name" to place.name)
        ))
        selectedPlaces = listOf(place)
        hideRouteAndRetainOrigin()
        routeDestination = null
        routeDestinationFloorAsset = null
        query = place.name
        rememberRecentPlace(place)
        isSearchActive = false
        focusManager.clearFocus()
    }

    fun selectRouteOrigin(place: MapPlace) {
        val targetFloorAsset = MapAssetCatalog.assetForFloor(place.floorId) ?: return
        val graph = navigationGraphs[targetFloorAsset]
            ?: NavigationAssetByFloor[targetFloorAsset]?.let(repository::loadNavigationGraphFromAssetsOrNull)
            ?: return
        if (buildRouteEndpointCandidates(place, graph, accessDataForFloor(targetFloorAsset)).isEmpty()) return
        navigationGraphs[targetFloorAsset] = graph
        routeOriginPoint = place.centerPoint()
        routeOriginPlace = place
        routeOriginFloorAsset = targetFloorAsset
        routeOriginExpiresAt = null
        awaitingRouteOrigin = false
        showRoute = true
        query = ""
        selectedPlaces = routeDestination
            ?.takeIf { routeDestinationFloorAsset == selectedFloorAsset }
            ?.let(::listOf)
            .orEmpty()
        rememberRecentPlace(place)
        isSearchActive = false
        focusManager.clearFocus()
    }

    fun submitSearch() {
        val searchSelection = searchResults.selectionForQuery(query)

        if (searchSelection.isEmpty()) {
            if (!awaitingRouteOrigin) {
                selectedPlaces = emptyList()
                hideRouteAndRetainOrigin()
                routeDestination = null
                routeDestinationFloorAsset = null
            }
            scope.launch {
                snackbarHostState.showSnackbar("Sin coincidencias")
            }
            return
        }

        if (awaitingRouteOrigin) {
            selectRouteOrigin(searchSelection.first())
            return
        }

        if (searchSelection.size == 1) {
            selectPlace(searchSelection.first())
            return
        }

        selectedPlaces = searchSelection
        hideRouteAndRetainOrigin()
        routeDestination = null
        routeDestinationFloorAsset = null
        searchSelection.firstOrNull()?.let(::rememberRecentPlace)
        isSearchActive = false
        focusManager.clearFocus()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            EscomBottomNavigationBar(navController = navController)
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            InteractiveMapArea(
                places = places,
                selectedPlaces = selectedPlaces,
                routeOriginPoint = routeOriginPoint.takeIf {
                    routeOriginFloorAsset == selectedFloorAsset
                },
                routePoints = routePointsByFloor[selectedFloorAsset].orEmpty(),
                routeTransitionPoint = currentRouteTransition?.point,
                transitionLabel = currentRouteTransition?.toInstructionLabel(),
                awaitingRouteOrigin = awaitingRouteOrigin,
                showRoute = showRoute,
                navigationGraph = navigationGraphs[selectedFloorAsset],
                floorKey = selectedFloorAsset,
                mapImageResName = currentFloor?.imageResName ?: "escom_mapa_pb",
                mapContentDescription = currentFloor?.let { "Mapa 2D de ESCOM - ${it.name}" }
                    ?: "Mapa 2D de ESCOM",
                onPlaceSelected = ::selectPlace,
                onRouteOriginPlaced = { point, place ->
                    routeOriginPoint = point
                    routeOriginPlace = place
                    routeOriginFloorAsset = place?.floorId?.let(MapAssetCatalog::assetForFloor) ?: selectedFloorAsset
                    routeOriginExpiresAt = null
                    awaitingRouteOrigin = false
                    showRoute = true
                    query = ""
                    isSearchActive = false
                    focusManager.clearFocus()
                },
                onOverviewChange = { isMapOverview = it },
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(0f)
            )

            MapTopSearchArea(
                query = query,
                onQueryChange = { query = it },
                searchResults = searchResults,
                frequentPlaces = frequentPlaces,
                recentPlaces = recentPlaces,
                floorReferences = FloorReferenceLabels[selectedFloorAsset].orEmpty(),
                selectedPlaces = selectedPlaces,
                isSearchActive = isSearchActive,
                selectingRouteOrigin = awaitingRouteOrigin,
                showTitle = isMapOverview,
                onSearchActiveChange = { isSearchActive = it },
                onClearQuery = {
                    query = ""
                    if (!awaitingRouteOrigin) {
                        selectedPlaces = emptyList()
                        hideRouteAndRetainOrigin()
                        routeDestination = null
                        routeDestinationFloorAsset = null
                    }
                },
                onPlaceSelected = { place ->
                    if (awaitingRouteOrigin) selectRouteOrigin(place) else selectPlace(place)
                },
                onSearchSubmit = ::submitSearch,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .zIndex(2f)
            )

            FloorFloatingSelector(
                selectedAsset = selectedFloorAsset,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 12.dp)
                    .zIndex(2f),
                onFloorSelected = { asset ->
                    selectedFloorAsset = asset
                }
            )

            MapLegendPanel(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 12.dp, bottom = 12.dp)
                    .zIndex(2f),
                expanded = isLegendExpanded,
                onExpandedChange = { isLegendExpanded = it }
            )

            if (showRoute && wayfindingSummary != null) {
                TurnByTurnInstructionsSheet(
                    wayfinding = wayfindingSummary,
                    destinationName = routeDestination?.name ?: selectedPlace?.name ?: "Destino",
                    originName = routeOriginPlace?.name ?: "Punto de partida",
                    onCloseRoute = {
                        hideRouteAndRetainOrigin()
                        routeDestination = null
                        routeDestinationFloorAsset = null
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 10.dp)
                        .zIndex(3f)
                )
            } else {
                selectedPlace?.let { place ->
                    PlaceBottomPanel(
                        place = place,
                        selectedCount = selectedPlaces.size,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(start = 12.dp, end = 12.dp, bottom = 10.dp)
                            .zIndex(3f),
                        onClose = {
                            selectedPlaces = emptyList()
                            query = ""
                            hideRouteAndRetainOrigin()
                            routeDestination = null
                            routeDestinationFloorAsset = null
                        },
                        routeAvailable = selectedPlaces.size == 1,
                        routeVisible = showRoute,
                        hasRetainedRouteOrigin = routeOriginPoint != null,
                        awaitingRouteOrigin = awaitingRouteOrigin,
                        onRequestRouteOrigin = {
                            routeDestination = place
                            routeDestinationFloorAsset = MapAssetCatalog.assetForFloor(place.floorId) ?: selectedFloorAsset
                            if (!showRouteFromRetainedOrigin()) {
                                awaitingRouteOrigin = true
                                showRoute = false
                                query = ""
                                isSearchActive = false
                                focusManager.clearFocus()
                            }
                        },
                        onRouteToggle = {
                            if (showRoute) {
                                hideRouteAndRetainOrigin()
                            } else {
                                showRouteFromRetainedOrigin()
                            }
                        },
                        onChangeRouteOrigin = {
                            routeDestination = place
                            routeDestinationFloorAsset = MapAssetCatalog.assetForFloor(place.floorId) ?: selectedFloorAsset
                            awaitingRouteOrigin = true
                            showRoute = false
                            routeOriginExpiresAt = null
                            query = ""
                            isSearchActive = false
                            focusManager.clearFocus()
                        }
                    )
                }
            }
        }
    }

    BackHandler(enabled = isSearchActive) {
        isSearchActive = false
        focusManager.clearFocus()
    }
}

@Composable
private fun MapTopSearchArea(
    query: String,
    onQueryChange: (String) -> Unit,
    searchResults: List<MapPlace>,
    frequentPlaces: List<MapPlace>,
    recentPlaces: List<MapPlace>,
    floorReferences: List<String>,
    selectedPlaces: List<MapPlace>,
    isSearchActive: Boolean,
    selectingRouteOrigin: Boolean,
    showTitle: Boolean = true,
    onSearchActiveChange: (Boolean) -> Unit,
    onClearQuery: () -> Unit,
    onPlaceSelected: (MapPlace) -> Unit,
    onSearchSubmit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val searchShape = RoundedCornerShape(14.dp)
    val searchContentColor = MaterialTheme.colorScheme.onSurface
    val searchSecondaryColor = MaterialTheme.colorScheme.onSurfaceVariant
    val searchContainerColor = MaterialTheme.colorScheme.surface
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 4.dp, bottom = 6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        AnimatedVisibility(
            visible = showTitle,
            enter = fadeIn(tween(160)) + expandVertically(tween(160)),
            exit = fadeOut(tween(140)) + shrinkVertically(tween(140))
        ) {
            Text(
                text = "Mapa de ESCOM",
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .padding(horizontal = 2.dp)
                    .padding(bottom = 2.dp)
            )
        }

        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .shadow(1.dp, searchShape)
                .onFocusChanged { focusState ->
                    if (focusState.isFocused) {
                        onSearchActiveChange(true)
                    }
                }
                .onPreviewKeyEvent { event ->
                    if (event.key == Key.Enter && event.type == KeyEventType.KeyUp) {
                        onSearchSubmit()
                        true
                    } else {
                        false
                    }
                },
            textStyle = MaterialTheme.typography.bodyMedium,
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = {
                    onSearchSubmit()
                }
            ),
            shape = searchShape,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = searchContainerColor,
                unfocusedContainerColor = searchContainerColor,
                focusedTextColor = searchContentColor,
                unfocusedTextColor = searchContentColor,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                focusedLeadingIconColor = MaterialTheme.colorScheme.primary,
                unfocusedLeadingIconColor = searchSecondaryColor,
                focusedTrailingIconColor = searchSecondaryColor,
                unfocusedTrailingIconColor = searchSecondaryColor,
                focusedPlaceholderColor = searchSecondaryColor,
                unfocusedPlaceholderColor = searchSecondaryColor,
                cursorColor = MaterialTheme.colorScheme.primary
            ),
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = null,
                    tint = if (isSearchActive) MaterialTheme.colorScheme.primary else searchSecondaryColor
                )
            },
            trailingIcon = {
                if (query.isNotBlank() || isSearchActive) {
                    IconButton(
                        onClick = {
                            if (query.isNotBlank()) {
                                onClearQuery()
                            } else {
                                onSearchActiveChange(false)
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = if (query.isNotBlank()) {
                                "Limpiar búsqueda"
                            } else {
                                "Cerrar búsqueda"
                            },
                            tint = searchSecondaryColor
                        )
                    }
                }
            },
            placeholder = {
                Text(
                    text = if (selectingRouteOrigin) {
                        "Buscar lugar de inicio"
                    } else {
                        "Buscar salón o alias"
                    }
                )
            }
        )

        AnimatedVisibility(
            visible = isSearchActive,
            enter = fadeIn(tween(180)) + expandVertically(tween(180)),
            exit = fadeOut(tween(150)) + shrinkVertically(tween(150))
        ) {
            Surface(
                modifier = Modifier.border(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant,
                    RoundedCornerShape(14.dp)
                ),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (query.isBlank()) {
                        if (selectingRouteOrigin) {
                            Text(
                                text = "Elige un inicio con el buscador o tocando el mapa",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        SearchSuggestionSection(
                            title = "Ubicaciones frecuentes",
                            places = frequentPlaces,
                            onPlaceSelected = onPlaceSelected
                        )
                        SearchSuggestionSection(
                            title = "Búsquedas recientes",
                            places = recentPlaces,
                            onPlaceSelected = onPlaceSelected
                        )
                        FloorReferenceChips(references = floorReferences)
                    } else {
                        Text(
                            text = "Resultados",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelMedium
                        )
                        SearchResultChips(
                            results = searchResults,
                            selectedPlaces = selectedPlaces,
                            onPlaceSelected = onPlaceSelected
                        )
                        if (searchResults.isEmpty()) {
                            Text(
                                text = "Sin coincidencias en este nivel",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun List<MapPlace>.selectionForQuery(query: String): List<MapPlace> {
    if (isEmpty()) return emptyList()

    val normalizedQuery = query.normalizeForSearch()
    val restroomResults = filter { it.type == PlaceType.RESTROOM }

    return if (normalizedQuery.isRestroomSearch() && restroomResults.isNotEmpty()) {
        restroomResults
    } else {
        listOf(first())
    }
}

private fun String.isRestroomSearch(): Boolean {
    return restroomWords.any { containsSearchWord(it) } ||
        womenRestroomWords.any { containsSearchWord(it) } ||
        menRestroomWords.any { containsSearchWord(it) }
}

private fun String.containsSearchWord(word: String): Boolean {
    return split(Regex("\\s+")).any { it == word }
}

private fun String.normalizeForSearch(): String {
    val normalized = Normalizer.normalize(this, Normalizer.Form.NFD)
    return normalized
        .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
        .lowercase()
        .trim()
}

private fun List<MapPlace>.selectionKey(): String {
    return joinToString(separator = "|") { it.id }
}

@Composable
private fun SearchSuggestionSection(
    title: String,
    places: List<MapPlace>,
    onPlaceSelected: (MapPlace) -> Unit
) {
    if (places.isEmpty()) return

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            places.take(MaxRecentMapPlaces).forEach { place ->
                AssistChip(
                    onClick = { onPlaceSelected(place) },
                    label = {
                        Text(
                            text = place.name,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                )
            }
        }
    }

}

@Composable
private fun FloorReferenceChips(references: List<String>) {
    if (references.isEmpty()) return

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "Referencias de este nivel",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            references.forEach { reference ->
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = reference,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchResultChips(
    results: List<MapPlace>,
    selectedPlaces: List<MapPlace>,
    onPlaceSelected: (MapPlace) -> Unit
) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        results.take(8).forEach { place ->
            InputChip(
                selected = selectedPlaces.any { it.id == place.id },
                onClick = { onPlaceSelected(place) },
                label = {
                    Text(
                        text = place.name,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_escom_location_pin),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(18.dp)
                    )
                }
            )
        }
    }
}

@Composable
private fun InteractiveMapArea(
    places: SnapshotStateList<MapPlace>,
    selectedPlaces: List<MapPlace>,
    routeOriginPoint: NormalizedMapPoint?,
    routePoints: List<NormalizedMapPoint>,
    routeTransitionPoint: NormalizedMapPoint?,
    transitionLabel: String?,
    awaitingRouteOrigin: Boolean,
    showRoute: Boolean,
    navigationGraph: MapNavigationGraph?,
    floorKey: String,
    mapImageResName: String,
    mapContentDescription: String,
    onPlaceSelected: (MapPlace) -> Unit,
    onRouteOriginPlaced: (NormalizedMapPoint, MapPlace?) -> Unit,
    onOverviewChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var scale by remember(floorKey) { mutableFloatStateOf(MinMapScale) }
    var offset by remember(floorKey) { mutableStateOf(Offset.Zero) }

    val isOverview by remember(scale, offset) {
        derivedStateOf {
            scale <= 1.08f && (offset.x * offset.x + offset.y * offset.y) <= 144f
        }
    }

    LaunchedEffect(isOverview) {
        onOverviewChange(isOverview)
    }

    var displayedResolutionLevel by remember(floorKey) {
        mutableStateOf(MapResolutionLevel.LOW)
    }
    var mapSize by remember { mutableStateOf(IntSize(0, 0)) }
    val scaleResolutionLevel by remember(floorKey) {
        derivedStateOf {
            determineMapResolutionLevel(
                currentLevel = displayedResolutionLevel,
                scale = scale
            )
        }
    }
    val resolvedMapResource = remember(
        context,
        mapImageResName,
        displayedResolutionLevel
    ) {
        resolveMapDrawableResource(
            context = context,
            floorImageResName = mapImageResName,
            resolutionLevel = displayedResolutionLevel
        )
    }

    LaunchedEffect(floorKey, routePoints, showRoute, selectedPlaces.selectionKey(), mapSize) {
        if (selectedPlaces.isEmpty() && !showRoute) return@LaunchedEffect
        if (mapSize.width == 0 || mapSize.height == 0) return@LaunchedEffect

        if (showRoute && routePoints.isNotEmpty()) {
            val routeFocus = focusedRouteScaleAndOffset(routePoints, mapSize)
            scale = routeFocus.first
            offset = routeFocus.second
        } else if (selectedPlaces.isNotEmpty()) {
            val targetScale = suggestedFocusScale(selectedPlaces)
            scale = targetScale
            offset = focusedMapOffset(places = selectedPlaces, scale = targetScale, mapSize = mapSize)
        }

        displayedResolutionLevel = determineMapResolutionLevel(
            currentLevel = displayedResolutionLevel,
            scale = scale
        )
    }

    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.background)
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val availableAspectRatio = if (maxHeight.value > 0f) {
                maxWidth.value / maxHeight.value
            } else {
                MapImageAspectRatio
            }
            val mapViewportModifier = if (availableAspectRatio > MapImageAspectRatio) {
                Modifier
                    .fillMaxHeight()
                    .aspectRatio(MapImageAspectRatio, matchHeightConstraintsFirst = true)
            } else {
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(MapImageAspectRatio)
            }

            Box(
                modifier = mapViewportModifier
                .align(Alignment.Center)
                .onSizeChanged { mapSize = it }
                .pointerInput(floorKey) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        // TODO: Aplicar el zoom alrededor del centroide sin alterar los límites actuales.
                        val adjustedZoom = 1f + (zoom - 1f) * MapZoomSensitivity
                        val nextScale = (scale * adjustedZoom).coerceIn(MinMapScale, MaxMapScale)
                        val panMultiplier = 1f + ((nextScale - MinMapScale) / (MaxMapScale - MinMapScale)) *
                            (MaxPanSpeedMultiplier - 1f)

                        val maxPanX = size.width * (nextScale - MinMapScale) / 2f
                        val maxPanY = size.height * (nextScale - MinMapScale) / 2f

                        offset = if (nextScale == MinMapScale) {
                            Offset.Zero
                        } else {
                            Offset(
                                x = (offset.x + pan.x * panMultiplier).coerceIn(-maxPanX, maxPanX),
                                y = (offset.y + pan.y * panMultiplier).coerceIn(-maxPanY, maxPanY)
                            )
                        }
                        scale = nextScale
                    }
                }
                .pointerInput(floorKey) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        do {
                            val event = awaitPointerEvent(PointerEventPass.Final)
                        } while (event.changes.any { it.pressed })

                        displayedResolutionLevel = scaleResolutionLevel
                    }
                }
                .pointerInput(floorKey, awaitingRouteOrigin, navigationGraph) {
                    detectTapGestures { tapOffset ->
                        val centerX = size.width / 2f
                        val centerY = size.height / 2f
                        val relativeX = ((tapOffset.x - centerX - offset.x) / scale + centerX) / size.width
                        val relativeY = ((tapOffset.y - centerY - offset.y) / scale + centerY) / size.height
                        val toleranceX = PlaceTapTolerancePx / scale / size.width
                        val toleranceY = PlaceTapTolerancePx / scale / size.height

                        val clickedPlace = places.lastOrNull { place ->
                            place.bounds.any { bounds ->
                                bounds.contains(relativeX, relativeY, toleranceX, toleranceY)
                            }
                        }

                        if (awaitingRouteOrigin) {
                            val rawPoint = NormalizedMapPoint(
                                x = relativeX.coerceIn(0f, 1f),
                                y = relativeY.coerceIn(0f, 1f)
                            )
                            val placePoint = clickedPlace?.centerPoint()
                            val pathPoint = navigationGraph
                                ?.let { graph ->
                                    val nodes = graph.nodes.associate { node ->
                                        node.id to node.toNormalizedPoint(graph)
                                    }
                                    rawPoint.nearestNavigationAnchor(
                                        graph = graph,
                                        nodes = nodes,
                                        allowStairEdges = false
                                    )?.point
                                }
                                ?.takeIf { candidate ->
                                    rawPoint.screenDistanceTo(
                                        candidate,
                                        Size(size.width.toFloat(), size.height.toFloat()),
                                        scale
                                    ) <=
                                        RoutePathSnapTolerance.toPx()
                                }
                            onRouteOriginPlaced(
                                placePoint ?: pathPoint ?: rawPoint,
                                clickedPlace
                            )
                            return@detectTapGestures
                        }

                        if (clickedPlace != null) {
                            onPlaceSelected(clickedPlace)
                        }
                    }
                }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = offset.x
                            translationY = offset.y
                        }
                ) {
                    MapRasterImage(
                        mapImageResId = resolvedMapResource,
                        contentDescription = mapContentDescription,
                        modifier = Modifier.fillMaxSize()
                    )

                    MapOverlayCanvas(
                        places = places,
                        selectedPlaces = selectedPlaces,
                        routePoints = routePoints,
                        routeOriginPoint = routeOriginPoint,
                        routeTransitionPoint = routeTransitionPoint,
                        transitionLabel = transitionLabel,
                        showRoute = showRoute,
                        scale = scale,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        if (awaitingRouteOrigin) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 96.dp),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.inverseSurface,
                contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_escom_location_pin),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(17.dp)
                    )
                    Text("Elige el inicio en el mapa o buscador", style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        if (scale > MinMapScale + 0.01f || offset != Offset.Zero) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 12.dp, top = 96.dp)
                    .size(44.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant
                ),
                onClick = {
                    scale = MinMapScale
                    offset = Offset.Zero
                    displayedResolutionLevel = MapResolutionLevel.LOW
                }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.CenterFocusStrong,
                        contentDescription = "Ver mapa completo",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }

}

@Composable
private fun MapRasterImage(
    mapImageResId: Int,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    Image(
        painter = painterResource(id = mapImageResId),
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = ContentScale.Fit
    )
}

private fun determineMapResolutionLevel(
    currentLevel: MapResolutionLevel,
    scale: Float
): MapResolutionLevel {
    return when (currentLevel) {
        MapResolutionLevel.LOW -> when {
            scale >= HighResolutionEnterScale -> MapResolutionLevel.HIGH
            scale >= MediumResolutionEnterScale -> MapResolutionLevel.MEDIUM
            else -> MapResolutionLevel.LOW
        }

        MapResolutionLevel.MEDIUM -> when {
            scale >= HighResolutionEnterScale -> MapResolutionLevel.HIGH
            scale <= MediumResolutionExitScale -> MapResolutionLevel.LOW
            else -> MapResolutionLevel.MEDIUM
        }

        MapResolutionLevel.HIGH -> {
            if (scale <= HighResolutionExitScale) {
                MapResolutionLevel.MEDIUM
            } else {
                MapResolutionLevel.HIGH
            }
        }
    }
}

private fun buildMapResourceName(
    floorImageResName: String,
    resolutionLevel: MapResolutionLevel
): String {
    return "${floorImageResName}_${resolutionLevel.resourceSuffix}"
}

private fun resolveMapDrawableResource(
    context: Context,
    floorImageResName: String,
    resolutionLevel: MapResolutionLevel
): Int {
    val compatibleBaseNames = buildList {
        add(floorImageResName)
        Regex("^(.*_mapa_)([12])$").matchEntire(floorImageResName)?.let { match ->
            add("${match.groupValues[1]}p${match.groupValues[2]}")
        }
    }.distinct()

    val sameFloorResourceId = compatibleBaseNames
        .asSequence()
        .flatMap { baseName ->
            sequenceOf(buildMapResourceName(baseName, resolutionLevel), baseName) +
                MapResolutionLevel.entries.asSequence().map { level ->
                    buildMapResourceName(baseName, level)
                }
        }
        .map { resourceName ->
            context.resources.getIdentifier(resourceName, "drawable", context.packageName)
        }
        .firstOrNull { resourceId -> resourceId != 0 }

    checkNotNull(sameFloorResourceId) {
        "No existe ningún mapa para el piso: $floorImageResName"
    }

    return sameFloorResourceId
}

private fun suggestedFocusScale(places: List<MapPlace>): Float {
    val bounds = places.enclosingBounds()
        ?: return if (places.size > 1) MinMapScale else SelectedPlaceFocusScale
    val boundsWidth = (bounds.right - bounds.left).coerceAtLeast(0.01f)
    val boundsHeight = (bounds.bottom - bounds.top).coerceAtLeast(0.01f)
    val minimumScale = if (places.size > 1) MinMapScale else SelectedPlaceFocusScale

    return minOf(0.68f / boundsWidth, 0.68f / boundsHeight)
        .coerceIn(minimumScale, MaxMapScale)
}

private fun focusedMapOffset(
    places: List<MapPlace>,
    scale: Float,
    mapSize: IntSize
): Offset {
    val bounds = places.enclosingBounds() ?: return Offset.Zero
    val placeCenterX = (bounds.left + bounds.right) / 2f
    val placeCenterY = (bounds.top + bounds.bottom) / 2f

    return clampMapOffset(
        offset = Offset(
            x = (0.5f - placeCenterX) * mapSize.width * scale,
            y = (0.5f - placeCenterY) * mapSize.height * scale
        ),
        scale = scale,
        mapSize = mapSize
    )
}

private fun focusedRouteScaleAndOffset(
    routePoints: List<NormalizedMapPoint>,
    mapSize: IntSize
): Pair<Float, Offset> {
    if (routePoints.isEmpty() || mapSize.width == 0 || mapSize.height == 0) {
        return MinMapScale to Offset.Zero
    }

    val minX = routePoints.minOf { it.x }
    val maxX = routePoints.maxOf { it.x }
    val minY = routePoints.minOf { it.y }
    val maxY = routePoints.maxOf { it.y }
    val width = maxX - minX
    val height = maxY - minY
    val widthScale = if (width > 0f) RouteFocusContentFraction / width else MaxRouteFocusScale
    val heightScale = if (height > 0f) RouteFocusContentFraction / height else MaxRouteFocusScale
    val targetScale = minOf(widthScale, heightScale).coerceIn(MinMapScale, MaxRouteFocusScale)
    val centerX = (minX + maxX) / 2f
    val centerY = (minY + maxY) / 2f
    val targetOffset = clampMapOffset(
        offset = Offset(
            x = (0.5f - centerX) * mapSize.width * targetScale,
            y = (0.5f - centerY) * mapSize.height * targetScale
        ),
        scale = targetScale,
        mapSize = mapSize
    )
    return targetScale to targetOffset
}

private fun RouteTransition.toInstructionLabel(): String? {
    val fromIndex = MapFloorAssets.indexOf(fromFloorAsset)
    val toIndex = MapFloorAssets.indexOf(toFloorAsset)
    if (fromIndex < 0 || toIndex < 0 || fromIndex == toIndex) return null
    val action = if (toIndex > fromIndex) "Sube" else "Baja"
    return "$action a ${toFloorAsset.toFloorDisplayName()}"
}

private fun String.toFloorDisplayName(): String = when (this) {
    "places_pb.json" -> "Planta Baja"
    "places_p1.json" -> "Piso 1"
    "places_p2.json" -> "Piso 2"
    else -> this
}

private fun List<MapPlace>.enclosingBounds(): MapBounds? {
    val placeBounds = flatMap { it.bounds }
    if (placeBounds.isEmpty()) return null

    return MapBounds(
        left = placeBounds.minOf { it.left },
        top = placeBounds.minOf { it.top },
        right = placeBounds.maxOf { it.right },
        bottom = placeBounds.maxOf { it.bottom }
    )
}

private fun clampMapOffset(
    offset: Offset,
    scale: Float,
    mapSize: IntSize
): Offset {
    val maxPanX = mapSize.width * (scale - MinMapScale) / 2f
    val maxPanY = mapSize.height * (scale - MinMapScale) / 2f

    return Offset(
        x = offset.x.coerceIn(-maxPanX, maxPanX),
        y = offset.y.coerceIn(-maxPanY, maxPanY)
    )
}

@Composable
private fun MapOverlayCanvas(
    places: List<MapPlace>,
    selectedPlaces: List<MapPlace>,
    routePoints: List<NormalizedMapPoint>,
    routeOriginPoint: NormalizedMapPoint?,
    routeTransitionPoint: NormalizedMapPoint?,
    transitionLabel: String?,
    showRoute: Boolean,
    scale: Float,
    modifier: Modifier = Modifier
) {
    val mapHighlight = MaterialTheme.appExtendedColors.mapHighlight
    val mapHighlightStroke = MaterialTheme.appExtendedColors.mapHighlightStroke
    val routeColor = MaterialTheme.colorScheme.primary
    val routeHaloColor = Color.White.copy(alpha = 0.92f)
    val onSurface = MaterialTheme.colorScheme.onSurface
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    Canvas(modifier = modifier) {
        val density = this.density

        places
            .sortedByDescending { place -> place.bounds.sumOf { it.area().toDouble() } }
            .forEach { place ->
                val areaPath = place.bounds.toUnionPath(size) ?: return@forEach
                drawPath(path = areaPath, color = place.type.zoneColor())
                drawPath(
                    path = areaPath,
                    color = place.type.zoneStrokeColor(),
                    style = Stroke(width = 0.75f / scale)
                )

                // Identificación visual de aulas, laboratorios, espacios con código, Gestión Escolar
                // y espacios institucionales de Edificio de Gobierno (P1) con suficiente zoom
                val isGobiernoInstitutional = place.building.equals("Edificio de Gobierno", ignoreCase = true) &&
                    (place.type == PlaceType.ADMINISTRATIVE || place.type == PlaceType.INSTITUTIONAL_AREA ||
                     place.type == PlaceType.OFFICE || place.type == PlaceType.MEETING_ROOM) &&
                    scale >= 1.45f
                val isRoomCandidate = place.type == PlaceType.CLASSROOM ||
                    place.type == PlaceType.LABORATORY ||
                    place.type == PlaceType.COMPUTER_LAB ||
                    place.code != null ||
                    place.id == "gestion_escolar" ||
                    isGobiernoInstitutional
                if (isRoomCandidate && scale >= MinRoomLabelScale) {
                    val roomLabel = when {
                        place.id == "gestion_escolar" -> "Gestión Escolar"
                        place.building.equals("Edificio de Gobierno", ignoreCase = true) && place.code == null -> when (place.id) {
                            "departamento_extension_apoyos_educativos" -> "DEyAE"
                            "subdireccion_servicios_educativos_integracion_social" -> "SSEIS"
                            "subdireccion_academica" -> "Subdir. Académica"
                            "direccion" -> "Dirección"
                            "subdireccion_administrativa" -> "Subdir. Admin."
                            "pagaduria" -> "Pagaduría"
                            "sala_consejo_tecnico_consultivo_escolar" -> "Sala de Consejo"
                            "coordinacion_enlace_gestion_tecnica" -> "Enlace Técnico"
                            "departamento_recursos_materiales_servicios" -> "Rec. Materiales"
                            "coordinacion_sustentabilidad_capital_humano_recursos_financieros" -> "Cap. Humano / RF"
                            else -> place.name
                        }
                        else -> extractRoomNumber(place) ?: place.code
                    }
                    if (roomLabel != null) {
                        place.centerPoint()?.toOffset(size)?.let { center ->
                            val boundLeft = place.bounds.minOfOrNull { it.left } ?: 0f
                            val boundRight = place.bounds.maxOfOrNull { it.right } ?: 1f
                            val boundTop = place.bounds.minOfOrNull { it.top } ?: 0f
                            val boundBottom = place.bounds.maxOfOrNull { it.bottom } ?: 1f
                            val availableWidthPx = (boundRight - boundLeft) * size.width
                            val availableHeightPx = (boundBottom - boundTop) * size.height

                            val targetSp = if (place.id == "gestion_escolar" || isGobiernoInstitutional) {
                                (6.5f + (scale - MinRoomLabelScale) * 0.28f).coerceIn(6.0f, 8.0f)
                            } else {
                                (7.2f + (scale - MinRoomLabelScale) * 0.35f).coerceIn(6.5f, 9.5f)
                            }
                            val canvasTextSize = (targetSp * density) / scale

                            val roomPaint = Paint().apply {
                                color = onSurface.copy(alpha = if (place.id == "gestion_escolar" || isGobiernoInstitutional) (if (isDark) 0.85f else 0.78f) else 0.72f).toArgb()
                                textAlign = Paint.Align.CENTER
                                textSize = canvasTextSize
                                typeface = Typeface.DEFAULT_BOLD
                                isAntiAlias = true
                            }

                            val textWidth = roomPaint.measureText(roomLabel)
                            val canWrapVertically = availableHeightPx >= availableWidthPx * 0.48f
                            val shouldSplit = canWrapVertically &&
                                (roomLabel.contains("-") || roomLabel.contains("/") || roomLabel.contains(" ")) &&
                                (textWidth > availableWidthPx * 0.82f || availableHeightPx > availableWidthPx * 1.3f || place.id == "gestion_escolar")
                            val lines = if (shouldSplit) {
                                if (place.id == "gestion_escolar") {
                                    listOf("Gestión", "Escolar")
                                } else {
                                    roomLabel.split(Regex("[-/ ]")).filter(String::isNotBlank)
                                }
                            } else {
                                listOf(roomLabel)
                            }

                            val maxLineWidth = lines.maxOfOrNull { roomPaint.measureText(it) } ?: textWidth
                            val widthFactor = if (maxLineWidth > availableWidthPx * 0.88f && maxLineWidth > 0f) {
                                (availableWidthPx * 0.88f) / maxLineWidth
                            } else 1.0f
                            val rawTotalHeight = roomPaint.fontSpacing * lines.size
                            val heightFactor = if (rawTotalHeight > availableHeightPx * 0.84f && rawTotalHeight > 0f) {
                                (availableHeightPx * 0.84f) / rawTotalHeight
                            } else 1.0f
                            val scaleFactor = minOf(widthFactor, heightFactor)

                            val screenRenderedTextSizePx = canvasTextSize * scaleFactor * scale
                            if (screenRenderedTextSizePx >= 4.6f * density) {
                                roomPaint.textSize = canvasTextSize * scaleFactor
                                val lineHeight = roomPaint.fontSpacing
                                val totalTextHeight = lineHeight * lines.size
                                val startY = center.y - (totalTextHeight / 2f) + (lineHeight * 0.75f)

                                lines.forEachIndexed { lineIdx, lineText ->
                                    drawContext.canvas.nativeCanvas.drawText(
                                        lineText,
                                        center.x,
                                        startY + (lineIdx * lineHeight),
                                        roomPaint
                                    )
                                }
                            }
                        }
                    }
                }
            }

        if (showRoute) {
            if (routePoints.size == 1) {
                val point = routePoints.first().toOffset(size)
                drawCircle(
                    color = routeHaloColor,
                    radius = (14f * density) / scale,
                    center = point
                )
                drawCircle(
                    color = routeColor,
                    radius = (8f * density) / scale,
                    center = point
                )
            } else {
                routePoints.zipWithNext().forEach { (start, end) ->
                    drawLine(
                        color = routeHaloColor,
                        start = start.toOffset(size),
                        end = end.toOffset(size),
                        strokeWidth = RouteHaloWidth / scale
                    )
                    drawLine(
                        color = routeColor,
                        start = start.toOffset(size),
                        end = end.toOffset(size),
                        strokeWidth = RouteStrokeWidth / scale
                    )
                }
            }
        }

        routeTransitionPoint?.toOffset(size)?.let { transitionCenter ->
            drawFloorTransitionPin(transitionCenter, scale, transitionLabel)
        }

        selectedPlaces.forEach { place ->
            val selectedAreaPath = place.bounds.toUnionPath(size)

            if (selectedAreaPath != null) {
                drawPath(
                    path = selectedAreaPath,
                    color = Color.White.copy(alpha = if (isDark) 0.72f else 0.9f),
                    style = Stroke(width = (6f * density) / scale)
                )
                drawPath(
                    path = selectedAreaPath,
                    color = mapHighlight.copy(alpha = 0.96f)
                )

                drawPath(
                    path = selectedAreaPath,
                    color = mapHighlightStroke,
                    style = Stroke(width = (2.5f * density) / scale)
                )
            }
        }

        // Pines vectoriales de alta definición (Skia native anti-aliased)
        routeOriginPoint?.let { origin ->
            drawSelectionPin(origin, scale, isOrigin = true)
        }

        selectedPlaces.forEach { place ->
            place.selectionMarkerPoints().forEach { point ->
                drawSelectionPin(point, scale, isOrigin = false)
            }
        }
    }
}

private fun DrawScope.drawSelectionPin(
    point: NormalizedMapPoint,
    scale: Float,
    isOrigin: Boolean = false
) {
    val density = this.density
    val center = point.toOffset(size)
    // Tamaño visual en pantalla calibrado y elegante (~26dp), no gigantesco
    val pinHeight = (26f * density) / scale
    val pinWidth = pinHeight * 0.72f
    val radius = pinWidth / 2f

    // 1. Sombra elíptica sutil en el punto de contacto (suelo)
    val shadowWidth = pinWidth * 0.82f
    val shadowHeight = shadowWidth * 0.32f
    drawOval(
        color = Color.Black.copy(alpha = 0.22f),
        topLeft = Offset(center.x - shadowWidth / 2f, center.y - shadowHeight / 2f),
        size = Size(shadowWidth, shadowHeight)
    )

    // 2. Geometría vectorial pura del pin de ubicación (anclado en center.x, center.y)
    val pinHeadCenterY = center.y - pinHeight + radius
    val pinPath = Path().apply {
        moveTo(center.x, center.y)
        cubicTo(
            center.x - radius * 0.35f, center.y - pinHeight * 0.45f,
            center.x - radius, pinHeadCenterY + radius * 0.5f,
            center.x - radius, pinHeadCenterY
        )
        arcTo(
            rect = Rect(
                center.x - radius,
                pinHeadCenterY - radius,
                center.x + radius,
                pinHeadCenterY + radius
            ),
            startAngleDegrees = 180f,
            sweepAngleDegrees = 180f,
            forceMoveTo = false
        )
        cubicTo(
            center.x + radius, pinHeadCenterY + radius * 0.5f,
            center.x + radius * 0.35f, center.y - pinHeight * 0.45f,
            center.x, center.y
        )
        close()
    }

    val pinFillColor = if (isOrigin) Color(0xFF0284C7) else Color(0xFF005696)
    val strokeWidth = (2.2f * density) / scale

    // Borde blanco de alto contraste
    drawPath(
        path = pinPath,
        color = Color.White,
        style = Stroke(width = strokeWidth)
    )

    // Relleno institucional
    drawPath(
        path = pinPath,
        color = pinFillColor
    )

    // Detalle central moderno
    drawCircle(
        color = Color.White,
        radius = radius * 0.38f,
        center = Offset(center.x, pinHeadCenterY)
    )
}

private fun DrawScope.drawFloorTransitionPin(
    center: Offset,
    scale: Float,
    transitionLabel: String?
) {
    val radius = PinSizeTransition / scale
    drawCircle(Color.White, radius * 1.45f, center)
    drawCircle(Color(0xFFF59E0B), radius, center)
    drawLine(
        color = Color.White,
        start = Offset(center.x - radius * 0.45f, center.y + radius * 0.28f),
        end = Offset(center.x + radius * 0.45f, center.y - radius * 0.28f),
        strokeWidth = 2.5f / scale
    )

    if (transitionLabel != null) {
        val badgeTextSize = (9.0f * density) / scale
        val textPaint = Paint().apply {
            color = Color.White.toArgb()
            textAlign = Paint.Align.CENTER
            textSize = badgeTextSize
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }

        val textWidth = textPaint.measureText(transitionLabel)
        val padH = (7f * density) / scale
        val pillHeight = (18f * density) / scale
        val pillWidth = textWidth + padH * 2f
        val pillTop = center.y - (radius * 1.8f) - pillHeight
        val pillLeft = center.x - (pillWidth / 2f)

        // Fondo redondeado elegante de la insignia de transición
        drawRoundRect(
            color = Color(0xFF1E293B).copy(alpha = 0.94f),
            topLeft = Offset(pillLeft, pillTop),
            size = Size(pillWidth, pillHeight),
            cornerRadius = CornerRadius(pillHeight / 2f, pillHeight / 2f)
        )
        drawRoundRect(
            color = Color.White.copy(alpha = 0.65f),
            topLeft = Offset(pillLeft, pillTop),
            size = Size(pillWidth, pillHeight),
            cornerRadius = CornerRadius(pillHeight / 2f, pillHeight / 2f),
            style = Stroke(width = 1f / scale)
        )

        val textBaselineY = pillTop + (pillHeight / 2f) + (badgeTextSize * 0.35f)
        drawContext.canvas.nativeCanvas.drawText(
            transitionLabel,
            center.x,
            textBaselineY,
            textPaint
        )
    }
}

fun NormalizedMapPoint.toOffset(size: Size): Offset = Offset(x * size.width, y * size.height)

fun NormalizedMapPoint.screenDistanceTo(other: NormalizedMapPoint, size: Size, scale: Float): Float {
    val dx = (x - other.x) * size.width * scale
    val dy = (y - other.y) * size.height * scale
    return sqrt(dx * dx + dy * dy)
}

private fun MapNavigationNode.toNormalizedPoint(graph: MapNavigationGraph) = NormalizedMapPoint(
    x = (x / graph.coordinateWidth).coerceIn(0f, 1f),
    y = ((y + graph.verticalOffset) / graph.mapHeight).coerceIn(0f, 1f)
)

private fun MapBounds.area(): Float =
    ((right - left).coerceAtLeast(0f) * (bottom - top).coerceAtLeast(0f))

private fun MapPlace.selectionMarkerPoints(): List<NormalizedMapPoint> =
    if (markerPerBound) {
        bounds.map { bound ->
            NormalizedMapPoint(
                x = (bound.left + bound.right) / 2f,
                y = (bound.top + bound.bottom) / 2f
            )
        }
    } else {
        listOfNotNull(centerPoint())
    }

internal fun buildRouteEndpointCandidates(
    place: MapPlace,
    graph: MapNavigationGraph,
    accessData: MapFloorAccessData?
): List<RouteEndpointCandidate> {
    val service = DefaultRouteCalculationService()
    return service.buildRouteEndpointCandidates(place, graph, accessData)
}

private data class NavigationAnchor(
    val point: NormalizedMapPoint,
    val from: String,
    val to: String
)

private fun NormalizedMapPoint.nearestNavigationAnchor(
    graph: MapNavigationGraph,
    nodes: Map<String, NormalizedMapPoint>,
    allowStairEdges: Boolean
): NavigationAnchor? {
    val nodeTypes = graph.nodes.associate { node -> node.id to node.type }
    return graph.edges
        .asSequence()
        .filter { edge ->
            allowStairEdges || (
                nodeTypes.getValue(edge.from) == MapNavigationNodeType.PATH &&
                    nodeTypes.getValue(edge.to) == MapNavigationNodeType.PATH
                )
        }
        .map { edge ->
            val start = nodes.getValue(edge.from)
            val end = nodes.getValue(edge.to)
            NavigationAnchor(
                point = closestPointOnSegment(start, end),
                from = edge.from,
                to = edge.to
            )
        }
        .minByOrNull { anchor -> distanceSquaredTo(anchor.point) }
}

private fun NormalizedMapPoint.closestPointOnSegment(
    start: NormalizedMapPoint,
    end: NormalizedMapPoint
): NormalizedMapPoint {
    val segmentX = end.x - start.x
    val segmentY = end.y - start.y
    val lengthSquared = segmentX * segmentX + segmentY * segmentY
    if (lengthSquared == 0f) return start
    val projection = (((x - start.x) * segmentX + (y - start.y) * segmentY) / lengthSquared)
        .coerceIn(0f, 1f)
    return NormalizedMapPoint(
        x = start.x + segmentX * projection,
        y = start.y + segmentY * projection
    )
}

internal fun buildMultiFloorRoutePoints(
    graphsByFloor: Map<String, MapNavigationGraph>,
    originFloorAsset: String?,
    origin: NormalizedMapPoint?,
    originPlace: MapPlace?,
    destinationFloorAsset: String?,
    destination: MapPlace?,
    accessDataByFloor: Map<String, MapFloorAccessData>
): Map<String, List<NormalizedMapPoint>> {
    val service = DefaultRouteCalculationService()
    return service.calculateRouteWithGraphs(
        graphsByFloor = graphsByFloor,
        originFloorAsset = originFloorAsset,
        originPoint = origin,
        originPlace = originPlace,
        destinationFloorAsset = destinationFloorAsset,
        destinationPlace = destination,
        accessDataByFloor = accessDataByFloor
    ).pointsByFloor
}

private fun PlaceType.zoneColor(): Color = when (this) {
    PlaceType.CLASSROOM -> Color(0x553B82C4)
    PlaceType.LABORATORY, PlaceType.COMPUTER_LAB -> Color(0x665B61C9)
    PlaceType.ADMINISTRATIVE, PlaceType.OFFICE, PlaceType.INSTITUTIONAL_AREA -> Color(0x662A9D8F)
    PlaceType.SERVICE, PlaceType.RESTROOM, PlaceType.ACCESSIBILITY -> Color(0x6648CAE4)
    PlaceType.STUDY_AREA, PlaceType.SPORTS_AREA, PlaceType.RECREATION_AREA, PlaceType.CLUB -> Color(0x66E59B5B)
    PlaceType.AUDITORIUM, PlaceType.MEETING_ROOM, PlaceType.LIBRARY -> Color(0x66B06AB3)
    PlaceType.GREEN_AREA -> Color(0x6679B86B)
    PlaceType.PARKING -> Color(0x554A7898)
    PlaceType.ENTRANCE -> Color(0x7794C83D)
    PlaceType.MAINTENANCE, PlaceType.SMOKING_AREA -> Color(0x558A9299)
    PlaceType.REFERENCE, PlaceType.GENERAL -> Color.Transparent
}

private fun PlaceType.zoneStrokeColor(): Color = when (this) {
    PlaceType.SPORTS_AREA, PlaceType.RECREATION_AREA -> Color(0xFFB86122).copy(alpha = 0.65f)
    PlaceType.GREEN_AREA -> Color(0xFF397A36).copy(alpha = 0.65f)
    PlaceType.ENTRANCE -> Color(0xFF4D7C0F).copy(alpha = 0.72f)
    PlaceType.REFERENCE, PlaceType.GENERAL -> Color.Transparent
    else -> Color(0xFF174B73).copy(alpha = 0.38f)
}

private fun List<MapBounds>.toUnionPath(size: Size): Path? {
    if (isEmpty()) return null

    return map { bounds ->
        Path().apply {
            addRect(
                Rect(
                    left = bounds.left * size.width,
                    top = bounds.top * size.height,
                    right = bounds.right * size.width,
                    bottom = bounds.bottom * size.height
                )
            )
        }
    }.reduce { unionPath, boundsPath ->
        Path.combine(PathOperation.Union, unionPath, boundsPath)
    }
}

private fun MapBounds.contains(
    x: Float,
    y: Float,
    toleranceX: Float,
    toleranceY: Float
): Boolean {
    return x in (left - toleranceX)..(right + toleranceX) &&
        y in (top - toleranceY)..(bottom + toleranceY)
}

@Composable
private fun FloorFloatingSelector(
    selectedAsset: String,
    modifier: Modifier = Modifier,
    onFloorSelected: (String) -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        shadowElevation = 2.dp,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            listOf(
                "P2" to "places_p2.json",
                "P1" to "places_p1.json",
                "PB" to "places_pb.json"
            ).forEach { (label, asset) ->
                FloorButton(
                    label = label,
                    selected = selectedAsset == asset,
                    enabled = true,
                    onClick = { onFloorSelected(asset) }
                )
            }
        }
    }
}

@Composable
private fun FloorButton(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val textColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else if (enabled) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
    }

    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .then(
                if (selected) {
                    Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
                } else {
                    Modifier
                }
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = textColor,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

private fun Context.loadRecentMapPlaceIds(): List<String> {
    val rawValue = getSharedPreferences(RecentMapSearchesPrefs, Context.MODE_PRIVATE)
        .getString(RecentMapPlaceIdsKey, "")
        .orEmpty()

    return rawValue
        .split("|")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .distinct()
        .take(MaxRecentMapPlaces)
}

private fun Context.saveRecentMapPlaceIds(placeIds: List<String>) {
    getSharedPreferences(RecentMapSearchesPrefs, Context.MODE_PRIVATE)
        .edit()
        .putString(RecentMapPlaceIdsKey, placeIds.distinct().take(MaxRecentMapPlaces).joinToString("|"))
        .apply()
}

@Composable
private fun PlaceBottomPanel(
    place: MapPlace,
    selectedCount: Int,
    modifier: Modifier = Modifier,
    onClose: () -> Unit,
    routeAvailable: Boolean,
    routeVisible: Boolean,
    hasRetainedRouteOrigin: Boolean,
    awaitingRouteOrigin: Boolean,
    onRequestRouteOrigin: () -> Unit,
    onRouteToggle: () -> Unit,
    onChangeRouteOrigin: () -> Unit
) {
    var expanded by remember(place.id) { mutableStateOf(false) }

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant,
                RoundedCornerShape(16.dp)
            )
            .pointerInput(place.id) {
                detectVerticalDragGestures { _, dragAmount ->
                    when {
                        dragAmount < -8f -> expanded = true
                        dragAmount > 8f -> expanded = false
                    }
                }
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(42.dp)
                    .height(5.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(50))
                    .align(Alignment.CenterHorizontally)
            )

            Row(
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    val displayTitle = remember(place) {
                        if (place.type == PlaceType.CLASSROOM && !place.name.contains("Salón", true)) {
                            "Salón ${place.name}"
                        } else place.name
                    }
                    Text(
                        text = displayTitle,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.headlineSmall
                    )

                    Text(
                        text = "${place.type.displayName} · ${place.building}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Text(
                        text = place.floorName,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    if (selectedCount > 1) {
                        Text(
                            text = "$selectedCount coincidencias resaltadas",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Cerrar"
                    )
                }
            }

            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn(tween(200)) + expandVertically(tween(200)),
                exit = fadeOut(tween(180)) + shrinkVertically(tween(180))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 180.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = place.description,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (routeAvailable) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = if (routeVisible) onRouteToggle else onRequestRouteOrigin,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_escom_location_pin),
                                    contentDescription = null,
                                    tint = Color.Unspecified,
                                    modifier = Modifier.size(17.dp)
                                )
                                Text(
                                    text = when {
                                        awaitingRouteOrigin -> "Indica el lugar de inicio"
                                        routeVisible -> "Quitar ruta"
                                        hasRetainedRouteOrigin -> "Mostrar ruta"
                                        else -> "¿Cómo llegar?"
                                    },
                                    modifier = Modifier.padding(start = 6.dp)
                                )
                            }

                            if (hasRetainedRouteOrigin && !awaitingRouteOrigin) {
                                TextButton(
                                    onClick = onChangeRouteOrigin,
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                ) {
                                    Text("Cambiar inicio")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MapLegendPanel(
    modifier: Modifier = Modifier,
    items: List<MapLegendItem> = RealMapLegendItems,
    expanded: Boolean = false,
    onExpandedChange: (Boolean) -> Unit = {}
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start
    ) {
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Surface(
                modifier = Modifier
                    .padding(bottom = 6.dp)
                    .width(160.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
                shadowElevation = 2.dp,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        "Simbología",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    items.forEach { item ->
                        LegendItem(item)
                    }
                    Text(
                        text = "Toca un salón para ver más información.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }

        Surface(
            onClick = { onExpandedChange(!expanded) },
            shape = RoundedCornerShape(11.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 1.dp,
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant
            )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Leyenda",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.labelMedium
                )
                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) "Contraer leyenda" else "Expandir leyenda",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(17.dp)
                )
            }
        }
    }
}

@Composable
private fun LegendItem(item: MapLegendItem) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Box(
            modifier = Modifier
                .size(11.dp)
                .background(
                    color = item.color,
                    shape = if (item.marker == MapLegendMarker.ZONE) {
                        RoundedCornerShape(2.dp)
                    } else {
                        CircleShape
                    }
                )
        )
        Text(item.label, style = MaterialTheme.typography.bodySmall)
    }
}

@Preview(showBackground = true)
@Composable
fun MapScreenPreview() {
    val navController = rememberNavController()
    EscomAppTheme {
        MapScreen(navController = navController)
    }
}

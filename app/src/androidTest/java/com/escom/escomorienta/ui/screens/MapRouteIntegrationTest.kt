package com.escom.escomorienta.ui.screens

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.escom.escomorienta.data.map.MapRepository
import com.escom.escomorienta.data.map.centerPoint
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MapRouteIntegrationTest {
    @Test fun BUG_ROUTE_01_cross_floor_route_produces_render_segments() {
        val repository = MapRepository(ApplicationProvider.getApplicationContext())
        val origin = repository.searchAllPlaces("biblioteca").first()
        val destination = repository.searchAllPlaces("1202").first()
        val originAsset = com.escom.escomorienta.data.map.MapAssetCatalog.assetForFloor(origin.floorId)
        val destinationAsset = com.escom.escomorienta.data.map.MapAssetCatalog.assetForFloor(destination.floorId)
        assertNotNull(originAsset)
        assertNotNull(destinationAsset)
        val graphs = mapOf(
            "places_pb.json" to repository.loadNavigationGraphFromAssetsOrNull("navigation_pb.json")!!,
            "places_p1.json" to repository.loadNavigationGraphFromAssetsOrNull("navigation_p1.json")!!,
            "places_p2.json" to repository.loadNavigationGraphFromAssetsOrNull("navigation_p2.json")!!
        )
        val segments = buildMultiFloorRoutePoints(
            graphsByFloor = graphs,
            originFloorAsset = originAsset,
            origin = origin.centerPoint(),
            originPlace = origin,
            destinationFloorAsset = destinationAsset,
            destination = destination,
            accessDataByFloor = mapOf(
                "places_pb.json" to repository.loadFloorAccessDataFromAssetsOrNull("accesos_pb.json")!!
            )
        )
        assertTrue("La navegación no basta: deben existir segmentos dibujables", segments.values.any { it.size >= 2 })
    }

    @Test fun ROUTE_EQ_01_to_06_text_and_map_places_share_canonical_endpoints_and_levels() {
        val repository = MapRepository(ApplicationProvider.getApplicationContext())
        val mapOrigin = repository.findById("biblioteca")!!
        val mapDestination = repository.findById("salon_1202")!!
        val textOrigin = repository.searchAllPlaces("biblioteca").single { it.id == mapOrigin.id }
        val textDestination = repository.searchAllPlaces("1202").single { it.id == mapDestination.id }
        val selectedOrigin = repository.searchAllPlaces(mapOrigin.id).single { it.id == mapOrigin.id }
        val selectedDestination = repository.searchAllPlaces(mapDestination.id).single { it.id == mapDestination.id }
        val originAsset = com.escom.escomorienta.data.map.MapAssetCatalog.assetForFloor(textOrigin.floorId)!!
        val destinationAsset = com.escom.escomorienta.data.map.MapAssetCatalog.assetForFloor(textDestination.floorId)!!
        val originGraph = repository.loadNavigationGraphFromAssetsOrNull("navigation_pb.json")!!
        val destinationGraph = repository.loadNavigationGraphFromAssetsOrNull("navigation_p2.json")!!
        val graphs = mapOf(originAsset to originGraph,
            "places_p1.json" to repository.loadNavigationGraphFromAssetsOrNull("navigation_p1.json")!!,
            destinationAsset to destinationGraph)
        val accessData = mapOf(
            "places_pb.json" to repository.loadFloorAccessDataFromAssetsOrNull("accesos_pb.json")!!
        )
        val textSegments = buildMultiFloorRoutePoints(
            graphs, originAsset, textOrigin.centerPoint(), textOrigin,
            destinationAsset, textDestination, accessData
        )
        val mapSegments = buildMultiFloorRoutePoints(
            graphs, originAsset, selectedOrigin.centerPoint(), selectedOrigin,
            destinationAsset, selectedDestination, accessData
        )
        assertEquals(mapSegments.keys.toList(), textSegments.keys.toList()) // EQ-04/06
        assertEquals(mapSegments, textSegments) // EQ-05/08
        assertTrue(textSegments.size > 1)
    }

    @Test fun ROUTE_EQ_07_endpoint_without_graph_node_never_invents_point() {
        val repository = MapRepository(ApplicationProvider.getApplicationContext())
        val place = repository.searchAllPlaces("1202").first()
        val emptyGraph = com.escom.escomorienta.data.map.MapNavigationGraph(
            floorId = place.floorId, coordinateWidth = 1f, coordinateHeight = 1f,
            mapWidth = 1f, mapHeight = 1f, verticalOffset = 0f, nodes = emptyList(), edges = emptyList()
        )
        assertTrue(buildRouteEndpointCandidates(place, emptyGraph, null).isEmpty())
    }
}

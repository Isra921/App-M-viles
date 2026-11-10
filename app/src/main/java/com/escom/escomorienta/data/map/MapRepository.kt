package com.escom.escomorienta.data.map

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.Normalizer
import com.escom.escomorienta.domain.location.LocationInfo
import com.escom.escomorienta.domain.location.LocationRepository
import com.escom.escomorienta.domain.location.LocationMention

class MapRepository(
    private val context: Context
) : LocationRepository {
    private val allPlaces: List<MapPlace> by lazy {
        MapAssetCatalog.floorAssets.flatMap(::loadPlacesFromAssets)
    }
    private val globalIndex: MapPlaceIndex by lazy { MapPlaceIndex(allPlaces) }
    fun loadFloorFromAssets(assetFileName: String): MapFloor {
        val json = readAssetFile(assetFileName)
        val root = JSONObject(json)
        val floorObject = root.getJSONObject("floor")

        return MapFloor(
            id = floorObject.getString("id"),
            name = floorObject.getString("name"),
            assetFileName = assetFileName,
            imageResName = floorObject.getString("imageResName")
        )
    }

    fun loadPlacesFromAssets(assetFileName: String): List<MapPlace> {
        val json = readAssetFile(assetFileName)
        val root = JSONObject(json)
        val floorObject = root.getJSONObject("floor")

        val floorId = floorObject.getString("id")
        val floorName = floorObject.getString("name")

        val placesArray = root.getJSONArray("places")
        val places = mutableListOf<MapPlace>()

        for (index in 0 until placesArray.length()) {
            val item = placesArray.getJSONObject(index)
            places.add(
                MapPlace(
                    id = item.getString("id"),
                    name = item.getString("name"),
                    aliases = item.optJSONArray("aliases").toStringList(),
                    type = PlaceType.fromJson(
                        item.optString("tipo", item.optString("type", "GENERAL"))
                    ),
                    building = item.optString("building", "ESCOM"),
                    floorId = floorId,
                    floorName = floorName,
                    description = item.optString("description", ""),
                    bounds = item.toMapBoundsList(),
                    navigationNodeId = item.optString("navigationNodeId").takeIf { it.isNotBlank() && it != "null" },
                    markerPerBound = item.optBoolean("markerPerBound", false),
                    code = item.optString("code").takeIf { it.isNotBlank() && it != "null" }
                )
            )
        }

        return places
    }

    fun searchPlaces(
        query: String,
        places: List<MapPlace>
    ): List<MapPlace> {
        val normalizedQuery = query.normalizeForSearch()

        if (normalizedQuery.isBlank()) {
            return emptyList()
        }

        val directMatches = places
            .filter { place ->
                val matchesId = place.id.normalizeForSearch() == normalizedQuery
                val normalizedName = place.name.normalizeForSearch()
                val matchesName = normalizedName.contains(normalizedQuery)

                val matchesAlias = place.aliases.any { alias ->
                    alias.normalizeForSearch().contains(normalizedQuery)
                }

                matchesId || matchesName || matchesAlias
            }
            .sortedWith(
                compareBy<MapPlace> {
                    it.id.normalizeForSearch() != normalizedQuery
                }.thenBy {
                    !it.name.normalizeForSearch().startsWith(normalizedQuery)
                }.thenBy {
                    it.name
                }
            )

        if (directMatches.isNotEmpty()) {
            return directMatches
        }

        // Try typo correction on 4-char classroom codes (e.g. 12p9 -> 1209, 12o1 -> 1201)
        val typoMatch = Regex("(?<![a-z0-9-])[0-9opil]{4}(?![a-z0-9-])").find(normalizedQuery)
        if (typoMatch != null && typoMatch.value.any(Char::isLetter)) {
            val interpreted = typoMatch.value.map { char ->
                when (char) {
                    'o', 'p' -> '0'
                    'i', 'l' -> '1'
                    else -> char
                }
            }.joinToString("")
            val correctedMatches = places.filter { place ->
                val matchesId = place.id.normalizeForSearch() == interpreted
                val matchesName = place.name.normalizeForSearch().contains(interpreted)
                val matchesAlias = place.aliases.any { alias ->
                    alias.normalizeForSearch().contains(interpreted)
                }
                matchesId || matchesName || matchesAlias
            }.sortedWith(
                compareBy<MapPlace> { !it.name.normalizeForSearch().startsWith(interpreted) }.thenBy { it.name }
            )
            if (correctedMatches.isNotEmpty()) {
                return correctedMatches
            }
        }

        if (!normalizedQuery.isRestroomQuery()) {
            // Compound search: building + category/keywords
            val queryTokens = normalizedQuery.split(Regex("\\s+")).filter(String::isNotBlank)
            val buildingTokens = listOf("edificio 1", "edificio 2", "edificio 3", "edificio 4", "edificio 5", "gobierno")
            val matchedBuilding = buildingTokens.firstOrNull { normalizedQuery.contains(it) }
            if (matchedBuilding != null) {
                val otherTokens = queryTokens.filterNot { matchedBuilding.contains(it) }
                if (otherTokens.isNotEmpty()) {
                    val scoped = places.filter { place ->
                        val placeText = (listOf(place.id, place.name, place.description) + place.aliases)
                            .joinToString(" ")
                            .normalizeForSearch()
                        place.matchesBuildingMention(normalizedQuery) && otherTokens.all { placeText.contains(it) }
                    }.sortedBy { it.name }
                    if (scoped.isNotEmpty()) {
                        return scoped
                    }
                }
            }
            return emptyList()
        }

        val requestedGender = normalizedQuery.requestedRestroomGender()
        val restroomMatches = places
            .filter { place -> place.type == PlaceType.RESTROOM || place.name.normalizeForSearch().contains("bano") || place.name.normalizeForSearch().contains("sanitario") }
            .filter { place ->
                requestedGender == null || place.matchesRestroomGender(requestedGender)
            }

        val scopedByBuilding = restroomMatches.filter { place ->
            place.matchesBuildingMention(normalizedQuery)
        }

        return (scopedByBuilding.takeIf { it.isNotEmpty() } ?: restroomMatches)
            .sortedBy { it.name }
    }

    override fun getAllLocations(): List<LocationInfo> = allPlaces.map { it.toLocationInfo() }

    override fun search(query: String): List<LocationInfo> = searchAllPlaces(query).map { it.toLocationInfo() }

    override fun findById(id: String): LocationInfo? = allPlaces.firstOrNull { it.id == id }?.toLocationInfo()

    override fun findMentioned(text: String): List<LocationInfo> {
        return globalIndex.mentioned(text).map { it.toLocationInfo() }
    }

    override fun findMentionDetails(text: String): List<LocationMention> =
        globalIndex.mentionDetails(text).map {
            LocationMention(it.place.toLocationInfo(), it.matchedText, it.interpretedAs)
        }

    fun searchAllPlaces(query: String): List<MapPlace> = searchPlaces(query, allPlaces)

    fun getPlaceById(id: String): MapPlace? = allPlaces.firstOrNull { it.id == id }

    fun loadNavigationGraphFromAssets(assetFileName: String): MapNavigationGraph {
        val root = JSONObject(readAssetFile(assetFileName))
        val coordinates = root.getJSONObject("coordinateSystem")
        val nodesArray = root.getJSONArray("nodes")
        val edgesArray = root.getJSONArray("edges")

        val nodes = buildList {
            for (index in 0 until nodesArray.length()) {
                val item = nodesArray.getJSONObject(index)
                add(
                    MapNavigationNode(
                        id = item.getString("id"),
                        x = item.getDouble("x").toFloat(),
                        y = item.getDouble("y").toFloat(),
                        type = MapNavigationNodeType.fromJson(
                            item.optString("tipo", item.optString("type", "CAMINO"))
                        )
                    )
                )
            }
        }
        val edges = buildList {
            for (index in 0 until edgesArray.length()) {
                val item = edgesArray.getJSONObject(index)
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

    fun loadNavigationGraphFromAssetsOrNull(assetFileName: String): MapNavigationGraph? {
        val availableMapAssets = context.assets.list("maps").orEmpty()
        if (assetFileName !in availableMapAssets) return null

        return loadNavigationGraphFromAssets(assetFileName)
    }

    fun loadFloorAccessDataFromAssetsOrNull(assetFileName: String): MapFloorAccessData? {
        val availableMapAssets = context.assets.list("maps").orEmpty()
        if (assetFileName !in availableMapAssets) return null

        val root = JSONObject(readAssetFile(assetFileName))
        val coordinates = root.getJSONObject("coordinateSystem")
        val placesArray = root.getJSONArray("places")
        val accessPointsByPlaceId = buildMap {
            for (placeIndex in 0 until placesArray.length()) {
                val place = placesArray.getJSONObject(placeIndex)
                val accessPointsArray = place.optJSONArray("accessPoints") ?: JSONArray()
                put(
                    place.getString("id"),
                    buildList {
                        for (accessIndex in 0 until accessPointsArray.length()) {
                            val access = accessPointsArray.getJSONObject(accessIndex)
                            add(
                                MapAccessPoint(
                                    id = access.getString("id"),
                                    x = access.getDouble("x").toFloat(),
                                    y = access.getDouble("y").toFloat(),
                                    type = access.optString("tipo", access.optString("type", "ENTRADA_SALIDA")),
                                    graphNodeIds = access.optJSONArray("graphNodeIds").toStringList()
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

    fun getSuggestedPlaces(places: List<MapPlace>): List<MapPlace> {
        val priorityIds = listOf(
            "gestion_escolar",
            "banos_pb",
            "biblioteca",
            "auditorio",
            "aula_1001",
            "aula_1002"
        )

        return priorityIds.mapNotNull { id ->
            places.firstOrNull { it.id == id }
        }
    }

    fun getPlacesByFloor(
        floorId: String,
        places: List<MapPlace>
    ): List<MapPlace> {
        return places.filter { it.floorId == floorId }
    }

    private fun readAssetFile(fileName: String): String {
        return context.assets.open("maps/$fileName")
            .bufferedReader()
            .use { it.readText() }
    }

    private fun JSONArray?.toStringList(): List<String> {
        if (this == null) return emptyList()

        val result = mutableListOf<String>()

        for (index in 0 until length()) {
            result.add(getString(index))
        }

        return result
    }

    private fun JSONObject.toMapBounds(): MapBounds {
        return MapBounds(
            left = getDouble("left").toFloat(),
            top = getDouble("top").toFloat(),
            right = getDouble("right").toFloat(),
            bottom = getDouble("bottom").toFloat()
        )
    }

    private fun JSONObject.toMapBoundsList(): List<MapBounds> {
        return when (val rawBounds = opt("bounds")) {
            is JSONArray -> buildList {
                for (index in 0 until rawBounds.length()) {
                    add(rawBounds.getJSONObject(index).toMapBounds())
                }
            }
            is JSONObject -> listOf(rawBounds.toMapBounds())
            else -> emptyList()
        }
    }

    private fun String.normalizeForSearch(): String {
        val normalized = Normalizer.normalize(this, Normalizer.Form.NFD)
        return normalized
            .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
            .lowercase()
            .trim()
    }

    private fun MapPlace.toLocationInfo(): LocationInfo {
        val cleanName = name.trim()
        val hasNumericCode = code != null && code.matches(Regex("^[A-Za-z]?\\d{3,4}(?:-\\d{3,4})?$"))
        val baseDisplayName = when {
            hasNumericCode -> code!!
            cleanName.startsWith("Salón ", ignoreCase = true) -> cleanName
            cleanName.startsWith("Salon ", ignoreCase = true) -> "Salón " + cleanName.substring(6).trim()
            cleanName.startsWith("Aula ", ignoreCase = true) -> cleanName
            type == PlaceType.CLASSROOM && cleanName.matches(Regex("^\\d{3,4}$")) -> cleanName
            else -> cleanName
        }
        val extraAliases = mutableListOf<String>()
        extraAliases.addAll(aliases)
        extraAliases.add(cleanName)
        code?.let {
            extraAliases.add(it)
            extraAliases.add("salon $it")
            extraAliases.add("salón $it")
            extraAliases.add("aula $it")
        }
        return LocationInfo(
            id = id,
            displayName = baseDisplayName,
            building = building,
            floorId = floorId,
            floorName = floorName,
            category = type.name,
            mapTarget = id,
            aliases = extraAliases.distinct(),
            description = description
        )
    }

    private fun String.isRestroomQuery(): Boolean {
        return restroomWords.any { containsWholeWord(it) } ||
            requestedRestroomGender() != null
    }

    private fun String.requestedRestroomGender(): RestroomGender? {
        val asksForWomen = womenRestroomWords.any { containsWholeWord(it) }
        val asksForMen = menRestroomWords.any { containsWholeWord(it) }

        return when {
            asksForWomen && !asksForMen -> RestroomGender.WOMEN
            asksForMen && !asksForWomen -> RestroomGender.MEN
            else -> null
        }
    }

    private fun String.containsWholeWord(word: String): Boolean {
        return split(Regex("\\s+")).any { it == word }
    }

    private fun MapPlace.matchesRestroomGender(gender: RestroomGender): Boolean {
        val searchableText = (listOf(name) + aliases)
            .joinToString(" ")
            .normalizeForSearch()

        return when (gender) {
            RestroomGender.WOMEN -> womenRestroomWords.any { searchableText.containsWholeWord(it) }
            RestroomGender.MEN -> menRestroomWords.any { searchableText.containsWholeWord(it) }
        }
    }

    private fun MapPlace.matchesBuildingMention(normalizedQuery: String): Boolean {
        return buildingSearchTokens().any { token ->
            normalizedQuery.contains(token)
        }
    }

    private fun MapPlace.buildingSearchTokens(): List<String> {
        val normalizedBuilding = building.normalizeForSearch()
        val tokens = mutableListOf(normalizedBuilding)

        if (normalizedBuilding.startsWith("edificio ")) {
            tokens.add(normalizedBuilding.removePrefix("edificio ").trim())
        }

        if (normalizedBuilding.contains("gobierno")) {
            tokens.add("gobierno")
            tokens.add("edificio gobierno")
            tokens.add("edificio de gobierno")
        }

        return tokens
            .filter { it.length > 1 }
            .distinct()
    }

    private enum class RestroomGender {
        WOMEN,
        MEN
    }

    private companion object {
        val restroomWords = setOf(
            "bano",
            "banos",
            "sanitario",
            "sanitarios",
            "wc",
            "servicio",
            "servicios"
        )

        val womenRestroomWords = setOf(
            "mujer",
            "mujeres",
            "dama",
            "damas",
            "femenino",
            "femeninos",
            "femenina",
            "femeninas"
        )

        val menRestroomWords = setOf(
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
    }
}

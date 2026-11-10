package com.escom.escomorienta.domain.location

data class LocationInfo(
    val id: String,
    val displayName: String,
    val building: String,
    val floorId: String,
    val floorName: String,
    val category: String,
    val mapTarget: String,
    val aliases: List<String> = emptyList(),
    val description: String = ""
)

data class LocationMention(
    val location: LocationInfo,
    val matchedText: String,
    val interpretedAs: String? = null
)

interface LocationRepository {
    fun getAllLocations(): List<LocationInfo> = emptyList()
    fun search(query: String): List<LocationInfo>
    fun findMentioned(text: String): List<LocationInfo>
    fun findMentionDetails(text: String): List<LocationMention> =
        findMentioned(text).map { LocationMention(it, it.displayName) }
    fun findById(id: String): LocationInfo?
}

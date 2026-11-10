package com.escom.escomorienta.domain.institutional

import com.escom.escomorienta.domain.location.LocationRepository

enum class MapActionValidation { VALID, INVALID_LOCATION_ID, OFF_CAMPUS, NO_MAP_LOCATION }

class InstitutionalMapActionValidator(private val locations: LocationRepository) {
    fun validate(resource: InstitutionalResource): MapActionValidation {
        val id = resource.location?.mapLocationId
            ?: return if (resource.location?.displayName?.contains("DAE", ignoreCase = true) == true)
                MapActionValidation.OFF_CAMPUS else MapActionValidation.NO_MAP_LOCATION
        return if (locations.findById(id) != null) MapActionValidation.VALID
        else MapActionValidation.INVALID_LOCATION_ID
    }

    fun isValid(locationId: String?): Boolean = locationId != null && locations.findById(locationId) != null
}

package com.escom.escomorienta.domain.institutional

/** Institutional taxonomy. A location or external link is a property, never a resource type. */
enum class ResourceType { PROCEDURE, SERVICE, SUPPORT, INFORMATION }

enum class InstitutionalResourceCapability {
    SHOW_SUMMARY,
    SHOW_FULL_INFORMATION,
    SHOW_REQUIREMENTS,
    SHOW_STEPS,
    SHOW_SCHEDULE,
    SHOW_LOCATION,
    SHOW_ROUTE,
    OPEN_EXTERNAL_LINK
}

data class ResourceStep(val order: Int, val title: String, val description: String = "")
data class ResourceLocation(val mapLocationId: String? = null, val displayName: String)
data class ResourceSource(val title: String, val url: String? = null)
data class ResourceDocument(
    val id: String? = null,
    val name: String,
    val required: Boolean = false,
    val description: String = "",
    val type: String? = null,
    val url: String? = null
)
data class ResourceEstimatedTime(
    val type: String,
    val displayText: String,
    val official: Boolean = false,
    val basis: String? = null
)
data class ResourceTracking(
    val available: Boolean,
    val steps: List<ResourceStep> = emptyList(),
    val notes: List<String> = emptyList()
)
data class ResourceChannel(
    val id: String,
    val type: String,
    val name: String,
    val recommended: Boolean = false,
    val url: String? = null,
    val requirements: List<String> = emptyList(),
    val documents: List<ResourceDocument> = emptyList(),
    val estimatedTime: ResourceEstimatedTime? = null,
    val steps: List<ResourceStep> = emptyList(),
    val tracking: ResourceTracking? = null,
    val notes: List<String> = emptyList(),
    val variant: String? = null,
    val paymentDetails: List<String> = emptyList()
)
data class ResourceAction(val id: String, val name: String, val type: String, val url: String)
data class ResourceContact(
    val name: String,
    val role: String? = null,
    val phone: String? = null,
    val extension: String? = null,
    val email: String? = null,
    val shift: String? = null,
    val schedule: String? = null
)
data class ResourceOffering(
    val id: String,
    val type: String,
    val name: String,
    val description: String = "",
    val instructor: String? = null,
    val variants: List<String> = emptyList(),
    val divisions: List<String> = emptyList(),
    val topics: List<String> = emptyList(),
    val schedule: List<String> = emptyList(),
    val contacts: List<ResourceContact> = emptyList(),
    val location: ResourceLocation? = null,
    val cost: String? = null,
    val confidential: Boolean? = null,
    val documents: List<ResourceDocument> = emptyList(),
    val actions: List<ResourceAction> = emptyList(),
    val searchTargets: List<String> = emptyList(),
    val url: String? = null
)
data class ResourceAvailability(
    val active: Boolean?,
    val displayText: String,
    val notes: List<String> = emptyList()
)
data class ResourceApplicationProcess(
    val platformName: String,
    val platformUrl: String? = null,
    val steps: List<ResourceStep> = emptyList(),
    val recommendations: List<String> = emptyList()
)
data class ResourceScholarshipType(
    val id: String,
    val name: String,
    val requirements: List<String> = emptyList(),
    val requirementsStatus: String? = null
)

/** Specific archetype classification for better user experience and structured information. */
enum class InstitutionalArchetype {
    PROCEDURE,          // Trámites administrativos (bajas, constancias, credencial, boleta)
    FACILITY_SERVICE,   // Servicios e instalaciones continuas (biblioteca, médico, orientación)
    WORKSHOP_ACTIVITY,  // Actividades deportivas y talleres culturales
    SCHOLARSHIP         // Becas y apoyos estudiantiles
}

data class ResourceFaq(
    val question: String,
    val answer: String
)

data class ResourcePeriod(
    val type: String = "ACADEMIC_CALENDAR_WINDOW",
    val description: String,
    val warning: String? = null
)

data class ResourceServiceRule(
    val id: String? = null,
    val type: String,
    val description: String,
    val maxDurationMinutes: Int? = null,
    val minPeople: Int? = null,
    val maxPeople: Int? = null
)

data class ResourceSubService(
    val id: String,
    val name: String,
    val description: String = "",
    val rules: String? = null,
    val capacity: String? = null,
    val maxDurationMinutes: Int? = null
)

data class InstitutionalResource(
    val id: String,
    val type: ResourceType,
    val category: String,
    val name: String,
    val shortDescription: String = "",
    val description: String = "",
    val aliases: List<String> = emptyList(),
    val keywords: List<String> = emptyList(),
    val audience: List<String> = emptyList(),
    val requirements: List<String> = emptyList(),
    val documents: List<String> = emptyList(),
    val steps: List<ResourceStep> = emptyList(),
    val location: ResourceLocation? = null,
    val schedule: String? = null,
    val cost: String? = null,
    val notes: List<String> = emptyList(),
    val sources: List<ResourceSource> = emptyList(),
    val lastReviewed: String? = null,
    val channels: List<ResourceChannel> = emptyList(),
    val offerings: List<ResourceOffering> = emptyList(),
    val contact: ResourceContact? = null,
    val actions: List<ResourceAction> = emptyList(),
    val availability: ResourceAvailability? = null,
    val applicationProcess: ResourceApplicationProcess? = null,
    val scholarshipTypes: List<ResourceScholarshipType> = emptyList(),
    val resourceDocuments: List<ResourceDocument> = emptyList(),
    val archetype: InstitutionalArchetype = when (type) {
        ResourceType.PROCEDURE -> InstitutionalArchetype.PROCEDURE
        ResourceType.SUPPORT -> InstitutionalArchetype.SCHOLARSHIP
        ResourceType.SERVICE -> if (category == "CULTURA" || category == "DEPORTE") InstitutionalArchetype.WORKSHOP_ACTIVITY else InstitutionalArchetype.FACILITY_SERVICE
        ResourceType.INFORMATION -> InstitutionalArchetype.FACILITY_SERVICE
    },
    val period: ResourcePeriod? = null,
    val serviceRules: List<ResourceServiceRule> = emptyList(),
    val subServices: List<ResourceSubService> = emptyList(),
    val faqs: List<ResourceFaq> = emptyList()
) {
    /** Explicit conversational name; the stable [id] is never user-facing. */
    val canonicalName: String get() = name

    /** Capabilities are derived only from canonical catalog facts. */
    val supportedCapabilities: Set<InstitutionalResourceCapability>
        get() = buildSet {
            add(InstitutionalResourceCapability.SHOW_SUMMARY)
            add(InstitutionalResourceCapability.SHOW_FULL_INFORMATION)
            if (requirements.isNotEmpty() || documents.isNotEmpty() ||
                channels.any { it.requirements.isNotEmpty() || it.documents.isNotEmpty() } ||
                scholarshipTypes.any { it.requirements.isNotEmpty() }
            ) add(InstitutionalResourceCapability.SHOW_REQUIREMENTS)
            if (steps.isNotEmpty() || channels.any { it.steps.isNotEmpty() } ||
                applicationProcess?.steps?.isNotEmpty() == true
            ) add(InstitutionalResourceCapability.SHOW_STEPS)
            if (!schedule.isNullOrBlank() || offerings.any { it.schedule.isNotEmpty() })
                add(InstitutionalResourceCapability.SHOW_SCHEDULE)
            if (location?.mapLocationId != null) {
                add(InstitutionalResourceCapability.SHOW_LOCATION)
                add(InstitutionalResourceCapability.SHOW_ROUTE)
            }
            if (actions.isNotEmpty() || channels.any { it.url != null } ||
                resourceDocuments.any { it.url != null } || offerings.any { offering ->
                    offering.url != null || offering.actions.isNotEmpty() || offering.documents.any { it.url != null }
                }
            ) add(InstitutionalResourceCapability.OPEN_EXTERNAL_LINK)
        }
}

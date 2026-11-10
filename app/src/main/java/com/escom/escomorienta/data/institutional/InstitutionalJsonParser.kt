package com.escom.escomorienta.data.institutional

import com.escom.escomorienta.domain.institutional.*
import org.json.JSONArray
import org.json.JSONObject

class InstitutionalJsonParser {
    fun parseResource(json: String): InstitutionalResource = parseResource(JSONObject(json))

    fun parseManifest(json: String): List<String> {
        val root = JSONObject(json)
        val schemaVersion = root.optInt("schemaVersion", root.optInt("version", 0))
        require(schemaVersion > 0) { "manifest.schemaVersion debe ser mayor que cero" }
        return root.getJSONArray("resources").strings().map { it.replace('\\', '/') }.also { paths ->
            require(paths.none { it.isBlank() || it.startsWith("/") || ".." in it }) {
                "manifest contiene una ruta no válida"
            }
        }
    }

    private fun parseResource(root: JSONObject): InstitutionalResource {
        val id = root.optString("id").trim()
        val name = root.optString("name").ifEmpty { root.optString("title") }.trim()
        require(id.isNotEmpty()) { "id está vacío" }
        require(name.isNotEmpty()) { "name está vacío" }
        val rawArchetype = root.optString("archetype").trim().uppercase()
        val parsedArchetype = runCatching { InstitutionalArchetype.valueOf(rawArchetype) }.getOrNull()
        val type = runCatching { ResourceType.valueOf(root.optString("type").trim().uppercase()) }
            .getOrElse {
                when (parsedArchetype) {
                    InstitutionalArchetype.PROCEDURE -> ResourceType.PROCEDURE
                    InstitutionalArchetype.FACILITY_SERVICE -> ResourceType.SERVICE
                    InstitutionalArchetype.WORKSHOP_ACTIVITY -> ResourceType.SERVICE
                    InstitutionalArchetype.SCHOLARSHIP -> ResourceType.SUPPORT
                    null -> throw IllegalArgumentException("type no válido: ${root.optString("type")}")
                }
            }
        val effectiveArchetype = parsedArchetype ?: when (type) {
            ResourceType.PROCEDURE -> InstitutionalArchetype.PROCEDURE
            ResourceType.SUPPORT -> InstitutionalArchetype.SCHOLARSHIP
            ResourceType.SERVICE -> if (root.optString("category").trim().uppercase() in listOf("CULTURA", "DEPORTE")) InstitutionalArchetype.WORKSHOP_ACTIVITY else InstitutionalArchetype.FACILITY_SERVICE
            ResourceType.INFORMATION -> InstitutionalArchetype.FACILITY_SERVICE
        }
        val channels = root.optJSONArray("channels").objects().map(::parseChannel)
        val recommendedChannel = root.optJSONArray("channels").objects().firstOrNull { it.optBoolean("recommended", false) }
        val rawSteps = root.optJSONArray("workflowSteps") ?: root.optJSONArray("steps") ?: recommendedChannel?.optJSONArray("steps")
        val steps = rawSteps.objects().map { item ->
            val description = item.optString("description", item.optString("detail")).trim()
            val title = item.optString("title").trim().ifEmpty { description }
            val order = item.optInt("order", item.optInt("step", 0))
            ResourceStep(order, title, if (title == description) "" else description)
        }
        require(steps.all { it.order > 0 && it.title.isNotEmpty() }) { "steps requiere order > 0 y title" }
        require(steps.map { it.order }.distinct().size == steps.size) { "steps contiene órdenes duplicados" }

        val locationObj = root.optJSONObject("location")
        val directLocationId = root.optionalString("locationId")
        val resolvedLocation = when {
            locationObj != null -> {
                val displayName = locationObj.optString("displayName").trim()
                require(displayName.isNotEmpty()) { "location.displayName está vacío" }
                ResourceLocation(locationObj.optString("mapLocationId").trim().ifEmpty { null }, displayName)
            }
            directLocationId != null -> ResourceLocation(directLocationId, directLocationId.readableLabel())
            else -> null
        }

        val period = root.optJSONObject("period")?.let { p ->
            ResourcePeriod(
                type = p.optString("type", "ACADEMIC_CALENDAR_WINDOW"),
                description = p.optString("description"),
                warning = p.optionalString("warning")
            )
        }
        val serviceRules = root.optJSONArray("serviceRules").objects().map { rule ->
            ResourceServiceRule(
                id = rule.optionalString("id"),
                type = rule.optString("type"),
                description = rule.optString("description"),
                maxDurationMinutes = if (rule.has("maxDurationMinutes")) rule.optInt("maxDurationMinutes") else null,
                minPeople = if (rule.has("minPeople")) rule.optInt("minPeople") else null,
                maxPeople = if (rule.has("maxPeople")) rule.optInt("maxPeople") else null
            )
        }
        val subServices = root.optJSONArray("subServices").objects().map { sub ->
            ResourceSubService(
                id = sub.optString("id"),
                name = sub.optString("name"),
                description = sub.optString("description"),
                rules = sub.optionalString("rules"),
                capacity = sub.optionalString("capacity"),
                maxDurationMinutes = if (sub.has("maxDurationMinutes")) sub.optInt("maxDurationMinutes") else null
            )
        }
        val faqs = root.optJSONArray("faqs").objects().map { faq ->
            ResourceFaq(
                question = faq.optString("question"),
                answer = faq.optString("answer")
            )
        }
        val offerings = root.optJSONArray("offerings").objects().map(::parseOffering)

        return InstitutionalResource(
            id = id, type = type, category = root.optString("category").trim(), name = name,
            shortDescription = root.optString("shortDescription").ifEmpty { root.optString("summary") },
            description = root.optString("description"),
            aliases = root.optJSONArray("aliases").strings(), keywords = root.optJSONArray("keywords").strings(),
            audience = (root.optJSONArray("audience") ?: root.optJSONArray("targetAudience")).strings(),
            requirements = (root.optJSONArray("requirements") ?: root.optJSONArray("accessRequirements") ?: recommendedChannel?.optJSONArray("requirements")).strings(),
            documents = root.optJSONArray("documents").documentNames(), steps = steps.sortedBy { it.order },
            location = resolvedLocation,
            offerings = offerings,
            schedule = root.displayText("schedule")
                ?: recommendedChannel?.optJSONObject("estimatedTime")?.optionalString("displayText")
                ?: offerings.flatMap { it.schedule }.distinct().takeIf { it.isNotEmpty() }?.joinToString(" | "),
            cost = root.displayText("cost"),
            notes = root.optJSONArray("notes").strings(),
            sources = root.optJSONArray("sources").objects().map {
                val title = it.optString("title", it.optString("name")).trim()
                require(title.isNotEmpty()) { "sources.title está vacío" }
                ResourceSource(title, it.optionalString("url"))
            },
            lastReviewed = root.optionalString("lastReviewed") ?: root.optionalString("lastUpdated"),
            channels = channels,
            contact = root.optJSONObject("contact")?.let(::parseContact),
            actions = root.optJSONArray("actions").objects().map(::parseAction),
            availability = root.optJSONObject("availability")?.let { availability ->
                ResourceAvailability(
                    active = if (availability.has("activeCall") && !availability.isNull("activeCall")) availability.optBoolean("activeCall") else null,
                    displayText = availability.optString("displayText"),
                    notes = availability.optJSONArray("notes").strings()
                )
            },
            applicationProcess = root.optJSONObject("applicationProcess")?.let { process ->
                val platform = process.optJSONObject("platform")
                ResourceApplicationProcess(
                    platformName = platform?.optString("name").orEmpty(),
                    platformUrl = platform?.optionalString("url"),
                    steps = parseSteps(process.optJSONArray("steps")),
                    recommendations = process.optJSONArray("recommendations").strings()
                )
            },
            scholarshipTypes = root.optJSONArray("scholarshipTypes").objects().map { scholarship ->
                val directReqs = scholarship.optJSONArray("requirements").strings()
                val legacyReqs = scholarship.optJSONArray("referenceRequirements").objects().map { it.optString("description") }
                ResourceScholarshipType(
                    id = scholarship.optString("id"),
                    name = scholarship.optString("name"),
                    requirements = directReqs.ifEmpty { legacyReqs },
                    requirementsStatus = scholarship.optionalString("requirementsStatus")
                )
            },
            resourceDocuments = root.optJSONArray("documents").documents(),
            archetype = effectiveArchetype,
            period = period,
            serviceRules = serviceRules,
            subServices = subServices,
            faqs = faqs
        )
    }

    private fun parseOffering(item: JSONObject): ResourceOffering {
        val contacts = item.optJSONArray("contacts").objects().map(::parseContact)
        val directSchedule = parseOfferingSchedule(item.optJSONArray("schedule"))
        val contactSchedules = contacts.mapNotNull { it.schedule }
        val effectiveSchedule = directSchedule.ifEmpty { contactSchedules }
        return ResourceOffering(
            id = item.optString("id"),
            type = item.optString("type"),
            name = item.optString("name"),
            description = item.optString("description", item.optString("shortDescription")),
            instructor = item.optionalString("instructor")?.let { instructor ->
                listOfNotNull(item.optionalString("instructorRole"), instructor).joinToString(" ")
            },
            variants = item.optJSONArray("variants").strings(),
            divisions = item.optJSONArray("divisions").strings(),
            topics = item.optJSONArray("topics").strings(),
            schedule = effectiveSchedule,
            contacts = contacts,
            location = item.optJSONObject("location")?.let { location ->
                ResourceLocation(location.optionalString("mapLocationId"), location.optString("displayName"))
            },
            cost = item.displayText("cost"),
            confidential = if (item.has("confidential") && !item.isNull("confidential")) item.optBoolean("confidential") else null,
            documents = item.optJSONArray("documents").documents(),
            actions = item.optJSONArray("actions").objects().map(::parseAction),
            searchTargets = item.optJSONArray("searchTargets").strings(),
            url = item.optionalString("url")
        )
    }

    private fun parseContact(item: JSONObject): ResourceContact = ResourceContact(
        name = listOfNotNull(item.optionalString("title"), item.optionalString("name")).joinToString(" "),
        role = item.optionalString("role"),
        phone = item.optionalString("phone"),
        extension = item.optionalString("extension"),
        email = item.optionalString("email"),
        shift = item.optionalString("shift")?.toReadableShift(),
        schedule = item.optJSONObject("schedule")?.let(::formatSchedule)
    )

    private fun parseAction(item: JSONObject) = ResourceAction(
        id = item.optString("id"), name = item.optString("name"),
        type = item.optString("type"), url = item.optString("url")
    )

    private fun parseOfferingSchedule(scheduleArray: JSONArray?): List<String> {
        if (scheduleArray == null) return emptyList()
        return buildList {
            for (i in 0 until scheduleArray.length()) {
                when (val item = scheduleArray.opt(i)) {
                    is String -> if (item.isNotBlank()) add(item.trim())
                    is JSONObject -> add(formatSchedule(item))
                }
            }
        }
    }

    private fun formatSchedule(item: JSONObject): String {
        item.optionalString("displayText")?.let { return it }
        val rawDays = item.optJSONArray("days").strings()
        val day = when {
            item.has("day") -> item.optString("day").toReadableDay()
            rawDays.isNotEmpty() -> formatDaysList(rawDays)
            else -> ""
        }
        val start = item.optionalString("start")
        val end = item.optionalString("end")
        val times = listOfNotNull(start, end).joinToString(" a ")
        return if (day.isNotBlank() && times.isNotBlank()) "$day de $times hrs" else day.ifBlank { times }
    }

    private fun formatDaysList(days: List<String>): String {
        val normalized = days.map { it.toReadableDay() }
        if (normalized.size == 5 && normalized.containsAll(listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes"))) {
            return "Lunes a Viernes"
        }
        return normalized.joinToString(", ")
    }

    private fun String.toReadableDay(): String = when (trim().uppercase()) {
        "LUNES" -> "Lunes"
        "MARTES" -> "Martes"
        "MIERCOLES", "MIÉRCOLES" -> "Miércoles"
        "JUEVES" -> "Jueves"
        "VIERNES" -> "Viernes"
        "SABADO", "SÁBADO" -> "Sábado"
        "DOMINGO" -> "Domingo"
        else -> lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }

    private fun String.toReadableShift(): String = when (trim().uppercase()) {
        "MATUTINO" -> "Turno matutino"
        "VESPERTINO" -> "Turno vespertino"
        "MIXTO" -> "Turno mixto"
        else -> lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }

    private fun parseChannel(item: JSONObject): ResourceChannel {
        val id = item.optString("id").trim()
        val name = item.optString("name").trim()
        require(id.isNotEmpty() && name.isNotEmpty()) { "channels requiere id y name" }
        val steps = parseSteps(item.optJSONArray("steps"))
        val trackingObject = item.optJSONObject("tracking")
        return ResourceChannel(
            id = id,
            type = item.optString("type").trim(),
            name = name,
            recommended = item.optBoolean("recommended", false),
            url = item.optionalString("url"),
            requirements = item.optJSONArray("requirements").strings(),
            documents = (item.optJSONArray("requiredDocuments") ?: item.optJSONArray("documents")).documents(),
            estimatedTime = item.optJSONObject("estimatedTime")?.let { time ->
                ResourceEstimatedTime(
                    type = time.optString("type"),
                    displayText = time.optString("displayText"),
                    official = time.optBoolean("official", false),
                    basis = time.optionalString("basis")
                )
            },
            steps = steps,
            tracking = trackingObject?.let { tracking ->
                ResourceTracking(
                    available = tracking.optBoolean("available", false),
                    steps = parseSteps(tracking.optJSONArray("steps")),
                    notes = tracking.optJSONArray("notes").strings()
                )
            },
            notes = item.optJSONArray("notes").strings(),
            variant = item.optionalString("variant"),
            paymentDetails = item.optJSONObject("payment")?.let(::parsePayment).orEmpty()
        )
    }

    private fun parsePayment(payment: JSONObject): List<String> = buildList {
        payment.optionalString("displayText")?.let { add("Importe: $it") }
        payment.optionalString("method")?.let { add("Método: ${it.readableLabel()}") }
        payment.optionalString("bank")?.let { add("Banco: $it") }
        payment.optionalString("beneficiary")?.let { add("Beneficiario: $it") }
        payment.optionalString("agreement")?.let { add("Convenio: $it") }
        payment.optionalString("displayClabe")?.let { add("CLABE: $it") }
        payment.optionalString("reference")?.let { add("Referencia: $it") }
        payment.optJSONObject("validity")?.optionalString("displayText")?.let { add(it) }
        addAll(payment.optJSONArray("notes").strings())
    }

    private fun parseSteps(array: JSONArray?): List<ResourceStep> = array.objects().map { step ->
        val description = step.optString("description").trim()
        val title = step.optString("title").trim().ifEmpty { description }
        ResourceStep(step.optInt("order", 0), title, if (title == description) "" else description)
    }.also { steps ->
        require(steps.all { it.order > 0 && it.title.isNotEmpty() }) { "steps requiere order > 0 y contenido" }
        require(steps.map { it.order }.distinct().size == steps.size) { "steps contiene órdenes duplicados" }
    }.sortedBy { it.order }
}

private fun JSONObject.optionalString(key: String) = optString(key).trim().ifEmpty { null }
private fun JSONObject.displayText(key: String): String? = when (val value = opt(key)) {
    null, JSONObject.NULL -> null
    is String -> value.trim().ifEmpty { null }
    is JSONObject -> value.optString("displayText").trim().ifEmpty { null }
    else -> value.toString()
}
private fun String.readableLabel() = lowercase().replace('_', ' ').replaceFirstChar(Char::uppercase)
private fun JSONArray?.strings(): List<String> = if (this == null) emptyList() else buildList {
    for (i in 0 until length()) add(getString(i).trim()).also { require(last().isNotEmpty()) { "lista contiene un valor vacío" } }
}
private fun JSONArray?.objects(): List<JSONObject> = if (this == null) emptyList() else buildList {
    for (i in 0 until length()) add(getJSONObject(i))
}
private fun JSONArray?.documentNames(): List<String> = if (this == null) emptyList() else buildList {
    for (i in 0 until length()) {
        val value = opt(i)
        val displayText = when (value) {
            is String -> value.trim()
            is JSONObject -> value.optString("name").trim().let { name ->
                val description = value.optString("description").trim()
                if (description.isEmpty()) name else "$name — $description"
            }
            else -> ""
        }
        require(displayText.isNotEmpty()) { "documents contiene un valor inválido" }
        add(displayText)
    }
}
private fun JSONArray?.documents(): List<ResourceDocument> = if (this == null) emptyList() else buildList {
    for (i in 0 until length()) {
        when (val value = opt(i)) {
            is String -> add(ResourceDocument(name = value))
            is JSONObject -> {
                val name = value.optString("name").trim()
                require(name.isNotEmpty()) { "document requiere name" }
                add(ResourceDocument(
                    id = value.optString("id").ifBlank { null }, name = name,
                    required = value.optBoolean("required", false), description = value.optString("description"),
                    type = value.optString("type").ifBlank { null }, url = value.optString("url").ifBlank { null }
                ))
            }
            else -> error("document inválido")
        }
    }
}

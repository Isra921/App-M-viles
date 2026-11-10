package com.escom.escomorienta.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.escom.escomorienta.data.institutional.InstitutionalLocalDataSource
import com.escom.escomorienta.data.institutional.LocalInstitutionalRepository
import com.escom.escomorienta.domain.institutional.*
import com.escom.escomorienta.navigation.Routes
import com.escom.escomorienta.ui.institutional.*
import com.escom.escomorienta.data.map.MapRepository
import com.escom.escomorienta.domain.institutional.InstitutionalMapActionValidator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProceduresScreen(navController: NavHostController) {
    val vm: InstitutionalViewModel = viewModel(factory = InstitutionalViewModelFactory(LocalContext.current))
    val state by vm.state.collectAsState()
    val query by vm.query.collectAsState()
    val category by vm.category.collectAsState()
    val archetype by vm.archetype.collectAsState()
    Scaffold(bottomBar = { EscomBottomNavigationBar(navController) }, topBar = { TopAppBar(title = { Text("Trámites y servicios", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold) }, navigationIcon = {
        IconButton(onClick = navController::navigateUp) { Icon(Icons.Filled.ArrowBack, "Volver") }
    }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = vm::setQuery,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                placeholder = { Text("Buscar trámites, servicios o actividades") },
                leadingIcon = { Icon(Icons.Filled.Search, null, Modifier.size(20.dp)) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ProcedureCategoryChip("Todos", archetype == null) { vm.setArchetype(null) }
                ProcedureCategoryChip("Trámites", archetype == InstitutionalArchetype.PROCEDURE) { vm.setArchetype(InstitutionalArchetype.PROCEDURE) }
                ProcedureCategoryChip("Servicios", archetype == InstitutionalArchetype.FACILITY_SERVICE) { vm.setArchetype(InstitutionalArchetype.FACILITY_SERVICE) }
                ProcedureCategoryChip("Deportes y Cultura", archetype == InstitutionalArchetype.WORKSHOP_ACTIVITY) { vm.setArchetype(InstitutionalArchetype.WORKSHOP_ACTIVITY) }
                ProcedureCategoryChip("Becas", archetype == InstitutionalArchetype.SCHOLARSHIP) { vm.setArchetype(InstitutionalArchetype.SCHOLARSHIP) }
            }
            Spacer(Modifier.height(6.dp))
            val categories = (state as? InstitutionalUiState.Content)?.categories.orEmpty()
            if (categories.size > 1) {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = category == null,
                        onClick = { vm.setCategory(null) },
                        label = { Text("Todas las áreas", style = MaterialTheme.typography.labelSmall) }
                    )
                    categories.forEach { item ->
                        FilterChip(
                            selected = category == item,
                            onClick = { vm.setCategory(item) },
                            label = { Text(item.toCategoryLabel(), style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
            }
            when (val current = state) {
                InstitutionalUiState.Loading -> Box(Modifier.fillMaxSize()) { CircularProgressIndicator() }
                is InstitutionalUiState.Error -> Column { Text(current.message); TextButton(onClick = vm::refresh) { Text("Reintentar") } }
                is InstitutionalUiState.Empty -> Text(if (current.searchActive) "No se encontraron resultados para la búsqueda" else "No existen recursos cargados")
                is InstitutionalUiState.Content -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
                    items(current.resources, key = { it.id }) { resource ->
                        Surface(
                            onClick = { navController.navigate(Routes.procedureDetail(resource.id)) },
                            modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp)),
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Row(
                                Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    when (resource.archetype) {
                                        InstitutionalArchetype.PROCEDURE -> Icons.Filled.Description
                                        InstitutionalArchetype.FACILITY_SERVICE -> Icons.Filled.Place
                                        InstitutionalArchetype.WORKSHOP_ACTIVITY -> Icons.Filled.Description
                                        InstitutionalArchetype.SCHOLARSHIP -> Icons.Filled.Description
                                    },
                                    null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(resource.name, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                resource.archetype.toArchetypeLabel(),
                                                Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                        Text(resource.category.toCategoryLabel(), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
                                    }
                                    if (resource.shortDescription.isNotBlank()) {
                                        Text(
                                            resource.shortDescription,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 3,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                Icon(Icons.Filled.ChevronRight, "Abrir", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProcedureCategoryChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = MaterialTheme.colorScheme.outlineVariant,
            selectedBorderColor = MaterialTheme.colorScheme.primary
        )
    )
}

private fun InstitutionalArchetype.toArchetypeLabel(): String = when (this) {
    InstitutionalArchetype.PROCEDURE -> "Trámite"
    InstitutionalArchetype.FACILITY_SERVICE -> "Servicio"
    InstitutionalArchetype.WORKSHOP_ACTIVITY -> "Actividad"
    InstitutionalArchetype.SCHOLARSHIP -> "Beca"
}

private fun String.toCategoryLabel(): String = when (this) {
    "GESTION_ESCOLAR" -> "Gestión escolar"
    "SERVICIOS_ESCOLARES" -> "Servicios escolares"
    "SERVICIOS_ESTUDIANTILES" -> "Servicios estudiantiles"
    "CULTURA" -> "Cultura"
    "DEPORTE" -> "Deporte"
    "SALUD" -> "Salud"
    "BECAS" -> "Becas"
    "BIBLIOTECA" -> "Biblioteca"
    else -> lowercase().replace('_', ' ').trim().replaceFirstChar(Char::uppercase)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProcedureDetailScreen(navController: NavHostController, resourceId: String) {
    val repository = remember { LocalInstitutionalRepository(InstitutionalLocalDataSource.from(navController.context)) }
    val mapActions = remember { InstitutionalMapActionValidator(MapRepository(navController.context)) }
    var resource by remember(resourceId) { mutableStateOf<InstitutionalResource?>(null) }
    var loaded by remember(resourceId) { mutableStateOf(false) }

    LaunchedEffect(resourceId) {
        resource = repository.getById(resourceId)
        loaded = true
    }

    Scaffold(
        bottomBar = { EscomBottomNavigationBar(navController) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        resource?.name ?: "Detalle",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = navController::navigateUp) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        if (!loaded) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            resource?.let { item ->
                ResourceDetail(
                    item = item,
                    modifier = Modifier.padding(padding),
                    validMapLocation = mapActions::isValid,
                    onMap = { id -> navController.navigate(Routes.mapLocation(id)) }
                )
            } ?: Box(Modifier.fillMaxSize().padding(padding).padding(24.dp), contentAlignment = Alignment.Center) {
                Text("No se encontró la información del trámite o servicio.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ResourceDetail(
    item: InstitutionalResource,
    modifier: Modifier,
    validMapLocation: (String?) -> Boolean,
    onMap: (String) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- 1. Header con Badges ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        item.archetype.toArchetypeIcon(),
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        item.archetype.toArchetypeLabel(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    item.category.toCategoryLabel(),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // --- 2. Título y Descripción ---
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                item.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            val fullDesc = item.description.ifBlank { item.shortDescription }
            if (fullDesc.isNotBlank()) {
                Text(
                    fullDesc,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 22.sp
                )
            }
        }

        // --- 3. Tarjeta de Información Rápida (Horario, Costo, Ubicación) ---
        val hasSchedule = !item.schedule.isNullOrBlank()
        val hasCost = !item.cost.isNullOrBlank()
        val hasLocation = item.location != null
        val hasAvailability = item.availability != null

        if (hasSchedule || hasCost || hasLocation || hasAvailability) {
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.outlinedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (hasSchedule) {
                        QuickInfoRow(
                            icon = Icons.Filled.AccessTime,
                            title = "Horario de atención",
                            value = item.schedule.orEmpty()
                        )
                    }

                    if (hasCost) {
                        QuickInfoRow(
                            icon = Icons.Filled.Payments,
                            title = "Costo",
                            value = item.cost.orEmpty()
                        )
                    }

                    item.location?.let { location ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                Icons.Filled.Place,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp).padding(top = 2.dp)
                            )
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    "Ubicación",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    location.displayName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                location.mapLocationId?.takeIf(validMapLocation)?.let { id ->
                                    FilledTonalButton(
                                        onClick = { onMap(id) },
                                        modifier = Modifier.padding(top = 4.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Filled.Place, null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Ver en mapa", style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                        }
                    }

                    item.availability?.let { availability ->
                        QuickInfoRow(
                            icon = Icons.Filled.EventAvailable,
                            title = "Disponibilidad",
                            value = availability.displayText
                        )
                        if (availability.notes.isNotEmpty()) {
                            Column(Modifier.padding(start = 32.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                availability.notes.forEach { note ->
                                    Text("• $note", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- 4. Periodo y Convocatoria ---
        item.period?.let { period ->
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.outlinedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionHeader(Icons.Filled.CalendarMonth, "Periodo de solicitud")
                    Text(period.description, style = MaterialTheme.typography.bodyMedium)
                    period.warning?.let { warning ->
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    Icons.Filled.WarningAmber,
                                    null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.size(18.dp).padding(top = 2.dp)
                                )
                                Text(
                                    warning,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodySmall,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- 5. Requisitos y Documentación ---
        if (item.requirements.isNotEmpty() || item.documents.isNotEmpty()) {
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionHeader(Icons.Filled.FactCheck, "Requisitos y documentos requeridos")
                    if (item.requirements.isNotEmpty()) {
                        Text("Requisitos:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                        item.requirements.forEach { req ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    Icons.Filled.CheckCircle,
                                    null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp).padding(top = 2.dp)
                                )
                                Text(req, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                    if (item.documents.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        Text("Documentación solicitada:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                        item.documents.forEach { doc ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    Icons.Filled.Description,
                                    null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(16.dp).padding(top = 2.dp)
                                )
                                Text(doc, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }

        // --- 6. Modalidades y Canales de Solicitud ---
        if (item.channels.isNotEmpty()) {
            SectionHeader(Icons.Filled.AltRoute, "¿Cómo solicitarlo?")
            item.channels.forEach { channel ->
                ChannelCard(channel, item.location?.mapLocationId?.takeIf(validMapLocation), onMap)
            }
        } else if (item.steps.isNotEmpty()) {
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionHeader(Icons.Filled.FormatListNumbered, "Pasos para realizar el trámite")
                    item.steps.forEach { step ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        "${step.order}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(step.title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                                if (step.description.isNotBlank()) {
                                    Text(step.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- 7. Proceso de Solicitud (Plataformas externas como SIBec) ---
        item.applicationProcess?.let { ApplicationProcessBlock(it) }

        // --- 8. Tipos de Beca (si aplica) ---
        if (item.scholarshipTypes.isNotEmpty()) {
            ScholarshipTypesBlock(item.scholarshipTypes)
        }

        // --- 9. Reglamento y Condiciones de Servicio ---
        if (item.serviceRules.isNotEmpty()) {
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionHeader(Icons.Filled.Rule, "Reglamento y condiciones de uso")
                    item.serviceRules.forEach { rule ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                            Text("•", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text(rule.description, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }

        // --- 10. Subservicios (Cubículos, Salas de lectura, etc.) ---
        if (item.subServices.isNotEmpty()) {
            SectionHeader(Icons.Filled.MeetingRoom, "Áreas y espacios disponibles")
            item.subServices.forEach { sub ->
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(sub.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                        if (sub.description.isNotBlank()) {
                            Text(sub.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        sub.capacity?.let {
                            Text("Capacidad: $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        }
                        sub.rules?.let {
                            Text("Reglamento: $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        // --- 11. Ofertas y Servicios Especializados (Servicio Médico, Odontología, etc.) ---
        if (item.offerings.isNotEmpty()) {
            SectionHeader(Icons.Filled.MedicalServices, "Servicios y áreas de atención")
            item.offerings.forEach { OfferingCard(it, validMapLocation, onMap) }
        }

        // --- 12. Documentos y Formatos Generales del Recurso ---
        ResourceDocumentsBlock(item.resourceDocuments)

        // --- 13. Acciones Generales del Recurso ---
        ResourceActionsBlock(item.actions)

        // --- 14. Contacto Institucional General ---
        item.contact?.let { contact ->
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionHeader(Icons.Filled.ContactPhone, "Contacto institucional")
                    ContactBlock(contact)
                }
            }
        }

        // --- 15. Notas Importantes ---
        if (item.notes.isNotEmpty()) {
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.outlinedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                )
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SectionHeader(Icons.Filled.Info, "Notas importantes")
                    item.notes.forEach { note ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                            Text("•", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text(note, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }

        // --- 16. Preguntas Frecuentes ---
        if (item.faqs.isNotEmpty()) {
            SectionHeader(Icons.Filled.HelpOutline, "Preguntas frecuentes")
            item.faqs.forEach { faq ->
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            faq.question,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            faq.answer,
                            style = MaterialTheme.typography.bodyMedium,
                            lineHeight = 20.sp
                        )
                    }
                }
            }
        }

        // --- 17. Fuentes y Actualización ---
        if (item.sources.isNotEmpty() || !item.lastReviewed.isNullOrBlank()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (item.sources.isNotEmpty()) {
                    Text(
                        "Fuentes de información:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    item.sources.forEach { source ->
                        Text(
                            "• ${source.title}${source.url?.let { " ($it)" } ?: ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                item.lastReviewed?.let { date ->
                    Text(
                        "Última revisión: $date",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickInfoRow(icon: ImageVector, title: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp).padding(top = 2.dp)
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun SectionHeader(icon: ImageVector, title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun ChannelCard(
    channel: ResourceChannel,
    mapLocationId: String?,
    onMap: (String) -> Unit
) {
    var expanded by remember(channel.id, channel.variant) { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current

    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    when (channel.type) {
                        "IN_PERSON" -> Icons.Filled.Person
                        "ONLINE_PLATFORM" -> Icons.Filled.Language
                        "WEB_FORM" -> Icons.Filled.Assignment
                        else -> Icons.Filled.HelpOutline
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    channel.name,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (channel.recommended) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            "Recomendado",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            channel.variant?.let {
                Text(
                    it.lowercase().replaceFirstChar(Char::uppercase),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium
                )
            }

            Text(
                channel.summary(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { expanded = !expanded },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(if (expanded) "Ocultar detalles" else channel.actionLabel(), style = MaterialTheme.typography.labelMedium)
                }
                if (channel.type == "IN_PERSON" && mapLocationId != null) {
                    FilledTonalButton(
                        onClick = { onMap(mapLocationId) },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Filled.Place, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Ubicación", style = MaterialTheme.typography.labelMedium)
                    }
                }
                channel.url?.let { url ->
                    Button(
                        onClick = { uriHandler.openUri(url) },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Abrir", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            if (expanded) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                if (channel.requirements.isNotEmpty()) {
                    Text("Requisitos específicos:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                    channel.requirements.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
                }

                if (channel.documents.isNotEmpty()) {
                    Text("Documentos necesarios:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                    channel.documents.forEach { doc ->
                        Text(
                            "• ${doc.name}${if (doc.required) " (obligatorio)" else ""}${if (doc.description.isNotBlank()) " — ${doc.description}" else ""}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                channel.estimatedTime?.displayText?.let { time ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.AccessTime, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                        Text("Tiempo estimado: $time", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    }
                }

                if (channel.steps.isNotEmpty()) {
                    Text("Pasos a seguir:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                    channel.steps.forEach { step ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier.size(20.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("${step.order}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                }
                            }
                            Text(step.title, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                if (channel.notes.isNotEmpty()) {
                    Text("Notas:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                    channel.notes.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
                }
            }
        }
    }
}

@Composable
private fun ApplicationProcessBlock(process: ResourceApplicationProcess) {
    val uriHandler = LocalUriHandler.current
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionHeader(Icons.Filled.AssignmentInd, "Proceso de solicitud")
            if (process.platformName.isNotBlank()) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Plataforma:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    Text(process.platformName, style = MaterialTheme.typography.bodyMedium)
                }
            }
            if (process.steps.isNotEmpty()) {
                Text("Pasos del proceso:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                process.steps.forEach { step ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(22.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("${step.order}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        }
                        Text(step.title, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            if (process.recommendations.isNotEmpty()) {
                Text("Recomendaciones:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                process.recommendations.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
            }
            process.platformUrl?.let { url ->
                Button(
                    onClick = { uriHandler.openUri(url) },
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Abrir plataforma")
                }
            }
        }
    }
}

@Composable
private fun ScholarshipTypesBlock(types: List<ResourceScholarshipType>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader(Icons.Filled.School, "Tipos de beca disponibles")
        types.forEach { type ->
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(type.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    if (type.requirements.isNotEmpty()) {
                        Text("Requisitos principales:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                        type.requirements.forEach { req ->
                            Text("• $req", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    type.requirementsStatus?.let { status ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                status.lowercase().replace('_', ' ').replaceFirstChar(Char::uppercase),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OfferingCard(
    offering: ResourceOffering,
    validMapLocation: (String?) -> Boolean,
    onMap: (String) -> Unit
) {
    var expanded by remember(offering.id) { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current

    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    offering.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                offering.cost?.let { costText ->
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            costText,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            if (offering.description.isNotBlank()) {
                Text(
                    offering.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Horarios de la oferta (Directos o de contactos)
            if (offering.schedule.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.AccessTime,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                "Horarios de atención",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        offering.schedule.forEach { sch ->
                            Text(
                                sch,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Ubicación si existe
            offering.location?.let { location ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Place, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    Text(
                        location.displayName,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                    location.mapLocationId?.takeIf(validMapLocation)?.let { id ->
                        TextButton(
                            onClick = { onMap(id) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("Ver mapa", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            // Botón expandir detalles
            val hasExtraDetails = offering.contacts.isNotEmpty() || offering.documents.isNotEmpty() ||
                    offering.actions.isNotEmpty() || offering.topics.isNotEmpty() || offering.url != null

            if (hasExtraDetails) {
                OutlinedButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    Text(if (expanded) "Ocultar personal y detalles" else "Ver personal y detalles")
                }
            }

            if (expanded) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                // Responsable / Instructor
                offering.instructor?.takeIf { it.isNotBlank() }?.let {
                    Text("Responsable: $it", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                }

                // Temas abordados
                if (offering.topics.isNotEmpty()) {
                    Text("Temas de atención:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                    Text(offering.topics.joinToString(", "), style = MaterialTheme.typography.bodySmall)
                }

                // Personal de contacto
                if (offering.contacts.isNotEmpty()) {
                    Text("Personal de atención:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                    offering.contacts.forEach { contact ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                ContactBlock(contact)
                            }
                        }
                    }
                }

                // Documentos descargables
                if (offering.documents.isNotEmpty()) {
                    Text("Documentos informativos:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                    offering.documents.forEach { doc ->
                        if (doc.url != null) {
                            FilledTonalButton(
                                onClick = { uriHandler.openUri(doc.url) },
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.OpenInNew, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(doc.name, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        } else {
                            Text("• ${doc.name}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                // Acciones y formularios
                if (offering.actions.isNotEmpty()) {
                    offering.actions.forEach { action ->
                        Button(
                            onClick = { uriHandler.openUri(action.url) },
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.OpenInNew, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(action.name, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }

                offering.url?.let { url ->
                    TextButton(onClick = { uriHandler.openUri(url) }) {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Abrir enlace oficial")
                    }
                }
            }
        }
    }
}

@Composable
private fun ContactBlock(contact: ResourceContact) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Person, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            Text(
                contact.name.ifBlank { "Personal encargado" },
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium
            )
            contact.shift?.let { shiftText ->
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        shiftText,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        contact.role?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        contact.phone?.let { phone ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Phone, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "Teléfono: $phone${contact.extension?.let { " ext. $it" } ?: ""}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        contact.email?.let { email ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Email, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Correo: $email", style = MaterialTheme.typography.bodySmall)
            }
        }

        contact.schedule?.let { schedule ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.AccessTime, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                Text("Horario: $schedule", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun ResourceDocumentsBlock(documents: List<ResourceDocument>) {
    if (documents.isEmpty()) return
    val uriHandler = LocalUriHandler.current
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionHeader(Icons.Filled.AttachFile, "Documentos y formatos")
            documents.forEach { document ->
                if (document.url != null) {
                    FilledTonalButton(
                        onClick = { uriHandler.openUri(document.url) },
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(document.name, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                } else {
                    Text("• ${document.name}${if (document.required) " (obligatorio)" else ""}", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun ResourceActionsBlock(actions: List<ResourceAction>) {
    if (actions.isEmpty()) return
    val uriHandler = LocalUriHandler.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        actions.forEach { action ->
            Button(
                onClick = { uriHandler.openUri(action.url) },
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.OpenInNew, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(action.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            }
        }
    }
}

private fun InstitutionalArchetype.toArchetypeIcon(): ImageVector = when (this) {
    InstitutionalArchetype.PROCEDURE -> Icons.Filled.Description
    InstitutionalArchetype.FACILITY_SERVICE -> Icons.Filled.MedicalServices
    InstitutionalArchetype.WORKSHOP_ACTIVITY -> Icons.Filled.SportsBasketball
    InstitutionalArchetype.SCHOLARSHIP -> Icons.Filled.School
}

private fun ResourceChannel.summary(): String = when {
    estimatedTime?.displayText?.isNotBlank() == true -> estimatedTime.displayText
    type == "IN_PERSON" -> "Solicitud presencial en ventanilla o módulo."
    type == "ONLINE_PLATFORM" -> "Solicitud mediante plataforma institucional."
    type == "WEB_FORM" -> "Solicitud mediante formulario web."
    else -> "Modalidad disponible."
}

private fun ResourceChannel.actionLabel(): String = when (type) {
    "IN_PERSON" -> "Ver requisitos y pasos"
    else -> "Ver pasos a seguir"
}

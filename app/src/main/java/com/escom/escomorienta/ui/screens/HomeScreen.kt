package com.escom.escomorienta.ui.screens

import android.net.Uri
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.escom.escomorienta.R
import com.escom.escomorienta.data.schedule.ClassSession
import com.escom.escomorienta.data.schedule.LocalSettings
import com.escom.escomorienta.data.schedule.ScheduleRepository
import com.escom.escomorienta.navigation.Routes
import com.escom.escomorienta.navigation.bottomNavItems
import com.escom.escomorienta.ui.theme.EscomAppTheme
import java.util.Calendar

@Composable
fun HomeScreen(
    navController: NavHostController
) {
    val context = LocalContext.current
    val isPreview = LocalInspectionMode.current
    val repository = remember(context, isPreview) {
        if (isPreview) null else ScheduleRepository.getInstance(context.applicationContext)
    }
    val classes = if (repository == null) {
        emptyList()
    } else {
        val observedClasses by repository.observeAllClasses().collectAsState(initial = emptyList())
        observedClasses
    }
    val settings = if (repository == null) {
        LocalSettings()
    } else {
        val observedSettings by repository.observeLocalSettings().collectAsState(initial = LocalSettings())
        observedSettings
    }
    val currentOrNextClass = remember(classes) {
        classes.findCurrentOrNextClass()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            EscomBottomNavigationBar(navController = navController)
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                HomeHeader(
                    alias = settings.userAlias,
                    onSettingsClick = {
                        navController.navigate(Routes.SETTINGS)
                    }
                )
            }

            item {
                CurrentScheduleCard(
                    classSession = currentOrNextClass,
                    onViewMapClick = {
                        navController.navigateToBottomNavRoute(
                            currentOrNextClass?.classroomName.toMapRoute()
                        )
                    },
                    onScheduleClick = {
                        navController.navigateToBottomNavRoute(Routes.SCHEDULE)
                    }
                )
            }

            item {
                Text(
                    text = "Accesos rápidos",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                    fontSize = 19.sp
                )
            }

            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MainMenuCard(
                        title = "Mapa de ESCOM",
                        description = "Plano detallado de la escuela",
                        icon = Icons.Filled.Place,
                        onClick = { navController.navigateToBottomNavRoute(Routes.MAP) }
                    )

                    MainMenuCard(
                        title = "Mi Horario",
                        description = "Tus clases y salones",
                        icon = Icons.Filled.Schedule,
                        onClick = { navController.navigateToBottomNavRoute(Routes.SCHEDULE) }
                    )

                    MainMenuCard(
                        title = "Trámites y servicios",
                        description = "Requisitos y procesos escolares",
                        icon = Icons.Filled.Description,
                        onClick = { navController.navigateToBottomNavRoute(Routes.PROCEDURES) }
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeHeader(
    alias: String,
    onSettingsClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_escom_orienta),
                contentDescription = "ESCOM Orienta",
                modifier = Modifier.size(72.dp),
                contentScale = ContentScale.Fit
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 14.dp, end = 4.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "¡Bienvenido, ${alias.ifBlank { "Alumno" }}!",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                    fontSize = 21.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 25.sp
                )
                Text(
                    text = "ESCOM Orienta",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = "Configuración",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
        HorizontalDivider(
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
            thickness = 1.dp
        )
    }
}

@Composable
private fun CurrentScheduleCard(
    classSession: ClassSession?,
    onViewMapClick: () -> Unit,
    onScheduleClick: () -> Unit
) {
    val timingInfo = classSession?.toTimingInfo()

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)),
        onClick = onScheduleClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (classSession == null || timingInfo == null) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Sin horario",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )

                    Text(
                        text = "Carga tu horario",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )

                    Text(
                        text = "Agrega tus materias para ver aquí tu próxima clase.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp,
                        lineHeight = 18.sp
                    )
                }

                return@Column
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = timingInfo.title,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.AccessTime,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = timingInfo.label,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 6.dp),
                            maxLines = 2
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .width(3.dp)
                                .height(30.dp)
                                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp))
                        )
                        Column(Modifier.padding(start = 8.dp)) {
                            Text(
                                text = classSession.dayOfWeek.toShortDayLabel(),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${classSession.startTime} - ${classSession.endTime}",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onViewMapClick,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Map,
                        contentDescription = "Ver salón en mapa",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(25.dp)
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            ScheduleInfoPair(
                label = "Materia",
                value = classSession.subjectName,
                highlight = true
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            ScheduleInfoPair(
                label = "Profesor",
                value = classSession.teacherName?.takeIf { it.isNotBlank() } ?: "Sin profesor",
                icon = Icons.Filled.Person
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    ScheduleInfoPair(
                        label = "Grupo",
                        value = classSession.groupName ?: "Sin grupo",
                        icon = Icons.Filled.People
                    )
                }

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    ScheduleInfoPair(
                        label = "Salón",
                        value = classSession.classroomName ?: "Pendiente",
                        icon = Icons.Filled.Place
                    )
                }
            }
        }
    }
}

@Composable
private fun ScheduleInfoPair(
    label: String,
    value: String,
    icon: ImageVector? = null,
    highlight: Boolean = false
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (highlight) {
            Box(
                Modifier
                    .width(3.dp)
                    .height(38.dp)
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp))
            )
        } else if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
        Column(
            modifier = Modifier.padding(start = if (highlight || icon != null) 10.dp else 0.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
            Text(
                text = value,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = if (highlight) 15.sp else 13.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = if (highlight) 19.sp else 16.sp,
                maxLines = if (highlight) 3 else 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun MainMenuCard(
    title: String,
    description: String,
    icon: ImageVector? = null,
    iconRes: Int? = null,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp)),
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = 1.dp,
            pressedElevation = 0.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(11.dp)
            ) {
                if (iconRes != null) {
                    Icon(
                        painter = painterResource(iconRes),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.padding(9.dp).size(25.dp)
                    )
                } else if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(9.dp).size(25.dp)
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = title,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 18.sp,
                    maxLines = 2
                )
                Text(
                    text = description,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    lineHeight = 15.sp,
                    maxLines = 2
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
fun EscomBottomNavigationBar(
    navController: NavHostController
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        bottomNavItems.forEach { item ->
            val selected = currentRoute == item.route ||
                (item.route == Routes.MAP && currentRoute in listOf(Routes.MAP_WITH_QUERY, Routes.MAP_WITH_ROUTE)) ||
                (item.route == Routes.PROCEDURES && currentRoute == Routes.PROCEDURE_DETAIL)

            NavigationBarItem(
                selected = selected,
                onClick = {
                    val shouldResetMapSearch = currentRoute == Routes.MAP_WITH_QUERY && item.route == Routes.MAP
                    if (!selected || shouldResetMapSearch) {
                        navController.navigateToBottomNavRoute(item.route)
                    }
                },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        fontSize = 12.sp
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

private fun NavHostController.navigateToBottomNavRoute(route: String) {
    navigate(route) {
        launchSingleTop = true

        popUpTo(graph.findStartDestination().id) {
            saveState = false
        }
    }
}

private data class ClassTimingInfo(
    val title: String,
    val label: String
)

private fun ClassSession.toTimingInfo(
    calendar: Calendar = Calendar.getInstance()
): ClassTimingInfo {
    val today = calendar.toSchoolDayOfWeek()
    val currentMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
    val startMinutes = startTime.toMinutesOrZero()
    val endMinutes = endTime.toMinutesOrZero()

    if (today == dayOfWeek && currentMinutes in startMinutes..endMinutes) {
        val remainingMinutes = (endMinutes - currentMinutes).coerceAtLeast(0)

        return ClassTimingInfo(
            title = "Clase actual",
            label = if (remainingMinutes == 0) {
                "Termina ahora"
            } else {
                "Faltan ${remainingMinutes.toCompactDuration()} para terminar"
            }
        )
    }

    if (today == null) {
        return ClassTimingInfo(
            title = "Próxima clase",
            label = "Programada para ${dayOfWeek.toLongDayLabel()}"
        )
    }

    var dayOffset = dayDistance(today, dayOfWeek)
    if (dayOffset == 0 && currentMinutes > endMinutes) {
        dayOffset = 5
    }

    val minutesUntilStart = (dayOffset * 24 * 60 + startMinutes - currentMinutes).coerceAtLeast(0)

    return ClassTimingInfo(
        title = "Próxima clase",
        label = if (minutesUntilStart == 0) {
            "Está por comenzar"
        } else {
            "Faltan ${minutesUntilStart.toCompactDuration()} para comenzar"
        }
    )
}

private fun Int.toShortDayLabel(): String {
    return when (this) {
        1 -> "Lun"
        2 -> "Mar"
        3 -> "Mié"
        4 -> "Jue"
        5 -> "Vie"
        6 -> "Sáb"
        else -> "Horario"
    }
}

private fun Int.toLongDayLabel(): String {
    return when (this) {
        1 -> "lunes"
        2 -> "martes"
        3 -> "miércoles"
        4 -> "jueves"
        5 -> "viernes"
        6 -> "sábado"
        else -> "otro día"
    }
}

private fun Int.toCompactDuration(): String {
    val safeMinutes = coerceAtLeast(0)
    val days = safeMinutes / (24 * 60)
    val hours = (safeMinutes % (24 * 60)) / 60
    val minutes = safeMinutes % 60

    return when {
        safeMinutes < 60 -> "$safeMinutes min"
        days > 0 && hours > 0 -> "$days d $hours h"
        days > 0 -> "$days d"
        minutes > 0 -> "$hours h $minutes min"
        else -> "$hours h"
    }
}

private fun String?.toMapRoute(): String {
    val query = this?.trim()
    return if (query.isNullOrBlank()) {
        Routes.MAP
    } else {
        "${Routes.MAP}?${Routes.MAP_QUERY_ARGUMENT}=${Uri.encode(query)}"
    }
}

private fun List<ClassSession>.findCurrentOrNextClass(
    calendar: Calendar = Calendar.getInstance()
): ClassSession? {
    if (isEmpty()) return null

    val today = calendar.toSchoolDayOfWeek()
    if (today == null) {
        return minWithOrNull(
            compareBy<ClassSession> { it.dayOfWeek }
                .thenBy { it.startTime.toMinutesOrZero() }
        )
    }

    val currentMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
    val byNextOccurrence = compareBy<ClassSession> { dayDistance(today, it.dayOfWeek) }
        .thenBy { it.startTime.toMinutesOrZero() }

    return sortedWith(byNextOccurrence)
        .firstOrNull { session ->
            val distance = dayDistance(today, session.dayOfWeek)
            distance > 0 || session.endTime.toMinutesOrZero() >= currentMinutes
        }
        ?: minWithOrNull(byNextOccurrence)
}

private fun Calendar.toSchoolDayOfWeek(): Int? {
    return when (get(Calendar.DAY_OF_WEEK)) {
        Calendar.MONDAY -> 1
        Calendar.TUESDAY -> 2
        Calendar.WEDNESDAY -> 3
        Calendar.THURSDAY -> 4
        Calendar.FRIDAY -> 5
        Calendar.SATURDAY -> 6
        else -> null
    }
}

private fun dayDistance(fromDay: Int, toDay: Int): Int {
    return (toDay - fromDay + 7) % 7
}

private fun String.toMinutesOrZero(): Int {
    val parts = split(":")
    val hours = parts.getOrNull(0)?.toIntOrNull() ?: 0
    val minutes = parts.getOrNull(1)?.toIntOrNull() ?: 0
    return hours * 60 + minutes
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    val navController = rememberNavController()
    EscomAppTheme {
        HomeScreen(navController = navController)
    }
}

package com.fueltracker.app.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.fueltracker.app.model.FuelRecord
import com.fueltracker.app.ui.components.AddRecordSheet
import com.fueltracker.app.ui.viewmodel.FuelViewModel

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", "仪表盘", Icons.Default.Home)
    object History : Screen("history", "记录", Icons.Default.List)
    object Analytics : Screen("analytics", "统计", Icons.Default.Info)
    object Settings : Screen("settings", "车辆", Icons.Default.Settings)
}

@Composable
fun MainScreen(
    viewModel: FuelViewModel
) {
    var currentTab by remember { mutableStateOf<Screen>(Screen.Dashboard) }
    var showAddSheet by remember { mutableStateOf(false) }
    var editingRecord by remember { mutableStateOf<FuelRecord?>(null) }

    val vehicles by viewModel.vehicles.collectAsState()
    val selectedVehicleId by viewModel.selectedVehicleId.collectAsState()
    val records by viewModel.currentRecords.collectAsState()
    val stats by viewModel.dashboardStats.collectAsState()

    val navItems = listOf(
        Screen.Dashboard,
        Screen.History,
        Screen.Analytics,
        Screen.Settings
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = NavigationBarDefaults.Elevation
            ) {
                navItems.forEach { screen ->
                    val selected = currentTab == screen
                    NavigationBarItem(
                        selected = selected,
                        onClick = { currentTab = screen },
                        icon = { Icon(imageVector = screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF059669),
                            selectedTextColor = Color(0xFF059669),
                            indicatorColor = Color(0xFF10B981).copy(alpha = 0.15f)
                        )
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingRecord = null
                    showAddSheet = true
                },
                containerColor = Color(0xFF10B981),
                contentColor = Color.White
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "记一笔加油")
            }
        }
    ) { innerPadding ->
        val screenModifier = Modifier.padding(innerPadding)

        when (currentTab) {
            Screen.Dashboard -> {
                DashboardScreen(
                    vehicles = vehicles,
                    selectedVehicleId = selectedVehicleId,
                    stats = stats,
                    records = records,
                    onSelectVehicle = { viewModel.selectVehicle(it) },
                    onEditRecord = { record ->
                        editingRecord = record
                        showAddSheet = true
                    },
                    onDeleteRecord = { viewModel.deleteRecord(it) },
                    onNavigateToHistory = { currentTab = Screen.History },
                    onNavigateToAnalytics = { currentTab = Screen.Analytics },
                    modifier = screenModifier
                )
            }
            Screen.History -> {
                HistoryScreen(
                    records = records,
                    onEditRecord = { record ->
                        editingRecord = record
                        showAddSheet = true
                    },
                    onDeleteRecord = { viewModel.deleteRecord(it) },
                    modifier = screenModifier
                )
            }
            Screen.Analytics -> {
                AnalyticsScreen(
                    stats = stats,
                    records = records,
                    modifier = screenModifier
                )
            }
            Screen.Settings -> {
                SettingsScreen(
                    vehicles = vehicles,
                    selectedVehicleId = selectedVehicleId,
                    viewModel = viewModel,
                    modifier = screenModifier
                )
            }
        }

        if (showAddSheet) {
            AddRecordSheet(
                lastRecord = records.lastOrNull(),
                initialRecord = editingRecord,
                onDismiss = {
                    showAddSheet = false
                    editingRecord = null
                },
                onSave = { odo, vol, price, total, type, isFull, isMissed, station, note, id, timestamp ->
                    viewModel.saveRecord(
                        odometer = odo,
                        fuelAmount = vol,
                        unitPrice = price,
                        totalPrice = total,
                        fuelType = type,
                        isFull = isFull,
                        isMissedPrevious = isMissed,
                        stationName = station,
                        note = note,
                        id = id,
                        timestamp = timestamp
                    )
                    showAddSheet = false
                    editingRecord = null
                }
            )
        }
    }
}

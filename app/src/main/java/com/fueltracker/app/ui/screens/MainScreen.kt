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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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

    val vehicles by viewModel.vehicles.collectAsStateWithLifecycle()
    val selectedVehicleId by viewModel.selectedVehicleId.collectAsStateWithLifecycle()
    val records by viewModel.currentRecords.collectAsStateWithLifecycle()
    val stats by viewModel.dashboardStats.collectAsStateWithLifecycle()

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
                onClick = { showAddSheet = true },
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
                    onDeleteRecord = { viewModel.deleteRecord(it) },
                    onNavigateToHistory = { currentTab = Screen.History },
                    modifier = screenModifier
                )
            }
            Screen.History -> {
                HistoryScreen(
                    records = records,
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
                onDismiss = { showAddSheet = false },
                onSave = { odo, vol, price, total, type, isFull, isMissed, station, note ->
                    viewModel.saveRecord(odo, vol, price, total, type, isFull, isMissed, station, note)
                    showAddSheet = false
                }
            )
        }
    }
}

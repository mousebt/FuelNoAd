package com.fueltracker.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.fueltracker.app.ui.screens.MainScreen
import com.fueltracker.app.ui.theme.FuelTrackerTheme
import com.fueltracker.app.ui.viewmodel.FuelViewModel
import com.fueltracker.app.ui.viewmodel.ViewModelFactory

class MainActivity : ComponentActivity() {

    private val fuelViewModel: FuelViewModel by viewModels {
        val app = application as FuelApplication
        ViewModelFactory(app.repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FuelTrackerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(viewModel = fuelViewModel)
                }
            }
        }
    }
}

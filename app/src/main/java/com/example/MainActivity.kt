package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.ui.AppTab
import com.example.ui.AppViewModel
import com.example.ui.AppViewModelFactory
import com.example.ui.screens.DownloadsScreen
import com.example.ui.screens.FaceScanScreen
import com.example.ui.screens.GenerateScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val viewModel: AppViewModel by viewModels {
        AppViewModelFactory(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val state by viewModel.uiState.collectAsState()

            MyApplicationTheme(darkTheme = state.isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (state.isFaceScanning) {
                        // Immersive, full-screen interactive face-scan experience
                        FaceScanScreen(
                            viewModel = viewModel,
                            state = state,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        // Standard app scaffolding with elegant bottom navigation
                        Scaffold(
                            modifier = Modifier.fillMaxSize(),
                            bottomBar = {
                                BottomNavigationBar(
                                    currentTab = state.currentTab,
                                    onTabSelected = { tab -> viewModel.selectTab(tab) }
                                )
                            }
                        ) { innerPadding ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding)
                            ) {
                                when (state.currentTab) {
                                    AppTab.GENERATE -> {
                                        GenerateScreen(
                                            viewModel = viewModel,
                                            state = state,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    AppTab.DOWNLOADS -> {
                                        DownloadsScreen(
                                            viewModel = viewModel,
                                            state = state,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    AppTab.SETTINGS -> {
                                        SettingsScreen(
                                            viewModel = viewModel,
                                            state = state,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BottomNavigationBar(
    currentTab: AppTab,
    onTabSelected: (AppTab) -> Unit
) {
    NavigationBar(
        modifier = Modifier.testTag("bottom_nav_bar")
    ) {
        NavigationBarItem(
            selected = currentTab == AppTab.GENERATE,
            onClick = { onTabSelected(AppTab.GENERATE) },
            icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "Generate") },
            label = { Text("Generate", style = MaterialTheme.typography.labelMedium) },
            modifier = Modifier.testTag("tab_generate")
        )
        NavigationBarItem(
            selected = currentTab == AppTab.DOWNLOADS,
            onClick = { onTabSelected(AppTab.DOWNLOADS) },
            icon = { Icon(Icons.Default.Download, contentDescription = "Downloads") },
            label = { Text("Downloads", style = MaterialTheme.typography.labelMedium) },
            modifier = Modifier.testTag("tab_downloads")
        )
        NavigationBarItem(
            selected = currentTab == AppTab.SETTINGS,
            onClick = { onTabSelected(AppTab.SETTINGS) },
            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
            label = { Text("Settings", style = MaterialTheme.typography.labelMedium) },
            modifier = Modifier.testTag("tab_settings")
        )
    }
}

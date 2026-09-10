package com.example.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.dialogs.HeadphoneTripleSafetyDialogs
import com.example.ui.theme.AmberWarning
import com.example.ui.viewmodel.VolumeBoostViewModel

enum class MainNavigationTab {
    HOME, SETTINGS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: VolumeBoostViewModel = viewModel()
) {
    val context = LocalContext.current
    val hasSeenOnboarding by viewModel.hasSeenOnboarding.collectAsState()
    val engineState by viewModel.engineState.collectAsState()
    val masterEnabled by viewModel.masterEnabled.collectAsState()
    val globalBoostPercent by viewModel.globalBoostPercent.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val dynamicColor by viewModel.dynamicColor.collectAsState()
    val hapticsEnabled by viewModel.hapticsEnabled.collectAsState()
    val resumeOnBoot by viewModel.resumeOnBoot.collectAsState()
    val pendingHeadphoneConfirm by viewModel.pendingHeadphoneConfirm.collectAsState()
    var showDiagnostics by rememberSaveable { mutableStateOf(false) }

    var currentTab by rememberSaveable { mutableIntStateOf(MainNavigationTab.HOME.ordinal) }

    if (!hasSeenOnboarding) {
        OnboardingScreen(
            onComplete = { viewModel.completeOnboarding() }
        )
        return
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Volume Boost",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    if (engineState.isHeadphonesConnected) {
                        Surface(
                            color = AmberWarning.copy(alpha = 0.18f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.padding(end = 12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Headphones,
                                    contentDescription = null,
                                    tint = AmberWarning,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Protected",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = AmberWarning
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp,
                modifier = Modifier.testTag("bottom_nav_bar")
            ) {
                NavigationBarItem(
                    selected = currentTab == MainNavigationTab.HOME.ordinal,
                    onClick = { currentTab = MainNavigationTab.HOME.ordinal },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == MainNavigationTab.HOME.ordinal) Icons.Filled.Home else Icons.Outlined.Home,
                            contentDescription = "Home"
                        )
                    },
                    label = { Text("Home") },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )

                NavigationBarItem(
                    selected = currentTab == MainNavigationTab.SETTINGS.ordinal,
                    onClick = { currentTab = MainNavigationTab.SETTINGS.ordinal },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == MainNavigationTab.SETTINGS.ordinal) Icons.Filled.Settings else Icons.Outlined.Settings,
                            contentDescription = "Settings"
                        )
                    },
                    label = { Text("Settings") },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            Crossfade(targetState = currentTab, label = "tabCrossfade") { tabIndex ->
                when (tabIndex) {
                    MainNavigationTab.HOME.ordinal -> {
                        HomeScreen(
                            engineState = engineState,
                            masterEnabled = masterEnabled,
                            globalBoostPercent = globalBoostPercent,
                            hapticsEnabled = hapticsEnabled,
                            onToggleMaster = { viewModel.toggleMaster(it) },
                            onBoostChange = { boost -> viewModel.setGlobalBoostPercent(boost) },
                            onNavigateToDiagnostics = { showDiagnostics = true },
                            onDismissHeadphoneBanner = { viewModel.dismissHeadphoneDisconnectedBanner() }
                        )
                    }
                    MainNavigationTab.SETTINGS.ordinal -> {
                        SettingsScreen(
                            engineState = engineState,
                            themeMode = themeMode,
                            dynamicColor = dynamicColor,
                            hapticsEnabled = hapticsEnabled,
                            resumeOnBoot = resumeOnBoot,
                            isBatteryOptimizationIgnored = viewModel.isIgnoringBatteryOptimizations(),
                            onThemeChange = { viewModel.setThemeMode(it) },
                            onDynamicColorChange = { viewModel.setDynamicColor(it) },
                            onHapticsChange = { viewModel.setHapticsEnabled(it) },
                            onResumeOnBootChange = { viewModel.setResumeOnBoot(it) },
                            onRequestBatteryOpt = { viewModel.requestIgnoreBatteryOptimizations(context) },
                            onResetAllBoosts = { viewModel.resetAllBoostsToSafe() },
                            onNavigateToDiagnostics = { showDiagnostics = true }
                        )
                    }
                }
            }
        }

        // Headphone Triple Safety Confirmation Flow
        pendingHeadphoneConfirm?.let { pending ->
            HeadphoneTripleSafetyDialogs(
                targetPercent = pending.targetPercent,
                appName = pending.appName,
                deviceDescription = engineState.headphoneName,
                onDismiss = { viewModel.dismissHeadphoneSafety() },
                onConfirmed = { viewModel.confirmHeadphoneSafety() }
            )
        }

        if (showDiagnostics) {
            DiagnosticsScreen(
                engineState = engineState,
                onBack = { showDiagnostics = false },
                onRunDiagnosticCheck = { viewModel.runDiagnosticsCheck() }
            )
        }
    }
}

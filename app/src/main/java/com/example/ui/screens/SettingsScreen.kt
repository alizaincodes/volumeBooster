package com.example.ui.screens

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AudioEngineState
import com.example.ui.dialogs.HowItWorksSheet
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.RiskRed
import com.example.ui.theme.SafeGreen

@Composable
fun SettingsScreen(
    engineState: AudioEngineState,
    themeMode: String,
    dynamicColor: Boolean,
    hapticsEnabled: Boolean,
    resumeOnBoot: Boolean,
    isBatteryOptimizationIgnored: Boolean,
    onThemeChange: (String) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onHapticsChange: (Boolean) -> Unit,
    onResumeOnBootChange: (Boolean) -> Unit,
    onRequestBatteryOpt: () -> Unit,
    onResetAllBoosts: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showThemeDialog by remember { mutableStateOf(false) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }
    var showHowItWorksSheet by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Headphone status banner
            item(key = "headphone_status_card") {
                Surface(
                    color = if (engineState.isHeadphonesConnected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(
                                    if (engineState.isHeadphonesConnected) AmberWarning.copy(alpha = 0.2f)
                                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (engineState.isHeadphonesConnected) Icons.Default.Headphones else Icons.Default.Speaker,
                                contentDescription = null,
                                tint = if (engineState.isHeadphonesConnected) AmberWarning else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (engineState.isHeadphonesConnected) "Headphones Connected" else "Speaker Output",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (engineState.isHeadphonesConnected) {
                                    engineState.headphoneName ?: "Wired / Bluetooth Headset"
                                } else {
                                    "Built-in device loudspeaker"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Surface(
                            color = if (engineState.isHeadphonesConnected) AmberWarning.copy(alpha = 0.2f) else SafeGreen.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (engineState.isHeadphonesConnected) "PROTECTED" else "NORMAL",
                                color = if (engineState.isHeadphonesConnected) AmberWarning else SafeGreen,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Group: Appearance & Theme
            item(key = "group_appearance") {
                SettingsGroup(title = "Appearance & Interface") {
                    SettingsTile(
                        icon = Icons.Default.DarkMode,
                        title = "Theme",
                        subtitle = when (themeMode) {
                            "LIGHT" -> "Light"
                            "DARK" -> "Dark"
                            "AMOLED" -> "AMOLED Pure Black"
                            else -> "System Default"
                        },
                        onClick = { showThemeDialog = true }
                    )

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        SettingsToggleTile(
                            icon = Icons.Default.Palette,
                            title = "Material You Dynamic Color",
                            subtitle = "Derive accent palette from your system wallpaper",
                            checked = dynamicColor,
                            onCheckedChange = onDynamicColorChange
                        )
                    }

                    SettingsToggleTile(
                        icon = Icons.Default.Vibration,
                        title = "Haptic Feedback",
                        subtitle = "Vibrate on slider dragging and threshold transitions",
                        checked = hapticsEnabled,
                        onCheckedChange = onHapticsChange
                    )
                }
            }

            // Group: Hearing Safety & Emergency
            item(key = "group_safety") {
                SettingsGroup(title = "Hearing Safety & Health") {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Hearing, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Safe Listening Guidelines (WHO)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Follow the 60/60 rule: keep volume under 60% of device maximum for no more than 60 minutes per session to prevent noise-induced hearing loss.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Emergency reset button
                    Button(
                        onClick = { showResetConfirmDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = RiskRed.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().testTag("emergency_reset_button")
                    ) {
                        Icon(Icons.Default.RestartAlt, contentDescription = null, tint = RiskRed)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Reset All Boosts to 100% (Emergency)",
                            color = RiskRed,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Group: Battery & Engine
            item(key = "group_battery") {
                SettingsGroup(title = "Battery & Audio Engine") {
                    SettingsTile(
                        icon = Icons.Default.BatterySaver,
                        title = "Battery Optimization",
                        subtitle = if (isBatteryOptimizationIgnored) "Unrestricted (recommended)" else "Optimized (may be killed in background by OS)",
                        onClick = onRequestBatteryOpt
                    )

                    SettingsToggleTile(
                        icon = Icons.Default.PowerSettingsNew,
                        title = "Resume Boosting on Device Restart",
                        subtitle = "Automatically re-enable master boost when phone turns on",
                        checked = resumeOnBoot,
                        onCheckedChange = onResumeOnBootChange
                    )

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Engine CPU Architecture",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Status: ${engineState.engineStatus} | Callback-driven (0% polling)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Group: Transparency & About
            item(key = "group_about") {
                SettingsGroup(title = "About & Architecture") {
                    SettingsTile(
                        icon = Icons.Default.HelpOutline,
                        title = "How Volume Boost Works",
                        subtitle = "Learn why we don't need microphone access and how sessions work",
                        onClick = { showHowItWorksSheet = true }
                    )

                    SettingsTile(
                        icon = Icons.Default.Info,
                        title = "App Version",
                        subtitle = "1.0.0 (Production AudioEngine)",
                        onClick = {}
                    )
                }
            }
        }

        // Theme Dialog
        if (showThemeDialog) {
            ThemeSelectionDialog(
                currentTheme = themeMode,
                onSelect = {
                    onThemeChange(it)
                    showThemeDialog = false
                },
                onDismiss = { showThemeDialog = false }
            )
        }

        // Emergency Reset Confirmation Dialog
        if (showResetConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showResetConfirmDialog = false },
                icon = { Icon(Icons.Default.RestartAlt, contentDescription = null, tint = RiskRed) },
                title = { Text("Reset all volume boosts?") },
                text = { Text("This will reset all app profiles and active sessions back to the device's native 100% volume ceiling immediately.") },
                confirmButton = {
                    Button(
                        onClick = {
                            onResetAllBoosts()
                            showResetConfirmDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RiskRed)
                    ) {
                        Text("Reset All", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showResetConfirmDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // How it works bottom sheet
        if (showHowItWorksSheet) {
            HowItWorksSheet(onDismiss = { showHowItWorksSheet = false })
        }
    }
}

@Composable
private fun SettingsGroup(
    title: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun SettingsTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SettingsToggleTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}

@Composable
private fun ThemeSelectionDialog(
    currentTheme: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val options = listOf(
        "SYSTEM" to "System Default",
        "LIGHT" to "Light",
        "DARK" to "Dark",
        "AMOLED" to "AMOLED Pure Black"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose Theme", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                options.forEach { (key, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(key) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentTheme == key,
                            onClick = { onSelect(key) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(label, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

package com.example.ui.dialogs

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.RiskRed
import kotlinx.coroutines.delay

sealed class SafetyDialogStep {
    data object Dialog1Info : SafetyDialogStep()
    data object Dialog2RiskCheck : SafetyDialogStep()
    data object Dialog3CountdownConfirm : SafetyDialogStep()
}

@Composable
fun HeadphoneTripleSafetyDialogs(
    targetPercent: Int,
    appName: String,
    deviceDescription: String? = "Headphones",
    onDismiss: () -> Unit,
    onConfirmed: () -> Unit
) {
    var currentStep by remember { mutableStateOf<SafetyDialogStep>(SafetyDialogStep.Dialog1Info) }

    when (currentStep) {
        is SafetyDialogStep.Dialog1Info -> {
            Dialog1Informational(
                deviceDescription = deviceDescription,
                onCancel = onDismiss,
                onContinue = { currentStep = SafetyDialogStep.Dialog2RiskCheck }
            )
        }
        is SafetyDialogStep.Dialog2RiskCheck -> {
            Dialog2ExplicitRisk(
                onGoBack = { currentStep = SafetyDialogStep.Dialog1Info },
                onContinue = { currentStep = SafetyDialogStep.Dialog3CountdownConfirm }
            )
        }
        is SafetyDialogStep.Dialog3CountdownConfirm -> {
            Dialog3FinalConfirmationWithCooldown(
                targetPercent = targetPercent,
                appName = appName,
                onCancel = onDismiss,
                onConfirm = {
                    onConfirmed()
                }
            )
        }
    }
}

@Composable
private fun Dialog1Informational(
    deviceDescription: String?,
    onCancel: () -> Unit,
    onContinue: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        icon = {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(AmberWarning.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Headphones,
                    contentDescription = null,
                    tint = AmberWarning,
                    modifier = Modifier.size(32.dp)
                )
            }
        },
        title = {
            Text(
                text = "Headphones detected",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!deviceDescription.isNullOrEmpty()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Connected device: $deviceDescription",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
                Text(
                    text = "You're about to boost volume above safe levels while wearing headphones. Loud, boosted audio played directly into your ears carries a real risk of permanent hearing damage.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onContinue,
                colors = ButtonDefaults.buttonColors(containerColor = AmberWarning)
            ) {
                Text("Continue", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun Dialog2ExplicitRisk(
    onGoBack: () -> Unit,
    onContinue: () -> Unit
) {
    var isRiskAcknowledged by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onGoBack,
        icon = {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(RiskRed.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = RiskRed,
                    modifier = Modifier.size(32.dp)
                )
            }
        },
        title = {
            Text(
                text = "This can cause permanent hearing loss",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = RiskRed
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = "Boosting beyond 100% increases sound pressure at your eardrum well past manufacturer-safe limits. Hearing damage from loud headphone use is often gradual, painless while it happens, and irreversible.",
                    style = MaterialTheme.typography.bodyMedium
                )

                Surface(
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, RiskRed.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isRiskAcknowledged,
                            onCheckedChange = { isRiskAcknowledged = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = RiskRed,
                                checkmarkColor = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "I understand the risk of permanent hearing damage.",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onContinue,
                enabled = isRiskAcknowledged,
                colors = ButtonDefaults.buttonColors(
                    containerColor = RiskRed,
                    contentColor = Color.White,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
            ) {
                Text("Continue", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onGoBack) {
                Text("Go back")
            }
        }
    )
}

@Composable
private fun Dialog3FinalConfirmationWithCooldown(
    targetPercent: Int,
    appName: String,
    onCancel: () -> Unit,
    onConfirm: () -> Unit
) {
    // 3-second unskippable countdown
    var countdown by remember { mutableIntStateOf(3) }

    LaunchedEffect(Unit) {
        while (countdown > 0) {
            delay(1000)
            countdown--
        }
    }

    AlertDialog(
        onDismissRequest = onCancel,
        title = {
            Text(
                text = "Confirm volume boost",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Boost level: $targetPercent% will be applied to $appName while headphones are connected. Start at a lower level and increase gradually — never boost suddenly during playback.",
                    style = MaterialTheme.typography.bodyMedium
                )

                // Live disabled preview of requested boost slider position
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Preview Target", style = MaterialTheme.typography.labelMedium)
                            Text(
                                "$targetPercent%",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (targetPercent > 200) RiskRed else AmberWarning
                            )
                        }
                        Slider(
                            value = targetPercent.toFloat(),
                            onValueChange = {},
                            valueRange = 0f..300f,
                            enabled = false
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = countdown == 0,
                colors = ButtonDefaults.buttonColors(
                    containerColor = RiskRed,
                    contentColor = Color.White,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                if (countdown > 0) {
                    Text("Confirm ($countdown)", fontWeight = FontWeight.Bold)
                } else {
                    Text("Confirm", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text("Cancel")
            }
        }
    )
}

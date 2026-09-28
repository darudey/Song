package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.EqualizerPreset

val builtInPresets = listOf(
    EqualizerPreset("Flat", 0f, 0f, 0f, 0f, 0f),
    EqualizerPreset("Bass Boost", 8f, 5f, 0f, -2f, -1f),
    EqualizerPreset("Rock", 5f, 3f, -1f, 4f, 6f),
    EqualizerPreset("Pop", 2f, 4f, 5f, 3f, 1f),
    EqualizerPreset("EDM / Dance", 7f, 4f, -2f, 3f, 6f),
    EqualizerPreset("Jazz", 4f, 2f, 1f, 3f, 4f),
    EqualizerPreset("Classical", 5f, 3f, 0f, 2f, 5f),
    EqualizerPreset("Vocal Boost", -2f, 1f, 6f, 4f, 2f)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerSheet(
    activePreset: EqualizerPreset,
    bassBoost: Float,
    playbackSpeed: Float,
    onSelectPreset: (EqualizerPreset) -> Unit,
    onSetBassBoost: (Float) -> Unit,
    onSetPlaybackSpeed: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    var customBass by remember(activePreset) { mutableFloatStateOf(activePreset.bass) }
    var customLowMid by remember(activePreset) { mutableFloatStateOf(activePreset.lowMid) }
    var customMid by remember(activePreset) { mutableFloatStateOf(activePreset.mid) }
    var customHighMid by remember(activePreset) { mutableFloatStateOf(activePreset.highMid) }
    var customTreble by remember(activePreset) { mutableFloatStateOf(activePreset.treble) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        modifier = Modifier.testTag("equalizer_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    text = "Audio FX & Equalizer",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Presets Horizontal Row
            Text(
                text = "PRESETS",
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 1.2.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                builtInPresets.forEach { preset ->
                    val isSelected = activePreset.name == preset.name
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            onSelectPreset(preset)
                            customBass = preset.bass
                            customLowMid = preset.lowMid
                            customMid = preset.mid
                            customHighMid = preset.highMid
                            customTreble = preset.treble
                        },
                        label = { Text(preset.name) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 5-Band Graphic Sliders
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "5-BAND EQUALIZER",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    BandSlider(label = "60 Hz (Bass)", value = customBass) {
                        customBass = it
                        onSelectPreset(EqualizerPreset("Custom", customBass, customLowMid, customMid, customHighMid, customTreble))
                    }
                    BandSlider(label = "230 Hz (Low Mid)", value = customLowMid) {
                        customLowMid = it
                        onSelectPreset(EqualizerPreset("Custom", customBass, customLowMid, customMid, customHighMid, customTreble))
                    }
                    BandSlider(label = "910 Hz (Mid)", value = customMid) {
                        customMid = it
                        onSelectPreset(EqualizerPreset("Custom", customBass, customLowMid, customMid, customHighMid, customTreble))
                    }
                    BandSlider(label = "3.6 kHz (High Mid)", value = customHighMid) {
                        customHighMid = it
                        onSelectPreset(EqualizerPreset("Custom", customBass, customLowMid, customMid, customHighMid, customTreble))
                    }
                    BandSlider(label = "14 kHz (Treble)", value = customTreble) {
                        customTreble = it
                        onSelectPreset(EqualizerPreset("Custom", customBass, customLowMid, customMid, customHighMid, customTreble))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bass Boost & Playback Speed
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Extra Bass Boost",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = "${(bassBoost * 10).toInt()}%",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        )
                    }
                    Slider(
                        value = bassBoost,
                        onValueChange = onSetBassBoost,
                        valueRange = 0f..10f,
                        steps = 10,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Playback Speed (${playbackSpeed}x)",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { spd ->
                            FilterChip(
                                selected = playbackSpeed == spd,
                                onClick = { onSetPlaybackSpeed(spd) },
                                label = { Text("${spd}x") }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun BandSlider(label: String, value: Float, onValueChange: (Float) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.width(100.dp)
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = -10f..10f,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.secondary,
                activeTrackColor = MaterialTheme.colorScheme.secondary
            )
        )
        Text(
            text = "${if (value > 0) "+" else ""}${value.toInt()}dB",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            modifier = Modifier.width(42.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

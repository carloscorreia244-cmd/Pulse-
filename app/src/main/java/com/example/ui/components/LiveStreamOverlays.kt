package com.example.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.media.StreamPipelineState
import com.example.media.StreamQualityPreset
import com.example.media.StreamTelemetry
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.PulsoCreatorGold
import com.example.ui.theme.PulsoEmerald
import com.example.ui.theme.PulsoLiveCoral

@Composable
fun ViewerCountOverlayBadge(
    viewerCount: Int,
    isLive: Boolean,
    isActivelyStreaming: Boolean,
    elapsedSeconds: Int,
    modifier: Modifier = Modifier
) {
    val pulseTransition = rememberInfiniteTransition(label = "live_pulse")
    val dotAlpha by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 750),
            repeatMode = RepeatMode.Reverse
        ),
        label = "live_dot_alpha"
    )

    val mins = elapsedSeconds / 60
    val secs = elapsedSeconds % 60
    val timerText = "%02d:%02d".format(mins, secs)

    Surface(
        color = if (isLive) PulsoLiveCoral else Color(0xFF334155),
        shape = RoundedCornerShape(999.dp),
        modifier = modifier
            .semantics(mergeDescendants = true) {
                contentDescription = if (isLive) {
                    "$viewerCount espectadores assistindo ao vivo"
                } else {
                    "Transmissão encerrada com $viewerCount espectadores"
                }
            }
            .testTag("viewer_count_overlay")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (isLive) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .alpha(if (isActivelyStreaming) dotAlpha else 1f)
                        .clip(CircleShape)
                        .background(Color.White)
                )
            }
            Icon(
                imageVector = Icons.Filled.RemoveRedEye,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = viewerCount.toString(),
                color = Color.White,
                fontFamily = JetBrainsMono,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
            if (isActivelyStreaming) {
                Text(
                    text = "• $timerText",
                    color = Color.White.copy(alpha = 0.9f),
                    fontFamily = JetBrainsMono,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Composable
fun StreamTelemetryOverlayCard(
    telemetry: StreamTelemetry,
    isStreamingActive: Boolean,
    selectedQuality: StreamQualityPreset,
    onSelectQuality: (StreamQualityPreset) -> Unit,
    modifier: Modifier = Modifier
) {
    val stateColor = when (telemetry.pipelineState) {
        StreamPipelineState.STREAMING -> PulsoEmerald
        StreamPipelineState.READY -> PulsoCreatorGold
        StreamPipelineState.CAMERA_UNAVAILABLE -> PulsoLiveCoral
        else -> Color.White
    }

    Surface(
        color = Color(0xB3071417),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("stream_telemetry_overlay")
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Sensors,
                        contentDescription = null,
                        tint = stateColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = telemetry.pipelineState.label,
                        color = stateColor,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "${telemetry.codecName} • ${telemetry.resolutionLabel}",
                    color = Color.White.copy(alpha = 0.8f),
                    fontFamily = JetBrainsMono,
                    style = MaterialTheme.typography.labelSmall
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${telemetry.currentFps} FPS • ${telemetry.estimatedBitrateKbps} kbps",
                    color = Color.White,
                    fontFamily = JetBrainsMono,
                    fontSize = 11.sp
                )
                Text(
                    text = "Pacotes: ${telemetry.packetsPrepared} / Quadros: ${telemetry.framesCaptured}",
                    color = PulsoEmerald,
                    fontFamily = JetBrainsMono,
                    fontSize = 11.sp
                )
            }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(StreamQualityPreset.entries) { preset ->
                    val isSelected = selectedQuality == preset
                    Surface(
                        color = if (isSelected) PulsoEmerald else Color.White.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(999.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .clickable(enabled = !isStreamingActive) { onSelectQuality(preset) }
                            .testTag("quality_chip_${preset.name}")
                    ) {
                        Text(
                            text = preset.label,
                            color = if (isSelected) Color(0xFF002116) else Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HostCameraControlsRow(
    hasPermission: Boolean,
    isStreamingActive: Boolean,
    isMicMuted: Boolean,
    hasTorch: Boolean,
    isTorchEnabled: Boolean,
    onRequestPermission: () -> Unit,
    onSwitchCamera: () -> Unit,
    onToggleMic: () -> Unit,
    onToggleTorch: () -> Unit,
    onToggleStreaming: () -> Unit,
    onEndLive: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!hasPermission) {
                OutlinedButton(
                    onClick = onRequestPermission,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    modifier = Modifier.testTag("grant_camera_permission_button")
                ) {
                    Icon(Icons.Filled.Videocam, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Ativar Câmera")
                }
            } else {
                IconButton(
                    onClick = onSwitchCamera,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0x99000000))
                        .testTag("switch_camera_button")
                ) {
                    Icon(Icons.Filled.Cameraswitch, contentDescription = "Trocar câmera", tint = Color.White)
                }
                IconButton(
                    onClick = onToggleMic,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0x99000000))
                        .testTag("toggle_mic_button")
                ) {
                    Icon(
                        imageVector = if (isMicMuted) Icons.Filled.MicOff else Icons.Filled.Mic,
                        contentDescription = if (isMicMuted) "Ativar microfone" else "Silenciar microfone",
                        tint = if (isMicMuted) PulsoLiveCoral else Color.White
                    )
                }
                if (hasTorch) {
                    IconButton(
                        onClick = onToggleTorch,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(0x99000000))
                            .testTag("toggle_torch_button")
                    ) {
                        Icon(
                            imageVector = if (isTorchEnabled) Icons.Filled.FlashOn else Icons.Filled.FlashOff,
                            contentDescription = "Alternar lanterna",
                            tint = if (isTorchEnabled) PulsoCreatorGold else Color.White
                        )
                    }
                }
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onToggleStreaming,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isStreamingActive) PulsoCreatorGold else PulsoEmerald,
                    contentColor = Color(0xFF002116)
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                modifier = Modifier.testTag("toggle_stream_capture_button")
            ) {
                Icon(
                    imageVector = if (isStreamingActive) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isStreamingActive) "Pausar" else "Transmitir",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }

            Button(
                onClick = onEndLive,
                colors = ButtonDefaults.buttonColors(containerColor = PulsoLiveCoral),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                modifier = Modifier.testTag("end_my_live_button")
            ) {
                Icon(Icons.Filled.StopCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Encerrar", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}

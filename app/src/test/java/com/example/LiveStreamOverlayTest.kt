package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.example.media.StreamPipelineState
import com.example.media.StreamQualityPreset
import com.example.media.StreamTelemetry
import com.example.ui.components.StreamTelemetryOverlayCard
import com.example.ui.components.ViewerCountOverlayBadge
import com.example.ui.theme.PulsoTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LiveStreamOverlayTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun viewerCountOverlay_displaysCurrentViewerCountAndTimer() {
        composeTestRule.setContent {
            PulsoTheme {
                ViewerCountOverlayBadge(
                    viewerCount = 1284,
                    isLive = true,
                    isActivelyStreaming = true,
                    elapsedSeconds = 75
                )
            }
        }

        composeTestRule.onNodeWithTag("viewer_count_overlay").assertIsDisplayed()
        composeTestRule.onNodeWithText("1284").assertIsDisplayed()
        composeTestRule.onNodeWithText("• 01:15").assertIsDisplayed()
    }

    @Test
    fun streamTelemetryOverlay_displaysEncoderStateFpsAndBitrate() {
        composeTestRule.setContent {
            PulsoTheme {
                StreamTelemetryOverlayCard(
                    telemetry = StreamTelemetry(
                        pipelineState = StreamPipelineState.STREAMING,
                        qualityPreset = StreamQualityPreset.HD_720P,
                        resolutionLabel = "1280x720",
                        currentFps = 30,
                        estimatedBitrateKbps = 2620,
                        framesCaptured = 180,
                        packetsPrepared = 175,
                        codecName = "c2.android.avc.encoder"
                    ),
                    isStreamingActive = true,
                    selectedQuality = StreamQualityPreset.HD_720P,
                    onSelectQuality = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("stream_telemetry_overlay").assertIsDisplayed()
        composeTestRule.onNodeWithText("Transmitindo ao vivo").assertIsDisplayed()
        composeTestRule.onNodeWithText("30 FPS • 2620 kbps").assertIsDisplayed()
        composeTestRule.onNodeWithText("Pacotes: 175 / Quadros: 180").assertIsDisplayed()
    }
}

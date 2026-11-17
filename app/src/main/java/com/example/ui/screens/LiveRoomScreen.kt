package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.local.LiveCommentEntity
import com.example.data.local.LiveStreamEntity
import com.example.data.local.UserEntity
import com.example.media.CameraStreamPipeline
import com.example.media.StreamPipelineState
import com.example.media.StreamQualityPreset
import com.example.model.DEFAULT_LIVE_GIFTS
import com.example.model.LiveGiftOption
import com.example.model.formatCurrencyBrl
import com.example.ui.components.HostCameraControlsRow
import com.example.ui.components.PulsoAvatar
import com.example.ui.components.StreamTelemetryOverlayCard
import com.example.ui.components.VerifiedBadge
import com.example.ui.components.ViewerCountOverlayBadge
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.PulsoCreatorGold
import com.example.ui.theme.PulsoEmerald
import kotlinx.coroutines.delay

@Composable
fun LiveRoomScreen(
    live: LiveStreamEntity?,
    comments: List<LiveCommentEntity>,
    currentUser: UserEntity?,
    onClose: () -> Unit,
    onSendComment: (String) -> Unit,
    onSendGift: (LiveGiftOption, String) -> Unit,
    onSubscribeCreator: () -> Unit,
    onEndLive: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (live == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Carregando transmissão...")
        }
        return
    }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val listState = rememberLazyListState()

    val streamPipeline = remember { CameraStreamPipeline(context) }
    val telemetry by streamPipeline.telemetry.collectAsStateWithLifecycle()

    var commentText by remember { mutableStateOf("") }
    var useFrontCamera by remember { mutableStateOf(true) }
    var isMicMuted by remember { mutableStateOf(false) }
    var isStreamingActive by remember(live.id) { mutableStateOf(live.isHostSelf && live.isLive) }
    var selectedQuality by remember { mutableStateOf(StreamQualityPreset.HD_720P) }
    var elapsedSeconds by remember(live.id) { mutableIntStateOf(0) }
    var liveViewerCount by remember(live.id) { mutableIntStateOf(live.viewerCount) }

    var hasCameraMicPerm by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        hasCameraMicPerm = perms[Manifest.permission.CAMERA] == true
    }

    DisposableEffect(Unit) {
        onDispose { streamPipeline.release() }
    }

    LaunchedEffect(live.isHostSelf) {
        if (live.isHostSelf && !hasCameraMicPerm) {
            permLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
        }
    }

    LaunchedEffect(isStreamingActive) {
        streamPipeline.setStreamingActive(isStreamingActive)
    }

    // Live timer & organic viewer count ticker while broadcasting or watching live
    LaunchedEffect(live.isLive, isStreamingActive) {
        while (live.isLive) {
            delay(1000L)
            if (isStreamingActive || !live.isHostSelf) {
                elapsedSeconds++
                if (elapsedSeconds % 4 == 0) {
                    val delta = (-1..3).random()
                    liveViewerCount = (liveViewerCount + delta).coerceAtLeast(1)
                }
            }
        }
    }

    LaunchedEffect(comments.size) {
        if (comments.isNotEmpty()) {
            listState.animateScrollToItem(comments.size - 1)
        }
    }

    val latestSuperGift = remember(comments) {
        comments.lastOrNull { it.isSuperDonation }
    }
    var showSuperBanner by remember { mutableStateOf(false) }

    LaunchedEffect(latestSuperGift?.id) {
        if (latestSuperGift != null) {
            showSuperBanner = true
            delay(4500L)
            showSuperBanner = false
        }
    }

    val thumbRes = when (live.thumbnailKey) {
        "music" -> R.drawable.img_live_music_thumb
        "tech" -> R.drawable.img_live_tech_thumb
        "culinary" -> R.drawable.img_live_culinary_thumb
        else -> R.drawable.img_live_studio_banner
    }

    val progress = if (live.goalTargetCents > 0L) {
        (live.goalCurrentCents.toFloat() / live.goalTargetCents.toFloat()).coerceIn(0f, 1f)
    } else 0f

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF060E10))
            .imePadding()
    ) {
        // Video Layer: CameraX Preview + ImageAnalysis frame capture when hosting with permission
        if (live.isHostSelf && live.isLive && hasCameraMicPerm &&
            telemetry.pipelineState != StreamPipelineState.CAMERA_UNAVAILABLE
        ) {
            val previewView = remember { PreviewView(context) }
            LaunchedEffect(useFrontCamera, selectedQuality, hasCameraMicPerm) {
                streamPipeline.bindCamera(
                    lifecycleOwner = lifecycleOwner,
                    previewView = previewView,
                    useFrontCamera = useFrontCamera,
                    qualityPreset = selectedQuality
                )
            }
            AndroidView(
                factory = { previewView },
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("camerax_live_preview")
            )
        } else {
            Image(
                painter = painterResource(id = thumbRes),
                contentDescription = live.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Gradient readability overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xCC060E10),
                            Color(0x33060E10),
                            Color(0xAA060E10),
                            Color(0xF2060E10)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Host, Viewer Count Overlay & Camera Stream Controls
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Surface(
                        color = Color(0x88000000),
                        shape = RoundedCornerShape(999.dp),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PulsoAvatar(
                                name = live.hostName,
                                colorHex = live.hostAvatarColorHex,
                                size = 34.dp,
                                isLive = live.isLive
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f, fill = false)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = live.hostName,
                                        color = Color.White,
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    VerifiedBadge(tierRaw = live.hostVerificationTier, size = 14.dp)
                                }
                                Text(
                                    text = live.title,
                                    color = Color.White.copy(alpha = 0.75f),
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ViewerCountOverlayBadge(
                            viewerCount = liveViewerCount,
                            isLive = live.isLive,
                            isActivelyStreaming = isStreamingActive,
                            elapsedSeconds = elapsedSeconds
                        )

                        IconButton(
                            onClick = onClose,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color(0x88000000))
                                .testTag("close_live_room_button")
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = "Sair da Live", tint = Color.White)
                        }
                    }
                }

                // Stream Telemetry & Quality Selector HUD when hosting
                if (live.isHostSelf && live.isLive) {
                    StreamTelemetryOverlayCard(
                        telemetry = telemetry,
                        isStreamingActive = isStreamingActive,
                        selectedQuality = selectedQuality,
                        onSelectQuality = { selectedQuality = it }
                    )

                    HostCameraControlsRow(
                        hasPermission = hasCameraMicPerm,
                        isStreamingActive = isStreamingActive,
                        isMicMuted = isMicMuted,
                        hasTorch = telemetry.hasTorch,
                        isTorchEnabled = telemetry.isTorchEnabled,
                        onRequestPermission = {
                            permLauncher.launch(
                                arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
                            )
                        },
                        onSwitchCamera = { useFrontCamera = !useFrontCamera },
                        onToggleMic = { isMicMuted = !isMicMuted },
                        onToggleTorch = { streamPipeline.toggleTorch() },
                        onToggleStreaming = { isStreamingActive = !isStreamingActive },
                        onEndLive = {
                            isStreamingActive = false
                            streamPipeline.setStreamingActive(false)
                            onEndLive()
                            onClose()
                        }
                    )
                }

                // Goal & Monetization Bar
                Surface(
                    color = Color(0xAA0C1A1D),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.Lock, contentDescription = null, tint = PulsoEmerald, modifier = Modifier.size(13.dp))
                                Text(
                                    text = "Meta: ${live.goalTitle}",
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = "${formatCurrencyBrl(live.goalCurrentCents)} / ${formatCurrencyBrl(live.goalTargetCents)}",
                                fontFamily = JetBrainsMono,
                                color = PulsoEmerald,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(999.dp)),
                            color = PulsoEmerald,
                            trackColor = Color.White.copy(alpha = 0.16f)
                        )
                    }
                }

                if (!live.isHostSelf) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Saldo: ${formatCurrencyBrl(currentUser?.walletBalanceCents ?: 0L)}",
                            color = Color.White.copy(alpha = 0.85f),
                            fontFamily = JetBrainsMono,
                            style = MaterialTheme.typography.labelSmall
                        )
                        Button(
                            onClick = onSubscribeCreator,
                            enabled = !live.isSubscribedByMe,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (live.isSubscribedByMe) PulsoEmerald else PulsoCreatorGold,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(999.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("subscribe_creator_button")
                        ) {
                            Icon(Icons.Filled.Star, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (live.isSubscribedByMe) {
                                    "Membro do Clube"
                                } else {
                                    "Apoiar ${formatCurrencyBrl(live.subscriptionPriceCents)}/mês"
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // Bottom Live Chat + Super Donations + Gift Strip
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                AnimatedVisibility(
                    visible = showSuperBanner && latestSuperGift != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    latestSuperGift?.let { gift ->
                        Surface(
                            color = PulsoCreatorGold,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(gift.giftEmoji.ifEmpty { "🎁" }, fontSize = 24.sp)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${gift.authorName} enviou ${formatCurrencyBrl(gift.donationCents)}!",
                                        color = Color.Black,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                    Text(
                                        text = gift.text,
                                        color = Color.Black.copy(alpha = 0.85f),
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(comments, key = { it.id }) { comment ->
                        Surface(
                            color = if (comment.isSuperDonation) {
                                PulsoCreatorGold.copy(alpha = 0.24f)
                            } else {
                                Color(0x77000000)
                            },
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (comment.giftEmoji.isNotEmpty()) {
                                    Text(comment.giftEmoji, fontSize = 18.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = comment.authorName,
                                            color = if (comment.isSuperDonation) PulsoCreatorGold else PulsoEmerald,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        VerifiedBadge(tierRaw = comment.authorVerificationTier, size = 12.dp)
                                        if (comment.donationCents > 0L) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = formatCurrencyBrl(comment.donationCents),
                                                color = PulsoCreatorGold,
                                                fontFamily = JetBrainsMono,
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                    }
                                    Text(
                                        text = comment.text,
                                        color = Color.White,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(DEFAULT_LIVE_GIFTS, key = { it.id }) { gift ->
                        Surface(
                            color = Color(0xCC13272B),
                            shape = RoundedCornerShape(999.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .clickable { onSendGift(gift, commentText) }
                                .testTag("live_gift_${gift.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(gift.emoji, fontSize = 16.sp)
                                Text(
                                    text = "${gift.label} • ${formatCurrencyBrl(gift.amountCents)}",
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = commentText,
                        onValueChange = { commentText = it },
                        placeholder = { Text("Comente na live (E2EE)...", color = Color.White.copy(alpha = 0.65f)) },
                        singleLine = true,
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = PulsoEmerald,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.35f),
                            focusedContainerColor = Color(0x88000000),
                            unfocusedContainerColor = Color(0x88000000)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("live_comment_input")
                    )
                    IconButton(
                        onClick = {
                            onSendComment(commentText)
                            commentText = ""
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(PulsoEmerald)
                            .testTag("send_live_comment_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Enviar comentário",
                            tint = Color(0xFF002116)
                        )
                    }
                }
            }
        }
    }
}

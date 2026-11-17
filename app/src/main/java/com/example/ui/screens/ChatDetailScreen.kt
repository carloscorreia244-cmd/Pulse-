package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.data.local.ChatEntity
import com.example.data.local.MessageType
import com.example.data.local.UserEntity
import com.example.media.VoiceRecorderPlayer
import com.example.model.DecryptedMessage
import com.example.model.formatClockTime
import com.example.model.formatCurrencyBrl
import com.example.ui.components.DonationSheet
import com.example.ui.components.E2EEShieldPill
import com.example.ui.components.PulsoAvatar
import com.example.ui.components.VerifiedBadge
import com.example.ui.theme.BubbleIncomingDark
import com.example.ui.theme.BubbleIncomingLight
import com.example.ui.theme.BubbleOutgoingDark
import com.example.ui.theme.BubbleOutgoingLight
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.PulsoCreatorGold
import com.example.ui.theme.PulsoEmerald
import com.example.ui.theme.PulsoLiveCoral
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    chat: ChatEntity?,
    messages: List<DecryptedMessage>,
    currentUser: UserEntity?,
    onBack: () -> Unit,
    onSendText: (String, String?) -> Unit,
    onSendImage: (String) -> Unit,
    onSendAudio: (String?, Int) -> Unit,
    onToggleReaction: (String, List<String>, String) -> Unit,
    onDeleteMessage: (String) -> Unit,
    onMarkDownloaded: (String) -> Unit,
    onTogglePin: (Boolean) -> Unit,
    onToggleVerifiedKey: (Boolean) -> Unit,
    onSendDonation: (amountCents: Long, note: String) -> Unit,
    onExportTranscript: () -> Unit,
    onNotify: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val voiceHelper = remember { VoiceRecorderPlayer(context.applicationContext) }

    var inputText by remember { mutableStateOf("") }
    var replyPreview by remember { mutableStateOf<String?>(null) }
    var showSecuritySheet by remember { mutableStateOf(false) }
    var showDonationSheet by remember { mutableStateOf(false) }
    var showCallDialog by remember { mutableStateOf(false) }
    var selectedMessageForMenu by remember { mutableStateOf<DecryptedMessage?>(null) }
    var inspectCipherMessage by remember { mutableStateOf<DecryptedMessage?>(null) }

    var isRecordingAudio by remember { mutableStateOf(false) }
    var recordingSeconds by remember { mutableIntStateOf(0) }
    var playingAudioId by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        onDispose { voiceHelper.release() }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    LaunchedEffect(isRecordingAudio) {
        if (isRecordingAudio) {
            recordingSeconds = 0
            while (isRecordingAudio) {
                delay(1000L)
                recordingSeconds++
            }
        }
    }

    // Fallback timer for seeded voice notes without real disk files
    LaunchedEffect(playingAudioId) {
        val id = playingAudioId
        if (id != null) {
            val msg = messages.firstOrNull { it.id == id }
            if (msg != null && msg.mediaUri == null) {
                delay((msg.audioDurationSec.coerceAtLeast(2) * 1000L).coerceAtMost(6000L))
                if (playingAudioId == id) playingAudioId = null
            }
        }
    }

    // Zero-permission Android Photo Picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            onSendImage(uri.toString())
        }
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val started = voiceHelper.startRecording()
            if (started) {
                isRecordingAudio = true
            } else {
                // Fallback timer capture if audio hardware unavailable in headless environment
                isRecordingAudio = true
            }
        } else {
            onNotify("Permissão de microfone necessária para gravar áudio.")
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .imePadding(),
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("chat_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar"
                        )
                    }
                },
                title = {
                    if (chat != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable { showSecuritySheet = true }
                                .testTag("chat_header_security_trigger")
                        ) {
                            PulsoAvatar(
                                name = chat.title,
                                colorHex = chat.peerAvatarColorHex,
                                size = 38.dp,
                                isOnline = chat.isOnline
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = chat.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    VerifiedBadge(tierRaw = chat.peerVerificationTier, size = 15.dp)
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Lock,
                                        contentDescription = null,
                                        tint = PulsoEmerald,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Text(
                                        text = if (chat.isOnline) "online • E2EE" else "criptografia ponta a ponta",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showDonationSheet = true },
                        modifier = Modifier.testTag("chat_donate_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.VolunteerActivism,
                            contentDescription = "Enviar doação",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = onExportTranscript,
                        modifier = Modifier.testTag("chat_export_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Download,
                            contentDescription = "Baixar conversa"
                        )
                    }
                    IconButton(
                        onClick = { showSecuritySheet = true },
                        modifier = Modifier.testTag("chat_security_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Security,
                            contentDescription = "Inspecionar criptografia E2EE"
                        )
                    }
                    IconButton(
                        onClick = { showCallDialog = true },
                        modifier = Modifier.testTag("chat_call_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Call,
                            contentDescription = "Chamada criptografada"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .navigationBarsPadding()
            ) {
                if (replyPreview != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Respondendo: $replyPreview",
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { replyPreview = null },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Filled.Close, contentDescription = "Cancelar resposta", modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    IconButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.testTag("pick_image_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Image,
                            contentDescription = "Enviar imagem",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (isRecordingAudio) {
                        Surface(
                            color = PulsoLiveCoral.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(PulsoLiveCoral)
                                    )
                                    Text(
                                        text = "Gravando áudio • ${recordingSeconds}s",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = PulsoLiveCoral
                                    )
                                }
                                TextButton(onClick = {
                                    voiceHelper.stopRecording(discard = true)
                                    isRecordingAudio = false
                                }) {
                                    Text("Cancelar")
                                }
                            }
                        }
                        IconButton(
                            onClick = {
                                val (path, secs) = voiceHelper.stopRecording(discard = false)
                                val finalDuration = maxOf(secs, recordingSeconds).coerceAtLeast(1)
                                isRecordingAudio = false
                                onSendAudio(path, finalDuration)
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                                .testTag("stop_send_audio_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Stop,
                                contentDescription = "Enviar áudio",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    } else {
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            placeholder = { Text("Mensagem criptografada...") },
                            maxLines = 4,
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("chat_message_input")
                        )

                        if (inputText.isNotBlank()) {
                            IconButton(
                                onClick = {
                                    onSendText(inputText, replyPreview)
                                    inputText = ""
                                    replyPreview = null
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                                    .testTag("send_message_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Enviar mensagem",
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        } else {
                            IconButton(
                                onClick = {
                                    val hasPerm = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.RECORD_AUDIO
                                    ) == PackageManager.PERMISSION_GRANTED
                                    if (hasPerm) {
                                        voiceHelper.startRecording()
                                        isRecordingAudio = true
                                    } else {
                                        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .testTag("record_audio_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Mic,
                                    contentDescription = "Gravar mensagem de voz",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 680.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { showSecuritySheet = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Criptografia de ponta a ponta AES-256-GCM ativa. Toque para conferir o número de segurança e o texto cifrado.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                items(messages, key = { it.id }) { msg ->
                    Box {
                        MessageBubble(
                            message = msg,
                            isPlayingAudio = playingAudioId == msg.id,
                            onPlayAudioToggle = {
                                voiceHelper.togglePlayback(msg.id, msg.mediaUri) { newId ->
                                    playingAudioId = newId
                                }
                            },
                            onLongOrClick = { selectedMessageForMenu = msg }
                        )

                        DropdownMenu(
                            expanded = selectedMessageForMenu?.id == msg.id,
                            onDismissRequest = { selectedMessageForMenu = null }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                listOf("❤️", "👏", "🔥", "🔐", "😂").forEach { emoji ->
                                    Text(
                                        text = emoji,
                                        fontSize = 20.sp,
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .clickable {
                                                onToggleReaction(msg.id, msg.reactions, emoji)
                                                selectedMessageForMenu = null
                                            }
                                            .padding(4.dp)
                                    )
                                }
                            }
                            DropdownMenuItem(
                                text = { Text("Responder") },
                                onClick = {
                                    replyPreview = msg.plaintext
                                    selectedMessageForMenu = null
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Baixar / Salvar mensagem") },
                                leadingIcon = { Icon(Icons.Filled.Download, contentDescription = null) },
                                onClick = {
                                    onMarkDownloaded(msg.id)
                                    selectedMessageForMenu = null
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Ver cifra AES-GCM") },
                                leadingIcon = { Icon(Icons.Filled.Security, contentDescription = null) },
                                onClick = {
                                    inspectCipherMessage = msg
                                    selectedMessageForMenu = null
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Apagar") },
                                leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) },
                                onClick = {
                                    onDeleteMessage(msg.id)
                                    selectedMessageForMenu = null
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showSecuritySheet && chat != null) {
        E2EESecurityModalSheet(
            chat = chat,
            latestMessage = messages.lastOrNull(),
            onDismiss = { showSecuritySheet = false },
            onTogglePin = onTogglePin,
            onToggleVerified = onToggleVerifiedKey,
            onExportTranscript = {
                showSecuritySheet = false
                onExportTranscript()
            }
        )
    }

    if (showDonationSheet && chat != null && currentUser != null) {
        DonationSheet(
            recipientName = chat.title,
            recipientHandle = chat.peerHandle,
            recipientTier = chat.peerVerificationTier,
            availableBalanceCents = currentUser.walletBalanceCents,
            onDismiss = { showDonationSheet = false },
            onConfirmDonation = { amountCents, note ->
                showDonationSheet = false
                onSendDonation(amountCents, note)
            }
        )
    }

    if (inspectCipherMessage != null) {
        val m = inspectCipherMessage!!
        AlertDialog(
            onDismissRequest = { inspectCipherMessage = null },
            title = { Text("Criptografia AES-256-GCM") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Texto decifrado:", style = MaterialTheme.typography.labelMedium)
                    Text(m.plaintext, style = MaterialTheme.typography.bodyMedium)
                    Text("IV (96-bit Base64):", style = MaterialTheme.typography.labelMedium)
                    Text(m.ivBase64, fontFamily = JetBrainsMono, fontSize = 11.sp)
                    Text("Ciphertext autenticado:", style = MaterialTheme.typography.labelMedium)
                    Text(m.ciphertextBase64, fontFamily = JetBrainsMono, fontSize = 11.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = { inspectCipherMessage = null }) { Text("Fechar") }
            }
        )
    }

    if (showCallDialog && chat != null) {
        AlertDialog(
            onDismissRequest = { showCallDialog = false },
            icon = { Icon(Icons.Filled.Lock, contentDescription = null, tint = PulsoEmerald) },
            title = { Text("Chamada E2EE com ${chat.title}") },
            text = {
                Text(
                    "As chaves ECDH P-256 da sessão estão prontas. " +
                        "Número de segurança: ${chat.safetyNumber.take(17)}..."
                )
            },
            confirmButton = {
                Button(onClick = {
                    showCallDialog = false
                    onNotify("Chamada criptografada iniciada com ${chat.title}")
                }) {
                    Text("Conectar áudio")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCallDialog = false }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun MessageBubble(
    message: DecryptedMessage,
    isPlayingAudio: Boolean,
    onPlayAudioToggle: () -> Unit,
    onLongOrClick: () -> Unit
) {
    if (message.type == MessageType.SYSTEM) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = message.plaintext,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
        return
    }

    val isDark = isSystemInDarkTheme()
    val bubbleColor = when {
        message.type == MessageType.DONATION -> if (isDark) Color(0xFF0E3F33) else Color(0xFFCCFBF1)
        message.isFromMe -> if (isDark) BubbleOutgoingDark else BubbleOutgoingLight
        else -> if (isDark) BubbleIncomingDark else BubbleIncomingLight
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isFromMe) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = bubbleColor,
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (message.isFromMe) 18.dp else 4.dp,
                bottomEnd = if (message.isFromMe) 4.dp else 18.dp
            ),
            tonalElevation = 1.dp,
            modifier = Modifier
                .widthIn(max = 310.dp)
                .clip(RoundedCornerShape(18.dp))
                .clickable(onClick = onLongOrClick)
                .testTag("message_bubble_${message.id}")
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                if (!message.isFromMe && message.senderName.isNotBlank()) {
                    Text(
                        text = message.senderName,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                }

                if (!message.replyToPreview.isNullOrBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                    ) {
                        Text(
                            text = message.replyToPreview,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                when (message.type) {
                    MessageType.DONATION -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(bottom = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.VolunteerActivism,
                                contentDescription = null,
                                tint = PulsoCreatorGold,
                                modifier = Modifier.size(22.dp)
                            )
                            Column {
                                Text(
                                    text = "DOAÇÃO DIRETA",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PulsoCreatorGold
                                )
                                Text(
                                    text = formatCurrencyBrl(message.donationAmountCents),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontFamily = JetBrainsMono,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Text(
                            text = message.plaintext,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    MessageType.IMAGE -> {
                        if (!message.mediaUri.isNullOrBlank()) {
                            AsyncImage(
                                model = message.mediaUri,
                                contentDescription = "Imagem enviada na conversa",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                        Text(
                            text = message.plaintext,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    MessageType.AUDIO -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            IconButton(
                                onClick = onPlayAudioToggle,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(
                                    imageVector = if (isPlayingAudio) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                    contentDescription = if (isPlayingAudio) "Pausar áudio" else "Ouvir áudio",
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    listOf(10, 18, 24, 14, 22, 16, 26, 12, 20, 15, 19, 11).forEach { h ->
                                        Box(
                                            modifier = Modifier
                                                .width(3.dp)
                                                .height(h.dp)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(
                                                    if (isPlayingAudio) MaterialTheme.colorScheme.primary
                                                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                                                )
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${message.audioDurationSec}s • Áudio E2EE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    else -> {
                        Text(
                            text = message.plaintext,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (message.reactions.isNotEmpty()) {
                        Surface(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(999.dp)
                        ) {
                            Text(
                                text = message.reactions.joinToString(" "),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                            )
                        }
                    }
                    if (message.isDownloaded) {
                        Icon(
                            imageVector = Icons.Filled.Download,
                            contentDescription = "Salva localmente",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                            modifier = Modifier.size(11.dp)
                        )
                    }
                    Text(
                        text = formatClockTime(message.timestampMillis),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (message.isFromMe) {
                        Icon(
                            imageVector = Icons.Filled.DoneAll,
                            contentDescription = "Entregue",
                            tint = PulsoEmerald,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun E2EESecurityModalSheet(
    chat: ChatEntity,
    latestMessage: DecryptedMessage?,
    onDismiss: () -> Unit,
    onTogglePin: (Boolean) -> Unit,
    onToggleVerified: (Boolean) -> Unit,
    onExportTranscript: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.VerifiedUser,
                    contentDescription = null,
                    tint = PulsoEmerald,
                    modifier = Modifier.size(28.dp)
                )
                Column {
                    Text(
                        text = "Criptografia de Ponta a Ponta",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    E2EEShieldPill(verified = chat.keyVerifiedByUser)
                }
            }

            Text(
                text = "Protocolo: ECDH (secp256r1) + Derivação SHA-256 + AES-256-GCM. Somente você e ${chat.title} possuem a chave compartilhada.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            val clipboard = LocalClipboardManager.current
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable {
                        clipboard.setText(AnnotatedString(chat.safetyNumber))
                    }
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "NÚMERO DE SEGURANÇA (60 DÍGITOS)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Icon(
                            imageVector = Icons.Filled.ContentCopy,
                            contentDescription = "Copiar número de segurança",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = chat.safetyNumber,
                        fontFamily = JetBrainsMono,
                        fontSize = 14.sp,
                        lineHeight = 22.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            if (latestMessage != null) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "ÚLTIMA CIFRA ARMAZENADA (AES-GCM)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = latestMessage.ciphertextBase64.take(64) + "...",
                            fontFamily = JetBrainsMono,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Marcar chave como verificada", style = MaterialTheme.typography.bodyLarge)
                Switch(
                    checked = chat.keyVerifiedByUser,
                    onCheckedChange = onToggleVerified,
                    modifier = Modifier.testTag("verify_e2ee_switch")
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Filled.PushPin, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("Fixar conversa no topo", style = MaterialTheme.typography.bodyLarge)
                }
                Switch(
                    checked = chat.isPinned,
                    onCheckedChange = onTogglePin
                )
            }

            OutlinedButton(
                onClick = onExportTranscript,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(Icons.Filled.Download, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Baixar histórico completo (.txt)")
            }
        }
    }
}

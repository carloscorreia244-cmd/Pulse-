package com.example.ui.screens

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.LiveStreamEntity
import com.example.model.formatCurrencyBrl
import com.example.ui.components.PulsoAvatar
import com.example.ui.components.VerifiedBadge
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.PulsoCreatorGold
import com.example.ui.theme.PulsoEmerald
import com.example.ui.theme.PulsoLiveCoral

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LivesScreen(
    liveStreams: List<LiveStreamEntity>,
    onOpenLive: (String) -> Unit,
    onStartMyLive: (
        title: String,
        category: String,
        goalTitle: String,
        goalTargetCents: Long,
        supportersOnly: Boolean
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    val categories = listOf("Todas", "Música", "Tecnologia", "Culinária", "Conversa")
    var selectedCategory by remember { mutableStateOf("Todas") }
    var showGoLiveSheet by remember { mutableStateOf(false) }

    val filtered = remember(liveStreams, selectedCategory) {
        liveStreams.filter { live ->
            selectedCategory == "Todas" || live.category.equals(selectedCategory, ignoreCase = true)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Lives & Criadores",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Transmissões monetizadas e apoio direto",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showGoLiveSheet = true },
                icon = { Icon(Icons.Filled.Videocam, contentDescription = null) },
                text = { Text("Iniciar Live", fontWeight = FontWeight.Bold) },
                containerColor = PulsoLiveCoral,
                contentColor = Color.White,
                modifier = Modifier.testTag("start_live_fab")
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 680.dp),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Hero Studio Banner
                item {
                    Card(
                        shape = RoundedCornerShape(22.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(156.dp)
                            .clickable { showGoLiveSheet = true }
                            .testTag("studio_hero_banner")
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            Image(
                                painter = painterResource(id = R.drawable.img_live_studio_banner),
                                contentDescription = "Estúdio de transmissão ao vivo Pulso",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(
                                                Color(0xE6071514),
                                                Color(0x99071514),
                                                Color.Transparent
                                            )
                                        )
                                    )
                            )
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(18.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Surface(
                                    color = PulsoEmerald.copy(alpha = 0.22f),
                                    shape = RoundedCornerShape(999.dp)
                                ) {
                                    Text(
                                        text = "MONETIZAÇÃO DIRETA • 0% TAXA OCULTA",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = PulsoEmerald,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Crie sua Live agora",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Receba presentes, doações e assinaturas com selo verificado.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.85f)
                                    )
                                }
                            }
                        }
                    }
                }

                // Category filter strip
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(categories) { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat) }
                            )
                        }
                    }
                }

                items(filtered, key = { it.id }) { live ->
                    LiveStreamCard(
                        live = live,
                        onClick = { onOpenLive(live.id) }
                    )
                }
            }
        }
    }

    if (showGoLiveSheet) {
        GoLiveModalSheet(
            onDismiss = { showGoLiveSheet = false },
            onStartLive = { title, cat, goalTitle, targetCents, supportersOnly ->
                showGoLiveSheet = false
                onStartMyLive(title, cat, goalTitle, targetCents, supportersOnly)
            }
        )
    }
}

@Composable
private fun LiveStreamCard(
    live: LiveStreamEntity,
    onClick: () -> Unit
) {
    val thumbRes = when (live.thumbnailKey) {
        "music" -> R.drawable.img_live_music_thumb
        "tech" -> R.drawable.img_live_tech_thumb
        "culinary" -> R.drawable.img_live_culinary_thumb
        else -> R.drawable.img_live_studio_banner
    }
    val progress = if (live.goalTargetCents > 0L) {
        (live.goalCurrentCents.toFloat() / live.goalTargetCents.toFloat()).coerceIn(0f, 1f)
    } else 0f

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("live_card_${live.id}")
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(172.dp)
            ) {
                Image(
                    painter = painterResource(id = thumbRes),
                    contentDescription = live.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0x55000000), Color.Transparent, Color(0xCC000000))
                            )
                        )
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = if (live.isLive) PulsoLiveCoral else Color(0xFF334155),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.LiveTv,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = if (live.isLive) "AO VIVO" else "ENCERRADA",
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (live.isSupportersOnly) {
                            Surface(
                                color = PulsoCreatorGold,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Filled.Star, contentDescription = null, tint = Color.Black, modifier = Modifier.size(12.dp))
                                    Text("VIP", color = Color.Black, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Surface(
                            color = Color(0xAA000000),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.RemoveRedEye,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = live.viewerCount.toString(),
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }

                Surface(
                    color = Color(0xAA000000),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
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
                            text = "${live.category} • Chat E2EE",
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PulsoAvatar(
                        name = live.hostName,
                        colorHex = live.hostAvatarColorHex,
                        size = 42.dp,
                        isLive = live.isLive
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = live.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = live.hostName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            VerifiedBadge(tierRaw = live.hostVerificationTier, size = 14.dp)
                            Text(
                                text = "• ${live.hostHandle}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Meta: ${live.goalTitle}",
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "${formatCurrencyBrl(live.goalCurrentCents)} / ${formatCurrencyBrl(live.goalTargetCents)}",
                                fontFamily = JetBrainsMono,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(999.dp)),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GoLiveModalSheet(
    onDismiss: () -> Unit,
    onStartLive: (
        title: String,
        category: String,
        goalTitle: String,
        goalTargetCents: Long,
        supportersOnly: Boolean
    ) -> Unit
) {
    val categories = listOf("Conversa", "Música", "Tecnologia", "Culinária")
    var title by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Conversa") }
    var goalTitle by remember { mutableStateOf("Equipamento de Áudio") }
    var goalReais by remember { mutableStateOf("500") }
    var supportersOnly by remember { mutableStateOf(false) }

    val targetCents = (goalReais.replace(",", ".").toDoubleOrNull()?.times(100)?.toLong() ?: 50000L)
        .coerceAtLeast(1000L)

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Estúdio ao Vivo Pulso",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Título da transmissão") },
                placeholder = { Text("Ex: Tira-dúvidas e música ao vivo") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("go_live_title_input")
            )

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categories) { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat) }
                    )
                }
            }

            OutlinedTextField(
                value = goalTitle,
                onValueChange = { goalTitle = it },
                label = { Text("Meta de arrecadação") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = goalReais,
                onValueChange = { goalReais = it },
                label = { Text("Valor da meta (R$)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Exclusiva para Apoiadores", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Apenas assinantes do seu Clube acessam o chat VIP",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = supportersOnly,
                    onCheckedChange = { supportersOnly = it }
                )
            }

            Button(
                onClick = {
                    onStartLive(title, selectedCategory, goalTitle, targetCents, supportersOnly)
                },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("confirm_go_live_button")
            ) {
                Icon(Icons.Filled.Videocam, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Entrar ao vivo agora", fontWeight = FontWeight.Bold)
            }
        }
    }
}

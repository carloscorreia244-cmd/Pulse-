package com.example.ui.screens

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.crypto.E2EECryptoEngine
import com.example.data.local.UserEntity
import com.example.data.local.VerificationTier
import com.example.model.parseVerificationTier
import com.example.ui.components.PulsoAvatar
import com.example.ui.components.VerificationTierPill
import com.example.ui.components.VerifiedBadge
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.PulsoCreatorGold
import com.example.ui.theme.PulsoEmerald
import com.example.ui.theme.PulsoPartnerEmerald
import com.example.ui.theme.PulsoVerifiedBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    currentUser: UserEntity?,
    onSaveProfileAndBadge: (name: String, bio: String, tier: VerificationTier) -> Unit,
    onRotateKeys: () -> Unit,
    onExportAllBackup: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (currentUser == null) return

    var nameInput by remember(currentUser.id) { mutableStateOf(currentUser.displayName) }
    var bioInput by remember(currentUser.id) { mutableStateOf(currentUser.bio) }
    var selectedTier by remember(currentUser.id) {
        mutableStateOf(parseVerificationTier(currentUser.verificationTier))
    }

    LaunchedEffect(currentUser.verificationTier) {
        selectedTier = parseVerificationTier(currentUser.verificationTier)
    }

    val keyFingerprint = remember(currentUser.publicKeyBase64) {
        E2EECryptoEngine.shortFingerprint(currentUser.publicKeyBase64)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Perfil & Verificação",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
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
                // Header Profile Card
                item {
                    Card(
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                PulsoAvatar(
                                    name = currentUser.displayName,
                                    colorHex = currentUser.avatarColorHex,
                                    size = 64.dp,
                                    isOnline = true
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = currentUser.displayName,
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        VerifiedBadge(tierRaw = selectedTier.name, size = 18.dp)
                                    }
                                    Text(
                                        text = "${currentUser.handle} • ${currentUser.followersCount} seguidores",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    VerificationTierPill(tierRaw = selectedTier.name)
                                }
                            }

                            OutlinedTextField(
                                value = nameInput,
                                onValueChange = { nameInput = it },
                                label = { Text("Nome de exibição") },
                                leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("profile_name_input")
                            )
                            OutlinedTextField(
                                value = bioInput,
                                onValueChange = { bioInput = it },
                                label = { Text("Biografia do criador") },
                                maxLines = 2,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // Verification Badge Center
                item {
                    Card(
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Filled.Verified, contentDescription = null, tint = PulsoVerifiedBlue)
                                Text(
                                    text = "Central do Selo de Verificação",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Text(
                                text = "O selo autentica sua identidade criptográfica nas conversas, doações e transmissões ao vivo.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            BadgeTierRow(
                                title = "Selo Azul • Identidade Verificada",
                                subtitle = "Chave pública ECDH autenticada no dispositivo",
                                selected = selectedTier == VerificationTier.BLUE_VERIFIED,
                                color = PulsoVerifiedBlue,
                                onSelect = { selectedTier = VerificationTier.BLUE_VERIFIED }
                            )
                            BadgeTierRow(
                                title = "Selo Ouro • Criador Monetizado",
                                subtitle = "Habilitado para metas em Lives e Clube VIP",
                                selected = selectedTier == VerificationTier.GOLD_CREATOR,
                                color = PulsoCreatorGold,
                                onSelect = { selectedTier = VerificationTier.GOLD_CREATOR }
                            )
                            BadgeTierRow(
                                title = "Selo Esmeralda • Parceiro Oficial",
                                subtitle = "Destaque em comunidades e salas ao vivo",
                                selected = selectedTier == VerificationTier.EMERALD_PARTNER,
                                color = PulsoPartnerEmerald,
                                onSelect = { selectedTier = VerificationTier.EMERALD_PARTNER }
                            )

                            Button(
                                onClick = { onSaveProfileAndBadge(nameInput, bioInput, selectedTier) },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("save_profile_badge_button")
                            ) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Salvar perfil e selo", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // E2EE Master Key & Backup Download Card
                item {
                    Card(
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Filled.Lock, contentDescription = null, tint = PulsoEmerald)
                                Text(
                                    text = "Cofre Criptográfico & Download",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            val clipboard = LocalClipboardManager.current
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable {
                                        clipboard.setText(AnnotatedString(keyFingerprint))
                                    }
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Filled.Key, contentDescription = null, tint = PulsoEmerald, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "CHAVE PÚBLICA ECDH P-256",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = PulsoEmerald
                                            )
                                        }
                                        Icon(
                                            imageVector = Icons.Filled.ContentCopy,
                                            contentDescription = "Copiar chave pública",
                                            tint = PulsoEmerald,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                    Text(
                                        text = keyFingerprint,
                                        fontFamily = JetBrainsMono,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Armazenamento local seguro ativo (modo offline/dispositivo)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = onRotateKeys,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("rotate_keys_button")
                                ) {
                                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Girar chave")
                                }

                                Button(
                                    onClick = onExportAllBackup,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("download_all_backup_button")
                                ) {
                                    Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Baixar tudo")
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
private fun BadgeTierRow(
    title: String,
    subtitle: String,
    selected: Boolean,
    color: androidx.compose.ui.graphics.Color,
    onSelect: () -> Unit
) {
    Surface(
        color = if (selected) color.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onSelect)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(selected = selected, onClick = onSelect)
            Spacer(modifier = Modifier.width(6.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

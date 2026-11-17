package com.example.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.TransactionType
import com.example.data.local.UserEntity
import com.example.data.local.WalletTransactionEntity
import com.example.model.formatCurrencyBrl
import com.example.model.formatShortDate
import com.example.ui.components.DonationSheet
import com.example.ui.components.PulsoAvatar
import com.example.ui.components.VerifiedBadge
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.PulsoCreatorGold
import com.example.ui.theme.PulsoEmerald
import com.example.ui.theme.PulsoLiveCoral

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletScreen(
    currentUser: UserEntity?,
    contacts: List<UserEntity>,
    transactions: List<WalletTransactionEntity>,
    onTopUp: (Long) -> Unit,
    onWithdraw: (Long, String) -> Unit,
    onDonateToUser: (peerUserId: String, amountCents: Long, note: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showTopUpSheet by remember { mutableStateOf(false) }
    var showWithdrawSheet by remember { mutableStateOf(false) }
    var showSelectRecipientSheet by remember { mutableStateOf(false) }
    var donationRecipient by remember { mutableStateOf<UserEntity?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Carteira & Doações",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Monetização de lives e transferências P2P",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
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
                // Hero Balance Card
                item {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFF063D2D), Color(0xFF0B252C), Color(0xFF101C1F))
                                    )
                                )
                                .padding(20.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "SALDO DISPONÍVEL",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = PulsoEmerald
                                    )
                                    currentUser?.let {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = it.handle,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White.copy(alpha = 0.8f)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            VerifiedBadge(tierRaw = it.verificationTier, size = 14.dp)
                                        }
                                    }
                                }

                                Text(
                                    text = formatCurrencyBrl(currentUser?.walletBalanceCents ?: 0L),
                                    style = MaterialTheme.typography.displaySmall,
                                    fontFamily = JetBrainsMono,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    WalletActionButton(
                                        icon = Icons.Filled.AddCircle,
                                        label = "Adicionar",
                                        onClick = { showTopUpSheet = true },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("wallet_topup_button")
                                    )
                                    WalletActionButton(
                                        icon = Icons.Filled.VolunteerActivism,
                                        label = "Doar P2P",
                                        onClick = { showSelectRecipientSheet = true },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("wallet_donate_button")
                                    )
                                    WalletActionButton(
                                        icon = Icons.Filled.Payments,
                                        label = "Sacar PIX",
                                        onClick = { showWithdrawSheet = true },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("wallet_withdraw_button")
                                    )
                                }
                            }
                        }
                    }
                }

                // Revenue Breakdown Cards
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatMiniCard(
                            title = "Receita de Lives",
                            value = formatCurrencyBrl(currentUser?.totalLiveRevenueCents ?: 0L),
                            accent = PulsoLiveCoral,
                            modifier = Modifier.weight(1f)
                        )
                        StatMiniCard(
                            title = "Doações Recebidas",
                            value = formatCurrencyBrl(currentUser?.totalDonationsReceivedCents ?: 0L),
                            accent = PulsoEmerald,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    Text(
                        text = "Extrato & Comprovantes",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(transactions, key = { it.id }) { tx ->
                    TransactionRow(tx = tx)
                }
            }
        }
    }

    if (showTopUpSheet) {
        TopUpSheet(
            onDismiss = { showTopUpSheet = false },
            onConfirm = { cents ->
                showTopUpSheet = false
                onTopUp(cents)
            }
        )
    }

    if (showWithdrawSheet && currentUser != null) {
        WithdrawSheet(
            maxCents = currentUser.walletBalanceCents,
            onDismiss = { showWithdrawSheet = false },
            onConfirm = { cents, pixKey ->
                showWithdrawSheet = false
                onWithdraw(cents, pixKey)
            }
        )
    }

    if (showSelectRecipientSheet) {
        ModalBottomSheet(onDismissRequest = { showSelectRecipientSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Escolha quem apoiar",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                contacts.forEach { user ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                showSelectRecipientSheet = false
                                donationRecipient = user
                            }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PulsoAvatar(
                            name = user.displayName,
                            colorHex = user.avatarColorHex,
                            size = 44.dp,
                            isOnline = user.isOnline
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(user.displayName, fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.width(4.dp))
                                VerifiedBadge(tierRaw = user.verificationTier, size = 14.dp)
                            }
                            Text(
                                text = user.handle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    if (donationRecipient != null && currentUser != null) {
        val peer = donationRecipient!!
        DonationSheet(
            recipientName = peer.displayName,
            recipientHandle = peer.handle,
            recipientTier = peer.verificationTier,
            availableBalanceCents = currentUser.walletBalanceCents,
            onDismiss = { donationRecipient = null },
            onConfirmDonation = { cents, note ->
                donationRecipient = null
                onDonateToUser(peer.id, cents, note)
            }
        )
    }
}

@Composable
private fun WalletActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color.White.copy(alpha = 0.12f),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = PulsoEmerald, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                color = Color.White,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun StatMiniCard(
    title: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Bold,
                color = accent
            )
        }
    }
}

@Composable
private fun TransactionRow(tx: WalletTransactionEntity) {
    val isPositive = tx.amountCents >= 0L
    val icon = when (tx.type) {
        TransactionType.LIVE_REVENUE.name -> Icons.Filled.LiveTv
        TransactionType.SUBSCRIPTION.name -> Icons.Filled.Star
        TransactionType.DONATION_SENT.name -> Icons.Filled.ArrowUpward
        TransactionType.WITHDRAWAL.name -> Icons.Filled.Payments
        else -> Icons.Filled.ArrowDownward
    }
    val tint = if (isPositive) PulsoEmerald else MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tx.counterpartyName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = tx.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${formatShortDate(tx.timestampMillis)} • ${tx.referenceCode}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = (if (isPositive) "+" else "") + formatCurrencyBrl(tx.amountCents),
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleSmall,
                color = if (isPositive) PulsoEmerald else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopUpSheet(
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    val options = listOf(2500L, 5000L, 10000L, 25000L)
    var selected by remember { mutableLongStateOf(5000L) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Adicionar Saldo via PIX",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                options.forEach { cents ->
                    FilterChip(
                        selected = selected == cents,
                        onClick = { selected = cents },
                        label = { Text(formatCurrencyBrl(cents)) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Button(
                onClick = { onConfirm(selected) },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("confirm_topup_button")
            ) {
                Text("Confirmar recarga de ${formatCurrencyBrl(selected)}", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WithdrawSheet(
    maxCents: Long,
    onDismiss: () -> Unit,
    onConfirm: (Long, String) -> Unit
) {
    var reaisText by remember { mutableStateOf("50,00") }
    var pixKey by remember { mutableStateOf("") }
    val amountCents = reaisText.replace(",", ".").toDoubleOrNull()?.times(100)?.toLong() ?: 0L

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
                text = "Sacar Ganhos via PIX",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Máximo disponível: ${formatCurrencyBrl(maxCents)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
            OutlinedTextField(
                value = reaisText,
                onValueChange = { reaisText = it },
                label = { Text("Valor do saque (R$)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = pixKey,
                onValueChange = { pixKey = it },
                label = { Text("Chave PIX (CPF, e-mail ou aleatória)") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = { onConfirm(amountCents, pixKey) },
                enabled = amountCents > 0L && amountCents <= maxCents,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text("Solicitar transferência", fontWeight = FontWeight.Bold)
            }
        }
    }
}

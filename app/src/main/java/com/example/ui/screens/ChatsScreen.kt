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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ChatEntity
import com.example.data.local.LiveStreamEntity
import com.example.data.local.UserEntity
import com.example.data.local.VerificationTier
import com.example.model.formatClockTime
import com.example.ui.components.PulsoAvatar
import com.example.ui.components.VerifiedBadge
import com.example.ui.theme.PulsoEmerald

private enum class ChatFilter(val label: String) {
    ALL("Todas"),
    UNREAD("Não lidas"),
    VERIFIED("Verificados"),
    PINNED("Fixadas")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatsScreen(
    chats: List<ChatEntity>,
    contacts: List<UserEntity>,
    liveStreams: List<LiveStreamEntity>,
    onChatClick: (String) -> Unit,
    onLiveClick: (String) -> Unit,
    onStartChatWithContact: (UserEntity) -> Unit,
    onCreateNewContact: (name: String, handle: String) -> Unit,
    onExportAllChats: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var isSearchOpen by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf(ChatFilter.ALL) }
    var showNewChatSheet by remember { mutableStateOf(false) }

    val filteredChats = remember(chats, searchQuery, selectedFilter) {
        chats.filter { chat ->
            val matchesQuery = searchQuery.isBlank() ||
                chat.title.contains(searchQuery, ignoreCase = true) ||
                chat.peerHandle.contains(searchQuery, ignoreCase = true) ||
                chat.lastMessagePreview.contains(searchQuery, ignoreCase = true)

            val matchesFilter = when (selectedFilter) {
                ChatFilter.ALL -> true
                ChatFilter.UNREAD -> chat.unreadCount > 0
                ChatFilter.VERIFIED -> chat.peerVerificationTier != VerificationTier.NONE.name
                ChatFilter.PINNED -> chat.isPinned
            }
            matchesQuery && matchesFilter
        }
    }

    val activeLives = remember(liveStreams) { liveStreams.filter { it.isLive } }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    if (isSearchOpen) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Buscar conversas ou @usuário") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("chats_search_input")
                        )
                    } else {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Pulso",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Surface(
                                    color = PulsoEmerald.copy(alpha = 0.14f),
                                    shape = RoundedCornerShape(999.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
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
                                            text = "E2EE",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = PulsoEmerald
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (isSearchOpen) searchQuery = ""
                            isSearchOpen = !isSearchOpen
                        },
                        modifier = Modifier.testTag("toggle_search_button")
                    ) {
                        Icon(
                            imageVector = if (isSearchOpen) Icons.Filled.Close else Icons.Filled.Search,
                            contentDescription = "Buscar conversas"
                        )
                    }
                    IconButton(
                        onClick = onExportAllChats,
                        modifier = Modifier.testTag("export_all_chats_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Download,
                            contentDescription = "Baixar backup das mensagens"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showNewChatSheet = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("new_chat_fab")
            ) {
                Icon(
                    imageVector = Icons.Filled.AddComment,
                    contentDescription = "Nova conversa"
                )
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
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 680.dp),
                contentPadding = PaddingValues(bottom = 96.dp)
            ) {
                if (activeLives.isNotEmpty()) {
                    item {
                        Column(modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)) {
                            Text(
                                text = "Ao vivo agora",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                items(activeLives, key = { it.id }) { live ->
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .width(74.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { onLiveClick(live.id) }
                                            .padding(vertical = 4.dp)
                                            .testTag("live_story_${live.id}")
                                    ) {
                                        PulsoAvatar(
                                            name = live.hostName,
                                            colorHex = live.hostAvatarColorHex,
                                            size = 58.dp,
                                            isLive = true
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Text(
                                                text = live.hostName.substringBefore(" "),
                                                style = MaterialTheme.typography.bodySmall,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            VerifiedBadge(tierRaw = live.hostVerificationTier, size = 12.dp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(ChatFilter.entries) { filter ->
                            FilterChip(
                                selected = selectedFilter == filter,
                                onClick = { selectedFilter = filter },
                                label = { Text(filter.label) },
                                modifier = Modifier.testTag("chat_filter_${filter.name}")
                            )
                        }
                    }
                }

                if (filteredChats.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 32.dp, vertical = 48.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(44.dp)
                            )
                            Text(
                                text = "Nenhuma conversa encontrada",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Inicie um novo bate-papo com criptografia de ponta a ponta usando o botão abaixo.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(filteredChats, key = { it.id }) { chat ->
                        ChatRowItem(
                            chat = chat,
                            onClick = { onChatClick(chat.id) }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 82.dp, end = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }

    if (showNewChatSheet) {
        NewChatModalSheet(
            contacts = contacts,
            onDismiss = { showNewChatSheet = false },
            onSelectContact = { user ->
                showNewChatSheet = false
                onStartChatWithContact(user)
            },
            onCreateContact = { name, handle ->
                showNewChatSheet = false
                onCreateNewContact(name, handle)
            }
        )
    }
}

@Composable
private fun ChatRowItem(
    chat: ChatEntity,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("chat_row_${chat.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PulsoAvatar(
            name = chat.title,
            colorHex = chat.peerAvatarColorHex,
            size = 54.dp,
            isOnline = chat.isOnline
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = chat.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    VerifiedBadge(tierRaw = chat.peerVerificationTier, size = 16.dp)
                    if (chat.keyVerifiedByUser) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Filled.VerifiedUser,
                            contentDescription = "Chave E2EE verificada",
                            tint = PulsoEmerald,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = formatClockTime(chat.lastMessageTimeMillis),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (chat.unreadCount > 0) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = chat.lastMessagePreview,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (chat.isMuted) {
                        Icon(
                            imageVector = Icons.Filled.NotificationsOff,
                            contentDescription = "Silenciada",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    if (chat.isPinned) {
                        Icon(
                            imageVector = Icons.Filled.PushPin,
                            contentDescription = "Fixada",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    if (chat.unreadCount > 0) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = chat.unreadCount.toString(),
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewChatModalSheet(
    contacts: List<UserEntity>,
    onDismiss: () -> Unit,
    onSelectContact: (UserEntity) -> Unit,
    onCreateContact: (name: String, handle: String) -> Unit
) {
    var newName by remember { mutableStateOf("") }
    var newHandle by remember { mutableStateOf("") }

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
                text = "Nova Conversa E2EE",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Cada conversa gera automaticamente uma chave compartilhada ECDH P-256.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                label = { Text("Nome do contato") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("new_contact_name_input")
            )
            OutlinedTextField(
                value = newHandle,
                onValueChange = { newHandle = it },
                label = { Text("@usuário") },
                placeholder = { Text("@ana.silva") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("new_contact_handle_input")
            )
            Button(
                onClick = { onCreateContact(newName, newHandle) },
                enabled = newName.isNotBlank() && newHandle.isNotBlank(),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("create_contact_chat_button")
            ) {
                Text("Criar chave & iniciar conversa", fontWeight = FontWeight.SemiBold)
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            Text(
                text = "Contatos no Pulso",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            contacts.take(5).forEach { contact ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSelectContact(contact) }
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PulsoAvatar(
                        name = contact.displayName,
                        colorHex = contact.avatarColorHex,
                        size = 42.dp,
                        isOnline = contact.isOnline
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = contact.displayName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            VerifiedBadge(tierRaw = contact.verificationTier, size = 14.dp)
                        }
                        Text(
                            text = contact.handle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

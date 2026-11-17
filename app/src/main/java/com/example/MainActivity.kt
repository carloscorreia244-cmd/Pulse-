package com.example

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.LiveTv
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.local.AppDatabase
import com.example.data.repository.PulsoRepository
import com.example.ui.PulsoViewModel
import com.example.ui.screens.ChatDetailScreen
import com.example.ui.screens.ChatsScreen
import com.example.ui.screens.LiveRoomScreen
import com.example.ui.screens.LivesScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.WalletScreen
import com.example.ui.theme.PulsoTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private sealed class PulsoTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    data object Chats : PulsoTab("chats", "Conversas", Icons.AutoMirrored.Filled.Chat, Icons.AutoMirrored.Outlined.Chat)
    data object Lives : PulsoTab("lives", "Lives", Icons.Filled.LiveTv, Icons.Outlined.LiveTv)
    data object Wallet : PulsoTab("wallet", "Carteira", Icons.Filled.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet)
    data object Profile : PulsoTab("profile", "Perfil", Icons.Filled.VerifiedUser, Icons.Outlined.VerifiedUser)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PulsoTheme {
                PulsoRootApp()
            }
        }
    }
}

@Composable
fun PulsoRootApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val viewModel: PulsoViewModel = viewModel(
        factory = remember(context) {
            val db = AppDatabase.getInstance(context.applicationContext)
            PulsoViewModel.Factory(PulsoRepository(db))
        }
    )

    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val contacts by viewModel.contacts.collectAsStateWithLifecycle()
    val chats by viewModel.chats.collectAsStateWithLifecycle()
    val liveStreams by viewModel.liveStreams.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()

    var pendingExportChatId by remember { mutableStateOf<String?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val transcript = withContext(Dispatchers.IO) {
                    viewModel.buildExportTranscript(pendingExportChatId, includeCiphertext = true)
                }
                val saved = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openOutputStream(uri)?.use { stream ->
                            stream.write(transcript.toByteArray(Charsets.UTF_8))
                        }
                    }.isSuccess
                }
                viewModel.notifyMessage(
                    if (saved) "Mensagens baixadas com sucesso!"
                    else "Não foi possível salvar o arquivo."
                )
            }
        }
    }

    fun triggerExport(chatId: String?, fileName: String) {
        pendingExportChatId = chatId
        exportLauncher.launch(fileName)
    }

    LaunchedEffect(Unit) {
        viewModel.toastMessages.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: PulsoTab.Chats.route
    val tabs = listOf(PulsoTab.Chats, PulsoTab.Lives, PulsoTab.Wallet, PulsoTab.Profile)
    val showTopLevelNav = currentRoute in tabs.map { it.route }

    val totalUnread = remember(chats) { chats.sumOf { it.unreadCount } }
    val activeLiveCount = remember(liveStreams) { liveStreams.count { it.isLive } }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = {
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.navigationBarsPadding()
                )
            },
            bottomBar = {
                if (showTopLevelNav && !useRail) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        tabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(tab.route) {
                                        popUpTo(PulsoTab.Chats.route) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    TabIconWithBadge(
                                        tab = tab,
                                        selected = selected,
                                        totalUnread = totalUnread,
                                        activeLiveCount = activeLiveCount
                                    )
                                },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag("nav_tab_${tab.route}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (showTopLevelNav && useRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        tabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(tab.route) {
                                        popUpTo(PulsoTab.Chats.route) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    TabIconWithBadge(
                                        tab = tab,
                                        selected = selected,
                                        totalUnread = totalUnread,
                                        activeLiveCount = activeLiveCount
                                    )
                                },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag("nav_rail_${tab.route}")
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    NavHost(
                        navController = navController,
                        startDestination = PulsoTab.Chats.route
                    ) {
                        composable(PulsoTab.Chats.route) {
                            ChatsScreen(
                                chats = chats,
                                contacts = contacts,
                                liveStreams = liveStreams,
                                onChatClick = { chatId ->
                                    viewModel.markChatRead(chatId)
                                    navController.navigate("chat/$chatId")
                                },
                                onLiveClick = { liveId ->
                                    navController.navigate("live/$liveId")
                                },
                                onStartChatWithContact = { user ->
                                    viewModel.openOrCreateChat(user) { chatId ->
                                        navController.navigate("chat/$chatId")
                                    }
                                },
                                onCreateNewContact = { name, handle ->
                                    viewModel.createNewContactAndChat(name, handle) { chatId ->
                                        navController.navigate("chat/$chatId")
                                    }
                                },
                                onExportAllChats = {
                                    triggerExport(null, "pulso_backup_mensagens.txt")
                                }
                            )
                        }

                        composable(PulsoTab.Lives.route) {
                            LivesScreen(
                                liveStreams = liveStreams,
                                onOpenLive = { liveId ->
                                    navController.navigate("live/$liveId")
                                },
                                onStartMyLive = { title, cat, goalTitle, targetCents, supportersOnly ->
                                    viewModel.startMyLive(title, cat, goalTitle, targetCents, supportersOnly) { liveId ->
                                        navController.navigate("live/$liveId")
                                    }
                                }
                            )
                        }

                        composable(PulsoTab.Wallet.route) {
                            WalletScreen(
                                currentUser = currentUser,
                                contacts = contacts,
                                transactions = transactions,
                                onTopUp = { cents -> viewModel.addTopUp(cents) },
                                onWithdraw = { cents, pix -> viewModel.withdrawToPix(cents, pix) },
                                onDonateToUser = { peerId, cents, note ->
                                    viewModel.sendPeerDonation(peerId, cents, note)
                                }
                            )
                        }

                        composable(PulsoTab.Profile.route) {
                            ProfileScreen(
                                currentUser = currentUser,
                                onSaveProfileAndBadge = { name, bio, tier ->
                                    viewModel.updateProfileAndBadge(name, bio, tier)
                                },
                                onRotateKeys = { viewModel.rotateMasterKeys() },
                                onExportAllBackup = {
                                    triggerExport(null, "pulso_backup_completo.txt")
                                }
                            )
                        }

                        composable(
                            route = "chat/{chatId}",
                            arguments = listOf(navArgument("chatId") { type = NavType.StringType })
                        ) { entry ->
                            val chatId = entry.arguments?.getString("chatId").orEmpty()
                            val chat by viewModel.observeChat(chatId)
                                .collectAsStateWithLifecycle(initialValue = null)
                            val messages by viewModel.observeMessages(chatId)
                                .collectAsStateWithLifecycle(initialValue = emptyList())

                            LaunchedEffect(chatId) {
                                viewModel.markChatRead(chatId)
                            }

                            ChatDetailScreen(
                                chat = chat,
                                messages = messages,
                                currentUser = currentUser,
                                onBack = { navController.popBackStack() },
                                onSendText = { text, reply -> viewModel.sendTextMessage(chatId, text, reply) },
                                onSendImage = { uri -> viewModel.sendImageMessage(chatId, uri) },
                                onSendAudio = { uri, secs -> viewModel.sendAudioMessage(chatId, uri, secs) },
                                onToggleReaction = { msgId, reactions, emoji ->
                                    viewModel.toggleMessageReaction(msgId, reactions, emoji)
                                },
                                onDeleteMessage = { msgId -> viewModel.deleteMessage(msgId) },
                                onMarkDownloaded = { msgId -> viewModel.markMessageDownloaded(msgId) },
                                onTogglePin = { pinned -> viewModel.togglePinChat(chatId, pinned) },
                                onToggleVerifiedKey = { verified -> viewModel.setChatKeyVerified(chatId, verified) },
                                onSendDonation = { amountCents, note ->
                                    val peerId = chat?.peerUserId ?: return@ChatDetailScreen
                                    viewModel.sendPeerDonation(peerId, amountCents, note)
                                },
                                onExportTranscript = {
                                    val safeTitle = (chat?.title ?: "conversa").lowercase().replace(" ", "_")
                                    triggerExport(chatId, "pulso_${safeTitle}.txt")
                                },
                                onNotify = { msg -> viewModel.notifyMessage(msg) }
                            )
                        }

                        composable(
                            route = "live/{liveId}",
                            arguments = listOf(navArgument("liveId") { type = NavType.StringType })
                        ) { entry ->
                            val liveId = entry.arguments?.getString("liveId").orEmpty()
                            val live by viewModel.observeLiveStream(liveId)
                                .collectAsStateWithLifecycle(initialValue = null)
                            val comments by viewModel.observeLiveComments(liveId)
                                .collectAsStateWithLifecycle(initialValue = emptyList())

                            LiveRoomScreen(
                                live = live,
                                comments = comments,
                                currentUser = currentUser,
                                onClose = { navController.popBackStack() },
                                onSendComment = { text -> viewModel.sendLiveComment(liveId, text) },
                                onSendGift = { gift, note -> viewModel.sendLiveGift(liveId, gift, note) },
                                onSubscribeCreator = { viewModel.subscribeToLiveCreator(liveId) },
                                onEndLive = { viewModel.endMyLive(liveId) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TabIconWithBadge(
    tab: PulsoTab,
    selected: Boolean,
    totalUnread: Int,
    activeLiveCount: Int
) {
    val icon = if (selected) tab.selectedIcon else tab.unselectedIcon
    when (tab) {
        PulsoTab.Chats -> {
            if (totalUnread > 0) {
                BadgedBox(badge = { Badge { Text(totalUnread.toString()) } }) {
                    Icon(icon, contentDescription = tab.label)
                }
            } else {
                Icon(icon, contentDescription = tab.label)
            }
        }
        PulsoTab.Lives -> {
            if (activeLiveCount > 0) {
                BadgedBox(badge = { Badge { Text(activeLiveCount.toString()) } }) {
                    Icon(icon, contentDescription = tab.label)
                }
            } else {
                Icon(icon, contentDescription = tab.label)
            }
        }
        else -> {
            Icon(icon, contentDescription = tab.label)
        }
    }
}

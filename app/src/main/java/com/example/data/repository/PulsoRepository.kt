package com.example.data.repository

import androidx.room.withTransaction
import com.example.crypto.E2EECryptoEngine
import com.example.data.local.AppDatabase
import com.example.data.local.ChatEntity
import com.example.data.local.LiveCommentEntity
import com.example.data.local.LiveStreamEntity
import com.example.data.local.MessageEntity
import com.example.data.local.MessageStatus
import com.example.data.local.MessageType
import com.example.data.local.TransactionType
import com.example.data.local.UserEntity
import com.example.data.local.VerificationTier
import com.example.data.local.WalletTransactionEntity
import com.example.model.DecryptedMessage
import com.example.model.LiveGiftOption
import com.example.model.formatCurrencyBrl
import com.example.model.formatShortDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.util.UUID

class PulsoRepository(private val db: AppDatabase) {

    private val userDao = db.userDao()
    private val chatDao = db.chatDao()
    private val messageDao = db.messageDao()
    private val liveDao = db.liveStreamDao()
    private val liveCommentDao = db.liveCommentDao()
    private val walletDao = db.walletDao()

    fun observeCurrentUser(): Flow<UserEntity?> = userDao.observeCurrentUser()

    fun observeContacts(): Flow<List<UserEntity>> = userDao.observeContacts()

    fun observeChats(): Flow<List<ChatEntity>> = chatDao.observeAllChats()

    fun observeChat(chatId: String): Flow<ChatEntity?> = chatDao.observeChat(chatId)

    fun observeDecryptedMessages(chatId: String): Flow<List<DecryptedMessage>> {
        return combine(
            chatDao.observeChat(chatId),
            messageDao.observeMessagesForChat(chatId)
        ) { chat, rawMessages ->
            val secret = chat?.sharedSecretBase64 ?: return@combine emptyList()
            rawMessages.map { msg ->
                val plaintext = E2EECryptoEngine.decrypt(
                    ciphertextBase64 = msg.ciphertextBase64,
                    ivBase64 = msg.ivBase64,
                    sharedSecretBase64 = secret
                )
                DecryptedMessage(
                    id = msg.id,
                    chatId = msg.chatId,
                    senderId = msg.senderId,
                    senderName = msg.senderName,
                    isFromMe = msg.isFromMe,
                    plaintext = plaintext,
                    ciphertextBase64 = msg.ciphertextBase64,
                    ivBase64 = msg.ivBase64,
                    type = MessageType.entries.firstOrNull { it.name == msg.messageType } ?: MessageType.TEXT,
                    mediaUri = msg.mediaUri,
                    audioDurationSec = msg.audioDurationSec,
                    donationAmountCents = msg.donationAmountCents,
                    replyToPreview = msg.replyToPreview,
                    reactions = msg.reactions.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                    timestampMillis = msg.timestampMillis,
                    status = MessageStatus.entries.firstOrNull { it.name == msg.status } ?: MessageStatus.READ,
                    isDownloaded = msg.isDownloaded
                )
            }
        }
    }

    fun observeLiveStreams(): Flow<List<LiveStreamEntity>> = liveDao.observeLiveStreams()

    fun observeLiveStream(liveId: String): Flow<LiveStreamEntity?> = liveDao.observeLiveStream(liveId)

    fun observeLiveComments(liveId: String): Flow<List<LiveCommentEntity>> =
        liveCommentDao.observeComments(liveId)

    fun observeTransactions(): Flow<List<WalletTransactionEntity>> = walletDao.observeTransactions()

    suspend fun markChatRead(chatId: String) {
        chatDao.markChatRead(chatId)
    }

    suspend fun togglePinChat(chatId: String, pinned: Boolean) {
        chatDao.setPinned(chatId, pinned)
    }

    suspend fun toggleMuteChat(chatId: String, muted: Boolean) {
        chatDao.setMuted(chatId, muted)
    }

    suspend fun setChatKeyVerified(chatId: String, verified: Boolean) {
        chatDao.setKeyVerified(chatId, verified)
    }

    suspend fun sendTextMessage(
        chatId: String,
        text: String,
        replyToPreview: String? = null
    ) {
        val clean = text.trim()
        if (clean.isEmpty()) return
        val chat = chatDao.getChatById(chatId) ?: return
        val encrypted = E2EECryptoEngine.encrypt(clean, chat.sharedSecretBase64)
        val now = System.currentTimeMillis()
        val msg = MessageEntity(
            id = "msg_${UUID.randomUUID()}",
            chatId = chatId,
            senderId = "me",
            senderName = "Você",
            isFromMe = true,
            ciphertextBase64 = encrypted.ciphertextBase64,
            ivBase64 = encrypted.ivBase64,
            messageType = MessageType.TEXT.name,
            replyToPreview = replyToPreview,
            timestampMillis = now,
            status = MessageStatus.DELIVERED.name,
            isDownloaded = true
        )
        db.withTransaction {
            messageDao.insert(msg)
            chatDao.upsert(
                chat.copy(
                    lastMessagePreview = clean,
                    lastMessageTimeMillis = now
                )
            )
        }
    }

    suspend fun sendImageMessage(chatId: String, uriString: String, caption: String) {
        val chat = chatDao.getChatById(chatId) ?: return
        val body = caption.ifBlank { "Foto criptografada" }
        val encrypted = E2EECryptoEngine.encrypt(body, chat.sharedSecretBase64)
        val now = System.currentTimeMillis()
        val msg = MessageEntity(
            id = "msg_${UUID.randomUUID()}",
            chatId = chatId,
            senderId = "me",
            senderName = "Você",
            isFromMe = true,
            ciphertextBase64 = encrypted.ciphertextBase64,
            ivBase64 = encrypted.ivBase64,
            messageType = MessageType.IMAGE.name,
            mediaUri = uriString,
            timestampMillis = now,
            status = MessageStatus.DELIVERED.name,
            isDownloaded = true
        )
        db.withTransaction {
            messageDao.insert(msg)
            chatDao.upsert(
                chat.copy(
                    lastMessagePreview = "📷 $body",
                    lastMessageTimeMillis = now
                )
            )
        }
    }

    suspend fun sendAudioMessage(chatId: String, audioUri: String?, durationSec: Int) {
        val chat = chatDao.getChatById(chatId) ?: return
        val safeDuration = durationSec.coerceAtLeast(1)
        val body = "Mensagem de voz (${safeDuration}s)"
        val encrypted = E2EECryptoEngine.encrypt(body, chat.sharedSecretBase64)
        val now = System.currentTimeMillis()
        val msg = MessageEntity(
            id = "msg_${UUID.randomUUID()}",
            chatId = chatId,
            senderId = "me",
            senderName = "Você",
            isFromMe = true,
            ciphertextBase64 = encrypted.ciphertextBase64,
            ivBase64 = encrypted.ivBase64,
            messageType = MessageType.AUDIO.name,
            mediaUri = audioUri,
            audioDurationSec = safeDuration,
            timestampMillis = now,
            status = MessageStatus.DELIVERED.name,
            isDownloaded = true
        )
        db.withTransaction {
            messageDao.insert(msg)
            chatDao.upsert(
                chat.copy(
                    lastMessagePreview = "🎙️ Áudio (${safeDuration}s)",
                    lastMessageTimeMillis = now
                )
            )
        }
    }

    suspend fun toggleMessageReaction(messageId: String, currentReactions: List<String>, emoji: String) {
        val updated = if (currentReactions.contains(emoji)) {
            currentReactions.filterNot { it == emoji }
        } else {
            currentReactions + emoji
        }
        messageDao.updateReactions(messageId, updated.joinToString(","))
    }

    suspend fun markMessageDownloaded(messageId: String) {
        messageDao.markDownloaded(messageId)
    }

    suspend fun deleteMessage(messageId: String) {
        messageDao.deleteMessage(messageId)
    }

    suspend fun startOrGetChatWithUser(user: UserEntity): String {
        val existing = chatDao.getChatByPeerId(user.id)
        if (existing != null) return existing.id

        val me = userDao.getCurrentUser() ?: error("Current user missing")
        val sharedSecret = E2EECryptoEngine.deriveSharedSecretBase64(
            me.privateKeyBase64,
            user.publicKeyBase64
        )
        val safetyNumber = E2EECryptoEngine.computeSafetyNumber(
            me.publicKeyBase64,
            user.publicKeyBase64
        )
        val chatId = "chat_${UUID.randomUUID()}"
        val now = System.currentTimeMillis()
        val chat = ChatEntity(
            id = chatId,
            peerUserId = user.id,
            title = user.displayName,
            peerHandle = user.handle,
            peerVerificationTier = user.verificationTier,
            peerAvatarColorHex = user.avatarColorHex,
            isOnline = user.isOnline,
            lastMessagePreview = "Sessão E2EE iniciada",
            lastMessageTimeMillis = now,
            sharedSecretBase64 = sharedSecret,
            safetyNumber = safetyNumber,
            keyVerifiedByUser = false
        )
        val sysEnc = E2EECryptoEngine.encrypt(
            "As mensagens e doações desta conversa são protegidas com criptografia de ponta a ponta (ECDH P-256 + AES-256-GCM).",
            sharedSecret
        )
        val sysMsg = MessageEntity(
            id = "msg_${UUID.randomUUID()}",
            chatId = chatId,
            senderId = "system",
            senderName = "Pulso Segurança",
            isFromMe = false,
            ciphertextBase64 = sysEnc.ciphertextBase64,
            ivBase64 = sysEnc.ivBase64,
            messageType = MessageType.SYSTEM.name,
            timestampMillis = now
        )
        db.withTransaction {
            chatDao.upsert(chat)
            messageDao.insert(sysMsg)
        }
        return chatId
    }

    suspend fun createContactAndChat(displayName: String, handle: String): String {
        val cleanName = displayName.trim()
        val cleanHandle = handle.trim().let { if (it.startsWith("@")) it else "@$it" }
        val keys = E2EECryptoEngine.generateIdentityKeyPair()
        val colors = listOf(0xFF00A872, 0xFF1D9BF0, 0xFFFF3B6B, 0xFFF59E0B, 0xFF8B5CF6)
        val newUser = UserEntity(
            id = "user_${UUID.randomUUID()}",
            displayName = cleanName,
            handle = cleanHandle,
            bio = "Contato verificado por chave pública no Pulso",
            avatarColorHex = colors.random(),
            isCurrentUser = false,
            verificationTier = VerificationTier.NONE.name,
            publicKeyBase64 = keys.publicKeyBase64,
            isOnline = true,
            lastSeenLabel = "online agora"
        )
        userDao.upsert(newUser)
        return startOrGetChatWithUser(newUser)
    }

    suspend fun rotateMasterIdentityKeys() {
        val me = userDao.getCurrentUser() ?: return
        val newPair = E2EECryptoEngine.generateIdentityKeyPair()
        val updatedMe = me.copy(
            publicKeyBase64 = newPair.publicKeyBase64,
            privateKeyBase64 = newPair.privateKeyBase64
        )
        userDao.update(updatedMe)
    }

    /**
     * Sends a user-to-user donation inside a chat or directly to a peer user.
     */
    suspend fun sendPeerDonation(
        peerUserId: String,
        amountCents: Long,
        note: String
    ): Result<Unit> {
        if (amountCents <= 0L) return Result.failure(IllegalArgumentException("Valor inválido."))
        val me = userDao.getCurrentUser() ?: return Result.failure(IllegalStateException("Usuário não encontrado."))
        if (me.walletBalanceCents < amountCents) {
            return Result.failure(IllegalStateException("Saldo insuficiente na carteira Pulso."))
        }
        val peer = userDao.getUserById(peerUserId)
            ?: return Result.failure(IllegalStateException("Destinatário não encontrado."))

        val chatId = startOrGetChatWithUser(peer)
        val chat = chatDao.getChatById(chatId)!!
        val cleanNote = note.trim().ifBlank { "Doação direta via Pulso" }
        val encrypted = E2EECryptoEngine.encrypt(cleanNote, chat.sharedSecretBase64)
        val now = System.currentTimeMillis()
        val txRef = "PLS-DP-${(1000..9999).random()}"

        val donationMsg = MessageEntity(
            id = "msg_${UUID.randomUUID()}",
            chatId = chatId,
            senderId = "me",
            senderName = "Você",
            isFromMe = true,
            ciphertextBase64 = encrypted.ciphertextBase64,
            ivBase64 = encrypted.ivBase64,
            messageType = MessageType.DONATION.name,
            donationAmountCents = amountCents,
            timestampMillis = now,
            status = MessageStatus.READ.name,
            isDownloaded = true
        )

        val tx = WalletTransactionEntity(
            id = "tx_${UUID.randomUUID()}",
            type = TransactionType.DONATION_SENT.name,
            counterpartyName = peer.displayName,
            counterpartyHandle = peer.handle,
            amountCents = -amountCents,
            note = "$cleanNote • Ref $txRef",
            referenceCode = txRef,
            timestampMillis = now
        )

        db.withTransaction {
            userDao.update(
                me.copy(
                    walletBalanceCents = me.walletBalanceCents - amountCents,
                    totalDonationsSentCents = me.totalDonationsSentCents + amountCents
                )
            )
            userDao.update(
                peer.copy(
                    walletBalanceCents = peer.walletBalanceCents + amountCents,
                    totalDonationsReceivedCents = peer.totalDonationsReceivedCents + amountCents
                )
            )
            messageDao.insert(donationMsg)
            chatDao.upsert(
                chat.copy(
                    lastMessagePreview = "💚 Doação enviada: ${formatCurrencyBrl(amountCents)}",
                    lastMessageTimeMillis = now
                )
            )
            walletDao.insert(tx)
        }
        return Result.success(Unit)
    }

    suspend fun addWalletTopUp(amountCents: Long): Result<Unit> {
        if (amountCents <= 0L) return Result.failure(IllegalArgumentException("Valor inválido."))
        val me = userDao.getCurrentUser() ?: return Result.failure(IllegalStateException("Usuário não encontrado."))
        val now = System.currentTimeMillis()
        val txRef = "PLS-PIX-${(1000..9999).random()}"
        db.withTransaction {
            userDao.update(me.copy(walletBalanceCents = me.walletBalanceCents + amountCents))
            walletDao.insert(
                WalletTransactionEntity(
                    id = "tx_${UUID.randomUUID()}",
                    type = TransactionType.TOP_UP.name,
                    counterpartyName = "Recarga PIX Instantânea",
                    counterpartyHandle = me.handle,
                    amountCents = amountCents,
                    note = "Crédito adicionado à Carteira Pulso",
                    referenceCode = txRef,
                    timestampMillis = now
                )
            )
        }
        return Result.success(Unit)
    }

    suspend fun withdrawToPix(amountCents: Long, pixKey: String): Result<Unit> {
        if (amountCents <= 0L) return Result.failure(IllegalArgumentException("Valor inválido."))
        val me = userDao.getCurrentUser() ?: return Result.failure(IllegalStateException("Usuário não encontrado."))
        if (me.walletBalanceCents < amountCents) {
            return Result.failure(IllegalStateException("Saldo insuficiente para saque."))
        }
        val cleanPix = pixKey.trim().ifBlank { "Chave PIX padrão" }
        val now = System.currentTimeMillis()
        val txRef = "PLS-SQ-${(1000..9999).random()}"
        db.withTransaction {
            userDao.update(me.copy(walletBalanceCents = me.walletBalanceCents - amountCents))
            walletDao.insert(
                WalletTransactionEntity(
                    id = "tx_${UUID.randomUUID()}",
                    type = TransactionType.WITHDRAWAL.name,
                    counterpartyName = "Saque PIX ($cleanPix)",
                    counterpartyHandle = me.handle,
                    amountCents = -amountCents,
                    note = "Transferência de ganhos de lives e doações",
                    referenceCode = txRef,
                    timestampMillis = now
                )
            )
        }
        return Result.success(Unit)
    }

    suspend fun sendLiveComment(liveId: String, text: String) {
        val clean = text.trim()
        if (clean.isEmpty()) return
        val me = userDao.getCurrentUser() ?: return
        liveCommentDao.insert(
            LiveCommentEntity(
                id = "lc_${UUID.randomUUID()}",
                liveId = liveId,
                authorName = me.displayName,
                authorVerificationTier = me.verificationTier,
                text = clean,
                timestampMillis = System.currentTimeMillis()
            )
        )
    }

    suspend fun sendLiveGift(liveId: String, gift: LiveGiftOption, message: String): Result<Unit> {
        val me = userDao.getCurrentUser() ?: return Result.failure(IllegalStateException("Usuário não encontrado."))
        if (me.walletBalanceCents < gift.amountCents) {
            return Result.failure(IllegalStateException("Saldo insuficiente para enviar ${gift.label}."))
        }
        val live = liveDao.getLiveStream(liveId) ?: return Result.failure(IllegalStateException("Live não encontrada."))
        val now = System.currentTimeMillis()
        val txRef = "PLS-LVG-${(1000..9999).random()}"
        val cleanNote = message.trim().ifBlank { "Enviou ${gift.emoji} ${gift.label}!" }

        db.withTransaction {
            if (!live.isHostSelf) {
                userDao.update(
                    me.copy(
                        walletBalanceCents = me.walletBalanceCents - gift.amountCents,
                        totalDonationsSentCents = me.totalDonationsSentCents + gift.amountCents
                    )
                )
            }
            liveDao.upsert(
                live.copy(
                    goalCurrentCents = live.goalCurrentCents + gift.amountCents,
                    totalEarnedCents = live.totalEarnedCents + gift.amountCents
                )
            )
            liveCommentDao.insert(
                LiveCommentEntity(
                    id = "lc_${UUID.randomUUID()}",
                    liveId = liveId,
                    authorName = me.displayName,
                    authorVerificationTier = me.verificationTier,
                    text = cleanNote,
                    isSuperDonation = true,
                    giftEmoji = gift.emoji,
                    donationCents = gift.amountCents,
                    timestampMillis = now
                )
            )
            walletDao.insert(
                WalletTransactionEntity(
                    id = "tx_${UUID.randomUUID()}",
                    type = TransactionType.DONATION_SENT.name,
                    counterpartyName = "${live.hostName} (${gift.emoji} ${gift.label})",
                    counterpartyHandle = live.hostHandle,
                    amountCents = -gift.amountCents,
                    note = "Super Doação na Live: ${live.title}",
                    referenceCode = txRef,
                    timestampMillis = now
                )
            )
        }
        return Result.success(Unit)
    }

    suspend fun subscribeToLiveCreator(liveId: String): Result<Unit> {
        val me = userDao.getCurrentUser() ?: return Result.failure(IllegalStateException("Usuário não encontrado."))
        val live = liveDao.getLiveStream(liveId) ?: return Result.failure(IllegalStateException("Live não encontrada."))
        if (live.isSubscribedByMe) return Result.success(Unit)
        if (me.walletBalanceCents < live.subscriptionPriceCents) {
            return Result.failure(IllegalStateException("Saldo insuficiente para assinar o Clube de Apoiadores."))
        }
        val now = System.currentTimeMillis()
        val txRef = "PLS-SUB-${(1000..9999).random()}"

        db.withTransaction {
            userDao.update(
                me.copy(
                    walletBalanceCents = me.walletBalanceCents - live.subscriptionPriceCents,
                    totalDonationsSentCents = me.totalDonationsSentCents + live.subscriptionPriceCents
                )
            )
            liveDao.upsert(
                live.copy(
                    isSubscribedByMe = true,
                    totalEarnedCents = live.totalEarnedCents + live.subscriptionPriceCents,
                    goalCurrentCents = live.goalCurrentCents + live.subscriptionPriceCents
                )
            )
            liveCommentDao.insert(
                LiveCommentEntity(
                    id = "lc_${UUID.randomUUID()}",
                    liveId = liveId,
                    authorName = me.displayName,
                    authorVerificationTier = me.verificationTier,
                    text = "Assinou o Clube de Apoiadores! ⭐",
                    isSuperDonation = true,
                    giftEmoji = "⭐",
                    donationCents = live.subscriptionPriceCents,
                    timestampMillis = now
                )
            )
            walletDao.insert(
                WalletTransactionEntity(
                    id = "tx_${UUID.randomUUID()}",
                    type = TransactionType.SUBSCRIPTION.name,
                    counterpartyName = "Clube ${live.hostName}",
                    counterpartyHandle = live.hostHandle,
                    amountCents = -live.subscriptionPriceCents,
                    note = "Assinatura mensal de apoiador verificado",
                    referenceCode = txRef,
                    timestampMillis = now
                )
            )
        }
        return Result.success(Unit)
    }

    suspend fun startMyLive(
        title: String,
        category: String,
        goalTitle: String,
        goalTargetCents: Long,
        supportersOnly: Boolean
    ): String {
        val me = userDao.getCurrentUser() ?: error("Usuário não encontrado")
        val liveId = "live_self_${UUID.randomUUID()}"
        val now = System.currentTimeMillis()
        val stream = LiveStreamEntity(
            id = liveId,
            hostUserId = me.id,
            hostName = me.displayName,
            hostHandle = me.handle,
            hostVerificationTier = me.verificationTier,
            hostAvatarColorHex = me.avatarColorHex,
            title = title.trim().ifBlank { "Minha Transmissão ao Vivo" },
            description = "Transmissão ao vivo com chat criptografado e metas de apoio.",
            category = category,
            thumbnailKey = "studio",
            viewerCount = 24,
            isLive = true,
            isHostSelf = true,
            isSupportersOnly = supportersOnly,
            goalTitle = goalTitle.trim().ifBlank { "Meta da Comunidade" },
            goalTargetCents = goalTargetCents.coerceAtLeast(1000L),
            goalCurrentCents = 0L,
            totalEarnedCents = 0L,
            startedAtMillis = now
        )
        val welcome = LiveCommentEntity(
            id = "lc_${UUID.randomUUID()}",
            liveId = liveId,
            authorName = "Pulso Estúdio",
            authorVerificationTier = VerificationTier.EMERALD_PARTNER.name,
            text = "Transmissão iniciada! Doações e presentes vão direto para sua Carteira Pulso.",
            timestampMillis = now
        )
        db.withTransaction {
            liveDao.upsert(stream)
            liveCommentDao.insert(welcome)
        }
        return liveId
    }

    suspend fun endMyLive(liveId: String) {
        val live = liveDao.getLiveStream(liveId) ?: return
        val me = userDao.getCurrentUser()
        db.withTransaction {
            liveDao.endLive(liveId)
            if (live.isHostSelf && live.totalEarnedCents > 0L && me != null) {
                userDao.update(
                    me.copy(
                        walletBalanceCents = me.walletBalanceCents + live.totalEarnedCents,
                        totalLiveRevenueCents = me.totalLiveRevenueCents + live.totalEarnedCents
                    )
                )
                walletDao.insert(
                    WalletTransactionEntity(
                        id = "tx_${UUID.randomUUID()}",
                        type = TransactionType.LIVE_REVENUE.name,
                        counterpartyName = "Live: ${live.title}",
                        counterpartyHandle = me.handle,
                        amountCents = live.totalEarnedCents,
                        note = "Repasse automático da transmissão encerrada",
                        referenceCode = "PLS-LV-${(1000..9999).random()}",
                        timestampMillis = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    suspend fun updateProfileAndBadge(
        displayName: String,
        bio: String,
        verificationTier: VerificationTier
    ) {
        val me = userDao.getCurrentUser() ?: return
        userDao.update(
            me.copy(
                displayName = displayName.trim().ifBlank { me.displayName },
                bio = bio.trim(),
                verificationTier = verificationTier.name
            )
        )
    }

    /**
     * Generates a downloadable transcript of a single chat or all chats (for SAF CreateDocument).
     */
    suspend fun buildChatExportText(chatId: String?, includeCiphertext: Boolean): String {
        val chats = if (chatId != null) {
            listOfNotNull(chatDao.getChatById(chatId))
        } else {
            // export all
            val allMsgs = messageDao.getAllMessages()
            val chatIds = allMsgs.map { it.chatId }.distinct()
            chatIds.mapNotNull { chatDao.getChatById(it) }
        }

        return buildString {
            appendLine("=== PULSO • EXPORTAÇÃO SEGURA DE MENSAGENS ===")
            appendLine("Protocolo: ECDH secp256r1 + AES-256-GCM")
            appendLine("Exportado em: ${formatShortDate(System.currentTimeMillis())}")
            appendLine()
            for (chat in chats) {
                appendLine("--------------------------------------------------")
                appendLine("Conversa: ${chat.title} (${chat.peerHandle})")
                appendLine("Número de Segurança: ${chat.safetyNumber}")
                appendLine("Status da Chave: ${if (chat.keyVerifiedByUser) "Verificada" else "Pendente"}")
                appendLine()
                val msgs = messageDao.getMessagesForChat(chat.id)
                for (msg in msgs) {
                    val plain = E2EECryptoEngine.decrypt(msg.ciphertextBase64, msg.ivBase64, chat.sharedSecretBase64)
                    val time = formatShortDate(msg.timestampMillis)
                    val prefix = if (msg.donationAmountCents > 0) {
                        "[DOAÇÃO ${formatCurrencyBrl(msg.donationAmountCents)}] "
                    } else ""
                    appendLine("[$time] ${msg.senderName}: $prefix$plain")
                    if (includeCiphertext) {
                        appendLine("    AES-GCM IV=${msg.ivBase64} | CIPHER=${msg.ciphertextBase64.take(40)}...")
                    }
                    messageDao.markDownloaded(msg.id)
                }
                appendLine()
            }
        }
    }
}

package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.crypto.E2EECryptoEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@Database(
    entities = [
        UserEntity::class,
        ChatEntity::class,
        MessageEntity::class,
        LiveStreamEntity::class,
        LiveCommentEntity::class,
        WalletTransactionEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun chatDao(): ChatDao
    abstract fun messageDao(): MessageDao
    abstract fun liveStreamDao(): LiveStreamDao
    abstract fun liveCommentDao(): LiveCommentDao
    abstract fun walletDao(): WalletDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context.applicationContext).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(appContext: Context): AppDatabase {
            val callback = object : Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                        seedInitialData(getInstance(appContext))
                    }
                }
            }
            return Room.databaseBuilder(appContext, AppDatabase::class.java, "pulso_e2ee.db")
                .addCallback(callback)
                .build()
        }

        suspend fun seedInitialData(db: AppDatabase) {
            if (db.userDao().getCurrentUser() != null) return

            val now = System.currentTimeMillis()
            val myKeys = E2EECryptoEngine.generateIdentityKeyPair()
            val camilaKeys = E2EECryptoEngine.generateIdentityKeyPair()
            val rafaelKeys = E2EECryptoEngine.generateIdentityKeyPair()
            val marinaKeys = E2EECryptoEngine.generateIdentityKeyPair()
            val lucasKeys = E2EECryptoEngine.generateIdentityKeyPair()
            val grupoKeys = E2EECryptoEngine.generateIdentityKeyPair()

            val me = UserEntity(
                id = "me",
                displayName = "Carlos Correia",
                handle = "@carlos.pulso",
                bio = "Criador digital & entusiasta de privacidade E2EE.",
                avatarColorHex = 0xFF00A872,
                isCurrentUser = true,
                verificationTier = VerificationTier.BLUE_VERIFIED.name,
                publicKeyBase64 = myKeys.publicKeyBase64,
                privateKeyBase64 = myKeys.privateKeyBase64,
                isOnline = true,
                lastSeenLabel = "online agora",
                followersCount = 1280,
                walletBalanceCents = 48500L, // R$ 485,00
                totalDonationsReceivedCents = 19200L,
                totalLiveRevenueCents = 34000L,
                totalDonationsSentCents = 12500L
            )

            val camila = UserEntity(
                id = "user_camila",
                displayName = "Camila Duarte",
                handle = "@camiladuarte.som",
                bio = "Cantora & compositora • Lives acústicas toda quinta",
                avatarColorHex = 0xFFFF3B6B,
                verificationTier = VerificationTier.GOLD_CREATOR.name,
                publicKeyBase64 = camilaKeys.publicKeyBase64,
                isOnline = true,
                lastSeenLabel = "ao vivo agora",
                followersCount = 48200
            )

            val rafael = UserEntity(
                id = "user_rafael",
                displayName = "Rafael Mendes",
                handle = "@rafadev",
                bio = "Engenheiro de Segurança & Criptografia Aplicada",
                avatarColorHex = 0xFF1D9BF0,
                verificationTier = VerificationTier.EMERALD_PARTNER.name,
                publicKeyBase64 = rafaelKeys.publicKeyBase64,
                isOnline = true,
                lastSeenLabel = "online agora",
                followersCount = 29400
            )

            val marina = UserEntity(
                id = "user_marina",
                displayName = "Chef Marina Costa",
                handle = "@marinacozinha",
                bio = "Gastronomia afetiva ao vivo • Receitas e dicas",
                avatarColorHex = 0xFFF59E0B,
                verificationTier = VerificationTier.GOLD_CREATOR.name,
                publicKeyBase64 = marinaKeys.publicKeyBase64,
                isOnline = false,
                lastSeenLabel = "visto hoje às 15:40",
                followersCount = 63100
            )

            val lucas = UserEntity(
                id = "user_lucas",
                displayName = "Lucas Ferreira",
                handle = "@lucasferreira",
                bio = "Produtor audiovisual & designer de interfaces",
                avatarColorHex = 0xFF8B5CF6,
                verificationTier = VerificationTier.BLUE_VERIFIED.name,
                publicKeyBase64 = lucasKeys.publicKeyBase64,
                isOnline = false,
                lastSeenLabel = "visto hoje às 14:12",
                followersCount = 4150
            )

            val grupo = UserEntity(
                id = "user_grupo_criadores",
                displayName = "Clube Criadores Brasil",
                handle = "@clubecriadores",
                bio = "Comunidade oficial de criadores verificados no Pulso",
                avatarColorHex = 0xFF14B8A6,
                verificationTier = VerificationTier.EMERALD_PARTNER.name,
                publicKeyBase64 = grupoKeys.publicKeyBase64,
                isOnline = true,
                lastSeenLabel = "14 membros online",
                followersCount = 980
            )

            db.userDao().upsertAll(listOf(me, camila, rafael, marina, lucas, grupo))

            // Derive real ECDH shared secrets and encrypt all seeded messages
            val secretCamila = E2EECryptoEngine.deriveSharedSecretBase64(myKeys.privateKeyBase64, camila.publicKeyBase64)
            val secretRafael = E2EECryptoEngine.deriveSharedSecretBase64(myKeys.privateKeyBase64, rafael.publicKeyBase64)
            val secretMarina = E2EECryptoEngine.deriveSharedSecretBase64(myKeys.privateKeyBase64, marina.publicKeyBase64)
            val secretLucas = E2EECryptoEngine.deriveSharedSecretBase64(myKeys.privateKeyBase64, lucas.publicKeyBase64)
            val secretGrupo = E2EECryptoEngine.deriveSharedSecretBase64(myKeys.privateKeyBase64, grupo.publicKeyBase64)

            val chatCamila = ChatEntity(
                id = "chat_camila",
                peerUserId = camila.id,
                title = camila.displayName,
                peerHandle = camila.handle,
                peerVerificationTier = camila.verificationTier,
                peerAvatarColorHex = camila.avatarColorHex,
                isPinned = true,
                isOnline = true,
                unreadCount = 2,
                lastMessagePreview = "Vou entrar ao vivo em 5 min! Obrigada pelo apoio ❤️",
                lastMessageTimeMillis = now - 3 * 60_000L,
                sharedSecretBase64 = secretCamila,
                safetyNumber = E2EECryptoEngine.computeSafetyNumber(myKeys.publicKeyBase64, camila.publicKeyBase64),
                keyVerifiedByUser = true
            )

            val chatRafael = ChatEntity(
                id = "chat_rafael",
                peerUserId = rafael.id,
                title = rafael.displayName,
                peerHandle = rafael.handle,
                peerVerificationTier = rafael.verificationTier,
                peerAvatarColorHex = rafael.avatarColorHex,
                isPinned = true,
                isOnline = true,
                unreadCount = 0,
                lastMessagePreview = "Chaves ECDH P-256 sincronizadas com sucesso.",
                lastMessageTimeMillis = now - 25 * 60_000L,
                sharedSecretBase64 = secretRafael,
                safetyNumber = E2EECryptoEngine.computeSafetyNumber(myKeys.publicKeyBase64, rafael.publicKeyBase64),
                keyVerifiedByUser = true
            )

            val chatMarina = ChatEntity(
                id = "chat_marina",
                peerUserId = marina.id,
                title = marina.displayName,
                peerHandle = marina.handle,
                peerVerificationTier = marina.verificationTier,
                peerAvatarColorHex = marina.avatarColorHex,
                isOnline = false,
                unreadCount = 1,
                lastMessagePreview = "Você enviou uma doação de R$ 25,00",
                lastMessageTimeMillis = now - 2 * 3600_000L,
                sharedSecretBase64 = secretMarina,
                safetyNumber = E2EECryptoEngine.computeSafetyNumber(myKeys.publicKeyBase64, marina.publicKeyBase64),
                keyVerifiedByUser = false
            )

            val chatGrupo = ChatEntity(
                id = "chat_grupo",
                peerUserId = grupo.id,
                title = grupo.displayName,
                peerHandle = grupo.handle,
                peerVerificationTier = grupo.verificationTier,
                peerAvatarColorHex = grupo.avatarColorHex,
                isGroup = true,
                isOnline = true,
                unreadCount = 3,
                lastMessagePreview = "Bate-papo de monetização para lives hoje às 20h!",
                lastMessageTimeMillis = now - 4 * 3600_000L,
                sharedSecretBase64 = secretGrupo,
                safetyNumber = E2EECryptoEngine.computeSafetyNumber(myKeys.publicKeyBase64, grupo.publicKeyBase64),
                keyVerifiedByUser = true
            )

            val chatLucas = ChatEntity(
                id = "chat_lucas",
                peerUserId = lucas.id,
                title = lucas.displayName,
                peerHandle = lucas.handle,
                peerVerificationTier = lucas.verificationTier,
                peerAvatarColorHex = lucas.avatarColorHex,
                isOnline = false,
                unreadCount = 0,
                lastMessagePreview = "Baixei o áudio da nossa última reunião.",
                lastMessageTimeMillis = now - 26 * 3600_000L,
                sharedSecretBase64 = secretLucas,
                safetyNumber = E2EECryptoEngine.computeSafetyNumber(myKeys.publicKeyBase64, lucas.publicKeyBase64),
                keyVerifiedByUser = false
            )

            db.chatDao().upsertAll(listOf(chatCamila, chatRafael, chatMarina, chatGrupo, chatLucas))

            fun encMsg(
                id: String,
                chatId: String,
                senderId: String,
                senderName: String,
                isFromMe: Boolean,
                secret: String,
                body: String,
                type: MessageType = MessageType.TEXT,
                donationCents: Long = 0L,
                audioSec: Int = 0,
                reactions: String = "",
                offsetMin: Long
            ): MessageEntity {
                val enc = E2EECryptoEngine.encrypt(body, secret)
                return MessageEntity(
                    id = id,
                    chatId = chatId,
                    senderId = senderId,
                    senderName = senderName,
                    isFromMe = isFromMe,
                    ciphertextBase64 = enc.ciphertextBase64,
                    ivBase64 = enc.ivBase64,
                    messageType = type.name,
                    audioDurationSec = audioSec,
                    donationAmountCents = donationCents,
                    reactions = reactions,
                    timestampMillis = now - offsetMin * 60_000L,
                    status = MessageStatus.READ.name,
                    isDownloaded = true
                )
            }

            val seedMessages = listOf(
                encMsg("m1", "chat_camila", "user_camila", "Camila Duarte", false, secretCamila,
                    "Oi Carlos! Tudo pronto para a live acústica de hoje?", offsetMin = 42),
                encMsg("m2", "chat_camila", "me", "Você", true, secretCamila,
                    "Tudo certo! Já ativei meu selo verificado e testei a criptografia.", reactions = "🔥", offsetMin = 38),
                encMsg("m3", "chat_camila", "me", "Você", true, secretCamila,
                    "Doação enviada para aquecer a meta da live!", type = MessageType.DONATION, donationCents = 2500L, reactions = "❤️", offsetMin = 20),
                encMsg("m4", "chat_camila", "user_camila", "Camila Duarte", false, secretCamila,
                    "Vou entrar ao vivo em 5 min! Obrigada pelo apoio ❤️", offsetMin = 3),

                encMsg("m5", "chat_rafael", "user_rafael", "Rafael Mendes", false, secretRafael,
                    "Fala Carlos! Testando a troca de chaves ECDH P-256 + AES-256-GCM.", offsetMin = 90),
                encMsg("m6", "chat_rafael", "me", "Você", true, secretRafael,
                    "Show! Confirmei o número de segurança de 60 dígitos aqui.", offsetMin = 65),
                encMsg("m7", "chat_rafael", "user_rafael", "Rafael Mendes", false, secretRafael,
                    "Chaves ECDH P-256 sincronizadas com sucesso.", reactions = "🔐", offsetMin = 25),

                encMsg("m8", "chat_marina", "user_marina", "Chef Marina Costa", false, secretMarina,
                    "Oi! Vou ensinar massa artesanal recheada na live de hoje.", offsetMin = 180),
                encMsg("m9", "chat_marina", "me", "Você", true, secretMarina,
                    "Apoio para os ingredientes da transmissão!", type = MessageType.DONATION, donationCents = 2500L, offsetMin = 120),

                encMsg("m10", "chat_grupo", "user_rafael", "Rafael Mendes", false, secretGrupo,
                    "Bem-vindos ao grupo oficial de criadores com criptografia ponta a ponta.", offsetMin = 300),
                encMsg("m11", "chat_grupo", "user_camila", "Camila Duarte", false, secretGrupo,
                    "Bate-papo de monetização para lives hoje às 20h!", reactions = "👏,🚀", offsetMin = 240),

                encMsg("m12", "chat_lucas", "user_lucas", "Lucas Ferreira", false, secretLucas,
                    "Áudio criptografado com resumo da pauta de gravação.", type = MessageType.AUDIO, audioSec = 18, offsetMin = 1600),
                encMsg("m13", "chat_lucas", "me", "Você", true, secretLucas,
                    "Baixei o áudio da nossa última reunião.", offsetMin = 1560)
            )
            db.messageDao().insertAll(seedMessages)

            // Seed active Lives
            val lives = listOf(
                LiveStreamEntity(
                    id = "live_camila",
                    hostUserId = camila.id,
                    hostName = camila.displayName,
                    hostHandle = camila.handle,
                    hostVerificationTier = camila.verificationTier,
                    hostAvatarColorHex = camila.avatarColorHex,
                    title = "Sessão Acústica & Pedidos ao Vivo",
                    description = "Toco suas músicas favoritas! Envie um presente para escolher a próxima faixa.",
                    category = "Música",
                    thumbnailKey = "music",
                    viewerCount = 1420,
                    isLive = true,
                    isSupportersOnly = false,
                    goalTitle = "Novo Microfone de Estúdio",
                    goalTargetCents = 150000L,
                    goalCurrentCents = 98500L,
                    totalEarnedCents = 98500L,
                    startedAtMillis = now - 32 * 60_000L
                ),
                LiveStreamEntity(
                    id = "live_rafael",
                    hostUserId = rafael.id,
                    hostName = rafael.displayName,
                    hostHandle = rafael.handle,
                    hostVerificationTier = rafael.verificationTier,
                    hostAvatarColorHex = rafael.avatarColorHex,
                    title = "Arquitetura de Criptografia & Kotlin Ao Vivo",
                    description = "Construindo protocolo E2EE do zero com ECDH e AES-GCM + Q&A com apoiadores.",
                    category = "Tecnologia",
                    thumbnailKey = "tech",
                    viewerCount = 865,
                    isLive = true,
                    isSupportersOnly = false,
                    goalTitle = "Servidor Open-Source Comunitário",
                    goalTargetCents = 100000L,
                    goalCurrentCents = 62000L,
                    totalEarnedCents = 62000L,
                    startedAtMillis = now - 50 * 60_000L
                ),
                LiveStreamEntity(
                    id = "live_marina",
                    hostUserId = marina.id,
                    hostName = marina.displayName,
                    hostHandle = marina.handle,
                    hostVerificationTier = marina.verificationTier,
                    hostAvatarColorHex = marina.avatarColorHex,
                    title = "Masterclass: Massa Fresca & Molho Rústico",
                    description = "Receita completa passo a passo. Membros do Clube recebem o livro digital!",
                    category = "Culinária",
                    thumbnailKey = "culinary",
                    viewerCount = 612,
                    isLive = true,
                    isSupportersOnly = false,
                    goalTitle = "Forno Profissional da Cozinha",
                    goalTargetCents = 200000L,
                    goalCurrentCents = 145000L,
                    totalEarnedCents = 145000L,
                    startedAtMillis = now - 18 * 60_000L
                )
            )
            db.liveStreamDao().upsertAll(lives)

            val liveComments = listOf(
                LiveCommentEntity(
                    id = "lc1",
                    liveId = "live_camila",
                    authorName = "Rafael Mendes",
                    authorVerificationTier = VerificationTier.EMERALD_PARTNER.name,
                    text = "Som impecável hoje, Camila!",
                    isSuperDonation = true,
                    giftEmoji = "🚀",
                    donationCents = 5000L,
                    timestampMillis = now - 120_000L
                ),
                LiveCommentEntity(
                    id = "lc2",
                    liveId = "live_camila",
                    authorName = "Ana Beatriz",
                    text = "Toca MPB acústico por favor!!",
                    timestampMillis = now - 60_000L
                ),
                LiveCommentEntity(
                    id = "lc3",
                    liveId = "live_rafael",
                    authorName = "Lucas Ferreira",
                    authorVerificationTier = VerificationTier.BLUE_VERIFIED.name,
                    text = "Excelente explicação sobre IV no AES-GCM!",
                    isSuperDonation = true,
                    giftEmoji = "☕",
                    donationCents = 1500L,
                    timestampMillis = now - 90_000L
                ),
                LiveCommentEntity(
                    id = "lc4",
                    liveId = "live_marina",
                    authorName = "Juliana Alves",
                    text = "Essa massa ficou linda demais!",
                    isSuperDonation = true,
                    giftEmoji = "💎",
                    donationCents = 15000L,
                    timestampMillis = now - 45_000L
                )
            )
            db.liveCommentDao().insertAll(liveComments)

            val transactions = listOf(
                WalletTransactionEntity(
                    id = "tx_1",
                    type = TransactionType.LIVE_REVENUE.name,
                    counterpartyName = "Live: Papo de Tecnologia",
                    counterpartyHandle = "@carlos.pulso",
                    amountCents = 34000L,
                    note = "Arrecadação de presentes e super doações",
                    referenceCode = "PLS-LV-9482",
                    timestampMillis = now - 48 * 3600_000L
                ),
                WalletTransactionEntity(
                    id = "tx_2",
                    type = TransactionType.DONATION_RECEIVED.name,
                    counterpartyName = "Rafael Mendes",
                    counterpartyHandle = "@rafadev",
                    amountCents = 19200L,
                    note = "Apoio pelo conteúdo de segurança E2EE",
                    referenceCode = "PLS-DR-4810",
                    timestampMillis = now - 18 * 3600_000L
                ),
                WalletTransactionEntity(
                    id = "tx_3",
                    type = TransactionType.DONATION_SENT.name,
                    counterpartyName = "Camila Duarte",
                    counterpartyHandle = "@camiladuarte.som",
                    amountCents = -2500L,
                    note = "Doação na conversa • Meta da Live",
                    referenceCode = "PLS-DS-7731",
                    timestampMillis = now - 20 * 60_000L
                ),
                WalletTransactionEntity(
                    id = "tx_4",
                    type = TransactionType.DONATION_SENT.name,
                    counterpartyName = "Chef Marina Costa",
                    counterpartyHandle = "@marinacozinha",
                    amountCents = -2500L,
                    note = "Apoio para ingredientes da transmissão",
                    referenceCode = "PLS-DS-7704",
                    timestampMillis = now - 120 * 60_000L
                )
            )
            db.walletDao().insertAll(transactions)
        }
    }
}

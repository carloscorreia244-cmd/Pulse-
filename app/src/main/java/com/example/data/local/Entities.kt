package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class VerificationTier {
    NONE,
    BLUE_VERIFIED,
    GOLD_CREATOR,
    EMERALD_PARTNER
}

enum class MessageType {
    TEXT,
    IMAGE,
    AUDIO,
    DONATION,
    SYSTEM
}

enum class MessageStatus {
    SENT,
    DELIVERED,
    READ
}

enum class TransactionType {
    DONATION_SENT,
    DONATION_RECEIVED,
    LIVE_REVENUE,
    SUBSCRIPTION,
    TOP_UP,
    WITHDRAWAL
}

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val displayName: String,
    val handle: String,
    val bio: String,
    val avatarColorHex: Long,
    val isCurrentUser: Boolean = false,
    val verificationTier: String = VerificationTier.NONE.name,
    val publicKeyBase64: String,
    val privateKeyBase64: String = "",
    val isOnline: Boolean = false,
    val lastSeenLabel: String = "visto recentemente",
    val followersCount: Int = 0,
    val walletBalanceCents: Long = 0L,
    val totalDonationsReceivedCents: Long = 0L,
    val totalLiveRevenueCents: Long = 0L,
    val totalDonationsSentCents: Long = 0L
)

@Entity(tableName = "chats")
data class ChatEntity(
    @PrimaryKey val id: String,
    val peerUserId: String,
    val title: String,
    val peerHandle: String,
    val peerVerificationTier: String,
    val peerAvatarColorHex: Long,
    val isGroup: Boolean = false,
    val isPinned: Boolean = false,
    val isMuted: Boolean = false,
    val isOnline: Boolean = false,
    val unreadCount: Int = 0,
    val lastMessagePreview: String = "",
    val lastMessageTimeMillis: Long = System.currentTimeMillis(),
    val sharedSecretBase64: String,
    val safetyNumber: String,
    val keyVerifiedByUser: Boolean = false
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val chatId: String,
    val senderId: String,
    val senderName: String,
    val isFromMe: Boolean,
    val ciphertextBase64: String,
    val ivBase64: String,
    val messageType: String = MessageType.TEXT.name,
    val mediaUri: String? = null,
    val audioDurationSec: Int = 0,
    val donationAmountCents: Long = 0L,
    val replyToPreview: String? = null,
    val reactions: String = "",
    val timestampMillis: Long = System.currentTimeMillis(),
    val status: String = MessageStatus.READ.name,
    val isDownloaded: Boolean = true
)

@Entity(tableName = "live_streams")
data class LiveStreamEntity(
    @PrimaryKey val id: String,
    val hostUserId: String,
    val hostName: String,
    val hostHandle: String,
    val hostVerificationTier: String,
    val hostAvatarColorHex: Long,
    val title: String,
    val description: String,
    val category: String,
    val thumbnailKey: String,
    val viewerCount: Int,
    val isLive: Boolean = true,
    val isHostSelf: Boolean = false,
    val isSupportersOnly: Boolean = false,
    val isSubscribedByMe: Boolean = false,
    val subscriptionPriceCents: Long = 1990L,
    val goalTitle: String,
    val goalTargetCents: Long,
    val goalCurrentCents: Long,
    val totalEarnedCents: Long,
    val startedAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "live_comments")
data class LiveCommentEntity(
    @PrimaryKey val id: String,
    val liveId: String,
    val authorName: String,
    val authorVerificationTier: String = VerificationTier.NONE.name,
    val text: String,
    val isSuperDonation: Boolean = false,
    val giftEmoji: String = "",
    val donationCents: Long = 0L,
    val timestampMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "wallet_transactions")
data class WalletTransactionEntity(
    @PrimaryKey val id: String,
    val type: String,
    val counterpartyName: String,
    val counterpartyHandle: String,
    val amountCents: Long,
    val note: String,
    val referenceCode: String,
    val timestampMillis: Long = System.currentTimeMillis()
)

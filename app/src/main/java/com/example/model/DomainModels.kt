package com.example.model

import com.example.data.local.MessageStatus
import com.example.data.local.MessageType
import com.example.data.local.VerificationTier
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DecryptedMessage(
    val id: String,
    val chatId: String,
    val senderId: String,
    val senderName: String,
    val isFromMe: Boolean,
    val plaintext: String,
    val ciphertextBase64: String,
    val ivBase64: String,
    val type: MessageType,
    val mediaUri: String?,
    val audioDurationSec: Int,
    val donationAmountCents: Long,
    val replyToPreview: String?,
    val reactions: List<String>,
    val timestampMillis: Long,
    val status: MessageStatus,
    val isDownloaded: Boolean
)

data class LiveGiftOption(
    val id: String,
    val label: String,
    val emoji: String,
    val amountCents: Long
)

val DEFAULT_LIVE_GIFTS = listOf(
    LiveGiftOption("coffee", "Café", "☕", 500L),
    LiveGiftOption("pulse", "Pulso", "💚", 1500L),
    LiveGiftOption("rocket", "Foguete", "🚀", 5000L),
    LiveGiftOption("diamond", "Diamante", "💎", 15000L)
)

fun parseVerificationTier(raw: String): VerificationTier {
    return VerificationTier.entries.firstOrNull { it.name == raw } ?: VerificationTier.NONE
}

fun formatCurrencyBrl(cents: Long): String {
    val format = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
    return format.format(cents / 100.0)
}

fun formatClockTime(millis: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale("pt", "BR"))
    return sdf.format(Date(millis))
}

fun formatShortDate(millis: Long): String {
    val sdf = SimpleDateFormat("dd/MM HH:mm", Locale("pt", "BR"))
    return sdf.format(Date(millis))
}

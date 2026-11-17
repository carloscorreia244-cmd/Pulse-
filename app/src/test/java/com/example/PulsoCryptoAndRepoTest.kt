package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.crypto.E2EECryptoEngine
import com.example.data.local.AppDatabase
import com.example.data.local.VerificationTier
import com.example.data.repository.PulsoRepository
import com.example.model.DEFAULT_LIVE_GIFTS
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PulsoCryptoAndRepoTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: PulsoRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        runBlocking {
            AppDatabase.seedInitialData(db)
        }
        repository = PulsoRepository(db)
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun e2ee_ecdhAndAesGcm_roundTripAndSafetyNumberSymmetry() {
        val alice = E2EECryptoEngine.generateIdentityKeyPair()
        val bob = E2EECryptoEngine.generateIdentityKeyPair()

        val aliceShared = E2EECryptoEngine.deriveSharedSecretBase64(alice.privateKeyBase64, bob.publicKeyBase64)
        val bobShared = E2EECryptoEngine.deriveSharedSecretBase64(bob.privateKeyBase64, alice.publicKeyBase64)
        assertEquals(aliceShared, bobShared)

        val secretText = "Olá! Doação confirmada com segurança E2EE 🔐"
        val encrypted = E2EECryptoEngine.encrypt(secretText, aliceShared)
        assertNotEquals(secretText, encrypted.ciphertextBase64)

        val decrypted = E2EECryptoEngine.decrypt(encrypted.ciphertextBase64, encrypted.ivBase64, bobShared)
        assertEquals(secretText, decrypted)

        val safetyAB = E2EECryptoEngine.computeSafetyNumber(alice.publicKeyBase64, bob.publicKeyBase64)
        val safetyBA = E2EECryptoEngine.computeSafetyNumber(bob.publicKeyBase64, alice.publicKeyBase64)
        assertEquals(safetyAB, safetyBA)
        assertEquals(60, safetyAB.replace(" ", "").length)
    }

    @Test
    fun repository_sendEncryptedMessageAndDonation_updatesBalanceAndTranscript() = runBlocking {
        val meBefore = repository.observeCurrentUser().first()!!
        val chatId = repository.createContactAndChat("Mariana Silva", "@marianas")
        repository.sendTextMessage(chatId, "Mensagem secreta de teste")

        val msgs = repository.observeDecryptedMessages(chatId).first()
        assertTrue(msgs.any { it.plaintext == "Mensagem secreta de teste" })

        val chats = repository.observeChats().first()
        val targetChat = chats.first { it.id == chatId }
        val donationRes = repository.sendPeerDonation(targetChat.peerUserId, 2000L, "Apoio direto!")
        assertTrue(donationRes.isSuccess)

        val meAfter = repository.observeCurrentUser().first()!!
        assertEquals(meBefore.walletBalanceCents - 2000L, meAfter.walletBalanceCents)

        val transcript = repository.buildChatExportText(chatId, includeCiphertext = true)
        assertTrue(transcript.contains("Mensagem secreta de teste"))
        assertTrue(transcript.contains("DOAÇÃO"))
    }

    @Test
    fun repository_liveBroadcastAndSuperGift_tracksMonetizationAndVerification() = runBlocking {
        repository.updateProfileAndBadge("Carlos Criador", "Lives diárias", VerificationTier.GOLD_CREATOR)
        val me = repository.observeCurrentUser().first()!!
        assertEquals(VerificationTier.GOLD_CREATOR.name, me.verificationTier)

        val liveId = repository.startMyLive(
            title = "Live de Lançamento",
            category = "Tecnologia",
            goalTitle = "Meta Novo Estúdio",
            goalTargetCents = 10000L,
            supportersOnly = false
        )
        val gift = DEFAULT_LIVE_GIFTS.first()
        val res = repository.sendLiveGift(liveId, gift, "Mandou bem!")
        assertTrue(res.isSuccess)

        val live = repository.observeLiveStream(liveId).first()!!
        assertEquals(gift.amountCents, live.totalEarnedCents)
    }
}

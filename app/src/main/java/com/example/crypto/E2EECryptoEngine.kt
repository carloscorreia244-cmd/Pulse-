package com.example.crypto

import android.util.Base64
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SecureRandom
import java.security.spec.ECGenParameterSpec
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

data class E2EEKeyPair(
    val publicKeyBase64: String,
    val privateKeyBase64: String
)

data class EncryptedPayload(
    val ciphertextBase64: String,
    val ivBase64: String
)

object E2EECryptoEngine {
    private const val EC_CURVE = "secp256r1"
    private const val AES_TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_BITS = 128
    private const val GCM_IV_BYTES = 12

    private val secureRandom = SecureRandom()

    fun generateIdentityKeyPair(): E2EEKeyPair {
        val generator = KeyPairGenerator.getInstance("EC")
        generator.initialize(ECGenParameterSpec(EC_CURVE), secureRandom)
        val pair = generator.generateKeyPair()
        return E2EEKeyPair(
            publicKeyBase64 = Base64.encodeToString(pair.public.encoded, Base64.NO_WRAP),
            privateKeyBase64 = Base64.encodeToString(pair.private.encoded, Base64.NO_WRAP)
        )
    }

    fun deriveSharedSecretBase64(
        myPrivateKeyBase64: String,
        peerPublicKeyBase64: String
    ): String {
        val keyFactory = KeyFactory.getInstance("EC")
        val privateBytes = Base64.decode(myPrivateKeyBase64, Base64.NO_WRAP)
        val publicBytes = Base64.decode(peerPublicKeyBase64, Base64.NO_WRAP)
        val privateKey: PrivateKey = keyFactory.generatePrivate(PKCS8EncodedKeySpec(privateBytes))
        val publicKey: PublicKey = keyFactory.generatePublic(X509EncodedKeySpec(publicBytes))

        val agreement = KeyAgreement.getInstance("ECDH")
        agreement.init(privateKey)
        agreement.doPhase(publicKey, true)
        val rawSecret = agreement.generateSecret()

        // KDF: SHA-256 over ECDH shared secret with domain separator
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update("PULSO-E2EE-AES256-GCM-V1".toByteArray(Charsets.UTF_8))
        val aesKeyBytes = digest.digest(rawSecret)
        return Base64.encodeToString(aesKeyBytes, Base64.NO_WRAP)
    }

    fun encrypt(plaintext: String, sharedSecretBase64: String): EncryptedPayload {
        val keyBytes = Base64.decode(sharedSecretBase64, Base64.NO_WRAP)
        val secretKey = SecretKeySpec(keyBytes, "AES")
        val iv = ByteArray(GCM_IV_BYTES).also { secureRandom.nextBytes(it) }

        val cipher = Cipher.getInstance(AES_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_BITS, iv))
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))

        return EncryptedPayload(
            ciphertextBase64 = Base64.encodeToString(ciphertext, Base64.NO_WRAP),
            ivBase64 = Base64.encodeToString(iv, Base64.NO_WRAP)
        )
    }

    fun decrypt(
        ciphertextBase64: String,
        ivBase64: String,
        sharedSecretBase64: String
    ): String {
        return try {
            val keyBytes = Base64.decode(sharedSecretBase64, Base64.NO_WRAP)
            val ivBytes = Base64.decode(ivBase64, Base64.NO_WRAP)
            val cipherBytes = Base64.decode(ciphertextBase64, Base64.NO_WRAP)
            val secretKey = SecretKeySpec(keyBytes, "AES")

            val cipher = Cipher.getInstance(AES_TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_BITS, ivBytes))
            String(cipher.doFinal(cipherBytes), Charsets.UTF_8)
        } catch (_: Exception) {
            "[Mensagem criptografada indisponível]"
        }
    }

    /**
     * Generates a 60-digit Signal/WhatsApp-style Safety Number from two EC public keys.
     */
    fun computeSafetyNumber(publicKeyA: String, publicKeyB: String): String {
        val ordered = listOf(publicKeyA, publicKeyB).sorted()
        val digest = MessageDigest.getInstance("SHA-256")
        var hash = digest.digest((ordered[0] + "|" + ordered[1]).toByteArray(Charsets.UTF_8))
        // 5 rounds of SHA-256 stretching for fingerprint stability
        repeat(4) {
            hash = digest.digest(hash)
        }
        val digits = buildString {
            for (i in 0 until 24) {
                val byteVal = hash[i % hash.size].toInt() and 0xFF
                append((byteVal * 39).toString().padStart(3, '0'))
            }
        }.take(60)
        return digits.chunked(5).joinToString(" ")
    }

    fun shortFingerprint(publicKeyBase64: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(publicKeyBase64.toByteArray(Charsets.UTF_8))
        return hash.take(8).joinToString(":") { "%02X".format(it) }
    }
}

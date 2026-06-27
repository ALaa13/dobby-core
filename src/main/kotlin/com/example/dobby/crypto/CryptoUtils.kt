package com.example.dobby.crypto

import com.example.dobby.config.log
import com.example.dobby.exception.DobbyException
import java.security.GeneralSecurityException
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

object CryptoUtils {
    fun encryptToken(
        plainTextToken: String,
        encryptionKey: String,
    ): String =
        try {
            val keySpec = SecretKeySpec(encryptionKey.toByteArray(Charsets.UTF_8), "AES")

            val cipher = Cipher.getInstance("AES")
            cipher.init(Cipher.ENCRYPT_MODE, keySpec)

            val encryptedBytes = cipher.doFinal(plainTextToken.toByteArray(Charsets.UTF_8))

            Base64.getEncoder().encodeToString(encryptedBytes)
        } catch (e: GeneralSecurityException) {
            log.error("Cryptographic token encryption failed. Check key length/validity.", e)
            throw DobbyException.DatabaseException("Failed to securely encrypt token")
        } catch (e: Exception) {
            log.error("Unexpected error during token encryption", e)
            throw e
        }

    fun decryptToken(
        encryptedTokenBase64: String,
        encryptionKey: String,
    ): String =
        try {
            val keySpec = SecretKeySpec(encryptionKey.toByteArray(Charsets.UTF_8), "AES")

            val cipher = Cipher.getInstance("AES")
            cipher.init(Cipher.DECRYPT_MODE, keySpec)

            val encryptedBytes = Base64.getDecoder().decode(encryptedTokenBase64)
            val decryptedBytes = cipher.doFinal(encryptedBytes)

            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: GeneralSecurityException) {
            log.error("Cryptographic token decryption failed. Verification or key issue.", e)
            throw DobbyException.DatabaseException("Failed to securely decrypt token")
        } catch (e: Exception) {
            log.error("Unexpected error during token decryption", e)
            throw e
        }
}
